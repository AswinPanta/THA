package com.treasurehuntadventure.tha.multiplayer;

import android.location.Location;
import android.util.Log;

import com.treasurehuntadventure.tha.TreasureRarity;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced proximity detection system for treasure hunting with dynamic ranges and notifications
 */
public class ProximityDetectionSystem {
    private static final String TAG = "ProximityDetection";
    
    // Proximity ranges based on treasure rarity (in meters)
    private static final Map<TreasureRarity, ProximityConfig> RARITY_CONFIGS = new HashMap<>();
    
    // Detection zones
    private static final float CAPTURE_RANGE = 50.0f;      // Can capture treasure
    private static final float INTERACTION_RANGE = 100.0f;  // Can see treasure details
    private static final float AWARENESS_RANGE = 300.0f;    // Treasure appears on map
    private static final float DETECTION_RANGE = 1000.0f;   // Treasure direction indicator
    
    static {
        // Configure proximity ranges for different rarities
        RARITY_CONFIGS.put(TreasureRarity.COMMON, new ProximityConfig(
                CAPTURE_RANGE, INTERACTION_RANGE, AWARENESS_RANGE, DETECTION_RANGE, 1.0f));
        RARITY_CONFIGS.put(TreasureRarity.UNCOMMON, new ProximityConfig(
                CAPTURE_RANGE, INTERACTION_RANGE * 1.2f, AWARENESS_RANGE * 1.3f, DETECTION_RANGE * 1.5f, 1.2f));
        RARITY_CONFIGS.put(TreasureRarity.RARE, new ProximityConfig(
                CAPTURE_RANGE, INTERACTION_RANGE * 1.5f, AWARENESS_RANGE * 1.6f, DETECTION_RANGE * 2.0f, 1.5f));
        RARITY_CONFIGS.put(TreasureRarity.EPIC, new ProximityConfig(
                CAPTURE_RANGE, INTERACTION_RANGE * 2.0f, AWARENESS_RANGE * 2.0f, DETECTION_RANGE * 3.0f, 2.0f));
        RARITY_CONFIGS.put(TreasureRarity.LEGENDARY, new ProximityConfig(
                CAPTURE_RANGE, INTERACTION_RANGE * 3.0f, AWARENESS_RANGE * 3.0f, DETECTION_RANGE * 5.0f, 3.0f));
    }
    
    private final Map<String, TreasureProximityState> treasureStates;
    private final Map<String, PlayerProximityState> playerStates;
    private final ProximityEventListener eventListener;
    private Location currentPlayerLocation;
    
    public interface ProximityEventListener {
        void onTreasureDetected(String treasureId, TreasureRarity rarity, float distance, float bearing);
        void onTreasureInAwareness(String treasureId, TreasureRarity rarity, float distance);
        void onTreasureInInteraction(String treasureId, TreasureRarity rarity, float distance);
        void onTreasureInCapture(String treasureId, TreasureRarity rarity, float distance);
        void onTreasureLostProximity(String treasureId, ProximityZone previousZone);
        void onPlayerDetected(String playerId, float distance, float bearing);
        void onPlayerLostProximity(String playerId);
        void onProximityAlert(String message, AlertType type);
    }
    
    public enum ProximityZone {
        NONE, DETECTION, AWARENESS, INTERACTION, CAPTURE
    }
    
    public enum AlertType {
        INFO, WARNING, SUCCESS, DANGER
    }
    
    public ProximityDetectionSystem(ProximityEventListener listener) {
        this.treasureStates = new ConcurrentHashMap<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.eventListener = listener;
    }
    
    /**
     * Updates player location and recalculates all proximities
     */
    public void updatePlayerLocation(Location location) {
        this.currentPlayerLocation = location;
        
        // Update treasure proximities
        updateTreasureProximities();
        
        // Update player proximities
        updatePlayerProximities();
    }
    
    /**
     * Adds or updates a treasure for proximity tracking
     */
    public void addTreasure(String treasureId, GeoPoint location, TreasureRarity rarity) {
        TreasureProximityState state = new TreasureProximityState(treasureId, location, rarity);
        treasureStates.put(treasureId, state);
        
        if (currentPlayerLocation != null) {
            updateTreasureProximity(state);
        }
    }
    
    /**
     * Removes a treasure from proximity tracking
     */
    public void removeTreasure(String treasureId) {
        TreasureProximityState state = treasureStates.remove(treasureId);
        if (state != null && state.currentZone != ProximityZone.NONE) {
            eventListener.onTreasureLostProximity(treasureId, state.currentZone);
        }
    }
    
