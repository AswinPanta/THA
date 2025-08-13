package com.treasurehuntadventure.tha.challenges;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive player behavior analytics engine for ML model training and personalized challenge generation
 */
public class PlayerBehaviorAnalyzer {
    private static final String TAG = "PlayerBehaviorAnalyzer";
    
    // Firestore collections
    private static final String BEHAVIOR_ANALYTICS_COLLECTION = "player_behavior_analytics";
    private static final String SESSION_DATA_COLLECTION = "session_data";
    private static final String PREFERENCE_PATTERNS_COLLECTION = "preference_patterns";
    
    // Behavior tracking categories
    public enum BehaviorCategory {
        EXPLORATION("exploration"),
        TREASURE_HUNTING("treasure_hunting"),
        SOCIAL_INTERACTION("social_interaction"),
        CHALLENGE_COMPLETION("challenge_completion"),
        TIME_PATTERNS("time_patterns"),
        LOCATION_PATTERNS("location_patterns"),
        DIFFICULTY_PREFERENCE("difficulty_preference"),
        REWARD_MOTIVATION("reward_motivation");
        
        private final String value;
        
        BehaviorCategory(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    // Player behavior profile for ML training
    public static class PlayerBehaviorProfile {
        private String playerId;
        private long lastUpdated;
        
        // Gameplay patterns
        private GameplayPatterns gameplayPatterns;
        private PreferenceProfile preferenceProfile;
        private PerformanceMetrics performanceMetrics;
        private EngagementMetrics engagementMetrics;
        private SocialBehavior socialBehavior;
        
        // ML training features
        private Map<String, Double> featureVector;
        private List<String> behaviorTags;
        private double engagementScore;
        private double skillLevel;
        private double challengePreference;
        
        public PlayerBehaviorProfile() {
            this.gameplayPatterns = new GameplayPatterns();
            this.preferenceProfile = new PreferenceProfile();
            this.performanceMetrics = new PerformanceMetrics();
            this.engagementMetrics = new EngagementMetrics();
            this.socialBehavior = new SocialBehavior();
            this.featureVector = new HashMap<>();
            this.behaviorTags = new ArrayList<>();
        }
        
        // Getters and setters
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public GameplayPatterns getGameplayPatterns() { return gameplayPatterns; }
        public void setGameplayPatterns(GameplayPatterns gameplayPatterns) { this.gameplayPatterns = gameplayPatterns; }
        
        public PreferenceProfile getPreferenceProfile() { return preferenceProfile; }
        public void setPreferenceProfile(PreferenceProfile preferenceProfile) { this.preferenceProfile = preferenceProfile; }
        
        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }
        
        public EngagementMetrics getEngagementMetrics() { return engagementMetrics; }
        public void setEngagementMetrics(EngagementMetrics engagementMetrics) { this.engagementMetrics = engagementMetrics; }
        
        public SocialBehavior getSocialBehavior() { return socialBehavior; }
        public void setSocialBehavior(SocialBehavior socialBehavior) { this.socialBehavior = socialBehavior; }
        
        public Map<String, Double> getFeatureVector() { return featureVector; }
        public void setFeatureVector(Map<String, Double> featureVector) { this.featureVector = featureVector; }
        
        public List<String> getBehaviorTags() { return behaviorTags; }
        public void setBehaviorTags(List<String> behaviorTags) { this.behaviorTags = behaviorTags; }
        
        public double getEngagementScore() { return engagementScore; }
        public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }
        
        public double getSkillLevel() { return skillLevel; }
        public void setSkillLevel(double skillLevel) { this.skillLevel = skillLevel; }
        
        public double getChallengePreference() { return challengePreference; }
        public void setChallengePreference(double challengePreference) { this.challengePreference = challengePreference; }
    }
    
    // Gameplay patterns data class
    public static class GameplayPatterns {
        private Map<Integer, Double> hourlyActivity; // Hour of day -> activity level
        private Map<String, Integer> dayOfWeekActivity; // Day -> session count
        private double averageSessionDuration;
        private int sessionsPerWeek;
        private List<String> preferredGameModes;
        private Map<String, Double> locationPreferences; // Location type -> preference score
        private double explorationRadius;
        private int treasureHuntingFrequency;
        
        public GameplayPatterns() {
            this.hourlyActivity = new HashMap<>();
            this.dayOfWeekActivity = new HashMap<>();
            this.preferredGameModes = new ArrayList<>();
            this.locationPreferences = new HashMap<>();
        }
        
        // Getters and setters
        public Map<Integer, Double> getHourlyActivity() { return hourlyActivity; }
        public void setHourlyActivity(Map<Integer, Double> hourlyActivity) { this.hourlyActivity = hourlyActivity; }
        
        public Map<String, Integer> getDayOfWeekActivity() { return dayOfWeekActivity; }
        public void setDayOfWeekActivity(Map<String, Integer> dayOfWeekActivity) { this.dayOfWeekActivity = dayOfWeekActivity; }
        
        public double getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(double averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }
        
        public int getSessionsPerWeek() { return sessionsPerWeek; }
        public void setSessionsPerWeek(int sessionsPerWeek) { this.sessionsPerWeek = sessionsPerWeek; }
        
