package com.unimelb.angry_io.System;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.unimelb.angry_io.Network.BluetoothService;
import com.unimelb.angry_io.Network.WifiService;

/**
 * Created by randy on 21/09/15.
 */
public class AngryResource extends Application {
    private BluetoothService BTService;
    private WifiService wifiService;
    private Context ctx;
    // Game centre
    private GoogleApiClient mGoogleApiClient = null;

    //Game
    public Context getCurrentContext(){ return ctx;}
    public void setCurrentContext(Context context){
        this.ctx = context;
    }

    //Game Centre
    public void setGoogleApiClient(GoogleApiClient GoogleApiClient){
        this.mGoogleApiClient = GoogleApiClient;
    }
    public GoogleApiClient getGoogleApiClient(){
        return this.mGoogleApiClient;
    }

    //BT
    public BluetoothService getBTService(){ return BTService;}
    public void setBTService(BluetoothService BTService){
        this.BTService = BTService;
    };

    //Wi-Fi
    public WifiService getWifiService(){ return wifiService;}
    public void setWifiService(WifiService wifiService){
        this.wifiService = wifiService;
    };
}
