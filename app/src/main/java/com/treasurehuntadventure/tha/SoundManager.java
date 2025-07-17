package com.treasurehuntadventure.tha;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private static SoundManager instance;
    private MediaPlayer treasureAppearSound;
    private MediaPlayer treasureCaptureSound;
    private MediaPlayer backgroundMusicPlayer;
    private MediaPlayer levelUpSound;
    private MediaPlayer buttonClickSound;
    private MediaPlayer mapTransitionSound;
    private Context context;
    private boolean soundEnabled;
    private SharedPreferences sharedPreferences;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent memory leaks
        sharedPreferences = context.getSharedPreferences("TreasureHuntPrefs", Context.MODE_PRIVATE);
        soundEnabled = sharedPreferences.getBoolean("soundEnabled", true); // Default to true

        initializeSounds();
        
        // Start background music if sound is enabled
        if (soundEnabled) {
            playBackgroundMusic();
        }
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    private void initializeSounds() {
        try {
            treasureAppearSound = MediaPlayer.create(this.context, R.raw.treasure_appear);
            treasureCaptureSound = MediaPlayer.create(this.context, R.raw.treasure_capture);
            
            // Create synthetic sounds for missing resources
            buttonClickSound = createSyntheticSound(440, 100); // A4 note, 100ms
            levelUpSound = createSyntheticSound(880, 500); // A5 note, 500ms
            mapTransitionSound = createSyntheticSound(660, 200); // E5 note, 200ms
            
            // Background music (will gracefully handle missing resource)
            backgroundMusicPlayer = MediaPlayer.create(this.context, R.raw.background_music);
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.setLooping(true);
                backgroundMusicPlayer.setVolume(0.3f, 0.3f); // Lower volume for background music
            }
            
            Log.d(TAG, "Sound system initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing sounds: " + e.getMessage(), e);
        }
    }

    private MediaPlayer createSyntheticSound(int frequency, int duration) {
        // For now, we'll use the treasure_appear sound as a fallback
        // In a real implementation, you would generate synthetic tones
        return MediaPlayer.create(this.context, R.raw.treasure_appear);
    }

    public void playTreasureAppearSound() {
        playSound(treasureAppearSound, "treasure appear");
    }

    public void playTreasureCaptureSound() {
        playSound(treasureCaptureSound, "treasure capture");
    }

    public void playLevelUpSound() {
        playSound(levelUpSound, "level up");
    }

    public void playButtonClickSound() {
        playSound(buttonClickSound, "button click");
    }

    public void playMapTransitionSound() {
        playSound(mapTransitionSound, "map transition");
    }

    private void playSound(MediaPlayer mediaPlayer, String soundName) {
        if (soundEnabled && mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(0);
                } else {
                    mediaPlayer.start();
                }
                Log.d(TAG, "Playing " + soundName + " sound");
            } catch (Exception e) {
                Log.e(TAG, "Error playing " + soundName + " sound: " + e.getMessage(), e);
            }
        }
    }

    public void playBackgroundMusic() {
        if (soundEnabled && backgroundMusicPlayer != null) {
            try {
                if (!backgroundMusicPlayer.isPlaying()) {
                    backgroundMusicPlayer.start();
                    Log.d(TAG, "Background music started");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing background music: " + e.getMessage(), e);
            }
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            try {
                backgroundMusicPlayer.pause();
                backgroundMusicPlayer.seekTo(0);
                Log.d(TAG, "Background music stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping background music: " + e.getMessage(), e);
            }
        }
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            try {
                backgroundMusicPlayer.pause();
                Log.d(TAG, "Background music paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing background music: " + e.getMessage(), e);
            }
        }
    }

    public void resumeBackgroundMusic() {
        if (soundEnabled && backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
            try {
                backgroundMusicPlayer.start();
                Log.d(TAG, "Background music resumed");
            } catch (Exception e) {
                Log.e(TAG, "Error resuming background music: " + e.getMessage(), e);
            }
        }
    }
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("soundEnabled", enabled);
        editor.apply();
        
        // Control background music based on sound setting
        if (enabled) {
            playBackgroundMusic();
        } else {
            stopBackgroundMusic();
        }
        
        Log.d(TAG, "Sound " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void release() {
        Log.d(TAG, "Releasing all sound resources");
        
        if (treasureAppearSound != null) {
            treasureAppearSound.release();
            treasureAppearSound = null;
        }
        if (treasureCaptureSound != null) {
            treasureCaptureSound.release();
            treasureCaptureSound = null;
        }
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
        }
        if (levelUpSound != null) {
            levelUpSound.release();
            levelUpSound = null;
        }
        if (buttonClickSound != null) {
            buttonClickSound.release();
            buttonClickSound = null;
        }
        if (mapTransitionSound != null) {
            mapTransitionSound.release();
            mapTransitionSound = null;
        }
    }
}