        public List<String> getPreferredGameModes() { return preferredGameModes; }
        public void setPreferredGameModes(List<String> preferredGameModes) { this.preferredGameModes = preferredGameModes; }
        
        public Map<String, Double> getLocationPreferences() { return locationPreferences; }
        public void setLocationPreferences(Map<String, Double> locationPreferences) { this.locationPreferences = locationPreferences; }
        
        public double getExplorationRadius() { return explorationRadius; }
        public void setExplorationRadius(double explorationRadius) { this.explorationRadius = explorationRadius; }
        
        public int getTreasureHuntingFrequency() { return treasureHuntingFrequency; }
        public void setTreasureHuntingFrequency(int treasureHuntingFrequency) { this.treasureHuntingFrequency = treasureHuntingFrequency; }
    }
    
    // Preference profile data class
    public static class PreferenceProfile {
        private Map<String, Double> challengeTypePreferences; // Challenge type -> preference score
        private Map<TreasureRarity, Double> rarityPreferences; // Rarity -> preference score
        private double difficultyPreference; // 0.0 (easy) to 1.0 (hard)
        private Map<String, Double> rewardTypePreferences; // Reward type -> preference score
        private double competitivenessLevel;
        private double explorationVsEfficiency; // 0.0 (efficiency) to 1.0 (exploration)
        private List<String> favoriteLocations;
        private double socialInteractionPreference;
        
        public PreferenceProfile() {
            this.challengeTypePreferences = new HashMap<>();
            this.rarityPreferences = new HashMap<>();
            this.rewardTypePreferences = new HashMap<>();
            this.favoriteLocations = new ArrayList<>();
        }
        
        // Getters and setters
        public Map<String, Double> getChallengeTypePreferences() { return challengeTypePreferences; }
        public void setChallengeTypePreferences(Map<String, Double> challengeTypePreferences) { this.challengeTypePreferences = challengeTypePreferences; }
        
        public Map<TreasureRarity, Double> getRarityPreferences() { return rarityPreferences; }
        public void setRarityPreferences(Map<TreasureRarity, Double> rarityPreferences) { this.rarityPreferences = rarityPreferences; }
        
        public double getDifficultyPreference() { return difficultyPreference; }
        public void setDifficultyPreference(double difficultyPreference) { this.difficultyPreference = difficultyPreference; }
        
        public Map<String, Double> getRewardTypePreferences() { return rewardTypePreferences; }
        public void setRewardTypePreferences(Map<String, Double> rewardTypePreferences) { this.rewardTypePreferences = rewardTypePreferences; }
        
        public double getCompetitivenessLevel() { return competitivenessLevel; }
        public void setCompetitivenessLevel(double competitivenessLevel) { this.competitivenessLevel = competitivenessLevel; }
        
        public double getExplorationVsEfficiency() { return explorationVsEfficiency; }
        public void setExplorationVsEfficiency(double explorationVsEfficiency) { this.explorationVsEfficiency = explorationVsEfficiency; }
        
        public List<String> getFavoriteLocations() { return favoriteLocations; }
        public void setFavoriteLocations(List<String> favoriteLocations) { this.favoriteLocations = favoriteLocations; }
        
        public double getSocialInteractionPreference() { return socialInteractionPreference; }
        public void setSocialInteractionPreference(double socialInteractionPreference) { this.socialInteractionPreference = socialInteractionPreference; }
    }
    
    // Performance metrics data class
    public static class PerformanceMetrics {
        private double challengeCompletionRate;
        private double averageCompletionTime;
        private Map<String, Double> challengeTypeSuccessRates; // Challenge type -> success rate
        private double treasureFindingEfficiency;
        private double accuracyScore;
        private int currentStreak;
        private int longestStreak;
        private double improvementRate; // Performance improvement over time
        private Map<String, Double> skillProgression; // Skill area -> progression rate
        
        public PerformanceMetrics() {
            this.challengeTypeSuccessRates = new HashMap<>();
            this.skillProgression = new HashMap<>();
        }
        
        // Getters and setters
        public double getChallengeCompletionRate() { return challengeCompletionRate; }
        public void setChallengeCompletionRate(double challengeCompletionRate) { this.challengeCompletionRate = challengeCompletionRate; }
        
        public double getAverageCompletionTime() { return averageCompletionTime; }
        public void setAverageCompletionTime(double averageCompletionTime) { this.averageCompletionTime = averageCompletionTime; }
        
        public Map<String, Double> getChallengeTypeSuccessRates() { return challengeTypeSuccessRates; }
        public void setChallengeTypeSuccessRates(Map<String, Double> challengeTypeSuccessRates) { this.challengeTypeSuccessRates = challengeTypeSuccessRates; }
        
        public double getTreasureFindingEfficiency() { return treasureFindingEfficiency; }
        public void setTreasureFindingEfficiency(double treasureFindingEfficiency) { this.treasureFindingEfficiency = treasureFindingEfficiency; }
        
        public double getAccuracyScore() { return accuracyScore; }
        public void setAccuracyScore(double accuracyScore) { this.accuracyScore = accuracyScore; }
        
        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
        
        public int getLongestStreak() { return longestStreak; }
        public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
        
