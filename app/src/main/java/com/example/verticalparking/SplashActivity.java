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
        findViewById(R.id.logoCard).setScaleX(0.9f);
        findViewById(R.id.logoCard).setScaleY(0.9f);
        findViewById(R.id.logoCard).setAlpha(0f);
        findViewById(R.id.appNameText).setAlpha(0f);
        findViewById(R.id.taglineText).setAlpha(0f);
        findViewById(R.id.loadingText).setAlpha(0f);
        findViewById(R.id.loaderDots).setAlpha(0f);

        findViewById(R.id.logoCard).animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320).start();
        findViewById(R.id.appNameText).animate().alpha(1f).setStartDelay(120).setDuration(260).start();
        findViewById(R.id.taglineText).animate().alpha(1f).setStartDelay(180).setDuration(260).start();
        findViewById(R.id.loadingText).animate().alpha(1f).setStartDelay(230).setDuration(220).start();
        findViewById(R.id.loaderDots).animate().alpha(1f).setStartDelay(280).setDuration(220).start();

        Animation pulse1 = AnimationUtils.loadAnimation(this, R.anim.splash_dot_pulse);
        Animation pulse2 = AnimationUtils.loadAnimation(this, R.anim.splash_dot_pulse);
        Animation pulse3 = AnimationUtils.loadAnimation(this, R.anim.splash_dot_pulse);
        pulse2.setStartOffset(140);
        pulse3.setStartOffset(280);
        findViewById(R.id.loaderDot1).startAnimation(pulse1);
        findViewById(R.id.loaderDot2).startAnimation(pulse2);
        findViewById(R.id.loaderDot3).startAnimation(pulse3);
    }
}
