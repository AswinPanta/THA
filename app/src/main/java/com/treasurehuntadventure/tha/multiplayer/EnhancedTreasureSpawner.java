package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.treasurehuntadventure.tha.Treasure;
import com.treasurehuntadventure.tha.TreasureRarity;

import org.osmdroid.util.GeoPoint as OsmGeoPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced treasure spawning system with intelligent algorithms for real-time multiplayer gameplay
 */
public class EnhancedTreasureSpawner {
    private static final String TAG = "EnhancedTreasureSpawner";
    
    // Spawn configuration constants
    private static final int MAX_TREASURES_PER_AREA = 8;
    private static final int MIN_TREASURES_PER_AREA = 3;
    private static final double SPAWN_RADIUS_KM = 2.0; // 2km radius around player
    private static final double MIN_TREASURE_DISTANCE_M = 100.0; // Minimum distance between treasures
    private static final double SAFE_ZONE_RADIUS_M = 50.0; // Safe zone around player
    private static final long TREASURE_LIFETIME_MS = 30 * 60 * 1000; // 30 minutes
    private static final long SPAWN_COOLDOWN_MS = 2 * 60 * 1000; // 2 minutes between spawns
    
    // Rarity probability weights
    private static final Map<TreasureRarity, Double> BASE_RARITY_WEIGHTS = new HashMap<>();
    private static final Map<TreasureRarity, Double> PEAK_HOUR_WEIGHTS = new HashMap<>();
    private static final Map<TreasureRarity, Double> DISTANCE_BONUS_WEIGHTS = new HashMap<>();
    
    static {
        // Base rarity distribution
        BASE_RARITY_WEIGHTS.put(TreasureRarity.COMMON, 0.50);
        BASE_RARITY_WEIGHTS.put(TreasureRarity.UNCOMMON, 0.30);
        BASE_RARITY_WEIGHTS.put(TreasureRarity.RARE, 0.15);
        BASE_RARITY_WEIGHTS.put(TreasureRarity.EPIC, 0.04);
        BASE_RARITY_WEIGHTS.put(TreasureRarity.LEGENDARY, 0.01);
        
        // Peak hours (morning/evening) bonus
        PEAK_HOUR_WEIGHTS.put(TreasureRarity.COMMON, 0.35);
        PEAK_HOUR_WEIGHTS.put(TreasureRarity.UNCOMMON, 0.30);
        PEAK_HOUR_WEIGHTS.put(TreasureRarity.RARE, 0.20);
        PEAK_HOUR_WEIGHTS.put(TreasureRarity.EPIC, 0.10);
        PEAK_HOUR_WEIGHTS.put(TreasureRarity.LEGENDARY, 0.05);
        
        // Distance bonus (farther from player = rarer)
        DISTANCE_BONUS_WEIGHTS.put(TreasureRarity.COMMON, 0.30);
        DISTANCE_BONUS_WEIGHTS.put(TreasureRarity.UNCOMMON, 0.25);
        DISTANCE_BONUS_WEIGHTS.put(TreasureRarity.RARE, 0.25);
        DISTANCE_BONUS_WEIGHTS.put(TreasureRarity.EPIC, 0.15);
        DISTANCE_BONUS_WEIGHTS.put(TreasureRarity.LEGENDARY, 0.05);
    }
    
    private final FirebaseFirestore firestore;
    private final Random random;
    private final Map<String, Long> lastSpawnTimes;
    private final TreasureAnalytics analytics;
    
    public EnhancedTreasureSpawner() {
        this.firestore = FirebaseFirestore.getInstance();
        this.random = new Random();
        this.lastSpawnTimes = new HashMap<>();
        this.analytics = new TreasureAnalytics();
    }
    
