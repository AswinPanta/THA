package com.treasurehuntadventure.tha;

import java.util.Date;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private String iconResource;
    private AchievementType type;
    private int targetValue;
    private int currentProgress;
    private boolean isUnlocked;
    private Date unlockedDate;
    private int xpReward;
    private int coinsReward;
    private AchievementRarity rarity;
    
    public enum AchievementType {
        TREASURE_CAPTURE,
        LEVEL_MILESTONE,
        DISTANCE_TRAVELED,
        CONSECUTIVE_DAYS,
        SOCIAL_INTERACTION,
        EXPLORATION,
        SPEED_ACHIEVEMENT,
        SPECIAL_EVENT
    }
    
    public enum AchievementRarity {
        BRONZE(50, 100),
        SILVER(100, 250),
        GOLD(200, 500),
        PLATINUM(500, 1000),
        LEGENDARY(1000, 2500);
        
        private final int xpReward;
        private final int coinsReward;
        
        AchievementRarity(int xpReward, int coinsReward) {
            this.xpReward = xpReward;
            this.coinsReward = coinsReward;
        }
        
        public int getXpReward() { return xpReward; }
        public int getCoinsReward() { return coinsReward; }
    }
    
    public Achievement(String id, String title, String description, AchievementType type, 
                      int targetValue, AchievementRarity rarity) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.targetValue = targetValue;
        this.rarity = rarity;
        this.currentProgress = 0;
        this.isUnlocked = false;
        this.xpReward = rarity.getXpReward();
        this.coinsReward = rarity.getCoinsReward();
        this.iconResource = getIconForType(type);
    }
    
    private String getIconForType(AchievementType type) {
        switch (type) {
            case TREASURE_CAPTURE: return "ic_treasure_achievement";
            case LEVEL_MILESTONE: return "ic_level_achievement";
            case DISTANCE_TRAVELED: return "ic_compass_achievement";
            case CONSECUTIVE_DAYS: return "ic_calendar_achievement";
            case SOCIAL_INTERACTION: return "ic_chat_achievement";
            case EXPLORATION: return "ic_map_achievement";
            case SPEED_ACHIEVEMENT: return "ic_speed_achievement";
            case SPECIAL_EVENT: return "ic_special_achievement";
            default: return "ic_default_achievement";
        }
    }
    
    public void updateProgress(int progress) {
        this.currentProgress = Math.min(progress, targetValue);
        if (this.currentProgress >= targetValue && !isUnlocked) {
            unlock();
        }
    }
    
    public void addProgress(int amount) {
        updateProgress(this.currentProgress + amount);
    }
    
    private void unlock() {
        this.isUnlocked = true;
        this.unlockedDate = new Date();
    }
    
    public float getProgressPercentage() {
        return targetValue > 0 ? (float) currentProgress / targetValue : 0f;
    }
    
    public boolean isCompleted() {
        return currentProgress >= targetValue;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) { this.iconResource = iconResource; }
    
    public AchievementType getType() { return type; }
    public void setType(AchievementType type) { this.type = type; }
    
    public int getTargetValue() { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }
    
    public int getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }
    
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    
    public Date getUnlockedDate() { return unlockedDate; }
    public void setUnlockedDate(Date unlockedDate) { this.unlockedDate = unlockedDate; }
    
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    
    public int getCoinsReward() { return coinsReward; }
    public void setCoinsReward(int coinsReward) { this.coinsReward = coinsReward; }
    
    public AchievementRarity getRarity() { return rarity; }
    public void setRarity(AchievementRarity rarity) { this.rarity = rarity; }
}
