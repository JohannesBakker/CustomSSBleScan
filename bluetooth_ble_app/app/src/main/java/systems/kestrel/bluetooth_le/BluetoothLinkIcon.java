package systems.kestrel.bluetooth_le;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;


/**
 * Created by Rahul Vivek M N on 4/24/16.
 */

enum BluetoothLinkStatus {
    CONNECTED(R.drawable.pocket_link_new_connected_to_gps),
    NOT_CONNECTED(R.drawable.pocketlink_not_connected),
    CONNECTED_BUT_NO_PACKETS(R.drawable.pocketlink_connected_no_packets);



    private int image;

    private BluetoothLinkStatus(int drawableId) {
        this.image = drawableId;
    }

    public int getImage() {
        return image;
    }

}

public class BluetoothLinkIcon extends ImageButton {
    private BluetoothLinkStatus mCurrentStatus;

    public BluetoothLinkIcon(Context context) {
        super(context);
    }

    public BluetoothLinkIcon(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public BluetoothLinkIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public void setStatus(BluetoothLinkStatus status) {
        this.setImageResource(status.getImage());
        mCurrentStatus = status;
    }

    public BluetoothLinkStatus getCurrentStatus() {
        return mCurrentStatus;
    }
}
