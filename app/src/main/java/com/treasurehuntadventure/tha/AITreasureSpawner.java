package com.treasurehuntadventure.tha;

import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI-driven treasure spawning system that intelligently places treasures
 * based on various factors including geographic features, player behavior,
 * time-based events, and dynamic difficulty.
 */
public class AITreasureSpawner {
    private static final String TAG = "AITreasureSpawner";
    
    // Spawn radius configurations
    private static final double MIN_SPAWN_RADIUS = 0.005; // ~500m
    private static final double MAX_SPAWN_RADIUS = 0.02;  // ~2km
    private static final double SAFE_ZONE_RADIUS = 0.001; // ~100m around player
    
    // Time-based event configurations
    private static final int MORNING_HOUR_START = 6;
    private static final int MORNING_HOUR_END = 10;
    private static final int EVENING_HOUR_START = 18;
    private static final int EVENING_HOUR_END = 22;
    
    // Difficulty and rarity weights
    private static final Map<TreasureRarity, Double> TIME_BASED_RARITY_WEIGHTS = new HashMap<>();
    private static final Map<TreasureRarity, Double> DISTANCE_BASED_RARITY_WEIGHTS = new HashMap<>();
    
    static {
        // Morning/Evening special time weights (higher chance for rare treasures)
        TIME_BASED_RARITY_WEIGHTS.put(TreasureRarity.COMMON, 0.3);
        TIME_BASED_RARITY_WEIGHTS.put(TreasureRarity.UNCOMMON, 0.3);
        TIME_BASED_RARITY_WEIGHTS.put(TreasureRarity.RARE, 0.25);
        TIME_BASED_RARITY_WEIGHTS.put(TreasureRarity.EPIC, 0.1);
        TIME_BASED_RARITY_WEIGHTS.put(TreasureRarity.LEGENDARY, 0.05);
        
        // Distance-based weights (farther = rarer)
        DISTANCE_BASED_RARITY_WEIGHTS.put(TreasureRarity.COMMON, 0.5);
        DISTANCE_BASED_RARITY_WEIGHTS.put(TreasureRarity.UNCOMMON, 0.3);
        DISTANCE_BASED_RARITY_WEIGHTS.put(TreasureRarity.RARE, 0.15);
        DISTANCE_BASED_RARITY_WEIGHTS.put(TreasureRarity.EPIC, 0.04);
        DISTANCE_BASED_RARITY_WEIGHTS.put(TreasureRarity.LEGENDARY, 0.01);
    }
    
    private final Random random = new Random();
    private final List<GeoPoint> playerVisitHistory = new ArrayList<>();
    private final Map<GeoPoint, Integer> areaVisitFrequency = new HashMap<>();
    
    /**
     * Generates an AI-driven treasure spawn location and rarity based on multiple factors.
     * 
     * @param playerLocation Current player location
     * @param playerLevel Current player level
     * @param existingTreasures List of existing treasure locations to avoid clustering
     * @return A TreasureSpawnData object containing location and rarity
     */
    public TreasureSpawnData generateIntelligentSpawn(Location playerLocation, int playerLevel, List<GeoPoint> existingTreasures) {
        // Record player visit
        GeoPoint playerGeoPoint = new GeoPoint(playerLocation.getLatitude(), playerLocation.getLongitude());
        recordPlayerVisit(playerGeoPoint);
        
        // Generate spawn location
        GeoPoint spawnLocation = generateSmartLocation(playerGeoPoint, existingTreasures);
        
        // Determine rarity based on multiple factors
        TreasureRarity rarity = determineSmartRarity(playerGeoPoint, spawnLocation, playerLevel);
        
        // Adjust based on special conditions
        if (isSpecialTimeEvent()) {
            rarity = possiblyUpgradeRarity(rarity);
            Log.d(TAG, "Special time event active! Rarity potentially upgraded.");
        }
        
        return new TreasureSpawnData(spawnLocation, rarity);
    }
    
