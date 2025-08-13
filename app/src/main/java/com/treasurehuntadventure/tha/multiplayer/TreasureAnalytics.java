package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.treasurehuntadventure.tha.Treasure;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analytics system for tracking treasure spawning patterns and player behavior
 */
public class TreasureAnalytics {
    private static final String TAG = "TreasureAnalytics";
    private static final String ANALYTICS_COLLECTION = "treasure_analytics";
    
    private final FirebaseFirestore firestore;
    private final Map<String, SpawnMetrics> areaMetrics;
    private final Map<TreasureRarity, Integer> raritySpawnCounts;
    private final Map<String, Long> playerSpawnTimes;
    
    public TreasureAnalytics() {
        this.firestore = FirebaseFirestore.getInstance();
        this.areaMetrics = new ConcurrentHashMap<>();
        this.raritySpawnCounts = new ConcurrentHashMap<>();
        this.playerSpawnTimes = new ConcurrentHashMap<>();
        
        // Initialize rarity counters
        for (TreasureRarity rarity : TreasureRarity.values()) {
            raritySpawnCounts.put(rarity, 0);
        }
    }
    
    /**
     * Records a treasure spawn event for analytics
     */
    public void recordTreasureSpawn(Treasure treasure, Location playerLocation, int playerLevel) {
        try {
            String areaKey = getAreaKey(playerLocation);
            long timestamp = System.currentTimeMillis();
            
            // Update local metrics
            updateLocalMetrics(areaKey, treasure.getRarity(), playerLevel, timestamp);
            
            // Create analytics document
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("treasureId", treasure.getId());
            analyticsData.put("rarity", treasure.getRarity().name());
            analyticsData.put("spawnLatitude", treasure.getLocation().getLatitude());
            analyticsData.put("spawnLongitude", treasure.getLocation().getLongitude());
            analyticsData.put("playerLatitude", playerLocation.getLatitude());
            analyticsData.put("playerLongitude", playerLocation.getLongitude());
            analyticsData.put("playerLevel", playerLevel);
            analyticsData.put("areaKey", areaKey);
            analyticsData.put("timestamp", timestamp);
            analyticsData.put("eventType", "SPAWN");
            
            // Save to Firestore (async)
            firestore.collection(ANALYTICS_COLLECTION)
                    .add(analyticsData)
                    .addOnSuccessListener(documentReference -> 
                            Log.d(TAG, "Analytics recorded for treasure spawn: " + treasure.getId()))
                    .addOnFailureListener(e -> 
                            Log.w(TAG, "Failed to record analytics for treasure spawn", e));
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording treasure spawn analytics", e);
        }
    }
    
    /**
     * Records a treasure capture event
     */
    public void recordTreasureCapture(String treasureId, String playerId, Location captureLocation, 
                                    TreasureRarity rarity, long spawnTime) {
        try {
            long captureTime = System.currentTimeMillis();
            long treasureLifetime = captureTime - spawnTime;
            String areaKey = getAreaKey(captureLocation);
            
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("treasureId", treasureId);
            analyticsData.put("playerId", playerId);
            analyticsData.put("rarity", rarity.name());
            analyticsData.put("captureLatitude", captureLocation.getLatitude());
            analyticsData.put("captureLongitude", captureLocation.getLongitude());
            analyticsData.put("areaKey", areaKey);
            analyticsData.put("spawnTime", spawnTime);
            analyticsData.put("captureTime", captureTime);
            analyticsData.put("treasureLifetime", treasureLifetime);
            analyticsData.put("eventType", "CAPTURE");
            
            // Update area metrics
            SpawnMetrics metrics = areaMetrics.get(areaKey);
            if (metrics != null) {
                metrics.recordCapture(rarity, treasureLifetime);
            }
            
            firestore.collection(ANALYTICS_COLLECTION)
                    .add(analyticsData)
                    .addOnSuccessListener(documentReference -> 
                            Log.d(TAG, "Analytics recorded for treasure capture: " + treasureId))
                    .addOnFailureListener(e -> 
                            Log.w(TAG, "Failed to record analytics for treasure capture", e));
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording treasure capture analytics", e);
        }
    }
    
    /**
     * Records player movement and activity patterns
     */
    public void recordPlayerActivity(String playerId, Location location, String activityType) {
        try {
            String areaKey = getAreaKey(location);
            long timestamp = System.currentTimeMillis();
            
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("playerId", playerId);
            analyticsData.put("latitude", location.getLatitude());
            analyticsData.put("longitude", location.getLongitude());
            analyticsData.put("areaKey", areaKey);
            analyticsData.put("activityType", activityType);
            analyticsData.put("timestamp", timestamp);
            analyticsData.put("eventType", "PLAYER_ACTIVITY");
            
            // Track player spawn frequency
            String playerAreaKey = playerId + "_" + areaKey;
            Long lastSpawn = playerSpawnTimes.get(playerAreaKey);
            if (lastSpawn != null) {
                analyticsData.put("timeSinceLastActivity", timestamp - lastSpawn);
            }
            playerSpawnTimes.put(playerAreaKey, timestamp);
            
            firestore.collection(ANALYTICS_COLLECTION)
                    .add(analyticsData)
                    .addOnFailureListener(e -> 
                            Log.w(TAG, "Failed to record player activity analytics", e));
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording player activity analytics", e);
        }
    }
    
