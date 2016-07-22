package com.unimelb.angry_io.Network;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.unimelb.angry_io.Activities.JoinActivity;
import com.unimelb.angry_io.Cmd.PROTOCOL;
import com.unimelb.angry_io.System.CONFIG;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

public class BluetoothService {

    Context ctx;
    //private static final String TAG = "BluetoothService";
    private static final String TAG = "Angry_io";

    private static final String GAME_SECURED = "GameSecured";
    private static final String GAME_NORMAL = "GameNormal";

    static private LinkedList<String> mLinkedList = new LinkedList<String>();
    private static final ArrayList<UUID> mUuids = new ArrayList<UUID>();

    private final BluetoothAdapter mAdapter;
    private Handler mBTHandler;

    private AcceptThread mGameSecuredAcceptThread;
    private AcceptThread mGameNormalAcceptThread;
    private ConnectThread mGameConnectThread;
    private ConnectedThread mGameConnectedThread;
    private int mBTState;

    private ArrayList AvailableBTList;
    private ArrayList AvailableBTListAddr;
    private ArrayList BTDeviceConnectionList;

    private ArrayList mGameAcceptThreadList;
    static private ArrayList mGameConnectedThreadList;

    public static final int BT_STATE_FREE = 0;
    public static final int BT_STATE_LISTEN = 1;
    public static final int BT_STATE_CONNECTING = 2;
    public static final int BT_STATE_CONNECTED = 3;

    private boolean MASTER = true;

    public void notMaster() {
        this.MASTER = false;
    }

    public Handler getmBTHandler() {
        return mBTHandler;
    }

    public void setmBTHandler(Handler mBTHandler) {
        this.mBTHandler = mBTHandler;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mAdapter;
    }

