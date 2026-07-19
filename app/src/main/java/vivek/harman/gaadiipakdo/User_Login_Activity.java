package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class User_Login_Activity extends AppCompatActivity {

    private EditText userId, password;

    private Button loginBtn;

    private TextView registerText, forgotPasswordText;

    private DatabaseReference usersRef;

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

                getWindow().getInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        setContentView(R.layout.activity_user_login);
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

        TextView registerTextt = findViewById(R.id.registerTextt);

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

        registerTextt.setText(spannableString);

        // =========================
        // VIEWS
        // =========================

        userId = findViewById(R.id.userIdd);

        password = findViewById(R.id.etuserPassword);

        loginBtn = findViewById(R.id.loginBtnn);

        registerText = findViewById(R.id.registerTextt);

        forgotPasswordText =
                findViewById(R.id.forgotPasswordText);

        ImageView passwordToggle =
                findViewById(R.id.passwordToggle);

        final boolean[] passwordVisible = {false};

        passwordToggle.setOnClickListener(v -> {

            if (passwordVisible[0]) {

                password.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                passwordToggle.setImageResource(
                        R.drawable.ic_visibility);

                passwordVisible[0] = false;

            } else {

                password.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                passwordToggle.setImageResource(
                        R.drawable.ic_visibility_off);

                passwordVisible[0] = true;
            }

            password.setSelection(
                    password.getText().length());
        });

        // =========================
// AUTO SCROLL
// =========================


        androidx.core.widget.NestedScrollView scrollView =
                findViewById(R.id.scrollView);

        password.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() ->
                        scrollView.smoothScrollTo(0, loginBtn.getBottom()), 200);
            }
        });

        // =========================
        // FIREBASE
        // =========================

        usersRef = FirebaseDatabase
                .getInstance()
                .getReference("Users");

        // =========================
        // SESSION CHECK
        // =========================

        SharedPreferences prefs =
                getSharedPreferences("app", MODE_PRIVATE);
/// /////////////////////////////////////
        boolean isLoggedIn =
                prefs.getBoolean("isLoggedIn", false);

        String role =
                prefs.getString("role", "");

        String sessionToken =
                prefs.getString(
                        "sessionToken",
                        ""
                );

        if (isLoggedIn
                && role.equals("user")
                && !sessionToken.isEmpty()) {
/// //////////////////////////
            Intent intent =
                    new Intent(
                            User_Login_Activity.this,
                            User_Maps_Activity.class
                    );

            startActivity(intent);

            finish();

            return;
        }

        // =========================
        // FORGOT PASSWORD
        // =========================

        forgotPasswordText.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            User_Login_Activity.this,
                            Forgot_Password_Activity.class
                    )
            );
        });

        // =========================
        // REGISTER
        // =========================

        registerText.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            User_Login_Activity.this,
                            User_Register_Activity.class
                    )
            );
        });

        // =========================
        // LOGIN BUTTON
        // =========================

        loginBtn.setOnClickListener(v -> loginUser());
    }

    // =========================
    // LOGIN USER
    // =========================

    private void loginUser() {

        String phone =
                userId.getText()
                        .toString()
                        .trim();

        String pass =
                password.getText()
                        .toString()
                        .trim();

        // =========================
        // VALIDATION
        // =========================

        if (TextUtils.isEmpty(phone)) {

            userId.setError("Enter Phone Number");

            userId.requestFocus();

            return;
        }

        if (phone.length() != 10) {

            userId.setError("Enter Valid Phone Number");

            userId.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(pass)) {

            password.setError("Enter Password");

            password.requestFocus();

            return;
        }

        // =========================
        // DATABASE CHECK
        // =========================

        usersRef.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                boolean phoneFound = false;
                boolean loginSuccess = false;

                for (DataSnapshot snapshot : task.getResult().getChildren()) {

                    String dbPhone =
                            snapshot.child("phone")
                                    .getValue(String.class);

                    String dbPassword =
                            snapshot.child("password")
                                    .getValue(String.class);

                    if (dbPhone != null &&
                            dbPhone.equals(phone)) {

                        phoneFound = true;

                        if (dbPassword != null &&
                                dbPassword.equals(pass)) {

                            loginSuccess = true;

                            String sessionToken =
                                    java.util.UUID.randomUUID().toString();

                            snapshot.getRef()
                                    .child("sessionToken")
                                    .setValue(sessionToken)
                                    .addOnSuccessListener(unused -> {

                                        SharedPreferences prefs =
                                                getSharedPreferences(
                                                        "app",
                                                        MODE_PRIVATE
                                                );

                                        prefs.edit().clear().apply();

                                        SharedPreferences.Editor editor =
                                                prefs.edit();

                                        editor.putBoolean("isLoggedIn", true);
                                        editor.putString("role", "user");
                                        editor.putString("phone", phone);
                                        editor.putString("sessionToken", sessionToken);
                                        editor.putString("userKey", snapshot.getKey());

                                        editor.apply();

                                        Intent intent =
                                                new Intent(
                                                        User_Login_Activity.this,
                                                        User_Maps_Activity.class
                                                );

                                        startActivity(intent);

                                        finish();
                                    });

                            break;
                        }
                    }
                }

                if (!loginSuccess) {

                    if (!phoneFound) {

                        Toast.makeText(
                                User_Login_Activity.this,
                                "Phone number not registered",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                User_Login_Activity.this,
                                "Incorrect password",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }

            } else {

                Toast.makeText(
                        User_Login_Activity.this,
                        "Database Error",
                        Toast.LENGTH_SHORT
                ).show();
            }

        });
    }
}