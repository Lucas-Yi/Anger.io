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

import com.unimelb.angry_io.Activities.MultipleGameActivity;
import com.unimelb.angry_io.Activities.JoinActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class WifiService {

    Context ctx;
    private static final String TAG = "BluetoothService";

    private static final String GAME_SECURED = "GameSecured";
    private static final String GAME_NORMAL = "GameNormal";

    private static final UUID GAME_SECURED_ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID GAME_NORMAL_ID = UUID.fromString("1e351103-2003-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private Handler mBTHandler;

    private AcceptThread mGameSecuredAcceptThread;
    private AcceptThread mGameNormalAcceptThread;
    private ConnectThread mGameConnectThread;
    private ConnectedThread mGameConnectedThread;
    private int mBTState;

    public static final int BT_STATE_FREE = 0;
    public static final int BT_STATE_LISTEN = 1;
    public static final int BT_STATE_CONNECTING = 2;
    public static final int BT_STATE_CONNECTED = 3;


    public Handler getmBTHandler() {
        return mBTHandler;
    }

    public void setmBTHandler(Handler mBTHandler) {
        this.mBTHandler = mBTHandler;
    }

    public WifiService(Context context, Handler handler) {
        this.ctx = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mBTState = BT_STATE_FREE;
        mBTHandler = handler;
    }

    private String StateToStr(int st){
        if(st == BT_STATE_FREE) return "BT_STATE_FREE";
        else if(st == BT_STATE_LISTEN) return "BT_STATE_LISTEN";
        else if(st == BT_STATE_CONNECTING) return "BT_STATE_CONNECTING";
        else if(st == BT_STATE_CONNECTED) return "BT_STATE_CONNECTED";
        else return "BT_STATE_UNKNOW";
    }
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + StateToStr(mBTState) + " -> " + StateToStr(state));
        mBTState = state;

        mBTHandler.obtainMessage(JoinActivity.INFO_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mBTState;
    }

    public synchronized void start() {
        Log.d(TAG, "BluetoothService start [0]");

        if (mGameConnectThread != null) {
            mGameConnectThread.cancel(); mGameConnectThread = null;}

        if (mGameConnectedThread != null) {
            mGameConnectedThread.cancel(); mGameConnectedThread = null;}

        setState(BT_STATE_LISTEN);

        if (mGameSecuredAcceptThread == null) {
            mGameSecuredAcceptThread = new AcceptThread(true);
            mGameSecuredAcceptThread.start();
        }
        if (mGameNormalAcceptThread == null) {
            mGameNormalAcceptThread = new AcceptThread(true);
            mGameNormalAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "BT connecting, Target: " + device.getName()+" [2]");
        mGameConnectThread = new ConnectThread(device, true);
        mGameConnectThread.start();
        setState(BT_STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "BT connected, Link Type:" + socketType + "[3]");

        if (mGameConnectThread != null) {
            mGameConnectThread.cancel(); mGameConnectThread = null;}

        if (mGameConnectedThread != null) {
            mGameConnectedThread.cancel(); mGameConnectedThread = null;}

        if (mGameSecuredAcceptThread != null) {
            mGameSecuredAcceptThread.cancel();
            mGameSecuredAcceptThread = null;
        }
        if (mGameNormalAcceptThread != null) {
            mGameNormalAcceptThread.cancel();
            mGameNormalAcceptThread = null;
        }

        mGameConnectedThread = new ConnectedThread(socket, socketType);
        mGameConnectedThread.start();

        Message msg = mBTHandler.obtainMessage(JoinActivity.INFO_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(JoinActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mBTHandler.sendMessage(msg);

        setState(BT_STATE_CONNECTED);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop [-1]");

        if (mGameConnectThread != null) {
            mGameConnectThread.cancel();
            mGameConnectThread = null;
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

    public void write(byte[] out) {
        ConnectedThread r;

        synchronized (this) {
            if (mBTState != BT_STATE_CONNECTED) return;
            r = mGameConnectedThread;
        }

        r.write(out);
    }

    private void connectionFailed(IOException e) {
        Message msg = mBTHandler.obtainMessage(JoinActivity.INFO_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(JoinActivity.SHOW_TOAST, "Unable to connect device " + e.getMessage());
        msg.setData(bundle);
        mBTHandler.sendMessage(msg);

        WifiService.this.start();
    }

    private void connectionLost() {
        Message msg = mBTHandler.obtainMessage(JoinActivity.INFO_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(JoinActivity.SHOW_TOAST, "Device connection was lost");
        msg.setData(bundle);
        mBTHandler.sendMessage(msg);

        WifiService.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        @SuppressLint("NewApi")
        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "GameSecured":"GameNormal";

            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(GAME_SECURED,
                            GAME_SECURED_ID);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            GAME_NORMAL, GAME_NORMAL_ID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "Accept start mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            while (mBTState != BT_STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (WifiService.this) {
                        switch (mBTState) {
                            case BT_STATE_LISTEN:
                            case BT_STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case BT_STATE_FREE:
                            case BT_STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                Log.d(TAG, "!!!!!!!!!!!SOCKET CLOSE 320\n");
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

        @SuppressLint("NewApi")
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "GameSecured" : "GameNormal";

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            GAME_SECURED_ID);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            GAME_NORMAL_ID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "Connect start mGameConnectThread SocketType:" + mSocketType);
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

            synchronized (WifiService.this) {
                mGameConnectThread = null;
            }

            connected(mmSocket, mmDevice, mSocketType);
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

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "Connected start mGameConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    mBTHandler.obtainMessage(MultipleGameActivity.INFO_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    WifiService.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                mBTHandler.obtainMessage(JoinActivity.INFO_WRITE, -1, -1, buffer)
                        .sendToTarget();
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
}
