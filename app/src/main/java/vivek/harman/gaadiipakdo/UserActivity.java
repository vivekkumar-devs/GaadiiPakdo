package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;

public class UserActivity extends AppCompatActivity {

    EditText userId, password;
    Button loginBtn;
    TextView registerText;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ STATUS BAR FIX (Same as other activities)
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

        setContentView(R.layout.activity_user);

        userId = findViewById(R.id.userIdd);
        password = findViewById(R.id.etuserPassword);
        loginBtn = findViewById(R.id.loginBtnn);
        registerText = findViewById(R.id.registerTextt);

        auth = FirebaseAuth.getInstance();

        // ✅ AUTO LOGIN CHECK
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String role = getSharedPreferences("app", MODE_PRIVATE)
                    .getString("role", "");

            if (role.equals("user")) {
                goToUserMap();
            }
        }

        loginBtn.setOnClickListener(v -> {

            String email = userId.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                userId.setError("Enter Email");
                return;
            }

            if (TextUtils.isEmpty(pass)) {
                password.setError("Enter Password");
                return;
            }

            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            // ✅ SAVE ROLE
                            getSharedPreferences("app", MODE_PRIVATE)
                                    .edit()
                                    .putString("role", "user")
                                    .apply();

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                            goToUserMap();
                        } else {
                            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        registerText.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void goToUserMap() {
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }
}