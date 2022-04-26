package com.moringaschool.suggestedlocations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.moringaschool.suggestedlocations.databinding.ActivityMapsBinding;

import java.util.List;

import butterknife.BindView;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private CameraPosition cameraPosition;
    private GoogleMap mMap;
    private LatLng nairobi;

    private FusedLocationProviderClient mFusedLocationProviderClient; // Responsible for fetching the current location of the device.
    private PlacesClient mPlacesClient; // Responsible for typing the suggestions a user will see as they type the address of the place they would like.
    private List<AutocompletePrediction> mPredictionsList; // Responsible for storing the predicted addresses returned by the placesClient.
    private Location mLocation; // Used to store the location variable returned by the locationProviderClient.
    private LocationCallback mLocationCallback; // Used to recall for the current devices location, in the case that it is null.

    @BindView(R.id.materialSearchBar)
    MaterialSearchBar mMaterialSearchBar;
    @BindView(R.id.containedButton)
    Button mButtonSearch;

    private View mMapView;

    private final float DEFAULT_MAP_ZOOM = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        mMapView = mapFragment.getView();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Nairobi and move the camera
        nairobi = new LatLng(-1.286389, 36.817223);
        mMap.addMarker(new MarkerOptions().position(nairobi).title("Marker in Nairobi"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi, 14f));

        Dexter.withActivity(MapsActivity.this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);

                        // Construct a Places Client
                        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
                        mPlacesClient = Places.createClient(MapsActivity.this);

                        // Construct a FusedLocationProviderClient
                        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                        if (mMapView != null && mMapView.findViewById(Integer.parseInt("1")) != null) {
                            View locationButton = ((View) mMapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
                            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                            layoutParams.setMargins(0, 0, 40, 180);
                        }

                        getDeviceLocation();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
                            alertDialog.setCancelable(false);
                            alertDialog.setTitle("Permission Denied");
                            alertDialog.setMessage("In order to proceed you must allow this application to access your location.");
                            alertDialog.setNegativeButton("No", null);
                            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                                }
                            }).show();
                        } else {
                            Toast.makeText(MapsActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    private void getDeviceLocation() {

        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }


            @SuppressLint("MissingPermission") Task<Location> locationTask = mFusedLocationProviderClient.getLastLocation();
            locationTask.addOnCompleteListener(MapsActivity.this, new OnCompleteListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        mLocation = task.getResult();
                        if (mLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), DEFAULT_MAP_ZOOM));
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude())));
                        } else {
                            Log.e(TAG, "Current location is null, using defaults");
                            Log.e(TAG, "Exception: ", task.getException());
                            final LocationRequest locationRequest = LocationRequest.create();
                            locationRequest.setInterval(10000);
                            locationRequest.setFastestInterval(5000);
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                            mLocationCallback = new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    mLocation = locationResult.getLastLocation();
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), DEFAULT_MAP_ZOOM));
                                    mMap.clear();
                                    mMap.addMarker(new MarkerOptions().position(nairobi));

                                }
                            };
                            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
                        }
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: ", e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}