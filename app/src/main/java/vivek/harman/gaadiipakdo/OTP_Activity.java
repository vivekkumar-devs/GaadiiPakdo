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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTP_Activity extends AppCompatActivity {

    // =========================
    // Views
    // =========================

    private EditText otpEditText;

    private Button verifyButton;

    private TextView phoneText;

    private TextView resendOtpText;

    // =========================
    // Firebase
    // =========================

    private FirebaseAuth mAuth;

    // =========================
    // Data
    // =========================

    private String verificationId;

    private String phone;

    private String from;

    // =========================
    // Resend Token
    // =========================

    private PhoneAuthProvider.ForceResendingToken resendToken;

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

        setContentView(R.layout.activity_otp);

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

        otpEditText =
                findViewById(R.id.otpEditText);

        verifyButton =
                findViewById(R.id.verifyButton);

        phoneText =
                findViewById(R.id.phoneText);

        resendOtpText =
                findViewById(R.id.resendOtpText);

        // =========================
        // Firebase
        // =========================

        mAuth = FirebaseAuth.getInstance();

        // =========================
        // Intent Data
        // =========================

        verificationId =
                getIntent().getStringExtra(
                        "verificationId"
                );

        phone =
                getIntent().getStringExtra(
                        "phone"
                );

        from =
                getIntent().getStringExtra(
                        "from"
                );

        // =========================
        // Show Phone
        // =========================

        if (phone != null) {

            phoneText.setText("+91 " + phone);
        }

        // =========================
        // Verify Button
        // =========================

        verifyButton.setOnClickListener(v -> {

            verifyOTP();
        });

        // =========================
        // Resend OTP
        // =========================

        resendOtpText.setOnClickListener(v -> {

            resendOTP();
        });
    }

    // =========================
    // Verify OTP
    // =========================

    private void verifyOTP() {

        String otp =
                otpEditText.getText()
                        .toString()
                        .trim();

        // =========================
        // Validation
        // =========================

        if (TextUtils.isEmpty(otp)) {

            otpEditText.setError(
                    "Enter OTP"
            );

            otpEditText.requestFocus();

            return;
        }

        if (otp.length() != 6) {

            otpEditText.setError(
                    "Enter Valid OTP"
            );

            otpEditText.requestFocus();

            return;
        }

        // =========================
        // Create Credential
        // =========================

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(
                        verificationId,
                        otp
                );

        // =========================
        // Verify Credential
        // =========================

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                OTP_Activity.this,
                                "OTP Verified",
                                Toast.LENGTH_SHORT
                        ).show();

                        // =========================
                        // Forgot Password Flow
                        // =========================

                        if ("forgot_password"
                                .equals(from)) {

                            Intent intent =
                                    new Intent(
                                            OTP_Activity.this,
                                            Reset_Password_Activity.class
                                    );

                            intent.putExtra(
                                    "phone",
                                    phone
                            );

                            startActivity(intent);

                            finish();
                        }

                    } else {

                        Toast.makeText(
                                OTP_Activity.this,
                                "Invalid OTP",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // =========================
    // Resend OTP
    // =========================

    private void resendOTP() {

        if (phone == null) {

            Toast.makeText(
                    this,
                    "Phone Number Missing",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(
                                "+91" + phone
                        )
                        .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                        )
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider
                .verifyPhoneNumber(options);

        Toast.makeText(
                this,
                "OTP Resent",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================
    // Callbacks
    // =========================

    private final
    PhoneAuthProvider
            .OnVerificationStateChangedCallbacks
            callbacks =
            new PhoneAuthProvider
                    .OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(
                        @NonNull
                        PhoneAuthCredential credential
                ) {

                }

                @Override
                public void onVerificationFailed(
                        @NonNull
                        FirebaseException e
                ) {

                    Toast.makeText(
                            OTP_Activity.this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }

                @Override
                public void onCodeSent(
                        @NonNull String newVerificationId,
                        @NonNull PhoneAuthProvider
                                .ForceResendingToken token
                ) {

                    super.onCodeSent(
                            newVerificationId,
                            token
                    );

                    verificationId =
                            newVerificationId;

                    resendToken = token;
                }
            };
}