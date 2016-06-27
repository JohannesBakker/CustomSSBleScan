package systems.kestrel.bluetooth_le.services.Bluetooth.Ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import com.radiusnetworks.bluetooth.BluetoothCrashResolver;


import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import systems.kestrel.bluetooth_le.BluetoothLE;
import systems.kestrel.bluetooth_le.Utils.Logger;
import systems.kestrel.bluetooth_le.services.Bluetooth.Event.SEvent;

/**
 * Created by Dev
 */
public class BleManager implements BleScannerListener, BlePeripheralDelegate, BluetoothCrashResolver.UpdateNotifier {

    public static int           BT_ERROR_NO             = 0;
    public static int           BT_ERROR_UNKNOWN        = -1;
    public static int           BT_ERROR_NO_SERVICE     = -2;
    public static int           BT_ERROR_NO_DEVICE      = -3;
    public static int           BT_ERROR_ENABLE_TIMEOUT = -4;
    public static int          BT_ERROR_CONNECT_TIMEOUT = -5;
    public static int           BT_ERROR_CONNECT_FAILED = -6;
    public static int           BT_ERROR_COMMAND_FAILED = -7;
    public static int          BT_ERROR_COMMAND_TIMEOUT = -8;
    public static int     BT_ERROR_COMMAND_WRITE_FAILED = -9;

    public static final String TAG = "BleManager";
    public static final int   REQUEST_ENABLE_BLE = 1;

    protected static BleManager _instance = null;
    /*!
     *  \brief Posted when BLEManager receives advertisement from a peripheral.
     *
     *  userInfo keys:
     *  - kBLEManagerPeripheralKey
     *  - kBLEManagerManagerKey
     *  - kBLEManagerAdvertisementDataKey
     */
    public static final String kBLEManagerDiscoveredPeripheralNotification = "blemanager discovered peripheral notification";

    /*!
     *  \brief  Posted when BLEManager removes old advertising peripherals.
     *
     *  userInfo keys:
     *  - kBLEManagerPeripheralKey
     *  - kBLEManagerManagerKey
     *
     *  This notification posted as a result of calling BLEManager::purgeAdvertisingDevices:
     */
    public static final String kBLEManagerUndiscoveredPeripheralNotification = "blemanager undiscovered peripheral notification";

    /*!
     *  \brief  Posted when a BLEPeripheral connects
     *
     *  userInfo keys:
     *  - kBLEManagerPeripheralKey
     *  - kBLEManagerManagerKey
     */
    public static final String kBLEManagerConnectedPeripheralNotification = "blemanager connected peripheral notification";

    /*!
     *  \brief Posted when a BLEPeripheral disconnects
     *
     *  userInfo keys:
     *  - kBLEManagerPeripheralKey
     *  - kBLEManagerManagerKey
     */
    public static final String kBLEManagerDisconnectedPeripheralNotification = "blemanager disconnected peripheral notification";

    /*!
     *  \brief Posted when BLEPeripheral fails to connect
     *
     *  userInfo keys:
     *  - kBLEManagerPeripheralKey
     *  - kBLEManagerManagerKey
     */
    public static final String kBLEManagerPeripheralConnectionFailedNotification = "blemanager peripheral connection failed notification";

    public static final String kBLEManagerPeripheralServiceDiscovered = "blemanager service discovered";

    public static final String kBLEManagerPeripheralDataAvailable = "blemanager data available";
    public static final String kBLEManagerPeripheralRssiUpdated = "blemanager rssi updated";
    public static final String kBLEManagerPeripheralDescriptorWrite = "peripheral descriptor write";

    /*!
     *  \brief  Notification posted when Bluetooth state changes
     */
    public static final String kBLEManagerStateChanged = "blemanager state changed";
    public static final String kBLEManagerPeripheralDataWrited = "blemanager data writed";

    protected BluetoothAdapter mBluetoothAdapter;
    protected Context mContext;
    protected boolean mAdapterDisabledManually;
    protected ArrayList<BlePeripheral> scannedPeripherals;
    protected ArrayList<UUID> services;
    protected boolean mScanStarted;
    protected boolean mMustStartScan;
    protected Handler stopHandler;
    protected Runnable stopRunnable;
    protected Handler enableHandler;

