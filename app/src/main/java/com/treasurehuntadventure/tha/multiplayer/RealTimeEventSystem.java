package com.treasurehuntadventure.tha.multiplayer;

import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real-time event system for multiplayer treasure hunting with WebSocket-like functionality
 */
public class RealTimeEventSystem {
    private static final String TAG = "RealTimeEventSystem";
    
    // Event types
    public enum EventType {
        TREASURE_SPAWNED,
        TREASURE_CAPTURED,
        TREASURE_EXPIRED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        PLAYER_MOVED,
        GAME_STATE_CHANGED,
        PROXIMITY_ALERT,
        SYSTEM_MESSAGE
    }
    
    // Event priority levels
    public enum EventPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        EventPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    private final FirebaseFirestore firestore;
    private final Map<String, ListenerRegistration> eventListeners;
    private final Map<EventType, EventHandler> eventHandlers;
    private final ExecutorService eventExecutor;
    private final EventQueue eventQueue;
    private final String playerId;
    
    // Event listener interfaces
    public interface EventHandler {
        void handleEvent(GameEvent event);
    }
    
    public interface GlobalEventListener {
        void onTreasureSpawned(TreasureSpawnEvent event);
        void onTreasureCaptured(TreasureCaptureEvent event);
        void onTreasureExpired(TreasureExpirationEvent event);
        void onPlayerJoined(PlayerJoinEvent event);
        void onPlayerLeft(PlayerLeaveEvent event);
        void onPlayerMoved(PlayerMovementEvent event);
        void onGameStateChanged(GameStateChangeEvent event);
        void onProximityAlert(ProximityAlertEvent event);
        void onSystemMessage(SystemMessageEvent event);
    }
    
    public RealTimeEventSystem(String playerId) {
        this.firestore = FirebaseFirestore.getInstance();
        this.eventListeners = new ConcurrentHashMap<>();
        this.eventHandlers = new ConcurrentHashMap<>();
        this.eventExecutor = Executors.newFixedThreadPool(3);
        this.eventQueue = new EventQueue();
        this.playerId = playerId;
        
        setupEventHandlers();
    }
    
    /**
     * Starts the real-time event system
     */
    public void start(GlobalEventListener globalListener) {
        Log.d(TAG, "Starting real-time event system for player: " + playerId);
        
        // Start event queue processor
        eventQueue.start();
        
        // Setup global event listener
        setupGlobalEventListener(globalListener);
        
        // Start listening for treasure events
        startTreasureEventListener();
        
        // Start listening for player events
        startPlayerEventListener();
        
        // Start listening for game state events
        startGameStateEventListener();
        
        // Announce player presence
        announcePlayerJoined();
    }
    
    /**
     * Stops the real-time event system
     */
    public void stop() {
        Log.d(TAG, "Stopping real-time event system");
        
        // Announce player leaving
        announcePlayerLeft();
        
        // Stop all listeners
        for (ListenerRegistration listener : eventListeners.values()) {
            listener.remove();
        }
        eventListeners.clear();
        
        // Stop event queue
        eventQueue.stop();
        
        // Shutdown executor
        eventExecutor.shutdown();
    }
    
    /**
     * Publishes an event to the system
     */
    public void publishEvent(GameEvent event) {
        Log.d(TAG, "Publishing event: " + event.getType() + " with priority: " + event.getPriority());
        
        // Add to local event queue
        eventQueue.enqueue(event);
        
        // Publish to Firestore for other players
        publishEventToFirestore(event);
    }
    
    /**
     * Registers a custom event handler
     */
    public void registerEventHandler(EventType eventType, EventHandler handler) {
        eventHandlers.put(eventType, handler);
    }
    
    /**
     * Publishes a treasure spawn event
     */
    public void publishTreasureSpawn(String treasureId, double latitude, double longitude, 
                                   String rarity, long spawnTime) {
        TreasureSpawnEvent event = new TreasureSpawnEvent(treasureId, latitude, longitude, 
                rarity, spawnTime, playerId);
        publishEvent(event);
    }
    
