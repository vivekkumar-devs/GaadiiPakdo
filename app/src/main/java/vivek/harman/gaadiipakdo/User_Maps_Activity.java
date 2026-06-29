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
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.*;
import androidx.annotation.NonNull;
import android.net.Uri;
import android.widget.Toast;

public class User_Maps_Activity extends AppCompatActivity {

    private MapView map;
    private boolean driversLoaded = false;
    private String driverPhone = "";
    private LocationManager locationManager;
    private static final long DRIVER_TIMEOUT = 60000; // 60 seconds

    private double userLat, userLng;

    private DatabaseReference driversRef;

    private DatabaseReference sessionRef;

    private ValueEventListener sessionListener;

    private boolean isLoggingOut = false;

    private Polyline routeOuter;
    private final List<Marker> routeDots = new ArrayList<>();

    // UI
    private TextView txtDriverName, txtVehicleNumber, txtCapacity, txtLastUpdated;
    private TextView distanceText, timeText, txtStatus;
    private ImageButton callDriverBtn;
    private View noDriverLayout;
    private View vehicleCard;
    private View infoCard;


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

        setContentView(R.layout.activity_user_maps);



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

        vehicleCard = findViewById(R.id.vehicleCard);
        infoCard = findViewById(R.id.infoCard);

        callDriverBtn = findViewById(R.id.callDriverBtn);