    public BluetoothService(Context context, Handler handler) {
        this.ctx = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mBTState = BT_STATE_FREE;
        mBTHandler = handler;

        AvailableBTList = new ArrayList<BluetoothDevice>();
        AvailableBTListAddr = new ArrayList<BluetoothDevice>();
        BTDeviceConnectionList = new ArrayList<BTDeviceConnection>();

        mGameAcceptThreadList = new ArrayList<AcceptThread>();
        mGameConnectedThreadList = new ArrayList<ConnectedThread>();

        mUuids.add(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        mUuids.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        mUuids.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        mUuids.add(UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
        mUuids.add(UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
        mUuids.add(UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
        mUuids.add(UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));
    }

    private String StateToStr(int st) {
        if (st == BT_STATE_FREE) return "BT_STATE_FREE";
        else if (st == BT_STATE_LISTEN) return "BT_STATE_LISTEN";
        else if (st == BT_STATE_CONNECTING) return "BT_STATE_CONNECTING";
        else if (st == BT_STATE_CONNECTED) return "BT_STATE_CONNECTED";
        else return "BT_STATE_UNKNOW";
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + StateToStr(mBTState) + " -> " + StateToStr(state));
        mBTState = state;
        mBTHandler.obtainMessage(JoinActivity.INFO_STATE_CHANGE, state, -1).sendToTarget();
    }


    //PUBLIC FUNCTIONS
    public synchronized void RestartAllAvailableBTDevice() {
        //Creat AcceptThread
        for (int i = 0; i < AvailableBTListAddr.size(); i++) {
            AcceptThread mGameAcceptThread;
            mGameAcceptThread = new AcceptThread(true, (BluetoothDevice) AvailableBTListAddr.get(i),
                    AvailableBTListAddr.indexOf(((BluetoothDevice) AvailableBTListAddr.get(i)).getAddress()));
            mGameAcceptThread.start();
            mGameAcceptThreadList.add(mGameAcceptThread);
        }
    }

    public synchronized void RestartAvailableBTDevice(BluetoothDevice device) {
        AcceptThread mGameAcceptThread;
        //Creat AcceptThread
        mGameAcceptThread = new AcceptThread(true, device, AvailableBTListAddr.lastIndexOf(device.getAddress()));
        mGameAcceptThread.start();
        mGameAcceptThreadList.add(mGameAcceptThread);
    }


    public synchronized void addAvailableBTDevice(BluetoothDevice device) {
        if (!AvailableBTListAddr.contains(device.getAddress())) {
            int index;
            AcceptThread mGameAcceptThread;
            //TODO Check if  AcceptThread for this device has already been created
            BTDeviceConnection mBTConnection = new BTDeviceConnection(device);
            AvailableBTList.add(device);
            index = AvailableBTList.indexOf(device);
            AvailableBTListAddr.add(device.getAddress());

            mGameAcceptThread = new AcceptThread(true, device, index);
            mGameAcceptThread.start();
            mGameAcceptThreadList.add(mGameAcceptThread);

            BTDeviceConnectionList.add(mBTConnection);
        }
    }

    public synchronized int getState() {
        return mBTState;
    }

    public synchronized void start() {
        setState(BT_STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "BT connecting, Target: " + device.getName() + " [2]");
        mGameConnectThread = new ConnectThread(device, true);
        mGameConnectThread.start();
        setState(BT_STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType, int index) {
        if (index != -1) {
            ConnectedThread mConnectedThread;
            mConnectedThread = new ConnectedThread(socket, socketType);
            mConnectedThread.start();

            mGameConnectedThreadList.add(mConnectedThread);

            ((BTDeviceConnection) BTDeviceConnectionList.get(index)).ConnectedIndex
                    = mGameConnectedThreadList.indexOf(mConnectedThread);
        }else{
            if (mGameConnectThread != null) {
                mGameConnectThread.cancel();
                mGameConnectThread = null;
            }
            if (mGameConnectedThread != null) {
                mGameConnectedThread.cancel();
                mGameConnectedThread = null;
            }
            //ConnectedThread mGameConnectedThread;
            mGameConnectedThread = new ConnectedThread(socket, socketType);
            mGameConnectedThread.start();
        }
        Message msg = mBTHandler.obtainMessage(JoinActivity.INFO_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(JoinActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mBTHandler.sendMessage(msg);

        setState(BT_STATE_CONNECTED);
    }


    public synchronized void stop() {
        if (mGameConnectThread != null) {
            mGameConnectThread.cancel();
            mGameConnectThread = null;
        }
        for (int i = 0; i < mGameAcceptThreadList.size(); i++) {
            if (((AcceptThread) mGameAcceptThreadList.get(i)) != null) {
                ((AcceptThread) mGameAcceptThreadList.get(i)).cancel();
                mGameAcceptThreadList.remove(i);
            }
        }
        for (int i = 0; i < mGameConnectedThreadList.size(); i++) {
            if (((ConnectedThread) mGameConnectedThreadList.get(i)) != null) {
                ((ConnectedThread) mGameConnectedThreadList.get(i)).cancel();
                mGameConnectedThreadList.remove(i);
            }
        }
        if (mGameConnectedThread != null) {
            mGameConnectedThread.cancel();
            mGameConnectedThread = null;
        }
        if (mGameSecuredAcceptThread != null) {
            mGameSecuredAcceptThread.cancel();
            mGameSecuredAcceptThread = null;
        }

        if (mGameNormalAcceptThread != null) {
            mGameNormalAcceptThread.cancel();
            mGameNormalAcceptThread = null;
        }
        setState(BT_STATE_FREE);
    }


    public synchronized void stop(BluetoothDevice device) {
        int index = AvailableBTList.indexOf(device);
        if (mGameConnectThread != null) {
            mGameConnectThread.cancel();
            mGameConnectThread = null;
        }
        if (mGameConnectedThreadList.size() > 0) {
            BTDeviceConnection tmp = (BTDeviceConnection) BTDeviceConnectionList.get(index);
            ConnectedThread killConnectedThread = (ConnectedThread) mGameConnectedThreadList.get(tmp.ConnectedIndex);
            if (killConnectedThread != null) {
                killConnectedThread.cancel();
                mGameConnectedThreadList.remove(index);
            }
        }
    }

    public void write(byte[] out) {
        ConnectedThread r = null;
        synchronized (this) {
            if (mBTState != BT_STATE_CONNECTED) return;
            if (!MASTER) {
                r = mGameConnectedThread;
            }
        }
        if (!MASTER) {
            r.write(out);
        } else {
            for (int i = 0; i < mGameConnectedThreadList.size(); i++) {
                ((ConnectedThread) mGameConnectedThreadList.get(i)).write(out);
            }
        }
    }

    private void connectionFailed(IOException e) {
        Message msg = mBTHandler.obtainMessage(JoinActivity.INFO_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(JoinActivity.SHOW_TOAST, "Unable to connect device " + e.getMessage());
        msg.setData(bundle);
        mBTHandler.sendMessage(msg);

        BluetoothService.this.start();
    }

    private void connectionLost() {
        Message msg = mBTHandler.obtainMessage(JoinActivity.INFO_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(JoinActivity.SHOW_TOAST, "Device connection was lost");
        msg.setData(bundle);
        mBTHandler.sendMessage(msg);
        BluetoothService.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;
        private UUID mUUID;
        private BluetoothDevice mDevice;
        private int index;

        @SuppressLint("NewApi")
        public AcceptThread(boolean secure, int connNumber) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "GameSecured" : "GameNormal";
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(GAME_SECURED,
                            mUuids.get(connNumber));
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            GAME_NORMAL, mUuids.get(connNumber));
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }


        @SuppressLint("NewApi")
        public AcceptThread(boolean secure, BluetoothDevice device, int mIndex) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "GameSecured" : "GameNormal";
            mDevice = device;
            this.index = mIndex;

            mUUID = UUID.fromString("e0917680-d427-11e4-8830-" + device.getAddress().replace(":", ""));
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(GAME_SECURED,
                            mUUID);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            GAME_NORMAL, mUUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);
            BluetoothSocket socket;

            while (mBTState != BT_STATE_CONNECTED) {
                try {
                    Log.d(TAG, "!!!!2222. Before Accept:");
                    socket = mmServerSocket.accept();
                    if (mDevice != null)
                        Log.d(TAG, "!!!!3333. AcceptServerSocket from " + mDevice.getName() + " \nUUID: " + mUUID);
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mBTState) {
                            case BT_STATE_LISTEN:
                            case BT_STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType, index);
                                break;
                            case BT_STATE_FREE:
                            case BT_STATE_CONNECTED:
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        private UUID mUUID;

        @SuppressLint("NewApi")
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "GameSecured" : "GameNormal";
            mUUID = UUID.fromString("e0917680-d427-11e4-8830-" + mAdapter.getAddress().replace(":", ""));
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            mUUID);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            mUUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread" + mSocketType);
            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed(e);
                return;
            }

            synchronized (BluetoothService.this) {
                mGameConnectThread = null;
            }

            connected(mmSocket, mmDevice, mSocketType, -1);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            //TODO Implement IN/OUT stream list
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "Connected start mGameConnectedThread");
            byte[] buffer = new byte[CONFIG.NW_FRAGMENT_SIZE + CONFIG.NW_FRA_DELIMITER.getBytes().length];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String raw_message = new String(buffer);
                    if (raw_message.contains(CONFIG.NW_LOG_DELIMITER)) {
                    }
                    else {
                        while (!raw_message.contains(PROTOCOL.LOG_DELIMITER)) {
                            raw_message = raw_message.split(CONFIG.NW_FRA_DELIMITER)[0];
                            mmInStream.read(buffer);
                            String raw_next = new String(buffer);
                            if (raw_next.contains(CONFIG.NW_FRA_DELIMITER)) {
                                raw_next = raw_next.split(CONFIG.NW_FRA_DELIMITER)[0];
                            } else {
                                Log.d("Protocol", "NO Fragment !!!! ");
                            }
                            raw_message = raw_message + raw_next;
                        }
                    }
                    String message = raw_message.split(PROTOCOL.LOG_DELIMITER)[0];
                    mLinkedList.add(message);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    BluetoothService.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class BTDeviceConnection {
        BluetoothDevice mDevice;
        public int ConnectedIndex;
        BTDeviceConnection(BluetoothDevice device) {
            this.mDevice = device;
        }
    }

    static public int getNumConnected() {
        return mGameConnectedThreadList.size();
    }


    static public boolean isRxMsg() {
        boolean bIsRxMsg = !mLinkedList.isEmpty();
        return bIsRxMsg;
    }

    static public String getRxMsg() {
        String msg;
        if (!mLinkedList.isEmpty()) {
            msg = mLinkedList.poll();
            return msg;
        } else return null;
    }
}
