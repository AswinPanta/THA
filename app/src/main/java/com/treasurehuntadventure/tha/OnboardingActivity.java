package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class OnboardingActivity extends AppCompatActivity {
    private static final String PREFS = "OnboardingPrefs";
    private static final String KEY_DONE = "onboarding_done";
    private ViewPager viewPager;
    private int[] layouts = {
            R.layout.onboarding_page_map,
            R.layout.onboarding_page_ar,
            R.layout.onboarding_page_chat,
            R.layout.onboarding_page_leaderboard,
            R.layout.onboarding_page_challenges,
            R.layout.onboarding_page_profile
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DONE, false)) {
            goToLogin();
            return;
        }
        setContentView(R.layout.activity_onboarding);
        viewPager = findViewById(R.id.onboardingViewPager);
        PagerAdapter adapter = new PagerAdapter() {
            @Override
            public int getCount() { return layouts.length; }
            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) { return view == object; }
            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View v = LayoutInflater.from(OnboardingActivity.this).inflate(layouts[position], container, false);
                if (position == layouts.length - 1) {
                    Button btn = v.findViewById(R.id.getStartedButton);
                    btn.setVisibility(View.VISIBLE);
                    btn.setOnClickListener(view -> {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply();
                        goToLogin();
                    });
                }
                container.addView(v);
                return v;
            }
            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }
        };
        viewPager.setAdapter(adapter);
    }
    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
} 