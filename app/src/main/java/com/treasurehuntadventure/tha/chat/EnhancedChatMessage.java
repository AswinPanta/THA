package com.treasurehuntadventure.tha.chat;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced chat message model with translation, moderation, and context awareness
 */
@IgnoreExtraProperties
public class EnhancedChatMessage {
    
    // Core message properties
    private String messageId;
    private String senderId;
    private String senderName;
    private String originalText;
    private String channelId;
    private long timestamp;
    private MessageType type;
    private MessageStatus status;
    
    // Translation properties
    private String detectedLanguage;
    private Map<String, String> translations; // language code -> translated text
    private float languageConfidence;
    
    // Moderation properties
    private ModerationResult moderationResult;
    private boolean isModerated;
    private List<String> flaggedReasons;
    private float toxicityScore;
    
    // Context awareness
    private GameContext gameContext;
    private Map<String, Object> metadata;
    
    // UI state (excluded from database)
    @Exclude
    private boolean isTranslated;
    @Exclude
    private String displayText;
    @Exclude
    private boolean isExpanded;
    
    // Message types
    public enum MessageType {
        TEXT("text"),
        SYSTEM("system"),
        GAME_EVENT("game_event"),
        TREASURE_ALERT("treasure_alert"),
        PLAYER_JOIN("player_join"),
        PLAYER_LEAVE("player_leave"),
        ACHIEVEMENT("achievement"),
        LOCATION_SHARE("location_share"),
        EMOJI_REACTION("emoji_reaction");
        
        private final String value;
        
