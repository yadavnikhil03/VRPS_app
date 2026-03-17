package com.example.verticalparking;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class EspRealtimeClient {

    private EspRealtimeClient() {
    }

    public static final class RealtimeStatus {
        public final int availableSlots;
        public final int totalSlots;
        public final String status;
        public final boolean online;

        public RealtimeStatus(int availableSlots, int totalSlots, String status, boolean online) {
            this.availableSlots = availableSlots;
            this.totalSlots = totalSlots;
            this.status = status;
            this.online = online;
        }
    }

    public static RealtimeStatus fetchStatus(String host, String path) throws Exception {
        String normalizedHost = host == null ? "" : host.trim();
        if (normalizedHost.isEmpty()) {
            throw new IOException("ESP host is empty");
        }
        if (!normalizedHost.startsWith("http://") && !normalizedHost.startsWith("https://")) {
            normalizedHost = "http://" + normalizedHost;
        }

        String normalizedPath = (path == null || path.trim().isEmpty()) ? "/status" : path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        URL url = new URL(normalizedHost + normalizedPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(2500);
        connection.setReadTimeout(2500);
        connection.setRequestProperty("Accept", "application/json");

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("ESP endpoint returned HTTP " + code);
        }

        String payload;
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            payload = sb.toString();
        } finally {
            connection.disconnect();
        }

        JSONObject object = new JSONObject(payload);
        int available = readInt(object, 20, "availableSlots", "available_slots", "freeSlots", "free_slots");
        int total = readInt(object, 20, "totalSlots", "total_slots", "capacity", "max_slots");
        String status = readString(object, "status", "state", "systemStatus", "system_status");
        boolean online = readBoolean(object, true, "isConnected", "connected", "online");

        return new RealtimeStatus(available, total, status, online);
    }

    private static int readInt(JSONObject object, int fallback, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                return object.optInt(key, fallback);
            }
        }
        return fallback;
    }

    private static String readString(JSONObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                String value = object.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    private static boolean readBoolean(JSONObject object, boolean fallback, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                return object.optBoolean(key, fallback);
            }
        }
        return fallback;
    }
}