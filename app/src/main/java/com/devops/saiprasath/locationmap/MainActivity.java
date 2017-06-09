package com.devops.saiprasath.locationmapmyindia;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.mmi.LicenceManager;
import com.mmi.MapView;
import com.mmi.MapmyIndiaMapView;
import com.mmi.apis.distance.DistanceManager;
import com.mmi.apis.routing.DirectionManager;
import com.mmi.layers.PathOverlay;
import com.mmi.layers.UserLocationOverlay;
import com.mmi.layers.location.OnLocationClickListener;
import com.mmi.util.GeoPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements LocationListener {
    MapView mapView = null;
    MapmyIndiaMapView mapmyIndiaMapView = null;
    final String RESTapiToken = "5itkt4uu414wyj4vhppg6tf1cmyopxq6";
    final String MapApiToken = "rwplzvrfwunuer37s4nyweblcllduoil";
    DistanceManager.VehicleType vtype = DistanceManager.VehicleType.TAXI;
    DistanceManager.RouteType rtype = DistanceManager.RouteType.QUICKEST;
    DistanceManager.Avoid avoids = DistanceManager.Avoid.UNPAVED_ROADS;
    int advices = DirectionManager.Advises.YES.ordinal();
    boolean alternatives = false;
    Location mLocation;
    LocationManager locationManager;
    ArrayList<Double> latitudes = new ArrayList<>();
    ArrayList<Double> longitudes = new ArrayList<>();
    ArrayList<GeoPoint> geopoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LicenceManager.getInstance().setMapSDKKey(MapApiToken);
        LicenceManager.getInstance().setRestAPIKey(RESTapiToken);
        locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);
        mLocation = getCurLocation();
        mapmyIndiaMapView = (MapmyIndiaMapView)findViewById(R.id.mmiView);
        mapView = mapmyIndiaMapView.getMapView();
        new mmiThread().execute("http://apis.mapmyindia.com/advancedmaps/v1/"+RESTapiToken+"/route?start="+mLocation.getLatitude()+","+mLocation.getLongitude()+"&destination=12.8342,79.7036&alternatives="+alternatives+"&with_advices="+advices+"");

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location!=null) {
            this.mLocation = location;
        }
        mLocation = getCurLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class mmiThread extends AsyncTask<String,Void,String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mapView = mapmyIndiaMapView.getMapView();
        }

        @Override
        protected String doInBackground(String... params) {
            String line = null;
            try {
                URL url = new URL(params[0]);
                System.out.println(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(30000);
                httpURLConnection.setAllowUserInteraction(true);
                httpURLConnection.connect();
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode!=200){
                    System.out.println("The http connection could not be sustained");
                }
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                while ((line=bufferedReader.readLine())!=null){
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            }
            catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            System.out.println(s);
            try {
                JSONObject root = new JSONObject(s);
                String responseCode = root.getString("responseCode");
                if (Integer.parseInt(responseCode)==200){
                    JSONObject results = root.getJSONObject("results");
                    JSONArray trips = results.getJSONArray("trips");
                    JSONArray advices =  ((JSONObject) trips.get(0)).getJSONArray("advices");
                    for (int i=0;i<advices.length();i++){
                        JSONObject pt = ((JSONObject)advices.get(i)).getJSONObject("pt");
                        double lat = pt.getDouble("lat");
                        double lng = pt.getDouble("lng");
                        latitudes.add(i,lat);
                        longitudes.add(i,lng);
                        System.out.println(lat+"\t"+lng);
                    }
                }
                else{
                    System.out.println("Could not get error response");
                    System.out.println("Good Bye");
                }
                if (latitudes.size()==longitudes.size()) {
                    for (int i = 0; i < latitudes.size(); i++) {
                        geopoints.add(i, new GeoPoint(latitudes.get(i), longitudes.get(i)));
                    }
                }
                getLocationOverlay();
                getPathOverlay();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    private Location getCurLocation() {
        Location tempLoc=null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1);
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500*60,5F, this);
                if (locationManager != null) {
                    tempLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    System.out.println(tempLoc.getLatitude());
                    System.out.println(tempLoc.getLongitude());
                }
            }
            return tempLoc;
        }
        catch (Exception e){e.printStackTrace();
        return null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getLocationOverlay();
        getPathOverlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLocationOverlay();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onDetach();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public void getPathOverlay()
    {
        PathOverlay pathOverlay = new PathOverlay(MainActivity.this);
        pathOverlay.setPoints(geopoints);
        pathOverlay.setWidth(7);
        pathOverlay.setColor(R.drawable.gradientpath);
        mapView.setBounds(geopoints);

        mapView.setMultiTouchControls(true);
        mapView.setFocusableInTouchMode(true);
        mapView.setHapticFeedbackEnabled(false);
        mapView.getOverlays().add(pathOverlay);
        mapView.computeScroll();
        mapView.invalidate();
    }
    public void getLocationOverlay(){
        UserLocationOverlay userLocationOverlay = new UserLocationOverlay(mapView);
        userLocationOverlay.setCurrentLocationResId(R.drawable.rsz_icon);
        userLocationOverlay.enableMyLocation();
        userLocationOverlay.setOnLocationClickListener(new OnLocationClickListener() {
            @Override
            public void OnSingleTapConfirmed() {
                Toast.makeText(MainActivity.this,"You are here",Toast.LENGTH_LONG).show();
            }
        });
        mapView.getOverlays().add(userLocationOverlay);
        mapView.invalidate();
    }
}
