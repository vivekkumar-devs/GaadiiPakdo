package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.HashMap;

public class DriverRegisterActivity extends AppCompatActivity {

    private EditText etName, etVehicle, etcapacity, etEmail, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference driversRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_register);

        etName = findViewById(R.id.etName);
        etVehicle = findViewById(R.id.etVehicle);
        etcapacity = findViewById(R.id.etcapacity);
        etEmail = findViewById(R.id.etidd);
        etPassword = findViewById(R.id.etpassworrd);
        etConfirm = findViewById(R.id.etconforrmmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        mAuth = FirebaseAuth.getInstance();

        driversRef = FirebaseDatabase
                .getInstance("https://gaadiipakdo-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Drivers");

        btnRegister.setOnClickListener(v -> registerDriver());

        tvLogin.setOnClickListener(v -> finish());


        // ✅ STATUS BAR FIX (Same as Register Activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

    }

    private void registerDriver() {

        String name = etName.getText().toString().trim();
        String vehicle = etVehicle.getText().toString().trim();
        String capacity = etcapacity.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(vehicle) ||
                TextUtils.isEmpty(capacity) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirm)) {

            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {

                    String uid = mAuth.getCurrentUser().getUid();

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("name", name);
                    map.put("vehicleNumber", vehicle);
                    map.put("capacity", Integer.parseInt(capacity));
                    map.put("email", email);
                    map.put("isOnline", false);

                    driversRef.child(uid).setValue(map)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Registration Success", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, DriverMapsActivity.class));
                                finish();
                            });

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}