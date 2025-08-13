package com.treasurehuntadventure.tha.leaderboard;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerValue;
import com.treasurehuntadventure.tha.Achievement;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive player profile model with detailed stats, achievements, and ranking metrics
 */
@IgnoreExtraProperties
public class PlayerProfile {
    
    // Basic player information
    private String playerId;
    private String displayName;
    private String email;
    private String avatarUrl;
    private long createdAt;
    private long lastActiveAt;
    
    // Level and progression
    private int level;
    private long experience;
    private long totalExperience;
    private int coins;
    private long totalBounty;
    
    // Core statistics
    private PlayerStatistics statistics;
    
    // Rankings and scores
    private PlayerRankings rankings;
    
    // Achievements and badges
    private List<String> unlockedAchievements;
    private List<PlayerBadge> badges;
    private String currentTitle;
    
    // Activity tracking
    private PlayerActivity recentActivity;
    
    // Social features
    private List<String> friends;
    private String teamId;
    private Map<String, Object> preferences;
    
    // Metadata
    private Map<String, Object> metadata;
    
    /**
     * Player statistics data class
     */
    public static class PlayerStatistics {
        private long treasuresCaptured;
        private Map<String, Integer> treasuresByRarity;
        private double distanceWalked;
        private long timePlayed;
        private int consecutiveDays;
        private long lastLoginStreak;
        
        // Combat and interaction stats
        private int treasuresBattled;
        private int treasuresWon;
        private int treasuresLost;
        private int chatMessagesSent;
        private int helpRequestsAnswered;
        
        // Exploration stats
        private int locationsVisited;
        private int landmarksDiscovered;
        private double maxDistanceFromHome;
        private int nightTimeCaptures;
        private int weatherChallengeCaptures;
        
        // Speed and efficiency
        private long fastestCaptureTime;
        private int capturesInOneDay;
        private double averageCaptureTime;
        private int perfectCaptureStreak;
        
        public PlayerStatistics() {
            this.treasuresByRarity = new HashMap<>();
            for (TreasureRarity rarity : TreasureRarity.values()) {
                treasuresByRarity.put(rarity.name(), 0);
            }
        }
        
        // Getters and setters
        public long getTreasuresCaptured() { return treasuresCaptured; }
        public void setTreasuresCaptured(long treasuresCaptured) { this.treasuresCaptured = treasuresCaptured; }
        
        public Map<String, Integer> getTreasuresByRarity() { return treasuresByRarity; }
        public void setTreasuresByRarity(Map<String, Integer> treasuresByRarity) { this.treasuresByRarity = treasuresByRarity; }
        
        public double getDistanceWalked() { return distanceWalked; }
        public void setDistanceWalked(double distanceWalked) { this.distanceWalked = distanceWalked; }
        
        public long getTimePlayed() { return timePlayed; }
        public void setTimePlayed(long timePlayed) { this.timePlayed = timePlayed; }
        
        public int getConsecutiveDays() { return consecutiveDays; }
        public void setConsecutiveDays(int consecutiveDays) { this.consecutiveDays = consecutiveDays; }
        
        public long getLastLoginStreak() { return lastLoginStreak; }
        public void setLastLoginStreak(long lastLoginStreak) { this.lastLoginStreak = lastLoginStreak; }
        
        public int getTreasuresBattled() { return treasuresBattled; }
        public void setTreasuresBattled(int treasuresBattled) { this.treasuresBattled = treasuresBattled; }
        
        public int getTreasuresWon() { return treasuresWon; }
        public void setTreasuresWon(int treasuresWon) { this.treasuresWon = treasuresWon; }
        
        public int getTreasuresLost() { return treasuresLost; }
        public void setTreasuresLost(int treasuresLost) { this.treasuresLost = treasuresLost; }
        
        public int getChatMessagesSent() { return chatMessagesSent; }
        public void setChatMessagesSent(int chatMessagesSent) { this.chatMessagesSent = chatMessagesSent; }
        
        public int getHelpRequestsAnswered() { return helpRequestsAnswered; }
        public void setHelpRequestsAnswered(int helpRequestsAnswered) { this.helpRequestsAnswered = helpRequestsAnswered; }
        
