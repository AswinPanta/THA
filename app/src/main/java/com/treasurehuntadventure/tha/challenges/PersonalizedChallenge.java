package com.treasurehuntadventure.tha.challenges;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced personalized challenge model with AI metadata and adaptive features
 */
@IgnoreExtraProperties
public class PersonalizedChallenge {
    
    // Basic challenge information
    private String challengeId;
    private String playerId;
    private String title;
    private String description;
    private String type; // ChallengeType
    private String difficulty; // DifficultyLevel
    
    // AI-generated metadata
    private double difficultyScore; // 0.0 to 1.0
    private double predictedEngagement; // 0.0 to 1.0
    private double personalizationScore; // 0.0 to 1.0
    private List<String> aiTags; // AI-generated tags for categorization
    private Map<String, Double> confidenceScores; // AI model confidence scores
    
    // Challenge parameters
    private Map<String, Object> parameters; // Dynamic challenge parameters
    private Map<String, Object> constraints; // Challenge constraints and limits
    private List<String> prerequisites; // Required conditions
    private Map<String, Object> context; // Location, time, social context
    
    // Progress tracking
    private ChallengeProgress progress;
    private List<Milestone> milestones;
    private boolean completed;
    private boolean failed;
    private long startTime;
    private long endTime;
    private long timeLimit; // in milliseconds
    
    // Rewards and incentives
    private ChallengeRewards rewards;
    private List<String> bonusConditions; // Conditions for bonus rewards
    private boolean bonusEarned;
    
    // Adaptive features
    private boolean adaptive; // Can difficulty be adjusted dynamically
    private String parentChallengeId; // For adaptive challenges
    private List<String> adaptationHistory; // History of difficulty adjustments
    private double currentPerformance; // Real-time performance score
    
    // Social features
    private boolean multiplayer;
    private List<String> participantIds;
    private String teamId;
    private boolean competitive;
    private Map<String, Object> socialContext;
    
    // Analytics and feedback
    private double satisfactionScore; // Player satisfaction (0.0 to 1.0)
    private double completionScore; // How well the challenge was completed
    private Map<String, Object> analyticsData;
    private List<String> playerFeedback;
    
    // Metadata
    private long createdAt;
    private long updatedAt;
    private String createdBy; // "AI" or "MANUAL"
    private String version;
    private Map<String, Object> metadata;
    
    /**
     * Challenge progress tracking
     */
    public static class ChallengeProgress {
        private double completionPercentage; // 0.0 to 1.0
        private Map<String, Object> currentValues; // Current progress values
        private Map<String, Object> targetValues; // Target values to achieve
        private List<String> completedSteps;
        private String currentStep;
        private long lastUpdated;
        private Map<String, Object> progressData;
        
        public ChallengeProgress() {
            this.currentValues = new HashMap<>();
            this.targetValues = new HashMap<>();
            this.completedSteps = new ArrayList<>();
            this.progressData = new HashMap<>();
        }
        
        // Getters and setters
        public double getCompletionPercentage() { return completionPercentage; }
        public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }
        
        public Map<String, Object> getCurrentValues() { return currentValues; }
        public void setCurrentValues(Map<String, Object> currentValues) { this.currentValues = currentValues; }
        
        public Map<String, Object> getTargetValues() { return targetValues; }
        public void setTargetValues(Map<String, Object> targetValues) { this.targetValues = targetValues; }
        
