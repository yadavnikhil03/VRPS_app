package com.example.verticalparking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    private final List<SessionStore.SessionEvent> events = new ArrayList<>();

    public void submit(List<SessionStore.SessionEvent> newEvents) {
        List<SessionStore.SessionEvent> oldEvents = new ArrayList<>(events);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldEvents.size();
            }

            @Override
            public int getNewListSize() {
                return newEvents.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                SessionStore.SessionEvent oldItem = oldEvents.get(oldItemPosition);
                SessionStore.SessionEvent newItem = newEvents.get(newItemPosition);
                return oldItem.time.equals(newItem.time) && oldItem.title.equals(newItem.title);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SessionStore.SessionEvent oldItem = oldEvents.get(oldItemPosition);
                SessionStore.SessionEvent newItem = newEvents.get(newItemPosition);
                return oldItem.title.equals(newItem.title)
                        && oldItem.subtitle.equals(newItem.subtitle)
                        && oldItem.time.equals(newItem.time);
            }
        });

        events.clear();
        events.addAll(newEvents);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionStore.SessionEvent event = events.get(position);
        holder.title.setText(event.title);
        holder.subtitle.setText(event.subtitle);
        holder.time.setText(event.time);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView time;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvSessionTitle);
            subtitle = itemView.findViewById(R.id.tvSessionSubtitle);
            time = itemView.findViewById(R.id.tvSessionTime);
        }
    }
}