    /**
     * Publishes a treasure capture event
     */
    public void publishTreasureCapture(String treasureId, String capturedBy, long captureTime, 
                                     String rarity, int points) {
        TreasureCaptureEvent event = new TreasureCaptureEvent(treasureId, capturedBy, 
                captureTime, rarity, points);
        publishEvent(event);
    }
    
    /**
     * Publishes a player movement event
     */
    public void publishPlayerMovement(double latitude, double longitude, float bearing, 
                                    float speed) {
        PlayerMovementEvent event = new PlayerMovementEvent(playerId, latitude, longitude, 
                bearing, speed, System.currentTimeMillis());
        publishEvent(event);
    }
    
    /**
     * Publishes a proximity alert event
     */
    public void publishProximityAlert(String treasureId, String alertType, float distance) {
        ProximityAlertEvent event = new ProximityAlertEvent(playerId, treasureId, alertType, 
                distance, System.currentTimeMillis());
        publishEvent(event);
    }
    
    private void setupEventHandlers() {
        // Default event handlers that process events locally
        eventHandlers.put(EventType.TREASURE_SPAWNED, event -> {
            Log.d(TAG, "Processing treasure spawn event: " + ((TreasureSpawnEvent) event).treasureId);
        });
        
        eventHandlers.put(EventType.TREASURE_CAPTURED, event -> {
            Log.d(TAG, "Processing treasure capture event: " + ((TreasureCaptureEvent) event).treasureId);
        });
        
        eventHandlers.put(EventType.PLAYER_MOVED, event -> {
            Log.d(TAG, "Processing player movement event: " + ((PlayerMovementEvent) event).playerId);
        });
    }
    
    private void setupGlobalEventListener(GlobalEventListener globalListener) {
        if (globalListener == null) return;
        
        // Register global event handlers
        registerEventHandler(EventType.TREASURE_SPAWNED, event -> 
                globalListener.onTreasureSpawned((TreasureSpawnEvent) event));
        
        registerEventHandler(EventType.TREASURE_CAPTURED, event -> 
                globalListener.onTreasureCaptured((TreasureCaptureEvent) event));
        
        registerEventHandler(EventType.TREASURE_EXPIRED, event -> 
                globalListener.onTreasureExpired((TreasureExpirationEvent) event));
        
        registerEventHandler(EventType.PLAYER_JOINED, event -> 
                globalListener.onPlayerJoined((PlayerJoinEvent) event));
        
        registerEventHandler(EventType.PLAYER_LEFT, event -> 
                globalListener.onPlayerLeft((PlayerLeaveEvent) event));
        
        registerEventHandler(EventType.PLAYER_MOVED, event -> 
                globalListener.onPlayerMoved((PlayerMovementEvent) event));
        
        registerEventHandler(EventType.GAME_STATE_CHANGED, event -> 
                globalListener.onGameStateChanged((GameStateChangeEvent) event));
        
        registerEventHandler(EventType.PROXIMITY_ALERT, event -> 
                globalListener.onProximityAlert((ProximityAlertEvent) event));
        
        registerEventHandler(EventType.SYSTEM_MESSAGE, event -> 
                globalListener.onSystemMessage((SystemMessageEvent) event));
    }
    
    private void startTreasureEventListener() {
        Query treasureQuery = firestore.collection("treasure_events")
                .whereGreaterThan("timestamp", System.currentTimeMillis())
                .orderBy("timestamp", Query.Direction.ASCENDING);
        
        ListenerRegistration listener = treasureQuery.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Treasure event listener error", e);
                return;
            }
            
