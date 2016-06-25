package systems.kestrel.bluetooth_le.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;

import systems.kestrel.bluetooth_le.services.BluetoothLinkFacade;
import systems.kestrel.bluetooth_le.R;

public class BluetoothlinklistAdapter extends ArrayAdapter<BluetoothDevice> {

    private final LayoutInflater mInflater;
    private final ViewBinderHelper binderHelper;
    private BluetoothLinkFacade bluetoothLinkFacade;
    private AdapterView.OnItemClickListener onItemClickListener;

    public BluetoothlinklistAdapter(Context context, BluetoothLinkFacade pLinkFacade) {
        super(context, R.layout.bluetooth_row, pLinkFacade.deviceList);
        mInflater = LayoutInflater.from(context);
        binderHelper = new ViewBinderHelper();
        bluetoothLinkFacade = pLinkFacade;

        // uncomment if you want to open only one row at a time
         binderHelper.setOpenOnlyOne(true);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.bluetooth_row, parent, false);
//            TODO this is not working now, added event onItemClicklistner to the label
//            final View view = convertView;
//            final int _position = position;
//            convertView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Log.i("Adding", "Implementing eventLister");
//                    if (onItemClickListener != null) {
//                        onItemClickListener.onItemClick(null, view, _position, -1);
//                    }
//                }
//            });

            holder = new ViewHolder(convertView);
            holder.textView = (TextView) convertView.findViewById(R.id.pl_name);
            holder.deleteView = convertView.findViewById(R.id.delete_layout);
            holder.swipeLayout = (SwipeRevealLayout) convertView.findViewById(R.id.swipe_layout);
            holder.deleteView.setVisibility(View.INVISIBLE);
            convertView.setTag(holder);



        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final BluetoothDevice item = getItem(position);
        if (item != null) {
            final View view = convertView;
            final int _position = position;
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(null, view, _position, -1);
                    }
                }
            });
            if(TextUtils.isEmpty(item.getName())){
                holder.textView.setText(item.getAddress());
            }else {
                holder.textView.setText(item.getName());
            }

            if (this.bluetoothLinkFacade.connectedDevice != null) {
                if (item.getAddress() == this.bluetoothLinkFacade.connectedDevice.getAddress()) {
                    binderHelper.bind(holder.swipeLayout, item.getName());
                    holder.showAsConnected();
                    holder.deleteView.setVisibility(View.VISIBLE);
                    holder.swipeLayout.setLockDrag(false);

                    holder.deleteView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("PL", "Disconnecting");
                            if (bluetoothLinkFacade != null) {
                                bluetoothLinkFacade.disconnectFromCurrentDevice();
                                notifyDataSetChanged();
                                holder.swipeLayout.close(true);
                            }
                        }
                    });

                } else {
                    holder.showAsDisconnected();
                    holder.swipeLayout.setLockDrag(true);
                }
             } else {
                holder.showAsDisconnected();
                holder.swipeLayout.setLockDrag(true);
            }

        }



        return convertView;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }


    private class ViewHolder {
        TextView textView;
        View deleteView;
        SwipeRevealLayout swipeLayout;
        public ImageView plConnectedMark;


        public ViewHolder(View view){

//            this.disconnectButton = (Button)view.findViewById(R.id.disconnect_button);
            this.plConnectedMark = (ImageView)view.findViewById(R.id.pl_active);
            this.textView = (TextView) view.findViewById(R.id.pl_name);

//            this.disconnectButton.setOnClickListener(disconnectButtonListener);
        }

        public void showAsConnected() {
            this.plConnectedMark.setVisibility(View.VISIBLE);
//            this.disconnectButton.setVisibility(View.VISIBLE);
        }

        public void showAsDisconnected() {
            this.plConnectedMark.setVisibility(View.INVISIBLE);
//            this.disconnectButton.setVisibility(View.INVISIBLE);
        }
    }

}
