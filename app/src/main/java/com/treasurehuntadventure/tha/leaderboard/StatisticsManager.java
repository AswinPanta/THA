package com.treasurehuntadventure.tha.leaderboard;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive statistics manager for tracking detailed player metrics and performance analytics
 */
public class StatisticsManager {
    private static final String TAG = "StatisticsManager";
    
    // Firestore collections
    private static final String PLAYER_PROFILES_COLLECTION = "player_profiles";
    private static final String STATISTICS_COLLECTION = "player_statistics";
    private static final String DAILY_STATS_COLLECTION = "daily_statistics";
    private static final String ACHIEVEMENTS_COLLECTION = "player_achievements";
    
    private final Context context;
    private final FirebaseFirestore firestore;
    
    // Local statistics cache
    private final Map<String, PlayerProfile.PlayerStatistics> statsCache = new ConcurrentHashMap<>();
    
    // Session tracking
    private long sessionStartTime;
    private Location sessionStartLocation;
    private int sessionTreasuresCaptured;
    private double sessionDistanceWalked;
    
    // Statistics event listener
    private StatisticsEventListener eventListener;
    
    public interface StatisticsEventListener {
        void onStatisticUpdated(String playerId, String statistic, Object oldValue, Object newValue);
        void onMilestoneReached(String playerId, String milestone, Object value);
        void onPersonalRecordBroken(String playerId, String record, Object oldRecord, Object newRecord);
        void onStatisticsError(String error);
    }
    
