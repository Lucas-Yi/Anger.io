package com.unimelb.angry_io.Activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.unimelb.angry_io.Network.BluetoothService;
import com.unimelb.angry_io.Network.NetListAdapter;
import com.unimelb.angry_io.R;
import com.unimelb.angry_io.System.AngryResource;
import com.unimelb.angry_io.System.CONFIG;

import java.util.ArrayList;
import java.util.Set;

public class JoinActivity extends Activity {

    private static final String TAG = "Angry_io";

    private boolean MASTER = true;
    ListView list;
    ListView listViewAvailable;
    ArrayList<String> arrayListAvailable;
    ArrayList<String> arrayListAvailableMac;
    ArrayList<Integer> arrayListAvailableType;
    ArrayList<BluetoothDevice> AvailableBTList;
    public static final int TYPE_BLUETOOTH = 0;
    public static final int TYPE_WIFI = 1;

    NetListAdapter adapter;
    BluetoothDevice BTDevice;
    ListItemClickedonPaired listItemClickedonPaired;

    BluetoothAdapter bluetoothAdapter = null;
    BluetoothService bluetoothService;

    public final static int REQUEST_BLUETOOTH_ENABLE = 3;

    public static final int INFO_STATE_CHANGE = 1;
    public static final int INFO_READ = 2;
    public static final int INFO_WRITE = 3;
    public static final int INFO_DEVICE_NAME = 4;
    public static final int INFO_TOAST = 5;

    private static final int REQUEST_ENABLE_BT = 3;

    public static final String DEVICE_NAME = "device_name";
    public static final String SHOW_TOAST = "show_toast";

    public Button mRescanButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        listViewAvailable = (ListView) findViewById(R.id.listViewAvailable);
        arrayListAvailable = new ArrayList<String>();
        arrayListAvailableMac = new ArrayList<String>();
        arrayListAvailableType = new ArrayList<Integer>();
        AvailableBTList = new ArrayList<BluetoothDevice>();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CONFIG.player_id = bluetoothAdapter.getAddress();
        //Toast.makeText(JoinActivity.this, "My nickname: " + CONFIG.player_nickname, Toast.LENGTH_LONG).show();
        //Toast.makeText(JoinActivity.this, "My id: " + CONFIG.player_id, Toast.LENGTH_LONG).show();

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);

        listItemClickedonPaired = new ListItemClickedonPaired();


        mRescanButton = (Button) findViewById(R.id.rescanButton);
        mRescanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAvailableDevices();
            }
        });

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            bluetoothService = new BluetoothService(JoinActivity.this, mHandler);
            try {
                bluetoothService.start();
            } catch (Exception ex) {
                // Toast.makeText(getApplicationContext(), "Exception in start " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        getAvailableDevices();
        updateList();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        //this.unregisterReceiver(mReceiver);

        try {
            this.unregisterReceiver(this.mReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "epicReciver is already unregistered");
        }
    }


    @Override
    public synchronized void onResume() {
        super.onResume();

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);

        if (bluetoothService != null) {
            bluetoothService.setmBTHandler(mHandler);
            if (bluetoothService.getState() == BluetoothService.BT_STATE_FREE) {

            }
        }
    }

    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        if (pairedDevice.size() > 0) {
            for (BluetoothDevice device : pairedDevice) {
                if (!arrayListAvailableMac.contains(device.getAddress())) {
                    arrayListAvailable.add(device.getName());
                    arrayListAvailableMac.add(device.getAddress());
                    arrayListAvailableType.add(TYPE_BLUETOOTH);
                    AvailableBTList.add(device);

                    bluetoothService.addAvailableBTDevice(device);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }


    private void getAvailableDevices() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }

    class ListItemClickedonPaired implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (arrayListAvailableType.get(position) == TYPE_BLUETOOTH) {
                BTDevice = AvailableBTList.get(position);
                MASTER = false;
                bluetoothService.notMaster();
                bluetoothService.connect(BTDevice);
            }
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (arrayListAvailableType.get(position) == TYPE_BLUETOOTH) {
                BTDevice = AvailableBTList.get(position);
                MASTER = false;
                bluetoothService.notMaster();
                bluetoothService.connect(BTDevice);
            }
            Toast.makeText(getApplicationContext(), "Long click", Toast.LENGTH_SHORT).show();

            return true;
        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Intent myIntent = new Intent(getApplicationContext(), MultipleGameActivity.class);

            switch (msg.what) {
                case INFO_STATE_CHANGE:
                    switch (msg.arg1) {

                        case BluetoothService.BT_STATE_CONNECTED:
                            ((AngryResource) getApplication()).setBTService(bluetoothService);
                            myIntent.putExtra("MASTER", MASTER);
                            startActivity(myIntent);
                            break;
                        case BluetoothService.BT_STATE_CONNECTING:
                            break;
                        case BluetoothService.BT_STATE_LISTEN:
                            break;
                        case BluetoothService.BT_STATE_FREE:
                            break;
                    }
                    break;
                case INFO_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    break;
                case INFO_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    break;
                case INFO_DEVICE_NAME:

                    break;
                case INFO_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(SHOW_TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(this.mReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "epicReciver is already unregistered");
            //this.mReceiver = null;
        }
        if (bluetoothService != null) bluetoothService.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            Toast.makeText(getApplicationContext(), "REQUEST_BLUETOOTH_ENABLE", Toast.LENGTH_LONG).show();
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else {
                getAvailableDevices();
                updateList();
            }
        }
    }

    public void updateList() {
        adapter = new NetListAdapter(this, arrayListAvailable, arrayListAvailableMac, arrayListAvailableType);
        list = (ListView) findViewById(R.id.list);
        getPairedDevices();
        getAvailableDevices();
        list.setAdapter(adapter);
        list.setOnItemClickListener(listItemClickedonPaired);
        list.setOnItemLongClickListener(listItemClickedonPaired);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (!arrayListAvailableMac.contains(device.getAddress())) {
                    AvailableBTList.add(device);
                    arrayListAvailable.add(device.getName());
                    arrayListAvailableMac.add(device.getAddress());
                    arrayListAvailableType.add(TYPE_BLUETOOTH);
                    adapter.notifyDataSetChanged();

                    bluetoothService.addAvailableBTDevice(device);
                }
                list.setAdapter(null);
                updateList();
            } else if ((BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(), "connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    };
}
