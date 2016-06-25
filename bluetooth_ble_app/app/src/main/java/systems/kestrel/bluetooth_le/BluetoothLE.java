package systems.kestrel.bluetooth_le;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.support.multidex.MultiDex;

import com.radiusnetworks.bluetooth.BluetoothCrashResolver;


/**
 * Created by Dev on 6/26/16.
 */
public class BluetoothLE extends Application{

    public static BluetoothLE _instance = null;
    private BluetoothCrashResolver bluetoothCrashResolver = null;

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }
    @Override
    public void onCreate() {
        super.onCreate();

        _instance = this;

        bluetoothCrashResolver = new BluetoothCrashResolver(this);
        bluetoothCrashResolver.start();
    }

    public static BluetoothLE sharedInstance() {
        if (_instance == null) {
            _instance = new BluetoothLE();
        }

        return _instance;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public BluetoothCrashResolver getBluetoothCrashResolver() {
        return bluetoothCrashResolver;
    }
}
