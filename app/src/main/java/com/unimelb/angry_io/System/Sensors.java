package com.unimelb.angry_io.System;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Sensors implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mLightSensor;
    private WorldView worldView;
    private Context context;
    private long lastUpdate;
    private float currentLiSenMaxValue = 1;

    private String TAG = "Sensor";

    public Sensors(WorldView worldView, Context context) {
    	this.worldView = worldView;
    	this.context = context;
    	
    	lastUpdate = System.currentTimeMillis();
    	startSensor();
    }
    
    public void startSensor() {
        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //Randy
        mSensorManager.registerListener(this,
        		mAccelerometer,
                SensorManager.SENSOR_DELAY_GAME);

        mSensorManager.registerListener(this,
                mLightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	// Accuracy is given in onSensorChaged
    }

    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                //Log.d("Randy", "LightSnr: " + event.values[0]+"maxrange:");
                if(event.values[0] > currentLiSenMaxValue) currentLiSenMaxValue = event.values[0];
                if(event.values[0] < (currentLiSenMaxValue / 5)) {
                    worldView.SetScheme(worldView.NIGHT_MODE);
                } else {
                    worldView.SetScheme(worldView.DAY_MODE);
                }
                break;
            case Sensor.TYPE_ACCELEROMETER:
                int accuracy = event.accuracy;
                long timestamp = event.timestamp;
                float values[] = event.values;

                try {
                    long curTime = System.currentTimeMillis();

                    if ((curTime - lastUpdate) > 100) {
                        lastUpdate = curTime;
                        worldView.entityManager.sensorEvent(values[1],values[0]);
                        worldView.setSensorX((-1 * values[1] / 30) * 80/6);
                        worldView.setSensorY(((values[0]) / 30) * 80/6);
                    }
                } catch (Exception e) {
                    //Log.d("Error", e.toString());
                }
                break;
            default:
                break;
        }
    }
}

