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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    EditText name, userId, password, confirmPassword;
    Button registerBtn;
    TextView tvuserlogin;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ STATUS BAR FIX
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

        setContentView(R.layout.activity_register);

        // Bind views
        name = findViewById(R.id.name);
        userId = findViewById(R.id.userIde);
        password = findViewById(R.id.passworddd);
        confirmPassword = findViewById(R.id.confirmPassword);
        registerBtn = findViewById(R.id.registerBtn);
        tvuserlogin = findViewById(R.id.tvuserlogin);

        auth = FirebaseAuth.getInstance();

        // ✅ REGISTER BUTTON
        registerBtn.setOnClickListener(v -> {

            String n = name.getText().toString().trim();
            String email = userId.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String cpass = confirmPassword.getText().toString().trim();

            // 🔍 VALIDATION (Better UX)
            if (TextUtils.isEmpty(n)) {
                name.setError("Enter Name");
                name.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(email)) {
                userId.setError("Enter Email");
                userId.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(pass)) {
                password.setError("Enter Password");
                password.requestFocus();
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

            if (pass.length() < 6) {
                password.setError("Minimum 6 characters required");
                password.requestFocus();
                return;
            }

            // 🔥 FIREBASE SIGNUP
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            Toast.makeText(this,
                                    "Registration Successful",
                                    Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(RegisterActivity.this, UserActivity.class));
                            finish();

                        } else {
                            Toast.makeText(this,
                                    "Error: " +
                                            (task.getException() != null ?
                                                    task.getException().getMessage() :
                                                    "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // ✅ LOGIN LINK
        tvuserlogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, UserActivity.class));
            finish();
        });
    }
}