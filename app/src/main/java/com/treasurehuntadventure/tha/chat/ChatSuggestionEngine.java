package com.treasurehuntadventure.tha.chat;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context-aware chat suggestion engine that provides relevant suggestions based on game state
 */
public class ChatSuggestionEngine {
    private static final String TAG = "ChatSuggestionEngine";
    
    // Suggestion categories
    public enum SuggestionCategory {
        GREETING("greeting"),
        TREASURE_HUNTING("treasure_hunting"),
        TEAM_COORDINATION("team_coordination"),
        HELP_REQUEST("help_request"),
        CELEBRATION("celebration"),
        LOCATION_SHARING("location_sharing"),
        GAME_TIPS("game_tips"),
        SOCIAL("social"),
        EMERGENCY("emergency");
        
        private final String value;
        
        SuggestionCategory(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    // Game context types
    public enum GameContext {
        EXPLORING("exploring"),
        TREASURE_NEARBY("treasure_nearby"),
        TREASURE_CAPTURED("treasure_captured"),
        TEAM_FORMATION("team_formation"),
        HELP_NEEDED("help_needed"),
        ACHIEVEMENT_UNLOCKED("achievement_unlocked"),
        LOCATION_DISCOVERY("location_discovery"),
        IDLE("idle");
        
        private final String value;
        
        GameContext(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    // Predefined suggestion templates
    private static final Map<SuggestionCategory, List<String>> SUGGESTION_TEMPLATES = new HashMap<>();
    
    static {
        SUGGESTION_TEMPLATES.put(SuggestionCategory.GREETING, Arrays.asList(
            "Ahoy, matey! 🏴‍☠️",
            "Ready for treasure hunting? ⚓",
            "Let's find some treasure! 💰",
            "Good luck on your adventure! 🗺️",
            "May the winds be at your back! ⛵"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.TREASURE_HUNTING, Arrays.asList(
            "I found a treasure nearby! 💎",
            "Anyone see treasures in this area? 🔍",
            "Rare treasure spotted! 🌟",
            "This location has good treasures! 📍",
            "Let's search together! 🤝"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.TEAM_COORDINATION, Arrays.asList(
            "Let's team up! 👥",
            "Meet me at my location 📍",
            "Follow me, I know a good spot! 🗺️",
            "Split up to cover more ground? 🔄",
            "Regroup at the landmark! 🏛️"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.HELP_REQUEST, Arrays.asList(
            "Can someone help me? 🆘",
            "I'm stuck, need assistance! 😅",
            "How do I capture this treasure? ❓",
            "Tips for finding rare treasures? 💡",
            "Anyone know this area well? 🗺️"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.CELEBRATION, Arrays.asList(
            "Treasure captured! 🎉",
            "Great job everyone! 👏",
            "We did it! 🥳",
            "Amazing teamwork! 🤝",
            "Victory is ours! 🏆"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.LOCATION_SHARING, Arrays.asList(
            "Check out this location! 📍",
            "Great treasure spot here! 🗺️",
            "Beautiful view from here! 🌅",
            "Historic landmark nearby! 🏛️",
            "Perfect meeting point! 📍"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.GAME_TIPS, Arrays.asList(
            "Pro tip: Check near landmarks! 💡",
            "Rare treasures spawn at dawn! 🌅",
            "Team up for better rewards! 🤝",
            "Use AR mode for better detection! 📱",
            "Save energy for long hunts! ⚡"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.SOCIAL, Arrays.asList(
            "How's everyone doing? 😊",
            "Beautiful day for treasure hunting! ☀️",
            "Anyone from around here? 🏠",
            "What's your best find today? 🏆",
            "Love this game! ❤️"
        ));
        
        SUGGESTION_TEMPLATES.put(SuggestionCategory.EMERGENCY, Arrays.asList(
            "Need help urgently! 🚨",
            "Lost, can someone guide me? 🆘",
            "Phone battery dying! 🔋",
            "Weather getting bad! ⛈️",
            "Heading back for safety! 🏠"
        ));
    }
    
    private final Context context;
    private final Random random;
    private final Map<String, UserSuggestionProfile> userProfiles;
    
    // Current game state
    private GameContext currentContext = GameContext.IDLE;
    private Location currentLocation;
    private List<String> nearbyPlayers = new ArrayList<>();
    private TreasureRarity lastTreasureRarity;
    private long lastActivityTime = System.currentTimeMillis();
    
    // Suggestion callbacks
    private SuggestionListener suggestionListener;
    
    public interface SuggestionListener {
        void onSuggestionsUpdated(List<ChatSuggestion> suggestions);
        void onSuggestionSelected(ChatSuggestion suggestion);
        void onContextChanged(GameContext newContext);
    }
    
    public ChatSuggestionEngine(Context context) {
        this.context = context;
        this.random = new Random();
        this.userProfiles = new ConcurrentHashMap<>();
        
        Log.d(TAG, "Chat suggestion engine initialized");
    }
    
    /**
     * Updates game context and generates new suggestions
     */
    public void updateGameContext(GameContext context, Map<String, Object> contextData) {
        this.currentContext = context;
        this.lastActivityTime = System.currentTimeMillis();
        
        // Extract context data
        if (contextData != null) {
            if (contextData.containsKey("location")) {
                this.currentLocation = (Location) contextData.get("location");
            }
            if (contextData.containsKey("nearbyPlayers")) {
                this.nearbyPlayers = (List<String>) contextData.get("nearbyPlayers");
            }
            if (contextData.containsKey("treasureRarity")) {
                this.lastTreasureRarity = (TreasureRarity) contextData.get("treasureRarity");
            }
        }
        
        // Generate new suggestions
        List<ChatSuggestion> suggestions = generateContextualSuggestions();
        
        if (suggestionListener != null) {
            suggestionListener.onContextChanged(context);
            suggestionListener.onSuggestionsUpdated(suggestions);
        }
        
        Log.d(TAG, "Game context updated: " + context + ", generated " + suggestions.size() + " suggestions");
    }
    
    /**
     * Gets suggestions for current context
     */
    public List<ChatSuggestion> getCurrentSuggestions() {
        return generateContextualSuggestions();
    }
    
    /**
     * Gets suggestions for specific category
     */
    public List<ChatSuggestion> getSuggestionsByCategory(SuggestionCategory category) {
        List<String> templates = SUGGESTION_TEMPLATES.get(category);
        if (templates == null) return new ArrayList<>();
        
        List<ChatSuggestion> suggestions = new ArrayList<>();
        for (String template : templates) {
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    template,
                    category,
                    1.0f,
                    System.currentTimeMillis()
            ));
        }
        
        return suggestions;
    }
    
    /**
     * Records suggestion usage for learning
     */
    public void recordSuggestionUsage(String userId, ChatSuggestion suggestion) {
        UserSuggestionProfile profile = userProfiles.computeIfAbsent(userId, 
                k -> new UserSuggestionProfile(userId));
        
        profile.recordUsage(suggestion);
        
        if (suggestionListener != null) {
            suggestionListener.onSuggestionSelected(suggestion);
        }
        
        Log.d(TAG, "Recorded suggestion usage: " + suggestion.getCategory() + " by " + userId);
    }
    
    /**
     * Gets personalized suggestions for user
     */
    public List<ChatSuggestion> getPersonalizedSuggestions(String userId) {
        UserSuggestionProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return getCurrentSuggestions();
        }
        
        List<ChatSuggestion> suggestions = generateContextualSuggestions();
        
        // Adjust suggestions based on user preferences
        for (ChatSuggestion suggestion : suggestions) {
            float personalizedScore = profile.getPreferenceScore(suggestion.getCategory());
            suggestion.setRelevanceScore(suggestion.getRelevanceScore() * personalizedScore);
        }
        
        // Sort by relevance score
        suggestions.sort((s1, s2) -> Float.compare(s2.getRelevanceScore(), s1.getRelevanceScore()));
        
        return suggestions;
    }
    
    /**
     * Sets suggestion listener
     */
    public void setSuggestionListener(SuggestionListener listener) {
        this.suggestionListener = listener;
    }
    
    /**
     * Gets suggestion usage analytics
     */
    public Map<String, Object> getSuggestionAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        // Category usage statistics
        Map<SuggestionCategory, Integer> categoryUsage = new HashMap<>();
        for (UserSuggestionProfile profile : userProfiles.values()) {
            for (Map.Entry<SuggestionCategory, Integer> entry : profile.getCategoryUsage().entrySet()) {
                categoryUsage.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        
        analytics.put("categoryUsage", categoryUsage);
        analytics.put("totalUsers", userProfiles.size());
        analytics.put("currentContext", currentContext);
        analytics.put("lastActivityTime", lastActivityTime);
        
        return analytics;
    }
    
    private List<ChatSuggestion> generateContextualSuggestions() {
        List<ChatSuggestion> suggestions = new ArrayList<>();
        
        // Add context-specific suggestions
        switch (currentContext) {
            case EXPLORING:
                suggestions.addAll(createSuggestions(SuggestionCategory.TREASURE_HUNTING, 3));
                suggestions.addAll(createSuggestions(SuggestionCategory.LOCATION_SHARING, 2));
                break;
                
            case TREASURE_NEARBY:
                suggestions.addAll(createSuggestions(SuggestionCategory.TREASURE_HUNTING, 4));
                suggestions.addAll(createSuggestions(SuggestionCategory.TEAM_COORDINATION, 2));
                break;
                
            case TREASURE_CAPTURED:
                suggestions.addAll(createSuggestions(SuggestionCategory.CELEBRATION, 3));
                suggestions.addAll(createSuggestions(SuggestionCategory.SOCIAL, 2));
                break;
                
            case TEAM_FORMATION:
                suggestions.addAll(createSuggestions(SuggestionCategory.TEAM_COORDINATION, 4));
                suggestions.addAll(createSuggestions(SuggestionCategory.GREETING, 2));
                break;
                
            case HELP_NEEDED:
                suggestions.addAll(createSuggestions(SuggestionCategory.HELP_REQUEST, 4));
                suggestions.addAll(createSuggestions(SuggestionCategory.EMERGENCY, 1));
                break;
                
            case ACHIEVEMENT_UNLOCKED:
                suggestions.addAll(createSuggestions(SuggestionCategory.CELEBRATION, 3));
                suggestions.addAll(createSuggestions(SuggestionCategory.SOCIAL, 2));
                break;
                
            case LOCATION_DISCOVERY:
                suggestions.addAll(createSuggestions(SuggestionCategory.LOCATION_SHARING, 3));
                suggestions.addAll(createSuggestions(SuggestionCategory.TREASURE_HUNTING, 2));
                break;
                
            case IDLE:
            default:
                suggestions.addAll(createSuggestions(SuggestionCategory.GREETING, 2));
                suggestions.addAll(createSuggestions(SuggestionCategory.SOCIAL, 2));
                suggestions.addAll(createSuggestions(SuggestionCategory.GAME_TIPS, 1));
                break;
        }
        
        // Add time-based suggestions
        addTimeBased Suggestions(suggestions);
        
        // Add location-based suggestions
        addLocationBasedSuggestions(suggestions);
        
        // Add player-based suggestions
        addPlayerBasedSuggestions(suggestions);
        
        return suggestions;
    }
    
    private List<ChatSuggestion> createSuggestions(SuggestionCategory category, int count) {
        List<String> templates = SUGGESTION_TEMPLATES.get(category);
        if (templates == null) return new ArrayList<>();
        
        List<ChatSuggestion> suggestions = new ArrayList<>();
        List<String> shuffledTemplates = new ArrayList<>(templates);
        java.util.Collections.shuffle(shuffledTemplates, random);
        
        for (int i = 0; i < Math.min(count, shuffledTemplates.size()); i++) {
            String template = shuffledTemplates.get(i);
            float relevanceScore = calculateRelevanceScore(category, template);
            
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    template,
                    category,
                    relevanceScore,
                    System.currentTimeMillis()
            ));
        }
        
        return suggestions;
    }
    
    private float calculateRelevanceScore(SuggestionCategory category, String template) {
        float score = 1.0f;
        
        // Adjust based on context
        switch (currentContext) {
            case TREASURE_NEARBY:
                if (category == SuggestionCategory.TREASURE_HUNTING) score *= 1.5f;
                break;
            case TREASURE_CAPTURED:
                if (category == SuggestionCategory.CELEBRATION) score *= 1.5f;
                break;
            case HELP_NEEDED:
                if (category == SuggestionCategory.HELP_REQUEST) score *= 1.5f;
                break;
        }
        
        // Adjust based on nearby players
        if (!nearbyPlayers.isEmpty()) {
            if (category == SuggestionCategory.TEAM_COORDINATION) score *= 1.3f;
            if (category == SuggestionCategory.GREETING) score *= 1.2f;
        }
        
        // Adjust based on treasure rarity
        if (lastTreasureRarity != null) {
            if (lastTreasureRarity == TreasureRarity.LEGENDARY || lastTreasureRarity == TreasureRarity.EPIC) {
                if (category == SuggestionCategory.CELEBRATION) score *= 1.4f;
            }
        }
        
        return Math.min(score, 2.0f); // Cap at 2.0
    }
    
    private void addTimeBasedSuggestions(List<ChatSuggestion> suggestions) {
        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        
        // If idle for more than 5 minutes, suggest social interactions
        if (timeSinceLastActivity > 5 * 60 * 1000) {
            suggestions.addAll(createSuggestions(SuggestionCategory.SOCIAL, 1));
        }
        
        // Time of day based suggestions
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    "Good morning, treasure hunters! 🌅",
                    SuggestionCategory.GREETING,
                    1.2f,
                    System.currentTimeMillis()
            ));
        } else if (hour > 18) {
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    "Evening treasure hunt! 🌙",
                    SuggestionCategory.GREETING,
                    1.2f,
                    System.currentTimeMillis()
            ));
        }
    }
    
    private void addLocationBasedSuggestions(List<ChatSuggestion> suggestions) {
        if (currentLocation != null) {
            // Add location-specific suggestions
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    "Great hunting spot here! 📍",
                    SuggestionCategory.LOCATION_SHARING,
                    1.1f,
                    System.currentTimeMillis()
            ));
        }
    }
    
