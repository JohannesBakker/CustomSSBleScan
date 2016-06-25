package systems.kestrel.bluetooth_le.services.Bluetooth.Ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by Dev
 */
public interface BlePeripheralDelegate {
    public void gattConnected(BlePeripheral peripheral);
    public void gattDisconnected(BlePeripheral peripheral);
    public void gattServicesDiscovered(BlePeripheral peripheral);
    public void gattDataAvailable(BlePeripheral peripheral, BluetoothGattCharacteristic characteristic, byte[] value);
    public void gattReadRemoteRssi(BlePeripheral peripheral, int rssi);
    public void gattDescriptorWrite(BlePeripheral peripheral, BluetoothGattDescriptor descriptor, boolean status);
}