    /**
     * Gets spawn statistics for an area
     */
    public SpawnMetrics getAreaMetrics(Location location) {
        String areaKey = getAreaKey(location);
        return areaMetrics.getOrDefault(areaKey, new SpawnMetrics());
    }
    
    /**
     * Gets global rarity distribution statistics
     */
    public Map<TreasureRarity, Integer> getRarityDistribution() {
        return new HashMap<>(raritySpawnCounts);
    }
    
    /**
     * Calculates spawn density for balancing
     */
    public double calculateSpawnDensity(Location location, double radiusKm) {
        String centerAreaKey = getAreaKey(location);
        SpawnMetrics centerMetrics = areaMetrics.get(centerAreaKey);
        
        if (centerMetrics == null) {
            return 0.0;
        }
        
        // Simple density calculation based on spawns per hour in the area
        long currentTime = System.currentTimeMillis();
        long oneHourAgo = currentTime - (60 * 60 * 1000);
        
        return centerMetrics.getSpawnsInTimeRange(oneHourAgo, currentTime);
    }
    
    /**
     * Determines if an area is oversaturated with treasures
     */
    public boolean isAreaOversaturated(Location location) {
        double density = calculateSpawnDensity(location, 1.0); // 1km radius
        return density > 10; // More than 10 spawns per hour indicates oversaturation
    }
    
    /**
     * Gets recommended spawn adjustments based on analytics
     */
    public SpawnRecommendation getSpawnRecommendation(Location location, int playerLevel) {
        SpawnMetrics metrics = getAreaMetrics(location);
        double density = calculateSpawnDensity(location, 1.0);
        
        SpawnRecommendation recommendation = new SpawnRecommendation();
        
        // Adjust spawn rate based on density
        if (density < 2) {
            recommendation.spawnRateMultiplier = 1.5; // Increase spawns in low-activity areas
        } else if (density > 8) {
            recommendation.spawnRateMultiplier = 0.5; // Decrease spawns in high-activity areas
        } else {
            recommendation.spawnRateMultiplier = 1.0;
        }
        
        // Adjust rarity based on capture rates
        double captureRate = metrics.getCaptureRate();
        if (captureRate > 0.8) {
            recommendation.rarityBonus = 0.1; // Increase rare spawns if capture rate is high
        } else if (captureRate < 0.3) {
            recommendation.rarityBonus = -0.1; // Decrease rare spawns if capture rate is low
        }
        
        return recommendation;
    }
    
    private void updateLocalMetrics(String areaKey, TreasureRarity rarity, int playerLevel, long timestamp) {
        SpawnMetrics metrics = areaMetrics.computeIfAbsent(areaKey, k -> new SpawnMetrics());
        metrics.recordSpawn(rarity, playerLevel, timestamp);
        
        // Update global rarity counts
        raritySpawnCounts.merge(rarity, 1, Integer::sum);
    }
    
    private String getAreaKey(Location location) {
        // Create area key based on rounded coordinates (1km grid)
        int latGrid = (int) Math.round(location.getLatitude() * 100);
        int lonGrid = (int) Math.round(location.getLongitude() * 100);
        return latGrid + "," + lonGrid;
    }
    
    /**
     * Metrics for a specific area
     */
    public static class SpawnMetrics {
        private final Map<TreasureRarity, Integer> spawnCounts = new HashMap<>();
        private final Map<TreasureRarity, Integer> captureCounts = new HashMap<>();
        private final Map<Long, TreasureRarity> spawnTimeline = new HashMap<>();
        private int totalSpawns = 0;
        private int totalCaptures = 0;
        private long totalLifetime = 0;
        
        public SpawnMetrics() {
            for (TreasureRarity rarity : TreasureRarity.values()) {
                spawnCounts.put(rarity, 0);
                captureCounts.put(rarity, 0);
            }
        }
        
        public void recordSpawn(TreasureRarity rarity, int playerLevel, long timestamp) {
            spawnCounts.merge(rarity, 1, Integer::sum);
            spawnTimeline.put(timestamp, rarity);
            totalSpawns++;
        }
        
        public void recordCapture(TreasureRarity rarity, long lifetime) {
            captureCounts.merge(rarity, 1, Integer::sum);
            totalCaptures++;
            totalLifetime += lifetime;
        }
        
        public double getCaptureRate() {
            return totalSpawns > 0 ? (double) totalCaptures / totalSpawns : 0.0;
        }
        
        public double getAverageLifetime() {
            return totalCaptures > 0 ? (double) totalLifetime / totalCaptures : 0.0;
        }
        
        public int getSpawnsInTimeRange(long startTime, long endTime) {
            return (int) spawnTimeline.entrySet().stream()
                    .filter(entry -> entry.getKey() >= startTime && entry.getKey() <= endTime)
                    .count();
        }
        
        public Map<TreasureRarity, Integer> getSpawnCounts() {
            return new HashMap<>(spawnCounts);
        }
        
        public Map<TreasureRarity, Integer> getCaptureCounts() {
            return new HashMap<>(captureCounts);
        }
    }
    
    /**
     * Spawn recommendation based on analytics
     */
    public static class SpawnRecommendation {
        public double spawnRateMultiplier = 1.0;
        public double rarityBonus = 0.0;
        public boolean shouldSpawn = true;
        public String reason = "";
    }
}