        public int getLocationsVisited() { return locationsVisited; }
        public void setLocationsVisited(int locationsVisited) { this.locationsVisited = locationsVisited; }
        
        public int getLandmarksDiscovered() { return landmarksDiscovered; }
        public void setLandmarksDiscovered(int landmarksDiscovered) { this.landmarksDiscovered = landmarksDiscovered; }
        
        public double getMaxDistanceFromHome() { return maxDistanceFromHome; }
        public void setMaxDistanceFromHome(double maxDistanceFromHome) { this.maxDistanceFromHome = maxDistanceFromHome; }
        
        public int getNightTimeCaptures() { return nightTimeCaptures; }
        public void setNightTimeCaptures(int nightTimeCaptures) { this.nightTimeCaptures = nightTimeCaptures; }
        
        public int getWeatherChallengeCaptures() { return weatherChallengeCaptures; }
        public void setWeatherChallengeCaptures(int weatherChallengeCaptures) { this.weatherChallengeCaptures = weatherChallengeCaptures; }
        
        public long getFastestCaptureTime() { return fastestCaptureTime; }
        public void setFastestCaptureTime(long fastestCaptureTime) { this.fastestCaptureTime = fastestCaptureTime; }
        
        public int getCapturesInOneDay() { return capturesInOneDay; }
        public void setCapturesInOneDay(int capturesInOneDay) { this.capturesInOneDay = capturesInOneDay; }
        
        public double getAverageCaptureTime() { return averageCaptureTime; }
        public void setAverageCaptureTime(double averageCaptureTime) { this.averageCaptureTime = averageCaptureTime; }
        
        public int getPerfectCaptureStreak() { return perfectCaptureStreak; }
        public void setPerfectCaptureStreak(int perfectCaptureStreak) { this.perfectCaptureStreak = perfectCaptureStreak; }
    }
    
    /**
     * Player rankings data class
     */
    public static class PlayerRankings {
        private long totalScore;
        private int globalRank;
        private int weeklyRank;
        private int monthlyRank;
        private int regionalRank;
        private int friendsRank;
        
        // Category-specific rankings
        private int treasureHunterRank;
        private int explorerRank;
        private int socialRank;
        private int speedsterRank;
        
        // Ranking history
        private List<RankingSnapshot> rankingHistory;
        
        public PlayerRankings() {
            this.rankingHistory = new ArrayList<>();
        }
        
        // Getters and setters
        public long getTotalScore() { return totalScore; }
        public void setTotalScore(long totalScore) { this.totalScore = totalScore; }
        
        public int getGlobalRank() { return globalRank; }
        public void setGlobalRank(int globalRank) { this.globalRank = globalRank; }
        
        public int getWeeklyRank() { return weeklyRank; }
        public void setWeeklyRank(int weeklyRank) { this.weeklyRank = weeklyRank; }
        
        public int getMonthlyRank() { return monthlyRank; }
        public void setMonthlyRank(int monthlyRank) { this.monthlyRank = monthlyRank; }
        
        public int getRegionalRank() { return regionalRank; }
        public void setRegionalRank(int regionalRank) { this.regionalRank = regionalRank; }
        
        public int getFriendsRank() { return friendsRank; }
        public void setFriendsRank(int friendsRank) { this.friendsRank = friendsRank; }
        
        public int getTreasureHunterRank() { return treasureHunterRank; }
        public void setTreasureHunterRank(int treasureHunterRank) { this.treasureHunterRank = treasureHunterRank; }
        
        public int getExplorerRank() { return explorerRank; }
        public void setExplorerRank(int explorerRank) { this.explorerRank = explorerRank; }
        
        public int getSocialRank() { return socialRank; }
        public void setSocialRank(int socialRank) { this.socialRank = socialRank; }
        
        public int getSpeedsterRank() { return speedsterRank; }
        public void setSpeedsterRank(int speedsterRank) { this.speedsterRank = speedsterRank; }
        
