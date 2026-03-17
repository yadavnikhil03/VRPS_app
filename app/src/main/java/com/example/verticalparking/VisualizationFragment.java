package com.example.verticalparking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.verticalparking.databinding.FragmentVisualizationBinding;
import com.example.verticalparking.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VisualizationFragment extends Fragment {

    private FragmentVisualizationBinding binding;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();
    private SlotMapAdapter slotMapAdapter;

    private static final String PARKING_SESSION_PREFS = "ParkingSession";
    private static final String SETTINGS_PREFS = "Settings";
    private static final String KEY_AVAILABLE_SLOTS = "available_slots";
    private static final String KEY_REALTIME_ENABLED = "realtime_enabled";
    private static final String KEY_ESP_HOST = "esp_host";
    private static final String KEY_ESP_PATH = "esp_path";
    private static final String KEY_REALTIME_INTERVAL_MS = "realtime_interval_ms";
    private static final int DEFAULT_TOTAL_SLOTS = 20;
    private static final long VISUALIZATION_REFRESH_MS = 3000L;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshTowerState();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVisualizationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSlotMap();
        renderFromLocalState(false, getString(R.string.tower_source_cached));
    }

    @Override
    public void onResume() {
        super.onResume();
        startRefreshing();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRefreshing();
    }

    private void setupSlotMap() {
        binding.slotRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        slotMapAdapter = new SlotMapAdapter();
        binding.slotRecyclerView.setAdapter(slotMapAdapter);
    }

    private void startRefreshing() {
        stopRefreshing();
        refreshHandler.post(refreshRunnable);
    }

    private void stopRefreshing() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void scheduleNextRefresh() {
        if (!isAdded() || binding == null) {
            return;
        }
        refreshHandler.postDelayed(refreshRunnable, getConfiguredRefreshIntervalMs());
    }

    private long getConfiguredRefreshIntervalMs() {
        SharedPreferences settingsPrefs = requireActivity().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        int interval = settingsPrefs.getInt(KEY_REALTIME_INTERVAL_MS, (int) VISUALIZATION_REFRESH_MS);
        if (interval < 1500) {
            return 1500L;
        }
        return Math.min(interval, 10000);
    }

    private void refreshTowerState() {
        if (!isAdded() || binding == null) {
            return;
        }

        SharedPreferences settingsPrefs = requireActivity().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        boolean realtimeEnabled = settingsPrefs.getBoolean(KEY_REALTIME_ENABLED, false);
        if (!realtimeEnabled) {
            renderFromLocalState(false, getString(R.string.tower_source_sync_off));
            scheduleNextRefresh();
            return;
        }

        String espHost = settingsPrefs.getString(KEY_ESP_HOST, "192.168.4.1");
        String espPath = settingsPrefs.getString(KEY_ESP_PATH, "/status");

        pollExecutor.execute(() -> {
            try {
                EspRealtimeClient.RealtimeStatus status = EspRealtimeClient.fetchStatus(espHost, espPath);
                if (!isAdded()) {
                    return;
                }
                int total = status.totalSlots <= 0 ? DEFAULT_TOTAL_SLOTS : status.totalSlots;
                int available = clamp(status.availableSlots, 0, total);
                SharedPreferences parkingPrefs = requireActivity().getSharedPreferences(PARKING_SESSION_PREFS, Context.MODE_PRIVATE);
                parkingPrefs.edit().putInt(KEY_AVAILABLE_SLOTS, available).apply();

                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    renderTowerState(available, total, true, getString(R.string.tower_source_live));
                });
            } catch (Exception ignored) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    renderFromLocalState(false, getString(R.string.tower_source_cached));
                });
            } finally {
                scheduleNextRefresh();
            }
        });
    }

    private void renderFromLocalState(boolean online, String sourceText) {
        SharedPreferences parkingPrefs = requireActivity().getSharedPreferences(PARKING_SESSION_PREFS, Context.MODE_PRIVATE);
        int available = clamp(parkingPrefs.getInt(KEY_AVAILABLE_SLOTS, DEFAULT_TOTAL_SLOTS), 0, DEFAULT_TOTAL_SLOTS);
        renderTowerState(available, DEFAULT_TOTAL_SLOTS, online, sourceText);
    }

    private void renderTowerState(int available, int total, boolean online, String sourceText) {
        int occupied = Math.max(0, total - available);
        binding.visSubtitle.setText(getString(R.string.tower_summary_format, available, total));
        binding.activePlatformLabel.setText(resolveActivePlatformLabel(occupied));
        binding.rotationText.setText(online ? getString(R.string.tower_status_live) : getString(R.string.tower_status_cached));
        binding.rotationText.setTextColor(ContextCompat.getColor(requireContext(),
                online ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark));
        binding.visSourceText.setText(sourceText);

        List<SlotCellModel> cells = new ArrayList<>();
        int movingIndex = occupied > 0 ? occupied - 1 : -1;
        for (int i = 0; i < total; i++) {
            boolean isOccupied = i < occupied;
            boolean isMoving = i == movingIndex && occupied > 0;
            String label = getString(R.string.tower_slot_label, i + 1);
            cells.add(new SlotCellModel(label, isOccupied, isMoving));
        }
        slotMapAdapter.submit(cells);
    }

    private String resolveActivePlatformLabel(int occupied) {
        if (occupied <= 0) {
            return getString(R.string.tower_active_idle);
        }
        String slotLabel = getString(R.string.tower_slot_label, occupied);
        return getString(R.string.tower_active_moving_format, slotLabel);
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    @Override
    public void onDestroyView() {
        stopRefreshing();
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pollExecutor.shutdownNow();
    }

    private static class SlotCellModel {
        final String label;
        final boolean occupied;
        final boolean moving;

        SlotCellModel(String label, boolean occupied, boolean moving) {
            this.label = label;
            this.occupied = occupied;
            this.moving = moving;
        }
    }

    private class SlotMapAdapter extends RecyclerView.Adapter<SlotMapAdapter.SlotCellViewHolder> {
        private final List<SlotCellModel> items = new ArrayList<>();

        @NonNull
        @Override
        public SlotCellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot_cell, parent, false);
            return new SlotCellViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SlotCellViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void submit(List<SlotCellModel> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        class SlotCellViewHolder extends RecyclerView.ViewHolder {
            private final com.google.android.material.card.MaterialCardView card;
            private final TextView slotTitle;
            private final TextView slotState;

            SlotCellViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.slotCard);
                slotTitle = itemView.findViewById(R.id.slotTitle);
                slotState = itemView.findViewById(R.id.slotState);
            }

            void bind(SlotCellModel model) {
                slotTitle.setText(model.label);
                if (model.moving) {
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tower_slot_bg_moving));
                    slotState.setText(R.string.tower_slot_state_moving);
                    slotState.setTextColor(ContextCompat.getColor(requireContext(), R.color.tower_slot_text_moving));
                } else if (model.occupied) {
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tower_slot_bg_occupied));
                    slotState.setText(R.string.tower_slot_state_occupied);
                    slotState.setTextColor(ContextCompat.getColor(requireContext(), R.color.tower_slot_text_occupied));
                } else {
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tower_slot_bg_free));
                    slotState.setText(R.string.tower_slot_state_free);
                    slotState.setTextColor(ContextCompat.getColor(requireContext(), R.color.tower_slot_text_free));
                }
            }
        }
    }
}