    public StatisticsManager(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.sessionStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "Statistics manager initialized");
    }
    
    /**
     * Starts a new session for statistics tracking
     */
    public void startSession(String playerId, Location startLocation) {
        this.sessionStartTime = System.currentTimeMillis();
        this.sessionStartLocation = startLocation;
        this.sessionTreasuresCaptured = 0;
        this.sessionDistanceWalked = 0.0;
        
        // Update last active timestamp
        updatePlayerActivity(playerId);
        
        Log.d(TAG, "Session started for player: " + playerId);
    }
    
    /**
     * Ends current session and saves session statistics
     */
    public CompletableFuture<Void> endSession(String playerId, Location endLocation) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        
        // Calculate session distance if we have start location
        double sessionDistance = 0.0;
        if (sessionStartLocation != null && endLocation != null) {
            sessionDistance = sessionStartLocation.distanceTo(endLocation);
        }
        
        // Create session summary
        Map<String, Object> sessionStats = new HashMap<>();
        sessionStats.put("duration", sessionDuration);
        sessionStats.put("treasuresCaptured", sessionTreasuresCaptured);
        sessionStats.put("distanceWalked", sessionDistance + sessionDistanceWalked);
        sessionStats.put("startTime", sessionStartTime);
        sessionStats.put("endTime", System.currentTimeMillis());
        
        // Update player statistics
        updateStatistics(playerId, "timePlayed", sessionDuration, StatUpdateType.INCREMENT)
                .thenCompose(aVoid -> updateStatistics(playerId, "distanceWalked", 
                        sessionDistance + sessionDistanceWalked, StatUpdateType.INCREMENT))
                .thenAccept(aVoid -> {
                    Log.d(TAG, "Session ended for player: " + playerId + 
                            ", Duration: " + sessionDuration + "ms, Distance: " + 
                            (sessionDistance + sessionDistanceWalked) + "m");
                    future.complete(null);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to end session", throwable);
                    future.completeExceptionally(throwable);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Records treasure capture statistics
     */
    public CompletableFuture<Void> recordTreasureCapture(String playerId, TreasureRarity rarity, 
                                                       long captureTime, Location location) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        sessionTreasuresCaptured++;
        
        // Update multiple statistics
        WriteBatch batch = firestore.batch();
        
        // Update total treasures captured
        updateStatisticInBatch(batch, playerId, "treasuresCaptured", 1, StatUpdateType.INCREMENT);
        
        // Update rarity-specific count
        String rarityField = "treasuresByRarity." + rarity.name();
        updateStatisticInBatch(batch, playerId, rarityField, 1, StatUpdateType.INCREMENT);
        
        // Update capture time records
        if (captureTime > 0) {
            updateCaptureTimeRecords(batch, playerId, captureTime);
        }
        
        // Update location-based stats
        if (location != null) {
            updateLocationStats(batch, playerId, location);
        }
        
        // Update daily capture count
        updateDailyStatistic(batch, playerId, "treasuresCaptured", 1);
        
        // Check for time-based achievements
        checkTimeBasedCapture(batch, playerId);
        
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (eventListener != null) {
                        eventListener.onStatisticUpdated(playerId, "treasuresCaptured", 
                                sessionTreasuresCaptured - 1, sessionTreasuresCaptured);
                    }
                    
                    // Check for milestones
                    checkTreasureMilestones(playerId, sessionTreasuresCaptured);
                    
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to record treasure capture", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Records distance walked
     */
    public CompletableFuture<Void> recordDistanceWalked(String playerId, double distance, Location location) {
        sessionDistanceWalked += distance;
        
        return updateStatistics(playerId, "distanceWalked", distance, StatUpdateType.INCREMENT)
                .thenCompose(aVoid -> {
                    // Update max distance from home if location provided
                    if (location != null) {
                        return updateMaxDistanceFromHome(playerId, location);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
    
    /**
     * Records social interaction
     */
    public CompletableFuture<Void> recordSocialInteraction(String playerId, String interactionType, 
                                                         Map<String, Object> details) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        WriteBatch batch = firestore.batch();
        
        switch (interactionType) {
            case "chat_message":
                updateStatisticInBatch(batch, playerId, "chatMessagesSent", 1, StatUpdateType.INCREMENT);
                break;
            case "help_request_answered":
                updateStatisticInBatch(batch, playerId, "helpRequestsAnswered", 1, StatUpdateType.INCREMENT);
                break;
            case "team_joined":
                updateStatisticInBatch(batch, playerId, "teamsJoined", 1, StatUpdateType.INCREMENT);
                break;
        }
        
        // Update daily social stats
        updateDailyStatistic(batch, playerId, "socialInteractions", 1);
        
        batch.commit()
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Records achievement unlock
     */
    public CompletableFuture<Void> recordAchievementUnlock(String playerId, String achievementId, 
                                                         int xpReward, int coinsReward) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        WriteBatch batch = firestore.batch();
        
        // Update achievement count
        updateStatisticInBatch(batch, playerId, "achievementsUnlocked", 1, StatUpdateType.INCREMENT);
        
        // Update rewards earned
        updateStatisticInBatch(batch, playerId, "totalXpEarned", xpReward, StatUpdateType.INCREMENT);
        updateStatisticInBatch(batch, playerId, "totalCoinsEarned", coinsReward, StatUpdateType.INCREMENT);
        
        // Record achievement unlock time
        Map<String, Object> achievementData = new HashMap<>();
        achievementData.put("achievementId", achievementId);
        achievementData.put("unlockedAt", System.currentTimeMillis());
        achievementData.put("xpReward", xpReward);
        achievementData.put("coinsReward", coinsReward);
        
        batch.set(firestore.collection(ACHIEVEMENTS_COLLECTION)
                .document(playerId)
                .collection("unlocked")
                .document(achievementId), achievementData);
        
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (eventListener != null) {
                        eventListener.onMilestoneReached(playerId, "achievement_unlocked", achievementId);
                    }
                    future.complete(null);
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Records level up
     */
    public CompletableFuture<Void> recordLevelUp(String playerId, int newLevel, long totalExperience) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        WriteBatch batch = firestore.batch();
        
        // Update level and experience
        updateStatisticInBatch(batch, playerId, "level", newLevel, StatUpdateType.SET);
        updateStatisticInBatch(batch, playerId, "totalExperience", totalExperience, StatUpdateType.SET);
        
        // Record level up activity
        Map<String, Object> activityData = new HashMap<>();
        activityData.put("activityType", "level_up");
        activityData.put("description", "Reached level " + newLevel);
        activityData.put("timestamp", System.currentTimeMillis());
        activityData.put("data", Map.of("newLevel", newLevel, "totalExperience", totalExperience));
        
        batch.set(firestore.collection(PLAYER_PROFILES_COLLECTION)
                .document(playerId)
                .collection("recent_activities")
                .document(), activityData);
        
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (eventListener != null) {
                        eventListener.onMilestoneReached(playerId, "level_up", newLevel);
                    }
                    future.complete(null);
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Gets comprehensive player statistics
     */
    public CompletableFuture<PlayerProfile.PlayerStatistics> getPlayerStatistics(String playerId) {
        CompletableFuture<PlayerProfile.PlayerStatistics> future = new CompletableFuture<>();
        
        // Check cache first
        PlayerProfile.PlayerStatistics cached = statsCache.get(playerId);
        if (cached != null) {
            future.complete(cached);
            return future;
        }
        
        firestore.collection(PLAYER_PROFILES_COLLECTION)
                .document(playerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PlayerProfile profile = documentSnapshot.toObject(PlayerProfile.class);
                        if (profile != null && profile.getStatistics() != null) {
                            statsCache.put(playerId, profile.getStatistics());
                            future.complete(profile.getStatistics());
                        } else {
                            future.complete(new PlayerProfile.PlayerStatistics());
                        }
                    } else {
                        future.complete(new PlayerProfile.PlayerStatistics());
                    }
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Gets daily statistics for player
     */
    public CompletableFuture<Map<String, Object>> getDailyStatistics(String playerId) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        String todayKey = getTodayKey();
        
        firestore.collection(DAILY_STATS_COLLECTION)
                .document(playerId)
                .collection("daily")
                .document(todayKey)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        future.complete(documentSnapshot.getData());
                    } else {
                        future.complete(new HashMap<>());
                    }
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Sets statistics event listener
     */
    public void setEventListener(StatisticsEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Clears statistics cache
     */
    public void clearCache() {
        statsCache.clear();
    }
    
    // Private helper methods
    private enum StatUpdateType {
        SET, INCREMENT, MAX, MIN
    }
    
    private CompletableFuture<Void> updateStatistics(String playerId, String field, Object value, 
                                                   StatUpdateType updateType) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        Map<String, Object> updates = new HashMap<>();
        
        switch (updateType) {
            case SET:
                updates.put("statistics." + field, value);
                break;
            case INCREMENT:
                updates.put("statistics." + field, FieldValue.increment((Number) value));
                break;
            case MAX:
                // For max values, we need to read current value first
                // This is a simplified version - in production, use a transaction
                updates.put("statistics." + field, value);
                break;
        }
        
        updates.put("lastActiveAt", System.currentTimeMillis());
        
        firestore.collection(PLAYER_PROFILES_COLLECTION)
                .document(playerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Invalidate cache
                    statsCache.remove(playerId);
                    future.complete(null);
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    private void updateStatisticInBatch(WriteBatch batch, String playerId, String field, 
                                      Object value, StatUpdateType updateType) {
        Map<String, Object> updates = new HashMap<>();
        
        switch (updateType) {
            case SET:
                updates.put("statistics." + field, value);
                break;
            case INCREMENT:
                updates.put("statistics." + field, FieldValue.increment((Number) value));
                break;
        }
        
        updates.put("lastActiveAt", System.currentTimeMillis());
        
        batch.update(firestore.collection(PLAYER_PROFILES_COLLECTION).document(playerId), updates);
    }
    
    private void updateDailyStatistic(WriteBatch batch, String playerId, String field, Object value) {
        String todayKey = getTodayKey();
        
        Map<String, Object> dailyUpdate = new HashMap<>();
        dailyUpdate.put(field, FieldValue.increment((Number) value));
        dailyUpdate.put("date", todayKey);
        dailyUpdate.put("lastUpdated", System.currentTimeMillis());
        
        batch.set(firestore.collection(DAILY_STATS_COLLECTION)
                .document(playerId)
                .collection("daily")
                .document(todayKey), dailyUpdate, com.google.firebase.firestore.SetOptions.merge());
    }
    
    private void updateCaptureTimeRecords(WriteBatch batch, String playerId, long captureTime) {
        // Update fastest capture time (this would need a transaction in production)
        updateStatisticInBatch(batch, playerId, "lastCaptureTime", captureTime, StatUpdateType.SET);
        
        // Update average capture time (simplified - would need proper calculation)
        updateStatisticInBatch(batch, playerId, "totalCaptureTime", captureTime, StatUpdateType.INCREMENT);
        updateStatisticInBatch(batch, playerId, "captureCount", 1, StatUpdateType.INCREMENT);
    }
    
    private void updateLocationStats(WriteBatch batch, String playerId, Location location) {
        // Update locations visited (simplified)
        updateStatisticInBatch(batch, playerId, "lastLatitude", location.getLatitude(), StatUpdateType.SET);
        updateStatisticInBatch(batch, playerId, "lastLongitude", location.getLongitude(), StatUpdateType.SET);
    }
    
    private void checkTimeBasedCapture(WriteBatch batch, String playerId) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        // Night time capture (10 PM to 6 AM)
        if (hour >= 22 || hour <= 6) {
            updateStatisticInBatch(batch, playerId, "nightTimeCaptures", 1, StatUpdateType.INCREMENT);
        }
    }
    
    private CompletableFuture<Void> updateMaxDistanceFromHome(String playerId, Location location) {
        // This would need the player's home location to calculate properly
        // For now, just update the current location
        return updateStatistics(playerId, "currentLatitude", location.getLatitude(), StatUpdateType.SET)
                .thenCompose(aVoid -> updateStatistics(playerId, "currentLongitude", 
                        location.getLongitude(), StatUpdateType.SET));
    }
    
    private void updatePlayerActivity(String playerId) {
        Map<String, Object> activityUpdate = new HashMap<>();
        activityUpdate.put("lastActiveAt", System.currentTimeMillis());
        
        firestore.collection(PLAYER_PROFILES_COLLECTION)
                .document(playerId)
                .update(activityUpdate)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update player activity", e));
    }
    
    private void checkTreasureMilestones(String playerId, int totalTreasures) {
        int[] milestones = {1, 5, 10, 25, 50, 100, 250, 500, 1000};
        
        for (int milestone : milestones) {
            if (totalTreasures == milestone) {
                if (eventListener != null) {
                    eventListener.onMilestoneReached(playerId, "treasures_captured", milestone);
                }
                break;
            }
        }
    }
    
    private String getTodayKey() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", 
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }
}
