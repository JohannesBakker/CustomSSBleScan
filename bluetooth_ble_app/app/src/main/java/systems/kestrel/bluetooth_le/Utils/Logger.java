package systems.kestrel.bluetooth_le.Utils;

/**
 * Created by Dev
 */

public class Logger {
	public static final String TAG = "CustomBleScan";
	public static void log(String tag, String format, Object ...args) {
		{
			try {
				android.util.Log.w(TAG + ": " + tag, String.format(format, args));
			}
			catch (Exception e) {
				//e.printStackTrace();
			}
		}
	}

	public static void logError(String tag, String format, Object ...args) {
		//if (config.USE_CRASHLYTICSLOG) {
		//	Crashlytics.log(Log.ERROR, TAG + " : " + tag, String.format(format, args));
		//}
		//else
		{
			try {
			android.util.Log.e(TAG + ": " + tag, String.format(format, args));
			}
			catch (Exception e) {
				//e.printStackTrace();
			}
		}
	}

	public static void e(String tag, String format, Object ...args) {
		logError(tag, format, args);
	}
}
