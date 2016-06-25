package systems.kestrel.bluetooth_le.views;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;

import systems.kestrel.bluetooth_le.services.BluetoothLinkFacade;
import systems.kestrel.bluetooth_le.R;
import systems.kestrel.bluetooth_le.adapters.BluetoothlinklistAdapter;

/**
 * Created by ragsagar on 5/18/16.
 */
public class BluetoothPopup extends PopupWindow {

    private Context mContext;
    private ListView mListView;
    private TextView mStatusTextView;
    private BluetoothLinkFacade mBluetoothLinkFacade;
    private final static String TAG = "BluetoothPopup";
    private BluetoothlinklistAdapter mListAdapter;
    private SwipeMenu slideMenu;
    private ListAdapter adapter;

    public BluetoothPopup(final Context context, final BluetoothLinkFacade bluetoothLinkFacade) {
        super(context);
        this.mContext = context;
        this.mBluetoothLinkFacade = bluetoothLinkFacade;
        View popupView = LayoutInflater.from(context).inflate(R.layout.pocketlinks_popup, null);
        this.setContentView(popupView);
        this.setOutsideTouchable(true);
        this.setFocusable(true);
        mListView = (ListView) popupView.findViewById(R.id.listView);
        mListAdapter = new BluetoothlinklistAdapter(context, bluetoothLinkFacade);

        mListAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Connecting to device");
                if(mBluetoothLinkFacade != null && mBluetoothLinkFacade.deviceList.size() > 0) {
                    BluetoothDevice currentDevice = mBluetoothLinkFacade.deviceList.get(position);
                    mBluetoothLinkFacade.connectToDevice(currentDevice);
                }
            }
        });

        mListView.setAdapter(mListAdapter);
        mStatusTextView = (TextView) popupView.findViewById(R.id.nopocketlinks_text);
        this.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        this.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);


        this.setOnDismissListener(mOnDismissListener);
    }

    /**
     * This will notify the adapter about change in data. This method should be called
     * when we find new device, so the list will be rerendered.
     */
    public void refreshList() {
        if (mBluetoothLinkFacade != null) {
            // Refresh the list.
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
    }




    /**
     * On dismiss we are trying to stop the scanning. Otherwise the scanning will go on
     * draining the battery. Something I noticed is that if we have the popup open and the
     * user changes the orientation is dismiss listener won't get called and the
     * scanning will never stop. For this we have to put some timer into the scanning part
     * to stop after some time.
     */
    OnDismissListener mOnDismissListener = new OnDismissListener() {
        @Override
        public void onDismiss() {
            if (mBluetoothLinkFacade != null) {
                mBluetoothLinkFacade.stopScanning();
            }
        }
    };
}