    /**
     * Spawns treasures intelligently based on player location and game state
     */
    public CompletableFuture<List<Treasure>> spawnTreasuresForArea(Location playerLocation, String playerId, int playerLevel) {
        CompletableFuture<List<Treasure>> future = new CompletableFuture<>();
        
        // Check spawn cooldown
        String areaKey = getAreaKey(playerLocation);
        Long lastSpawn = lastSpawnTimes.get(areaKey);
        if (lastSpawn != null && System.currentTimeMillis() - lastSpawn < SPAWN_COOLDOWN_MS) {
            future.complete(new ArrayList<>());
            return future;
        }
        
        // Get existing treasures in the area
        getExistingTreasuresInArea(playerLocation).thenAccept(existingTreasures -> {
            try {
                List<Treasure> newTreasures = new ArrayList<>();
                int currentCount = existingTreasures.size();
                int targetCount = calculateTargetTreasureCount(playerLevel, currentCount);
                int toSpawn = Math.max(0, targetCount - currentCount);
                
                Log.d(TAG, "Area " + areaKey + " has " + currentCount + " treasures, spawning " + toSpawn + " more");
                
                if (toSpawn > 0) {
                    List<OsmGeoPoint> existingLocations = extractLocations(existingTreasures);
                    
                    for (int i = 0; i < toSpawn; i++) {
                        SpawnCandidate candidate = generateSpawnCandidate(playerLocation, existingLocations, playerLevel);
                        if (candidate != null) {
                            Treasure treasure = createTreasure(candidate, playerId);
                            saveTreasureToFirestore(treasure);
                            newTreasures.add(treasure);
                            existingLocations.add(candidate.location);
                            
                            // Analytics tracking
                            analytics.recordTreasureSpawn(treasure, playerLocation, playerLevel);
                        }
                    }
                    
                    lastSpawnTimes.put(areaKey, System.currentTimeMillis());
                }
                
                future.complete(newTreasures);
            } catch (Exception e) {
                Log.e(TAG, "Error spawning treasures", e);
                future.completeExceptionally(e);
            }
        }).exceptionally(throwable -> {
            Log.e(TAG, "Error getting existing treasures", throwable);
            future.completeExceptionally(throwable);
            return null;
        });
        
        return future;
    }
    
    /**
     * Gets existing treasures in the area around player location
     */
    private CompletableFuture<List<Treasure>> getExistingTreasuresInArea(Location playerLocation) {
        CompletableFuture<List<Treasure>> future = new CompletableFuture<>();
        
        // Calculate bounding box for the area
        double[] bounds = calculateBoundingBox(playerLocation, SPAWN_RADIUS_KM);
        
        firestore.collection("treasures")
                .whereEqualTo("captured", false)
                .whereGreaterThanOrEqualTo("latitude", bounds[0])
                .whereLessThanOrEqualTo("latitude", bounds[1])
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Treasure> treasures = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Treasure treasure = documentToTreasure(doc);
                            if (treasure != null && isInRadius(playerLocation, treasure.getLocation(), SPAWN_RADIUS_KM * 1000)) {
                                treasures.add(treasure);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing treasure document: " + doc.getId(), e);
                        }
                    }
                    future.complete(treasures);
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Generates a smart spawn candidate location with appropriate rarity
     */
    private SpawnCandidate generateSpawnCandidate(Location playerLocation, List<OsmGeoPoint> existingLocations, int playerLevel) {
        int maxAttempts = 100;
        SpawnCandidate bestCandidate = null;
        double bestScore = -1;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random location within spawn radius
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = SAFE_ZONE_RADIUS_M + random.nextDouble() * (SPAWN_RADIUS_KM * 1000 - SAFE_ZONE_RADIUS_M);
            
            double deltaLat = (distance * Math.cos(angle)) / 111320.0; // Approximate meters to degrees
            double deltaLon = (distance * Math.sin(angle)) / (111320.0 * Math.cos(Math.toRadians(playerLocation.getLatitude())));
            
            OsmGeoPoint candidateLocation = new OsmGeoPoint(
                    playerLocation.getLatitude() + deltaLat,
                    playerLocation.getLongitude() + deltaLon
            );
            
            // Calculate location score
            double score = calculateLocationScore(candidateLocation, playerLocation, existingLocations);
            
            if (score > bestScore) {
                bestScore = score;
                TreasureRarity rarity = calculateRarity(playerLocation, candidateLocation, playerLevel);
                bestCandidate = new SpawnCandidate(candidateLocation, rarity, score);
            }
        }
        