        public double getImprovementRate() { return improvementRate; }
        public void setImprovementRate(double improvementRate) { this.improvementRate = improvementRate; }
        
        public Map<String, Double> getSkillProgression() { return skillProgression; }
        public void setSkillProgression(Map<String, Double> skillProgression) { this.skillProgression = skillProgression; }
    }
    
    // Engagement metrics data class
    public static class EngagementMetrics {
        private double dailyEngagementScore;
        private double weeklyEngagementScore;
        private double monthlyEngagementScore;
        private int consecutiveDaysPlayed;
        private double sessionRetentionRate;
        private double featureUsageScore;
        private Map<String, Double> activityEngagement; // Activity type -> engagement score
        private double churnRisk; // 0.0 (low risk) to 1.0 (high risk)
        private long lastActiveTime;
        
        public EngagementMetrics() {
            this.activityEngagement = new HashMap<>();
        }
        
        // Getters and setters
        public double getDailyEngagementScore() { return dailyEngagementScore; }
        public void setDailyEngagementScore(double dailyEngagementScore) { this.dailyEngagementScore = dailyEngagementScore; }
        
        public double getWeeklyEngagementScore() { return weeklyEngagementScore; }
        public void setWeeklyEngagementScore(double weeklyEngagementScore) { this.weeklyEngagementScore = weeklyEngagementScore; }
        
        public double getMonthlyEngagementScore() { return monthlyEngagementScore; }
        public void setMonthlyEngagementScore(double monthlyEngagementScore) { this.monthlyEngagementScore = monthlyEngagementScore; }
        
        public int getConsecutiveDaysPlayed() { return consecutiveDaysPlayed; }
        public void setConsecutiveDaysPlayed(int consecutiveDaysPlayed) { this.consecutiveDaysPlayed = consecutiveDaysPlayed; }
        
        public double getSessionRetentionRate() { return sessionRetentionRate; }
        public void setSessionRetentionRate(double sessionRetentionRate) { this.sessionRetentionRate = sessionRetentionRate; }
        
        public double getFeatureUsageScore() { return featureUsageScore; }
        public void setFeatureUsageScore(double featureUsageScore) { this.featureUsageScore = featureUsageScore; }
        
        public Map<String, Double> getActivityEngagement() { return activityEngagement; }
        public void setActivityEngagement(Map<String, Double> activityEngagement) { this.activityEngagement = activityEngagement; }
        
        public double getChurnRisk() { return churnRisk; }
        public void setChurnRisk(double churnRisk) { this.churnRisk = churnRisk; }
        
        public long getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(long lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    }
    
    // Social behavior data class
    public static class SocialBehavior {
        private double socialInteractionFrequency;
        private int friendsCount;
        private double teamParticipationRate;
        private int helpRequestsGiven;
        private int helpRequestsReceived;
        private double chatActivityLevel;
        private Map<String, Double> socialFeatureUsage; // Feature -> usage score
        private double leaderboardEngagement;
        private double communityContribution;
        
        public SocialBehavior() {
            this.socialFeatureUsage = new HashMap<>();
        }
        
        // Getters and setters
        public double getSocialInteractionFrequency() { return socialInteractionFrequency; }
        public void setSocialInteractionFrequency(double socialInteractionFrequency) { this.socialInteractionFrequency = socialInteractionFrequency; }
        
        public int getFriendsCount() { return friendsCount; }
        public void setFriendsCount(int friendsCount) { this.friendsCount = friendsCount; }
        
        public double getTeamParticipationRate() { return teamParticipationRate; }
        public void setTeamParticipationRate(double teamParticipationRate) { this.teamParticipationRate = teamParticipationRate; }
        
        public int getHelpRequestsGiven() { return helpRequestsGiven; }
        public void setHelpRequestsGiven(int helpRequestsGiven) { this.helpRequestsGiven = helpRequestsGiven; }
        
        public int getHelpRequestsReceived() { return helpRequestsReceived; }
        public void setHelpRequestsReceived(int helpRequestsReceived) { this.helpRequestsReceived = helpRequestsReceived; }
        
        public double getChatActivityLevel() { return chatActivityLevel; }
        public void setChatActivityLevel(double chatActivityLevel) { this.chatActivityLevel = chatActivityLevel; }
        
        public Map<String, Double> getSocialFeatureUsage() { return socialFeatureUsage; }
        public void setSocialFeatureUsage(Map<String, Double> socialFeatureUsage) { this.socialFeatureUsage = socialFeatureUsage; }
        
        public double getLeaderboardEngagement() { return leaderboardEngagement; }
        public void setLeaderboardEngagement(double leaderboardEngagement) { this.leaderboardEngagement = leaderboardEngagement; }
        
        public double getCommunityContribution() { return communityContribution; }
        public void setCommunityContribution(double communityContribution) { this.communityContribution = communityContribution; }
    }
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final Map<String, PlayerBehaviorProfile> behaviorCache = new ConcurrentHashMap<>();
    
    // Session tracking
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    
    // Behavior analysis listeners
    private BehaviorAnalysisListener analysisListener;
    
