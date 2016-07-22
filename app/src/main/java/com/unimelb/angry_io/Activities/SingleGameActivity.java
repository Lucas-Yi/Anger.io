package com.unimelb.angry_io.Activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Window;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.unimelb.angry_io.R;
import com.unimelb.angry_io.System.AngryResource;
import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

public class SingleGameActivity extends Activity {

    private WorldView worldView;
    private static final String TAG = "SingleGameActivity";
    private GoogleApiClient mGoogleApiClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((AngryResource) getApplication()).setCurrentContext(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_single_game);

        worldView = (WorldView) findViewById(R.id.single_worldView);

        mGoogleApiClient = ((AngryResource) getApplication()).getGoogleApiClient();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();

        IntentFilter intentFilter = new IntentFilter(
                "android.intent.action.GAME");

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                //extract our message from intent
                int score = 0;
                if (intent.getAction().equals("android.intent.action.GAME"))

                    if ((score = intent.getIntExtra("GAME_COMPLETE", 0)) != 0) {
                        // Update LeaderBoard
                        Log.d("Randy", "!!!!!!SSSSSSSSCORE:" + score);
                        if (MenuActivity.mGoogleApiClient.isConnected())
                            Games.Leaderboards.submitScore(MenuActivity.mGoogleApiClient
                                    , CONFIG.LEADERBOARD_ID, (long) score);
                        worldView.stop();
                        finish();
                    }
            }
        };
        //registering our receiver
        this.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worldView.stop();
        try {
            this.unregisterReceiver(this.mReceiver);
        } catch (IllegalArgumentException IAe) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        worldView.stop();
        this.unregisterReceiver(this.mReceiver);
    }

    private BroadcastReceiver mReceiver;


}

