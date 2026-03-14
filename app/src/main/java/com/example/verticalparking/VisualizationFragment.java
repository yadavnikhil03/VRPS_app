package com.example.verticalparking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.FragmentVisualizationBinding;
import com.example.verticalparking.R;

public class VisualizationFragment extends Fragment {

    private FragmentVisualizationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVisualizationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Simulated visualization logic
        binding.activePlatformLabel.setText("SLOT-12 (MID-TOWER)");
        binding.rotationText.setText("ALIGNING...");
        binding.rotationText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
