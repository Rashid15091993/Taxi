package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.example.taxi.databinding.ActivityCustomMapsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    private ActivityCustomMapsBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;

    private Button customLogoutButton, settingsButton;
    private Button callTaxiButton;
    private String customID;
    private LatLng CustomerPosition;
    private int radius = 1;
    private  Boolean driverFound = false,requestType = false;
    private  String driverFoundID;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference CustomDataBaseRefer;
    private  DatabaseReference DriversAvailabRef;
    private  DatabaseReference DriversRef;
    private  DatabaseReference DriverLocationRef;

    private  ValueEventListener DriverLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        customLogoutButton = (Button)findViewById(R.id.custom_logout_button);
        callTaxiButton = (Button)findViewById(R.id.custom_order_button);
        settingsButton = (Button)findViewById(R.id.custom_setting_button);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomDataBaseRefer = FirebaseDatabase.getInstance().getReference().child("Custom Requests");
        DriversAvailabRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Intent intent = new Intent(CustomMapsActivity.this, SettingsActivity.class);
              intent.putExtra("type", "Customers");
              startActivity(intent);
            }
        });

        customLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                LogoutCustom();
            }
        });

        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestType){
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriverLocationRef.removeEventListener(DriverLocationRefListener);

                    if(driverFound != null) {
                        DriversRef = FirebaseDatabase.getInstance().getReference().child("Users")
                                .child("Drivers").child(driverFoundID).child("CustomerRideID");
                        DriversRef.removeValue();
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(CustomDataBaseRefer);
                    geoFire.removeLocation(customID);

                    if(PickUpMarker != null) {
                        PickUpMarker.remove();
                    }
                    if(driverMarker != null) {
                        driverMarker.remove();
                    }
                    callTaxiButton.setText("Вызвать такси");
                }
                else {
                    requestType = true;

                    GeoFire geoFire = new GeoFire(CustomDataBaseRefer);
                    geoFire.setLocation(customID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я здесь").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callTaxiButton.setText("Поиск водителя...");
                    getNearbyDrivers();
                }

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);
    }



    @Override
    public void onConnected(@Nullable  Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void LogoutCustom() {
        Intent welcomeIntent = new Intent(CustomMapsActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }

    private void getNearbyDrivers() {
        GeoFire geoFire = new GeoFire(DriversAvailabRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType) {
                    driverFound = true;
                    driverFoundID = key;

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users")
                            .child("Drivers")
                            .child(driverFoundID);

                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customID);
                    DriversRef.updateChildren(driverMap);

                    GetDriverLocation();
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound) {
                    radius = radius + 1;
                    getNearbyDrivers();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GetDriverLocation() {
        DriverLocationRefListener = DriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && requestType) {
                            List<Object> driverLocationMap = (List<Object>)snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            callTaxiButton.setText("Водитель найден!");

                            if (driverLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }

                            if (driverLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }
                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);

                            if(driverMarker != null) {
                                driverMarker.remove();
                            }
                            Location location1 = new Location("");
                            location1.setLatitude(CustomerPosition.latitude);
                            location1.setLongitude(CustomerPosition.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLongitude(DriverLatLng.longitude);

                            float distance = location1.distanceTo(location2);
                            if(distance > 100){
                                callTaxiButton.setText("Ваше такси подъезжает");
                            }
                            else {
                                callTaxiButton.setText("Рассстояние до такси " + String.valueOf(distance));

                                driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                        .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}