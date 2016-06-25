package systems.kestrel.bluetooth_le.services;
/**
 * Created by Rag Sagar.V on 3/28/16.
 */

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.UUID;


// Abstracts the pocket link connection, establishment and data reading.
public class BluetoothLinkFacade extends Service {
    private Activity mainActivity;
    public ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    public BluetoothDevice connectedDevice;
    public boolean isRecording = false; // This flag will be set when recording starts.

    private final static UUID serviceID = UUID.fromString("DA2B84F1-6279-48DE-BDC0-AFBEA0226079");
    private final static UUID modeCh = UUID.fromString("A87988B9-694C-479C-900E-95DFA6C00A24");
    private final static UUID txCh = UUID.fromString("18CDA784-4BD3-4370-85BB-BFED91EC86AF");
    private final static UUID infoCh = UUID.fromString("99564A02-DC01-4D3C-B04E-3BB1EF0571B2");
    private final static UUID rxCh = UUID.fromString("BF03260C-7205-4C25-AF43-93B1C299D159");

    private BluetoothLeScanner mLeScanner;
    private ScanSettings scanSettings;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    public final static int REQUEST_ENABLE_BT = 100;
    private BluetoothGatt mBluetoothGatt;
    private ScanCallback mLeScanCallback;
    private String TAG = "BluetoothLinkFacde";
    private Queue<Byte> pendingDataCache = new ArrayDeque<>(100);
    // Intent filters

    public static final String START_RECEIVING_PACKET = "start-receving-packet";
    public static final String FOUND_NEW_DEVICE = "found-new-device";
    public static final String PL_STATUS ="pl-status",
            PL_DISCONNECTED = "pl-disconnected",
            PL_CONNECTED ="pl-connected",
            CONNECTING_PL = "pl-connecting",
            PL_OUT_OF_RANGE ="pl-out-of-range",
            SCANING_PL ="scaning_pl";

    // Intent data keys
    public static final String EXTRA_DATAPACKET = "dataPacket", EXTRA_PACKET_TYPE = "packetType";

    private final IBinder mBinder = new PocketLinkServiceBinder();
    // To know the state in between when sending data is initiated and success is returned.
    private boolean sendingDataInitiated = false;