        callDriverBtn.setOnClickListener(v -> {

            if (driverPhone != null && !driverPhone.isEmpty()) {

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + driverPhone));
                startActivity(intent);

            } else {

                Toast.makeText(
                        User_Maps_Activity.this,
                        "No driver available",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(User_Maps_Activity.this, Welcome_Activity.class));
                finish();
            });
        }

        ImageButton btnLogout = findViewById(R.id.btnLogout);

        if (btnLogout != null) {

            btnLogout.setOnClickListener(v -> {

                isLoggingOut = true;

                SharedPreferences prefs =
                        getSharedPreferences("app", MODE_PRIVATE);

                String userKey =
                        prefs.getString("userKey", "");

                // Remove session token from Firebase
                if (!userKey.isEmpty()) {

                    FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(userKey)
                            .child("sessionToken")
                            .setValue("");

                }

                // Clear local session
                prefs.edit()
                        .clear()
                        .apply();

                // Open Login Screen
                Intent intent = new Intent(
                        User_Maps_Activity.this,
                        User_Login_Activity.class
                );

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(intent);

                finish();
            });
        }

        map = findViewById(R.id.map);
        noDriverLayout = findViewById(R.id.noDriverLayout);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                return true;
            }

            @Override
            public boolean onZoom(org.osmdroid.events.ZoomEvent event) {

                if (routeOuter == null || routeHalo == null || routeMain == null)
                    return true;

                double zoom = event.getZoomLevel();

                float roadWidth;

                if (zoom >= 19) {
                    roadWidth = 28f;
                } else if (zoom >= 18) {
                    roadWidth = 24f;
                } else if (zoom >= 17) {
                    roadWidth = 20f;
                } else if (zoom >= 16) {
                    roadWidth = 16f;
                } else {
                    roadWidth = 12f;
                }

                routeOuter.setWidth(roadWidth + 12f);
                routeHalo.setWidth(roadWidth + 8f);
                routeMain.setWidth(roadWidth);

                map.invalidate();

                return true;
            }
        });
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
                 startSessionListener();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkPermission();

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

        if (!driversLoaded) {
            driversLoaded = true;
            loadDrivers();
        }
    };

    // =============================
    // ONE TIME ONE LOGIN
    // =============================

    private void startSessionListener() {

        SharedPreferences prefs =
                getSharedPreferences(
                        "app",
                        MODE_PRIVATE
                );

        String userKey =
                prefs.getString(
                        "userKey",
                        ""
                );

        if (userKey.isEmpty()) {
            return;
        }

        sessionRef =
                FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(userKey)
                        .child("sessionToken");

        sessionListener =
                new ValueEventListener() {

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
                                serverToken.trim().isEmpty()) {
                            return;
                        }

                        if (localToken == null ||
                                localToken.trim().isEmpty()) {
                            return;
                        }

                        if (!serverToken.equals(localToken)) {

                            isLoggingOut = true;

                            Toast.makeText(
                                    User_Maps_Activity.this,
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
                                            User_Maps_Activity.this,
                                            User_Login_Activity.class
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
                            @NonNull DatabaseError error) {
                    }
                };

        sessionRef.addValueEventListener(
                sessionListener
        );
    }


    // =============================
    // TIME UPDATE STATUS
    // =============================
    private String getTimeAgo(long timestamp) {

        long diff = System.currentTimeMillis() - timestamp;

        long seconds = diff / 1000;

        if (seconds < 1)
            return "Just now";

        if (seconds < 60)
            return seconds + (seconds == 1 ? " sec ago" : " secs ago");

        long minutes = seconds / 60;

        if (minutes < 60)
            return minutes + (minutes == 1 ? " min ago" : " mins ago");

        long hours = minutes / 60;

        if (hours < 24)
            return hours + (hours == 1 ? " hr ago" : " hrs ago");

        long days = hours / 24;

        return days + (days == 1 ? " day ago" : " days ago");
    }

    // =============================
    // DRIVER LOADING
    // =============================
    private void loadDrivers() {

        driversRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                GeoPoint userPoint = new GeoPoint(userLat, userLng);

                for (Marker marker : driverMarkers) {
                    map.getOverlays().remove(marker);
                }

                driverMarkers.clear();

                double minDistance = Double.MAX_VALUE;
                DataSnapshot nearestSnap = null;
                GeoPoint nearestPoint = null;

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Double lat = snap.child("lat").getValue(Double.class);
                    Double lng = snap.child("lng").getValue(Double.class);

                    Boolean online =
                            snap.child("isOnline")
                                    .getValue(Boolean.class);
                    android.util.Log.d(
                            "DRIVER_CHECK",
                            snap.getKey()
                                    + " online=" + online
                    );

                    Long lastUpdated =
                            snap.child("lastUpdated")
                                    .getValue(Long.class);

                    if (lat == null || lng == null)
                        continue;

                    if (!Boolean.TRUE.equals(online))
                        continue;

                    if (lastUpdated == null)
                        continue;

                    long age =
                            System.currentTimeMillis()
                                    - lastUpdated;

                    if (age > DRIVER_TIMEOUT)
                        continue;

                    GeoPoint driverPoint = new GeoPoint(lat, lng);

                    float[] result = new float[1];
                    Location.distanceBetween(userLat, userLng, lat, lng, result);
                    double distance = result[0];

                    Marker marker = new Marker(map);
                    marker.setPosition(driverPoint);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                    Drawable carIcon = ContextCompat.getDrawable(
                            User_Maps_Activity.this, R.drawable.ic_cars);

                    marker.setIcon(carIcon);

                    driverMarkers.add(marker);

                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestSnap = snap;
                        nearestPoint = driverPoint;
                    }
                }

                if (nearestSnap != null && nearestPoint != null) {

                    // Show map
                    map.setVisibility(View.VISIBLE);
                    noDriverLayout.setVisibility(View.GONE);

                    drawRoute(userPoint, nearestPoint);
                    zoomToFit(userPoint, nearestPoint);
                    updateUI(nearestSnap, minDistance);

                } else {

                    // Show no-driver screen
                    map.setVisibility(View.GONE);
                    noDriverLayout.setVisibility(View.VISIBLE);

                    txtStatus.setText("No drivers available");

                    txtDriverName.setText("Driver: --");
                    txtVehicleNumber.setText("Vehicle: --");
                    txtCapacity.setText("Capacity: --");
                    txtLastUpdated.setText("Updated: --");

                    distanceText.setText("--");
                    timeText.setText("--");

                    driverPhone = "";

                    routeMain = null;
                    routeHalo = null;
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
        if (driversLoaded) {
            driversRef.get().addOnSuccessListener(snapshot -> {

                double minDistance = Double.MAX_VALUE;
                GeoPoint nearestPoint = null;

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Double lat = snap.child("lat").getValue(Double.class);
                    Double lng = snap.child("lng").getValue(Double.class);

                    Boolean online =
                            snap.child("isOnline").getValue(Boolean.class);

                    Long lastUpdated =
                            snap.child("lastUpdated").getValue(Long.class);

                    if (lat == null || lng == null)
                        continue;

                    if (!Boolean.TRUE.equals(online))
                        continue;

                    if (lastUpdated == null)
                        continue;

                    if (System.currentTimeMillis() - lastUpdated > DRIVER_TIMEOUT)
                        continue;

                    float[] result = new float[1];
                    Location.distanceBetween(
                            userLat,
                            userLng,
                            lat,
                            lng,
                            result
                    );

                    if (result[0] < minDistance) {
                        minDistance = result[0];
                        nearestPoint = new GeoPoint(lat, lng);
                    }
                }

                if (nearestPoint != null) {
                    drawRoute(userPoint, nearestPoint);
                }
            });
        }


    }

    // =============================
    // RENDER ORDER CONTROL
    // =============================
    private void renderMap() {

        map.getOverlays().clear();

        // Route layers
        if (routeOuter != null)
            map.getOverlays().add(routeOuter);

        if (routeHalo != null)
            map.getOverlays().add(routeHalo);

        if (routeMain != null)
            map.getOverlays().add(routeMain);

        // Route dots
        for (Marker dot : routeDots) {
            map.getOverlays().add(dot);
        }

// User marker
        if (userMarker != null)
            map.getOverlays().add(userMarker);
        // Driver markers
        for (Marker m : driverMarkers) {
            map.getOverlays().add(m);
        }
    }

    private void drawDottedRoute(GeoPoint user, GeoPoint driver) {

        // Remove old dots
        for (Marker dot : routeDots) {
            map.getOverlays().remove(dot);
        }
        routeDots.clear();

        // Remove route polylines
        routeOuter = null;
        routeHalo = null;
        routeMain = null;

        int dotCount = 20;

        for (int i = 0; i <= dotCount; i++) {

            double lat =
                    user.getLatitude()
                            + (driver.getLatitude() - user.getLatitude())
                            * i / (double) dotCount;

            double lon =
                    user.getLongitude()
                            + (driver.getLongitude() - user.getLongitude())
                            * i / (double) dotCount;

            Marker dot = new Marker(map);

            dot.setPosition(new GeoPoint(lat, lon));

            dot.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_CENTER
            );

            dot.setIcon(
                    ContextCompat.getDrawable(
                            this,
                            R.drawable.route_dot
                    )
            );

            routeDots.add(dot);
        }

        renderMap();
        map.invalidate();
    }
    private void drawRoute(GeoPoint user, GeoPoint driver) {

        float[] distanceResult = new float[1];

        Location.distanceBetween(
                user.getLatitude(),
                user.getLongitude(),
                driver.getLatitude(),
                driver.getLongitude(),
                distanceResult
        );

        // Direct line only when very close
        if (distanceResult[0] < 10) {

            runOnUiThread(() ->
                    drawDottedRoute(user, driver));

            return;
        }

        String url =
                "https://router.project-osrm.org/route/v1/driving/"
                        + user.getLongitude() + "," + user.getLatitude()
                        + ";"
                        + driver.getLongitude() + "," + driver.getLatitude()
                        + "?overview=full&geometries=geojson";

        new Thread(() -> {

            try {

                android.util.Log.d("OSRM_URL", url);

                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection)
                                new java.net.URL(url).openConnection();

                conn.setRequestProperty(
                        "User-Agent",
                        "GaadiiPakdo Android App"
                );

                conn.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();

                android.util.Log.d(
                        "OSRM_HTTP",
                        "Response Code = " + responseCode
                );

                if (responseCode != 200) {

                    runOnUiThread(() ->
                            drawDottedRoute(user, driver));

                    return;
                }

                java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(
                                        conn.getInputStream()
                                )
                        );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                android.util.Log.d(
                        "OSRM_RESPONSE",
                        response.toString()
                );

                org.json.JSONObject json =
                        new org.json.JSONObject(
                                response.toString()
                        );

                org.json.JSONArray routes =
                        json.getJSONArray("routes");

                if (routes.length() == 0) {

                    runOnUiThread(() ->
                            drawDottedRoute(user, driver));

                    return;
                }

                org.json.JSONArray coords =
                        routes.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONArray("coordinates");

                List<GeoPoint> points = new ArrayList<>();

                for (int i = 0; i < coords.length(); i++) {

                    org.json.JSONArray p =
                            coords.getJSONArray(i);

                    double lon = p.getDouble(0);
                    double lat = p.getDouble(1);

                    points.add(
                            new GeoPoint(lat, lon)
                    );
                }

                android.util.Log.d(
                        "OSRM_POINTS",
                        "Points = " + points.size()
                );

                runOnUiThread(() -> {

                    double zoom = map.getZoomLevelDouble();

                    float roadWidth;

                    if (zoom >= 19) {
                        roadWidth = 28f;
                    }
                    else if (zoom >= 18) {
                        roadWidth = 24f;
                    }
                    else if (zoom >= 17) {
                        roadWidth = 20f;
                    }
                    else if (zoom >= 16) {
                        roadWidth = 16f;
                    }
                    else {
                        roadWidth = 12f;
                    }

                    routeOuter = new Polyline();
                    routeOuter.setPoints(points);
                    routeOuter.setWidth(34f);
                    routeOuter.setColor(Color.parseColor("#AAB6C2"));

                    routeHalo = new Polyline();
                    routeHalo.setPoints(points);
                    routeHalo.setWidth(30f);
                    routeHalo.setColor(Color.parseColor("#4285F4"));

                    routeMain = new Polyline();
                    routeMain.setPoints(points);
                    routeMain.setWidth(22f);
                    routeMain.setColor(Color.parseColor("#7FB3FF"));

                    // Rounded route ends
                    routeOuter.getOutlinePaint().setStrokeCap(
                            android.graphics.Paint.Cap.ROUND);

                    routeHalo.getOutlinePaint().setStrokeCap(
                            android.graphics.Paint.Cap.ROUND);

                    routeMain.getOutlinePaint().setStrokeCap(
                            android.graphics.Paint.Cap.ROUND);

                    renderMap();
                    map.invalidate();
                });

            } catch (Exception e) {

                android.util.Log.e(
                        "OSRM_ERROR",
                        e.getMessage(),
                        e
                );

                runOnUiThread(() ->
                        drawDottedRoute(user, driver));
            }

        }).start();
    }

    private void zoomToFit(GeoPoint user, GeoPoint driver) {

        float[] result = new float[1];

        Location.distanceBetween(
                user.getLatitude(),
                user.getLongitude(),
                driver.getLatitude(),
                driver.getLongitude(),
                result
        );

        // Very close distance
        if (result[0] < 100) {

            GeoPoint center = new GeoPoint(
                    (user.getLatitude() + driver.getLatitude()) / 2,
                    (user.getLongitude() + driver.getLongitude()) / 2
            );

            map.getController().animateTo(center);
            map.getController().setZoom(18.0);

            return;
        }

        BoundingBox box = new BoundingBox(
                Math.max(user.getLatitude(), driver.getLatitude()),
                Math.max(user.getLongitude(), driver.getLongitude()),
                Math.min(user.getLatitude(), driver.getLatitude()),
                Math.min(user.getLongitude(), driver.getLongitude())
        );

        map.post(() -> {

            int bottomPadding =
                    vehicleCard.getHeight()
                            + infoCard.getHeight()
                            + 120;

            map.zoomToBoundingBox(
                    box,
                    true,
                    150,
                    bottomPadding,
                    500L
            );

            map.postDelayed(() -> {

                GeoPoint center = box.getCenterWithDateLine();

                double latShift =
                        (box.getLatNorth() - box.getLatSouth()) * 0.25;

                map.getController().animateTo(
                        new GeoPoint(
                                center.getLatitude() + latShift,
                                center.getLongitude()
                        )
                );

            }, 300);

        });
    }
    private void updateUI(DataSnapshot snap, double distanceMeters) {

        String name = snap.child("name").getValue(String.class);
        String vehicle = snap.child("vehicleNumber").getValue(String.class);
        Integer capacity = snap.child("capacity").getValue(Integer.class);
        Long updated = snap.child("lastUpdated").getValue(Long.class);

        driverPhone = snap.child("phone").getValue(String.class);

        if (driverPhone == null) {
            driverPhone = "";
        }

        txtDriverName.setText("Driver: " + (name != null ? name : "--"));
        txtVehicleNumber.setText("Vehicle: " + (vehicle != null ? vehicle : "--"));
        txtCapacity.setText("Capacity: " + (capacity != null ? capacity : "--"));


        if (updated != null) {
            txtLastUpdated.setText(
                    "Updated: " + getTimeAgo(updated)
            );
        } else {
            txtLastUpdated.setText("Updated: --");
        }
        double km = distanceMeters / 1000.0;

        distanceText.setText(
                km < 1
                        ? String.format("%.0f m", distanceMeters)
                        : String.format("%.2f km", km)
        );

        double speed = km < 1 ? 20.0 : 30.0;
        int eta = (int) ((km / speed) * 60);

        timeText.setText("" + Math.max(1, eta) + " min");

        txtStatus.setText("Nearest Driver Selected");
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

    super.onDestroy();
}
}