        public List<String> getCompletedSteps() { return completedSteps; }
        public void setCompletedSteps(List<String> completedSteps) { this.completedSteps = completedSteps; }
        
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public Map<String, Object> getProgressData() { return progressData; }
        public void setProgressData(Map<String, Object> progressData) { this.progressData = progressData; }
    }
    
    /**
     * Challenge milestone
     */
    public static class Milestone {
        private String milestoneId;
        private String name;
        private String description;
        private double targetValue;
        private boolean completed;
        private long completedAt;
        private Map<String, Object> rewards;
        
        public Milestone() {
            this.rewards = new HashMap<>();
        }
        
        public Milestone(String milestoneId, String name, String description, double targetValue) {
            this();
            this.milestoneId = milestoneId;
            this.name = name;
            this.description = description;
            this.targetValue = targetValue;
        }
        
        // Getters and setters
        public String getMilestoneId() { return milestoneId; }
        public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getTargetValue() { return targetValue; }
        public void setTargetValue(double targetValue) { this.targetValue = targetValue; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
        
        public Map<String, Object> getRewards() { return rewards; }
        public void setRewards(Map<String, Object> rewards) { this.rewards = rewards; }
    }
    
    /**
     * Challenge rewards
     */
    public static class ChallengeRewards {
        private int baseExperience;
        private int baseCoins;
        private List<String> items;
        private List<String> badges;
        private Map<String, Integer> bonusRewards;
        private double rewardMultiplier;
        private boolean hasRareReward;
        private String specialReward;
        
        public ChallengeRewards() {
            this.items = new ArrayList<>();
            this.badges = new ArrayList<>();
            this.bonusRewards = new HashMap<>();
            this.rewardMultiplier = 1.0;
        }
        
        // Getters and setters
        public int getBaseExperience() { return baseExperience; }
        public void setBaseExperience(int baseExperience) { this.baseExperience = baseExperience; }
        
        public int getBaseCoins() { return baseCoins; }
        public void setBaseCoins(int baseCoins) { this.baseCoins = baseCoins; }
        
        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }
        
        public List<String> getBadges() { return badges; }
        public void setBadges(List<String> badges) { this.badges = badges; }
        
        public Map<String, Integer> getBonusRewards() { return bonusRewards; }
        public void setBonusRewards(Map<String, Integer> bonusRewards) { this.bonusRewards = bonusRewards; }
        
        public double getRewardMultiplier() { return rewardMultiplier; }
        public void setRewardMultiplier(double rewardMultiplier) { this.rewardMultiplier = rewardMultiplier; }
        
        public boolean isHasRareReward() { return hasRareReward; }
        public void setHasRareReward(boolean hasRareReward) { this.hasRareReward = hasRareReward; }
        
        public String getSpecialReward() { return specialReward; }
        public void setSpecialReward(String specialReward) { this.specialReward = specialReward; }
    }
    
    // Constructors
    public PersonalizedChallenge() {
        this.aiTags = new ArrayList<>();
        this.confidenceScores = new HashMap<>();
        this.parameters = new HashMap<>();
        this.constraints = new HashMap<>();
        this.prerequisites = new ArrayList<>();
        this.context = new HashMap<>();
        this.progress = new ChallengeProgress();
        this.milestones = new ArrayList<>();
        this.rewards = new ChallengeRewards();
        this.bonusConditions = new ArrayList<>();
        this.adaptationHistory = new ArrayList<>();
        this.participantIds = new ArrayList<>();
        this.socialContext = new HashMap<>();
        this.analyticsData = new HashMap<>();
        this.playerFeedback = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.createdBy = "AI";
        this.version = "1.0";
    }
    
    public PersonalizedChallenge(String playerId, String title, String description, String type, String difficulty) {
        this();
        this.challengeId = generateChallengeId();
        this.playerId = playerId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.difficulty = difficulty;
    }
    
    // Utility methods
    @Exclude
    public boolean isActive() {
        return !completed && !failed && (timeLimit == 0 || System.currentTimeMillis() < startTime + timeLimit);
    }
    
    @Exclude
    public boolean isExpired() {
        return timeLimit > 0 && System.currentTimeMillis() >= startTime + timeLimit;
    }
    
    @Exclude
    public long getRemainingTime() {
        if (timeLimit == 0) return Long.MAX_VALUE;
        return Math.max(0, (startTime + timeLimit) - System.currentTimeMillis());
    }
    
    @Exclude
    public double getTimeProgress() {
        if (timeLimit == 0) return 0.0;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0, (double) elapsed / timeLimit);
    }
    
    @Exclude
    public String getDifficultyEmoji() {
        switch (difficulty) {
            case "BEGINNER": return "🟢";
            case "NOVICE": return "🔵";
            case "INTERMEDIATE": return "🟡";
            case "ADVANCED": return "🟠";
            case "EXPERT": return "🔴";
            case "MASTER": return "⚫";
            default: return "⚪";
        }
    }
    
    @Exclude
    public String getTypeEmoji() {
        switch (type) {
            case "treasure_hunt": return "💰";
            case "exploration": return "🗺️";
            case "speed_challenge": return "⚡";
            case "accuracy_challenge": return "🎯";
            case "social_challenge": return "👥";
            case "collection_challenge": return "📦";
            case "distance_challenge": return "🚶";
            case "time_challenge": return "⏰";
            case "skill_challenge": return "🏆";
            case "creative_challenge": return "🎨";
            default: return "⭐";
        }
    }
    
    @Exclude
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("challengeId", challengeId);
        map.put("playerId", playerId);
        map.put("title", title);
        map.put("description", description);
        map.put("type", type);
        map.put("difficulty", difficulty);
        map.put("difficultyScore", difficultyScore);
        map.put("predictedEngagement", predictedEngagement);
        map.put("personalizationScore", personalizationScore);
        map.put("aiTags", aiTags);
        map.put("confidenceScores", confidenceScores);
        map.put("parameters", parameters);
        map.put("constraints", constraints);
        map.put("prerequisites", prerequisites);
        map.put("context", context);
        map.put("progress", progress);
        map.put("milestones", milestones);
        map.put("completed", completed);
        map.put("failed", failed);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("timeLimit", timeLimit);
        map.put("rewards", rewards);
        map.put("bonusConditions", bonusConditions);
        map.put("bonusEarned", bonusEarned);
        map.put("adaptive", adaptive);
        map.put("parentChallengeId", parentChallengeId);
        map.put("adaptationHistory", adaptationHistory);
        map.put("currentPerformance", currentPerformance);
        map.put("multiplayer", multiplayer);
        map.put("participantIds", participantIds);
        map.put("teamId", teamId);
        map.put("competitive", competitive);
        map.put("socialContext", socialContext);
        map.put("satisfactionScore", satisfactionScore);
        map.put("completionScore", completionScore);
        map.put("analyticsData", analyticsData);
        map.put("playerFeedback", playerFeedback);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("createdBy", createdBy);
        map.put("version", version);
        map.put("metadata", metadata);
        return map;
    }
    
    private String generateChallengeId() {
        return "challenge_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    // Getters and setters
    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public double getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(double difficultyScore) { this.difficultyScore = difficultyScore; }
    
    public double getPredictedEngagement() { return predictedEngagement; }
    public void setPredictedEngagement(double predictedEngagement) { this.predictedEngagement = predictedEngagement; }
    
    public double getPersonalizationScore() { return personalizationScore; }
    public void setPersonalizationScore(double personalizationScore) { this.personalizationScore = personalizationScore; }
    
    public List<String> getAiTags() { return aiTags; }
    public void setAiTags(List<String> aiTags) { this.aiTags = aiTags; }
    
    public Map<String, Double> getConfidenceScores() { return confidenceScores; }
    public void setConfidenceScores(Map<String, Double> confidenceScores) { this.confidenceScores = confidenceScores; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
    
    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    public ChallengeProgress getProgress() { return progress; }
    public void setProgress(ChallengeProgress progress) { this.progress = progress; }
    
    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public boolean isFailed() { return failed; }
    public void setFailed(boolean failed) { this.failed = failed; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public long getTimeLimit() { return timeLimit; }
    public void setTimeLimit(long timeLimit) { this.timeLimit = timeLimit; }
    
    public ChallengeRewards getRewards() { return rewards; }
    public void setRewards(ChallengeRewards rewards) { this.rewards = rewards; }
    
    public List<String> getBonusConditions() { return bonusConditions; }
    public void setBonusConditions(List<String> bonusConditions) { this.bonusConditions = bonusConditions; }
    
    public boolean isBonusEarned() { return bonusEarned; }
    public void setBonusEarned(boolean bonusEarned) { this.bonusEarned = bonusEarned; }
    
    public boolean isAdaptive() { return adaptive; }
    public void setAdaptive(boolean adaptive) { this.adaptive = adaptive; }
    
    public String getParentChallengeId() { return parentChallengeId; }
    public void setParentChallengeId(String parentChallengeId) { this.parentChallengeId = parentChallengeId; }
    
    public List<String> getAdaptationHistory() { return adaptationHistory; }
    public void setAdaptationHistory(List<String> adaptationHistory) { this.adaptationHistory = adaptationHistory; }
    
    public double getCurrentPerformance() { return currentPerformance; }
    public void setCurrentPerformance(double currentPerformance) { this.currentPerformance = currentPerformance; }
    
    public boolean isMultiplayer() { return multiplayer; }
    public void setMultiplayer(boolean multiplayer) { this.multiplayer = multiplayer; }
    
    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }
    
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    
    public boolean isCompetitive() { return competitive; }
    public void setCompetitive(boolean competitive) { this.competitive = competitive; }
    
    public Map<String, Object> getSocialContext() { return socialContext; }
    public void setSocialContext(Map<String, Object> socialContext) { this.socialContext = socialContext; }
    
    public double getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(double satisfactionScore) { this.satisfactionScore = satisfactionScore; }
    
    public double getCompletionScore() { return completionScore; }
    public void setCompletionScore(double completionScore) { this.completionScore = completionScore; }
    
    public Map<String, Object> getAnalyticsData() { return analyticsData; }
    public void setAnalyticsData(Map<String, Object> analyticsData) { this.analyticsData = analyticsData; }
    
    public List<String> getPlayerFeedback() { return playerFeedback; }
    public void setPlayerFeedback(List<String> playerFeedback) { this.playerFeedback = playerFeedback; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
