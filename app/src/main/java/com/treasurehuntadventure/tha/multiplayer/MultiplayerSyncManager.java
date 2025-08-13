package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.treasurehuntadventure.tha.Treasure;
import com.treasurehuntadventure.tha.TreasureRarity;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages real-time multiplayer synchronization for treasure hunting game
 */
public class MultiplayerSyncManager {
    private static final String TAG = "MultiplayerSyncManager";
    
    // Collection names
    private static final String PLAYERS_COLLECTION = "players";
    private static final String TREASURES_COLLECTION = "treasures";
    private static final String GAME_SESSIONS_COLLECTION = "game_sessions";
    private static final String PLAYER_ACTIONS_COLLECTION = "player_actions";
    
    // Sync configuration
    private static final long PLAYER_UPDATE_INTERVAL_MS = 5000; // 5 seconds
    private static final long TREASURE_SYNC_INTERVAL_MS = 2000; // 2 seconds
    private static final double VISIBILITY_RADIUS_KM = 3.0; // 3km visibility radius
    private static final long CAPTURE_TIMEOUT_MS = 10000; // 10 seconds for capture timeout
    
    private final FirebaseFirestore firestore;
    private final Map<String, ListenerRegistration> activeListeners;
    private final Map<String, PlayerState> nearbyPlayers;
    private final Map<String, TreasureState> visibleTreasures;
    private final TreasureAnalytics analytics;
    
    private String currentPlayerId;
    private Location currentPlayerLocation;
    private MultiplayerEventListener eventListener;
    private long lastPlayerUpdate = 0;
    private long lastTreasureSync = 0;
    
    public interface MultiplayerEventListener {
        void onPlayerJoined(PlayerState player);
        void onPlayerLeft(String playerId);
        void onPlayerMoved(PlayerState player);
        void onTreasureSpawned(TreasureState treasure);
        void onTreasureCaptured(String treasureId, String playerId);
        void onTreasureExpired(String treasureId);
        void onSyncError(String error);
    }
    
    public MultiplayerSyncManager(String playerId) {
        this.firestore = FirebaseFirestore.getInstance();
        this.activeListeners = new ConcurrentHashMap<>();
        this.nearbyPlayers = new ConcurrentHashMap<>();
        this.visibleTreasures = new ConcurrentHashMap<>();
        this.analytics = new TreasureAnalytics();
        this.currentPlayerId = playerId;
    }
    
    /**
     * Starts real-time synchronization
     */
    public void startSync(Location playerLocation, MultiplayerEventListener listener) {
        this.currentPlayerLocation = playerLocation;
        this.eventListener = listener;
        
        Log.d(TAG, "Starting multiplayer sync for player: " + currentPlayerId);
        
        // Initialize player presence
        updatePlayerPresence(playerLocation);
        
        // Start listening for nearby players
        startPlayerListener();
        
        // Start listening for visible treasures
        startTreasureListener();
        
        // Start periodic updates
        startPeriodicUpdates();
    }
    
    /**
     * Stops all synchronization
     */
    public void stopSync() {
        Log.d(TAG, "Stopping multiplayer sync");
        
        // Remove all listeners
        for (ListenerRegistration listener : activeListeners.values()) {
            listener.remove();
        }
        activeListeners.clear();
        
        // Clear player presence
        removePlayerPresence();
        
        // Clear local state
        nearbyPlayers.clear();
        visibleTreasures.clear();
    }
    
    /**
     * Updates player location and syncs with other players
     */
    public void updatePlayerLocation(Location location) {
        this.currentPlayerLocation = location;
        
        long now = System.currentTimeMillis();
        if (now - lastPlayerUpdate >= PLAYER_UPDATE_INTERVAL_MS) {
            updatePlayerPresence(location);
            lastPlayerUpdate = now;
        }
    }
    
