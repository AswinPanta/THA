package com.treasurehuntadventure.tha;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class SoundManager {
    private static SoundManager instance;
    private MediaPlayer treasureAppearSound;
    private MediaPlayer treasureCaptureSound;
    private Context context;
    private boolean soundEnabled;
    private SharedPreferences sharedPreferences;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent memory leaks
        sharedPreferences = context.getSharedPreferences("TreasureHuntPrefs", Context.MODE_PRIVATE);
        soundEnabled = sharedPreferences.getBoolean("soundEnabled", true); // Default to true

        treasureAppearSound = MediaPlayer.create(this.context, R.raw.treasure_appear);
        treasureCaptureSound = MediaPlayer.create(this.context, R.raw.treasure_capture);
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    public void playTreasureAppearSound() {
        if (soundEnabled && treasureAppearSound != null) {
            treasureAppearSound.start();
        }
    }

    public void playTreasureCaptureSound() {
        if (soundEnabled && treasureCaptureSound != null) {
            treasureCaptureSound.start();
        }
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("soundEnabled", enabled);
        editor.apply();
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void release() {
        if (treasureAppearSound != null) {
            treasureAppearSound.release();
            treasureAppearSound = null;
        }
        if (treasureCaptureSound != null) {
            treasureCaptureSound.release();
            treasureCaptureSound = null;
        }
    }
}