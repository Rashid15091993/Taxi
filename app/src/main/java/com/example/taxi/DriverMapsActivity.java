package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.example.taxi.databinding.ActivityDriverMapsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private ActivityDriverMapsBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker PickUpMarker;

    private Button logoutDriverButton, settingDriverButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogoutDriverStatus = false;
    private DatabaseReference assignedCustomRef, AssignedCustomPositionRef;
    private String driverID, customID = "";

    private ValueEventListener AssignedCustomPositionListener;

    private ImageView callCustom;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView customPhoto;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();

        logoutDriverButton = (Button)findViewById(R.id.driver_logout_button);
        settingDriverButton = (Button)findViewById(R.id.driver_setting_button);

        mapFragment.getMapAsync(this);

        txtName = (TextView)findViewById(R.id.custom_name);
        txtPhone = (TextView)findViewById(R.id.custom_phone);
        customPhoto = (CircleImageView)findViewById(R.id.driver_photo);
        relativeLayout = findViewById(R.id.rell2);

        callCustom = (ImageView)findViewById(R.id.call_to_driver);


        relativeLayout.setVisibility(View.GONE);

        settingDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapsActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);

            }
        });

        logoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogoutDriverStatus = true;
                mAuth.signOut();

                LogoutDriver();
                DisconnectDriver();
            }
        });

        getAssignedCustomRequest();
    }

    private void getAssignedCustomRequest() {
        assignedCustomRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(driverID).child("CustomerRideID");

        assignedCustomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    customID = snapshot.getValue().toString();

                    AssignedCustomPosition();

                    getAssignedCustomInformation();
                }
                else {
                    customID = "";

                    if(PickUpMarker != null) {
                        PickUpMarker.remove();
                    }
                    if(AssignedCustomPositionListener != null) {
                        AssignedCustomPositionRef.removeEventListener(AssignedCustomPositionListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomInformation() {
        relativeLayout.setVisibility(View.VISIBLE);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(customID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);

                    callCustom.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int permissionCheck = ContextCompat.checkSelfPermission(DriverMapsActivity.this, Manifest.permission.CALL_PHONE);

                            if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(
                                        DriverMapsActivity.this, new String[] {Manifest.permission.CALL_PHONE}, 123
                                );
                            }
                            else{
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel " + phone));
                                startActivity(intent);
                            }
                        }
                    });

                    if(snapshot.hasChild("image")) {
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(customPhoto);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void AssignedCustomPosition() {
        AssignedCustomPositionRef = FirebaseDatabase.getInstance().getReference().child("Custom Requests")
                .child(customID).child("l");

        AssignedCustomPositionListener = AssignedCustomPositionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    List<Object> customerPositionMap = (List<Object>)snapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;
                    getAssignedCustomInformation();
                    if (customerPositionMap.get(0) != null) {
                        LocationLat = Double.parseDouble(customerPositionMap.get(0).toString());
                    }

                    if (customerPositionMap.get(1) != null) {
                        LocationLng = Double.parseDouble(customerPositionMap.get(1).toString());
                    }

                    LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);

                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                            .title("Забрать клиента тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(DriverLatLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100000);
        locationRequest.setFastestInterval(100000);
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
    public void onConnectionFailed(@NonNull  ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext() != null) {
            lastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference DriverAvalablityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");

            GeoFire geoFireAvalablity = new GeoFire(DriverAvalablityRef);
            geoFireAvalablity.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));

            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);

            switch (customID) {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvalablity.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvalablity.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }

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

        if(!currentLogoutDriverStatus) {
            DisconnectDriver();

        }
    }

    private void DisconnectDriver() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvalablityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");

        GeoFire geoFire = new GeoFire(DriverAvalablityRef);
        geoFire.removeLocation(userID);
    }

    private void LogoutDriver() {
        Intent welcomeIntent = new Intent(DriverMapsActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }
}