        public List<RankingSnapshot> getRankingHistory() { return rankingHistory; }
        public void setRankingHistory(List<RankingSnapshot> rankingHistory) { this.rankingHistory = rankingHistory; }
    }
    
    /**
     * Player badge data class
     */
    public static class PlayerBadge {
        private String badgeId;
        private String name;
        private String description;
        private String iconUrl;
        private BadgeRarity rarity;
        private long earnedAt;
        private Map<String, Object> criteria;
        
        public enum BadgeRarity {
            COMMON("Common", "#8E8E93"),
            UNCOMMON("Uncommon", "#34C759"),
            RARE("Rare", "#007AFF"),
            EPIC("Epic", "#AF52DE"),
            LEGENDARY("Legendary", "#FF9500"),
            MYTHIC("Mythic", "#FF3B30");
            
            private final String displayName;
            private final String color;
            
            BadgeRarity(String displayName, String color) {
                this.displayName = displayName;
                this.color = color;
            }
            
            public String getDisplayName() { return displayName; }
            public String getColor() { return color; }
        }
        
        public PlayerBadge() {} // Required for Firebase
        
        public PlayerBadge(String badgeId, String name, String description, BadgeRarity rarity) {
            this.badgeId = badgeId;
            this.name = name;
            this.description = description;
            this.rarity = rarity;
            this.earnedAt = System.currentTimeMillis();
            this.criteria = new HashMap<>();
        }
        
        // Getters and setters
        public String getBadgeId() { return badgeId; }
        public void setBadgeId(String badgeId) { this.badgeId = badgeId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getIconUrl() { return iconUrl; }
        public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
        
        public BadgeRarity getRarity() { return rarity; }
        public void setRarity(BadgeRarity rarity) { this.rarity = rarity; }
        
        public long getEarnedAt() { return earnedAt; }
        public void setEarnedAt(long earnedAt) { this.earnedAt = earnedAt; }
        
        public Map<String, Object> getCriteria() { return criteria; }
        public void setCriteria(Map<String, Object> criteria) { this.criteria = criteria; }
    }
    
    /**
     * Player activity data class
     */
    public static class PlayerActivity {
        private List<ActivityEntry> recentActivities;
        private long lastTreasureCapture;
        private long lastAchievementUnlock;
        private long lastLevelUp;
        private String lastLocation;
        
        public PlayerActivity() {
            this.recentActivities = new ArrayList<>();
        }
        
        // Getters and setters
        public List<ActivityEntry> getRecentActivities() { return recentActivities; }
        public void setRecentActivities(List<ActivityEntry> recentActivities) { this.recentActivities = recentActivities; }
        
        public long getLastTreasureCapture() { return lastTreasureCapture; }
        public void setLastTreasureCapture(long lastTreasureCapture) { this.lastTreasureCapture = lastTreasureCapture; }
        
        public long getLastAchievementUnlock() { return lastAchievementUnlock; }
        public void setLastAchievementUnlock(long lastAchievementUnlock) { this.lastAchievementUnlock = lastAchievementUnlock; }
        
        public long getLastLevelUp() { return lastLevelUp; }
        public void setLastLevelUp(long lastLevelUp) { this.lastLevelUp = lastLevelUp; }
        
        public String getLastLocation() { return lastLocation; }
        public void setLastLocation(String lastLocation) { this.lastLocation = lastLocation; }
    }
    
    /**
     * Activity entry data class
     */
    public static class ActivityEntry {
        private String activityType;
        private String description;
        private long timestamp;
        private Map<String, Object> data;
        
        public ActivityEntry() {} // Required for Firebase
        
