package com.example.verticalparking;

import android.content.Intent;
import com.example.verticalparking.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        bindingAnimations();

        // Keep splash brief while allowing branding and loader to be visible.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 1650);
    }

    private void bindingAnimations() {
        // Initial state
        findViewById(R.id.outerRing).setScaleX(0.8f);
        findViewById(R.id.outerRing).setScaleY(0.8f);
        findViewById(R.id.outerRing).setAlpha(0f);
        
        findViewById(R.id.logoCard).setScaleX(0.8f);
        findViewById(R.id.logoCard).setScaleY(0.8f);
        findViewById(R.id.logoCard).setAlpha(0f);
        
        findViewById(R.id.circularLoader).setAlpha(0f);
        findViewById(R.id.appNameText).setAlpha(0f);
        findViewById(R.id.taglineText).setAlpha(0f);
        // Core Ring and Logo Expansion
        findViewById(R.id.outerRing).animate().alpha(0.3f).scaleX(1f).scaleY(1f)
                .setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                
        findViewById(R.id.logoCard).animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(500).setStartDelay(100)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        // Reveal circular loader ring
        findViewById(R.id.circularLoader).animate().alpha(1f)
                .setStartDelay(300).setDuration(400).start();

        // Text fades
        findViewById(R.id.appNameText).animate().alpha(0.9f)
                .setStartDelay(400).setDuration(300).start();
                
        findViewById(R.id.taglineText).animate().alpha(1f)
                .setStartDelay(500).setDuration(300).start();
    }
}
