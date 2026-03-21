package com.example.verticalparking.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client with configurable ESP32 base URL.
 * Reads the host from SharedPreferences (Settings → esp_host).
 * Default: http://192.168.4.1
 */
public final class RetrofitClient {

    private static final String DEFAULT_HOST = "192.168.4.1";
    private static final int TIMEOUT_SECONDS = 5;

    private static Retrofit retrofit;
    private static String currentBaseUrl = "";

    private RetrofitClient() {
    }

    /**
     * Returns a configured ApiService instance.
     * Rebuilds the client if the host changes.
     */
    public static synchronized ApiService getApiService(Context context) {
        String baseUrl = buildBaseUrl(context);

        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            currentBaseUrl = baseUrl;
        }

        return retrofit.create(ApiService.class);
    }

    /**
     * Builds the base URL from saved ESP host preference.
     */
    private static String buildBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String host = prefs.getString("esp_host", DEFAULT_HOST);
        if (host == null || host.trim().isEmpty()) {
            host = DEFAULT_HOST;
        }
        host = host.trim();
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "http://" + host;
        }
        if (!host.endsWith("/")) {
            host = host + "/";
        }
        return host;
    }

    /**
     * Force rebuild of the client (e.g., after changing ESP host in settings).
     */
    public static synchronized void invalidate() {
        retrofit = null;
        currentBaseUrl = "";
    }
}
