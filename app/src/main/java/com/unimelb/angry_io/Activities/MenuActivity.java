package com.unimelb.angry_io.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.unimelb.angry_io.R;
import com.unimelb.angry_io.System.AngryResource;
import com.unimelb.angry_io.System.CONFIG;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public class MenuActivity extends Activity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    boolean isGMS = true;
    Context ctx;
    Button btnConfirm;
    String nickname;
    private static final String TAG = "Angry_io";

    // request codes we use when invoking an external activity
    private static final int RC_UNUSED = 5001;

    // Game centre
    static public GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;

        //Game Centre
        // Create the Google Api Client with access to the Play Games services
        Log.d(TAG, "!!!!!!!If GMS: " + GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()));
        if (isGMS) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                    .build();
            ((AngryResource) getApplication()).setGoogleApiClient(mGoogleApiClient);
        }

        // Nick name
        final Dialog dialog = new Dialog(MenuActivity.this);
        dialog.setContentView(R.layout.nickname);
        dialog.setTitle("Please enter your nickname");

        final EditText etUserNickName = (EditText) dialog.findViewById(R.id.etUserNickName);
        btnConfirm = (Button) dialog.findViewById(R.id.btnConfirm);

        // Set On ClickListener
        btnConfirm.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub

                // get The User name and Password
                //String userName=editTextUserName.getText().toString();
                nickname = etUserNickName.getText().toString();
                Toast.makeText(MenuActivity.this, "User NickName: " + nickname, Toast.LENGTH_LONG).show();
                CONFIG.player_nickname = nickname;
                dialog.dismiss();
            }
        });


        dialog.show();


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //Force device to stay in portrait orientation

        setContentView(R.layout.activity_menu);

        // Setup Views
        TextView txv_play = (TextView) findViewById(R.id.txv_single_player);
        TextView txv_login = (TextView) findViewById(R.id.txv_setup_multiple_players);
        TextView txv_about = (TextView) findViewById(R.id.txv_about);
        TextView txv_exit = (TextView) findViewById(R.id.txv_exit);

        // Setup Click Listener
        txv_play.setOnClickListener(this);
        txv_login.setOnClickListener(this);
        txv_about.setOnClickListener(this);
        txv_exit.setOnClickListener(this);

        if (isGMS) {
            findViewById(R.id.sign_in_button).setOnClickListener(this);
            findViewById(R.id.sign_out_button).setOnClickListener(this);
            findViewById(R.id.show_leaderboards_button).setOnClickListener(this);

            showSignInBar();
        }
    }

    // Shows the "sign in" bar (explanation and button).
    private void showSignInBar() {

        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        findViewById(R.id.show_leaderboards_button).setVisibility(View.GONE);

        ImageView vw = (ImageView) findViewById(R.id.avatar);
        vw.setImageBitmap(null);
        TextView name = (TextView) findViewById(R.id.playerName);
        name.setText("");
        TextView email = (TextView) findViewById((R.id.playerEmail));
        email.setText("");

    }

    // Shows the "sign out" bar (explanation and button).
    private void showSignOutBar() {

        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
        findViewById(R.id.show_leaderboards_button).setVisibility(View.VISIBLE);

        if (mGoogleApiClient.isConnected()) {
            Player player = Games.Players.getCurrentPlayer(mGoogleApiClient);
            String url = player.getIconImageUrl();
            TextView name = (TextView) findViewById(R.id.playerName);
            name.setText(player.getDisplayName());
            if (url != null) {
                ImageView vw = (ImageView) findViewById(R.id.avatar);

                // load the image in the background.
                new DownloadImageTask(vw).execute(url);
            }
        }
    }


    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * AsyncTask to download an image from a URL and set the image to the
     * ImageView that is passed in on the constructor.
     */
    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap mIcon11 = null;
            String url = strings[0];
            try {
                InputStream in = new URL(url).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
                mIcon11 = getCroppedBitmap(mIcon11);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (NullPointerException ne) {
                Log.e(TAG, ne.getMessage());
            }

            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            bmImage.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // For game centre
    @Override
    public void onConnected(Bundle bundle) {
        showSignOutBar();

        if (mGoogleApiClient.isConnected()) {
            Player player = Games.Players.getCurrentPlayer(mGoogleApiClient);
            Log.d(TAG, "~~~~~ pLAYER: " + player.getDisplayName());
        }
    }

    // For game centre
    @Override
    public void onConnectionSuspended(int i) {
        //TODO
    }

    private static int RC_SIGN_IN = 9001;

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInflow = true;
    private boolean mSignInClicked = false;

    // For game centre
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO
        if (mResolvingConnectionFailure) {
            // already resolving
            return;
        }

        // if the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInflow) {
            mAutoStartSignInflow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this,
                        requestCode, resultCode, R.string.signin_failure);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    public void onShowLeaderboardsRequested() {
        if (isSignedIn()) {
            startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(mGoogleApiClient),
                    RC_UNUSED);
        } else {
            BaseGameUtils.makeSimpleDialog(this, getString(R.string.leaderboards_not_available)).show();
        }
    }


    boolean mExplicitSignOut = false;

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.show_leaderboards_button:
                onShowLeaderboardsRequested();
                break;

            case R.id.sign_out_button:
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                showSignInBar();
                mExplicitSignOut = true;
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    try {
                        Games.signOut(mGoogleApiClient);

                    } catch (SecurityException sex) {
                        Log.e(TAG, sex.getMessage());
                    }
                }
                mGoogleApiClient.disconnect();
                break;

            case R.id.sign_in_button:

                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;

            case R.id.txv_single_player:

                CONFIG.mode = CONFIG.SINGLE_MODE;
                Intent Game = new Intent(MenuActivity.this, SingleGameActivity.class);
                startActivity(Game);
                break;

            case R.id.txv_setup_multiple_players:

                CONFIG.mode = CONFIG.MULTI_MODE;
                Intent intent = new Intent(
                        MenuActivity.this,
                        JoinActivity.class);
                intent.putExtra("mainclass", MenuActivity.this.getClass());
                startActivity(intent);
                break;

            case R.id.txv_about:

                makeAboutDialog(view.getId());
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
                    "This Game has been made by " +
                            "\nMing-Yu Chang, " +
                            "\nYu-Fei Yi, " +
                            "\nZhen-Ya Li, " +
                            "\nJian-Yu Li " +
                            "\nin UniMelb 2015");
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

        ((TextView) aboutDialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }
}
