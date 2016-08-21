package br.com.circulove.activities;

import java.io.InputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import br.com.circulove.R;
import br.com.circulove.UsuarioFragment;

public class LoginActivity extends AppCompatActivity
        implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static final int RC_SIGN_IN = 0;
    // Logcat tag
    private static final String TAG = "GooglePlusSignOn";

    // Profile pic image size in pixels
    private static final int PROFILE_PIC_SIZE = 400;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 91;

    private boolean mIntentInProgress;

    private boolean mSignInClicked;

    private ConnectionResult mConnectionResult;

    private SignInButton btnSignIn;
    private Button btnSignOut, btnRevokeAccess;
    private ImageView imgProfilePic;
    private TextView txtName, txtEmail;
    private LinearLayout llProfileLayout;

    Bundle bundleProfileInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.e(TAG, "onCreate!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
        btnSignOut = (Button) findViewById(R.id.btn_sign_out);
        btnRevokeAccess = (Button) findViewById(R.id.btn_revoke_access);

        // Button click listeners
        btnSignIn.setOnClickListener(this);
        btnSignOut.setOnClickListener(this);
        btnRevokeAccess.setOnClickListener(this);


        //Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //startActivityForResult(intent, 1);

        if(verificaConexao(getApplicationContext()))
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            Log.e(TAG, " onCreate End!");
        }
        else
        {
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ll_login);
            relativeLayout.setBackgroundResource(R.drawable.nointernet);
        }
        /*;*/
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch(id) {
            case DIALOG_PLAY_SERVICES_ERROR:
                if (GooglePlayServicesUtil.isUserRecoverableError(mConnectionResult.getErrorCode())) {
                    return GooglePlayServicesUtil.getErrorDialog(
                            mConnectionResult.getErrorCode(),
                            this,
                            RC_SIGN_IN,
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.e(TAG, "Google Play services resolution cancelled");
                                }
                            });
                } else {
                    return new AlertDialog.Builder(this)
                            .setMessage("play_services_error")
                            .setPositiveButton("close",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.e(TAG, "Google Play services error could not be "
                                                    + "resolved: " + mConnectionResult.getErrorCode());
                                        }
                                    }).create();
                }
            default:
                return super.onCreateDialog(id);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        Log.i(TAG, "See error types in http://developer.android.com/reference/com/google/android/gms/common/ConnectionResult.html");
        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this,
                    0).show();
            return;
        }

        if (connectionResult.getErrorCode() == ConnectionResult.SIGN_IN_REQUIRED) {
            Log.i(TAG, "Error type: SIGN_IN_REQUIRED");
        }

        if (connectionResult.getErrorCode() == ConnectionResult.NETWORK_ERROR ) {
            Log.i(TAG, "Error type: NETWORK_ERROR ");
        }

        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // An API requested for GoogleApiClient is not available. The device's current
            // configuration might not be supported with the requested API or a required component
            // may not be installed, such as the Android Wear application. You may need to use a
            // second GoogleApiClient to manage the application's optional APIs.
        } else if (!mIntentInProgress) {
            Log.i(TAG, "not IntentInProgress!");
            // Store the ConnectionResult for later usage
            mConnectionResult = connectionResult;

            if (mSignInClicked) {
                Log.i(TAG, "signInAlreadyClicked! Resolving sign error...");
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }

        Log.i(TAG, "onConnectionFailed end!");
        //TODO: Sign OUT?
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        if(verificaConexao(getApplicationContext()))
        {
            mSignInClicked = false;
            toast("User is connected!");
            // Get user's information
            getProfileInformation();

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtras(bundleProfileInformation);
            startActivity(intent);
        }
        else
        {
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ll_login);
            relativeLayout.setBackgroundResource(R.drawable.nointernet);
        }
        //updateUI(true);
    }

    public void getProfileInformation()
    {
        try
        {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null)
            {
                UsuarioFragment usuarioFragment = new UsuarioFragment();

                Person currentPerson = Plus.PeopleApi
                        .getCurrentPerson(mGoogleApiClient);
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
                txtName.setText(personName);
                txtEmail.setText(email);

                usuarioFragment.setNome(currentPerson.getDisplayName());
                usuarioFragment.setEmail(Plus.AccountApi.getAccountName(mGoogleApiClient));
                usuarioFragment.setImagem(currentPerson.getUrl());

                bundleProfileInformation = new Bundle();
                bundleProfileInformation.putString("setNome", currentPerson.getDisplayName());
                bundleProfileInformation.putString("setEmail", Plus.AccountApi.getAccountName(mGoogleApiClient));
                bundleProfileInformation.putString("setImagem", currentPerson.getImage().getUrl());

                // by default the profile url gives 50x50 px image only
                // we can replace the value with whatever dimension we want by
                // replacing sz=X
                personPhotoUrl = personPhotoUrl.substring(0,
                        personPhotoUrl.length() - 2)
                        + PROFILE_PIC_SIZE;

                new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);

            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // download Google Account profile image, to complete profile
    private class LoadProfileImage extends AsyncTask <String, Void, Bitmap>
    {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("error: ", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public void onConnectionSuspended(int arg0)
    {
        mGoogleApiClient.connect();
        updateUI(false);
    }

    private void updateUI(boolean isSignedIn)
    {
        if (isSignedIn)
        {
            btnSignIn.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);
            btnRevokeAccess.setVisibility(View.VISIBLE);
            llProfileLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            btnSignIn.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.GONE);
            btnRevokeAccess.setVisibility(View.GONE);
            llProfileLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v)
    {
        if (!mGoogleApiClient.isConnecting())
        {
            switch (v.getId()) {
                case R.id.btn_sign_in:
                    // Signin button clicked
                    signInWithGplus();
                    break;
                case R.id.btn_sign_out:
                    // Signout button clicked
                    signOutFromGplus();
                    break;
                case R.id.btn_revoke_access:
                    // Revoke access button clicked
                    revokeGplusAccess();
                    break;
            }
        }
    }

    private void signOutFromGplus()
    {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            updateUI(false);
        }
    }

    private void signInWithGplus()
    {
        if (!mGoogleApiClient.isConnecting()) {
            // We only process button clicks when GoogleApiClient is not transitioning
            // between connected and not connected.
            mSignInClicked = true;
            resolveSignInError();
        }
    }

    /**
     * Revoking access from google
     * */
    private void revokeGplusAccess()
    {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status arg0) {
                            Log.e(TAG, "User access revoked!");
                            mGoogleApiClient.disconnect();
                            mGoogleApiClient.connect();
                            updateUI(false);
                        }

                    });
        }
    }

    protected void onStart()
    {
        if(verificaConexao(getApplicationContext()))
        {
            Log.e(TAG, "onStart!");
            super.onStart();
            mGoogleApiClient.connect();
            Log.e(TAG, "onStart end!");
        }
        else
        {
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ll_login);
            relativeLayout.setBackgroundResource(R.drawable.nointernet);
            super.onStart();
        }
    }

    protected void onStop()
    {
        Log.e(TAG, "onStop!");

        if(verificaConexao(getApplicationContext()))
        {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
        else
        {
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ll_login);
            relativeLayout.setBackgroundResource(R.drawable.nointernet);
            super.onStart();
        }
        super.onStop();
    }

    private void resolveSignInError()
    {
        PendingIntent signInIntent  = mConnectionResult.getResolution();
        if (signInIntent != null) {
            if (mConnectionResult.hasResolution()) {
                try {
                    mIntentInProgress = true;
                    mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
                } catch (IntentSender.SendIntentException e) {
                    mIntentInProgress = false;
                    mGoogleApiClient.connect();
                }
            }
        } else {
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent)
    {
        if (requestCode == RC_SIGN_IN)
        {
            if (responseCode != RESULT_OK)
            {
                mSignInClicked = false;
                toast("ir para outra activity");
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    private void toast(String texto)
    {
        //
        Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
    }

    public static boolean verificaConexao(Context contexto)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        //Pego a conectividade do contexto o qual o metodo foi chamado

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        //Crio o objeto networkInfo que recebe as informacoes da NEtwork

        //System.out.println("NETWORK INFO: "+networkInfo.getSubtypeName());
        if ( (networkInfo != null) && (networkInfo.isConnectedOrConnecting())
                && (networkInfo.isAvailable()) )
        {
            //Se o objeto for nulo ou nao tem conectividade retorna false
            return true;
        }
        else
        {
            return false;
        }
    }
}
