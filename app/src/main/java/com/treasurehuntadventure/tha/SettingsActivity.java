package com.treasurehuntadventure.tha;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private Switch soundToggle;
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("TreasureHuntPrefs", MODE_PRIVATE);
        soundToggle = findViewById(R.id.sound_toggle);
        soundManager = SoundManager.getInstance(this);

        // Load saved sound setting
        boolean soundEnabled = sharedPreferences.getBoolean("soundEnabled", true);
        soundToggle.setChecked(soundEnabled);

        soundToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Play button click sound (if enabling sound)
                if (isChecked) {
                    soundManager.playButtonClickSound();
                }
                
                // Update sound manager with new setting
                soundManager.setSoundEnabled(isChecked);
            }
        });
    }
}