    /**
     * Adds or updates a player for proximity tracking
     */
    public void addPlayer(String playerId, Location location) {
        PlayerProximityState state = new PlayerProximityState(playerId, location);
        playerStates.put(playerId, state);
        
        if (currentPlayerLocation != null) {
            updatePlayerProximity(state);
        }
    }
    
    /**
     * Removes a player from proximity tracking
     */
    public void removePlayer(String playerId) {
        PlayerProximityState state = playerStates.remove(playerId);
        if (state != null && state.isDetected) {
            eventListener.onPlayerLostProximity(playerId);
        }
    }
    
    /**
     * Gets the current proximity zone for a treasure
     */
    public ProximityZone getTreasureProximityZone(String treasureId) {
        TreasureProximityState state = treasureStates.get(treasureId);
        return state != null ? state.currentZone : ProximityZone.NONE;
    }
    
    /**
     * Checks if a treasure can be captured from current location
     */
    public boolean canCaptureTreasure(String treasureId) {
        return getTreasureProximityZone(treasureId) == ProximityZone.CAPTURE;
    }
    
    /**
     * Gets distance to a treasure
     */
    public float getDistanceToTreasure(String treasureId) {
        TreasureProximityState state = treasureStates.get(treasureId);
        if (state == null || currentPlayerLocation == null) {
            return Float.MAX_VALUE;
        }
        
        Location treasureLocation = new Location("");
        treasureLocation.setLatitude(state.location.getLatitude());
        treasureLocation.setLongitude(state.location.getLongitude());
        
        return currentPlayerLocation.distanceTo(treasureLocation);
    }
    
    /**
     * Gets bearing to a treasure
     */
    public float getBearingToTreasure(String treasureId) {
        TreasureProximityState state = treasureStates.get(treasureId);
        if (state == null || currentPlayerLocation == null) {
            return 0.0f;
        }
        
        Location treasureLocation = new Location("");
        treasureLocation.setLatitude(state.location.getLatitude());
        treasureLocation.setLongitude(state.location.getLongitude());
        
        return currentPlayerLocation.bearingTo(treasureLocation);
    }
    
    /**
     * Gets all treasures in a specific proximity zone
     */
    public List<String> getTreasuresInZone(ProximityZone zone) {
        List<String> treasures = new ArrayList<>();
        for (TreasureProximityState state : treasureStates.values()) {
            if (state.currentZone == zone) {
                treasures.add(state.treasureId);
            }
        }
        return treasures;
    }
    
    /**
     * Gets the closest treasure in capture range
     */
    public String getClosestCapturableTreasure() {
        String closestTreasure = null;
        float closestDistance = Float.MAX_VALUE;
        
        for (TreasureProximityState state : treasureStates.values()) {
            if (state.currentZone == ProximityZone.CAPTURE) {
                float distance = getDistanceToTreasure(state.treasureId);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTreasure = state.treasureId;
                }
            }
        }
        
