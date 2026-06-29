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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ViewFlipper;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

public class Forgot_Password_Activity extends AppCompatActivity {

    private EditText phoneEditText;

    private ViewFlipper viewFlipper;

    private Button resetButton;

    private FirebaseAuth mAuth;

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

            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        setContentView(R.layout.activity_forgot_password);

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
        // Views
        // =========================

        phoneEditText = findViewById(R.id.phoneEditText);

        resetButton = findViewById(R.id.resetButton);


        viewFlipper = findViewById(R.id.viewFlipper);

        viewFlipper.setFlipInterval(3000); // 3 seconds
        viewFlipper.startFlipping();

        // =========================
        // Firebase Auth
        // =========================

        mAuth = FirebaseAuth.getInstance();

        // =========================
        // Reset Button
        // =========================

        resetButton.setOnClickListener(v -> sendOTP());
    }

    // =========================
    // Send OTP
    // =========================

    private void sendOTP() {

        String phone = phoneEditText.getText()
                .toString()
                .trim();

        // =========================
        // Validation
        // =========================

        if (TextUtils.isEmpty(phone)) {

            phoneEditText.setError("Enter Phone Number");

            phoneEditText.requestFocus();

            return;
        }

        if (phone.length() != 10) {

            phoneEditText.setError("Enter Valid Phone Number");

            phoneEditText.requestFocus();

            return;
        }

        // =========================
        // Firebase OTP
        // =========================

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91" + phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // =========================
    // OTP Callbacks
    // =========================

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(
                        @NonNull PhoneAuthCredential credential
                ) {

                    Toast.makeText(
                            Forgot_Password_Activity.this,
                            "Verification Completed",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onVerificationFailed(
                        @NonNull FirebaseException e
                ) {

                    Toast.makeText(
                            Forgot_Password_Activity.this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }

                @Override
                public void onCodeSent(
                        @NonNull String verificationId,
                        @NonNull PhoneAuthProvider.ForceResendingToken token
                ) {

                    super.onCodeSent(verificationId, token);

                    Toast.makeText(
                            Forgot_Password_Activity.this,
                            "OTP Sent Successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    // =========================
                    // Open OTP Screen
                    // =========================

                    Intent intent = new Intent(
                            Forgot_Password_Activity.this,
                            OTP_Activity.class
                    );

                    intent.putExtra(
                            "verificationId",
                            verificationId
                    );

                    intent.putExtra(
                            "phone",
                            phoneEditText.getText()
                                    .toString()
                                    .trim()
                    );

                    intent.putExtra(
                            "from",
                            "forgot_password"
                    );

                    startActivity(intent);
                }
            };
}