    /**
     * Attempts to capture a treasure with conflict resolution
     */
    public CompletableFuture<CaptureResult> captureTreasure(String treasureId, Location playerLocation) {
        CompletableFuture<CaptureResult> future = new CompletableFuture<>();
        
        DocumentReference treasureRef = firestore.collection(TREASURES_COLLECTION).document(treasureId);
        
        firestore.runTransaction((Transaction transaction) -> {
            DocumentSnapshot treasureSnapshot = transaction.get(treasureRef);
            
            if (!treasureSnapshot.exists()) {
                throw new RuntimeException("Treasure not found");
            }
            
            Boolean captured = treasureSnapshot.getBoolean("captured");
            if (captured != null && captured) {
                throw new RuntimeException("Treasure already captured");
            }
            
            // Check if another player is currently capturing
            String capturingPlayer = treasureSnapshot.getString("capturingPlayer");
            Long captureStartTime = treasureSnapshot.getLong("captureStartTime");
            
            if (capturingPlayer != null && !capturingPlayer.equals(currentPlayerId)) {
                long now = System.currentTimeMillis();
                if (captureStartTime != null && now - captureStartTime < CAPTURE_TIMEOUT_MS) {
                    throw new RuntimeException("Another player is capturing this treasure");
                }
            }
            
            // Validate proximity
            Double treasureLat = treasureSnapshot.getDouble("latitude");
            Double treasureLon = treasureSnapshot.getDouble("longitude");
            if (treasureLat == null || treasureLon == null) {
                throw new RuntimeException("Invalid treasure location");
            }
            
            Location treasureLocation = new Location("");
            treasureLocation.setLatitude(treasureLat);
            treasureLocation.setLongitude(treasureLon);
            
            float distance = playerLocation.distanceTo(treasureLocation);
            if (distance > 50.0f) { // 50 meter capture radius
                throw new RuntimeException("Too far from treasure");
            }
            
            // Mark as being captured
            Map<String, Object> updates = new HashMap<>();
            updates.put("capturingPlayer", currentPlayerId);
            updates.put("captureStartTime", System.currentTimeMillis());
            
            transaction.update(treasureRef, updates);
            
            return treasureSnapshot;
        }).addOnSuccessListener(treasureSnapshot -> {
            // Complete the capture after a short delay (simulating capture animation)
            completeTreasureCapture(treasureId, treasureSnapshot, future);
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Treasure capture failed: " + e.getMessage());
            future.complete(new CaptureResult(false, e.getMessage(), null));
        });
        
        return future;
    }
    
    /**
     * Gets current nearby players
     */
    public List<PlayerState> getNearbyPlayers() {
        return new ArrayList<>(nearbyPlayers.values());
    }
    
    /**
     * Gets current visible treasures
     */
    public List<TreasureState> getVisibleTreasures() {
        return new ArrayList<>(visibleTreasures.values());
    }
    
    private void startPlayerListener() {
        if (currentPlayerLocation == null) return;
        
        // Calculate bounding box for nearby players
        double[] bounds = calculateBoundingBox(currentPlayerLocation, VISIBILITY_RADIUS_KM);
        
        Query playersQuery = firestore.collection(PLAYERS_COLLECTION)
                .whereGreaterThanOrEqualTo("latitude", bounds[0])
                .whereLessThanOrEqualTo("latitude", bounds[1])
                .whereGreaterThan("lastUpdated", System.currentTimeMillis() - 60000); // Active in last minute
        
        ListenerRegistration listener = playersQuery.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Player listener error", e);
                if (eventListener != null) {
                    eventListener.onSyncError("Player sync error: " + e.getMessage());
                }
                return;
            }
            
