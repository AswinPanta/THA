package com.treasurehuntadventure.tha.chat;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive chat manager coordinating translation, moderation, suggestions, and real-time messaging
 */
public class EnhancedChatManager {
    private static final String TAG = "EnhancedChatManager";
    
    // Firebase paths
    private static final String MESSAGES_PATH = "chat_messages";
    private static final String CHANNELS_PATH = "chat_channels";
    private static final String PRESENCE_PATH = "user_presence";
    private static final String TYPING_PATH = "typing_indicators";
    
    // Chat configuration
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MESSAGE_HISTORY_LIMIT = 100;
    private static final long TYPING_TIMEOUT_MS = 3000;
    
    private final Context context;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    
    // Core components
    private final TranslationService translationService;
    private final ContentModerationManager moderationManager;
    private final ChatSuggestionEngine suggestionEngine;
    
    // State management
    private final Map<String, ChildEventListener> channelListeners = new ConcurrentHashMap<>();
    private final Map<String, List<EnhancedChatMessage>> messageCache = new ConcurrentHashMap<>();
    private final Map<String, UserPresence> userPresences = new ConcurrentHashMap<>();
    
    // Current user state
    private String currentUserId;
    private String currentUserName;
    private String preferredLanguage;
    private String currentChannelId;
    private Location currentLocation;
    
    // Chat callbacks
    private ChatEventListener chatEventListener;
    
    public interface ChatEventListener {
        void onMessageReceived(EnhancedChatMessage message);
        void onMessageSent(EnhancedChatMessage message);
        void onMessageModerated(String messageId, ContentModerationManager.ModerationResult result);
        void onTranslationComplete(String messageId, String translatedText);
        void onUserJoined(String channelId, String userId, String userName);
        void onUserLeft(String channelId, String userId);
        void onTypingStarted(String channelId, String userId);
        void onTypingStopped(String channelId, String userId);
        void onSuggestionsUpdated(List<ChatSuggestionEngine.ChatSuggestion> suggestions);
        void onChatError(String error);
    }
    
    public EnhancedChatManager(Context context) {
        this.context = context;
        this.database = FirebaseDatabase.getInstance();
        this.auth = FirebaseAuth.getInstance();
        
        // Initialize components
        this.translationService = new TranslationService(context);
        this.moderationManager = new ContentModerationManager(context);
        this.suggestionEngine = new ChatSuggestionEngine(context);
        
        // Setup component listeners
        setupComponentListeners();
        
        // Initialize user state
        initializeUserState();
        
        Log.d(TAG, "Enhanced chat manager initialized");
    }
    
