package com.treasurehuntadventure.tha;

import android.content.Context;
import android.content.SharedPreferences;

public class PlayerLevel {
    private static final String PREFS_NAME = "PlayerLevelPrefs";
    private static final String KEY_LEVEL = "player_level";
    private static final String KEY_EXPERIENCE = "player_experience";
    private static final String KEY_COINS = "player_coins";
    private static final String KEY_TOTAL_BOUNTY = "total_bounty";
    
    private int level;
    private int experience;
    private int coins;
    private int totalBounty;
    private SharedPreferences prefs;
    private LevelUpListener levelUpListener;
    
    public interface LevelUpListener {
        void onLevelUp(int newLevel, int coinsEarned, String newRank);
    }
    
    public void setLevelUpListener(LevelUpListener listener) {
        this.levelUpListener = listener;
    }
    
    public PlayerLevel(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadPlayerData();
    }
    
    private void loadPlayerData() {
        level = prefs.getInt(KEY_LEVEL, 1);
        experience = prefs.getInt(KEY_EXPERIENCE, 0);
        coins = prefs.getInt(KEY_COINS, 100); // Starting coins
        totalBounty = prefs.getInt(KEY_TOTAL_BOUNTY, 0);
    }
    
    public void addExperience(int exp) {
        experience += exp;
        checkForLevelUp();
        savePlayerData();
    }
    
    public void addCoins(int amount) {
        coins += amount;
        savePlayerData();
    }
    
    public void addBounty(int bounty) {
        totalBounty += bounty;
        savePlayerData();
    }
    
    private void checkForLevelUp() {
        int requiredExp = getExperienceForLevel(level + 1);
        if (experience >= requiredExp) {
            int previousLevel = level;
            level++;
            // Bonus coins for leveling up
            int coinsEarned = level * 50;
            coins += coinsEarned;
            
            // Notify listener about level up
            if (levelUpListener != null) {
                levelUpListener.onLevelUp(level, coinsEarned, getPirateRank());
            }
            
            checkForLevelUp(); // Check for multiple level ups
        }
    }
    
    public int getExperienceForLevel(int targetLevel) {
        // Experience formula: level^2 * 100
        return targetLevel * targetLevel * 100;
    }
    
    public int getExperienceToNextLevel() {
        return getExperienceForLevel(level + 1) - experience;
    }
    
    public float getLevelProgress() {
        int currentLevelExp = getExperienceForLevel(level);
        int nextLevelExp = getExperienceForLevel(level + 1);
        int progressExp = experience - currentLevelExp;
        int requiredExp = nextLevelExp - currentLevelExp;
        
        return (float) progressExp / requiredExp;
    }
    
    public String getPirateRank() {
        if (level >= 50) return "Pirate King";
        else if (level >= 40) return "Yonko";
        else if (level >= 30) return "Shichibukai";
        else if (level >= 20) return "Supernova";
        else if (level >= 15) return "Veteran Pirate";
        else if (level >= 10) return "Experienced Pirate";
        else if (level >= 5) return "Rookie Pirate";
        else return "Cabin Boy";
    }
    
    public String getRankEmoji() {
        if (level >= 50) return "👑";
        else if (level >= 40) return "⚡";
        else if (level >= 30) return "🗡️";
        else if (level >= 20) return "💫";
        else if (level >= 15) return "⚓";
        else if (level >= 10) return "🏴‍☠️";
        else if (level >= 5) return "🔰";
        else return "👶";
    }
    
    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            savePlayerData();
            return true;
        }
        return false;
    }
    
    private void savePlayerData() {
        prefs.edit()
            .putInt(KEY_LEVEL, level)
            .putInt(KEY_EXPERIENCE, experience)
            .putInt(KEY_COINS, coins)
            .putInt(KEY_TOTAL_BOUNTY, totalBounty)
            .apply();
    }
    
    // Getters
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getCoins() { return coins; }
    public int getTotalBounty() { return totalBounty; }
}