    public interface BehaviorAnalysisListener {
        void onBehaviorProfileUpdated(String playerId, PlayerBehaviorProfile profile);
        void onPatternDetected(String playerId, String pattern, double confidence);
        void onEngagementScoreChanged(String playerId, double oldScore, double newScore);
        void onChurnRiskDetected(String playerId, double riskLevel);
    }
    
    public PlayerBehaviorAnalyzer(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();

        Log.d(TAG, "Player behavior analyzer initialized");
    }

    /**
     * Records player behavior event for analysis
     */
    public CompletableFuture<Void> recordBehaviorEvent(String playerId, BehaviorCategory category,
                                                     Map<String, Object> eventData) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Create behavior event
        Map<String, Object> behaviorEvent = new HashMap<>();
        behaviorEvent.put("playerId", playerId);
        behaviorEvent.put("category", category.getValue());
        behaviorEvent.put("timestamp", System.currentTimeMillis());
        behaviorEvent.put("eventData", eventData);
        behaviorEvent.put("sessionId", getCurrentSessionId(playerId));

        // Store event in Firestore
        firestore.collection(BEHAVIOR_ANALYTICS_COLLECTION)
                .document(playerId)
                .collection("events")
                .add(behaviorEvent)
                .addOnSuccessListener(documentReference -> {
                    // Update behavior profile asynchronously
                    updateBehaviorProfile(playerId, category, eventData);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to record behavior event", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    /**
     * Records treasure hunting behavior
     */
    public CompletableFuture<Void> recordTreasureHuntingBehavior(String playerId, TreasureRarity rarity,
                                                               Location location, long huntDuration,
                                                               boolean successful) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("rarity", rarity.name());
        eventData.put("latitude", location.getLatitude());
        eventData.put("longitude", location.getLongitude());
        eventData.put("duration", huntDuration);
        eventData.put("successful", successful);
        eventData.put("timeOfDay", Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        eventData.put("dayOfWeek", Calendar.getInstance().get(Calendar.DAY_OF_WEEK));

        return recordBehaviorEvent(playerId, BehaviorCategory.TREASURE_HUNTING, eventData);
    }

    /**
     * Records challenge completion behavior
     */
    public CompletableFuture<Void> recordChallengeCompletionBehavior(String playerId, String challengeType,
                                                                   String difficulty, long completionTime,
                                                                   boolean completed, double accuracy) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("challengeType", challengeType);
        eventData.put("difficulty", difficulty);
        eventData.put("completionTime", completionTime);
        eventData.put("completed", completed);
        eventData.put("accuracy", accuracy);
        eventData.put("attempts", 1); // This could be tracked separately

        return recordBehaviorEvent(playerId, BehaviorCategory.CHALLENGE_COMPLETION, eventData);
    }

    /**
     * Records social interaction behavior
     */
    public CompletableFuture<Void> recordSocialInteractionBehavior(String playerId, String interactionType,
                                                                 Map<String, Object> interactionData) {
        Map<String, Object> eventData = new HashMap<>(interactionData);
        eventData.put("interactionType", interactionType);
        eventData.put("timestamp", System.currentTimeMillis());

        return recordBehaviorEvent(playerId, BehaviorCategory.SOCIAL_INTERACTION, eventData);
    }

    /**
     * Starts a new session for behavior tracking
     */
    public void startSession(String playerId, Location startLocation) {
        SessionData sessionData = new SessionData();
        sessionData.playerId = playerId;
        sessionData.sessionId = generateSessionId(playerId);
        sessionData.startTime = System.currentTimeMillis();
        sessionData.startLocation = startLocation;
        sessionData.events = new ArrayList<>();

        activeSessions.put(playerId, sessionData);

        // Record session start
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionStart", true);
        eventData.put("startLocation", Map.of("lat", startLocation.getLatitude(), "lng", startLocation.getLongitude()));

        recordBehaviorEvent(playerId, BehaviorCategory.TIME_PATTERNS, eventData);

        Log.d(TAG, "Started session for player: " + playerId);
    }

    /**
     * Ends current session and analyzes behavior patterns
     */
    public CompletableFuture<Void> endSession(String playerId, Location endLocation) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        SessionData sessionData = activeSessions.remove(playerId);
        if (sessionData == null) {
            future.complete(null);
            return future;
        }

        sessionData.endTime = System.currentTimeMillis();
        sessionData.endLocation = endLocation;
        sessionData.duration = sessionData.endTime - sessionData.startTime;

        // Record session end
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionEnd", true);
        eventData.put("duration", sessionData.duration);
        eventData.put("eventCount", sessionData.events.size());
        if (endLocation != null) {
            eventData.put("endLocation", Map.of("lat", endLocation.getLatitude(), "lng", endLocation.getLongitude()));
            if (sessionData.startLocation != null) {
                eventData.put("totalDistance", sessionData.startLocation.distanceTo(endLocation));
            }
        }

        recordBehaviorEvent(playerId, BehaviorCategory.TIME_PATTERNS, eventData)
                .thenRun(() -> {
                    // Analyze session patterns
                    analyzeSessionPatterns(playerId, sessionData);

                    // Store session data
                    storeSessionData(sessionData);

                    future.complete(null);
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });

        return future;
    }

