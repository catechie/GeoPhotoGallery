package com.boobastudio.geophotogallery;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String API_KEY_VALUE = "c8580ff4bf2322dbbc581efaf2b5edf0";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final int REQUEST_CHECK_SETTINGS = R.layout.activity_main;
    public static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private volatile Location mLocation;
    private LocationRequest mLocationRequest;
    private volatile boolean mRequestingLocationUpdates = true;
    private static final String LOCATION_KEY = "LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "LAST_UPDATED_TIME_STRING_KEY";
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "LOCATION_UPDATE_KEY";
    private String mLastUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateValuesFromBundle(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API).build();

        LocationSettingsRequest.Builder builder = buildLocationSettingsRequest();
        Log.d(TAG, "onCreate: mRequestLocationUpdate = " + mRequestingLocationUpdates);
        //check if the location setting was turned on
        PendingResult<LocationSettingsResult> locationResult = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        locationResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        mRequestingLocationUpdates = true;
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user location setting dialog
                        try {
                            mRequestingLocationUpdates = false;
                            status.startResolutionForResult(
                                    MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Error:" + e.getMessage());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Toast.makeText(MainActivity.this, "An unresolvable error has occurred and setting change unavailable. App will exit...", Toast.LENGTH_LONG).show();
                        finish();
                        break;
                }
            }
        });

    }

    private LocationSettingsRequest.Builder buildLocationSettingsRequest() {
        mLocationRequest = new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_LOW_POWER);
        return new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();

        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // if (!isDataLoaded)
        Log.d(TAG, "onConnected");
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        /*if(mLocation==null)
            searchByLocation(null);*/

    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "startLocationUpdates: ");
    }

    private void searchByLocation(Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "searchByLocation() and user rejected to turn on location service!");
            return;
        }
        if (location == null)
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        else mLocation = location;
        if (mLocation != null) {

            Uri APIEndpoint = Uri.parse("https://api.flickr.com/services/rest").buildUpon().appendQueryParameter("api_key", API_KEY_VALUE)
                    .appendQueryParameter("method", SEARCH_METHOD)
                    .appendQueryParameter("format", "json").appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("lat", "" + mLocation.getLatitude())
                    .appendQueryParameter("lon", "" + mLocation.getLongitude())
                    .appendQueryParameter("extras", "url_s, url_l")
                    .build();

            String urlString = APIEndpoint.toString();
            SearchPhotosByLocation task = new SearchPhotosByLocation(urlString);
            task.execute();
            Log.d(TAG, "searchByLocation() and locantion is lat: " + mLocation.getLatitude() + " lon: "
                    + mLocation.getLongitude());
        } else {
            Log.d(TAG, "location is null!");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (mRequestingLocationUpdates) {
            searchByLocation(location);
            mRequestingLocationUpdates = false;
        }
        mLocation = location;
        Log.d(TAG, "onLocationChanged()");
    }

    private class SearchPhotosByLocation extends AsyncTask<Void, Integer, Object> {

        String urlString = null;

        public SearchPhotosByLocation(String urlString) {
            this.urlString = urlString;
        }

        @Override
        protected Object doInBackground(Void... params) {
            Log.d(TAG, "SearchPhotoBy location, doInBackground()");
            if (urlString != null)
                return RestfulCallFetchResult.queryJsonResult(urlString);
            else return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            Log.d(TAG, "onPostExecute()");
            String jsonResult = new String((byte[]) o);
            ArrayList<PhotoInfo> listOfPhotos;
            try {
                listOfPhotos = parseItems(new JSONObject(jsonResult));
                Point size = new Point();
                (getWindowManager().getDefaultDisplay()).getSize(size);
                GridView v = (GridView) findViewById(R.id.gridView);
                float spacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        2, getResources().getDisplayMetrics());
                v.setNumColumns(size.x / 480);
                v.setPadding((int) spacing, (int) spacing, (int) spacing, (int) spacing);
                v.setVerticalSpacing((int) spacing);
                v.setHorizontalSpacing((int) spacing);
                if ((v != null) && (listOfPhotos != null)) {
                    v.setAdapter(new GalleryAdapter(listOfPhotos, R.layout.photo_item, MainActivity.this));
                    findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
                    // isDataLoaded = true;
                }

            } catch (IOException e) {
                Log.e(TAG, "Error:" + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Error:" + e.getMessage());
            }

        }
    }


    private ArrayList<PhotoInfo> parseItems(JSONObject jsonResult) throws IOException, JSONException {

        JSONObject photosJsonObject = jsonResult.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        ArrayList<PhotoInfo> items = new ArrayList<>();

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            PhotoInfo item = new PhotoInfo();

            if (photoJsonObject.has("url_s") && photoJsonObject.has("url_l")) {
                item.setUrl_S(photoJsonObject.getString("url_s"));
                item.setUrl_L(photoJsonObject.getString("url_l"));
                items.add(item);
            }

        }
        Log.d(TAG, "parseItems()");
        return items;
    }

    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates()");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mRequestingLocationUpdates = true;
                        Log.d(TAG, "onActivityResult(): user has turned on location setting!");
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(this, "You chose not to turn on Location service, the app exited...", Toast.LENGTH_LONG).show();
                        finish();
                    default:
                        break;
                }
                break;
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                ;
                mRequestingLocationUpdates = true;
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                String mLastUpdate_Time = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            Log.d(TAG, "updateValuesFromBundle: ");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: ");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: ");
        Toast.makeText(this, "An unresolvable error has occurred and geo location service can't be connected. App will be shut down...", Toast.LENGTH_LONG).show();
        finish();
    }

}
