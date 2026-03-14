package com.example.verticalparking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.FragmentDashboardBinding;
import com.example.verticalparking.R;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        updateSystemStats();

        // Setup button listeners
        binding.btnParkLarge.setOnClickListener(v -> {
            startActivity(new android.content.Intent(getActivity(), ParkActivity.class));
        });

        binding.btnRetrieveLarge.setOnClickListener(v -> {
            startActivity(new android.content.Intent(getActivity(), RetrieveActivity.class));
        });
    }

    private void updateSystemStats() {
        // Simulated data for now
        int totalSlots = 20;
        int occupied = 8;
        int available = totalSlots - occupied;

        binding.tvAvailableSlots.setText(String.valueOf(available));
        binding.systemStatusText.setText("ONLINE");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