            if (snapshot != null) {
                for (DocumentChange change : snapshot.getDocumentChanges()) {
                    if (change.getType() == DocumentChange.Type.ADDED) {
                        processFirestoreEvent(change.getDocument());
                    }
                }
            }
        });
        
        eventListeners.put("treasures", listener);
    }
    
    private void startPlayerEventListener() {
        Query playerQuery = firestore.collection("player_events")
                .whereGreaterThan("timestamp", System.currentTimeMillis())
                .orderBy("timestamp", Query.Direction.ASCENDING);
        
        ListenerRegistration listener = playerQuery.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Player event listener error", e);
                return;
            }
            
            if (snapshot != null) {
                for (DocumentChange change : snapshot.getDocumentChanges()) {
                    if (change.getType() == DocumentChange.Type.ADDED) {
                        processFirestoreEvent(change.getDocument());
                    }
                }
            }
        });
        
        eventListeners.put("players", listener);
    }
    
    private void startGameStateEventListener() {
        Query gameStateQuery = firestore.collection("game_events")
                .whereGreaterThan("timestamp", System.currentTimeMillis())
                .orderBy("timestamp", Query.Direction.ASCENDING);
        
        ListenerRegistration listener = gameStateQuery.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Game state event listener error", e);
                return;
            }
            
            if (snapshot != null) {
                for (DocumentChange change : snapshot.getDocumentChanges()) {
                    if (change.getType() == DocumentChange.Type.ADDED) {
                        processFirestoreEvent(change.getDocument());
                    }
                }
            }
        });
        
        eventListeners.put("gameState", listener);
    }
    
    private void processFirestoreEvent(DocumentSnapshot document) {
        try {
            String eventTypeStr = document.getString("eventType");
            if (eventTypeStr == null) return;
            
            EventType eventType = EventType.valueOf(eventTypeStr);
            GameEvent event = createEventFromDocument(eventType, document);
            
            if (event != null && !event.getSourcePlayerId().equals(playerId)) {
                // Only process events from other players
                eventQueue.enqueue(event);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing Firestore event", e);
        }
    }
    
    private GameEvent createEventFromDocument(EventType eventType, DocumentSnapshot document) {
        switch (eventType) {
            case TREASURE_SPAWNED:
                return TreasureSpawnEvent.fromDocument(document);
            case TREASURE_CAPTURED:
                return TreasureCaptureEvent.fromDocument(document);
            case TREASURE_EXPIRED:
                return TreasureExpirationEvent.fromDocument(document);
            case PLAYER_JOINED:
                return PlayerJoinEvent.fromDocument(document);
            case PLAYER_LEFT:
                return PlayerLeaveEvent.fromDocument(document);
            case PLAYER_MOVED:
                return PlayerMovementEvent.fromDocument(document);
            case GAME_STATE_CHANGED:
                return GameStateChangeEvent.fromDocument(document);
            case PROXIMITY_ALERT:
                return ProximityAlertEvent.fromDocument(document);
            case SYSTEM_MESSAGE:
                return SystemMessageEvent.fromDocument(document);
            default:
                return null;
        }
    }
    
    private void publishEventToFirestore(GameEvent event) {
        String collection = getCollectionForEventType(event.getType());
        if (collection == null) return;
        
        Map<String, Object> eventData = event.toMap();
        eventData.put("eventType", event.getType().name());
        eventData.put("priority", event.getPriority().name());
        eventData.put("sourcePlayerId", event.getSourcePlayerId());
        eventData.put("timestamp", event.getTimestamp());
        
        firestore.collection(collection)
                .add(eventData)
                .addOnSuccessListener(documentReference -> 
                        Log.d(TAG, "Event published to Firestore: " + event.getType()))
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Failed to publish event to Firestore", e));
    }
    
    private String getCollectionForEventType(EventType eventType) {
        switch (eventType) {
            case TREASURE_SPAWNED:
            case TREASURE_CAPTURED:
            case TREASURE_EXPIRED:
                return "treasure_events";
            case PLAYER_JOINED:
            case PLAYER_LEFT:
            case PLAYER_MOVED:
                return "player_events";
            case GAME_STATE_CHANGED:
            case PROXIMITY_ALERT:
            case SYSTEM_MESSAGE:
                return "game_events";
            default:
                return null;
        }
    }
    
    private void announcePlayerJoined() {
        PlayerJoinEvent event = new PlayerJoinEvent(playerId, System.currentTimeMillis());
        publishEvent(event);
    }
    
    private void announcePlayerLeft() {
        PlayerLeaveEvent event = new PlayerLeaveEvent(playerId, System.currentTimeMillis());
        publishEvent(event);
    }
    
    /**
     * Event queue for processing events in order of priority
     */
    private class EventQueue {
        private final java.util.concurrent.PriorityBlockingQueue<GameEvent> queue;
        private volatile boolean running = false;
        private Thread processorThread;
        
        public EventQueue() {
            this.queue = new java.util.concurrent.PriorityBlockingQueue<>(100, 
                    (e1, e2) -> Integer.compare(e2.getPriority().getLevel(), e1.getPriority().getLevel()));
        }
        
        public void start() {
            running = true;
            processorThread = new Thread(this::processEvents);
            processorThread.start();
        }
        
        public void stop() {
            running = false;
            if (processorThread != null) {
                processorThread.interrupt();
            }
        }
        
        public void enqueue(GameEvent event) {
            queue.offer(event);
        }
        
        private void processEvents() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    GameEvent event = queue.take();
                    processEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error processing event", e);
                }
            }
        }
        
        private void processEvent(GameEvent event) {
            EventHandler handler = eventHandlers.get(event.getType());
            if (handler != null) {
                eventExecutor.execute(() -> {
                    try {
                        handler.handleEvent(event);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in event handler for " + event.getType(), e);
                    }
                });
            }
        }
    }
    
    /**
     * Base class for all game events
     */
    public abstract static class GameEvent implements Comparable<GameEvent> {
        protected final EventType type;
        protected final EventPriority priority;
        protected final String sourcePlayerId;
        protected final long timestamp;
        
        public GameEvent(EventType type, EventPriority priority, String sourcePlayerId, long timestamp) {
            this.type = type;
            this.priority = priority;
            this.sourcePlayerId = sourcePlayerId;
            this.timestamp = timestamp;
        }
        
        public EventType getType() { return type; }
        public EventPriority getPriority() { return priority; }
        public String getSourcePlayerId() { return sourcePlayerId; }
        public long getTimestamp() { return timestamp; }
        
        public abstract Map<String, Object> toMap();
        
        @Override
        public int compareTo(GameEvent other) {
            return Integer.compare(other.priority.getLevel(), this.priority.getLevel());
        }
    }
    
    /**
     * Treasure spawn event
     */
    public static class TreasureSpawnEvent extends GameEvent {
        public final String treasureId;
        public final double latitude;
        public final double longitude;
        public final String rarity;
        public final long spawnTime;
        
        public TreasureSpawnEvent(String treasureId, double latitude, double longitude, 
                                String rarity, long spawnTime, String sourcePlayerId) {
            super(EventType.TREASURE_SPAWNED, EventPriority.HIGH, sourcePlayerId, System.currentTimeMillis());
            this.treasureId = treasureId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rarity = rarity;
            this.spawnTime = spawnTime;
        }
        
        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("treasureId", treasureId);
            map.put("latitude", latitude);
            map.put("longitude", longitude);
            map.put("rarity", rarity);
            map.put("spawnTime", spawnTime);
            return map;
        }
        
        public static TreasureSpawnEvent fromDocument(DocumentSnapshot doc) {
            return new TreasureSpawnEvent(
                    doc.getString("treasureId"),
                    doc.getDouble("latitude"),
                    doc.getDouble("longitude"),
                    doc.getString("rarity"),
                    doc.getLong("spawnTime"),
                    doc.getString("sourcePlayerId")
            );
        }
    }
    
    /**
     * Treasure capture event
     */
    public static class TreasureCaptureEvent extends GameEvent {
        public final String treasureId;
        public final String capturedBy;
        public final long captureTime;
        public final String rarity;
        public final int points;
        
        public TreasureCaptureEvent(String treasureId, String capturedBy, long captureTime, 
                                  String rarity, int points) {
            super(EventType.TREASURE_CAPTURED, EventPriority.HIGH, capturedBy, System.currentTimeMillis());
            this.treasureId = treasureId;
            this.capturedBy = capturedBy;
            this.captureTime = captureTime;
            this.rarity = rarity;
            this.points = points;
        }
        
        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("treasureId", treasureId);
            map.put("capturedBy", capturedBy);
            map.put("captureTime", captureTime);
            map.put("rarity", rarity);
            map.put("points", points);
            return map;
        }
        
        public static TreasureCaptureEvent fromDocument(DocumentSnapshot doc) {
            return new TreasureCaptureEvent(
                    doc.getString("treasureId"),
                    doc.getString("capturedBy"),
                    doc.getLong("captureTime"),
                    doc.getString("rarity"),
                    doc.getLong("points").intValue()
            );
        }
    }

    /**
     * Treasure expiration event
     */
    public static class TreasureExpirationEvent extends GameEvent {
        public final String treasureId;
        public final long expirationTime;
        public final String reason;

        public TreasureExpirationEvent(String treasureId, long expirationTime, String reason, String sourcePlayerId) {
            super(EventType.TREASURE_EXPIRED, EventPriority.NORMAL, sourcePlayerId, System.currentTimeMillis());
            this.treasureId = treasureId;
            this.expirationTime = expirationTime;
            this.reason = reason;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("treasureId", treasureId);
            map.put("expirationTime", expirationTime);
            map.put("reason", reason);
            return map;
        }

        public static TreasureExpirationEvent fromDocument(DocumentSnapshot doc) {
            return new TreasureExpirationEvent(
                    doc.getString("treasureId"),
                    doc.getLong("expirationTime"),
                    doc.getString("reason"),
                    doc.getString("sourcePlayerId")
            );
        }
    }

    /**
     * Player join event
     */
    public static class PlayerJoinEvent extends GameEvent {
        public final String playerId;
        public final long joinTime;

        public PlayerJoinEvent(String playerId, long joinTime) {
            super(EventType.PLAYER_JOINED, EventPriority.NORMAL, playerId, System.currentTimeMillis());
            this.playerId = playerId;
            this.joinTime = joinTime;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("playerId", playerId);
            map.put("joinTime", joinTime);
            return map;
        }

        public static PlayerJoinEvent fromDocument(DocumentSnapshot doc) {
            return new PlayerJoinEvent(
                    doc.getString("playerId"),
                    doc.getLong("joinTime")
            );
        }
    }

    /**
     * Player leave event
     */
    public static class PlayerLeaveEvent extends GameEvent {
        public final String playerId;
        public final long leaveTime;

        public PlayerLeaveEvent(String playerId, long leaveTime) {
            super(EventType.PLAYER_LEFT, EventPriority.NORMAL, playerId, System.currentTimeMillis());
            this.playerId = playerId;
            this.leaveTime = leaveTime;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("playerId", playerId);
            map.put("leaveTime", leaveTime);
            return map;
        }

        public static PlayerLeaveEvent fromDocument(DocumentSnapshot doc) {
            return new PlayerLeaveEvent(
                    doc.getString("playerId"),
                    doc.getLong("leaveTime")
            );
        }
    }

    /**
     * Player movement event
     */
    public static class PlayerMovementEvent extends GameEvent {
        public final String playerId;
        public final double latitude;
        public final double longitude;
        public final float bearing;
        public final float speed;
        public final long movementTime;

        public PlayerMovementEvent(String playerId, double latitude, double longitude,
                                 float bearing, float speed, long movementTime) {
            super(EventType.PLAYER_MOVED, EventPriority.LOW, playerId, System.currentTimeMillis());
            this.playerId = playerId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.bearing = bearing;
            this.speed = speed;
            this.movementTime = movementTime;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("playerId", playerId);
            map.put("latitude", latitude);
            map.put("longitude", longitude);
            map.put("bearing", bearing);
            map.put("speed", speed);
            map.put("movementTime", movementTime);
            return map;
        }

        public static PlayerMovementEvent fromDocument(DocumentSnapshot doc) {
            return new PlayerMovementEvent(
                    doc.getString("playerId"),
                    doc.getDouble("latitude"),
                    doc.getDouble("longitude"),
                    doc.getDouble("bearing").floatValue(),
                    doc.getDouble("speed").floatValue(),
                    doc.getLong("movementTime")
            );
        }
    }

    /**
     * Game state change event
     */
    public static class GameStateChangeEvent extends GameEvent {
        public final String stateType;
        public final String newValue;
        public final String oldValue;
        public final String reason;

        public GameStateChangeEvent(String stateType, String newValue, String oldValue,
                                  String reason, String sourcePlayerId) {
            super(EventType.GAME_STATE_CHANGED, EventPriority.HIGH, sourcePlayerId, System.currentTimeMillis());
            this.stateType = stateType;
            this.newValue = newValue;
            this.oldValue = oldValue;
            this.reason = reason;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("stateType", stateType);
            map.put("newValue", newValue);
            map.put("oldValue", oldValue);
            map.put("reason", reason);
            return map;
        }

        public static GameStateChangeEvent fromDocument(DocumentSnapshot doc) {
            return new GameStateChangeEvent(
                    doc.getString("stateType"),
                    doc.getString("newValue"),
                    doc.getString("oldValue"),
                    doc.getString("reason"),
                    doc.getString("sourcePlayerId")
            );
        }
    }

    /**
     * Proximity alert event
     */
    public static class ProximityAlertEvent extends GameEvent {
        public final String playerId;
        public final String treasureId;
        public final String alertType;
        public final float distance;
        public final long alertTime;

        public ProximityAlertEvent(String playerId, String treasureId, String alertType,
                                 float distance, long alertTime) {
            super(EventType.PROXIMITY_ALERT, EventPriority.NORMAL, playerId, System.currentTimeMillis());
            this.playerId = playerId;
            this.treasureId = treasureId;
            this.alertType = alertType;
            this.distance = distance;
            this.alertTime = alertTime;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("playerId", playerId);
            map.put("treasureId", treasureId);
            map.put("alertType", alertType);
            map.put("distance", distance);
            map.put("alertTime", alertTime);
            return map;
        }

        public static ProximityAlertEvent fromDocument(DocumentSnapshot doc) {
            return new ProximityAlertEvent(
                    doc.getString("playerId"),
                    doc.getString("treasureId"),
                    doc.getString("alertType"),
                    doc.getDouble("distance").floatValue(),
                    doc.getLong("alertTime")
            );
        }
    }

    /**
     * System message event
     */
    public static class SystemMessageEvent extends GameEvent {
        public final String messageType;
        public final String message;
        public final String targetPlayerId;
        public final Map<String, Object> metadata;

        public SystemMessageEvent(String messageType, String message, String targetPlayerId,
                                Map<String, Object> metadata, String sourcePlayerId) {
            super(EventType.SYSTEM_MESSAGE, EventPriority.NORMAL, sourcePlayerId, System.currentTimeMillis());
            this.messageType = messageType;
            this.message = message;
            this.targetPlayerId = targetPlayerId;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("messageType", messageType);
            map.put("message", message);
            map.put("targetPlayerId", targetPlayerId);
            map.put("metadata", metadata);
            return map;
        }

        public static SystemMessageEvent fromDocument(DocumentSnapshot doc) {
            return new SystemMessageEvent(
                    doc.getString("messageType"),
                    doc.getString("message"),
                    doc.getString("targetPlayerId"),
                    (Map<String, Object>) doc.get("metadata"),
                    doc.getString("sourcePlayerId")
            );
        }
    }
}
