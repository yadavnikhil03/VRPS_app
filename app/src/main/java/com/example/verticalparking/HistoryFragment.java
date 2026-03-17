package com.example.verticalparking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.verticalparking.databinding.FragmentHistoryBinding;
import com.example.verticalparking.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private SessionAdapter sessionAdapter;
    private boolean newestFirst = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        sessionAdapter = new SessionAdapter();
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecyclerView.setAdapter(sessionAdapter);
        binding.historyRecyclerView.setLayoutAnimation(
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_session_stagger)
        );

        newestFirst = true;
        binding.chipNewestFirst.setChecked(true);

        boolean autoSaveEnabled = SessionStore.isAutoSaveEnabled(requireContext());
        binding.chipAutoSaved.setChecked(autoSaveEnabled);
        binding.chipAutoSaved.setText(autoSaveEnabled ? getString(R.string.history_autosave_on) : getString(R.string.history_autosave_off));

        binding.chipNewestFirst.setOnClickListener(v -> {
            newestFirst = !newestFirst;
            binding.chipNewestFirst.setChecked(newestFirst);
            binding.chipNewestFirst.setText(newestFirst ? getString(R.string.history_newest_first) : getString(R.string.history_oldest_first));
            loadEvents();
        });

        binding.chipAutoSaved.setOnClickListener(v -> {
            boolean enabled = !SessionStore.isAutoSaveEnabled(requireContext());
            SessionStore.setAutoSaveEnabled(requireContext(), enabled);
            binding.chipAutoSaved.setChecked(enabled);
            binding.chipAutoSaved.setText(enabled ? getString(R.string.history_autosave_on) : getString(R.string.history_autosave_off));
            Toast.makeText(requireContext(), enabled ? R.string.history_autosave_enabled_message : R.string.history_autosave_disabled_message, Toast.LENGTH_SHORT).show();
        });

        binding.btnClearHistory.setOnClickListener(v -> showClearHistoryConfirmation());

        loadEvents();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.chipAutoSaved.setChecked(SessionStore.isAutoSaveEnabled(requireContext()));
        java.util.List<SessionStore.SessionEvent> events = SessionStore.getEvents(requireContext());
        if (!newestFirst) {
            java.util.Collections.reverse(events);
        }
        sessionAdapter.submit(events);
        binding.historyEmptyCard.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadEvents() {
        java.util.List<SessionStore.SessionEvent> events = SessionStore.getEvents(requireContext());
        if (!newestFirst) {
            java.util.Collections.reverse(events);
        }
        sessionAdapter.submit(events);
        binding.historyEmptyCard.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        if (!events.isEmpty()) {
            binding.historyRecyclerView.scheduleLayoutAnimation();
        }
    }

    private void showClearHistoryConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_clear_history)
                .setMessage(R.string.confirm_clear_history_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_clear_history, (dialog, which) -> {
                    SessionStore.clearAllEvents(requireContext());
                    loadEvents();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
