package systems.kestrel.bluetooth_le;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;


import java.util.ArrayList;


import systems.kestrel.bluetooth_le.services.BluetoothLinkFacade;
import systems.kestrel.bluetooth_le.views.BluetoothPopup;


import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class MainActivity extends AppCompatActivity {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };





    public static final String TAG = "MainActivity";


    BluetoothLinkIcon bluetoothLinkButton;

    public ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothLinkFacade bluetoothLinkFacade;
    private Intent mPocketLinkServiceIntent;

    private BluetoothPopup mBluetoothPopup;



    private BluetoothLinkStatus mLastBluetoothLinkStatus;

    SharedPreferences sharedpreferences;



    private BroadcastReceiver bluetoothCallback = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mDeviceList = bluetoothLinkFacade.deviceList;
            if (mBluetoothPopup != null) {
                mBluetoothPopup.refreshList();
            }
        }
    };

    private void checkAndStartLocationUpdates() {
        Log.i(TAG, "Checking location permissions.");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request for permission
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION},
                    PermissionRequest.LOCATION);
        } else {

        }
    }



    private boolean isRecording = false;





    private BroadcastReceiver packetCallback = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packetType = (String) intent.getSerializableExtra(
                    BluetoothLinkFacade.EXTRA_PACKET_TYPE);


        }
    };

    private BroadcastReceiver pocketLinkConnectionCallback = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = (String) intent.getSerializableExtra(bluetoothLinkFacade.PL_STATUS);
            if (status == bluetoothLinkFacade.PL_DISCONNECTED) {

                bluetoothLinkButton.setStatus(BluetoothLinkStatus.NOT_CONNECTED);
            } else if (status == bluetoothLinkFacade.PL_OUT_OF_RANGE) {
                bluetoothLinkButton.setStatus(BluetoothLinkStatus.NOT_CONNECTED);
            } else if (status == bluetoothLinkFacade.PL_CONNECTED) {

                bluetoothLinkButton.setStatus(BluetoothLinkStatus.CONNECTED_BUT_NO_PACKETS);
            } else if (status == bluetoothLinkFacade.CONNECTING_PL) {

            } else if (status == bluetoothLinkFacade.SCANING_PL) {

            }

            if (mBluetoothPopup != null) {
                mBluetoothPopup.refreshList();
            }
        }
    };




    /**
     * Returns true if flight recording is going on.
     *
     * @return
     */
    public Boolean isRecording() {
        return this.isRecording;
    }

    // Save the packets we were recording into db and continue.

    /**
     * Centers map to the default location and set the default altitude.
     * Currently the default location is Dubai.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValuesFromBundle(savedInstanceState);
        setContentView(R.layout.activity_main);



        mPocketLinkServiceIntent = new Intent(this, BluetoothLinkFacade.class);
        startService(mPocketLinkServiceIntent);



        checkAndStartLocationUpdates();


        bluetoothLinkButton = (BluetoothLinkIcon)findViewById(R.id.pocket_link_btn);

        bluetoothLinkButton.setOnClickListener(buttonListener);
//        trackUp.setOnClickListener(buttonListener);


    }






    private void showBluetoothPopup() {
        if (bluetoothLinkFacade.checkBluetoothDevice(MainActivity.this)) {
            if (bluetoothLinkFacade != null) {
                bluetoothLinkFacade.scanForDevices();
            }
            mBluetoothPopup = new BluetoothPopup(this, bluetoothLinkFacade);
            mBluetoothPopup.showAsDropDown(this.bluetoothLinkButton);
        }
    }



    View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.pocket_link_btn:
                    showBluetoothPopup();
                    break;

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
//        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
//        mSensorManager.unregisterListener(this, mAccelerometer);
//        mSensorManager.unregisterListener(this, mMagnetometer);

    }


    /**
     *  TODO Start handling two finger gestures for map pan .
     */


