package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
public class Reset_Password_Activity extends AppCompatActivity {

    // =========================
    // Views
    // =========================

    private EditText newPassword;

    private EditText confirmPassword;

    private Button resetPasswordButton;

    // =========================
    // Firebase
    // =========================

    private FirebaseAuth mAuth;

    private DatabaseReference usersRef;

    private DatabaseReference driversRef;

    // =========================
    // Data
    // =========================

    private String phone;

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

        setContentView(R.layout.activity_reset_password);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View root = findViewById(R.id.main);

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
        // =========================
        // Bind Views
        // =========================

        newPassword =
                findViewById(R.id.newPassword);

        confirmPassword =
                findViewById(R.id.confirmPassword);

        resetPasswordButton =
                findViewById(R.id.resetPasswordButton);

        ImageView newPasswordToggle =
                findViewById(R.id.newPasswordToggle);

        ImageView confirmPasswordToggle =
                findViewById(R.id.confirmPasswordToggle);

        final boolean[] newPasswordVisible = {false};

        newPasswordToggle.setOnClickListener(v -> {

            if (newPasswordVisible[0]) {

                newPassword.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                newPasswordToggle.setImageResource(
                        R.drawable.ic_visibility);

            } else {

                newPassword.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                newPasswordToggle.setImageResource(
                        R.drawable.ic_visibility_off);
            }

            newPasswordVisible[0] =
                    !newPasswordVisible[0];

            newPassword.setSelection(
                    newPassword.getText().length());
        });

        final boolean[] confirmPasswordVisible = {false};

        confirmPasswordToggle.setOnClickListener(v -> {

            if (confirmPasswordVisible[0]) {

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

            confirmPasswordVisible[0] =
                    !confirmPasswordVisible[0];

            confirmPassword.setSelection(
                    confirmPassword.getText().length());
        });

        // =========================
        // Firebase
        // =========================

        mAuth = FirebaseAuth.getInstance();

        usersRef = FirebaseDatabase
                .getInstance()
                .getReference("Users");

        driversRef = FirebaseDatabase
                .getInstance()
                .getReference("Drivers");

        // =========================
        // Intent Data
        // =========================

        phone =
                getIntent().getStringExtra(
                        "phone"
                );

        // =========================
        // Reset Button
        // =========================

        resetPasswordButton.setOnClickListener(v -> {

            resetPassword();
        });
    }
    private boolean isValidPassword(String password) {

        boolean hasUppercase =
                password.matches(".*[A-Z].*");

        boolean hasNumber =
                password.matches(".*\\d.*");

        boolean hasSpecial =
                password.matches(".*[^a-zA-Z0-9].*");

        return password.length() >= 8
                && hasUppercase
                && hasNumber
                && hasSpecial;
    }

    // =========================
    // Reset Password
    // =========================

    private void resetPassword() {

        String pass =
                newPassword.getText()
                        .toString()
                        .trim();

        String cpass =
                confirmPassword.getText()
                        .toString()
                        .trim();

        // =========================
        // Validation
        // =========================

        if (TextUtils.isEmpty(pass)) {

            newPassword.setError(
                    "Enter New Password"
            );

            newPassword.requestFocus();

            return;
        }

        if (!isValidPassword(pass)) {

            newPassword.requestFocus();

            Toast.makeText(
                    this,
                    "Password doesn't meet requirements",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (TextUtils.isEmpty(cpass)) {

            confirmPassword.setError(
                    "Confirm Password"
            );

            confirmPassword.requestFocus();

            return;
        }

        if (!pass.equals(cpass)) {

            confirmPassword.setError(
                    "Passwords do not match"
            );

            confirmPassword.requestFocus();

            return;
        }

        // =========================
        // Update Firebase Auth Password
        // =========================

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Session Expired. Verify OTP Again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }

        user.updatePassword(pass)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        // =========================
                        // Optional Database Update
                        // =========================

                        updatePasswordInDatabase(pass);

                    } else {

                        Toast.makeText(
                                Reset_Password_Activity.this,
                                "Failed: " +
                                        task.getException()
                                                .getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================
    // Update Database Password
    // =========================

    private void updatePasswordInDatabase(
            String password
    ) {

        usersRef.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                boolean found = false;

                for (DataSnapshot snapshot :
                        task.getResult().getChildren()) {

                    String dbPhone =
                            snapshot.child("phone")
                                    .getValue(String.class);

                    if (dbPhone != null &&
                            dbPhone.equals(phone)) {

                        found = true;

                        snapshot.getRef()
                                .child("password")
                                .setValue(password);

                        break;
                    }
                }

                if (!found) {

                    checkDrivers(password);

                } else {

                    successLogin();
                }

            } else {

                Toast.makeText(
                        this,
                        "Database Error",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // =========================
    // Check Driver Database
    // =========================

    private void checkDrivers(
            String password
    ) {

        driversRef.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                for (DataSnapshot snapshot :
                        task.getResult().getChildren()) {

                    String dbPhone =
                            snapshot.child("phone")
                                    .getValue(String.class);

                    if (dbPhone != null &&
                            dbPhone.equals(phone)) {

                        snapshot.getRef()
                                .child("password")
                                .setValue(password);

                        break;
                    }
                }

                successLogin();

            } else {

                Toast.makeText(
                        this,
                        "Database Error",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // =========================
    // Success
    // =========================

    private void successLogin() {

        Toast.makeText(
                this,
                "Password Reset Successful",
                Toast.LENGTH_SHORT
        ).show();

        FirebaseAuth.getInstance().signOut();

        Intent intent =
                new Intent(
                        Reset_Password_Activity.this,
                        Welcome_Activity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}