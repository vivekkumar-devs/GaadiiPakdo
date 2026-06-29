package vivek.harman.gaadiipakdo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Toast;
import android.widget.ViewFlipper;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

public class Welcome_Activity extends AppCompatActivity {

    CardView userCard, driverCard;
    ViewFlipper viewFlipper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

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

        userCard = findViewById(R.id.userCard);
        driverCard = findViewById(R.id.driverCard);
        viewFlipper = findViewById(R.id.viewFlipper);

        viewFlipper.setAutoStart(true);
        viewFlipper.setFlipInterval(3000);
        viewFlipper.startFlipping();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        String role = prefs.getString("role", "");

        userCard.setOnClickListener(v -> {
            if (role.equals("driver")) {
                Toast.makeText(this, "Drivers cannot access User panel", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, User_Login_Activity.class));
            }
        });

        driverCard.setOnClickListener(v -> {
            if (role.equals("user")) {
                Toast.makeText(this, "Users cannot access Driver panel", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, Driver_Login_Activity.class));
            }
        });
    }
}