        public ActivityEntry(String activityType, String description, Map<String, Object> data) {
            this.activityType = activityType;
            this.description = description;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getActivityType() { return activityType; }
        public void setActivityType(String activityType) { this.activityType = activityType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
    
    /**
     * Ranking snapshot for history tracking
     */
    public static class RankingSnapshot {
        private long timestamp;
        private int globalRank;
        private long totalScore;
        private String period; // daily, weekly, monthly
        
        public RankingSnapshot() {} // Required for Firebase
        
        public RankingSnapshot(int globalRank, long totalScore, String period) {
            this.timestamp = System.currentTimeMillis();
            this.globalRank = globalRank;
            this.totalScore = totalScore;
            this.period = period;
        }
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public int getGlobalRank() { return globalRank; }
        public void setGlobalRank(int globalRank) { this.globalRank = globalRank; }
        
        public long getTotalScore() { return totalScore; }
        public void setTotalScore(long totalScore) { this.totalScore = totalScore; }
        
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
    }
    
    // Constructors
    public PlayerProfile() {
        // Required for Firebase
        this.statistics = new PlayerStatistics();
        this.rankings = new PlayerRankings();
        this.unlockedAchievements = new ArrayList<>();
        this.badges = new ArrayList<>();
        this.recentActivity = new PlayerActivity();
        this.friends = new ArrayList<>();
        this.preferences = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public PlayerProfile(String playerId, String displayName, String email) {
        this();
        this.playerId = playerId;
        this.displayName = displayName;
        this.email = email;
        this.level = 1;
        this.experience = 0;
        this.totalExperience = 0;
        this.coins = 100;
        this.totalBounty = 0;
        this.createdAt = System.currentTimeMillis();
        this.lastActiveAt = System.currentTimeMillis();
        this.currentTitle = "Rookie Pirate";
    }
    
    // Utility methods
    @Exclude
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId);
        map.put("displayName", displayName);
        map.put("email", email);
        map.put("avatarUrl", avatarUrl);
        map.put("createdAt", createdAt);
        map.put("lastActiveAt", ServerValue.TIMESTAMP);
        map.put("level", level);
        map.put("experience", experience);
        map.put("totalExperience", totalExperience);
        map.put("coins", coins);
        map.put("totalBounty", totalBounty);
        map.put("statistics", statistics);
        map.put("rankings", rankings);
        map.put("unlockedAchievements", unlockedAchievements);
        map.put("badges", badges);
        map.put("currentTitle", currentTitle);
        map.put("recentActivity", recentActivity);
        map.put("friends", friends);
        map.put("teamId", teamId);
        map.put("preferences", preferences);
        map.put("metadata", metadata);
        return map;
    }
    
    @Exclude
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
    
    @Exclude
    public String getTierName() {
        if (rankings.getGlobalRank() <= 10) return "Legendary Pirate";
        else if (rankings.getGlobalRank() <= 50) return "Master Pirate";
        else if (rankings.getGlobalRank() <= 100) return "Expert Pirate";
        else if (rankings.getGlobalRank() <= 500) return "Skilled Pirate";
        else if (rankings.getGlobalRank() <= 1000) return "Experienced Pirate";
        else return "Rookie Pirate";
    }
    
    // Getters and setters
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(long lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public long getExperience() { return experience; }
    public void setExperience(long experience) { this.experience = experience; }
    
    public long getTotalExperience() { return totalExperience; }
    public void setTotalExperience(long totalExperience) { this.totalExperience = totalExperience; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    public long getTotalBounty() { return totalBounty; }
    public void setTotalBounty(long totalBounty) { this.totalBounty = totalBounty; }
    
    public PlayerStatistics getStatistics() { return statistics; }
    public void setStatistics(PlayerStatistics statistics) { this.statistics = statistics; }
    
    public PlayerRankings getRankings() { return rankings; }
    public void setRankings(PlayerRankings rankings) { this.rankings = rankings; }
    
    public List<String> getUnlockedAchievements() { return unlockedAchievements; }
    public void setUnlockedAchievements(List<String> unlockedAchievements) { this.unlockedAchievements = unlockedAchievements; }
    
    public List<PlayerBadge> getBadges() { return badges; }
    public void setBadges(List<PlayerBadge> badges) { this.badges = badges; }
    
    public String getCurrentTitle() { return currentTitle; }
    public void setCurrentTitle(String currentTitle) { this.currentTitle = currentTitle; }
    
    public PlayerActivity getRecentActivity() { return recentActivity; }
    public void setRecentActivity(PlayerActivity recentActivity) { this.recentActivity = recentActivity; }
    
    public List<String> getFriends() { return friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }
    
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    
    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
