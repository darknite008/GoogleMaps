package com.om.googlemaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.om.googlemaps.model.LatitudeLongitude;

import java.util.ArrayList;
import java.util.List;

public class SearchPlaces extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    private GoogleMap mMap;
    private AutoCompleteTextView autoCompleteTextView;
    private Button button;
    private List<LatitudeLongitude> latitudeLongitudes;
    Marker marker;
    CameraUpdate center, zoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_search_places);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );

        autoCompleteTextView = findViewById( R.id.autoText );
        button = findViewById( R.id.btnser );
        fillArrayListAndSetAdapter();
        button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty( autoCompleteTextView.getText().toString() )) {
                    autoCompleteTextView.setError( "Please provide location" );
                    return;
                }
                int position = SearchArrayList( autoCompleteTextView.getText().toString() );
                if (position > -1) {
                    loadMap( position );
                } else {
                    Toast.makeText( SearchPlaces.this, "Location not found"
                            + autoCompleteTextView.getText().toString() + position, Toast.LENGTH_SHORT ).show();
                }
            }
        } );
    }
    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates( mGoogleApiClient,  SearchPlaces.this );
        }
    }
    private void loadMap(int position) {
        if (marker != null) {
            marker.remove();
        }
        double latitude = latitudeLongitudes.get( position ).getLat();
        double longitude = latitudeLongitudes.get( position ).getLon();
        String markers = latitudeLongitudes.get( position ).getMarker();
        center = CameraUpdateFactory.newLatLng( new LatLng( latitude, longitude ) );
        zoom = CameraUpdateFactory.zoomTo( 17 );
        marker = mMap.addMarker( new MarkerOptions().position( new LatLng( latitude, longitude ) ).title( markers ) );
        mMap.moveCamera( center );
        mMap.animateCamera( zoom );
        mMap.getUiSettings().setZoomControlsEnabled( true );

    }

    private int SearchArrayList(String name) {
        for (int i = 0; i < latitudeLongitudes.size(); i++) {
            if (latitudeLongitudes.get( i ).getMarker().contains( name )) {
                return i;
            }
        }
        return -1;
    }

    private void fillArrayListAndSetAdapter() {
        latitudeLongitudes = new ArrayList<>();
        latitudeLongitudes.add( new LatitudeLongitude( 27.7046496, 85.3304344, "Global Bank" ) );
        latitudeLongitudes.add( new LatitudeLongitude( 27.7134481, 85.33241992, "Nagpokhari" ) );
        latitudeLongitudes.add( new LatitudeLongitude( 27.7127827, 85.3265391, "Hotel Brihaspati" ) );
        latitudeLongitudes.add( new LatitudeLongitude( 27.7052354, 85.3300396, "Softwarica College" ) );
        latitudeLongitudes.add( new LatitudeLongitude( 27.7065389, 85.3075012, "Electronic Gadgets" ) );

        String[] data = new String[latitudeLongitudes.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = latitudeLongitudes.get( i ).getMarker();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                SearchPlaces.this,
                android.R.layout.simple_list_item_1,
                data
        );
        autoCompleteTextView.setAdapter( adapter );
        autoCompleteTextView.setThreshold( 1 );

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        center = CameraUpdateFactory.newLatLng( new LatLng( 27.7172453, 85.3239605 ) );
        zoom = CameraUpdateFactory.zoomTo( 15 );
        // mMap.moveCamera( center );
        mMap.animateCamera( zoom );
        mMap.getUiSettings().setZoomControlsEnabled( true );
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission( this,
                    Manifest.permission.ACCESS_FINE_LOCATION )
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mMap.setMyLocationEnabled( true );

            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {

            buildGoogleApiClient();
            mMap.setMyLocationEnabled( true );
        }
    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks( this )
                .addOnConnectionFailedListener( this )
                .addApi( LocationServices.API )
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval( 1000 );
        mLocationRequest.setFastestInterval( 1000 );
        mLocationRequest.setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );
        if (ContextCompat.checkSelfPermission( this,
                Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates( mGoogleApiClient, mLocationRequest, SearchPlaces.this );
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position( latLng );
        markerOptions.title( "Current Position" );
        markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_MAGENTA ) );
        mCurrLocationMarker = mMap.addMarker( markerOptions );

        //move map camera
        //mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( latLng, 11 ) );
        //  mMap.moveCamera( center );
        mMap.animateCamera( zoom );
        mMap.getUiSettings().setZoomControlsEnabled( true );

    }
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION )
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale( this,
                    Manifest.permission.ACCESS_FINE_LOCATION )) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder( this )
                        .setTitle( "Location Permission Needed" )
                        .setMessage( "This app needs the Location permission, please accept to use location functionality" )
                        .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions( SearchPlaces.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        } )
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions( this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission( this,
                            Manifest.permission.ACCESS_FINE_LOCATION )
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled( true );
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText( this, "permission denied", Toast.LENGTH_LONG ).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
