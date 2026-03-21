package com.example.verticalparking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.verticalparking.api.ApiService;
import com.example.verticalparking.api.RetrofitClient;
import com.example.verticalparking.database.AppDatabase;
import com.example.verticalparking.database.UserVehicle;
import com.example.verticalparking.databinding.ActivityParkBinding;
import com.example.verticalparking.models.ParkRequest;
import com.example.verticalparking.models.ParkResponse;
import com.example.verticalparking.utils.PinUtils;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParkActivity extends AppCompatActivity {

    private ActivityParkBinding binding;
    private AppDatabase db;
    private String currentUserId;

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

        db = AppDatabase.getInstance(this);

        // Get or generate a userId
        SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = userPrefs.getString("userId", null);
        if (currentUserId == null) {
            currentUserId = UUID.randomUUID().toString();
            userPrefs.edit().putString("userId", currentUserId).apply();
        }

        // Toolbar back navigation
        binding.parkToolbar.setNavigationOnClickListener(v -> finish());

        // NFC Demo scan
        binding.btnScanParkingTag.setOnClickListener(v -> {
            binding.etPlate.setText("MH12AB1234");
            binding.etVehicleName.setText("My Car");
            Toast.makeText(this, "Demo scan applied", Toast.LENGTH_SHORT).show();
        });

        // Park Confirm
        binding.btnParkConfirm.setOnClickListener(v -> attemptPark());
    }

    private void attemptPark() {
        // Clear previous errors
        binding.plateInputLayout.setError(null);
        binding.parkingPinInputLayout.setError(null);
        binding.confirmPinInputLayout.setError(null);

        String plate = String.valueOf(binding.etPlate.getText()).trim();
        String vehicleName = String.valueOf(binding.etVehicleName.getText()).trim();
        String pin = String.valueOf(binding.etParkingPin.getText()).trim();
        String confirmPin = String.valueOf(binding.etConfirmParkingPin.getText()).trim();

        // --- Validation ---
        if (plate.isEmpty()) {
            binding.plateInputLayout.setError("License plate required");
            return;
        }

        if (!PinUtils.isValidPin(pin)) {
            binding.parkingPinInputLayout.setError("PIN must be exactly 4 digits");
            return;
        }

        if (!pin.equals(confirmPin)) {
            binding.confirmPinInputLayout.setError("PIN does not match");
            return;
        }

        // --- Check if user already has a parked vehicle ---
        UserVehicle existing = db.userVehicleDao().getByUserId(currentUserId);
        if (existing != null && existing.isParked()) {
            binding.tvParkingState.setText("You already have a vehicle parked (Slot " + existing.getSlotNumber() + "). Retrieve it first.");
            Toast.makeText(this, "A vehicle is already parked in your session", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Check if same plate is already parked ---
        UserVehicle samePlate = db.userVehicleDao().getParkedByPlate(plate);
        if (samePlate != null) {
            binding.plateInputLayout.setError("This plate is already parked");
            return;
        }

        // --- Send request to ESP32 ---
        setLoadingState(true, "Sending park request to system...");

        ApiService api = RetrofitClient.getApiService(this);
        ParkRequest request = new ParkRequest(currentUserId, pin, vehicleName);

        api.parkVehicle(request).enqueue(new Callback<ParkResponse>() {
            @Override
            public void onResponse(@NonNull Call<ParkResponse> call, @NonNull Response<ParkResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ParkResponse body = response.body();
                    int slot = body.getSlot();

                    // Save to Room DB
                    UserVehicle vehicle = new UserVehicle();
                    vehicle.setUserId(currentUserId);
                    vehicle.setLicensePlate(plate);
                    vehicle.setVehicleName(vehicleName.isEmpty() ? plate : vehicleName);
                    vehicle.setParked(true);
                    vehicle.setSlotNumber(slot);
                    vehicle.setPinHash(PinUtils.hashPin(pin));
                    vehicle.setParkedAt(System.currentTimeMillis());
                    db.userVehicleDao().insertOrUpdate(vehicle);

                    // Also save to SharedPreferences for backward compatibility
                    saveToParkingSession(plate, pin, String.valueOf(slot));

                    // Log to session history
                    SessionStore.addEvent(ParkActivity.this, "Vehicle parked", plate + " → Slot " + slot);

                    setLoadingState(false, null);
                    showSlotAllocation(slot);
                    Toast.makeText(ParkActivity.this, "Parked in Slot " + slot, Toast.LENGTH_SHORT).show();

                    // Finish after a brief delay so user sees the result
                    binding.getRoot().postDelayed(() -> finish(), 1500);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Park request failed";
                    setLoadingState(false, msg);
                    Toast.makeText(ParkActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkResponse> call, @NonNull Throwable t) {
                // ESP unreachable — fallback to local simulation
                handleOfflinePark(plate, vehicleName, pin);
            }
        });
    }

    /**
     * Fallback when ESP32 is not reachable.
     * Simulates parking locally so the app remains functional for demo/testing.
     */
    private void handleOfflinePark(String plate, String vehicleName, String pin) {
        SharedPreferences parkingPrefs = getSharedPreferences("ParkingSession", MODE_PRIVATE);
        int available = parkingPrefs.getInt("available_slots", 20);

        if (available <= 0) {
            setLoadingState(false, "No slots available");
            Toast.makeText(this, "No parking slots available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate slot assignment
        int slot = 21 - available;
        int updatedAvailable = available - 1;

        // Save to Room DB
        UserVehicle vehicle = new UserVehicle();
        vehicle.setUserId(currentUserId);
        vehicle.setLicensePlate(plate);
        vehicle.setVehicleName(vehicleName.isEmpty() ? plate : vehicleName);
        vehicle.setParked(true);
        vehicle.setSlotNumber(slot);
        vehicle.setPinHash(PinUtils.hashPin(pin));
        vehicle.setParkedAt(System.currentTimeMillis());
        db.userVehicleDao().insertOrUpdate(vehicle);

        // Save to SharedPreferences
        saveToParkingSession(plate, PinUtils.hashPin(pin), String.valueOf(slot));
        parkingPrefs.edit().putInt("available_slots", updatedAvailable).apply();

        SessionStore.addEvent(this, "Vehicle parked (offline)", plate + " → Slot " + slot);

        setLoadingState(false, "ESP offline — parked locally (Slot " + slot + ")");
        showSlotAllocation(slot);
        Toast.makeText(this, "Parked locally in Slot " + slot + " (ESP offline)", Toast.LENGTH_LONG).show();

        binding.getRoot().postDelayed(this::finish, 2000);
    }

    private void saveToParkingSession(String plate, String pin, String slot) {
        SharedPreferences parkingPrefs = getSharedPreferences("ParkingSession", MODE_PRIVATE);
        parkingPrefs.edit()
                .putBoolean("is_parked", true)
                .putString("plate", plate)
                .putString("pin", pin)
                .putString("slot", slot)
                .apply();
    }

    private void setLoadingState(boolean loading, String message) {
        binding.btnParkConfirm.setEnabled(!loading);
        binding.btnScanParkingTag.setEnabled(!loading);
        if (message != null && !message.isEmpty()) {
            binding.tvParkingState.setText(message);
        }
        if (loading) {
            binding.cardSlotInfo.setVisibility(View.VISIBLE);
            binding.parkingProgress.setVisibility(View.VISIBLE);
            binding.slotNumber.setText("...");
        } else {
            binding.parkingProgress.setVisibility(View.GONE);
        }
    }

    private void showSlotAllocation(int slot) {
        binding.cardSlotInfo.setVisibility(View.VISIBLE);
        binding.parkingProgress.setVisibility(View.GONE);
        binding.slotNumber.setText(String.valueOf(slot));
    }
}