        return closestTreasure;
    }
    
    /**
     * Validates if a treasure capture is legitimate based on proximity
     */
    public boolean validateTreasureCapture(String treasureId, Location captureLocation) {
        TreasureProximityState state = treasureStates.get(treasureId);
        if (state == null) {
            return false;
        }
        
        Location treasureLocation = new Location("");
        treasureLocation.setLatitude(state.location.getLatitude());
        treasureLocation.setLongitude(state.location.getLongitude());
        
        float distance = captureLocation.distanceTo(treasureLocation);
        ProximityConfig config = RARITY_CONFIGS.get(state.rarity);
        
        return distance <= config.captureRange;
    }
    
    private void updateTreasureProximities() {
        for (TreasureProximityState state : treasureStates.values()) {
            updateTreasureProximity(state);
        }
    }
    
    private void updateTreasureProximity(TreasureProximityState state) {
        if (currentPlayerLocation == null) return;
        
        Location treasureLocation = new Location("");
        treasureLocation.setLatitude(state.location.getLatitude());
        treasureLocation.setLongitude(state.location.getLongitude());
        
        float distance = currentPlayerLocation.distanceTo(treasureLocation);
        float bearing = currentPlayerLocation.bearingTo(treasureLocation);
        
        ProximityConfig config = RARITY_CONFIGS.get(state.rarity);
        ProximityZone newZone = determineProximityZone(distance, config);
        ProximityZone oldZone = state.currentZone;
        
        if (newZone != oldZone) {
            state.currentZone = newZone;
            handleZoneTransition(state.treasureId, state.rarity, oldZone, newZone, distance, bearing);
        }
        
        state.lastDistance = distance;
        state.lastBearing = bearing;
        state.lastUpdate = System.currentTimeMillis();
    }
    
    private void updatePlayerProximities() {
        for (PlayerProximityState state : playerStates.values()) {
            updatePlayerProximity(state);
        }
    }
    
    private void updatePlayerProximity(PlayerProximityState state) {
        if (currentPlayerLocation == null) return;
        
        float distance = currentPlayerLocation.distanceTo(state.location);
        float bearing = currentPlayerLocation.bearingTo(state.location);
        
        boolean wasDetected = state.isDetected;
        boolean isNowDetected = distance <= DETECTION_RANGE;
        
        if (isNowDetected && !wasDetected) {
            state.isDetected = true;
            eventListener.onPlayerDetected(state.playerId, distance, bearing);
        } else if (!isNowDetected && wasDetected) {
            state.isDetected = false;
            eventListener.onPlayerLostProximity(state.playerId);
        }
        
        state.lastDistance = distance;
        state.lastBearing = bearing;
        state.lastUpdate = System.currentTimeMillis();
    }
    
    private ProximityZone determineProximityZone(float distance, ProximityConfig config) {
        if (distance <= config.captureRange) {
            return ProximityZone.CAPTURE;
        } else if (distance <= config.interactionRange) {
            return ProximityZone.INTERACTION;
        } else if (distance <= config.awarenessRange) {
            return ProximityZone.AWARENESS;
        } else if (distance <= config.detectionRange) {
            return ProximityZone.DETECTION;
        } else {
            return ProximityZone.NONE;
        }
    }
    
    private void handleZoneTransition(String treasureId, TreasureRarity rarity, ProximityZone oldZone, 
                                    ProximityZone newZone, float distance, float bearing) {
        Log.d(TAG, "Treasure " + treasureId + " zone transition: " + oldZone + " -> " + newZone);
        
        switch (newZone) {
            case DETECTION:
                eventListener.onTreasureDetected(treasureId, rarity, distance, bearing);
                break;
            case AWARENESS:
                eventListener.onTreasureInAwareness(treasureId, rarity, distance);
                if (oldZone == ProximityZone.NONE) {
                    eventListener.onProximityAlert("Treasure detected nearby!", AlertType.INFO);
                }
                break;
            case INTERACTION:
                eventListener.onTreasureInInteraction(treasureId, rarity, distance);
                eventListener.onProximityAlert("Treasure within interaction range!", AlertType.WARNING);
                break;
            case CAPTURE:
                eventListener.onTreasureInCapture(treasureId, rarity, distance);
                eventListener.onProximityAlert("Treasure ready to capture!", AlertType.SUCCESS);
                break;
            case NONE:
                eventListener.onTreasureLostProximity(treasureId, oldZone);
                break;
        }
    }
    
    /**
     * Configuration for proximity ranges based on treasure rarity
     */
    private static class ProximityConfig {
        final float captureRange;
        final float interactionRange;
        final float awarenessRange;
        final float detectionRange;
        final float rarityMultiplier;
        
        ProximityConfig(float captureRange, float interactionRange, float awarenessRange, 
                       float detectionRange, float rarityMultiplier) {
            this.captureRange = captureRange;
            this.interactionRange = interactionRange;
            this.awarenessRange = awarenessRange;
            this.detectionRange = detectionRange;
            this.rarityMultiplier = rarityMultiplier;
        }
    }
    
    /**
     * State tracking for treasure proximity
     */
    private static class TreasureProximityState {
        final String treasureId;
        final GeoPoint location;
        final TreasureRarity rarity;
        ProximityZone currentZone = ProximityZone.NONE;
        float lastDistance = Float.MAX_VALUE;
        float lastBearing = 0.0f;
        long lastUpdate = 0;
        
        TreasureProximityState(String treasureId, GeoPoint location, TreasureRarity rarity) {
            this.treasureId = treasureId;
            this.location = location;
            this.rarity = rarity;
        }
    }
    
    /**
     * State tracking for player proximity
     */
    private static class PlayerProximityState {
        final String playerId;
        final Location location;
        boolean isDetected = false;
        float lastDistance = Float.MAX_VALUE;
        float lastBearing = 0.0f;
        long lastUpdate = 0;
        
        PlayerProximityState(String playerId, Location location) {
            this.playerId = playerId;
            this.location = location;
        }
    }
}
