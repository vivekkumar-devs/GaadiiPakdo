package vivek.harman.gaadiipakdo;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
public class Driver_Maps_Activity extends AppCompatActivity {
    private MapView map;
    private View waitingLayout;
    private boolean isLoggingOut = false;

    private DatabaseReference sessionRef;
    private ValueEventListener sessionListener;

    private TextView tvStatus;
    // =============================
    // DRIVER DETAILS
    // =============================
    private TextView txtDriverName, txtVehicleNumber, txtCapacity;
    private LocationManager locationManager;
    private Marker driverMarker;
    private DatabaseReference locationRef;
    private String uid;
    private static final String TAG =
            "DRIVER_FIREBASE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext()
                )
        );
        setContentView(R.layout.activity_driver_maps);
        // =============================
        // STATUS BAR
        // =============================
        getWindow().setStatusBarColor(
                Color.WHITE
        );
        getWindow().getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );





        View root = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    0,
                    0,
                    0,
                    systemBars.bottom
            );

            return insets;
        });


        // =============================
        // VIEWS
        // =============================

        map = findViewById(R.id.mapdriver);
        waitingLayout = findViewById(R.id.waitingLayout);

        tvStatus = findViewById(R.id.tvStatus);

        txtDriverName =
                findViewById(R.id.txtDriverName);

        txtVehicleNumber =
                findViewById(R.id.txtVehicle);

        txtCapacity =
                findViewById(R.id.txtCapacity);

        ImageButton bbtnBack =
                findViewById(R.id.bbtnBack);

        ImageButton btonLogout =
                findViewById(R.id.btonLogout);


        // =============================
        // BACK BUTTON
        // =============================

        bbtnBack.setOnClickListener(v ->
                finish()
        );
        // =============================
        // LOGOUT
        // =============================

        if (btonLogout != null) {

            btonLogout.setOnClickListener(v -> {

                isLoggingOut = true;

                locationManager.removeUpdates(locationListener);

                DatabaseReference driverRef =
                        locationRef.child(uid);

                driverRef.child("lat").removeValue();
                driverRef.child("lng").removeValue();

                driverRef.child("isOnline")
                        .setValue(false)
                        .addOnSuccessListener(unused -> {

                            getSharedPreferences(
                                    "app",
                                    MODE_PRIVATE
                            ).edit().clear().apply();

                            Intent intent =
                                    new Intent(
                                            Driver_Maps_Activity.this,
                                            Driver_Login_Activity.class
                                    );

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();
                        });
            });
        }

        // =============================
        // MAP SETUP
        // =============================

        map.setTileSource(
                TileSourceFactory.MAPNIK
        );

        map.setMultiTouchControls(true);

        map.getController().setZoom(15.0);

        // =============================
        // LOCATION MANAGER
        // =============================

        locationManager =
                (LocationManager) getSystemService(
                        Context.LOCATION_SERVICE
                );

        // =============================
        // GET DRIVER ID
        // =============================

        uid = getSharedPreferences(
                "app",
                MODE_PRIVATE
        ).getString(
                "driverId",
                ""
        );

        if (uid == null || uid.isEmpty()) {

            Toast.makeText(
                    this,
                    "Session Expired",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        // =============================
        // FIREBASE
        // =============================

        locationRef = FirebaseDatabase
                .getInstance()
                .getReference("Drivers");
        startSessionListener();

        monitorRideStatus();
        // =============================
        // ONLINE STATUS
        // =============================

        setDriverOnline(true);

        // =============================
        // LOAD DRIVER DETAILS
        // =============================

        loadDriverDetails();
        showWaitingScreen();
        showWaitingScreen();
        // =============================
        // CHECK LOCATION PERMISSION
        // =============================

        checkPermissions();
    }

    private void monitorRideStatus() {

        locationRef.child(uid)
                .child("hasPassenger")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Boolean hasPassenger =
                                snapshot.getValue(Boolean.class);

                        if (hasPassenger != null && hasPassenger) {

                            showMapScreen();

                        } else {

                            showWaitingScreen();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

        private void showWaitingScreen() {

            map.setVisibility(View.GONE);

            if (waitingLayout != null) {
                waitingLayout.setVisibility(View.VISIBLE);
            }

            tvStatus.setText("Waiting for Passenger");
        }

        private void showMapScreen() {

            if (waitingLayout != null) {
                waitingLayout.setVisibility(View.GONE);
            }

            map.setVisibility(View.VISIBLE);

            tvStatus.setText("Live location updating");
        }



    // =============================
    // ONLINE STATUS
    // =============================

    private void setDriverOnline(
            boolean status
    ) {

        if (uid == null) return;

        locationRef.child(uid)
                .child("isOnline")
                .setValue(status)
                .addOnSuccessListener(unused ->
                        Log.d(TAG,
                                "isOnline updated = " + status))
                .addOnFailureListener(e ->
                        Log.e(TAG,
                                "Online status failed: "
                                        + e.getMessage()));
    }

    // =============================
    // LOAD DRIVER DETAILS
    // =============================

    private void loadDriverDetails() {

        if (uid == null) return;

        locationRef.child(uid)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (snapshot.exists()) {

                                    String name =
                                            snapshot.child("name")
                                                    .getValue(String.class);

                                    String vehicle =
                                            snapshot.child("vehicleNumber")
                                                    .getValue(String.class);

                                    Integer capacity =
                                            snapshot.child("capacity")
                                                    .getValue(Integer.class);

                                    txtDriverName.setText("" + (name != null ? name : "--"));

                                    txtVehicleNumber.setText(
                                            "" + (vehicle != null ? vehicle : "--"));

                                    txtCapacity.setText("" + (capacity != null ? capacity : "--"));
                                }
                            }
                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                tvStatus.setText(
                                        "Failed to load profile"
                                );
                            }
                        });
    }
    private void startSessionListener() {

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        "app",
                        MODE_PRIVATE
                );

        String driverKey =
                prefs.getString(
                        "driverKey",
                        ""
                );

        if (driverKey.isEmpty()) {
            return;
        }

        sessionRef =
                FirebaseDatabase.getInstance()
                        .getReference("Drivers")
                        .child(driverKey)
                        .child("sessionToken");

        sessionListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                String serverToken =
                        snapshot.getValue(
                                String.class
                        );

                String localToken =
                        getSharedPreferences(
                                "app",
                                MODE_PRIVATE
                        ).getString(
                                "sessionToken",
                                ""
                        );

                if (serverToken == null ||
                        serverToken.isEmpty()) {
                    return;
                }

                if (localToken == null ||
                        localToken.isEmpty()) {
                    return;
                }

                if (!serverToken.equals(
                        localToken
                )) {

                    isLoggingOut = true;

                    Toast.makeText(
                            Driver_Maps_Activity.this,
                            "Account logged in on another device",
                            Toast.LENGTH_LONG
                    ).show();

                    getSharedPreferences(
                            "app",
                            MODE_PRIVATE
                    )
                            .edit()
                            .clear()
                            .apply();

                    Intent intent =
                            new Intent(
                                    Driver_Maps_Activity.this,
                                    Driver_Login_Activity.class
                            );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);

                    finish();
                }
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
            }
        };

        sessionRef.addValueEventListener(
                sessionListener
        );
    }

    // =============================
    // LOCATION PERMISSION
    // =============================

    private void checkPermissions() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    1
            );

        } else {

            startLocationUpdates();
        }
    }

    // =============================
    // LOCATION UPDATES
    // =============================

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Location lastLocation =
                locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );

        if (lastLocation != null) {

            updateLocation(lastLocation);
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,
                2,
                locationListener
        );

        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000,
                2,
                locationListener
        );
    }

    // =============================
    // LOCATION LISTENER
    // =============================

    private final LocationListener locationListener =
            location -> {

                if (location != null) {

                    updateLocation(location);
                }
            };

    // =============================
    // UPDATE LOCATION
    // =============================

    private void updateLocation(
            Location location
    ) {
        if (isLoggingOut) {
            return;
        }

        double lat =
                location.getLatitude();

        double lng =
                location.getLongitude();

        GeoPoint current =
                new GeoPoint(lat, lng);

        map.getController()
                .animateTo(current);

        if (driverMarker == null) {

            driverMarker =
                    new Marker(map);

            driverMarker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
            );

            Drawable icon =
                    ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_cars
                    );

            driverMarker.setIcon(icon);

            map.getOverlays()
                    .add(driverMarker);
        }

        driverMarker.setPosition(current);

        HashMap<String, Object> mapData =
                new HashMap<>();

        mapData.put("lat", lat);
        mapData.put("lng", lng);

        mapData.put("isOnline", true);

        mapData.put(
                "lastUpdated",
                System.currentTimeMillis()
        );

        locationRef.child(uid)
                .updateChildren(mapData)
                .addOnSuccessListener(aVoid -> {

                    tvStatus.setText(
                            "Live location updating"
                    );

                    Log.d(
                            TAG,
                            "Location sent"
                    );
                })
                .addOnFailureListener(e -> {

                    tvStatus.setText(
                            "Firebase error!"
                    );

                    Log.e(
                            TAG,
                            "Error: "
                                    + e.getMessage()
                    );
                });

        map.invalidate();
    }

    // =============================
    // LIFECYCLE
    // =============================

    @Override
    protected void onResume() {

        super.onResume();

        isLoggingOut = false;

        setDriverOnline(true);
    }

    @Override
    protected void onPause() {

        super.onPause();
    }
    @Override
    protected void onDestroy() {

        if (sessionRef != null &&
                sessionListener != null) {

            sessionRef.removeEventListener(
                    sessionListener
            );
        }

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        locationListener
                );

            } catch (Exception ignored) {
            }
        }

        if (!isLoggingOut &&
                uid != null &&
                locationRef != null) {

            locationRef.child(uid)
                    .child("isOnline")
                    .setValue(false);
        }

        super.onDestroy();
    }
}