            if (snapshot != null) {
                handlePlayerUpdates(snapshot.getDocuments());
            }
        });
        
        activeListeners.put("players", listener);
    }
    
    private void startTreasureListener() {
        if (currentPlayerLocation == null) return;
        
        // Calculate bounding box for visible treasures
        double[] bounds = calculateBoundingBox(currentPlayerLocation, VISIBILITY_RADIUS_KM);
        
        Query treasuresQuery = firestore.collection(TREASURES_COLLECTION)
                .whereEqualTo("captured", false)
                .whereGreaterThanOrEqualTo("latitude", bounds[0])
                .whereLessThanOrEqualTo("latitude", bounds[1]);
        
        ListenerRegistration listener = treasuresQuery.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Treasure listener error", e);
                if (eventListener != null) {
                    eventListener.onSyncError("Treasure sync error: " + e.getMessage());
                }
                return;
            }
            
            if (snapshot != null) {
                handleTreasureUpdates(snapshot.getDocuments());
            }
        });
        
        activeListeners.put("treasures", listener);
    }
    
    private void startPeriodicUpdates() {
        // This would typically be handled by a background service or timer
        // For now, we'll rely on the calling activity to trigger updates
    }
    
    private void updatePlayerPresence(Location location) {
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("playerId", currentPlayerId);
        playerData.put("latitude", location.getLatitude());
        playerData.put("longitude", location.getLongitude());
        playerData.put("lastUpdated", System.currentTimeMillis());
        playerData.put("status", "active");
        
        firestore.collection(PLAYERS_COLLECTION)
                .document(currentPlayerId)
                .set(playerData)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update player presence", e));
        
        // Record analytics
        analytics.recordPlayerActivity(currentPlayerId, location, "LOCATION_UPDATE");
    }
    
    private void removePlayerPresence() {
        firestore.collection(PLAYERS_COLLECTION)
                .document(currentPlayerId)
                .delete()
                .addOnFailureListener(e -> Log.w(TAG, "Failed to remove player presence", e));
    }
    
    private void handlePlayerUpdates(List<DocumentSnapshot> documents) {
        Map<String, PlayerState> currentPlayers = new HashMap<>();
        
        for (DocumentSnapshot doc : documents) {
            String playerId = doc.getId();
            if (playerId.equals(currentPlayerId)) continue; // Skip self
            
            try {
                PlayerState player = documentToPlayerState(doc);
                if (player != null && isPlayerNearby(player)) {
                    currentPlayers.put(playerId, player);
                    
                    // Check if this is a new player or moved player
                    PlayerState existingPlayer = nearbyPlayers.get(playerId);
                    if (existingPlayer == null) {
                        if (eventListener != null) {
                            eventListener.onPlayerJoined(player);
                        }
                    } else if (!existingPlayer.location.equals(player.location)) {
                        if (eventListener != null) {
                            eventListener.onPlayerMoved(player);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing player document: " + doc.getId(), e);
            }
        }
        
        // Check for players who left
        for (String playerId : nearbyPlayers.keySet()) {
            if (!currentPlayers.containsKey(playerId)) {
                if (eventListener != null) {
                    eventListener.onPlayerLeft(playerId);
                }
            }
        }
        
        nearbyPlayers.clear();
        nearbyPlayers.putAll(currentPlayers);
    }
    
    private void handleTreasureUpdates(List<DocumentSnapshot> documents) {
        Map<String, TreasureState> currentTreasures = new HashMap<>();
        
        for (DocumentSnapshot doc : documents) {
            try {
                TreasureState treasure = documentToTreasureState(doc);
                if (treasure != null && isTreasureVisible(treasure)) {
                    currentTreasures.put(doc.getId(), treasure);
                    
                    // Check if this is a new treasure
                    if (!visibleTreasures.containsKey(doc.getId())) {
                        if (eventListener != null) {
                            eventListener.onTreasureSpawned(treasure);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing treasure document: " + doc.getId(), e);
            }
        }
        
        // Check for treasures that were captured or expired
        for (String treasureId : visibleTreasures.keySet()) {
            if (!currentTreasures.containsKey(treasureId)) {
                if (eventListener != null) {
                    eventListener.onTreasureCaptured(treasureId, "unknown");
                }
            }
        }
        
        visibleTreasures.clear();
        visibleTreasures.putAll(currentTreasures);
    }
    
    private void completeTreasureCapture(String treasureId, DocumentSnapshot treasureSnapshot, 
                                       CompletableFuture<CaptureResult> future) {
        // Simulate capture delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            DocumentReference treasureRef = firestore.collection(TREASURES_COLLECTION).document(treasureId);
            
            firestore.runTransaction((Transaction transaction) -> {
                DocumentSnapshot currentSnapshot = transaction.get(treasureRef);
                
                // Double-check that we're still the capturing player
                String capturingPlayer = currentSnapshot.getString("capturingPlayer");
                if (!currentPlayerId.equals(capturingPlayer)) {
                    throw new RuntimeException("Capture was interrupted by another player");
                }
                
                // Mark as captured
                Map<String, Object> updates = new HashMap<>();
                updates.put("captured", true);
                updates.put("capturedBy", currentPlayerId);
                updates.put("captureTime", System.currentTimeMillis());
                updates.put("capturingPlayer", null);
                updates.put("captureStartTime", null);
                
                transaction.update(treasureRef, updates);
                
                return currentSnapshot;
            }).addOnSuccessListener(snapshot -> {
                // Create capture result
                TreasureState treasureState = documentToTreasureState(snapshot);
                CaptureResult result = new CaptureResult(true, "Treasure captured successfully!", treasureState);
                future.complete(result);
                
                // Record analytics
                if (treasureState != null) {
                    Long spawnTime = snapshot.getLong("spawnTime");
                    analytics.recordTreasureCapture(treasureId, currentPlayerId, currentPlayerLocation, 
                            treasureState.rarity, spawnTime != null ? spawnTime : System.currentTimeMillis());
                }
                
                // Notify listeners
                if (eventListener != null) {
                    eventListener.onTreasureCaptured(treasureId, currentPlayerId);
                }
                
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Failed to complete treasure capture", e);
                future.complete(new CaptureResult(false, "Capture failed: " + e.getMessage(), null));
            });
        }, 1000); // 1 second capture animation delay
    }
    
    private PlayerState documentToPlayerState(DocumentSnapshot doc) {
        try {
            String playerId = doc.getId();
            Double lat = doc.getDouble("latitude");
            Double lon = doc.getDouble("longitude");
            Long lastUpdated = doc.getLong("lastUpdated");
            String status = doc.getString("status");
            
            if (lat == null || lon == null) return null;
            
            Location location = new Location("");
            location.setLatitude(lat);
            location.setLongitude(lon);
            
            return new PlayerState(playerId, location, lastUpdated, status);
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to player state", e);
            return null;
        }
    }
    
    private TreasureState documentToTreasureState(DocumentSnapshot doc) {
        try {
            String treasureId = doc.getId();
            String title = doc.getString("title");
            Double lat = doc.getDouble("latitude");
            Double lon = doc.getDouble("longitude");
            String rarityStr = doc.getString("rarity");
            Long spawnTime = doc.getLong("spawnTime");
            String capturingPlayer = doc.getString("capturingPlayer");
            
            if (lat == null || lon == null || rarityStr == null) return null;
            
            GeoPoint location = new GeoPoint(lat, lon);
            TreasureRarity rarity = TreasureRarity.valueOf(rarityStr);
            
            return new TreasureState(treasureId, title, location, rarity, spawnTime, capturingPlayer);
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to treasure state", e);
            return null;
        }
    }
    
    private boolean isPlayerNearby(PlayerState player) {
        if (currentPlayerLocation == null) return false;
        float distance = currentPlayerLocation.distanceTo(player.location);
        return distance <= VISIBILITY_RADIUS_KM * 1000;
    }
    
    private boolean isTreasureVisible(TreasureState treasure) {
        if (currentPlayerLocation == null) return false;
        
        Location treasureLocation = new Location("");
        treasureLocation.setLatitude(treasure.location.getLatitude());
        treasureLocation.setLongitude(treasure.location.getLongitude());
        
        float distance = currentPlayerLocation.distanceTo(treasureLocation);
        return distance <= VISIBILITY_RADIUS_KM * 1000;
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
    
    /**
     * Player state for multiplayer tracking
     */
    public static class PlayerState {
        public final String playerId;
        public final Location location;
        public final Long lastUpdated;
        public final String status;
        
        public PlayerState(String playerId, Location location, Long lastUpdated, String status) {
            this.playerId = playerId;
            this.location = location;
            this.lastUpdated = lastUpdated;
            this.status = status;
        }
    }
    
    /**
     * Treasure state for multiplayer tracking
     */
    public static class TreasureState {
        public final String treasureId;
        public final String title;
        public final GeoPoint location;
        public final TreasureRarity rarity;
        public final Long spawnTime;
        public final String capturingPlayer;
        
        public TreasureState(String treasureId, String title, GeoPoint location, TreasureRarity rarity, 
                           Long spawnTime, String capturingPlayer) {
            this.treasureId = treasureId;
            this.title = title;
            this.location = location;
            this.rarity = rarity;
            this.spawnTime = spawnTime;
            this.capturingPlayer = capturingPlayer;
        }
    }
    
    /**
     * Result of a treasure capture attempt
     */
    public static class CaptureResult {
        public final boolean success;
        public final String message;
        public final TreasureState treasure;
        
        public CaptureResult(boolean success, String message, TreasureState treasure) {
            this.success = success;
            this.message = message;
            this.treasure = treasure;
        }
    }
}
