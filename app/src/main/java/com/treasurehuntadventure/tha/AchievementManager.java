package com.treasurehuntadventure.tha;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementManager {
    private static final String TAG = "AchievementManager";
    private static final String PREFS_NAME = "AchievementPrefs";
    private static final String KEY_ACHIEVEMENTS = "achievements";
    
    private static AchievementManager instance;
    private Context context;
    private SharedPreferences prefs;
    private Map<String, Achievement> achievements;
    private List<AchievementListener> listeners;
    private Gson gson;
    
    public interface AchievementListener {
        void onAchievementUnlocked(Achievement achievement);
        void onAchievementProgress(Achievement achievement, int oldProgress, int newProgress);
    }
    
    private AchievementManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.achievements = new HashMap<>();
        this.listeners = new ArrayList<>();
        this.gson = new Gson();
        
        initializeAchievements();
        loadAchievements();
    }
    
    public static synchronized AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context);
        }
        return instance;
    }
    
    private void initializeAchievements() {
        // Treasure Capture Achievements
        addAchievement(new Achievement("first_treasure", "First Treasure", "Capture your first treasure", 
                Achievement.AchievementType.TREASURE_CAPTURE, 1, Achievement.AchievementRarity.BRONZE));
        addAchievement(new Achievement("treasure_hunter", "Treasure Hunter", "Capture 10 treasures", 
                Achievement.AchievementType.TREASURE_CAPTURE, 10, Achievement.AchievementRarity.SILVER));
        addAchievement(new Achievement("treasure_master", "Treasure Master", "Capture 50 treasures", 
                Achievement.AchievementType.TREASURE_CAPTURE, 50, Achievement.AchievementRarity.GOLD));
        addAchievement(new Achievement("legendary_hunter", "Legendary Hunter", "Capture 100 treasures", 
                Achievement.AchievementType.TREASURE_CAPTURE, 100, Achievement.AchievementRarity.LEGENDARY));
        
        // Level Milestone Achievements
        addAchievement(new Achievement("level_5", "Seasoned Pirate", "Reach level 5", 
                Achievement.AchievementType.LEVEL_MILESTONE, 5, Achievement.AchievementRarity.BRONZE));
        addAchievement(new Achievement("level_10", "Experienced Captain", "Reach level 10", 
                Achievement.AchievementType.LEVEL_MILESTONE, 10, Achievement.AchievementRarity.SILVER));
        addAchievement(new Achievement("level_25", "Notorious Pirate", "Reach level 25", 
                Achievement.AchievementType.LEVEL_MILESTONE, 25, Achievement.AchievementRarity.GOLD));
        addAchievement(new Achievement("level_50", "Pirate Legend", "Reach level 50", 
                Achievement.AchievementType.LEVEL_MILESTONE, 50, Achievement.AchievementRarity.LEGENDARY));
        
        // Distance Achievements
        addAchievement(new Achievement("explorer", "Explorer", "Travel 1000 meters", 
                Achievement.AchievementType.DISTANCE_TRAVELED, 1000, Achievement.AchievementRarity.BRONZE));
        addAchievement(new Achievement("wanderer", "Wanderer", "Travel 5000 meters", 
                Achievement.AchievementType.DISTANCE_TRAVELED, 5000, Achievement.AchievementRarity.SILVER));
        addAchievement(new Achievement("nomad", "Nomad", "Travel 10000 meters", 
                Achievement.AchievementType.DISTANCE_TRAVELED, 10000, Achievement.AchievementRarity.GOLD));
        
        // Consecutive Days
        addAchievement(new Achievement("daily_pirate", "Daily Pirate", "Login 3 consecutive days", 
                Achievement.AchievementType.CONSECUTIVE_DAYS, 3, Achievement.AchievementRarity.BRONZE));
        addAchievement(new Achievement("dedicated_captain", "Dedicated Captain", "Login 7 consecutive days", 
                Achievement.AchievementType.CONSECUTIVE_DAYS, 7, Achievement.AchievementRarity.SILVER));
        addAchievement(new Achievement("loyal_crew", "Loyal Crew", "Login 30 consecutive days", 
                Achievement.AchievementType.CONSECUTIVE_DAYS, 30, Achievement.AchievementRarity.LEGENDARY));
        
        // Social Achievements
        addAchievement(new Achievement("social_pirate", "Social Pirate", "Send 10 chat messages", 
                Achievement.AchievementType.SOCIAL_INTERACTION, 10, Achievement.AchievementRarity.BRONZE));
        addAchievement(new Achievement("chatty_captain", "Chatty Captain", "Send 50 chat messages", 
                Achievement.AchievementType.SOCIAL_INTERACTION, 50, Achievement.AchievementRarity.SILVER));
        
        // Speed Achievements
        addAchievement(new Achievement("speed_demon", "Speed Demon", "Capture 5 treasures in 1 hour", 
                Achievement.AchievementType.SPEED_ACHIEVEMENT, 5, Achievement.AchievementRarity.GOLD));
        
        Log.d(TAG, "Initialized " + achievements.size() + " achievements");
    }
    
    private void addAchievement(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
    }
    
    public void addListener(AchievementListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(AchievementListener listener) {
        listeners.remove(listener);
    }
    
    public void updateProgress(Achievement.AchievementType type, int progress) {
        boolean hasChanges = false;
        
        for (Achievement achievement : achievements.values()) {
            if (achievement.getType() == type && !achievement.isUnlocked()) {
                int oldProgress = achievement.getCurrentProgress();
                achievement.updateProgress(progress);
                
                if (achievement.getCurrentProgress() != oldProgress) {
                    hasChanges = true;
                    notifyProgressUpdate(achievement, oldProgress, achievement.getCurrentProgress());
                    
                    if (achievement.isUnlocked() && !achievement.isUnlocked()) {
                        notifyAchievementUnlocked(achievement);
                    }
                }
            }
        }
        
        if (hasChanges) {
            saveAchievements();
        }
    }
    
    public void addProgress(Achievement.AchievementType type, int amount) {
        boolean hasChanges = false;
        
        for (Achievement achievement : achievements.values()) {
            if (achievement.getType() == type && !achievement.isUnlocked()) {
                int oldProgress = achievement.getCurrentProgress();
                achievement.addProgress(amount);
                
                if (achievement.getCurrentProgress() != oldProgress) {
                    hasChanges = true;
                    notifyProgressUpdate(achievement, oldProgress, achievement.getCurrentProgress());
                    
                    if (achievement.isCompleted() && !achievement.isUnlocked()) {
                        achievement.setUnlocked(true);
                        notifyAchievementUnlocked(achievement);
                    }
                }
            }
        }
        
        if (hasChanges) {
            saveAchievements();
        }
    }
    
    public void checkLevelAchievements(int currentLevel) {
        updateProgress(Achievement.AchievementType.LEVEL_MILESTONE, currentLevel);
    }
    
    public void checkTreasureAchievements(int treasureCount) {
        updateProgress(Achievement.AchievementType.TREASURE_CAPTURE, treasureCount);
    }
    
    public void checkDistanceAchievements(float distanceTraveled) {
        updateProgress(Achievement.AchievementType.DISTANCE_TRAVELED, (int) distanceTraveled);
    }
    
    public void checkSocialAchievements(int messageCount) {
        updateProgress(Achievement.AchievementType.SOCIAL_INTERACTION, messageCount);
    }
    
    private void notifyAchievementUnlocked(Achievement achievement) {
        Log.d(TAG, "Achievement unlocked: " + achievement.getTitle());
        for (AchievementListener listener : listeners) {
            listener.onAchievementUnlocked(achievement);
        }
    }
    
    private void notifyProgressUpdate(Achievement achievement, int oldProgress, int newProgress) {
        for (AchievementListener listener : listeners) {
            listener.onAchievementProgress(achievement, oldProgress, newProgress);
        }
    }
    
    public List<Achievement> getAllAchievements() {
        return new ArrayList<>(achievements.values());
    }
    
    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }
    
    public List<Achievement> getProgressAchievements() {
        List<Achievement> inProgress = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (!achievement.isUnlocked() && achievement.getCurrentProgress() > 0) {
                inProgress.add(achievement);
            }
        }
        return inProgress;
    }
    
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }
    
    private void saveAchievements() {
        try {
            String json = gson.toJson(achievements);
            prefs.edit().putString(KEY_ACHIEVEMENTS, json).apply();
            Log.d(TAG, "Achievements saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving achievements", e);
        }
    }
    
    private void loadAchievements() {
        try {
            String json = prefs.getString(KEY_ACHIEVEMENTS, null);
            if (json != null) {
                Type type = new TypeToken<Map<String, Achievement>>(){}.getType();
                Map<String, Achievement> savedAchievements = gson.fromJson(json, type);
                
                // Update existing achievements with saved progress
                for (Map.Entry<String, Achievement> entry : savedAchievements.entrySet()) {
                    if (achievements.containsKey(entry.getKey())) {
                        Achievement saved = entry.getValue();
                        Achievement current = achievements.get(entry.getKey());
                        current.setCurrentProgress(saved.getCurrentProgress());
                        current.setUnlocked(saved.isUnlocked());
                        current.setUnlockedDate(saved.getUnlockedDate());
                    }
                }
                
                Log.d(TAG, "Achievements loaded successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading achievements", e);
        }
    }
}
