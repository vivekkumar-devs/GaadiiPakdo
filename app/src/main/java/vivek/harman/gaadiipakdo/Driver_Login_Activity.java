package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Driver_Login_Activity extends AppCompatActivity {

    private EditText etPhone, etPassword;

    private Button btnLogin;

    private TextView tvRegister, forgotPasswordText;

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

        setContentView(R.layout.activity_driver_login);

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
        TextView tvRegister = findViewById(R.id.tvRegister);

        String text = "Don't have an account? Signup";

        SpannableString spannableString = new SpannableString(text);

        ForegroundColorSpan colorSpan =
                new ForegroundColorSpan(Color.parseColor("#2563FF"));

        spannableString.setSpan(
                colorSpan,
                text.indexOf("Signup"),
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvRegister.setText(spannableString);

        // =========================
        // VIEWS
        // =========================

        etPhone =
                findViewById(R.id.etDriverPhone);

        etPassword =
                findViewById(R.id.etPassword);

        btnLogin =
                findViewById(R.id.btnLogin);

        tvRegister =
                findViewById(R.id.tvRegister);

        forgotPasswordText =
                findViewById(R.id.forgotPasswordText);


        ImageView passwordToggle =
                findViewById(R.id.driverPasswordToggle);

        final boolean[] passwordVisible = {false};

        passwordToggle.setOnClickListener(v -> {

                    if (passwordVisible[0]) {

                        etPassword.setTransformationMethod(
                                PasswordTransformationMethod.getInstance());

                        passwordToggle.setImageResource(
                                R.drawable.ic_visibility);

                        passwordVisible[0] = false;

                    } else {

                        etPassword.setTransformationMethod(
                                HideReturnsTransformationMethod.getInstance());

                        passwordToggle.setImageResource(
                                R.drawable.ic_visibility_off);

                        passwordVisible[0] = true;
                    }

                    etPassword.setSelection(
                            etPassword.getText().length());

        });
        // =========================
// AUTO SCROLL
// =========================


        androidx.core.widget.NestedScrollView scrollView =
                findViewById(R.id.scrollView);

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() ->
                        scrollView.smoothScrollTo(0, btnLogin.getBottom()), 200);
            }
        });

        // =========================
        // FIREBASE
        // =========================

        driversRef = FirebaseDatabase.getInstance()
                .getReference("Drivers");

        // =========================
        // SESSION CHECK
        // =========================

        SharedPreferences prefs =
                getSharedPreferences(
                        "app",
                        MODE_PRIVATE
                );

        boolean isLoggedIn =
                prefs.getBoolean(
                        "isLoggedIn",
                        false
                );

        String role =
                prefs.getString(
                        "role",
                        ""
                );

        if (isLoggedIn &&
                "driver".equals(role)) {

            goToDriverMap();

            return;
        }

        // =========================
        // FORGOT PASSWORD
        // =========================

        forgotPasswordText.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Driver_Login_Activity.this,
                            Forgot_Password_Activity.class
                    );

            intent.putExtra(
                    "role",
                    "driver"
            );

            startActivity(intent);
        });

        // =========================
        // REGISTER
        // =========================

        tvRegister.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Driver_Login_Activity.this,
                            Driver_Register_Activity.class
                    );

            startActivity(intent);
        });

        // =========================
        // LOGIN
        // =========================

        btnLogin.setOnClickListener(v ->
                loginDriver()
        );
    }

    // =========================
    // LOGIN DRIVER
    // =========================

    private void loginDriver() {

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();

        String pass =
                etPassword.getText()
                        .toString()
                        .trim();

        // =========================
        // VALIDATION
        // =========================

        if (TextUtils.isEmpty(phone)) {

            etPhone.setError(
                    "Enter Phone Number"
            );

            etPhone.requestFocus();

            return;
        }

        if (phone.length() != 10) {

            etPhone.setError(
                    "Enter Valid Phone Number"
            );

            etPhone.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(pass)) {

            etPassword.setError(
                    "Enter Password"
            );

            etPassword.requestFocus();

            return;
        }

        // =========================
        // DISABLE BUTTON
        // =========================

        btnLogin.setEnabled(false);

        // =========================
        // FIREBASE LOGIN CHECK
        // =========================

        driversRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        boolean phoneFound = false;
                        boolean loginSuccess = false;

                        for (DataSnapshot data : snapshot.getChildren()) {

                            String dbPhone =
                                    data.child("phone")
                                            .getValue(String.class);

                            String dbPassword =
                                    data.child("password")
                                            .getValue(String.class);

                            if (dbPhone != null &&
                                    dbPhone.equals(phone)) {

                                phoneFound = true;

                                if (dbPassword != null &&
                                        dbPassword.equals(pass)) {

                                    loginSuccess = true;

                                    String driverId = data.getKey();

                                    String sessionToken =
                                            java.util.UUID.randomUUID().toString();

                                    android.util.Log.d(
                                            "LOGIN_TOKEN",
                                            "Generated Token = " + sessionToken
                                    );

                                    data.getRef()
                                            .child("sessionToken")
                                            .setValue(sessionToken);

                                    SharedPreferences.Editor editor =
                                            getSharedPreferences(
                                                    "app",
                                                    MODE_PRIVATE
                                            ).edit();

                                    editor.putBoolean("isLoggedIn", true);

                                    editor.putString("role", "driver");

                                    editor.putString("phone", phone);

                                    editor.putString("driverId", driverId);

                                    editor.putString("sessionToken", sessionToken);

                                    editor.putString("driverKey", driverId);

                                    editor.apply();

                                    Toast.makeText(
                                            Driver_Login_Activity.this,
                                            "Login Successful",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    goToDriverMap();

                                    break;
                                }
                            }
                        }

                        btnLogin.setEnabled(true);

                        btnLogin.setEnabled(true);

                        if (!loginSuccess) {

                            if (!phoneFound) {

                                Toast.makeText(
                                        Driver_Login_Activity.this,
                                        "Phone number not registered",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        Driver_Login_Activity.this,
                                        "Incorrect password",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        btnLogin.setEnabled(true);

                        Toast.makeText(
                                Driver_Login_Activity.this,
                                "Database Error: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================
    // OPEN DRIVER MAP
    // =========================

    private void goToDriverMap() {

        Intent intent =
                new Intent(
                        Driver_Login_Activity.this,
                        Driver_Maps_Activity.class
                );

        startActivity(intent);

        finish();
    }
}