    /**
     * Gets comprehensive behavior profile for a player
     */
    public CompletableFuture<PlayerBehaviorProfile> getBehaviorProfile(String playerId) {
        CompletableFuture<PlayerBehaviorProfile> future = new CompletableFuture<>();

        // Check cache first
        PlayerBehaviorProfile cached = behaviorCache.get(playerId);
        if (cached != null && System.currentTimeMillis() - cached.getLastUpdated() < 300000) { // 5 minutes
            future.complete(cached);
            return future;
        }

        // Load from Firestore
        firestore.collection(BEHAVIOR_ANALYTICS_COLLECTION)
                .document(playerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PlayerBehaviorProfile profile = documentSnapshot.toObject(PlayerBehaviorProfile.class);
                        if (profile != null) {
                            behaviorCache.put(playerId, profile);
                            future.complete(profile);
                        } else {
                            // Create new profile
                            PlayerBehaviorProfile newProfile = createNewBehaviorProfile(playerId);
                            future.complete(newProfile);
                        }
                    } else {
                        // Create new profile
                        PlayerBehaviorProfile newProfile = createNewBehaviorProfile(playerId);
                        future.complete(newProfile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load behavior profile", e);
                    // Return cached or new profile
                    PlayerBehaviorProfile profile = cached != null ? cached : createNewBehaviorProfile(playerId);
                    future.complete(profile);
                });

        return future;
    }

    /**
     * Generates ML feature vector for a player
     */
    public CompletableFuture<Map<String, Double>> generateFeatureVector(String playerId) {
        return getBehaviorProfile(playerId)
                .thenApply(profile -> {
                    Map<String, Double> features = new HashMap<>();

                    // Gameplay pattern features
                    GameplayPatterns gameplay = profile.getGameplayPatterns();
                    features.put("avg_session_duration", gameplay.getAverageSessionDuration());
                    features.put("sessions_per_week", (double) gameplay.getSessionsPerWeek());
                    features.put("exploration_radius", gameplay.getExplorationRadius());
                    features.put("treasure_hunting_frequency", (double) gameplay.getTreasureHuntingFrequency());

                    // Performance features
                    PerformanceMetrics performance = profile.getPerformanceMetrics();
                    features.put("challenge_completion_rate", performance.getChallengeCompletionRate());
                    features.put("avg_completion_time", performance.getAverageCompletionTime());
                    features.put("accuracy_score", performance.getAccuracyScore());
                    features.put("current_streak", (double) performance.getCurrentStreak());
                    features.put("improvement_rate", performance.getImprovementRate());

                    // Preference features
                    PreferenceProfile preferences = profile.getPreferenceProfile();
                    features.put("difficulty_preference", preferences.getDifficultyPreference());
                    features.put("competitiveness_level", preferences.getCompetitivenessLevel());
                    features.put("exploration_vs_efficiency", preferences.getExplorationVsEfficiency());
                    features.put("social_interaction_preference", preferences.getSocialInteractionPreference());

                    // Engagement features
                    EngagementMetrics engagement = profile.getEngagementMetrics();
                    features.put("daily_engagement_score", engagement.getDailyEngagementScore());
                    features.put("weekly_engagement_score", engagement.getWeeklyEngagementScore());
                    features.put("consecutive_days_played", (double) engagement.getConsecutiveDaysPlayed());
                    features.put("session_retention_rate", engagement.getSessionRetentionRate());
                    features.put("churn_risk", engagement.getChurnRisk());

                    // Social features
                    SocialBehavior social = profile.getSocialBehavior();
                    features.put("social_interaction_frequency", social.getSocialInteractionFrequency());
                    features.put("friends_count", (double) social.getFriendsCount());
                    features.put("team_participation_rate", social.getTeamParticipationRate());
                    features.put("chat_activity_level", social.getChatActivityLevel());
                    features.put("leaderboard_engagement", social.getLeaderboardEngagement());

                    // Time-based features
                    addTimeBasedFeatures(features, gameplay.getHourlyActivity(), gameplay.getDayOfWeekActivity());

                    return features;
                });
    }

    /**
     * Sets behavior analysis listener
     */
    public void setBehaviorAnalysisListener(BehaviorAnalysisListener listener) {
        this.analysisListener = listener;
    }

    /**
     * Clears behavior cache
     */
    public void clearCache() {
        behaviorCache.clear();
    }