//    public void updateRotatingObjects(SensorEvent event){
//        compassButton.listentoRotate(event);
//    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(mPocketLinkServiceIntent, mPocketLinkServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(bluetoothCallback, new IntentFilter(BluetoothLinkFacade.FOUND_NEW_DEVICE));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(packetCallback, new IntentFilter(BluetoothLinkFacade.START_RECEIVING_PACKET));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(pocketLinkConnectionCallback, new IntentFilter(BluetoothLinkFacade.PL_STATUS));


    }

    @Override
    protected void onStop() {


        super.onStop();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(bluetoothCallback);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(packetCallback);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(pocketLinkConnectionCallback);


        if (mBluetoothPopup != null) {
            mBluetoothPopup.dismiss();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionRequest.LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "User enabled location.");

            }
        } else if (requestCode == BluetoothLinkFacade.REQUEST_ENABLE_BT) {
            Log.i(TAG, "User enabled bluetooth");
            if (resultCode == Activity.RESULT_OK) {
                showBluetoothPopup();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case PermissionRequest.LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted.
                    Log.i(TAG, "Permission granted");


                } else {
                    // Permission Denied

                }
                return;
        }
    }

    // On changing orientation onCreate will be called again. So we don't want to subscribe to
    // the location updates again on changing orientation. This is not tested.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // We need to save the following.
        // currentDeviceLocation, lastReceivedDataPacket, isRecording, packetsForPath,
        // pocketlinkFacade,



        outState.putSerializable(StateKey.pocketLinkStatus, bluetoothLinkButton.getCurrentStatus());





        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void initializeUI() {
        // This could happen when the device orientation changes or device gets locked


        // Update the falcon marker to the last known location.




        if (mLastBluetoothLinkStatus != null) {
            bluetoothLinkButton.setStatus(mLastBluetoothLinkStatus);
        }


    }

    private void updateServiceRelatedChanges() {



    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {



            mLastBluetoothLinkStatus = (BluetoothLinkStatus) savedInstanceState.getSerializable(
                    StateKey.pocketLinkStatus);





        }
    }

    private ServiceConnection mPocketLinkServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            BluetoothLinkFacade.PocketLinkServiceBinder binder =
                    (BluetoothLinkFacade.PocketLinkServiceBinder) service;
            bluetoothLinkFacade = binder.getService();


            updateServiceRelatedChanges();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    // Showning and hiding the race and win view with update values also



}

final class StateKey {
    public static String connectedToGoogleAPI = "connectedToGoogleAPI";
    public static final String lastReceivedDataPacket = "lastReceivedDataPacket",
    isRecording = "isRecording", currentDeviceLocation = "currentDeviceLocation",
    currentDeviceHeading = "currentDeviceHeading", bluetoothLinkFacade = "bluetoothLinkFacade",
    currentMapLatitude = "currentMapLatitude", currentMapLongitude = "currentMapLongitude",
    receivedPackets = "receivedPackets", googleAPIClient = "googleAPIClient",
    currentMapAltitude = "currentMapAltitude", mapStateIndex = "mapStateIndex",
    mapModeButtonMode = "mapModeButtonMode", pocketLinkStatus = "pocketLinkStatus",
    statusMessage = "statusMessage", transmitterIconStatus = "transmitterIconStatus",
    satelliteIconStatus = "satelliteIconStatus", selectedFabButton = "selectedFabButton",
    selectedChannelNumber = "selectedChannelNumber", selectedChannelLabel = "selectedChannelLabel",
            trainingClockStartTime = "trainingClockStartTime", firstTrainigPacket= "firstTrainigPacket",
            trainingPackets= "trainingPackets", trainingTimer = "trainingTimer",
            trainingDistance = "trainingDistance", traingMaxSpeed = "traingMaxSpeed",
            trainingFinish = "trainingFinish", trainingAvgSpeed = "trainingAvgSpeed";
}

final class PermissionRequest {
    public static final int LOCATION = 1;
}