    private void addPlayerBasedSuggestions(List<ChatSuggestion> suggestions) {
        if (nearbyPlayers.size() > 0) {
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    "Hey nearby players! 👋",
                    SuggestionCategory.GREETING,
                    1.3f,
                    System.currentTimeMillis()
            ));
        }
        
        if (nearbyPlayers.size() > 2) {
            suggestions.add(new ChatSuggestion(
                    generateSuggestionId(),
                    "Let's form a team! 👥",
                    SuggestionCategory.TEAM_COORDINATION,
                    1.4f,
                    System.currentTimeMillis()
            ));
        }
    }
    
    private String generateSuggestionId() {
        return "suggestion_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }
    
    /**
     * Chat suggestion data class
     */
    public static class ChatSuggestion {
        private final String suggestionId;
        private final String text;
        private final SuggestionCategory category;
        private float relevanceScore;
        private final long timestamp;
        
        public ChatSuggestion(String suggestionId, String text, SuggestionCategory category, 
                            float relevanceScore, long timestamp) {
            this.suggestionId = suggestionId;
            this.text = text;
            this.category = category;
            this.relevanceScore = relevanceScore;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public String getSuggestionId() { return suggestionId; }
        public String getText() { return text; }
        public SuggestionCategory getCategory() { return category; }
        public float getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(float relevanceScore) { this.relevanceScore = relevanceScore; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * User suggestion profile for personalization
     */
    private static class UserSuggestionProfile {
        private final String userId;
        private final Map<SuggestionCategory, Integer> categoryUsage;
        private final Map<SuggestionCategory, Long> lastUsageTime;
        
        public UserSuggestionProfile(String userId) {
            this.userId = userId;
            this.categoryUsage = new ConcurrentHashMap<>();
            this.lastUsageTime = new ConcurrentHashMap<>();
        }
        
        public void recordUsage(ChatSuggestion suggestion) {
            SuggestionCategory category = suggestion.getCategory();
            categoryUsage.merge(category, 1, Integer::sum);
            lastUsageTime.put(category, System.currentTimeMillis());
        }
        
        public float getPreferenceScore(SuggestionCategory category) {
            int usage = categoryUsage.getOrDefault(category, 0);
            if (usage == 0) return 1.0f;
            
            // Higher usage = higher preference
            return 1.0f + (usage * 0.1f);
        }
        
        public Map<SuggestionCategory, Integer> getCategoryUsage() {
            return new HashMap<>(categoryUsage);
        }
    }
}
