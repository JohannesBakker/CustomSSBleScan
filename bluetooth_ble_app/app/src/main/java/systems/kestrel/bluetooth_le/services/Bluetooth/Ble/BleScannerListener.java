package systems.kestrel.bluetooth_le.services.Bluetooth.Ble;

import android.bluetooth.BluetoothDevice;

public interface BleScannerListener {
	public void deviceScanned(BluetoothDevice device, int rssi, byte[] scanRecord);
	public boolean shouldCheckDevice(BluetoothDevice device, int rssi, byte[] scanRecord);
}