    public static class CharacteristicData {
        public BlePeripheral peripheral;
        public BluetoothGattCharacteristic characteristic;
        public byte[] value;
    }

    public static class DescriptorData {
        public BlePeripheral peripheral;
        public BluetoothGattDescriptor descriptor;
        public boolean success;
    }


    public static BleManager initialize(Context context, BluetoothCrashResolver bluetoothCrashResolver) {
        if (_instance == null) {
            _instance = new BleManager(context, bluetoothCrashResolver);
        }
        return _instance;
    }

    public static BleManager sharedInstance() {
        if (_instance == null) {
            _instance = new BleManager(BluetoothLE.sharedInstance(), BluetoothLE.sharedInstance().getBluetoothCrashResolver());
        }
        return _instance;
    }

    private BleManager(Context context, BluetoothCrashResolver bluetoothCrashResolver) {

        EventBus.getDefault().register(this);

        this.mContext = context;
        checkBluetoothAdapter();
        bluetoothCrashResolver.setUpdateNotifier(this);
        BleScanner.initialize(context, bluetoothCrashResolver);

        mAdapterDisabledManually = false;

        scannedPeripherals = new ArrayList<BlePeripheral>();
        mScanStarted = false;
        mMustStartScan = false;

        stopHandler = new Handler(context.getMainLooper());
        stopRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScanStarted) {
                    stopScan();
                }
            }
        };

        enableHandler = new Handler(context.getMainLooper());
    }

    private void checkBluetoothAdapter() {
        try {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        } catch (RuntimeException ex) {
            Log.e(TAG, "checkBluetoothAdapter : exception", ex);
        }
        if (mBluetoothAdapter == null) {
            Logger.e(TAG, "checkBluetoothAdapter - bluetooth adapter is null, bluetooth is not available");
        }
        else {
            //Logger.log(TAG, "checkBluetoothAdapter - bluetooth adapter - bluetooth is available");
        }
    }

    public BluetoothAdapter bluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public boolean isBleAvailable() {
        if (!isBleSupported(mContext))
            return false;

        if (mBluetoothAdapter == null)
            return false;

        return true;
    }

    public boolean isBleEnabled() {
        if (mBluetoothAdapter == null) {
            checkBluetoothAdapter();
            if (mBluetoothAdapter == null)
                return false;
        }

        return mBluetoothAdapter.isEnabled();
    }

    public void stopAdapter() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
            mAdapterDisabledManually = false;
        }
    }

    public void disableAdapter() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
            mAdapterDisabledManually = true;
        }
    }

    public void enableAdapter() {
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.enable();
        else {
            checkBluetoothAdapter();
            if (mBluetoothAdapter != null)
                mBluetoothAdapter.enable();
        }
    }

    public static boolean isBleSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void onEvent(SEvent e) {
        if (e.name.equals(SEvent.EVENT_BLUETOOTH_STATE_CHANGED)) {
            Integer obj = (Integer)e.object;
            int state = obj.intValue();

            //Logger.log(TAG, String.format("BleManager : bluetooth state changed : %d", state));

            if (state == BluetoothAdapter.STATE_OFF) {
                // stop scanning
                BleScanner.sharedInstance().stop();

                if (mMustStartScan == true) {
                    enableAdapter();
                }
                else {
                    if (mAdapterDisabledManually) {
                        enableHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                enableAdapter();
                            }
                        }, 1 * 1000);
                    }
                }
            }
            else if (state == BluetoothAdapter.STATE_ON) {
                // restart scanning when bluetooth is on
                if (mMustStartScan) {
                    _startScanLocally();
                }
            }
            else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                //mMustStartScan = false;
                mAdapterDisabledManually = false;
            }

            EventBus.getDefault().post(new SEvent(kBLEManagerStateChanged, obj));
        }
    }

    public boolean scanForPeripheralsWithServices(ArrayList<UUID> services, boolean allowDuplicates) {

        //Logger.log(TAG, "BleManager.scanForPeripheralsWithServices()");

        if (mScanStarted) {
            Logger.log(TAG, "already started scanning - mScanStarted = true, return");
            return true;
        }

        // check
        if (!isBleSupported(mContext))
            return false;

        mMustStartScan = true;

        if (!isBleEnabled()) {
            enableAdapter();
            return true;
        }

        this.services = services;
        return _startScanLocally();
    }

    public boolean restartScanForPeripherals() {
        Logger.log(TAG, "BleManager.restartScanForPeripherals()");

        if (!isBleSupported(mContext))
            return false;

        mMustStartScan = true;
        if (isBleEnabled()) {
            disableAdapter();
            return true;
        }
        else {
            enableAdapter();
            return true;
        }
    }

    private boolean _startScanLocally() {
        scannedPeripherals.clear();

        BleScanner.sharedInstance().listner = this;
        boolean isStarted = BleScanner.sharedInstance().start();

        mScanStarted = true;

        stopHandler.postDelayed(stopRunnable, 100 * 1000);
        return isStarted;
    }

    public void stopScan() {
        //Logger.log(TAG, "BleManager.stopScan()");

        mMustStartScan = false;
        BleScanner.sharedInstance().stop();
        mScanStarted = false;
    }

    public ArrayList<BlePeripheral> getScannedPeripherals() {
        return scannedPeripherals;
    }

    @Override
    public void deviceScanned(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (mScanStarted == false)
            return;

        Long currentTime = System.currentTimeMillis();
        List list = Collections.synchronizedList(scannedPeripherals);
        BlePeripheral existPeripheral = null;
        synchronized(list) {
            Iterator i = list.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                BlePeripheral peripheral = (BlePeripheral)i.next();
                if (peripheral.address().equalsIgnoreCase(device.getAddress())) {
                    existPeripheral = peripheral;
                    break;
                }
            }

            if (existPeripheral != null) {
                long prevScannedTime = existPeripheral.scannedTime;
                existPeripheral.scannedTime = currentTime;
                existPeripheral.setRssi(rssi);

                EventBus.getDefault().post(new SEvent(kBLEManagerDiscoveredPeripheralNotification, existPeripheral));
            } else {
                existPeripheral = new BlePeripheral(mContext, device.getAddress(), rssi, scanRecord);
                existPeripheral.delegate = this;
                list.add(existPeripheral);
                existPeripheral.scannedTime = currentTime;

                EventBus.getDefault().post(new SEvent(kBLEManagerDiscoveredPeripheralNotification, existPeripheral));
            }
        }
    }

    @Override
    public boolean shouldCheckDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (this.services == null) {

            if (device.getName() == null) {
                Logger.log(TAG, "checking device : Unsupported band was discovered(name=\"%s\", address=\"%s\""
                        , device.getName(), device.getAddress());
                return false;
            }
            if (device.getName().contains("Power Band")) {
                Logger.log(TAG, "checking device : Power Band was discovered successful (name=\"%s\", address=\"%s\"",
                        device.getName(), device.getAddress());
                return true;
            }
            Logger.log(TAG, "checking device : discovered Power Band (name=\"%s\", address=\"%s\"",
                    device.getName(), device.getAddress());
            return false;

        }
        else {
            ArrayList<UUID> uuids = parseUuids(scanRecord);
            boolean isPeripheral = false;
            for (UUID uuid : uuids) {
                if (services.contains(uuid)) {
                    isPeripheral = true;
                    break;
                }
            }
            if (isPeripheral) {
                Logger.log(TAG, "checking device : discovered PowerBand by Service (name=\"%s\", address=\"%s\"",
                        device.getName(), device.getAddress());
            } else {
                Logger.log(TAG, "checking device : An unsupported device found by Service (name=\"%s\", address=\"%s\"",
                        device.getName(), device.getAddress());
            }
            return isPeripheral;
        }
    }

    public boolean connectPeripheral(BlePeripheral peripheral) {
        //Logger.log("connectPeripheral (%s) (%s) - calling peripheral.connect()", peripheral.address(), peripheral.name());
        boolean ret = peripheral.connect();
        if (ret == false) {
            Logger.e(TAG, "connectPeripheral (address=\"%s\") (name=\"%s\") - calling peripheral.connect() - returned false",
                    peripheral.address(), peripheral.name());
        }
        return ret;
    }

    public void disconnectPeripheral(BlePeripheral peripheral) {
        Logger.log(TAG, "disconnectPeripheral (address=\"%s\") (name=\"%s\") - calling peripheral.disconnect()",
                peripheral.address(), peripheral.name());
        peripheral.disconnect();
    }

    public void purgeAdvertisingDevices(int duration) {
        // ignore it if scanning stored
        if (mScanStarted == false)
            return;

        Long currentTime = System.currentTimeMillis();
        List list = Collections.synchronizedList(scannedPeripherals);
        ArrayList<BlePeripheral> purgingDevices = new ArrayList<BlePeripheral>();
        synchronized(list) {
            Iterator i = list.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                BlePeripheral peripheral = (BlePeripheral)i.next();
                Long scannedTime = peripheral.scannedTime;
                if (scannedTime != null && (currentTime - scannedTime) >= TimeUnit.SECONDS.toMillis(duration)) {
                    purgingDevices.add(peripheral);
                }
            }
            for (BlePeripheral purgingDevice : purgingDevices) {
                list.remove(purgingDevice);
                EventBus.getDefault().post(new SEvent(kBLEManagerUndiscoveredPeripheralNotification, purgingDevice));
            }
        }
    }

    @Override
    public void gattConnected(BlePeripheral peripheral) {
        EventBus.getDefault().post(new SEvent(kBLEManagerConnectedPeripheralNotification, peripheral));
    }

    @Override
    public void gattDisconnected(BlePeripheral peripheral) {
        EventBus.getDefault().post(new SEvent(kBLEManagerDisconnectedPeripheralNotification, peripheral));
    }

    @Override
    public void gattServicesDiscovered(BlePeripheral peripheral) {
        EventBus.getDefault().post(new SEvent(kBLEManagerPeripheralServiceDiscovered, peripheral));
    }

    @Override
    public void gattDataAvailable(BlePeripheral peripheral, BluetoothGattCharacteristic characteristic, byte[] value) {
        CharacteristicData data = new CharacteristicData();
        data.peripheral = peripheral;
        data.characteristic = characteristic;
        data.value = value;
        EventBus.getDefault().post(new SEvent(kBLEManagerPeripheralDataAvailable, data));
    }

    @Override
    public void gattReadRemoteRssi(BlePeripheral peripheral, int rssi) {
        EventBus.getDefault().post(new SEvent(kBLEManagerPeripheralRssiUpdated, peripheral));
    }

    @Override
    public void gattDescriptorWrite(BlePeripheral peripheral, BluetoothGattDescriptor descriptor, boolean status) {
        DescriptorData data = new DescriptorData();
        data.peripheral = peripheral;
        data.descriptor = descriptor;
        data.success = status;
        EventBus.getDefault().post(new SEvent(kBLEManagerPeripheralDescriptorWrite, data));
    }

    @Override
    public void gattCharacteristicWrite(BlePeripheral peripheral, BluetoothGattCharacteristic characteristic, int status) {
        CharacteristicData data = new CharacteristicData();
        data.peripheral = peripheral;
        data.characteristic = characteristic;
        data.value = new byte[1];

        data.value[0] = (byte)status;

        EventBus.getDefault().post(new SEvent(kBLEManagerPeripheralDataWrited, data));
    }

    public static String getLongUuidFromShortUuid(String shortUuid) {
        return String.format("0000%s-0000-1000-8000-00805f9b34fb", shortUuid);
    }

    private ArrayList<UUID> parseUuids(byte[] advertisedData) {
        ArrayList<UUID> uuids = new ArrayList<UUID>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }

    @Override
    public void dataUpdated() {
        //
    }

    @Override
    public void scanStopped() {
        stopScan();
    }
}
