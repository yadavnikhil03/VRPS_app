package com.example.verticalparking;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.verticalparking.databinding.ActivityParkBinding;
import com.example.verticalparking.R;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ParkActivity extends AppCompatActivity {
    private ActivityParkBinding binding;
    private static final String KEY_AVAILABLE_SLOTS = "available_slots";
    private static final int TOTAL_SLOTS = 20;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);

        super.onCreate(savedInstanceState);
        binding = ActivityParkBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.parkToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        binding.btnScanParkingTag.setOnClickListener(v -> {
            binding.etPlate.setText("MH12AB1234");
            binding.slotNumber.setText("A-04");
            binding.tvParkingState.setText("Demo data applied. You can edit details before confirming.");
            Toast.makeText(this, "Demo scan applied", Toast.LENGTH_SHORT).show();
        });

        binding.btnParkConfirm.setOnClickListener(v -> {
            String plate = String.valueOf(binding.etPlate.getText()).trim();
            String pin = String.valueOf(binding.etParkingPin.getText()).trim();
            String confirmPin = String.valueOf(binding.etConfirmParkingPin.getText()).trim();

            binding.parkingPinInputLayout.setError(null);
            binding.confirmPinInputLayout.setError(null);

            if (plate.isEmpty()) {
                binding.plateInputLayout.setError("Vehicle number required");
                return;
            }

            binding.plateInputLayout.setError(null);

            if (pin.length() != 4) {
                binding.parkingPinInputLayout.setError("PIN must be 4 digits");
                return;
            }

            if (!pin.equals(confirmPin)) {
                binding.confirmPinInputLayout.setError("PIN does not match");
                return;
            }

            SharedPreferences parkingPrefs = getSharedPreferences("ParkingSession", MODE_PRIVATE);

            boolean alreadyParked = parkingPrefs.getBoolean("is_parked", false);
            if (alreadyParked) {
                Toast.makeText(this, "A vehicle is already parked in your active session", Toast.LENGTH_SHORT).show();
                return;
            }

            int available = parkingPrefs.getInt(KEY_AVAILABLE_SLOTS, TOTAL_SLOTS);
            if (available <= 0) {
                Toast.makeText(this, "No slots available right now", Toast.LENGTH_SHORT).show();
                return;
            }

            int updatedAvailable = available - 1;

            parkingPrefs.edit()
                    .putBoolean("is_parked", true)
                    .putString("plate", plate)
                    .putString("slot", String.valueOf(binding.slotNumber.getText()))
                    .putString("pin", pin)
                    .putInt(KEY_AVAILABLE_SLOTS, updatedAvailable)
                    .apply();

                SessionStore.addEvent(
                    this,
                    "Vehicle parked",
                    plate + " - Slot " + binding.slotNumber.getText()
                );

            Toast.makeText(this, "Parking request submitted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
