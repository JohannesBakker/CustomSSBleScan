package systems.kestrel.bluetooth_le;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by admin1 on 4/28/16.
 */
public class BatteryIndicator extends ImageView {
    public BatteryIndicator(Context context) {
        super(context);
    }

    public BatteryIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static boolean isBetween(Double x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    private int getImage(Double batteryPercentage) {
        if (isBetween(batteryPercentage, 1, 20)) {
            return R.drawable.battery_one;
        } else if(isBetween(batteryPercentage, 21, 40)) {
            return  R.drawable.battery_fourty;
        } else if(isBetween(batteryPercentage, 41, 60)) {
            return R.drawable.battery_sixty;
        } else if(isBetween(batteryPercentage, 61, 80)) {
            return R.drawable.battery_eighty;
        } else if(isBetween(batteryPercentage, 81, 200)) {
            return R.drawable.battery_full;
        } else {
            return R.drawable.battery_full;
        }
    }

    public void setDefaultImage() {
        this.setImageResource(R.drawable.battery_one);
    }

    public void setBattery(Double batteryPercentage) {
        if (batteryPercentage == null) {
            this.setDefaultImage();
        } else {
            this.setImageResource(this.getImage(batteryPercentage));
        }
    }
}
