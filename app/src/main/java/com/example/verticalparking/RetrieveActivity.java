package com.example.verticalparking;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.verticalparking.databinding.ActivityRetrieveBinding;
import com.example.verticalparking.R;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class RetrieveActivity extends AppCompatActivity {
    private ActivityRetrieveBinding binding;
    private static final String KEY_AVAILABLE_SLOTS = "available_slots";
    private static final int TOTAL_SLOTS = 20;
    private SharedPreferences parkingPrefs;
    private boolean vehicleFound;
    private String parkedPlate;
    private String parkedPin;
    private String parkedSlot;

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
        binding = ActivityRetrieveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.retrieveToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        parkingPrefs = getSharedPreferences("ParkingSession", MODE_PRIVATE);

        binding.btnScanRetrieveTag.setOnClickListener(v -> loadParkedVehicle(true));

        binding.btnRetrieveConfirm.setOnClickListener(v -> {
            loadParkedVehicle(false);
            if (!vehicleFound) {
                Toast.makeText(this, "No active parked vehicle found", Toast.LENGTH_SHORT).show();
                return;
            }

            String enteredPlate = String.valueOf(binding.etRetrievePlate.getText()).trim();
            if (enteredPlate.isEmpty()) {
                binding.retrievePlateInputLayout.setError("Plate number required");
                return;
            }

            if (!enteredPlate.equalsIgnoreCase(parkedPlate)) {
                binding.retrievePlateInputLayout.setError("Plate does not match parked vehicle");
                return;
            }

            binding.retrievePlateInputLayout.setError(null);

            String pin = String.valueOf(binding.etSecurityPin.getText()).trim();
            if (!pin.equals(parkedPin)) {
                binding.pinInputLayout.setError("Incorrect PIN");
                binding.tvRetrieveState.setText("PIN verification failed. Try again.");
                return;
            }

            binding.pinInputLayout.setError(null);
            binding.tvRetrieveState.setText("PIN verified. Platform is rotating and your car is coming down.");

            int available = parkingPrefs.getInt(KEY_AVAILABLE_SLOTS, TOTAL_SLOTS);
            int updatedAvailable = Math.min(TOTAL_SLOTS, available + 1);

                SessionStore.addEvent(
                    this,
                    "Vehicle retrieved",
                    parkedPlate + " - Slot " + parkedSlot
                );

            parkingPrefs.edit()
                    .putBoolean("is_parked", false)
                    .remove("plate")
                    .remove("pin")
                    .remove("slot")
                    .putInt(KEY_AVAILABLE_SLOTS, updatedAvailable)
                    .apply();

            Toast.makeText(this, "Retrieval approved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadParkedVehicle(boolean showToast) {
        boolean isParked = parkingPrefs.getBoolean("is_parked", false);
        if (!isParked) {
            vehicleFound = false;
            binding.retrieveDisplay.setText("- - -");
            binding.tvRetrieveState.setText("No parked vehicle found for this account.");
            if (showToast) {
                Toast.makeText(this, "No active parking session", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        parkedPlate = parkingPrefs.getString("plate", "");
        parkedPin = parkingPrefs.getString("pin", "");
        parkedSlot = parkingPrefs.getString("slot", "-");

        vehicleFound = true;
        binding.retrieveDisplay.setText(parkedPlate);
        binding.etRetrievePlate.setText(parkedPlate);
        binding.tvRetrieveState.setText("Vehicle in slot " + parkedSlot + ". Enter same plate and PIN.");
        if (showToast) {
            Toast.makeText(this, "Demo scan loaded", Toast.LENGTH_SHORT).show();
        }
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
