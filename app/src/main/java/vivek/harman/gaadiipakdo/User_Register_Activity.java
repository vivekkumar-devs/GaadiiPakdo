package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class User_Register_Activity extends AppCompatActivity {

    private EditText name, userPhone, password, confirmPassword;

    private Button registerBtn;

    private TextView tvuserlogin;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // =========================
        // Status Bar
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

        setContentView(R.layout.activity_user_register);

        TextView tvuserlogin = findViewById(R.id.tvuserlogin);

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

        tvuserlogin.setText(spannableString);

        // =========================
        // Views
        // =========================

        name = findViewById(R.id.name);

        userPhone = findViewById(R.id.userPhone);

        password = findViewById(R.id.passworddd);

        confirmPassword = findViewById(R.id.confirmPassword);

        registerBtn = findViewById(R.id.registerBtn);

        tvuserlogin = findViewById(R.id.tvuserlogin);

        ImageView passwordToggle =
                findViewById(R.id.passwordToggle);

        ImageView confirmPasswordToggle =
                findViewById(R.id.confirmPasswordToggle);

        final boolean[] passwordVisible = {false};

        passwordToggle.setOnClickListener(v -> {

            if (passwordVisible[0]) {

                password.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                passwordToggle.setImageResource(
                        R.drawable.ic_visibility);

            } else {

                password.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                passwordToggle.setImageResource(
                        R.drawable.ic_visibility_off);
            }

            passwordVisible[0] = !passwordVisible[0];

            password.setSelection(
                    password.getText().length());
        });

        final boolean[] confirmVisible = {false};

        confirmPasswordToggle.setOnClickListener(v -> {

            if (confirmVisible[0]) {

                confirmPassword.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                confirmPasswordToggle.setImageResource(
                        R.drawable.ic_visibility);

            } else {

                confirmPassword.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                confirmPasswordToggle.setImageResource(
                        R.drawable.ic_visibility_off);
            }

            confirmVisible[0] = !confirmVisible[0];

            confirmPassword.setSelection(
                    confirmPassword.getText().length());
        });

        // =========================
        // Firebase Database
        // =========================

        usersRef = FirebaseDatabase.getInstance()
                .getReference("Users");

        // =========================
        // Register Button
        // =========================

        registerBtn.setOnClickListener(v -> registerUser());

        // =========================
        // Login Redirect
        // =========================

        tvuserlogin.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            User_Register_Activity.this,
                            User_Login_Activity.class
                    )
            );

            finish();
        });
    }

    // =========================
    // Register Function
    // =========================

    private void registerUser() {

        String n = name.getText().toString().trim();

        String phone = userPhone.getText().toString().trim();

        String pass = password.getText().toString().trim();

        String cpass = confirmPassword.getText().toString().trim();

        // =========================
        // Validation
        // =========================

        if (TextUtils.isEmpty(n)) {

            name.setError("Enter Name");
            name.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {

            userPhone.setError("Enter Mobile Number");
            userPhone.requestFocus();
            return;
        }

        if (phone.length() != 10) {

            userPhone.setError("Enter Valid Mobile Number");
            userPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pass)) {

            password.setError("Enter Password");
            password.requestFocus();
            return;
        }
        String passwordError = getPasswordError(pass);

        if (passwordError != null) {

            password.requestFocus();

            Toast.makeText(
                    this,
                    passwordError,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (TextUtils.isEmpty(cpass)) {

            confirmPassword.setError("Confirm Password");
            confirmPassword.requestFocus();
            return;
        }

        if (!pass.equals(cpass)) {

            confirmPassword.setError("Passwords do not match");
            confirmPassword.requestFocus();
            return;
        }

        // =========================
        // Check Existing User
        // =========================

        usersRef.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                boolean alreadyExists = false;

                for (DataSnapshot snapshot : task.getResult().getChildren()) {

                    String dbPhone = snapshot.child("phone")
                            .getValue(String.class);

                    if (dbPhone != null &&
                            dbPhone.equals(phone)) {

                        alreadyExists = true;

                        break;
                    }
                }

                if (alreadyExists) {

                    Toast.makeText(
                            User_Register_Activity.this,
                            "Phone Number Already Registered",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    saveUserToDatabase(n, phone, pass);
                }

            } else {

                Toast.makeText(
                        User_Register_Activity.this,
                        "Database Error",
                        Toast.LENGTH_SHORT
                ).show();
            }
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
    // Save User
    // =========================

    private void saveUserToDatabase(
            String name,
            String phone,
            String password
    ) {

        String uid = usersRef.push().getKey();

        HashMap<String, Object> userMap = new HashMap<>();

        userMap.put("name", name);

        userMap.put("phone", phone);

        userMap.put("password", password);

        userMap.put("role", "user");

        usersRef.child(uid)
                .setValue(userMap)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                User_Register_Activity.this,
                                "Registration Successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                User_Register_Activity.this,
                                User_Login_Activity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);

                        finish();

                    } else {

                        Toast.makeText(
                                User_Register_Activity.this,
                                "Registration Failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}