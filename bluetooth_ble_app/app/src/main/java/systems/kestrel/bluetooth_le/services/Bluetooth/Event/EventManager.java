package systems.kestrel.bluetooth_le.services.Bluetooth.Event;

import android.content.Context;

import de.greenrobot.event.EventBus;

import systems.kestrel.bluetooth_le.BluetoothLE;

/**
 * Created by Dev
 */
public class EventManager {
    public static EventManager _instance;
    protected Context _context;

    public static EventManager initalize(Context context) {
        if (_instance == null)
            _instance = new EventManager(context);
        return _instance;
    }

    public static EventManager sharedInstance() {
        if (_instance == null) {
            _instance = new EventManager(BluetoothLE.sharedInstance().getApplicationContext());
        }
        return _instance;
    }

    private EventManager(Context context) {
        _context = context;
    }

    public void post(String name) {
        EventBus.getDefault().post(new SEvent(name));
    }

    public void post(String name, Object object) {
        EventBus.getDefault().post(new SEvent(name, object));
    }

    public void post(String name, Object object, String msg) {
        EventBus.getDefault().post(new SEvent(name, object, msg));
    }

    public void post(String name, Object object, String msg, String title) {
        EventBus.getDefault().post(new SEvent(name, object, msg, title));
    }

    public void register(Object objSubscriber) {
        EventBus.getDefault().register(objSubscriber);
    }

    public void unregister(Object objSubscriber) {
        EventBus.getDefault().unregister(objSubscriber);
    }

    public static boolean isEvent(SEvent e, String name) {
        if (name.equals(e.name))
            return true;
        return false;
    }
}
