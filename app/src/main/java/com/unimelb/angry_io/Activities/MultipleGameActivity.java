package com.unimelb.angry_io.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.common.base.Splitter;
import com.unimelb.angry_io.Cmd.PROTOCOL;
import com.unimelb.angry_io.Network.BluetoothService;
import com.unimelb.angry_io.R;
import com.unimelb.angry_io.System.AngryResource;
import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

import java.util.Calendar;

public class MultipleGameActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "Angry_io";
    private static final String TAGP = "Protocol";

    Context ctx;

    IntentFilter filter1;
    IntentFilter filter2;
    IntentFilter filter3;
    IntentFilter filter4;

    private WorldView worldView;
    static public BluetoothService bluetoothService;
    private boolean singleMode;
    private PowerManager.WakeLock wl;

    //Bluetooth
    BluetoothAdapter mBTAdapter;

    // Game Centre
    private GoogleApiClient mGoogleApiClient = null;


    //BROADCAST RECEIVER
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            } else if ((BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(), "connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            } else if ((BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(), "Player \"" + device.getName() + "\" has left the game", Toast.LENGTH_SHORT).show();

                //TODO if master pause stay here
                //TODO if slave pause close
                //TODO if AcceptThread disconnected check if it is connected one or just AcceptThread timeout
                if (!CONFIG.MASTER) {
                    if (bluetoothService != null) bluetoothService.stop();
                    worldView.stop();
                    finish();
                } else { //MASTER
                    //TODO CLOSE SPECIFIC DEVICE
                    if (bluetoothService != null) {
                        bluetoothService.stop(device);
                        bluetoothService.RestartAvailableBTDevice(device);
                    }
                }
            } else if ((BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action))) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(), "disconnected REQ from " + device.getName(), Toast.LENGTH_SHORT).show();
            } else if (("android.intent.action.GAME".equals(action))) {
                int score = 0;
                if ((score = intent.getIntExtra("GAME_COMPLETE", 0)) != 0) {
                    // Update LeaderBoard
                    if (MenuActivity.mGoogleApiClient.isConnected())
                        Games.Leaderboards.submitScore(MenuActivity.mGoogleApiClient
                                , CONFIG.LEADERBOARD_ID, (long) score);
                    worldView.stop();
                    finish();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ctx = this;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "MultipleGameActivity");
        wl.acquire();

        Bundle extras = getIntent().getExtras();

        String gameMode = extras.getString("gameMode");
        if (gameMode != null && gameMode.equals("single")) {
            singleMode = true;
        }
        singleMode = false;

        if (!singleMode) {
            //Create msgQueue
            bluetoothService = ((AngryResource) this.getApplication()).getBTService();//randy get handler
            bluetoothService.setmBTHandler(mHandler);

            CONFIG.MASTER = extras.getBoolean("MASTER");
            if (CONFIG.MASTER)
                Toast.makeText(ctx, "MMMMMMMM.I`m Master", Toast.LENGTH_SHORT).show();//Log.d(TAG, "MMMMMMMM.I`m Master");
            else
                Toast.makeText(ctx, "SSSSSSSS.I`m Slave", Toast.LENGTH_SHORT).show();//Log.d(TAG, "SSSSSSSS.I`m Slave");

            //Register for bluetooth state change
            filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter4 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);

            //KEEP DISCOVERY
            mBTAdapter = bluetoothService.getBluetoothAdapter();
            if (mBTAdapter.isDiscovering()) {
                mBTAdapter.cancelDiscovery();
            }

            mBTAdapter.startDiscovery();

            //registering our receiver
            IntentFilter intentFilter = new IntentFilter(
                    "android.intent.action.GAME");
            this.registerReceiver(mReceiver, intentFilter);
            this.registerReceiver(mReceiver, filter1);
            this.registerReceiver(mReceiver, filter2);
            this.registerReceiver(mReceiver, filter3);
            this.registerReceiver(mReceiver, filter4);
        }


        // Game Centre
        mGoogleApiClient = ((AngryResource) getApplication()).getGoogleApiClient();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //Force device to stay in portrait orientation
        requestWindowFeature(Window.FEATURE_NO_TITLE); //Remove banner from the top of the activity

        setContentView(R.layout.activity_multiple_game);

        worldView = (WorldView) findViewById(R.id.multiple_worldView);
    }


    public static final int INFO_STATE_CHANGE = 1;
    public static final int INFO_READ = 2;
    public static final int INFO_WRITE = 3;
    public static final int INFO_DEVICE_NAME = 4;
    public static final int INFO_TOAST = 5;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Toast.makeText(getApplicationContext(), "state change = " + msg.what , Toast.LENGTH_SHORT).show();
            switch (msg.what) {
                case INFO_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.BT_STATE_CONNECTED:
                            if (!CONFIG.MASTER) {
                            }
                            Toast.makeText(getApplicationContext(), "Status=BT_STATE_CONNECTED", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.BT_STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            //Toast.makeText(getApplicationContext(), "Status=BT_STATE_CONNECTING", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.BT_STATE_LISTEN:
                            //Toast.makeText(getApplicationContext(), "Status=BT_STATE_LISTEN", Toast.LENGTH_SHORT).show();
                        case BluetoothService.BT_STATE_FREE:
                            //Toast.makeText(getApplicationContext(), "Status=BT_STATE_FREE", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case INFO_WRITE:
                    ((AngryResource) getApplication()).setBTService(bluetoothService);

                    break;
                case INFO_READ:

                    Log.d("Protocol", "Info read activates here");
                    byte[] readBuf = (byte[]) msg.obj;
                    String message = new String(readBuf);
                    break;

                case INFO_DEVICE_NAME:
                    break;
                case INFO_TOAST:
                    break;
            }
        }
    };


    static public int getNumConnected() {
        return bluetoothService.getNumConnected();
    }

    static public void sendBTMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothService.BT_STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write

            // IMPORTANT add END token to the messages
            message += PROTOCOL.LOG_DELIMITER;
            if (message.length() > CONFIG.NW_FRAGMENT_SIZE) {

                Iterable<String> fragments = Splitter.fixedLength(CONFIG.NW_FRAGMENT_SIZE).split(message);
                for (String fragment : fragments) {
                    fragment = fragment + CONFIG.NW_FRA_DELIMITER;
                    byte[] send = fragment.getBytes();
                    bluetoothService.write(send);
                    Log.d(TAGP, "sendBTMessage: send fragment " + fragment);
                }
            } else {
                byte[] send = message.getBytes();

                Log.d(TAGP, "MutipleGA size<512 sendBTMessage " + message);
                bluetoothService.write(send);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worldView.stop();
        try {
            this.unregisterReceiver(this.mReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "epicReciver is already unregistered");
        }

        if (bluetoothService != null) {
            Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_SHORT).show();
            bluetoothService.stop();
        }
    }

    @Override
    protected void onPause() {
        worldView.stop();
        if (wl.isHeld()) {
            wl.release();
        }


        try {
            this.unregisterReceiver(this.mReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "epicReciver is already unregistered");
        }

        //if master pause stay here
        //if slave pause close
        if (bluetoothService != null) bluetoothService.stop();


        super.onPause();
    }


    @Override
    protected void onResume() {

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();

        //registering our receiver
        IntentFilter intentFilter = new IntentFilter(
                "android.intent.action.GAME");
        this.registerReceiver(mReceiver, intentFilter);

        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
        this.registerReceiver(mReceiver, filter4);
        bluetoothService = ((AngryResource) this.getApplication()).getBTService();
        if (bluetoothService != null) {
            bluetoothService.setmBTHandler(mHandler);
        }
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txv_single_player:
                Intent Game = new Intent(MultipleGameActivity.this, SingleGameActivity.class);
                startActivity(Game);
                //finish();
                break;

            case R.id.txv_setup_multiple_players:

                Toast.makeText(ctx, "Setup Multi-players!!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(
                        MultipleGameActivity.this,
                        JoinActivity.class);
                intent.putExtra("mainclass", MultipleGameActivity.this.getClass());
                startActivity(intent);
                break;

            case R.id.txv_about:
                Calendar c = Calendar.getInstance();
                break;

            case R.id.txv_exit:
                makeAboutDialog(view.getId());
                break;
        }
    }

    private void makeAboutDialog(int id) {
        final SpannableString strAbout;
        if (id == R.id.txv_about)
            strAbout = new SpannableString(
                    "This Game has been made by Ming-Yu, Chang, L in UniMelb 2015");
        else
            strAbout = new SpannableString(
                    "Are you sure to exit?");

        Linkify.addLinks(strAbout, Linkify.ALL);

        AlertDialog.Builder aboutDialogBuilder = new AlertDialog.Builder(this);

        aboutDialogBuilder.setMessage(strAbout);
        if (id == R.id.txv_about) {
            aboutDialogBuilder.setPositiveButton("Close",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {

                        }
                    });
        } else {
            aboutDialogBuilder.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                        }
                    });
            aboutDialogBuilder.setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {

                        }
                    });
        }

        AlertDialog aboutDialog = aboutDialogBuilder.create();

        aboutDialog.show();

        // Make the textview clickable. Must be called after show()
        ((TextView) aboutDialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }
}