    // Private helper methods
    private void updateBehaviorProfile(String playerId, BehaviorCategory category, Map<String, Object> eventData) {
        getBehaviorProfile(playerId)
                .thenAccept(profile -> {
                    // Update profile based on category
                    switch (category) {
                        case TREASURE_HUNTING:
                            updateTreasureHuntingProfile(profile, eventData);
                            break;
                        case CHALLENGE_COMPLETION:
                            updateChallengeCompletionProfile(profile, eventData);
                            break;
                        case SOCIAL_INTERACTION:
                            updateSocialInteractionProfile(profile, eventData);
                            break;
                        case TIME_PATTERNS:
                            updateTimePatternProfile(profile, eventData);
                            break;
                        case LOCATION_PATTERNS:
                            updateLocationPatternProfile(profile, eventData);
                            break;
                    }

                    // Recalculate derived metrics
                    recalculateDerivedMetrics(profile);

                    // Update cache and Firestore
                    profile.setLastUpdated(System.currentTimeMillis());
                    behaviorCache.put(playerId, profile);
                    saveBehaviorProfile(profile);

                    // Notify listener
                    if (analysisListener != null) {
                        analysisListener.onBehaviorProfileUpdated(playerId, profile);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to update behavior profile", throwable);
                    return null;
                });
    }

    private void updateTreasureHuntingProfile(PlayerBehaviorProfile profile, Map<String, Object> eventData) {
        GameplayPatterns gameplay = profile.getGameplayPatterns();
        PerformanceMetrics performance = profile.getPerformanceMetrics();

        // Update treasure hunting frequency
        gameplay.setTreasureHuntingFrequency(gameplay.getTreasureHuntingFrequency() + 1);

        // Update success rate if successful
        Boolean successful = (Boolean) eventData.get("successful");
        if (successful != null && successful) {
            // Update treasure finding efficiency
            Long duration = (Long) eventData.get("duration");
            if (duration != null) {
                double currentEfficiency = performance.getTreasureFindingEfficiency();
                double newEfficiency = calculateNewEfficiency(currentEfficiency, duration);
                performance.setTreasureFindingEfficiency(newEfficiency);
            }
        }

        // Update rarity preferences
        String rarity = (String) eventData.get("rarity");
        if (rarity != null) {
            PreferenceProfile preferences = profile.getPreferenceProfile();
            Map<TreasureRarity, Double> rarityPrefs = preferences.getRarityPreferences();
            TreasureRarity treasureRarity = TreasureRarity.valueOf(rarity);
            rarityPrefs.put(treasureRarity, rarityPrefs.getOrDefault(treasureRarity, 0.0) + 0.1);
        }
    }

    private void updateChallengeCompletionProfile(PlayerBehaviorProfile profile, Map<String, Object> eventData) {
        PerformanceMetrics performance = profile.getPerformanceMetrics();
        PreferenceProfile preferences = profile.getPreferenceProfile();

        String challengeType = (String) eventData.get("challengeType");
        Boolean completed = (Boolean) eventData.get("completed");
        Long completionTime = (Long) eventData.get("completionTime");
        Double accuracy = (Double) eventData.get("accuracy");

        if (challengeType != null && completed != null) {
            // Update challenge type success rates
            Map<String, Double> successRates = performance.getChallengeTypeSuccessRates();
            double currentRate = successRates.getOrDefault(challengeType, 0.0);
            double newRate = completed ? Math.min(1.0, currentRate + 0.05) : Math.max(0.0, currentRate - 0.02);
            successRates.put(challengeType, newRate);

            // Update challenge type preferences
            Map<String, Double> typePrefs = preferences.getChallengeTypePreferences();
            double currentPref = typePrefs.getOrDefault(challengeType, 0.5);
            double prefChange = completed ? 0.1 : -0.05;
            typePrefs.put(challengeType, Math.max(0.0, Math.min(1.0, currentPref + prefChange)));

            // Update overall completion rate
            double currentCompletionRate = performance.getChallengeCompletionRate();
            double newCompletionRate = (currentCompletionRate * 0.9) + (completed ? 0.1 : 0.0);
            performance.setChallengeCompletionRate(newCompletionRate);
        }

        if (completionTime != null) {
            // Update average completion time
            double currentAvg = performance.getAverageCompletionTime();
            double newAvg = (currentAvg * 0.9) + (completionTime * 0.1);
            performance.setAverageCompletionTime(newAvg);
        }

        if (accuracy != null) {
            // Update accuracy score
            double currentAccuracy = performance.getAccuracyScore();
            double newAccuracy = (currentAccuracy * 0.9) + (accuracy * 0.1);
            performance.setAccuracyScore(newAccuracy);
        }
    }

    private void updateSocialInteractionProfile(PlayerBehaviorProfile profile, Map<String, Object> eventData) {
        SocialBehavior social = profile.getSocialBehavior();

        String interactionType = (String) eventData.get("interactionType");
        if (interactionType != null) {
            // Update social interaction frequency
            social.setSocialInteractionFrequency(social.getSocialInteractionFrequency() + 0.1);

            // Update specific interaction types
            switch (interactionType) {
                case "chat_message":
                    social.setChatActivityLevel(social.getChatActivityLevel() + 0.1);
                    break;
                case "help_request_given":
                    social.setHelpRequestsGiven(social.getHelpRequestsGiven() + 1);
                    break;
                case "help_request_received":
                    social.setHelpRequestsReceived(social.getHelpRequestsReceived() + 1);
                    break;
                case "team_join":
                    social.setTeamParticipationRate(Math.min(1.0, social.getTeamParticipationRate() + 0.1));
                    break;
            }
        }
    }

    private void updateTimePatternProfile(PlayerBehaviorProfile profile, Map<String, Object> eventData) {
        GameplayPatterns gameplay = profile.getGameplayPatterns();

        // Update hourly activity patterns
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        Map<Integer, Double> hourlyActivity = gameplay.getHourlyActivity();
        hourlyActivity.put(currentHour, hourlyActivity.getOrDefault(currentHour, 0.0) + 1.0);

        // Update day of week patterns
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek);
        Map<String, Integer> dayActivity = gameplay.getDayOfWeekActivity();
        dayActivity.put(dayName, dayActivity.getOrDefault(dayName, 0) + 1);

        // Update session duration if session end
        Boolean sessionEnd = (Boolean) eventData.get("sessionEnd");
        if (sessionEnd != null && sessionEnd) {
            Long duration = (Long) eventData.get("duration");
            if (duration != null) {
                double currentAvg = gameplay.getAverageSessionDuration();
                double newAvg = (currentAvg * 0.9) + (duration * 0.1);
                gameplay.setAverageSessionDuration(newAvg);
            }
        }
    }

