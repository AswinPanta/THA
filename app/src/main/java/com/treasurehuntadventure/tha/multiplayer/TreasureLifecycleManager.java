package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.treasurehuntadventure.tha.TreasureRarity;

import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive treasure lifecycle management system
 */
public class TreasureLifecycleManager {
    private static final String TAG = "TreasureLifecycleManager";
    
    // Lifecycle constants
    private static final long DEFAULT_TREASURE_LIFETIME_MS = 30 * 60 * 1000; // 30 minutes
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
    private static final long SPAWN_CONDITION_CHECK_INTERVAL_MS = 2 * 60 * 1000; // 2 minutes
    private static final int MAX_CONCURRENT_TREASURES = 50;
    private static final int MIN_PLAYER_LEVEL_FOR_RARE = 5;
    private static final int MIN_PLAYER_LEVEL_FOR_EPIC = 15;
    private static final int MIN_PLAYER_LEVEL_FOR_LEGENDARY = 25;
    
    // Lifecycle stages
    public enum TreasureStage {
        SPAWNING,
        ACTIVE,
        EXPIRING,
        CAPTURED,
        EXPIRED,
        CLEANED_UP
    }
    
    // Spawn conditions
    public enum SpawnCondition {
        PLAYER_PROXIMITY,
        TIME_BASED,
        ACTIVITY_BASED,
        RARITY_BALANCE,
        AREA_DENSITY,
        PLAYER_LEVEL
    }
    
    private final FirebaseFirestore firestore;
    private final ScheduledExecutorService scheduler;
    private final Map<String, TreasureLifecycle> activeTreasures;
    private final Map<String, ScheduledFuture<?>> expirationTasks;
    private final EnhancedTreasureSpawner treasureSpawner;
    private final TreasureAnalytics analytics;
    private final RealTimeEventSystem eventSystem;
    
    private ScheduledFuture<?> cleanupTask;
    private ScheduledFuture<?> spawnConditionTask;
    
    public interface LifecycleEventListener {
        void onTreasureSpawned(TreasureLifecycle treasure);
        void onTreasureActivated(TreasureLifecycle treasure);
        void onTreasureExpiring(TreasureLifecycle treasure, long timeRemaining);
        void onTreasureCaptured(TreasureLifecycle treasure, String capturedBy);
        void onTreasureExpired(TreasureLifecycle treasure, String reason);
        void onTreasureCleanedUp(String treasureId);
    }
    
    public TreasureLifecycleManager(EnhancedTreasureSpawner spawner, TreasureAnalytics analytics, 
                                  RealTimeEventSystem eventSystem) {
        this.firestore = FirebaseFirestore.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.activeTreasures = new ConcurrentHashMap<>();
        this.expirationTasks = new ConcurrentHashMap<>();
        this.treasureSpawner = spawner;
        this.analytics = analytics;
        this.eventSystem = eventSystem;
    }
    
    /**
     * Starts the treasure lifecycle management system
     */
    public void start(LifecycleEventListener listener) {
        Log.d(TAG, "Starting treasure lifecycle manager");
        
        // Load existing active treasures
        loadActiveTreasures(listener);
        
        // Start cleanup task
        startCleanupTask(listener);
        
        // Start spawn condition monitoring
        startSpawnConditionMonitoring(listener);
    }
    
    /**
     * Stops the treasure lifecycle management system
     */
    public void stop() {
        Log.d(TAG, "Stopping treasure lifecycle manager");
        
        // Cancel all scheduled tasks
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
        }
        
        if (spawnConditionTask != null) {
            spawnConditionTask.cancel(true);
        }
        
        // Cancel all expiration tasks
        for (ScheduledFuture<?> task : expirationTasks.values()) {
            task.cancel(true);
        }
        expirationTasks.clear();
        
        // Shutdown scheduler
        scheduler.shutdown();
        