    /**
     * Generates a smart location avoiding existing treasures and considering visited areas.
     */
    private GeoPoint generateSmartLocation(GeoPoint playerLocation, List<GeoPoint> existingTreasures) {
        int maxAttempts = 50;
        GeoPoint bestLocation = null;
        double bestScore = -1;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate candidate location
            double distance = MIN_SPAWN_RADIUS + random.nextDouble() * (MAX_SPAWN_RADIUS - MIN_SPAWN_RADIUS);
            double angle = random.nextDouble() * 2 * Math.PI;
            
            double latOffset = distance * Math.cos(angle);
            double lonOffset = distance * Math.sin(angle);
            
            GeoPoint candidate = new GeoPoint(
                playerLocation.getLatitude() + latOffset,
                playerLocation.getLongitude() + lonOffset
            );
            
            // Calculate location score
            double score = calculateLocationScore(candidate, playerLocation, existingTreasures);
            
            if (score > bestScore) {
                bestScore = score;
                bestLocation = candidate;
            }
        }
        
        return bestLocation != null ? bestLocation : generateFallbackLocation(playerLocation);
    }
    
    /**
     * Calculates a score for a potential treasure location.
     * Higher scores indicate better locations.
     */
    private double calculateLocationScore(GeoPoint candidate, GeoPoint playerLocation, List<GeoPoint> existingTreasures) {
        double score = 1.0;
        
        // Penalty for being too close to player (avoid spawn camping)
        double playerDistance = calculateDistance(candidate, playerLocation);
        if (playerDistance < SAFE_ZONE_RADIUS) {
            score *= 0.1;
        }
        
        // Penalty for being too close to existing treasures
        for (GeoPoint existing : existingTreasures) {
            double treasureDistance = calculateDistance(candidate, existing);
            if (treasureDistance < 0.002) { // ~200m
                score *= 0.3;
            }
        }
        
        // Bonus for unexplored areas
        int visitCount = getAreaVisitCount(candidate);
        if (visitCount == 0) {
            score *= 2.0; // Double score for unvisited areas
        } else {
            score *= (1.0 / (1.0 + visitCount * 0.1)); // Decrease score for frequently visited areas
        }
        
        // Bonus for interesting spawn patterns (e.g., cardinal directions)
        if (isCardinalDirection(playerLocation, candidate)) {
            score *= 1.3;
        }
        
        return score;
    }
    
    /**
     * Determines treasure rarity based on multiple intelligent factors.
     */
    private TreasureRarity determineSmartRarity(GeoPoint playerLocation, GeoPoint treasureLocation, int playerLevel) {
        // Calculate distance factor
        double distance = calculateDistance(playerLocation, treasureLocation);
        double distanceFactor = Math.min(distance / MAX_SPAWN_RADIUS, 1.0);
        
        // Player level factor (higher level = better rarities)
        double levelFactor = Math.min(playerLevel / 50.0, 1.0); // Assumes max level ~50
        
        // Time-based factor
        boolean isSpecialTime = isSpecialTimeEvent();
        
        // Choose weight distribution
        Map<TreasureRarity, Double> weights;
        if (isSpecialTime) {
            weights = TIME_BASED_RARITY_WEIGHTS;
        } else {
            weights = DISTANCE_BASED_RARITY_WEIGHTS;
        }
        
        // Apply level and distance modifiers
        Map<TreasureRarity, Double> modifiedWeights = new HashMap<>();
        for (Map.Entry<TreasureRarity, Double> entry : weights.entrySet()) {
            double weight = entry.getValue();
            TreasureRarity rarity = entry.getKey();
            
            // Increase rare treasure chances based on player level and distance
            if (rarity.ordinal() >= TreasureRarity.RARE.ordinal()) {
                weight *= (1.0 + levelFactor * 0.5);
                weight *= (1.0 + distanceFactor * 0.3);
            }
            
            modifiedWeights.put(rarity, weight);
        }
        
        // Normalize weights and select rarity
        return selectRarityFromWeights(modifiedWeights);
    }
    
    /**
     * Selects a rarity based on weighted probabilities.
     */
    private TreasureRarity selectRarityFromWeights(Map<TreasureRarity, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        
        double cumulativeWeight = 0.0;
        for (Map.Entry<TreasureRarity, Double> entry : weights.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue <= cumulativeWeight) {
                return entry.getKey();
            }
        }
        
        return TreasureRarity.COMMON; // Fallback
    }
    
    /**
     * Checks if current time is during a special event period.
     */
    private boolean isSpecialTimeEvent() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        // Morning golden hour or evening golden hour
        return (hour >= MORNING_HOUR_START && hour < MORNING_HOUR_END) ||
               (hour >= EVENING_HOUR_START && hour < EVENING_HOUR_END);
    }
    
    /**
     * Possibly upgrades rarity during special events.
     */
    private TreasureRarity possiblyUpgradeRarity(TreasureRarity currentRarity) {
        if (random.nextDouble() < 0.3) { // 30% chance to upgrade
            int nextOrdinal = Math.min(currentRarity.ordinal() + 1, TreasureRarity.values().length - 1);
            return TreasureRarity.values()[nextOrdinal];
        }
        return currentRarity;
    }
    
    /**
     * Records player visit for behavioral pattern analysis.
     */
    private void recordPlayerVisit(GeoPoint location) {
        playerVisitHistory.add(location);
        if (playerVisitHistory.size() > 100) {
            playerVisitHistory.remove(0); // Keep history manageable
        }
        
        // Update area visit frequency
        GeoPoint roundedLocation = roundToGrid(location);
        areaVisitFrequency.merge(roundedLocation, 1, Integer::sum);
    }
    
    /**
     * Gets visit count for an area.
     */
    private int getAreaVisitCount(GeoPoint location) {
        GeoPoint roundedLocation = roundToGrid(location);
        return areaVisitFrequency.getOrDefault(roundedLocation, 0);
    }
    
    /**
     * Rounds location to a grid for area tracking.
     */
    private GeoPoint roundToGrid(GeoPoint location) {
        double gridSize = 0.001; // ~100m grid
        double lat = Math.round(location.getLatitude() / gridSize) * gridSize;
        double lon = Math.round(location.getLongitude() / gridSize) * gridSize;
        return new GeoPoint(lat, lon);
    }
    
    /**
     * Checks if treasure is in cardinal direction from player.
     */
    private boolean isCardinalDirection(GeoPoint player, GeoPoint treasure) {
        double latDiff = Math.abs(treasure.getLatitude() - player.getLatitude());
        double lonDiff = Math.abs(treasure.getLongitude() - player.getLongitude());
        double ratio = latDiff / (lonDiff + 0.0001); // Avoid division by zero
        
        // Check if roughly N/S/E/W (ratio very high or very low)
        return ratio > 10 || ratio < 0.1;
    }
    
    /**
     * Calculates distance between two GeoPoints.
     */
    private double calculateDistance(GeoPoint p1, GeoPoint p2) {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }
    
    /**
     * Generates fallback location using simple random offset.
     */
    private GeoPoint generateFallbackLocation(GeoPoint playerLocation) {
        double latOffset = (random.nextDouble() - 0.5) * 0.02;
        double lonOffset = (random.nextDouble() - 0.5) * 0.02;
        return new GeoPoint(
            playerLocation.getLatitude() + latOffset,
            playerLocation.getLongitude() + lonOffset
        );
    }
    
    /**
     * Data class for treasure spawn information.
     */
    public static class TreasureSpawnData {
        private final GeoPoint location;
        private final TreasureRarity rarity;
        
        public TreasureSpawnData(GeoPoint location, TreasureRarity rarity) {
            this.location = location;
            this.rarity = rarity;
        }
        
        public GeoPoint getLocation() {
            return location;
        }
        
        public TreasureRarity getRarity() {
            return rarity;
        }
    }
}
