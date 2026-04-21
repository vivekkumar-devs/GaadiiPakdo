package vivek.harman.gaadiipakdo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity {

    private MapView map;
    private LocationManager locationManager;

    private double userLat, userLng;

    private DatabaseReference driversRef;

    // UI
    private TextView txtDriverName, txtVehicleNumber, txtCapacity, txtLastUpdated;
    private TextView distanceText, timeText, txtStatus;

    // Map objects
    private Marker userMarker;
    private Polyline routeMain, routeHalo;

    private final List<Marker> driverMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_maps);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(MapsActivity.this, MainActivity.class));
                finish();
            });
        }

        ImageButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MapsActivity.this, UserActivity.class));
                finish();
            });
        }

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        txtDriverName = findViewById(R.id.txtDriverName);
        txtVehicleNumber = findViewById(R.id.txtVehicleNumber);
        txtCapacity = findViewById(R.id.txtCapacity);
        txtLastUpdated = findViewById(R.id.txtLastUpdated);
        distanceText = findViewById(R.id.distanceText);
        timeText = findViewById(R.id.timeText);
        txtStatus = findViewById(R.id.txtStatus);

        driversRef = FirebaseDatabase
                .getInstance("https://gaadiipakdo-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Drivers");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkPermission();
        loadDrivers();
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocation();
        }
    }

    private void startLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,
                2,
                locationListener
        );
    }

    private final LocationListener locationListener = location -> {
        if (location == null) return;

        userLat = location.getLatitude();
        userLng = location.getLongitude();

        updateUserMarker();
    };

    // =============================
    // DRIVER LOADING
    // =============================
    private void loadDrivers() {

        driversRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                GeoPoint userPoint = new GeoPoint(userLat, userLng);

                driverMarkers.clear();

                double minDistance = Double.MAX_VALUE;
                DataSnapshot nearestSnap = null;
                GeoPoint nearestPoint = null;

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Double lat = snap.child("lat").getValue(Double.class);
                    Double lng = snap.child("lng").getValue(Double.class);
                    Boolean online = snap.child("isOnline").getValue(Boolean.class);

                    if (lat == null || lng == null || !Boolean.TRUE.equals(online))
                        continue;

                    GeoPoint driverPoint = new GeoPoint(lat, lng);

                    float[] result = new float[1];
                    Location.distanceBetween(userLat, userLng, lat, lng, result);
                    double distance = result[0];

                    Marker marker = new Marker(map);
                    marker.setPosition(driverPoint);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                    Drawable carIcon = ContextCompat.getDrawable(
                            MapsActivity.this, R.drawable.ic_cars);

                    marker.setIcon(carIcon);

                    driverMarkers.add(marker);

                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestSnap = snap;
                        nearestPoint = driverPoint;
                    }
                }

                if (nearestSnap != null && nearestPoint != null) {
                    drawRoute(userPoint, nearestPoint);
                    zoomToFit(userPoint, nearestPoint);
                    updateUI(nearestSnap, minDistance);
                } else {
                    txtStatus.setText("No drivers available");
                }

                renderMap(); // 🔥 centralized rendering

                map.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    // =============================
    // USER MARKER
    // =============================
    private void updateUserMarker() {
        GeoPoint userPoint = new GeoPoint(userLat, userLng);

        if (userMarker == null) {
            userMarker = new Marker(map);
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            Drawable userIcon = ContextCompat.getDrawable(
                    this, R.drawable.ic_user_location);

            userMarker.setIcon(userIcon);
        }

        userMarker.setPosition(userPoint);

        renderMap(); // 🔥 refresh order
    }

    // =============================
    // RENDER ORDER CONTROL
    // =============================
    private void renderMap() {

        map.getOverlays().clear();

        // 1. Route (bottom)
        if (routeHalo != null) map.getOverlays().add(routeHalo);
        if (routeMain != null) map.getOverlays().add(routeMain);

        // 2. User
        if (userMarker != null) map.getOverlays().add(userMarker);

        // 3. Drivers (top)
        for (Marker m : driverMarkers) {
            map.getOverlays().add(m);
        }
    }

    // =============================
    // ROUTE DRAWING
    // =============================
    private void drawRoute(GeoPoint user, GeoPoint driver) {

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + user.getLongitude() + "," + user.getLatitude() + ";"
                + driver.getLongitude() + "," + driver.getLatitude()
                + "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.connect();

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                org.json.JSONObject json = new org.json.JSONObject(result.toString());

                org.json.JSONArray coords = json
                        .getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<GeoPoint> points = new ArrayList<>();

                for (int i = 0; i < coords.length(); i++) {
                    org.json.JSONArray p = coords.getJSONArray(i);
                    points.add(new GeoPoint(p.getDouble(1), p.getDouble(0)));
                }

                runOnUiThread(() -> {

                    routeHalo = new Polyline();
                    routeHalo.setPoints(points);
                    routeHalo.setWidth(14f);
                    routeHalo.setColor(Color.BLACK);

                    routeMain = new Polyline();
                    routeMain.setPoints(points);
                    routeMain.setWidth(8f);
                    routeMain.setColor(Color.parseColor("#2962FF"));

                    renderMap();
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void zoomToFit(GeoPoint p1, GeoPoint p2) {
        BoundingBox box = new BoundingBox(
                Math.max(p1.getLatitude(), p2.getLatitude()),
                Math.max(p1.getLongitude(), p2.getLongitude()),
                Math.min(p1.getLatitude(), p2.getLatitude()),
                Math.min(p1.getLongitude(), p2.getLongitude())
        );

        map.zoomToBoundingBox(box, true, 150);
    }

    private void updateUI(DataSnapshot snap, double distanceMeters) {

        String name = snap.child("name").getValue(String.class);
        String vehicle = snap.child("vehicleNumber").getValue(String.class);
        Integer capacity = snap.child("capacity").getValue(Integer.class);
        Long updated = snap.child("lastUpdated").getValue(Long.class);

        txtDriverName.setText("Driver: " + (name != null ? name : "--"));
        txtVehicleNumber.setText("Vehicle: " + (vehicle != null ? vehicle : "--"));
        txtCapacity.setText("Capacity: " + (capacity != null ? capacity : "--"));
        txtLastUpdated.setText(updated != null ? "Updated: " + updated : "--");

        double km = distanceMeters / 1000.0;

        distanceText.setText(
                km < 1
                        ? String.format("Distance: %.0f m", distanceMeters)
                        : String.format("Distance: %.2f km", km)
        );

        double speed = km < 1 ? 20.0 : 30.0;
        int eta = (int) ((km / speed) * 60);

        timeText.setText("ETA: " + Math.max(1, eta) + " min");

        txtStatus.setText("Nearest Driver Selected");
    }
}