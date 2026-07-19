package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Driver_Register_Activity extends AppCompatActivity {

    private EditText etName,
            driverPhone,
            etVehicle,
            etcapacity,
            etPassword,
            etConfirm;

    private Button btnRegister;

    private TextView tvLogin;

    private DatabaseReference driversRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // =========================
        // STATUS BAR
        // =========================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (getWindow().getInsetsController() != null) {

                getWindow().getInsetsController()
                        .setSystemBarsAppearance(
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        );
            }
        }

        setContentView(R.layout.activity_driver_register);

        TextView tvLogin = findViewById(R.id.tvLogin);

        String text = "Already have an account? Login";

        SpannableString spannableString = new SpannableString(text);

        ForegroundColorSpan colorSpan =
                new ForegroundColorSpan(Color.parseColor("#2563FF"));

        spannableString.setSpan(
                colorSpan,
                text.indexOf("Login"),
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvLogin.setText(spannableString);

        // =========================
        // VIEWS
        // =========================

        etName = findViewById(R.id.etName);

        driverPhone = findViewById(R.id.driverPhone);

        etVehicle = findViewById(R.id.etVehicle);

        etcapacity = findViewById(R.id.etcapacity);

        etPassword = findViewById(R.id.etpassworrd);

        etConfirm = findViewById(R.id.etconforrmmPassword);

        btnRegister = findViewById(R.id.btnRegister);

        tvLogin = findViewById(R.id.tvLogin);

        ImageView passwordToggle =
                findViewById(R.id.passwordToggle);

        ImageView confirmPasswordToggle =
                findViewById(R.id.confirmPasswordToggle);

        final boolean[] passwordVisible = {false};

        passwordToggle.setOnClickListener(v -> {

            if (passwordVisible[0]) {

                etPassword.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                passwordToggle.setImageResource(
                        R.drawable.ic_visibility);

            } else {

                etPassword.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                passwordToggle.setImageResource(
                        R.drawable.ic_visibility_off);
            }

            passwordVisible[0] = !passwordVisible[0];

            etPassword.setSelection(
                    etPassword.getText().length());
        });

        final boolean[] confirmVisible = {false};

        confirmPasswordToggle.setOnClickListener(v -> {

            if (confirmVisible[0]) {

                etConfirm.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                confirmPasswordToggle.setImageResource(
                        R.drawable.ic_visibility);

            } else {

                etConfirm.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                confirmPasswordToggle.setImageResource(
                        R.drawable.ic_visibility_off);
            }

            confirmVisible[0] = !confirmVisible[0];

            etConfirm.setSelection(
                    etConfirm.getText().length());
        });
        // =========================
        // FIREBASE
        // =========================

        driversRef = FirebaseDatabase.getInstance()
                .getReference("Drivers");

        // =========================
        // REGISTER BUTTON
        // =========================

        btnRegister.setOnClickListener(v -> registerDriver());

        // =========================
        // LOGIN BUTTON
        // =========================

        tvLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    Driver_Register_Activity.this,
                    Driver_Login_Activity.class
            );

            startActivity(intent);

            finish();
        });
    }

    private String getPasswordError(String password) {

        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }

        if (!password.matches(".*[A-Z].*")) {
            return "Add at least one uppercase letter";
        }

        if (!password.matches(".*\\d.*")) {
            return "Add at least one number";
        }

        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return "Add at least one special character";
        }

        return null;
    }

    private boolean isValidPassword(String password) {
        return getPasswordError(password) == null;
    }

    // =========================
    // REGISTER DRIVER
    // =========================

    private void registerDriver() {

        String name =
                etName.getText().toString().trim();

        String phone =
                driverPhone.getText().toString().trim();

        String vehicle =
                etVehicle.getText().toString().trim();

        String capacity =
                etcapacity.getText().toString().trim();

        String pass =
                etPassword.getText().toString().trim();

        String confirm =
                etConfirm.getText().toString().trim();

        // =========================
        // VALIDATION
        // =========================

        if (TextUtils.isEmpty(name)) {

            etName.setError("Enter Name");

            etName.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(phone)) {

            driverPhone.setError(
                    "Enter Mobile Number"
            );

            driverPhone.requestFocus();

            return;
        }

        if (phone.length() != 10) {

            driverPhone.setError(
                    "Enter Valid Mobile Number"
            );

            driverPhone.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(vehicle)) {

            etVehicle.setError(
                    "Enter Vehicle Number"
            );

            etVehicle.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(capacity)) {

            etcapacity.setError(
                    "Enter Capacity"
            );

            etcapacity.requestFocus();

            return;
        }

        if (!TextUtils.isDigitsOnly(capacity)) {

            etcapacity.setError(
                    "Enter Valid Capacity"
            );

            etcapacity.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(pass)) {

            etPassword.setError(
                    "Enter Password"
            );

            etPassword.requestFocus();

            return;
        }

        String passwordError = getPasswordError(pass);

        if (passwordError != null) {

            etPassword.requestFocus();

            Toast.makeText(
                    this,
                    passwordError,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (TextUtils.isEmpty(confirm)) {

            etConfirm.setError(
                    "Confirm Password"
            );

            etConfirm.requestFocus();

            return;
        }

        if (!pass.equals(confirm)) {

            etConfirm.setError(
                    "Passwords do not match"
            );

            etConfirm.requestFocus();

            return;
        }

        btnRegister.setEnabled(false);



        // =========================
        // CHECK EXISTING PHONE
        // =========================

        driversRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        boolean exists = false;

                        for (DataSnapshot data :
                                snapshot.getChildren()) {

                            String dbPhone =
                                    data.child("phone")
                                            .getValue(String.class);

                            if (dbPhone != null &&
                                    dbPhone.equals(phone)) {

                                exists = true;

                                break;
                            }
                        }

                        // =========================
                        // ALREADY EXISTS
                        // =========================

                        if (exists) {

                            btnRegister.setEnabled(true);

                            Toast.makeText(
                                    Driver_Register_Activity.this,
                                    "Phone Number Already Registered",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        // =========================
                        // CREATE DRIVER ID
                        // =========================

                        String driverId =
                                driversRef.push().getKey();

                        if (driverId == null) {

                            btnRegister.setEnabled(true);

                            Toast.makeText(
                                    Driver_Register_Activity.this,
                                    "Failed To Generate ID",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // =========================
                        // DRIVER DATA
                        // =========================

                        HashMap<String, Object> map =
                                new HashMap<>();

                        map.put("driverId", driverId);

                        map.put("name", name);

                        map.put("phone", phone);

                        map.put("vehicleNumber", vehicle);

                        map.put(
                                "capacity",
                                Integer.parseInt(capacity)
                        );

                        map.put("password", pass);

                        map.put("isOnline", false);

                        map.put(
                                "lastUpdated",
                                System.currentTimeMillis()
                        );

                        // =========================
                        // SAVE DATA
                        // =========================

                        driversRef.child(driverId)
                                .setValue(map)
                                .addOnSuccessListener(unused -> {

                                    btnRegister.setEnabled(true);

                                    Toast.makeText(
                                            Driver_Register_Activity.this,
                                            "Registration Successful",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    Intent intent =
                                            new Intent(
                                                    Driver_Register_Activity.this,
                                                    Driver_Login_Activity.class
                                            );

                                    intent.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);

                                    finish();
                                })
                                .addOnFailureListener(e -> {

                                    btnRegister.setEnabled(true);

                                    Toast.makeText(
                                            Driver_Register_Activity.this,
                                            "Database Error: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        btnRegister.setEnabled(true);

                        Toast.makeText(
                                Driver_Register_Activity.this,
                                "Database Error: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