    /**
     * Initializes chat system for current user
     */
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            future.complete(false);
            return future;
        }
        
        currentUserId = user.getUid();
        currentUserName = user.getDisplayName() != null ? user.getDisplayName() : "Anonymous";
        preferredLanguage = translationService.getUserPreferredLanguage();
        
        // Setup user presence
        setupUserPresence()
                .thenAccept(success -> {
                    if (success) {
                        Log.d(TAG, "Chat system initialized for user: " + currentUserName);
                        future.complete(true);
                    } else {
                        future.complete(false);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to initialize chat system", throwable);
                    future.complete(false);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Joins a chat channel
     */
    public CompletableFuture<Boolean> joinChannel(String channelId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (currentUserId == null) {
            future.complete(false);
            return future;
        }
        
        // Leave current channel if any
        if (currentChannelId != null) {
            leaveChannel(currentChannelId);
        }
        
        currentChannelId = channelId;
        
        // Setup message listener for channel
        setupChannelListener(channelId);
        
        // Update user presence in channel
        updateChannelPresence(channelId, true)
                .thenAccept(success -> {
                    if (success) {
                        // Load recent messages
                        loadRecentMessages(channelId);
                        
                        // Update suggestions based on channel context
                        updateSuggestionsForChannel(channelId);
                        
                        if (chatEventListener != null) {
                            chatEventListener.onUserJoined(channelId, currentUserId, currentUserName);
                        }
                        
                        Log.d(TAG, "Joined channel: " + channelId);
                        future.complete(true);
                    } else {
                        future.complete(false);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to join channel: " + channelId, throwable);
                    future.complete(false);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Leaves current chat channel
     */
    public CompletableFuture<Boolean> leaveChannel(String channelId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Remove message listener
        ChildEventListener listener = channelListeners.remove(channelId);
        if (listener != null) {
            database.getReference(MESSAGES_PATH).child(channelId).removeEventListener(listener);
        }
        
        // Update presence
        updateChannelPresence(channelId, false)
                .thenAccept(success -> {
                    if (chatEventListener != null) {
                        chatEventListener.onUserLeft(channelId, currentUserId);
                    }
                    
                    if (channelId.equals(currentChannelId)) {
                        currentChannelId = null;
                    }
                    
                    Log.d(TAG, "Left channel: " + channelId);
                    future.complete(true);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to leave channel: " + channelId, throwable);
                    future.complete(false);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Sends a message to current channel
     */
    public CompletableFuture<Boolean> sendMessage(String text) {
        return sendMessage(text, currentChannelId, EnhancedChatMessage.MessageType.TEXT);
    }
    
    /**
     * Sends a message to specified channel
     */
    public CompletableFuture<Boolean> sendMessage(String text, String channelId, 
                                                EnhancedChatMessage.MessageType messageType) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (currentUserId == null || text == null || text.trim().isEmpty()) {
            future.complete(false);
            return future;
        }
        
        if (text.length() > MAX_MESSAGE_LENGTH) {
            if (chatEventListener != null) {
                chatEventListener.onChatError("Message too long (max " + MAX_MESSAGE_LENGTH + " characters)");
            }
            future.complete(false);
            return future;
        }
        
        // Create message
        EnhancedChatMessage message = new EnhancedChatMessage(
                currentUserId, currentUserName, text.trim(), channelId, messageType);
        
        // Add game context
        addGameContextToMessage(message);
        
        // Moderate message
        moderationManager.moderateMessage(message, getGameContextForChannel(channelId))
                .thenAccept(moderationResult -> {
                    message.setModerationResult(moderationResult);
                    message.setModerated(true);
                    
                    if (moderationResult.isApproved()) {
                        // Detect language and translate if needed
                        processMessageTranslation(message)
                                .thenAccept(translationComplete -> {
                                    // Send to Firebase
                                    sendMessageToFirebase(message, future);
                                })
                                .exceptionally(throwable -> {
                                    Log.e(TAG, "Translation failed", throwable);
                                    // Send anyway without translation
                                    sendMessageToFirebase(message, future);
                                    return null;
                                });
                    } else {
                        // Message blocked by moderation
                        if (chatEventListener != null) {
                            chatEventListener.onMessageModerated(message.getMessageId(), moderationResult);
                        }
                        future.complete(false);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Moderation failed", throwable);
                    if (chatEventListener != null) {
                        chatEventListener.onChatError("Message moderation failed");
                    }
                    future.complete(false);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Translates a message to user's preferred language
     */
    public CompletableFuture<String> translateMessage(EnhancedChatMessage message) {
        if (message.hasTranslation(preferredLanguage)) {
            return CompletableFuture.completedFuture(message.getTranslation(preferredLanguage));
        }
        
        return translationService.translateText(message.getOriginalText(), 
                message.getDetectedLanguage(), preferredLanguage)
                .thenApply(result -> {
                    if (result.translatedText != null && !result.translatedText.equals(message.getOriginalText())) {
                        message.addTranslation(preferredLanguage, result.translatedText);
                        
                        if (chatEventListener != null) {
                            chatEventListener.onTranslationComplete(message.getMessageId(), result.translatedText);
                        }
                        
                        return result.translatedText;
                    }
                    return message.getOriginalText();
                });
    }
    
    /**
     * Updates typing indicator
     */
    public void updateTypingIndicator(boolean isTyping) {
        if (currentChannelId == null || currentUserId == null) return;
        
        DatabaseReference typingRef = database.getReference(TYPING_PATH)
                .child(currentChannelId)
                .child(currentUserId);
        
        if (isTyping) {
            Map<String, Object> typingData = new HashMap<>();
            typingData.put("userName", currentUserName);
            typingData.put("timestamp", ServerValue.TIMESTAMP);
            
            typingRef.setValue(typingData);
            
            // Auto-remove after timeout
            typingRef.removeValue(TYPING_TIMEOUT_MS);
        } else {
            typingRef.removeValue();
        }
    }
    
    /**
     * Updates game context for suggestions
     */
    public void updateGameContext(ChatSuggestionEngine.GameContext context, Location location, 
                                List<String> nearbyPlayers, TreasureRarity treasureRarity) {
        this.currentLocation = location;
        
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("location", location);
        contextData.put("nearbyPlayers", nearbyPlayers);
        contextData.put("treasureRarity", treasureRarity);
        
        suggestionEngine.updateGameContext(context, contextData);
    }
    
    /**
     * Gets current chat suggestions
     */
    public List<ChatSuggestionEngine.ChatSuggestion> getCurrentSuggestions() {
        if (currentUserId != null) {
            return suggestionEngine.getPersonalizedSuggestions(currentUserId);
        }
        return suggestionEngine.getCurrentSuggestions();
    }
    
    /**
     * Records suggestion usage
     */
    public void useSuggestion(ChatSuggestionEngine.ChatSuggestion suggestion) {
        if (currentUserId != null) {
            suggestionEngine.recordSuggestionUsage(currentUserId, suggestion);
        }
        
        // Send the suggestion as a message
        sendMessage(suggestion.getText());
    }
    
    /**
     * Gets cached messages for channel
     */
    public List<EnhancedChatMessage> getCachedMessages(String channelId) {
        return messageCache.getOrDefault(channelId, new ArrayList<>());
    }
    
    /**
     * Sets chat event listener
     */
    public void setChatEventListener(ChatEventListener listener) {
        this.chatEventListener = listener;
    }
    
    /**
     * Gets chat performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("translation", translationService.getPerformanceMetrics());
        metrics.put("moderation", moderationManager.getPerformanceMetrics());
        metrics.put("suggestions", suggestionEngine.getSuggestionAnalytics());
        metrics.put("activeChannels", channelListeners.size());
        metrics.put("cachedMessages", messageCache.values().stream().mapToInt(List::size).sum());
        metrics.put("connectedUsers", userPresences.size());
        return metrics;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Remove all listeners
        for (Map.Entry<String, ChildEventListener> entry : channelListeners.entrySet()) {
            database.getReference(MESSAGES_PATH).child(entry.getKey()).removeEventListener(entry.getValue());
        }
        channelListeners.clear();
        
        // Clear caches
        messageCache.clear();
        userPresences.clear();
        
        // Cleanup components
        translationService.cleanup();
        moderationManager.cleanup();
        
        // Update presence to offline
        if (currentUserId != null) {
            database.getReference(PRESENCE_PATH).child(currentUserId).child("online").setValue(false);
        }
        
        Log.d(TAG, "Enhanced chat manager cleaned up");
    }
    
    private void setupComponentListeners() {
        // Translation service listener
        translationService.setTranslationListener(new TranslationService.TranslationListener() {
            @Override
            public void onLanguageDetected(String text, String languageCode, float confidence) {
                Log.d(TAG, "Language detected: " + languageCode + " (" + confidence + ")");
            }
            
            @Override
            public void onTranslationComplete(String originalText, String translatedText, 
                                            String sourceLanguage, String targetLanguage) {
                Log.d(TAG, "Translation complete: " + sourceLanguage + " -> " + targetLanguage);
            }
            
            @Override
            public void onTranslationError(String text, String error) {
                Log.e(TAG, "Translation error: " + error);
                if (chatEventListener != null) {
                    chatEventListener.onChatError("Translation failed: " + error);
                }
            }
            
            @Override
            public void onTranslationCached(String cacheKey, boolean fromCache) {
                // Translation cached - no action needed
            }
        });
        
        // Moderation manager listener
        moderationManager.setModerationListener(new ContentModerationManager.ModerationListener() {
            @Override
            public void onModerationComplete(String messageId, ContentModerationManager.ModerationResult result) {
                if (chatEventListener != null) {
                    chatEventListener.onMessageModerated(messageId, result);
                }
            }
            
            @Override
            public void onModerationError(String messageId, String error) {
                Log.e(TAG, "Moderation error for message " + messageId + ": " + error);
                if (chatEventListener != null) {
                    chatEventListener.onChatError("Moderation failed: " + error);
                }
            }
            
            @Override
            public void onUserFlagged(String userId, List<ContentModerationManager.ViolationType> violations) {
                Log.w(TAG, "User flagged: " + userId + " for " + violations);
            }
            
            @Override
            public void onEscalationRequired(String messageId, ContentModerationManager.ModerationResult result) {
                Log.w(TAG, "Moderation escalation required for message: " + messageId);
            }
        });
        
        // Suggestion engine listener
        suggestionEngine.setSuggestionListener(new ChatSuggestionEngine.SuggestionListener() {
            @Override
            public void onSuggestionsUpdated(List<ChatSuggestionEngine.ChatSuggestion> suggestions) {
                if (chatEventListener != null) {
                    chatEventListener.onSuggestionsUpdated(suggestions);
                }
            }
            
            @Override
            public void onSuggestionSelected(ChatSuggestionEngine.ChatSuggestion suggestion) {
                Log.d(TAG, "Suggestion selected: " + suggestion.getCategory());
            }
            
            @Override
            public void onContextChanged(ChatSuggestionEngine.GameContext newContext) {
                Log.d(TAG, "Game context changed: " + newContext);
            }
        });
    }
    
    private void initializeUserState() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            currentUserName = user.getDisplayName() != null ? user.getDisplayName() : "Anonymous";
            preferredLanguage = translationService.getUserPreferredLanguage();
        }
    }
    
    private CompletableFuture<Boolean> setupUserPresence() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (currentUserId == null) {
            future.complete(false);
            return future;
        }
        
        DatabaseReference presenceRef = database.getReference(PRESENCE_PATH).child(currentUserId);
        
        Map<String, Object> presenceData = new HashMap<>();
        presenceData.put("online", true);
        presenceData.put("userName", currentUserName);
        presenceData.put("lastSeen", ServerValue.TIMESTAMP);
        presenceData.put("preferredLanguage", preferredLanguage);
        
        presenceRef.setValue(presenceData)
                .addOnSuccessListener(aVoid -> {
                    // Set up offline presence
                    presenceRef.child("online").onDisconnect().setValue(false);
                    presenceRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
                    
                    future.complete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to setup user presence", e);
                    future.complete(false);
                });
        
        return future;
    }
    
    private void setupChannelListener(String channelId) {
        DatabaseReference messagesRef = database.getReference(MESSAGES_PATH).child(channelId);
        
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                EnhancedChatMessage message = snapshot.getValue(EnhancedChatMessage.class);
                if (message != null) {
                    // Add to cache
                    List<EnhancedChatMessage> messages = messageCache.computeIfAbsent(channelId, k -> new ArrayList<>());
                    messages.add(message);
                    
                    // Limit cache size
                    if (messages.size() > MESSAGE_HISTORY_LIMIT) {
                        messages.remove(0);
                    }
                    
                    // Notify listener
                    if (chatEventListener != null && !message.getSenderId().equals(currentUserId)) {
                        chatEventListener.onMessageReceived(message);
                    }
                }
            }
            
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Handle message updates (e.g., translation added)
            }
            
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle message deletion
            }
            
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Not used for messages
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Channel listener cancelled: " + error.getMessage());
                if (chatEventListener != null) {
                    chatEventListener.onChatError("Connection lost: " + error.getMessage());
                }
            }
        };
        
        messagesRef.limitToLast(MESSAGE_HISTORY_LIMIT).addChildEventListener(listener);
        channelListeners.put(channelId, listener);
    }
    
    private CompletableFuture<Boolean> updateChannelPresence(String channelId, boolean isJoining) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        DatabaseReference channelPresenceRef = database.getReference(CHANNELS_PATH)
                .child(channelId)
                .child("participants")
                .child(currentUserId);
        
        if (isJoining) {
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("userName", currentUserName);
            participantData.put("joinTime", ServerValue.TIMESTAMP);
            participantData.put("preferredLanguage", preferredLanguage);
            
            channelPresenceRef.setValue(participantData)
                    .addOnSuccessListener(aVoid -> future.complete(true))
                    .addOnFailureListener(e -> future.complete(false));
        } else {
            channelPresenceRef.removeValue()
                    .addOnSuccessListener(aVoid -> future.complete(true))
                    .addOnFailureListener(e -> future.complete(false));
        }
        
        return future;
    }
    
    private void loadRecentMessages(String channelId) {
        database.getReference(MESSAGES_PATH).child(channelId)
                .limitToLast(MESSAGE_HISTORY_LIMIT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<EnhancedChatMessage> messages = new ArrayList<>();
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            EnhancedChatMessage message = messageSnapshot.getValue(EnhancedChatMessage.class);
                            if (message != null) {
                                messages.add(message);
                            }
                        }
                        messageCache.put(channelId, messages);
                        Log.d(TAG, "Loaded " + messages.size() + " recent messages for channel: " + channelId);
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load recent messages", error.toException());
                    }
                });
    }
    
    private void updateSuggestionsForChannel(String channelId) {
        // Update suggestions based on channel type
        ChatSuggestionEngine.GameContext context = ChatSuggestionEngine.GameContext.EXPLORING;
        
        if (channelId.contains("team")) {
            context = ChatSuggestionEngine.GameContext.TEAM_FORMATION;
        } else if (channelId.contains("help")) {
            context = ChatSuggestionEngine.GameContext.HELP_NEEDED;
        }
        
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("channelId", channelId);
        
        suggestionEngine.updateGameContext(context, contextData);
    }
    
    private void addGameContextToMessage(EnhancedChatMessage message) {
        if (currentLocation != null) {
            EnhancedChatMessage.GameContext gameContext = new EnhancedChatMessage.GameContext(
                    "lat:" + currentLocation.getLatitude() + ",lng:" + currentLocation.getLongitude(),
                    "treasure_hunting",
                    new ArrayList<>(), // nearby players would be populated from game state
                    null // treasure context would be populated if relevant
            );
            message.setGameContext(gameContext);
        }
    }
    
    private String getGameContextForChannel(String channelId) {
        if (channelId.contains("global")) {
            return "global_chat";
        } else if (channelId.contains("team")) {
            return "team_chat";
        } else if (channelId.contains("help")) {
            return "help_request";
        }
        return "treasure_hunt";
    }
    
    private CompletableFuture<Boolean> processMessageTranslation(EnhancedChatMessage message) {
        return translationService.detectLanguage(message.getOriginalText())
                .thenCompose(detection -> {
                    message.setDetectedLanguage(detection.languageCode);
                    message.setLanguageConfidence(detection.confidence);
                    
                    // Pre-translate to common languages for better user experience
                    List<String> commonLanguages = List.of("en", "es", "fr", "de", "zh");
                    List<CompletableFuture<TranslationService.TranslationResult>> translationFutures = new ArrayList<>();
                    
                    for (String lang : commonLanguages) {
                        if (!lang.equals(detection.languageCode)) {
                            translationFutures.add(translationService.translateText(
                                    message.getOriginalText(), detection.languageCode, lang));
                        }
                    }
                    
                    return CompletableFuture.allOf(translationFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                for (CompletableFuture<TranslationService.TranslationResult> future : translationFutures) {
                                    try {
                                        TranslationService.TranslationResult result = future.get();
                                        if (result.translatedText != null && !result.translatedText.equals(message.getOriginalText())) {
                                            message.addTranslation(result.targetLanguage, result.translatedText);
                                        }
                                    } catch (Exception e) {
                                        Log.w(TAG, "Translation failed for one language", e);
                                    }
                                }
                                return true;
                            });
                });
    }
    
    private void sendMessageToFirebase(EnhancedChatMessage message, CompletableFuture<Boolean> future) {
        DatabaseReference messageRef = database.getReference(MESSAGES_PATH)
                .child(message.getChannelId())
                .push();
        
        message.setMessageId(messageRef.getKey());
        
        messageRef.setValue(message.toFirebaseMap())
                .addOnSuccessListener(aVoid -> {
                    message.setStatus(EnhancedChatMessage.MessageStatus.SENT);
                    
                    if (chatEventListener != null) {
                        chatEventListener.onMessageSent(message);
                    }
                    
                    Log.d(TAG, "Message sent successfully: " + message.getMessageId());
                    future.complete(true);
                })
                .addOnFailureListener(e -> {
                    message.setStatus(EnhancedChatMessage.MessageStatus.FAILED);
                    
                    Log.e(TAG, "Failed to send message", e);
                    if (chatEventListener != null) {
                        chatEventListener.onChatError("Failed to send message: " + e.getMessage());
                    }
                    
                    future.complete(false);
                });
    }
    
    /**
     * User presence data class
     */
    public static class UserPresence {
        public boolean online;
        public String userName;
        public long lastSeen;
        public String preferredLanguage;
        
        public UserPresence() {} // Required for Firebase
        
        public UserPresence(boolean online, String userName, long lastSeen, String preferredLanguage) {
            this.online = online;
            this.userName = userName;
            this.lastSeen = lastSeen;
            this.preferredLanguage = preferredLanguage;
        }
    }
}