    private void updateLocationPatternProfile(PlayerBehaviorProfile profile, Map<String, Object> eventData) {
        GameplayPatterns gameplay = profile.getGameplayPatterns();

        // Update location preferences based on successful activities
        Boolean successful = (Boolean) eventData.get("successful");
        String locationType = (String) eventData.get("locationType");

        if (successful != null && locationType != null) {
            Map<String, Double> locationPrefs = gameplay.getLocationPreferences();
            double currentPref = locationPrefs.getOrDefault(locationType, 0.5);
            double prefChange = successful ? 0.1 : -0.05;
            locationPrefs.put(locationType, Math.max(0.0, Math.min(1.0, currentPref + prefChange)));
        }
    }

    private void recalculateDerivedMetrics(PlayerBehaviorProfile profile) {
        // Calculate engagement score
        EngagementMetrics engagement = profile.getEngagementMetrics();
        double engagementScore = calculateEngagementScore(profile);
        engagement.setDailyEngagementScore(engagementScore);
        profile.setEngagementScore(engagementScore);

        // Calculate skill level
        double skillLevel = calculateSkillLevel(profile);
        profile.setSkillLevel(skillLevel);

        // Calculate challenge preference
        double challengePreference = calculateChallengePreference(profile);
        profile.setChallengePreference(challengePreference);

        // Calculate churn risk
        double churnRisk = calculateChurnRisk(profile);
        engagement.setChurnRisk(churnRisk);

        // Update behavior tags
        List<String> behaviorTags = generateBehaviorTags(profile);
        profile.setBehaviorTags(behaviorTags);
    }

    private double calculateEngagementScore(PlayerBehaviorProfile profile) {
        GameplayPatterns gameplay = profile.getGameplayPatterns();
        PerformanceMetrics performance = profile.getPerformanceMetrics();
        SocialBehavior social = profile.getSocialBehavior();

        double score = 0.0;
        score += Math.min(1.0, gameplay.getSessionsPerWeek() / 7.0) * 0.3; // Session frequency
        score += Math.min(1.0, gameplay.getAverageSessionDuration() / 3600000.0) * 0.2; // Session duration
        score += performance.getChallengeCompletionRate() * 0.3; // Challenge completion
        score += Math.min(1.0, social.getSocialInteractionFrequency() / 10.0) * 0.2; // Social engagement

        return Math.max(0.0, Math.min(1.0, score));
    }

    private double calculateSkillLevel(PlayerBehaviorProfile profile) {
        PerformanceMetrics performance = profile.getPerformanceMetrics();

        double skillLevel = 0.0;
        skillLevel += performance.getChallengeCompletionRate() * 0.4;
        skillLevel += performance.getAccuracyScore() * 0.3;
        skillLevel += performance.getTreasureFindingEfficiency() * 0.2;
        skillLevel += Math.min(1.0, performance.getCurrentStreak() / 10.0) * 0.1;

        return Math.max(0.0, Math.min(1.0, skillLevel));
    }

    private double calculateChallengePreference(PlayerBehaviorProfile profile) {
        PreferenceProfile preferences = profile.getPreferenceProfile();
        return preferences.getDifficultyPreference();
    }

    private double calculateChurnRisk(PlayerBehaviorProfile profile) {
        EngagementMetrics engagement = profile.getEngagementMetrics();
        long timeSinceLastActive = System.currentTimeMillis() - engagement.getLastActiveTime();

        double riskScore = 0.0;

        // Time since last active (higher risk if longer)
        if (timeSinceLastActive > 7 * 24 * 60 * 60 * 1000) { // 7 days
            riskScore += 0.4;
        } else if (timeSinceLastActive > 3 * 24 * 60 * 60 * 1000) { // 3 days
            riskScore += 0.2;
        }

        // Low engagement score
        if (engagement.getDailyEngagementScore() < 0.3) {
            riskScore += 0.3;
        }

        // Low session retention
        if (engagement.getSessionRetentionRate() < 0.5) {
            riskScore += 0.2;
        }

        // Declining consecutive days
        if (engagement.getConsecutiveDaysPlayed() < 2) {
            riskScore += 0.1;
        }

        return Math.max(0.0, Math.min(1.0, riskScore));
    }

