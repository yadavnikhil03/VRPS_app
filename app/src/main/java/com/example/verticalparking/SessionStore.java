package com.example.verticalparking;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SessionStore {

    private static final String PREFS_NAME = "ParkingHistory";
    private static final String KEY_EVENTS = "events";
    private static final String SETTINGS_PREFS_NAME = "Settings";
    private static final String KEY_HISTORY_AUTOSAVE = "history_auto_save";
    private static final int MAX_EVENTS = 100;

    private SessionStore() {
    }

    public static void addEvent(Context context, String title, String subtitle) {
        if (!isAutoSaveEnabled(context)) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String eventsString = prefs.getString(KEY_EVENTS, "[]");
        JSONArray existing;
        try {
            existing = new JSONArray(eventsString);
        } catch (JSONException e) {
            existing = new JSONArray();
        }

        JSONObject event = new JSONObject();
        try {
            event.put("title", title);
            event.put("subtitle", subtitle);
            event.put("time", new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()));

            // Prepend event without overwriting existing entries.
            JSONArray merged = new JSONArray();
            merged.put(event);

            int copyCount = Math.min(existing.length(), MAX_EVENTS - 1);
            for (int i = 0; i < copyCount; i++) {
                JSONObject item = existing.optJSONObject(i);
                if (item != null) {
                    merged.put(item);
                }
            }

            prefs.edit().putString(KEY_EVENTS, merged.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public static List<SessionEvent> getEvents(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String eventsString = prefs.getString(KEY_EVENTS, "[]");
        List<SessionEvent> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(eventsString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new SessionEvent(
                        item.optString("title", "Session"),
                        item.optString("subtitle", ""),
                        item.optString("time", "")
                ));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static void clearAllEvents(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_EVENTS, "[]").apply();
    }

    public static boolean isAutoSaveEnabled(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean(KEY_HISTORY_AUTOSAVE, true);
    }

    public static void setAutoSaveEnabled(Context context, boolean enabled) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);
        settings.edit().putBoolean(KEY_HISTORY_AUTOSAVE, enabled).apply();
    }

    public static final class SessionEvent {
        public final String title;
        public final String subtitle;
        public final String time;

        public SessionEvent(String title, String subtitle, String time) {
            this.title = title;
            this.subtitle = subtitle;
            this.time = time;
        }
    }
}
