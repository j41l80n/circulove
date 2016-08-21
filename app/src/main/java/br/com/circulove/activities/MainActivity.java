package br.com.circulove.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import br.com.circulove.R;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private String url;

    private Location location;
    private LocationRequest  locationRequest;
    private NotificationManager notificationManager;
    private DrawerLayout drawerLayout;
    private GoogleApiClient googleApiClient;
    private LocationManager locationManager;
    private String provider;
    private Criteria criteria;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    double latitude = 0.0;
    double longitude = 0.0;

    NavigationView navigationView;
    RelativeLayout relativeLayout;
    Menu menu;

    private static String SING_UP_URL = "http://104.236.241.111/api/auth/singup";

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private boolean mResolvingError = false;

    private long UPDATE_INTERVAL = 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 1000; /* 2 sec */

    Button button;
    private GoogleMap googleMap;

    Marker marker;
    private String LOG = "LOG ";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    private void setup()
    {
        setupButtons();
        setupDrawerLayout();
        setupRelativeLayout();
        setupNavigationView();
        setupCriteria();
        setupLocationManager();
        setupSupportMapFragment();
        gerarNotificacao();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    setup();
                }
                else
                {
                    finish();
                }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                googleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            toast("error:" + connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    private void setupButtons()
    {
        //
        button = (Button) findViewById(R.id.button);
    }

    private synchronized void setupSupportMapFragment()
    {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.supportMapFragment);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API )
                .build();

        supportMapFragment.getMapAsync(this);
    }

    private void setupNavigationView()
    {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        menu = navigationView.getMenu();

        View headView = navigationView.getHeaderView(0);

        /*((TextView) headView.findViewById(R.id.nome_usuario))
                .setText(getIntent().getExtras().getString("setNome"));
        ((TextView) headView.findViewById(R.id.tipo_conexao))
                .setText(getIntent().getExtras().getString("setEmail"));

        Picasso.with(getApplicationContext())
                .load(getIntent().getExtras().getString("setImagem"))
                .error(R.drawable.ic_account_circle)
                .into(((CircleImageView) headView.findViewById(R.id.profile_image)));

        // navigationView.getMenu().findItem(R.id.nome_usuario).setTitle(b.getString("setNome"));
        //((TextView) findViewById(R.id.nome_usuario)).setText(b.getString("setNome"));*/

    }

    private void setupRelativeLayout()
    {
        relativeLayout = (RelativeLayout) findViewById(R.id.relativelayout_googleMap);
       // relativeLayout.setVisibility(View.GONE);
    }

    private void setupLocationManager()
    {
        //
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.getBestProvider(criteria, true);

        provider = locationManager.getBestProvider(criteria, true);
    }

    private void setupCriteria()
    {
        //configura criteria
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(true);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
        criteria.setBearingRequired(true);

        //API level 9 and up
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_FINE);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_FINE);
        criteria.setBearingAccuracy(Criteria.ACCURACY_LOW);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_MEDIUM);
    }

    public void gerarNotificacao()
    {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setTicker("texto");
        builder.setContentTitle("Circulove");
        //builder.setContentText("Notificação");
        builder.setSmallIcon(R.drawable.icon_gps);
        builder.setContentIntent(pendingIntent);

        NotificationCompat.InboxStyle notificationCompatInboxStyle = new NotificationCompat.InboxStyle();
        String [] notificacoes = new String[]{"Descrição1", "Descrição3"};
        for(int i = 0; i < notificacoes.length; i++)
        {
            notificationCompatInboxStyle.addLine(notificacoes[i]);
        }

        builder.setStyle(notificationCompatInboxStyle);

        Notification notification = builder.build();
        //notification.vibrate = new long[]{100, 180, 100, 180, 100, 200};
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(R.drawable.icon_gps, notification);

        try
        {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            ringtone.play();
        }
        catch (Exception exception)
        {

        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap)
    {
        googleMap.setMyLocationEnabled(true);
        provider = locationManager.getBestProvider(criteria, true);
        location = locationManager.getLastKnownLocation(provider);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude() , location.getLongitude()), 15));

        googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

        mapBegin();

        //polylline(googleMap);
    }

    private void polylline(GoogleMap googleMap)
    {
        //funcao para desenhar linhas no mapa
        googleMap
                .addPolyline((new PolylineOptions())
                        .add(new LatLng(location.getLatitude() , location.getLongitude())
                                ,new LatLng(-5.8417405, -35.1951816)
                                ,new LatLng(-5.8438858,-35.1971014)
                                ,new LatLng(-5.8437991,-35.1998607)
                                ,new LatLng(-5.8419213, -35.2095636))
                        .width(5).color(Color.BLUE)
                        .geodesic(true));
    }

    private void mapBegin()
    {
        //funcao para retornar ao ponto inicial da rota do circular
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(-5.837328 , -35.203224), 15));



                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(14.5f), 1000, null);
                //-5.8374534,-35.2096408
            }
        });
    }

    private static double calculaDistancia(LatLng inicio, LatLng fim)
    {
        //calcula a distancia entre dois pontos no mapa

        float pk = (float) (180.f / Math.PI);

        double a1 = inicio.latitude / pk;
        double a2 = inicio.longitude / pk;
        double b1 = fim.latitude / pk;
        double b2 = fim.longitude / pk;

        double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
        double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
        double t3 = Math.sin(a1)* Math.sin(b1);

        //double tt = Math.acos(t1 + t2 + t3);


        Location locationA = new Location("point A");
        locationA.setLatitude(inicio.latitude);
        locationA.setLongitude(inicio.longitude);

        Location locationB = new Location("point B");
        locationB.setLatitude(fim.latitude);
        locationB.setLongitude(fim.longitude);

        return Math.acos(t1 + t2 + t3);
        //return locationA.distanceTo(locationB);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 1);
        }
        else
        {
            googleApiClient.connect();
        }
    }

    @Override
    public void onStop()
    {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 1);
        }
        else
        {
            // Disconnecting the client invalidates it.
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);

            // only stop if it's connected, otherwise we crash
            if (googleApiClient != null)
            {
                googleApiClient.disconnect();
            }
        }
        super.onStop();
    }

    private void initLocationRequest()
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void starLocationUpdate()
    {
        initLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void stopLocationUpdate()
    {
        //funcao para liberar recursos
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, MainActivity.this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if(googleMap == null)
        {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMapFragment)).getMap();

            if (googleMap !=null)
            {
                cirarPontosMapa(location);
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .anchor(0.5f, 1.0f)
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        //.title(getIntent().getExtras().getString("setNome"))
                        .title("title")
                        .snippet("você está aqui.")
                        .icon(null));

                toast("distancia: " + calculaDistancia(
                        new LatLng(location.getLatitude(), location.getLongitude())
                        ,new LatLng(-5.8437991,-35.1998607)) + " para a ECT");
            }
        }
        //http://fisicainmaos.pe.hu/inserir.php?latitude=teste&longitude=teste

        //LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        //Criteria criteria = new Criteria();

        //String provider = service.getBestProvider(criteria, false);
        //location = service.getLastKnownLocation(provider);
        //-----------------------------------------------
        url = "http://fisicainmaos.pe.hu/inserir.php?latitude=" + location.getLatitude()
                + "&latitude=" + location.getLongitude();
        //new AsyncTaskLocalizacao().execute(url);
        //-----------------------------------------------
    }

    private void cirarPontosMapa(Location location)
    {
        /*Marker kiel = googleMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 1.0f)
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                //.title(getIntent().getExtras().getString("setNome"))
                .title("usuaário")
                .snippet("você está aqui.")
                .icon(null));
*/
        Marker m1 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8417405, -35.1951816))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("setor 2")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));


        Marker m2 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8438858,-35.1971014))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("computação")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));


        Marker m3 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8437991,-35.1998607))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("C&T")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m4 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8421087,-35.2032101))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("CB")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m5 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8409901, -35.2039987))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("CB")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m6 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8393004, -35.2046806))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("NUPLAN")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m7 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8383044, -35.2054156))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("EMUFRN")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m8 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8379269, -35.2062859))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("EMUFRN")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m9 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8382757, -35.2088474))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("teste")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m10 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8359003, -35.2107699))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("DART")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m11 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8386797, -35.2020272))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("Reitoria")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m12 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.83651, -35.2018522))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("DEF")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m13 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8340745, -35.2029969))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("Terminal/Direto")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m14 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8332226, -35.20308))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("Terminal/Inverso")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m15 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.832154, -35.204431))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("Residência Universitária")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m16 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8395165, -35.19548))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("teste")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m17 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.839668, -35.195627))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("teste")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m18 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8372491, -35.1970518))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("7ª BECOMB")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m19 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.832837, -35.202431))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("7ª BECOMB")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m20 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8319872, -35.2042817))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("7ª BECOMB")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m21 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8422462, -35.2059701))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("teste")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));

        Marker m22 = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(-5.8419213, -35.2095636))
                .anchor(0.5f, 0.5f)
                .title("Circulove")
                .snippet("Via Direta")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));
    }

    private void markerAddLocation(double latitude, double longitude, String titulo, String snipet)
    {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions
                .position(new LatLng(latitude, longitude))
                .title(titulo)
                .snippet(snipet)
                .draggable(true);

        marker = googleMap.addMarker(markerOptions);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if(location != null)
        {
            Log.d(LOG, "latitude: " + location.getLatitude());
            Log.d(LOG, "longitude: " + location.getLongitude());
        }

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);

        //atualiza posicao
        starLocationUpdate();

        // Note that this can be NULL if last location isn't already known.
        if (location != null) {
            // Print current location if not null
            Log.d("DEBUG", "current location: " + location.toString());
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        // Begin polling for new location updates.
        startLocationUpdates();

    }

    // Trigger new location updates at interval
    protected void startLocationUpdates()
    {
        // Create the location request
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    //AsyncTask<Parâmetros,Progresso,Resultado>
    public class AsyncTaskLocalizacao extends AsyncTask<String, Location, Location>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            // Showing progress dialog
            //progressDialog = new ProgressDialog(MainActivity.this);
            //progressDialog.setMessage("aguarde ...");
            //progressDialog.setCancelable(false);
            //progressDialog.show();

        }

        @Override
        protected Location doInBackground(String... params)
        {
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;


            try
            {
                URLConnection connection = new URL((params[0])).openConnection();
                //connection.setRequestProperty("Accept-Charset", charset);
                InputStream response = connection.getInputStream();
                return location;
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            //catch (JSONException e)
            //{
            //    e.printStackTrace();
            //}
            finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Location location)
        {

            super.onPostExecute(location);

            //if (progressDialog.isShowing())
            //{
                //progressDialog.dismiss();
            //}

            //localizacao = result;
            //googleMap.addMarker(new MarkerOptions().position(localizacao).title("Marker in Sydney"));
            //googleMap.moveCamera(CameraUpdateFactory.newLatLng(localizacao));
        }
    }

    private void setupDrawerLayout()
    {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public Location getLocation()
    {
        try
        {
            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Log.d("isGPSEnabled: ", "" + isGPSEnabled);
            Log.d("isNetworkEnabled: ", "" + isNetworkEnabled);

            if (!isGPSEnabled && !isNetworkEnabled)
            {
                Log.d("Network", "NO network");
            }
            else
            {
                this.canGetLocation = true;

                if (isNetworkEnabled)
                {
                    if (ActivityCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {

                        return location ;

                    }
                        //locationManager.requestLocationUpdates(
                        //LocationManager.NETWORK_PROVIDER,
                        //MIN_TIME_BW_UPDATES,
                        //MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        //       );
                    Log.d("Network", "Network");
                    if (locationManager != null)
                    {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null)
                        {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                if (isGPSEnabled)
                {
                    if (location == null)
                    {
                       // locationManager.requestLocationUpdates(
                        ////        LocationManager.GPS_PROVIDER,
                        //        MIN_TIME_BW_UPDATES,
                        //        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        //        this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    public static boolean verificaConexao(Context contexto)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        //Pego a conectividade do contexto o qual o metodo foi chamado

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        //Crio o objeto networkInfo que recebe as informacoes da NEtwork

        System.out.println("NETWORK INFO: "+networkInfo.getSubtypeName());
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

    private void toast(String texto)
    {
        //
        Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(googleApiClient != null && googleApiClient.isConnected())
        {
            starLocationUpdate();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if(googleApiClient != null)
        {
            //stopLocationUpdate();
        }
    }
}
