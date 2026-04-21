package vivek.harman.gaadiipakdo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;

public class DriverMapsActivity extends AppCompatActivity {

    private MapView map;
    private TextView tvStatus;

    // ✅ NEW UI (Driver Details)
    private TextView txtDriverName, txtVehicleNumber, txtCapacity;

    private LocationManager locationManager;
    private Marker driverMarker;

    private DatabaseReference locationRef;
    private String uid;

    private static final String TAG = "DRIVER_FIREBASE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );

        setContentView(R.layout.activity_driver_maps);

        // =============================
        // STATUS BAR
        // =============================
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        map = findViewById(R.id.mapdriver);
        tvStatus = findViewById(R.id.tvStatus);

        // ✅ Bind NEW TextViews (make sure IDs exist in XML)
        txtDriverName = findViewById(R.id.txtDriverName);
        txtVehicleNumber = findViewById(R.id.txtVehicle);
        txtCapacity = findViewById(R.id.txtCapacity);

        ImageButton bbtnBack = findViewById(R.id.bbtnBack);
        ImageButton btonLogout = findViewById(R.id.btonLogout);

        bbtnBack.setOnClickListener(v -> finish());
        btonLogout.setOnClickListener(v -> logoutDriver());

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        locationRef = FirebaseDatabase.getInstance().getReference("Drivers");

        setDriverOnline(true);

        // ✅ Fetch driver's own details
        loadDriverDetails();

        checkPermissions();
    }

    // =============================
    // 🔐 LOGOUT
    // =============================
    private void logoutDriver() {
        if (uid != null) {
            setDriverOnline(false);
        }

        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, DriverActivity.class));
        finish();
    }

    // =============================
    // 🟢 ONLINE STATUS
    // =============================
    private void setDriverOnline(boolean status) {
        if (uid == null) return;

        locationRef.child(uid).child("isOnline")
                .setValue(status)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Online status failed: " + e.getMessage()));
    }

    // =============================
    // ✅ FETCH DRIVER DETAILS
    // =============================
    private void loadDriverDetails() {

        if (uid == null) return;

        locationRef.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (snapshot.exists()) {

                            String name = snapshot.child("name").getValue(String.class);
                            String vehicle = snapshot.child("vehicleNumber").getValue(String.class);
                            Integer capacity = snapshot.child("capacity").getValue(Integer.class);

                            txtDriverName.setText("Driver: " + (name != null ? name : "--"));
                            txtVehicleNumber.setText("Vehicle: " + (vehicle != null ? vehicle : "--"));
                            txtCapacity.setText("Capacity: " + (capacity != null ? capacity : "--"));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvStatus.setText("Failed to load profile");
                    }
                });
    }

    // =============================
    // 📍 PERMISSIONS
    // =============================
    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }
    }

    // =============================
    // 📡 LOCATION UPDATES
    // =============================
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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

    private final LocationListener locationListener = location -> {
        if (location != null) {
            updateLocation(location);
        }
    };

    // =============================
    // 🔄 UPDATE LOCATION
    // =============================
    private void updateLocation(Location location) {

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        GeoPoint current = new GeoPoint(lat, lng);
        map.getController().animateTo(current);

        if (driverMarker == null) {
            driverMarker = new Marker(map);
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_cars);
            driverMarker.setIcon(icon);

            map.getOverlays().add(driverMarker);
        }

        driverMarker.setPosition(current);

        HashMap<String, Object> mapData = new HashMap<>();
        mapData.put("lat", lat);
        mapData.put("lng", lng);
        mapData.put("lastUpdated", System.currentTimeMillis());

        locationRef.child(uid).updateChildren(mapData)
                .addOnSuccessListener(aVoid -> {
                    tvStatus.setText("Live location updating...");
                    Log.d(TAG, "Location sent");
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Firebase error!");
                    Log.e(TAG, "Error: " + e.getMessage());
                });

        map.invalidate();
    }

    // =============================
    // 🔄 LIFECYCLE
    // =============================
    @Override
    protected void onResume() {
        super.onResume();
        setDriverOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        if (uid != null) {
            setDriverOnline(false);
        }
    }
}