    private List<String> generateBehaviorTags(PlayerBehaviorProfile profile) {
        List<String> tags = new ArrayList<>();

        // Engagement tags
        if (profile.getEngagementScore() > 0.8) {
            tags.add("highly_engaged");
        } else if (profile.getEngagementScore() < 0.3) {
            tags.add("low_engagement");
        }

        // Skill tags
        if (profile.getSkillLevel() > 0.8) {
            tags.add("expert_player");
        } else if (profile.getSkillLevel() < 0.3) {
            tags.add("beginner_player");
        }

        // Social tags
        SocialBehavior social = profile.getSocialBehavior();
        if (social.getSocialInteractionFrequency() > 5.0) {
            tags.add("social_player");
        } else if (social.getSocialInteractionFrequency() < 1.0) {
            tags.add("solo_player");
        }

        // Challenge preference tags
        PreferenceProfile preferences = profile.getPreferenceProfile();
        if (preferences.getDifficultyPreference() > 0.7) {
            tags.add("challenge_seeker");
        } else if (preferences.getDifficultyPreference() < 0.3) {
            tags.add("casual_player");
        }

        // Exploration tags
        if (preferences.getExplorationVsEfficiency() > 0.7) {
            tags.add("explorer");
        } else if (preferences.getExplorationVsEfficiency() < 0.3) {
            tags.add("efficiency_focused");
        }

        return tags;
    }

    private PlayerBehaviorProfile createNewBehaviorProfile(String playerId) {
        PlayerBehaviorProfile profile = new PlayerBehaviorProfile();
        profile.setPlayerId(playerId);
        profile.setLastUpdated(System.currentTimeMillis());

        // Initialize with default values
        profile.setEngagementScore(0.5);
        profile.setSkillLevel(0.3);
        profile.setChallengePreference(0.5);

        return profile;
    }

    private void saveBehaviorProfile(PlayerBehaviorProfile profile) {
        firestore.collection(BEHAVIOR_ANALYTICS_COLLECTION)
                .document(profile.getPlayerId())
                .set(profile)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save behavior profile", e));
    }

    private void analyzeSessionPatterns(String playerId, SessionData sessionData) {
        // Analyze patterns and detect anomalies
        if (sessionData.duration < 60000) { // Very short session
            if (analysisListener != null) {
                analysisListener.onPatternDetected(playerId, "short_session", 0.8);
            }
        } else if (sessionData.duration > 7200000) { // Very long session
            if (analysisListener != null) {
                analysisListener.onPatternDetected(playerId, "long_session", 0.9);
            }
        }
    }

    private void storeSessionData(SessionData sessionData) {
        firestore.collection(SESSION_DATA_COLLECTION)
                .add(sessionData.toMap())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store session data", e));
    }

    private String getCurrentSessionId(String playerId) {
        SessionData session = activeSessions.get(playerId);
        return session != null ? session.sessionId : "no_session";
    }

    private String generateSessionId(String playerId) {
        return playerId + "_" + System.currentTimeMillis();
    }

    private double calculateNewEfficiency(double currentEfficiency, long duration) {
        // Simple efficiency calculation - lower duration = higher efficiency
        double newEfficiencyPoint = Math.max(0.0, 1.0 - (duration / 300000.0)); // 5 minutes = 0 efficiency
        return (currentEfficiency * 0.9) + (newEfficiencyPoint * 0.1);
    }

    private String getDayName(int dayOfWeek) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek - 1];
    }

    private void addTimeBasedFeatures(Map<String, Double> features, Map<Integer, Double> hourlyActivity,
                                    Map<String, Integer> dayActivity) {
        // Find peak activity hours
        int peakHour = hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12);
        features.put("peak_activity_hour", (double) peakHour);

        // Calculate activity distribution
        double morningActivity = hourlyActivity.getOrDefault(6, 0.0) + hourlyActivity.getOrDefault(7, 0.0) +
                               hourlyActivity.getOrDefault(8, 0.0) + hourlyActivity.getOrDefault(9, 0.0);
        double eveningActivity = hourlyActivity.getOrDefault(18, 0.0) + hourlyActivity.getOrDefault(19, 0.0) +
                               hourlyActivity.getOrDefault(20, 0.0) + hourlyActivity.getOrDefault(21, 0.0);

        features.put("morning_activity_ratio", morningActivity / Math.max(1.0, morningActivity + eveningActivity));
        features.put("evening_activity_ratio", eveningActivity / Math.max(1.0, morningActivity + eveningActivity));

        // Weekend vs weekday activity
        int weekendActivity = dayActivity.getOrDefault("Saturday", 0) + dayActivity.getOrDefault("Sunday", 0);
        int weekdayActivity = dayActivity.values().stream().mapToInt(Integer::intValue).sum() - weekendActivity;

        features.put("weekend_activity_ratio", (double) weekendActivity / Math.max(1, weekendActivity + weekdayActivity));
    }

    /**
     * Session data class for tracking player sessions
     */
    private static class SessionData {
        public String playerId;
        public String sessionId;
        public long startTime;
        public long endTime;
        public long duration;
        public Location startLocation;
        public Location endLocation;
        public List<Map<String, Object>> events;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("playerId", playerId);
            map.put("sessionId", sessionId);
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("duration", duration);
            if (startLocation != null) {
                map.put("startLocation", Map.of("lat", startLocation.getLatitude(), "lng", startLocation.getLongitude()));
            }
            if (endLocation != null) {
                map.put("endLocation", Map.of("lat", endLocation.getLatitude(), "lng", endLocation.getLongitude()));
            }
            map.put("eventCount", events.size());
            return map;
        }
    }
}
