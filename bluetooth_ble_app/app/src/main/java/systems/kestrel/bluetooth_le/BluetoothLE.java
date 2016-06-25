package systems.kestrel.bluetooth_le;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;


/**
 * Created by malikumarbhutta on 4/13/16.
 */
public class BluetoothLE extends Application{


    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
