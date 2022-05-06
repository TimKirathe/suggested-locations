package com.moringaschool.suggestedlocations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

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
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import com.moringaschool.suggestedlocations.databinding.ActivityMapsBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

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
        ButterKnife.bind(this);

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

                        // Deals with the autocomplete options offered by the placesClient
                        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

                        mMaterialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                            @Override
                            public void onSearchStateChanged(boolean enabled) {

                            }

                            @Override
                            public void onSearchConfirmed(CharSequence text) {
                                startSearch(text.toString(), true, null, true);
                            }

                            @Override
                            public void onButtonClicked(int buttonCode) {
                                if(buttonCode == MaterialSearchBar.BUTTON_NAVIGATION) {
                                    // Opening or closing a navigation drawer.
                                } else if(buttonCode == MaterialSearchBar.BUTTON_BACK) {
                                    mMaterialSearchBar.closeSearch();
                                }
                            }
                        });

                        mMaterialSearchBar.addTextChangeListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                                                                                        .setCountry("ke")
                                                                                        .setSessionToken(token)
                                                                                        .setQuery(charSequence.toString())
                                                                                        .build();
                                mPlacesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                                    @Override
                                    public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                      if(task.isSuccessful()) {
                                        FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                        if(predictionsResponse != null) {
                                            mPredictionsList = predictionsResponse.getAutocompletePredictions();
                                            List<String> suggestionsList = new ArrayList<>();
                                            for (int i = 0; i < mPredictionsList.size(); i++) {
                                                AutocompletePrediction autocompletePrediction = mPredictionsList.get(i);
                                                suggestionsList.add(autocompletePrediction.getFullText(null).toString());
                                            }
                                            mMaterialSearchBar.updateLastSuggestions(suggestionsList);
                                            if(!mMaterialSearchBar.isSuggestionsVisible()) {
                                                mMaterialSearchBar.showSuggestionsList();
                                            }
                                        }
                                      } else {
                                          Log.e(TAG, "Prediction fetching request unsuccessful.");
                                      }
                                    }
                                });
                            }

                            @Override
                            public void afterTextChanged(Editable editable) {

                            }
                        });

                        mMaterialSearchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
                            @Override
                            public void OnItemClickListener(int position, View v) {
                                if(position >= mPredictionsList.size()) {
                                    return;
                                }
                                AutocompletePrediction prediction = mPredictionsList.get(position);
                                String suggestion = mMaterialSearchBar.getLastSuggestions().get(position).toString();
                                mMaterialSearchBar.setText(suggestion);

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mMaterialSearchBar.clearSuggestions();
                                    }
                                }, 1000);
                                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(mMaterialSearchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                                }
                                String placeId = prediction.getPlaceId();
                                List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG);

                                FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, fields).build();
                                mPlacesClient.fetchPlace(placeRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                                    @Override
                                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                                        Place place = fetchPlaceResponse.getPlace();
                                        Log.e(TAG, "Place Found: " + place.getName());
                                        LatLng latLngOfPlace = place.getLatLng();
                                        if (latLngOfPlace != null) {
                                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOfPlace, DEFAULT_MAP_ZOOM));
                                            mMap.clear();
                                            mMap.addMarker(new MarkerOptions().position(latLngOfPlace));
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        if (e instanceof ApiException) {
                                            ApiException exception = (ApiException) e;
                                            exception.printStackTrace();
                                            int status = exception.getStatusCode();
                                            Log.e(TAG, "Error: " + exception.getMessage());
                                            Log.e(TAG, "Status Code: " + status);
                                        }

                                    }
                                });
                            }

                            @Override
                            public void OnItemDeleteListener(int position, View v) {

                            }

                        });

                        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                            @Override
                            public boolean onMyLocationButtonClick() {
                                if(mMaterialSearchBar.isSuggestionsVisible()) {
                                    mMaterialSearchBar.clearSuggestions();
                                }
                                if(mMaterialSearchBar.isSearchOpened()) {
                                    mMaterialSearchBar.closeSearch();
                                }
                                return false;
                            }
                        });

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
                                    mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                                }
                            };
                            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "Unable to get your last location.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}