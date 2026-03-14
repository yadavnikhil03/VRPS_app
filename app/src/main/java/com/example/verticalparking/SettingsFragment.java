package com.example.verticalparking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.FragmentSettingsBinding;
import com.example.verticalparking.R;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        sharedPreferences = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupThemeToggles();
        setupLanguageToggles();
    }

    private void setupThemeToggles() {
        int savedTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            binding.themeToggleGroup.check(R.id.btnLightTheme);
        } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.themeToggleGroup.check(R.id.btnDarkTheme);
        } else {
            binding.themeToggleGroup.check(R.id.btnSystemTheme);
        }

        binding.themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int mode;
                if (checkedId == R.id.btnLightTheme) {
                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                } else if (checkedId == R.id.btnDarkTheme) {
                    mode = AppCompatDelegate.MODE_NIGHT_YES;
                } else {
                    mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
                
                sharedPreferences.edit().putInt("theme_mode", mode).apply();
                AppCompatDelegate.setDefaultNightMode(mode);
            }
        });
    }

    private void setupLanguageToggles() {
        // Simple implementation for demonstration
        // In a real app, this would involve updating the Locale and restarting the activity
        String lang = sharedPreferences.getString("language", "en");
        if (lang.equals("hi")) {
            binding.languageToggleGroup.check(R.id.btnLangHi);
        } else {
            binding.languageToggleGroup.check(R.id.btnLangEn);
        }

        binding.languageToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newLang = (checkedId == R.id.btnLangHi) ? "hi" : "en";
                String currentLang = sharedPreferences.getString("language", "en");
                
                if (!newLang.equals(currentLang)) {
                    LocaleHelper.setLocale(getActivity(), newLang);
                    getActivity().recreate(); // Restart activity to apply changes
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
