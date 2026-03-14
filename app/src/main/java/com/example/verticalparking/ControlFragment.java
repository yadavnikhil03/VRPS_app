package com.example.verticalparking;

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
        
        binding.motorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Simulate start transition
                binding.rpmProgress.setProgress(85, true);
                binding.rpmValueText.setText("1250.0");
                binding.tvTemp.setText("45°C");
            } else {
                binding.rpmProgress.setProgress(0, true);
                binding.rpmValueText.setText("0.0");
                binding.tvTemp.setText("32°C");
            }
        });

        binding.btnEmergencyStop.setOnClickListener(v -> {
            binding.motorSwitch.setChecked(false);
            binding.rpmProgress.setProgress(0, true);
            binding.rpmValueText.setText("0.0");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