        // Clear active treasures
        activeTreasures.clear();
    }
    
    /**
     * Creates a new treasure with full lifecycle management
     */
    public void createTreasure(GeoPoint location, TreasureRarity rarity, String title, 
                             String spawnedBy, Map<String, Object> metadata, 
                             LifecycleEventListener listener) {
        String treasureId = firestore.collection("treasures").document().getId();
        
        TreasureLifecycle treasure = new TreasureLifecycle(
                treasureId, title, location, rarity, spawnedBy, 
                System.currentTimeMillis(), calculateLifetime(rarity), metadata);
        
        // Save to Firestore
        saveTreasureToFirestore(treasure);
        
        // Add to active treasures
        activeTreasures.put(treasureId, treasure);
        
        // Schedule expiration
        scheduleExpiration(treasure, listener);
        
        // Notify listener
        if (listener != null) {
            listener.onTreasureSpawned(treasure);
        }
        
        // Publish event
        eventSystem.publishTreasureSpawn(treasureId, location.getLatitude(), 
                location.getLongitude(), rarity.name(), treasure.spawnTime);
        
        // Record analytics
        Location spawnLocation = new Location("");
        spawnLocation.setLatitude(location.getLatitude());
        spawnLocation.setLongitude(location.getLongitude());
        analytics.recordTreasureSpawn(createTreasureFromLifecycle(treasure), spawnLocation, 1);
    }
    
    /**
     * Captures a treasure and handles lifecycle transition
     */
    public boolean captureTreasure(String treasureId, String capturedBy, Location captureLocation,
                                 LifecycleEventListener listener) {
        TreasureLifecycle treasure = activeTreasures.get(treasureId);
        if (treasure == null || treasure.stage != TreasureStage.ACTIVE) {
            return false;
        }
        
        // Validate capture conditions
        if (!validateCaptureConditions(treasure, capturedBy, captureLocation)) {
            return false;
        }
        
        // Update treasure state
        treasure.stage = TreasureStage.CAPTURED;
        treasure.capturedBy = capturedBy;
        treasure.captureTime = System.currentTimeMillis();
        
        // Update in Firestore
        updateTreasureInFirestore(treasure);
        
        // Cancel expiration task
        ScheduledFuture<?> expirationTask = expirationTasks.remove(treasureId);
        if (expirationTask != null) {
            expirationTask.cancel(true);
        }
        
        // Remove from active treasures
        activeTreasures.remove(treasureId);
        
        // Notify listener
        if (listener != null) {
            listener.onTreasureCaptured(treasure, capturedBy);
        }
        
        // Publish event
        int points = calculateCapturePoints(treasure.rarity);
        eventSystem.publishTreasureCapture(treasureId, capturedBy, treasure.captureTime, 
                treasure.rarity.name(), points);
        
        // Record analytics
        analytics.recordTreasureCapture(treasureId, capturedBy, captureLocation, 
                treasure.rarity, treasure.spawnTime);
        
        return true;
    }
    
    /**
     * Checks spawn conditions and triggers spawning if needed
     */
    public void checkSpawnConditions(Location playerLocation, String playerId, int playerLevel,
                                   LifecycleEventListener listener) {
        // Check if we need more treasures in the area
        if (shouldSpawnTreasures(playerLocation, playerLevel)) {
            treasureSpawner.spawnTreasuresForArea(playerLocation, playerId, playerLevel)
                    .thenAccept(newTreasures -> {
                        for (com.treasurehuntadventure.tha.Treasure treasure : newTreasures) {
                            createTreasure(treasure.getLocation(), treasure.getRarity(), 
                                    treasure.getTitle(), playerId, new HashMap<>(), listener);
                        }
                    });
        }
    }
    
    /**
     * Gets all active treasures
     */
    public Map<String, TreasureLifecycle> getActiveTreasures() {
        return new HashMap<>(activeTreasures);
    }
    
    /**
     * Gets treasure lifecycle information
     */
    public TreasureLifecycle getTreasureLifecycle(String treasureId) {
        return activeTreasures.get(treasureId);
    }
    
    /**
     * Forces treasure expiration
     */
    public void expireTreasure(String treasureId, String reason, LifecycleEventListener listener) {
        TreasureLifecycle treasure = activeTreasures.get(treasureId);
        if (treasure != null) {
            expireTreasureInternal(treasure, reason, listener);
        }
    }
    
    private void loadActiveTreasures(LifecycleEventListener listener) {
        long currentTime = System.currentTimeMillis();
        
        firestore.collection("treasures")
                .whereEqualTo("captured", false)
                .whereGreaterThan("expirationTime", currentTime)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            TreasureLifecycle treasure = createLifecycleFromDocument(doc);
                            if (treasure != null) {
                                activeTreasures.put(treasure.treasureId, treasure);
                                scheduleExpiration(treasure, listener);
                                
                                if (listener != null) {
                                    listener.onTreasureActivated(treasure);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading treasure: " + doc.getId(), e);
                        }
                    }
                    Log.d(TAG, "Loaded " + activeTreasures.size() + " active treasures");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load active treasures", e));
    }
    
    private void startCleanupTask(LifecycleEventListener listener) {
        cleanupTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                performCleanup(listener);
            } catch (Exception e) {
                Log.e(TAG, "Error during cleanup", e);
            }
        }, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void startSpawnConditionMonitoring(LifecycleEventListener listener) {
        spawnConditionTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                // This would typically be triggered by player movement
                // For now, we'll just log that monitoring is active
                Log.d(TAG, "Monitoring spawn conditions for " + activeTreasures.size() + " treasures");
            } catch (Exception e) {
                Log.e(TAG, "Error during spawn condition monitoring", e);
            }
        }, SPAWN_CONDITION_CHECK_INTERVAL_MS, SPAWN_CONDITION_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void scheduleExpiration(TreasureLifecycle treasure, LifecycleEventListener listener) {
        long timeUntilExpiration = treasure.expirationTime - System.currentTimeMillis();
        
        if (timeUntilExpiration > 0) {
            ScheduledFuture<?> expirationTask = scheduler.schedule(() -> {
                expireTreasureInternal(treasure, "TIMEOUT", listener);
            }, timeUntilExpiration, TimeUnit.MILLISECONDS);
            
            expirationTasks.put(treasure.treasureId, expirationTask);
            
            // Schedule expiring warning (5 minutes before expiration)
            long warningTime = timeUntilExpiration - (5 * 60 * 1000);
            if (warningTime > 0) {
                scheduler.schedule(() -> {
                    if (treasure.stage == TreasureStage.ACTIVE && listener != null) {
                        treasure.stage = TreasureStage.EXPIRING;
                        listener.onTreasureExpiring(treasure, 5 * 60 * 1000);
                    }
                }, warningTime, TimeUnit.MILLISECONDS);
            }
        } else {
            // Already expired
            expireTreasureInternal(treasure, "ALREADY_EXPIRED", listener);
        }
    }
    
    private void expireTreasureInternal(TreasureLifecycle treasure, String reason, 
                                      LifecycleEventListener listener) {
        treasure.stage = TreasureStage.EXPIRED;
        treasure.expirationReason = reason;
        
        // Update in Firestore
        updateTreasureInFirestore(treasure);
        
        // Remove from active treasures
        activeTreasures.remove(treasure.treasureId);
        
        // Cancel expiration task
        ScheduledFuture<?> task = expirationTasks.remove(treasure.treasureId);
        if (task != null) {
            task.cancel(true);
        }
        
        // Notify listener
        if (listener != null) {
            listener.onTreasureExpired(treasure, reason);
        }
        
        // Publish event
        eventSystem.publishEvent(new RealTimeEventSystem.TreasureExpirationEvent(
                treasure.treasureId, System.currentTimeMillis(), reason, "SYSTEM"));
    }
    
    private void performCleanup(LifecycleEventListener listener) {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = currentTime - (24 * 60 * 60 * 1000); // 24 hours ago
        
        // Clean up old expired treasures from Firestore
        firestore.collection("treasures")
                .whereLessThan("expirationTime", cleanupThreshold)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                        if (listener != null) {
                            listener.onTreasureCleanedUp(doc.getId());
                        }
                    }
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "Cleaned up " + querySnapshot.size() + " old treasures");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to cleanup old treasures", e));
    }
    
    private boolean shouldSpawnTreasures(Location playerLocation, int playerLevel) {
        // Check current treasure density in the area
        long nearbyTreasures = activeTreasures.values().stream()
                .filter(treasure -> {
                    Location treasureLocation = new Location("");
                    treasureLocation.setLatitude(treasure.location.getLatitude());
                    treasureLocation.setLongitude(treasure.location.getLongitude());
                    return playerLocation.distanceTo(treasureLocation) <= 2000; // 2km radius
                })
                .count();
        
        // Spawn more treasures if density is low
        int targetTreasures = Math.min(3 + playerLevel / 10, 8);
        return nearbyTreasures < targetTreasures;
    }
    
    private boolean validateCaptureConditions(TreasureLifecycle treasure, String capturedBy, 
                                            Location captureLocation) {
        // Check distance
        Location treasureLocation = new Location("");
        treasureLocation.setLatitude(treasure.location.getLatitude());
        treasureLocation.setLongitude(treasure.location.getLongitude());
        
        float distance = captureLocation.distanceTo(treasureLocation);
        return distance <= 50.0f; // 50 meter capture radius
    }
    
    private long calculateLifetime(TreasureRarity rarity) {
        switch (rarity) {
            case COMMON:
                return 20 * 60 * 1000; // 20 minutes
            case UNCOMMON:
                return 30 * 60 * 1000; // 30 minutes
            case RARE:
                return 45 * 60 * 1000; // 45 minutes
            case EPIC:
                return 60 * 60 * 1000; // 1 hour
            case LEGENDARY:
                return 90 * 60 * 1000; // 1.5 hours
            default:
                return DEFAULT_TREASURE_LIFETIME_MS;
        }
    }
    
    private int calculateCapturePoints(TreasureRarity rarity) {
        switch (rarity) {
            case COMMON: return 10;
            case UNCOMMON: return 25;
            case RARE: return 50;
            case EPIC: return 100;
            case LEGENDARY: return 250;
            default: return 10;
        }
    }
    
    private void saveTreasureToFirestore(TreasureLifecycle treasure) {
        Map<String, Object> data = treasure.toFirestoreMap();
        firestore.collection("treasures")
                .document(treasure.treasureId)
                .set(data)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save treasure", e));
    }
    
    private void updateTreasureInFirestore(TreasureLifecycle treasure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("stage", treasure.stage.name());
        updates.put("captured", treasure.stage == TreasureStage.CAPTURED);
        updates.put("capturedBy", treasure.capturedBy);
        updates.put("captureTime", treasure.captureTime);
        updates.put("expirationReason", treasure.expirationReason);
        
        firestore.collection("treasures")
                .document(treasure.treasureId)
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update treasure", e));
    }
    
    private TreasureLifecycle createLifecycleFromDocument(DocumentSnapshot doc) {
        try {
            String id = doc.getId();
            String title = doc.getString("title");
            Double lat = doc.getDouble("latitude");
            Double lon = doc.getDouble("longitude");
            String rarityStr = doc.getString("rarity");
            String spawnedBy = doc.getString("spawnedBy");
            Long spawnTime = doc.getLong("spawnTime");
            Long expirationTime = doc.getLong("expirationTime");
            
            if (lat == null || lon == null || rarityStr == null || spawnTime == null || expirationTime == null) {
                return null;
            }
            
            GeoPoint location = new GeoPoint(lat, lon);
            TreasureRarity rarity = TreasureRarity.valueOf(rarityStr);
            
            TreasureLifecycle treasure = new TreasureLifecycle(id, title, location, rarity, 
                    spawnedBy, spawnTime, expirationTime - spawnTime, new HashMap<>());
            
            // Set stage based on current state
            Boolean captured = doc.getBoolean("captured");
            if (captured != null && captured) {
                treasure.stage = TreasureStage.CAPTURED;
                treasure.capturedBy = doc.getString("capturedBy");
                treasure.captureTime = doc.getLong("captureTime");
            } else if (System.currentTimeMillis() > expirationTime) {
                treasure.stage = TreasureStage.EXPIRED;
            } else {
                treasure.stage = TreasureStage.ACTIVE;
            }
            
            return treasure;
        } catch (Exception e) {
            Log.e(TAG, "Error creating lifecycle from document", e);
            return null;
        }
    }
    
    private com.treasurehuntadventure.tha.Treasure createTreasureFromLifecycle(TreasureLifecycle lifecycle) {
        com.treasurehuntadventure.tha.Treasure treasure = new com.treasurehuntadventure.tha.Treasure(
                lifecycle.title, "", lifecycle.location, lifecycle.rarity);
        treasure.setId(lifecycle.treasureId);
        return treasure;
    }
    
    /**
     * Treasure lifecycle data class
     */
    public static class TreasureLifecycle {
        public final String treasureId;
        public final String title;
        public final GeoPoint location;
        public final TreasureRarity rarity;
        public final String spawnedBy;
        public final long spawnTime;
        public final long lifetimeMs;
        public final long expirationTime;
        public final Map<String, Object> metadata;
        
        public TreasureStage stage = TreasureStage.SPAWNING;
        public String capturedBy;
        public Long captureTime;
        public String expirationReason;
        
        public TreasureLifecycle(String treasureId, String title, GeoPoint location, 
                               TreasureRarity rarity, String spawnedBy, long spawnTime, 
                               long lifetimeMs, Map<String, Object> metadata) {
            this.treasureId = treasureId;
            this.title = title;
            this.location = location;
            this.rarity = rarity;
            this.spawnedBy = spawnedBy;
            this.spawnTime = spawnTime;
            this.lifetimeMs = lifetimeMs;
            this.expirationTime = spawnTime + lifetimeMs;
            this.metadata = metadata;
            this.stage = TreasureStage.ACTIVE;
        }
        
        public long getTimeRemaining() {
            return Math.max(0, expirationTime - System.currentTimeMillis());
        }
        
        public double getLifetimeProgress() {
            long elapsed = System.currentTimeMillis() - spawnTime;
            return Math.min(1.0, (double) elapsed / lifetimeMs);
        }
        
        public Map<String, Object> toFirestoreMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", treasureId);
            map.put("title", title);
            map.put("latitude", location.getLatitude());
            map.put("longitude", location.getLongitude());
            map.put("rarity", rarity.name());
            map.put("spawnedBy", spawnedBy);
            map.put("spawnTime", spawnTime);
            map.put("expirationTime", expirationTime);
            map.put("stage", stage.name());
            map.put("captured", stage == TreasureStage.CAPTURED);
            map.put("capturedBy", capturedBy);
            map.put("captureTime", captureTime);
            map.put("expirationReason", expirationReason);
            map.put("metadata", metadata);
            return map;
        }
    }
}