        return bestCandidate;
    }
    
    /**
     * Calculates a score for a potential spawn location
     */
    private double calculateLocationScore(OsmGeoPoint candidate, Location playerLocation, List<OsmGeoPoint> existingLocations) {
        double score = 1.0;
        
        // Distance from player (prefer moderate distance)
        double playerDistance = calculateDistance(candidate, playerLocation);
        if (playerDistance < SAFE_ZONE_RADIUS_M) {
            score *= 0.1; // Heavy penalty for being too close
        } else if (playerDistance > SPAWN_RADIUS_KM * 1000 * 0.8) {
            score *= 0.7; // Light penalty for being too far
        }
        
        // Distance from existing treasures (avoid clustering)
        for (OsmGeoPoint existing : existingLocations) {
            double distance = calculateDistance(candidate, existing);
            if (distance < MIN_TREASURE_DISTANCE_M) {
                score *= 0.2; // Heavy penalty for clustering
            } else if (distance < MIN_TREASURE_DISTANCE_M * 2) {
                score *= 0.6; // Moderate penalty for being close
            }
        }
        
        // Add some randomness to prevent predictable patterns
        score *= (0.8 + random.nextDouble() * 0.4);
        
        return score;
    }
    
    /**
     * Calculates treasure rarity based on multiple factors
     */
    private TreasureRarity calculateRarity(Location playerLocation, OsmGeoPoint treasureLocation, int playerLevel) {
        Map<TreasureRarity, Double> weights = new HashMap<>(BASE_RARITY_WEIGHTS);
        
        // Time-based bonus
        if (isPeakHour()) {
            blendWeights(weights, PEAK_HOUR_WEIGHTS, 0.3);
        }
        
        // Distance-based bonus
        double distance = calculateDistance(treasureLocation, playerLocation);
        double distanceFactor = Math.min(distance / (SPAWN_RADIUS_KM * 1000), 1.0);
        if (distanceFactor > 0.6) {
            blendWeights(weights, DISTANCE_BONUS_WEIGHTS, distanceFactor * 0.4);
        }
        
        // Player level bonus (higher level = slightly better rarities)
        double levelBonus = Math.min(playerLevel / 50.0, 0.2);
        if (levelBonus > 0) {
            adjustWeightsForLevel(weights, levelBonus);
        }
        
        return selectRarityFromWeights(weights);
    }
    
    /**
     * Creates a treasure object from spawn candidate
     */
    private Treasure createTreasure(SpawnCandidate candidate, String spawnedBy) {
        String treasureId = firestore.collection("treasures").document().getId();
        String title = generateTreasureName(candidate.rarity);
        
        Treasure treasure = new Treasure(title, "", candidate.location, candidate.rarity);
        treasure.setId(treasureId);
        
        return treasure;
    }
    
    /**
     * Saves treasure to Firestore with proper structure
     */
    private void saveTreasureToFirestore(Treasure treasure) {
        Map<String, Object> treasureData = new HashMap<>();
        treasureData.put("id", treasure.getId());
        treasureData.put("title", treasure.getTitle());
        treasureData.put("imageUrl", treasure.getImageUrl());
        treasureData.put("latitude", treasure.getLocation().getLatitude());
        treasureData.put("longitude", treasure.getLocation().getLongitude());
        treasureData.put("rarity", treasure.getRarity().name());
        treasureData.put("captured", false);
        treasureData.put("spawnTime", System.currentTimeMillis());
        treasureData.put("expirationTime", System.currentTimeMillis() + TREASURE_LIFETIME_MS);
        
        firestore.collection("treasures")
                .document(treasure.getId())
                .set(treasureData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Treasure saved: " + treasure.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving treasure: " + treasure.getId(), e));
    }
    
    // Helper methods and utility functions
    private String getAreaKey(Location location) {
        // Create area key based on rounded coordinates (1km grid)
        int latGrid = (int) Math.round(location.getLatitude() * 100);
        int lonGrid = (int) Math.round(location.getLongitude() * 100);
        return latGrid + "," + lonGrid;
    }
    
    private int calculateTargetTreasureCount(int playerLevel, int currentCount) {
        // Base count + level bonus, capped at maximum
        int baseCount = MIN_TREASURES_PER_AREA;
        int levelBonus = Math.min(playerLevel / 10, MAX_TREASURES_PER_AREA - MIN_TREASURES_PER_AREA);
        return Math.min(baseCount + levelBonus, MAX_TREASURES_PER_AREA);
    }
    
    private double[] calculateBoundingBox(Location center, double radiusKm) {
        double latDelta = radiusKm / 111.32; // Approximate km to degrees latitude
        double lonDelta = radiusKm / (111.32 * Math.cos(Math.toRadians(center.getLatitude())));
        
        return new double[]{
                center.getLatitude() - latDelta,  // South
                center.getLatitude() + latDelta,  // North
                center.getLongitude() - lonDelta, // West
                center.getLongitude() + lonDelta  // East
        };
    }
    
    private boolean isInRadius(Location center, OsmGeoPoint point, double radiusM) {
        double distance = calculateDistance(point, center);
        return distance <= radiusM;
    }
    
    private double calculateDistance(OsmGeoPoint point1, Location location2) {
        Location loc1 = new Location("");
        loc1.setLatitude(point1.getLatitude());
        loc1.setLongitude(point1.getLongitude());
        return loc1.distanceTo(location2);
    }
    
    private double calculateDistance(OsmGeoPoint point1, OsmGeoPoint point2) {
        Location loc1 = new Location("");
        loc1.setLatitude(point1.getLatitude());
        loc1.setLongitude(point1.getLongitude());
        
        Location loc2 = new Location("");
        loc2.setLatitude(point2.getLatitude());
        loc2.setLongitude(point2.getLongitude());
        
        return loc1.distanceTo(loc2);
    }
    
    private List<OsmGeoPoint> extractLocations(List<Treasure> treasures) {
        List<OsmGeoPoint> locations = new ArrayList<>();
        for (Treasure treasure : treasures) {
            locations.add(treasure.getLocation());
        }
        return locations;
    }
    
    private Treasure documentToTreasure(QueryDocumentSnapshot doc) {
        try {
            String id = doc.getId();
            String title = doc.getString("title");
            String imageUrl = doc.getString("imageUrl");
            Double lat = doc.getDouble("latitude");
            Double lon = doc.getDouble("longitude");
            String rarityStr = doc.getString("rarity");
            
            if (lat == null || lon == null || rarityStr == null) {
                return null;
            }
            
            OsmGeoPoint location = new OsmGeoPoint(lat, lon);
            TreasureRarity rarity = TreasureRarity.valueOf(rarityStr);
            
            Treasure treasure = new Treasure(title, imageUrl, location, rarity);
            treasure.setId(id);
            return treasure;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to treasure", e);
            return null;
        }
    }
    
    private boolean isPeakHour() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
    }
    
    private void blendWeights(Map<TreasureRarity, Double> base, Map<TreasureRarity, Double> bonus, double factor) {
        for (TreasureRarity rarity : TreasureRarity.values()) {
            double baseWeight = base.getOrDefault(rarity, 0.0);
            double bonusWeight = bonus.getOrDefault(rarity, 0.0);
            base.put(rarity, baseWeight * (1 - factor) + bonusWeight * factor);
        }
    }
    
    private void adjustWeightsForLevel(Map<TreasureRarity, Double> weights, double levelBonus) {
        // Slightly increase rare treasure chances for higher level players
        weights.put(TreasureRarity.COMMON, weights.get(TreasureRarity.COMMON) * (1 - levelBonus));
        weights.put(TreasureRarity.RARE, weights.get(TreasureRarity.RARE) * (1 + levelBonus));
        weights.put(TreasureRarity.EPIC, weights.get(TreasureRarity.EPIC) * (1 + levelBonus * 1.5));
        weights.put(TreasureRarity.LEGENDARY, weights.get(TreasureRarity.LEGENDARY) * (1 + levelBonus * 2));
    }
    
    private TreasureRarity selectRarityFromWeights(Map<TreasureRarity, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        
        double currentWeight = 0;
        for (Map.Entry<TreasureRarity, Double> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        return TreasureRarity.COMMON; // Fallback
    }
    
    private String generateTreasureName(TreasureRarity rarity) {
        String[] prefixes = {"Ancient", "Mysterious", "Golden", "Silver", "Cursed", "Legendary", "Pirate's", "Captain's"};
        String[] suffixes = {"Chest", "Treasure", "Artifact", "Relic", "Bounty", "Hoard", "Cache", "Prize"};
        
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        
        return prefix + " " + suffix;
    }
    
    /**
     * Data class for spawn candidates
     */
    private static class SpawnCandidate {
        final OsmGeoPoint location;
        final TreasureRarity rarity;
        final double score;
        
        SpawnCandidate(OsmGeoPoint location, TreasureRarity rarity, double score) {
            this.location = location;
            this.rarity = rarity;
            this.score = score;
        }
    }
}
