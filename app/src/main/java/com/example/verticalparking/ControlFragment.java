package com.example.verticalparking;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.FragmentControlBinding;
import com.example.verticalparking.R;

public class ControlFragment extends Fragment {

    private FragmentControlBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentControlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshVehicleStatus();

        binding.btnStartParking.setOnClickListener(v ->
            startActivity(new Intent(requireActivity(), ParkActivity.class))
        );

        binding.btnStartRetrieval.setOnClickListener(v ->
            startActivity(new Intent(requireActivity(), RetrieveActivity.class))
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVehicleStatus();
    }

    private void refreshVehicleStatus() {
        SharedPreferences parkingPrefs = requireActivity().getSharedPreferences("ParkingSession", android.content.Context.MODE_PRIVATE);
        boolean isParked = parkingPrefs.getBoolean("is_parked", false);
        if (isParked) {
            String plate = parkingPrefs.getString("plate", "Unknown");
            String slot = parkingPrefs.getString("slot", "-");
            binding.tvVehicleState.setText("Vehicle parked now");
            binding.tvVehicleHint.setText("" + plate + " in slot " + slot + ". Use Retrieve when ready.");
        } else {
            binding.tvVehicleState.setText("No active parking session");
            binding.tvVehicleHint.setText("Scan QR/RFID at kiosk to continue.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
