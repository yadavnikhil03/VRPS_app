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
import com.example.verticalparking.databinding.ActivityRetrieveBinding;
import com.example.verticalparking.models.GenericResponse;
import com.example.verticalparking.models.RetrieveRequest;
import com.example.verticalparking.utils.PinUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RetrieveActivity extends AppCompatActivity {

    private ActivityRetrieveBinding binding;
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
        binding = ActivityRetrieveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = userPrefs.getString("userId", "");

        // Toolbar back navigation
        binding.retrieveToolbar.setNavigationOnClickListener(v -> finish());

        // NFC Demo scan
        binding.btnScanRetrieveTag.setOnClickListener(v -> loadParkedVehicle());

        // Retrieve Confirm
        binding.btnRetrieveConfirm.setOnClickListener(v -> attemptRetrieve());
    }

    /**
     * Loads the currently parked vehicle info from Room DB and fills the form.
     */
    private void loadParkedVehicle() {
        UserVehicle vehicle = db.userVehicleDao().getByUserId(currentUserId);
        if (vehicle != null && vehicle.isParked()) {
            binding.etRetrievePlate.setText(vehicle.getLicensePlate());
            binding.cardRetrieveStatus.setVisibility(View.VISIBLE);
            binding.retrieveDisplay.setText(vehicle.getLicensePlate());
            binding.tvRetrieveState.setText("Vehicle in Slot " + vehicle.getSlotNumber() + ". Enter your PIN to retrieve.");
            Toast.makeText(this, "Vehicle found — enter PIN", Toast.LENGTH_SHORT).show();
        } else {
            binding.cardRetrieveStatus.setVisibility(View.VISIBLE);
            binding.retrieveDisplay.setText("---");
            binding.tvRetrieveState.setText("No parked vehicle found for your account.");
            Toast.makeText(this, "No active parking session", Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptRetrieve() {
        binding.retrievePlateInputLayout.setError(null);
        binding.pinInputLayout.setError(null);

        String plate = String.valueOf(binding.etRetrievePlate.getText()).trim();
        String pin = String.valueOf(binding.etSecurityPin.getText()).trim();

        // --- Validation ---
        if (plate.isEmpty()) {
            binding.retrievePlateInputLayout.setError("License plate required");
            return;
        }

        if (!PinUtils.isValidPin(pin)) {
            binding.pinInputLayout.setError("PIN must be exactly 4 digits");
            return;
        }

        // --- Look up vehicle in Room DB ---
        UserVehicle vehicle = db.userVehicleDao().getParkedByPlate(plate);
        if (vehicle == null) {
            binding.retrievePlateInputLayout.setError("No parked vehicle found with this plate");
            showStatus("---", "No vehicle found. Check the plate number.");
            return;
        }

        // --- Verify PIN locally ---
        if (!PinUtils.verifyPin(pin, vehicle.getPinHash())) {
            binding.pinInputLayout.setError("Incorrect PIN");
            showStatus(vehicle.getLicensePlate(), "PIN verification failed. Try again.");
            return;
        }

        // --- Send request to ESP32 ---
        setLoadingState(true, "Sending retrieval request...");

        ApiService api = RetrofitClient.getApiService(this);
        RetrieveRequest request = new RetrieveRequest(vehicle.getUserId(), pin);

        api.retrieveVehicle(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    completeRetrieval(vehicle);
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage() : "Retrieval request failed";
                    setLoadingState(false, msg);
                    Toast.makeText(RetrieveActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                // ESP unreachable — retrieve locally
                handleOfflineRetrieve(vehicle);
            }
        });
    }

    /**
     * Completes the retrieval: updates Room DB, clears SharedPreferences, logs event.
     */
    private void completeRetrieval(UserVehicle vehicle) {
        int slot = vehicle.getSlotNumber();
        String plate = vehicle.getLicensePlate();

        // Update Room DB
        vehicle.setParked(false);
        vehicle.setSlotNumber(0);
        vehicle.setPinHash("");
        db.userVehicleDao().insertOrUpdate(vehicle);

        // Update SharedPreferences
        clearParkingSession(slot);

        SessionStore.addEvent(this, "Vehicle retrieved", plate + " ← Slot " + slot);

        setLoadingState(false, null);
        showStatus(plate, "Vehicle retrieved from Slot " + slot + ". Platform is rotating.");
        Toast.makeText(this, "Vehicle retrieved!", Toast.LENGTH_SHORT).show();

        binding.getRoot().postDelayed(this::finish, 2000);
    }

    /**
     * Fallback for when ESP32 is unreachable.
     */
    private void handleOfflineRetrieve(UserVehicle vehicle) {
        completeRetrieval(vehicle);
        binding.tvRetrieveState.setText("Retrieved locally (ESP offline). Slot " + vehicle.getSlotNumber());
    }

    private void clearParkingSession(int slot) {
        SharedPreferences parkingPrefs = getSharedPreferences("ParkingSession", MODE_PRIVATE);
        int available = parkingPrefs.getInt("available_slots", 20);
        int updatedAvailable = Math.min(20, available + 1);
        parkingPrefs.edit()
                .putBoolean("is_parked", false)
                .remove("plate")
                .remove("pin")
                .remove("slot")
                .putInt("available_slots", updatedAvailable)
                .apply();
    }

    private void setLoadingState(boolean loading, String message) {
        binding.btnRetrieveConfirm.setEnabled(!loading);
        binding.btnScanRetrieveTag.setEnabled(!loading);
        binding.cardRetrieveStatus.setVisibility(View.VISIBLE);
        if (loading) {
            binding.retrieveProgress.setVisibility(View.VISIBLE);
            binding.retrieveDisplay.setText("...");
        } else {
            binding.retrieveProgress.setVisibility(View.GONE);
        }
        if (message != null && !message.isEmpty()) {
            binding.tvRetrieveState.setText(message);
        }
    }

    private void showStatus(String display, String state) {
        binding.cardRetrieveStatus.setVisibility(View.VISIBLE);
        binding.retrieveDisplay.setText(display);
        binding.tvRetrieveState.setText(state);
    }
}