        MessageType(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    // Message status
    public enum MessageStatus {
        SENDING("sending"),
        SENT("sent"),
        DELIVERED("delivered"),
        READ("read"),
        FAILED("failed"),
        MODERATED("moderated"),
        DELETED("deleted");
        
        private final String value;
        
        MessageStatus(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    // Moderation result
    public static class ModerationResult {
        private boolean isApproved;
        private float confidenceScore;
        private List<String> detectedIssues;
        private String moderationAction;
        private long moderationTimestamp;
        
        public ModerationResult() {} // Required for Firebase
        
        public ModerationResult(boolean isApproved, float confidenceScore, 
                              List<String> detectedIssues, String moderationAction) {
            this.isApproved = isApproved;
            this.confidenceScore = confidenceScore;
            this.detectedIssues = detectedIssues;
            this.moderationAction = moderationAction;
            this.moderationTimestamp = System.currentTimeMillis();
        }
        
        // Getters and setters
        public boolean isApproved() { return isApproved; }
        public void setApproved(boolean approved) { isApproved = approved; }
        
        public float getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(float confidenceScore) { this.confidenceScore = confidenceScore; }
        
        public List<String> getDetectedIssues() { return detectedIssues; }
        public void setDetectedIssues(List<String> detectedIssues) { this.detectedIssues = detectedIssues; }
        
        public String getModerationAction() { return moderationAction; }
        public void setModerationAction(String moderationAction) { this.moderationAction = moderationAction; }
        
        public long getModerationTimestamp() { return moderationTimestamp; }
        public void setModerationTimestamp(long moderationTimestamp) { this.moderationTimestamp = moderationTimestamp; }
    }
    
    // Game context for context-aware features
    public static class GameContext {
        private String currentLocation;
        private String currentActivity;
        private List<String> nearbyPlayers;
        private String treasureContext;
        private Map<String, Object> gameState;
        
        public GameContext() {} // Required for Firebase
        
        public GameContext(String currentLocation, String currentActivity, 
                         List<String> nearbyPlayers, String treasureContext) {
            this.currentLocation = currentLocation;
            this.currentActivity = currentActivity;
            this.nearbyPlayers = nearbyPlayers;
            this.treasureContext = treasureContext;
            this.gameState = new HashMap<>();
        }
        
        // Getters and setters
        public String getCurrentLocation() { return currentLocation; }
        public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
        
        public String getCurrentActivity() { return currentActivity; }
        public void setCurrentActivity(String currentActivity) { this.currentActivity = currentActivity; }
        
        public List<String> getNearbyPlayers() { return nearbyPlayers; }
        public void setNearbyPlayers(List<String> nearbyPlayers) { this.nearbyPlayers = nearbyPlayers; }
        
        public String getTreasureContext() { return treasureContext; }
        public void setTreasureContext(String treasureContext) { this.treasureContext = treasureContext; }
        
        public Map<String, Object> getGameState() { return gameState; }
        public void setGameState(Map<String, Object> gameState) { this.gameState = gameState; }
    }
    
    // Constructors
    public EnhancedChatMessage() {
        // Required for Firebase
        this.translations = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public EnhancedChatMessage(String senderId, String senderName, String originalText, 
                             String channelId, MessageType type) {
        this();
        this.messageId = generateMessageId();
        this.senderId = senderId;
        this.senderName = senderName;
        this.originalText = originalText;
        this.channelId = channelId;
        this.type = type;
        this.status = MessageStatus.SENDING;
        this.timestamp = System.currentTimeMillis();
        this.isModerated = false;
        this.displayText = originalText;
    }
    
    // Utility methods
    @Exclude
    public String getDisplayText() {
        if (isTranslated && translations != null && !translations.isEmpty()) {
            // Return translated text if available
            return displayText != null ? displayText : originalText;
        }
        return originalText;
    }
    
    @Exclude
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }
    
    @Exclude
    public boolean hasTranslation(String languageCode) {
        return translations != null && translations.containsKey(languageCode);
    }
    
    @Exclude
    public String getTranslation(String languageCode) {
        return translations != null ? translations.get(languageCode) : null;
    }
    
    public void addTranslation(String languageCode, String translatedText) {
        if (translations == null) {
            translations = new HashMap<>();
        }
        translations.put(languageCode, translatedText);
    }
    
    @Exclude
    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM || type == MessageType.GAME_EVENT;
    }
    
    @Exclude
    public boolean needsModeration() {
        return type == MessageType.TEXT && !isModerated;
    }
    
    @Exclude
    public boolean isSafeToDisplay() {
        return moderationResult == null || moderationResult.isApproved();
    }
    
    @Exclude
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("senderId", senderId);
        map.put("senderName", senderName);
        map.put("originalText", originalText);
        map.put("channelId", channelId);
        map.put("timestamp", ServerValue.TIMESTAMP);
        map.put("type", type.getValue());
        map.put("status", status.getValue());
        map.put("detectedLanguage", detectedLanguage);
        map.put("translations", translations);
        map.put("languageConfidence", languageConfidence);
        map.put("moderationResult", moderationResult);
        map.put("isModerated", isModerated);
        map.put("flaggedReasons", flaggedReasons);
        map.put("toxicityScore", toxicityScore);
        map.put("gameContext", gameContext);
        map.put("metadata", metadata);
        return map;
    }
    
    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    // Getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    
    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }
    
    public Map<String, String> getTranslations() { return translations; }
    public void setTranslations(Map<String, String> translations) { this.translations = translations; }
    
    public float getLanguageConfidence() { return languageConfidence; }
    public void setLanguageConfidence(float languageConfidence) { this.languageConfidence = languageConfidence; }
    
    public ModerationResult getModerationResult() { return moderationResult; }
    public void setModerationResult(ModerationResult moderationResult) { this.moderationResult = moderationResult; }
    
    public boolean isModerated() { return isModerated; }
    public void setModerated(boolean moderated) { isModerated = moderated; }
    
    public List<String> getFlaggedReasons() { return flaggedReasons; }
    public void setFlaggedReasons(List<String> flaggedReasons) { this.flaggedReasons = flaggedReasons; }
    
    public float getToxicityScore() { return toxicityScore; }
    public void setToxicityScore(float toxicityScore) { this.toxicityScore = toxicityScore; }
    
    public GameContext getGameContext() { return gameContext; }
    public void setGameContext(GameContext gameContext) { this.gameContext = gameContext; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    @Exclude
    public boolean isTranslated() { return isTranslated; }
    @Exclude
    public void setTranslated(boolean translated) { isTranslated = translated; }
    
    @Exclude
    public boolean isExpanded() { return isExpanded; }
    @Exclude
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}
