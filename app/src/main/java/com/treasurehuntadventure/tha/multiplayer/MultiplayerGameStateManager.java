package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.treasurehuntadventure.tha.TreasureRarity;
import com.treasurehuntadventure.tha.auth.AuthStateManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Centralized multiplayer game state manager that coordinates all multiplayer components
 */
public class MultiplayerGameStateManager implements 
        RealTimeEventSystem.GlobalEventListener,
        MultiplayerSyncManager.MultiplayerEventListener,
        ProximityDetectionSystem.ProximityEventListener,
        TreasureLifecycleManager.LifecycleEventListener {
    
    private static final String TAG = "MultiplayerGameState";
    
    // Component instances
    private final EnhancedTreasureSpawner treasureSpawner;
    private final MultiplayerSyncManager syncManager;
    private final ProximityDetectionSystem proximitySystem;
    private final EnhancedMapRenderer mapRenderer;
    private final RealTimeEventSystem eventSystem;
    private final TreasureLifecycleManager lifecycleManager;
    private final TreasureDistributionAlgorithm distributionAlgorithm;
    private final TreasureAnalytics analytics;
    
    // Game state
    private final String playerId;
    private Location currentPlayerLocation;
    private int playerLevel = 1;
    private boolean isActive = false;
    private GameStateListener gameStateListener;
    
    // Performance tracking
    private long lastLocationUpdate = 0;
    private long lastTreasureSpawn = 0;
    private final Map<String, Long> performanceMetrics = new HashMap<>();
    
    public interface GameStateListener {
        void onGameStateInitialized();
        void onPlayerLocationUpdated(Location location);
        void onTreasureSpawned(String treasureId, GeoPoint location, TreasureRarity rarity);
        void onTreasureCaptured(String treasureId, String capturedBy, int points);
        void onPlayerJoined(String playerId, Location location);
        void onPlayerLeft(String playerId);
        void onProximityAlert(String message, ProximityDetectionSystem.AlertType type);
        void onGameError(String error);
    }
    
    public MultiplayerGameStateManager(MapView mapView, String playerId) {
        this.playerId = playerId;
        
        // Initialize analytics first
        this.analytics = new TreasureAnalytics();
        
        // Initialize core components
        this.treasureSpawner = new EnhancedTreasureSpawner();
        this.syncManager = new MultiplayerSyncManager(playerId);
        this.proximitySystem = new ProximityDetectionSystem(this);
        this.mapRenderer = new EnhancedMapRenderer(mapView.getContext(), mapView);
        this.eventSystem = new RealTimeEventSystem(playerId);
        this.lifecycleManager = new TreasureLifecycleManager(treasureSpawner, analytics, eventSystem);
        this.distributionAlgorithm = new TreasureDistributionAlgorithm(analytics);
        
        Log.d(TAG, "Multiplayer game state manager initialized for player: " + playerId);
    }
    
    /**
     * Starts the multiplayer game system
     */
    public void startMultiplayerGame(Location initialLocation, int playerLevel, 
                                   GameStateListener listener) {
        this.currentPlayerLocation = initialLocation;
        this.playerLevel = playerLevel;
        this.gameStateListener = listener;
        this.isActive = true;
        
        Log.d(TAG, "Starting multiplayer game at location: " + initialLocation.getLatitude() + 
                ", " + initialLocation.getLongitude());
        
        try {
            // Start event system
            eventSystem.start(this);
            
            // Start lifecycle manager
            lifecycleManager.start(this);
            
            // Start sync manager
            syncManager.startSync(initialLocation, this);
            
            // Initial treasure spawn
            spawnInitialTreasures(initialLocation);
            
            // Initialize proximity system
            proximitySystem.updatePlayerLocation(initialLocation);
            
            // Show proximity rings
            mapRenderer.showProximityRings(new GeoPoint(initialLocation.getLatitude(), 
                    initialLocation.getLongitude()), true);
            
            if (listener != null) {
                listener.onGameStateInitialized();
            }
            
            Log.d(TAG, "Multiplayer game started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting multiplayer game", e);
            if (listener != null) {
                listener.onGameError("Failed to start multiplayer game: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stops the multiplayer game system
     */
    public void stopMultiplayerGame() {
        Log.d(TAG, "Stopping multiplayer game");
        
        this.isActive = false;
        
        try {
            // Stop all components
            eventSystem.stop();
            lifecycleManager.stop();
            syncManager.stopSync();
            
            // Hide proximity rings
            mapRenderer.showProximityRings(null, false);
            
            Log.d(TAG, "Multiplayer game stopped successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping multiplayer game", e);
        }
    }
    
    /**
     * Updates player location and triggers all location-based systems
     */
    public void updatePlayerLocation(Location location) {
        if (!isActive) return;
        
        long now = System.currentTimeMillis();
        if (now - lastLocationUpdate < 1000) return; // Throttle to 1 second
        
        this.currentPlayerLocation = location;
        this.lastLocationUpdate = now;
        
        try {
            // Update all location-dependent systems
            syncManager.updatePlayerLocation(location);
            proximitySystem.updatePlayerLocation(location);
            
            // Update map renderer
            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapRenderer.showProximityRings(geoPoint, true);
            
            // Check spawn conditions
            lifecycleManager.checkSpawnConditions(location, playerId, playerLevel, this);
            
            // Publish movement event
            float bearing = location.hasBearing() ? location.getBearing() : 0f;
            float speed = location.hasSpeed() ? location.getSpeed() : 0f;
            eventSystem.publishPlayerMovement(location.getLatitude(), location.getLongitude(), 
                    bearing, speed);
            
            if (gameStateListener != null) {
                gameStateListener.onPlayerLocationUpdated(location);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating player location", e);
            if (gameStateListener != null) {
                gameStateListener.onGameError("Location update failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Attempts to capture a treasure
     */
    public CompletableFuture<Boolean> captureTreasure(String treasureId) {
        if (!isActive || currentPlayerLocation == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Check proximity first
        if (!proximitySystem.canCaptureTreasure(treasureId)) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Attempt capture through sync manager (handles conflicts)
        return syncManager.captureTreasure(treasureId, currentPlayerLocation)
                .thenApply(result -> {
                    if (result.success) {
                        // Handle successful capture through lifecycle manager
                        lifecycleManager.captureTreasure(treasureId, playerId, 
                                currentPlayerLocation, this);
                        
                        // Remove from map
                        mapRenderer.removeTreasureMarker(treasureId, true);
                        
                        // Remove from proximity system
                        proximitySystem.removeTreasure(treasureId);
                        
                        return true;
                    }
                    return false;
                });
    }
    
    /**
     * Gets current game statistics
     */
    public GameStatistics getGameStatistics() {
        GameStatistics stats = new GameStatistics();
        stats.activeTreasures = lifecycleManager.getActiveTreasures().size();
        stats.nearbyPlayers = syncManager.getNearbyPlayers().size();
        stats.visibleTreasures = syncManager.getVisibleTreasures().size();
        stats.captureableTreasures = proximitySystem.getTreasuresInZone(
                ProximityDetectionSystem.ProximityZone.CAPTURE).size();
        
        if (currentPlayerLocation != null) {
            stats.distributionStats = distributionAlgorithm.getDistributionStats(
                    currentPlayerLocation, 2.0);
        }
        
        return stats;
    }
    
    // RealTimeEventSystem.GlobalEventListener implementation
    @Override
    public void onTreasureSpawned(RealTimeEventSystem.TreasureSpawnEvent event) {
        if (event.getSourcePlayerId().equals(playerId)) return; // Skip own events
        
        GeoPoint location = new GeoPoint(event.latitude, event.longitude);
        TreasureRarity rarity = TreasureRarity.valueOf(event.rarity);
        
        // Add to proximity system
        proximitySystem.addTreasure(event.treasureId, location, rarity);
        
        // Add to map
        mapRenderer.addTreasureMarker(event.treasureId, location, rarity, 
                "Remote Treasure", true);
        
        if (gameStateListener != null) {
            gameStateListener.onTreasureSpawned(event.treasureId, location, rarity);
        }
    }
    
    @Override
    public void onTreasureCaptured(RealTimeEventSystem.TreasureCaptureEvent event) {
        // Remove from proximity system
        proximitySystem.removeTreasure(event.treasureId);
        
        // Remove from map
        mapRenderer.removeTreasureMarker(event.treasureId, true);
        
        if (gameStateListener != null) {
            gameStateListener.onTreasureCaptured(event.treasureId, event.capturedBy, event.points);
        }
    }
    
    @Override
    public void onTreasureExpired(RealTimeEventSystem.TreasureExpirationEvent event) {
        // Remove from proximity system
        proximitySystem.removeTreasure(event.treasureId);
        
        // Remove from map
        mapRenderer.removeTreasureMarker(event.treasureId, false);
    }
    
    @Override
    public void onPlayerJoined(RealTimeEventSystem.PlayerJoinEvent event) {
        if (gameStateListener != null) {
            gameStateListener.onPlayerJoined(event.playerId, null);
        }
    }
    
    @Override
    public void onPlayerLeft(RealTimeEventSystem.PlayerLeaveEvent event) {
        // Remove player marker
        mapRenderer.removePlayerMarker(event.playerId);
        
        // Remove from proximity system
        proximitySystem.removePlayer(event.playerId);
        
        if (gameStateListener != null) {
            gameStateListener.onPlayerLeft(event.playerId);
        }
    }
    
    @Override
    public void onPlayerMoved(RealTimeEventSystem.PlayerMovementEvent event) {
        if (event.playerId.equals(playerId)) return; // Skip own movement
        
        GeoPoint location = new GeoPoint(event.latitude, event.longitude);
        
        // Update player marker
        mapRenderer.addPlayerMarker(event.playerId, location, "Player", true);
        
        // Update proximity system
        Location playerLocation = new Location("");
        playerLocation.setLatitude(event.latitude);
        playerLocation.setLongitude(event.longitude);
        proximitySystem.addPlayer(event.playerId, playerLocation);
    }
    
    @Override
    public void onGameStateChanged(RealTimeEventSystem.GameStateChangeEvent event) {
        Log.d(TAG, "Game state changed: " + event.stateType + " = " + event.newValue);
    }
    
    @Override
    public void onProximityAlert(RealTimeEventSystem.ProximityAlertEvent event) {
        // Handle proximity alerts from other players
    }
    
    @Override
    public void onSystemMessage(RealTimeEventSystem.SystemMessageEvent event) {
        Log.d(TAG, "System message: " + event.message);
    }
    
    // MultiplayerSyncManager.MultiplayerEventListener implementation
    @Override
    public void onPlayerJoined(MultiplayerSyncManager.PlayerState player) {
        GeoPoint location = new GeoPoint(player.location.getLatitude(), player.location.getLongitude());
        mapRenderer.addPlayerMarker(player.playerId, location, "Player", false);
        proximitySystem.addPlayer(player.playerId, player.location);
    }
    
    @Override
    public void onPlayerLeft(String playerId) {
        mapRenderer.removePlayerMarker(playerId);
        proximitySystem.removePlayer(playerId);
    }
    
    @Override
    public void onPlayerMoved(MultiplayerSyncManager.PlayerState player) {
        GeoPoint location = new GeoPoint(player.location.getLatitude(), player.location.getLongitude());
        mapRenderer.addPlayerMarker(player.playerId, location, "Player", true);
        proximitySystem.addPlayer(player.playerId, player.location);
    }
    
    @Override
    public void onTreasureSpawned(MultiplayerSyncManager.TreasureState treasure) {
        proximitySystem.addTreasure(treasure.treasureId, treasure.location, treasure.rarity);
        mapRenderer.addTreasureMarker(treasure.treasureId, treasure.location, treasure.rarity, 
                treasure.title, true);
    }
    
    @Override
    public void onTreasureCaptured(String treasureId, String playerId) {
        proximitySystem.removeTreasure(treasureId);
        mapRenderer.removeTreasureMarker(treasureId, true);
    }
    
    @Override
    public void onTreasureExpired(String treasureId) {
        proximitySystem.removeTreasure(treasureId);
        mapRenderer.removeTreasureMarker(treasureId, false);
    }
    
    @Override
    public void onSyncError(String error) {
        Log.e(TAG, "Sync error: " + error);
        if (gameStateListener != null) {
            gameStateListener.onGameError("Sync error: " + error);
        }
    }
    
    // ProximityDetectionSystem.ProximityEventListener implementation
    @Override
    public void onTreasureDetected(String treasureId, TreasureRarity rarity, float distance, float bearing) {
        // Show direction arrow for distant treasures
        if (currentPlayerLocation != null) {
            GeoPoint playerLocation = new GeoPoint(currentPlayerLocation.getLatitude(), 
                    currentPlayerLocation.getLongitude());
            
            // Calculate treasure location from bearing and distance
            double lat = currentPlayerLocation.getLatitude() + 
                    (distance * Math.cos(Math.toRadians(bearing))) / 111320.0;
            double lon = currentPlayerLocation.getLongitude() + 
                    (distance * Math.sin(Math.toRadians(bearing))) / 
                    (111320.0 * Math.cos(Math.toRadians(currentPlayerLocation.getLatitude())));
            
            GeoPoint treasureLocation = new GeoPoint(lat, lon);
            mapRenderer.showDirectionArrow(playerLocation, treasureLocation, rarity, distance);
        }
    }
    
    @Override
    public void onTreasureInAwareness(String treasureId, TreasureRarity rarity, float distance) {
        mapRenderer.updateTreasureProximity(treasureId, 
                ProximityDetectionSystem.ProximityZone.AWARENESS, distance);
    }
    
    @Override
    public void onTreasureInInteraction(String treasureId, TreasureRarity rarity, float distance) {
        mapRenderer.updateTreasureProximity(treasureId, 
                ProximityDetectionSystem.ProximityZone.INTERACTION, distance);
    }
    
    @Override
    public void onTreasureInCapture(String treasureId, TreasureRarity rarity, float distance) {
        mapRenderer.updateTreasureProximity(treasureId, 
                ProximityDetectionSystem.ProximityZone.CAPTURE, distance);
        
        if (gameStateListener != null) {
            gameStateListener.onProximityAlert("Treasure ready to capture!", 
                    ProximityDetectionSystem.AlertType.SUCCESS);
        }
    }
    
    @Override
    public void onTreasureLostProximity(String treasureId, ProximityDetectionSystem.ProximityZone previousZone) {
        mapRenderer.updateTreasureProximity(treasureId, 
                ProximityDetectionSystem.ProximityZone.NONE, Float.MAX_VALUE);
        mapRenderer.hideDirectionArrow();
    }
    
    @Override
    public void onPlayerDetected(String playerId, float distance, float bearing) {
        // Player detection handled by sync manager
    }
    
    @Override
    public void onPlayerLostProximity(String playerId) {
        // Player proximity loss handled by sync manager
    }
    
    @Override
    public void onProximityAlert(String message, ProximityDetectionSystem.AlertType type) {
        if (gameStateListener != null) {
            gameStateListener.onProximityAlert(message, type);
        }
    }
    
    // TreasureLifecycleManager.LifecycleEventListener implementation
    @Override
    public void onTreasureSpawned(TreasureLifecycleManager.TreasureLifecycle treasure) {
        proximitySystem.addTreasure(treasure.treasureId, treasure.location, treasure.rarity);
        mapRenderer.addTreasureMarker(treasure.treasureId, treasure.location, treasure.rarity, 
                treasure.title, true);
        
        if (gameStateListener != null) {
            gameStateListener.onTreasureSpawned(treasure.treasureId, treasure.location, treasure.rarity);
        }
    }
    
    @Override
    public void onTreasureActivated(TreasureLifecycleManager.TreasureLifecycle treasure) {
        // Treasure activated from loading existing treasures
        proximitySystem.addTreasure(treasure.treasureId, treasure.location, treasure.rarity);
        mapRenderer.addTreasureMarker(treasure.treasureId, treasure.location, treasure.rarity, 
                treasure.title, false);
    }
    
    @Override
    public void onTreasureExpiring(TreasureLifecycleManager.TreasureLifecycle treasure, long timeRemaining) {
        // Visual indication that treasure is expiring soon
        mapRenderer.updateTreasureProximity(treasure.treasureId, 
                ProximityDetectionSystem.ProximityZone.AWARENESS, 0);
    }
    
    @Override
    public void onTreasureCaptured(TreasureLifecycleManager.TreasureLifecycle treasure, String capturedBy) {
        proximitySystem.removeTreasure(treasure.treasureId);
        mapRenderer.removeTreasureMarker(treasure.treasureId, true);
        
        if (gameStateListener != null) {
            int points = calculateCapturePoints(treasure.rarity);
            gameStateListener.onTreasureCaptured(treasure.treasureId, capturedBy, points);
        }
    }
    
    @Override
    public void onTreasureExpired(TreasureLifecycleManager.TreasureLifecycle treasure, String reason) {
        proximitySystem.removeTreasure(treasure.treasureId);
        mapRenderer.removeTreasureMarker(treasure.treasureId, false);
    }
    
    @Override
    public void onTreasureCleanedUp(String treasureId) {
        // Cleanup handled automatically
    }
    
    private void spawnInitialTreasures(Location location) {
        treasureSpawner.spawnTreasuresForArea(location, playerId, playerLevel)
                .thenAccept(treasures -> {
                    for (com.treasurehuntadventure.tha.Treasure treasure : treasures) {
                        lifecycleManager.createTreasure(treasure.getLocation(), treasure.getRarity(), 
                                treasure.getTitle(), playerId, new HashMap<>(), this);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to spawn initial treasures", throwable);
                    return null;
                });
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
    
    /**
     * Game statistics data class
     */
    public static class GameStatistics {
        public int activeTreasures;
        public int nearbyPlayers;
        public int visibleTreasures;
        public int captureableTreasures;
        public TreasureDistributionAlgorithm.DistributionStats distributionStats;
    }
}
