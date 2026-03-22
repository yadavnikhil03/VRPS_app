package com.example.verticalparking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.FragmentSettingsBinding;
import com.example.verticalparking.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String KEY_REALTIME_ENABLED = "realtime_enabled";
    private static final String KEY_ESP_HOST = "esp_host";
    private static final String KEY_ESP_PATH = "esp_path";
    private static final String KEY_REALTIME_INTERVAL_MS = "realtime_interval_ms";
    private static final String KEY_DIAG_LAST_SYNC_MS = "diag_last_sync_ms";
    private static final String KEY_DIAG_FAILURE_STREAK = "diag_failure_streak";
    private static final String KEY_DIAG_DATA_SOURCE = "diag_data_source";
    private static final String KEY_DIAG_NEXT_RETRY_DELAY_MS = "diag_next_retry_delay_ms";
    private static final String KEY_DIAG_TREND = "diag_trend";
    private static final int INTERVAL_FAST_MS = 2000;
    private static final int INTERVAL_BALANCED_MS = 3000;
    private static final int INTERVAL_POWER_SAVE_MS = 5000;
    private final ExecutorService connectionTestExecutor = Executors.newSingleThreadExecutor();

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
        setupRealtimeSync();
        refreshRealtimeDiagnostics();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRealtimeDiagnostics();
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

    private void setupRealtimeSync() {
        String savedHost = sharedPreferences.getString(KEY_ESP_HOST, "192.168.4.1");
        String savedPath = sharedPreferences.getString(KEY_ESP_PATH, "/status");
        boolean enabled = sharedPreferences.getBoolean(KEY_REALTIME_ENABLED, false);

        binding.inputEspHost.setText(savedHost);
        binding.inputEspPath.setText(savedPath);
        binding.switchRealtimeSync.setChecked(enabled);
        updateRealtimeUiState(enabled, savedHost);
        int savedInterval = sharedPreferences.getInt(KEY_REALTIME_INTERVAL_MS, INTERVAL_BALANCED_MS);
        int sliderValue = toSliderValue(savedInterval);
        binding.sliderRealtimeInterval.setValue(sliderValue);
        binding.tvRealtimeIntervalValue.setText(intervalLabelFor(sliderValue));
        binding.tvEspLatency.setText(R.string.realtime_latency_placeholder);
        binding.tvEspLastUpdated.setText(R.string.realtime_last_updated_placeholder);

        binding.sliderRealtimeInterval.addOnChangeListener((slider, value, fromUser) -> {
            int v = Math.round(value);
            binding.tvRealtimeIntervalValue.setText(intervalLabelFor(v));
            if (fromUser) {
                int intervalMs = toIntervalMs(v);
                sharedPreferences.edit().putInt(KEY_REALTIME_INTERVAL_MS, intervalMs).apply();
            }
        });

        binding.switchRealtimeSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_REALTIME_ENABLED, isChecked).apply();
            String host = binding.inputEspHost.getText() == null
                    ? ""
                    : binding.inputEspHost.getText().toString().trim();
            updateRealtimeUiState(isChecked, host);
        });

        binding.btnSaveEspConfig.setOnClickListener(v -> {
            String host = binding.inputEspHost.getText() == null
                    ? ""
                    : binding.inputEspHost.getText().toString().trim();
            String path = binding.inputEspPath.getText() == null
                    ? ""
                    : binding.inputEspPath.getText().toString().trim();

            if (host.isEmpty()) {
                binding.inputLayoutEspHost.setError(getString(R.string.esp_host_required));
                return;
            }

            binding.inputLayoutEspHost.setError(null);
            if (path.isEmpty()) {
                path = "/status";
            }

            sharedPreferences.edit()
                    .putString(KEY_ESP_HOST, host)
                    .putString(KEY_ESP_PATH, path)
                    .apply();

            updateRealtimeUiState(binding.switchRealtimeSync.isChecked(), host);
            Toast.makeText(requireContext(), R.string.realtime_config_saved, Toast.LENGTH_SHORT).show();
        });

        binding.btnTestEspConnection.setOnClickListener(v -> runConnectionTest());
    }

    private void refreshRealtimeDiagnostics() {
        String sourceKey = sharedPreferences.getString(KEY_DIAG_DATA_SOURCE, "off");
        int failureStreak = sharedPreferences.getInt(KEY_DIAG_FAILURE_STREAK, 0);
        long retryDelayMs = sharedPreferences.getLong(KEY_DIAG_NEXT_RETRY_DELAY_MS, 0L);
        long lastSyncMs = sharedPreferences.getLong(KEY_DIAG_LAST_SYNC_MS, 0L);
        String trend = sharedPreferences.getString(KEY_DIAG_TREND, "");

        int sourceLabelRes;
        if ("live".equalsIgnoreCase(sourceKey)) {
            sourceLabelRes = R.string.realtime_diag_source_live;
        } else if ("cached".equalsIgnoreCase(sourceKey)) {
            sourceLabelRes = R.string.realtime_diag_source_cached;
        } else if ("offline".equalsIgnoreCase(sourceKey)) {
            sourceLabelRes = R.string.realtime_diag_source_offline;
        } else {
            sourceLabelRes = R.string.realtime_diag_source_off;
        }

        String sourceLabel = getString(sourceLabelRes);
        binding.tvRealtimeDiagSource.setText(getString(R.string.realtime_diag_source_format, sourceLabel));
        binding.tvRealtimeDiagFailures.setText(getString(R.string.realtime_diag_failures_format, Math.max(0, failureStreak)));

        String retryText = retryDelayMs > 0L
                ? getString(R.string.realtime_diag_retry_format_ms, retryDelayMs)
                : getString(R.string.realtime_diag_retry_not_scheduled);
        binding.tvRealtimeDiagRetry.setText(retryText);

        String normalizedTrend = trend == null ? "" : trend.trim();
        binding.tvRealtimeDiagTrend.setText(buildColoredTrendText(normalizedTrend));
        binding.tvRealtimeDiagScore.setText(getString(
            R.string.realtime_diag_score_format,
            calculateSuccessRate(normalizedTrend)
        ));

        if (lastSyncMs > 0L) {
            String timeText = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(lastSyncMs));
            binding.tvEspLastUpdated.setText(getString(R.string.realtime_last_updated_format, timeText));
        }
    }

    private String toTrendGlyphs(String trend) {
        if (trend.isEmpty()) {
            return getString(R.string.realtime_diag_trend_empty);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trend.length(); i++) {
            char token = trend.charAt(i);
            if (token == 'S') {
                builder.append('●');
            } else if (token == 'C') {
                builder.append('◐');
            } else {
                builder.append('○');
            }
            if (i < trend.length() - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private CharSequence buildColoredTrendText(String trend) {
        String glyphs = toTrendGlyphs(trend);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(getString(R.string.realtime_diag_trend_label)).append(" ");
        int start = builder.length();
        builder.append(glyphs);

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.kinetic_primary);
        int tertiaryColor = ContextCompat.getColor(requireContext(), R.color.kinetic_secondary);
        int errorColor = ContextCompat.getColor(requireContext(), R.color.kinetic_error);

        for (int i = 0; i < glyphs.length(); i++) {
            char ch = glyphs.charAt(i);
            int color;
            if (ch == '●') {
                color = primaryColor;
            } else if (ch == '◐') {
                color = tertiaryColor;
            } else if (ch == '○') {
                color = errorColor;
            } else {
                continue;
            }
            int pos = start + i;
            builder.setSpan(new ForegroundColorSpan(color), pos, pos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    private int calculateSuccessRate(String trend) {
        if (trend.isEmpty()) {
            return 0;
        }
        int good = 0;
        int total = 0;
        for (int i = 0; i < trend.length(); i++) {
            char token = trend.charAt(i);
            if (token == 'S' || token == 'C' || token == 'F') {
                total++;
                if (token == 'S' || token == 'C') {
                    good++;
                }
            }
        }
        if (total == 0) {
            return 0;
        }
        return (good * 100) / total;
    }

    private void updateRealtimeUiState(boolean enabled, String host) {
        binding.inputLayoutEspHost.setEnabled(true);
        binding.inputLayoutEspPath.setEnabled(true);
        binding.inputEspHost.setEnabled(true);
        binding.inputEspPath.setEnabled(true);
        binding.btnSaveEspConfig.setEnabled(true);
        binding.btnTestEspConnection.setEnabled(true);

        if (enabled) {
            String normalizedHost = (host == null || host.trim().isEmpty()) ? "192.168.4.1" : host.trim();
            binding.tvEspSyncState.setText(getString(R.string.live_sync_host_format, normalizedHost));
        } else {
            binding.tvEspSyncState.setText(R.string.live_sync_off);
        }
    }

    private void runConnectionTest() {
        String host = binding.inputEspHost.getText() == null
                ? ""
                : binding.inputEspHost.getText().toString().trim();
        String path = binding.inputEspPath.getText() == null
                ? ""
                : binding.inputEspPath.getText().toString().trim();

        if (host.isEmpty()) {
            binding.inputLayoutEspHost.setError(getString(R.string.esp_host_required));
            return;
        }
        binding.inputLayoutEspHost.setError(null);

        if (path.isEmpty()) {
            path = "/status";
            binding.inputEspPath.setText(path);
        }

        binding.btnTestEspConnection.setEnabled(false);
        binding.tvEspSyncState.setText(R.string.realtime_testing);

        final String finalPath = path;
        connectionTestExecutor.execute(() -> {
            long start = SystemClock.elapsedRealtime();
            boolean testSuccess;
            try {
                EspRealtimeClient.fetchStatus(host, finalPath);
                testSuccess = true;
            } catch (Exception ignored) {
                testSuccess = false;
            }
            long elapsed = SystemClock.elapsedRealtime() - start;

            if (!isAdded()) {
                return;
            }

            long latencyMs = Math.max(1L, elapsed);
            final boolean success = testSuccess;
            String timeText = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date());
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                binding.btnTestEspConnection.setEnabled(true);
                if (success) {
                    binding.tvEspSyncState.setText(getString(R.string.realtime_test_success, latencyMs));
                    binding.tvEspLatency.setText(getString(R.string.realtime_latency_format, latencyMs));
                } else {
                    binding.tvEspSyncState.setText(R.string.realtime_test_failed);
                    binding.tvEspLatency.setText(R.string.realtime_latency_placeholder);
                }
                binding.tvEspLastUpdated.setText(getString(R.string.realtime_last_updated_format, timeText));
                refreshRealtimeDiagnostics();
            });
        });
    }

    private int toSliderValue(int intervalMs) {
        if (intervalMs <= INTERVAL_FAST_MS) {
            return 0;
        }
        if (intervalMs >= INTERVAL_POWER_SAVE_MS) {
            return 2;
        }
        return 1;
    }

    private int toIntervalMs(int sliderValue) {
        if (sliderValue <= 0) {
            return INTERVAL_FAST_MS;
        }
        if (sliderValue >= 2) {
            return INTERVAL_POWER_SAVE_MS;
        }
        return INTERVAL_BALANCED_MS;
    }

    private int intervalLabelFor(int sliderValue) {
        if (sliderValue <= 0) {
            return R.string.realtime_interval_fast;
        }
        if (sliderValue >= 2) {
            return R.string.realtime_interval_power_save;
        }
        return R.string.realtime_interval_balanced;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectionTestExecutor.shutdownNow();
    }
}
