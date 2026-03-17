package com.example.verticalparking;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.FragmentDashboardBinding;
import com.example.verticalparking.R;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private static final String PARKING_SESSION_PREFS = "ParkingSession";
    private static final String SETTINGS_PREFS = "Settings";
    private static final String KEY_AVAILABLE_SLOTS = "available_slots";
    private static final String KEY_REALTIME_ENABLED = "realtime_enabled";
    private static final String KEY_ESP_HOST = "esp_host";
    private static final String KEY_ESP_PATH = "esp_path";
    private static final String KEY_REALTIME_INTERVAL_MS = "realtime_interval_ms";
    private static final String KEY_DIAG_LAST_SYNC_MS = "diag_last_sync_ms";
    private static final String KEY_DIAG_FAILURE_STREAK = "diag_failure_streak";
    private static final String KEY_DIAG_DATA_SOURCE = "diag_data_source";
    private static final String KEY_DIAG_NEXT_RETRY_DELAY_MS = "diag_next_retry_delay_ms";
    private static final String KEY_DIAG_TREND = "diag_trend";
    private static final String KEY_CACHE_AVAILABLE_SLOTS = "cache_available_slots";
    private static final String KEY_CACHE_TOTAL_SLOTS = "cache_total_slots";
    private static final String KEY_CACHE_STATUS_TEXT = "cache_status_text";
    private static final String KEY_CACHE_ONLINE = "cache_online";
    private static final String KEY_CACHE_UPDATED_AT = "cache_updated_at";
    private static final int TOTAL_SLOTS = 20;
    private static final long REALTIME_POLL_MS = 3000L;
    private static final long REALTIME_MAX_BACKOFF_MS = 30000L;

    private final Handler realtimeHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService realtimeExecutor = Executors.newSingleThreadExecutor();
    private int currentTotalSlots = TOTAL_SLOTS;
    private int lastAnimatedAvailable = -1;
    private int lastAnimatedUtilization = -1;
    private int consecutiveRealtimeFailures = 0;

    private final Runnable realtimePollingRunnable = new Runnable() {
        @Override
        public void run() {
            pollRealtimeStatus();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        runPremiumEntranceAnimations();
        setupPremiumInteractions();
        updateSystemStats(false);

        binding.btnParkLarge.setOnClickListener(v -> {
            startActivity(new android.content.Intent(getActivity(), ParkActivity.class));
        });

        binding.btnRetrieveLarge.setOnClickListener(v -> {
            startActivity(new android.content.Intent(getActivity(), RetrieveActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSystemStats(false);
        startRealtimePolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRealtimePolling();
    }

    private void updateSystemStats(boolean animate) {
        SharedPreferences parkingPrefs = requireActivity().getSharedPreferences(PARKING_SESSION_PREFS, Context.MODE_PRIVATE);
        int available = clamp(parkingPrefs.getInt(KEY_AVAILABLE_SLOTS, currentTotalSlots), 0, currentTotalSlots);
        applyStatsToUi(available, currentTotalSlots, "", false, false, -1L, animate);
    }

    private void applyStatsToUi(int available,
                                int total,
                                String realtimeStatus,
                                boolean realtimeOnline,
                                boolean fromCache,
                                long cacheAgeMs,
                                boolean animate) {
        int occupied = total - available;
        int utilizationPercent = (occupied * 100) / Math.max(total, 1);

        binding.tvTotalSlots.setText("/ " + total);

        if (animate) {
            animateNumber(binding.tvAvailableSlots, lastAnimatedAvailable < 0 ? available : lastAnimatedAvailable, available, "");
            animateNumber(binding.tvHeroPercent, lastAnimatedUtilization < 0 ? utilizationPercent : lastAnimatedUtilization, utilizationPercent, "%");
            animateNumber(binding.tvUtilPercent, lastAnimatedUtilization < 0 ? utilizationPercent : lastAnimatedUtilization, utilizationPercent, "%");
        } else {
            binding.tvAvailableSlots.setText(String.valueOf(available));
            binding.tvHeroPercent.setText(utilizationPercent + "%");
            binding.tvUtilPercent.setText(utilizationPercent + "%");
        }

        lastAnimatedAvailable = available;
        lastAnimatedUtilization = utilizationPercent;

        binding.systemStatusText.setText(available > 0 ? "READY" : "FULL");
        binding.tvUtilSubtitle.setText(getString(R.string.utilization_subtitle_format, occupied));

        if (fromCache) {
            String normalized = realtimeStatus == null ? "" : realtimeStatus.trim();
            if (!normalized.isEmpty()) {
                binding.tvOpsDetail.setText(normalized);
            } else {
                binding.tvOpsDetail.setText(getString(R.string.live_sync_cached_data));
            }
            binding.tvRealtimeMode.setText(getString(R.string.live_sync_cached_age, toRelativeAge(cacheAgeMs)));
        } else if (realtimeOnline) {
            String normalized = realtimeStatus == null ? "" : realtimeStatus.trim();
            binding.tvOpsDetail.setText(normalized.isEmpty() ? getString(R.string.live_sync_connected) : normalized);
            binding.tvRealtimeMode.setText(getString(R.string.live_sync_connected));
        } else {
            binding.tvOpsDetail.setText(getString(R.string.live_sync_no_cache));
            binding.tvRealtimeMode.setText(getString(R.string.live_sync_offline));
        }
    }

    private void startRealtimePolling() {
        stopRealtimePolling();
        realtimeHandler.post(realtimePollingRunnable);
    }

    private void stopRealtimePolling() {
        realtimeHandler.removeCallbacks(realtimePollingRunnable);
    }

    private void pollRealtimeStatus() {
        if (!isAdded() || binding == null) {
            return;
        }

        SharedPreferences settingsPrefs = requireActivity().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        boolean enabled = settingsPrefs.getBoolean(KEY_REALTIME_ENABLED, false);
        if (!enabled) {
            consecutiveRealtimeFailures = 0;
            persistDiagnostics("off", 0, 0L);
            binding.tvRealtimeMode.setText(getString(R.string.live_sync_off));
            updateSystemStats(false);
            return;
        }

        if (!isNetworkAvailable()) {
            consecutiveRealtimeFailures++;
            applyCachedOrOfflineState();
            scheduleNextPoll(false);
            return;
        }

        binding.tvRealtimeMode.setText(getString(R.string.live_sync_connecting));
        String espHost = settingsPrefs.getString(KEY_ESP_HOST, "192.168.4.1");
        String espPath = settingsPrefs.getString(KEY_ESP_PATH, "/status");

        realtimeExecutor.execute(() -> {
            try {
                EspRealtimeClient.RealtimeStatus status = EspRealtimeClient.fetchStatus(espHost, espPath);
                consecutiveRealtimeFailures = 0;
                if (!isAdded()) {
                    return;
                }

                int total = status.totalSlots <= 0 ? TOTAL_SLOTS : status.totalSlots;
                int available = clamp(status.availableSlots, 0, total);
                currentTotalSlots = total;

                persistLiveDataToCache(available, total, status.status, status.online);
                persistDiagnostics("live", 0, 0L);

                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    applyStatsToUi(available, total, status.status, status.online, false, -1L, true);
                });
            } catch (Exception ignored) {
                consecutiveRealtimeFailures++;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null) {
                            return;
                        }
                        applyCachedOrOfflineState();
                    });
                }
            } finally {
                scheduleNextPoll(consecutiveRealtimeFailures == 0);
            }
        });
    }

    private void persistLiveDataToCache(int available, int total, String status, boolean online) {
        SharedPreferences parkingPrefs = requireActivity().getSharedPreferences(PARKING_SESSION_PREFS, Context.MODE_PRIVATE);
        parkingPrefs.edit()
                .putInt(KEY_AVAILABLE_SLOTS, available)
                .putInt(KEY_CACHE_AVAILABLE_SLOTS, available)
                .putInt(KEY_CACHE_TOTAL_SLOTS, total)
                .putString(KEY_CACHE_STATUS_TEXT, status == null ? "" : status)
                .putBoolean(KEY_CACHE_ONLINE, online)
                .putLong(KEY_CACHE_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    private void applyCachedOrOfflineState() {
        SharedPreferences parkingPrefs = requireActivity().getSharedPreferences(PARKING_SESSION_PREFS, Context.MODE_PRIVATE);
        long cachedAt = parkingPrefs.getLong(KEY_CACHE_UPDATED_AT, 0L);
        if (cachedAt > 0L) {
            int cachedTotal = parkingPrefs.getInt(KEY_CACHE_TOTAL_SLOTS, TOTAL_SLOTS);
            int cachedAvailable = clamp(parkingPrefs.getInt(KEY_CACHE_AVAILABLE_SLOTS, cachedTotal), 0, cachedTotal);
            String cachedStatus = parkingPrefs.getString(KEY_CACHE_STATUS_TEXT, "");
            boolean cachedOnline = parkingPrefs.getBoolean(KEY_CACHE_ONLINE, true);
            currentTotalSlots = cachedTotal;
            persistDiagnostics("cached", consecutiveRealtimeFailures, 0L);
            applyStatsToUi(cachedAvailable, cachedTotal, cachedStatus, cachedOnline, true,
                    System.currentTimeMillis() - cachedAt, false);
            return;
        }

        persistDiagnostics("offline", consecutiveRealtimeFailures, 0L);
        binding.tvRealtimeMode.setText(getString(R.string.live_sync_offline));
        binding.tvOpsDetail.setText(getString(R.string.live_sync_no_cache));
        updateSystemStats(false);
    }

    private void scheduleNextPoll(boolean success) {
        if (!isAdded() || binding == null) {
            return;
        }
        long nextPoll;
        long basePollMs = getConfiguredPollIntervalMs();
        if (success) {
            nextPoll = basePollMs;
        } else {
            int multiplier = 1 << Math.min(consecutiveRealtimeFailures, 4);
            nextPoll = Math.min(basePollMs * multiplier, REALTIME_MAX_BACKOFF_MS);
        }
        SharedPreferences settingsPrefs = requireActivity().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        settingsPrefs.edit().putLong(KEY_DIAG_NEXT_RETRY_DELAY_MS, nextPoll).apply();
        realtimeHandler.postDelayed(realtimePollingRunnable, nextPoll);
    }

    private long getConfiguredPollIntervalMs() {
        SharedPreferences settingsPrefs = requireActivity().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        int interval = settingsPrefs.getInt(KEY_REALTIME_INTERVAL_MS, (int) REALTIME_POLL_MS);
        if (interval < 1500) {
            return 1500L;
        }
        return Math.min(interval, 10000);
    }

    private void persistDiagnostics(String source, int failureStreak, long lastSyncOverride) {
        SharedPreferences settingsPrefs = requireActivity().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        long lastSyncMs = lastSyncOverride > 0L ? lastSyncOverride : System.currentTimeMillis();
        String existingTrend = settingsPrefs.getString(KEY_DIAG_TREND, "");
        String nextTrend = updateTrend(existingTrend, source);
        settingsPrefs.edit()
                .putString(KEY_DIAG_DATA_SOURCE, source)
                .putInt(KEY_DIAG_FAILURE_STREAK, Math.max(0, failureStreak))
                .putLong(KEY_DIAG_LAST_SYNC_MS, lastSyncMs)
                .putString(KEY_DIAG_TREND, nextTrend)
                .apply();
    }

    private String updateTrend(String currentTrend, String source) {
        if ("off".equalsIgnoreCase(source)) {
            return "";
        }
        char marker;
        if ("live".equalsIgnoreCase(source)) {
            marker = 'S';
        } else if ("cached".equalsIgnoreCase(source)) {
            marker = 'C';
        } else {
            marker = 'F';
        }

        String base = currentTrend == null ? "" : currentTrend;
        String next = base + marker;
        int maxLen = 14;
        if (next.length() > maxLen) {
            next = next.substring(next.length() - maxLen);
        }
        return next;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private String toRelativeAge(long ageMs) {
        if (ageMs <= 0) {
            return getString(R.string.live_sync_age_now);
        }
        long seconds = ageMs / 1000L;
        if (seconds < 60) {
            return getString(R.string.live_sync_age_seconds, seconds);
        }
        long minutes = seconds / 60L;
        if (minutes < 60) {
            return getString(R.string.live_sync_age_minutes, minutes);
        }
        long hours = minutes / 60L;
        return getString(R.string.live_sync_age_hours, hours);
    }

    private void runPremiumEntranceAnimations() {
        animateCard(binding.cardHero, 0);
        animateCard(binding.rowQuickActions, 90);
        animateCard(binding.cardUtilization, 160);
        animateCard(binding.cardOperational, 220);
    }

    private void setupPremiumInteractions() {
        setupScaleTouch(binding.btnParkLarge);
        setupScaleTouch(binding.btnRetrieveLarge);
    }

    private void setupScaleTouch(View target) {
        target.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }

    private void animateCard(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(28f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(420)
                .start();
    }

    private void animateNumber(TextView target, int start, int end, String suffix) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(360);
        animator.addUpdateListener(valueAnimator -> {
            int value = (int) valueAnimator.getAnimatedValue();
            target.setText(String.format(Locale.getDefault(), "%d%s", value, suffix));
        });
        animator.start();
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    @Override
    public void onDestroyView() {
        stopRealtimePolling();
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realtimeExecutor.shutdownNow();
    }
}