    public class PocketLinkServiceBinder extends Binder {
        public BluetoothLinkFacade getService() {
            return BluetoothLinkFacade.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    public void notifyAboutPacket(String packetType) {
        Intent intent = new Intent(BluetoothLinkFacade.START_RECEIVING_PACKET);
        intent.putExtra(BluetoothLinkFacade.EXTRA_PACKET_TYPE, packetType);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public BluetoothLinkFacade() {
        super();
        buildScanCallBacks();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly stopped.
        return START_STICKY;
    }

    // Requests the user to enable bluetooth if it is disabled.
    // If bluetooth is disabled returns false. Check this method
    // result before invoking the scan method.
    public boolean checkBluetoothDevice(Activity activity) {
        mBluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // Shows option to user to enable bluetooth if it is disabled.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        } else {
            Log.d(TAG, "Bluetooth LE is enabled.");
            return true;
        }
    }

    public void updateDeviceLocation(Location location) {

    }

    // Returns true if we are connected to the given device.
    public boolean isConnectedTo(BluetoothDevice device) {
        return device == this.connectedDevice;
    }
    // Scan for bluetooth devices and add it to a list.
    public void scanForDevices() {
        Log.d(TAG, "Scanning for pl.");
        notifyPocketLinkConnection(SCANING_PL);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){


        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceID.toString())).build();
        filters.add(filter);
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        mLeScanner.startScan(filters, scanSettings, mLeScanCallback);
        }
    }

    private void broadCastDeviceFound(BluetoothDevice device) {
        Intent intent = new Intent(FOUND_NEW_DEVICE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // This callback requires atleast API level 21.
    private void buildScanCallBacks(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            mLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    final BluetoothDevice btDevice = result.getDevice();
                    if(!deviceList.contains(btDevice)) {
                        Log.d(TAG, "Found new device " + btDevice.getName() + " " + btDevice.getAddress());
                        deviceList.add(btDevice);
                        broadCastDeviceFound(btDevice);
                        if(connectedDevice == null) {
                            // First time we start the app we scan for 10 seconds if there is an
                            // address of a last connected pocketlink in shared preferences.
                            // we have to automatically connect to it.
                        }
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Bluetooth scan failed. Error code " + errorCode);
            }
        };
        }
    }

    public void connectToDevice(final BluetoothDevice device) {
        Log.d(TAG, "Trying to connect to device: " + device.getName());
        // Passing autoconnect as true so connection to device will be reestablished
        // when the device is restarted or comes back in range.
        notifyPocketLinkConnection(CONNECTING_PL);
        mBluetoothGatt = device.connectGatt(mainActivity, true, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        mLeScanner.stopScan(mLeScanCallback);
    }

    public void stopScanning() {
        if (mLeScanner != null) {
            Log.d(TAG, "Stopping bluetooth scanning.");
            mLeScanner.stopScan(mLeScanCallback);
        }
    }

    // Disconnects from the pocketlink currently connected.
    public void disconnectFromCurrentDevice() {

        mBluetoothGatt.disconnect();
    }

    // This method tries to read a characterstic. On success the callback
    // OnCharacteristicRead will be called. On this callback we give
    // request to change device mode. Calling it here will fail, we have to
    // do this in the callback
    private void readDeviceInfo() {
        BluetoothGattService customService = mBluetoothGatt.getService(serviceID);
        BluetoothGattCharacteristic infoCharacteristic = customService.getCharacteristic(infoCh);
        if (infoCharacteristic != null) {
            Log.d(TAG, "Found info characteristic, reading info value.");
            mBluetoothGatt.readCharacteristic(infoCharacteristic);
        } else {
            Log.d(TAG, "Couldn't find info characteristic.");
        }
    }
    // This method will try to change the mode of the pocketlink bluetooth chipset.
    // This will make the pocketlink send all data written into serial device
    // in bluetooth. After successful request the callback onCharacteristicWrite
    // will be called. We have to do the next step of enable notification in this callback.
    private void changeDeviceMode() {
        BluetoothGattService customService = mBluetoothGatt.getService(serviceID);
        BluetoothGattCharacteristic modeCharacterisitic = customService.getCharacteristic(modeCh);
        if (modeCharacterisitic != null) {
            Log.d(TAG, "Found mode characteristic");
            modeCharacterisitic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            if (mBluetoothGatt.writeCharacteristic(modeCharacterisitic) == false) {
                Log.d(TAG, "Failed to write into mode characteristic");
            } else {
                Log.d(TAG, "Successfully initiated writing into characteristic.");
            }
        } else {
            Log.d(TAG, "Couldn't find characteristic to change mode.");
        }
    }

    // Enables auto notification from the device. After this the callback onCharacteristicChanged
    // will be called whenever we receive new data.
    private void enableNotificationFromDevice() {
        BluetoothGattService customService = mBluetoothGatt.getService(serviceID);
        BluetoothGattCharacteristic txCharacteristic = customService.getCharacteristic(txCh);
        if (txCharacteristic != null) {
            Log.d(TAG, "Found notification characteristic.");
            if (mBluetoothGatt.setCharacteristicNotification(txCharacteristic, true) == false) {
                Log.w(TAG, "Failed to enable notification.");
            }
            BluetoothGattDescriptor descriptor = txCharacteristic.getDescriptors().get(0);
            // BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE worked with our simulator
            // devices, but didn't work with real pocketlinks. Using
            // ENABLE_INDICATION_VALUE worked with pocketlinks.
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            if (mBluetoothGatt.writeDescriptor(descriptor) == true) {
                Log.d(TAG, "Write to descriptor initiated succesfully.");
            } else {
                Log.w(TAG, "Write to descriptor failed to initiate.");
            }
        } else {
            Log.w(TAG, "Couldn't find mode characteristic.");
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "STATE_CONNECTED");
                    connectedDevice = gatt.getDevice();
                    // Save the connected device address to the shared preferences
                    // for reconnecting on app restart.


                    notifyPocketLinkConnection(PL_CONNECTED);


                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    connectedDevice = null;
                    Log.d(TAG, "STATE_DISCONNECTED");


                    break;
                default:
                    connectedDevice = null;

                    Log.d(TAG, "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Bluetooth services discovered.");
            readDeviceInfo();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "Read characteristic info: " + characteristic.getValue().toString());
            if (characteristic.getUuid().equals(infoCh)) {
                // Finished reading info characteristic, now start changing the mode.
                changeDeviceMode();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,  BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write success.");
            } else {
                Log.w(TAG, "Descriptor write failed. " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Succesfully finished writing to characteristic.");
            } else {
                Log.w(TAG, "Failed to write into the characteristic.");
                // TODO: try again.
            }
            if (characteristic.getUuid().equals(modeCh)) {
                // Mode changing is done. Now start request to enable notification from device.
                enableNotificationFromDevice();
            } else if (characteristic.getUuid().equals(rxCh)) {
                sendingDataInitiated = false;
                // Data successfully send to the pocketlink. If there is data left to send,
                // send it.

            }


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // We receive data here. The data can be accessed by calling
            // characteristic.getValue()
            byte[] data = characteristic.getValue();
        }
    };









    public void notifyPocketLinkConnection(String status) {
        Intent intent = new Intent(PL_STATUS);
        intent.putExtra(PL_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Method that will tell acitvity to show a confirmation dialog to user
    // if he wants to go forward with the firmware updation.




    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
