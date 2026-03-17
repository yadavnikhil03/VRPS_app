package com.example.verticalparking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.example.verticalparking.databinding.ActivityMainBinding;
import com.example.verticalparking.R;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import androidx.activity.OnBackPressedCallback;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme before onCreate
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        setupNavigation();
    }

    private void setupNavigation() {
        setSupportActionBar(binding.toolbar);

        // Drawer Setup
        toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar,
                R.string.nav_open, R.string.nav_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Bottom Nav Setup
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;
            
            if (id == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (id == R.id.nav_control) {
                selectedFragment = new ControlFragment();
            } else if (id == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            }
            
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        // Set Default Fragment
        binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);

        // Drawer Nav Setup
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;
            
            if (id == R.id.drawer_home) {
                binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            } else if (id == R.id.drawer_settings) {
                selectedFragment = new SettingsFragment();
            }
            
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit();
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        binding.getRoot().findViewById(R.id.githubFooter).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_profile_url)));
            startActivity(browserIntent);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        ImageView githubAvatar = binding.getRoot().findViewById(R.id.ivGithubAvatar);
        Glide.with(this)
                .load(getString(R.string.github_avatar_url))
                .placeholder(R.drawable.ic_github)
                .error(R.drawable.ic_github)
            .into(githubAvatar);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
