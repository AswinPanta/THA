package com.treasurehuntadventure.tha.challenges;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.treasurehuntadventure.tha.TreasureRarity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * AI-powered challenge generator using machine learning models to create personalized challenges
 */
public class AIChallengeGenerator {
    private static final String TAG = "AIChallengeGenerator";
    
    // ML model files
    private static final String CHALLENGE_RECOMMENDATION_MODEL = "challenge_recommendation_model.tflite";
    private static final String DIFFICULTY_PREDICTION_MODEL = "difficulty_prediction_model.tflite";
    private static final String REWARD_OPTIMIZATION_MODEL = "reward_optimization_model.tflite";
    
    // Challenge generation parameters
    private static final int MAX_DAILY_CHALLENGES = 5;
    private static final int FEATURE_VECTOR_SIZE = 25;
    private static final double NOVELTY_THRESHOLD = 0.7;
    private static final double PERSONALIZATION_WEIGHT = 0.8;
    
    // Challenge types with ML-based generation
    public enum ChallengeType {
        TREASURE_HUNT("treasure_hunt", "Find specific treasures"),
        EXPLORATION("exploration", "Explore new areas"),
        SPEED_CHALLENGE("speed_challenge", "Complete tasks quickly"),
        ACCURACY_CHALLENGE("accuracy_challenge", "Achieve high accuracy"),
        SOCIAL_CHALLENGE("social_challenge", "Interact with other players"),
        COLLECTION_CHALLENGE("collection_challenge", "Collect specific items"),
        DISTANCE_CHALLENGE("distance_challenge", "Travel certain distances"),
        TIME_CHALLENGE("time_challenge", "Play for specific durations"),
        SKILL_CHALLENGE("skill_challenge", "Demonstrate specific skills"),
        CREATIVE_CHALLENGE("creative_challenge", "Complete unique objectives");
        
        private final String id;
        private final String description;
        
        ChallengeType(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public String getId() { return id; }
        public String getDescription() { return description; }
    }
    
    // Difficulty levels with AI-calculated parameters
    public enum DifficultyLevel {
        BEGINNER(0.2, "Easy and accessible"),
        NOVICE(0.4, "Slightly challenging"),
        INTERMEDIATE(0.6, "Moderately difficult"),
        ADVANCED(0.8, "Quite challenging"),
        EXPERT(1.0, "Very difficult"),
        MASTER(1.2, "Extremely challenging");
        
        private final double multiplier;
        private final String description;
        
        DifficultyLevel(double multiplier, String description) {
            this.multiplier = multiplier;
            this.description = description;
        }
        
        public double getMultiplier() { return multiplier; }
        public String getDescription() { return description; }
    }
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final PlayerBehaviorAnalyzer behaviorAnalyzer;
    private final Random random;
    
    // ML models
    private Interpreter challengeRecommendationModel;
    private Interpreter difficultyPredictionModel;
    private Interpreter rewardOptimizationModel;
    
    // Challenge templates and patterns
    private final Map<ChallengeType, List<ChallengeTemplate>> challengeTemplates;
    private final Map<String, Double> globalChallengePopularity;
    
    // Generation listeners
    private ChallengeGenerationListener generationListener;
    
    public interface ChallengeGenerationListener {
        void onChallengesGenerated(String playerId, List<PersonalizedChallenge> challenges);
        void onGenerationError(String playerId, String error);
        void onModelUpdated(String modelName, double accuracy);
        void onPersonalizationImproved(String playerId, double improvementScore);
    }
    
    public AIChallengeGenerator(Context context, PlayerBehaviorAnalyzer behaviorAnalyzer) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.behaviorAnalyzer = behaviorAnalyzer;
        this.random = new Random();
        this.challengeTemplates = new HashMap<>();
        this.globalChallengePopularity = new HashMap<>();
        
        // Initialize challenge templates
        initializeChallengeTemplates();
        
        // Load ML models
        loadMLModels();
        
        Log.d(TAG, "AI challenge generator initialized");
    }
    
    /**
     * Generates personalized daily challenges for a player using AI
     */
    public CompletableFuture<List<PersonalizedChallenge>> generateDailyChallenges(String playerId, 
                                                                                Location currentLocation) {
        CompletableFuture<List<PersonalizedChallenge>> future = new CompletableFuture<>();
        
        // Get player behavior profile
        behaviorAnalyzer.getBehaviorProfile(playerId)
                .thenCompose(behaviorProfile -> {
                    // Generate feature vector for ML models
                    return behaviorAnalyzer.generateFeatureVector(playerId)
                            .thenApply(featureVector -> {
                                List<PersonalizedChallenge> challenges = new ArrayList<>();
                                
                                try {
                                    // Use ML model to recommend challenge types
                                    List<ChallengeType> recommendedTypes = recommendChallengeTypes(featureVector);
                                    
                                    // Generate challenges for each recommended type
                                    for (int i = 0; i < Math.min(MAX_DAILY_CHALLENGES, recommendedTypes.size()); i++) {
                                        ChallengeType type = recommendedTypes.get(i);
                                        
                                        // Predict optimal difficulty
                                        DifficultyLevel difficulty = predictOptimalDifficulty(featureVector, type);
                                        
                                        // Generate specific challenge
                                        PersonalizedChallenge challenge = generateSpecificChallenge(
                                                playerId, type, difficulty, behaviorProfile, currentLocation);
                                        
                                        if (challenge != null) {
                                            // Calculate personalized rewards
                                            calculatePersonalizedRewards(challenge, featureVector, behaviorProfile);
                                            
                                            challenges.add(challenge);
                                        }
                                    }
                                    
                                    // Add novelty and variety
                                    addNoveltyAndVariety(challenges, behaviorProfile);
                                    
                                    // Sort by predicted engagement
                                    challenges.sort((c1, c2) -> Double.compare(
                                            c2.getPredictedEngagement(), c1.getPredictedEngagement()));
                                    
                                } catch (Exception e) {
                                    Log.e(TAG, "Error generating AI challenges", e);
                                    // Fallback to rule-based generation
                                    challenges.addAll(generateFallbackChallenges(playerId, behaviorProfile, currentLocation));
                                }
                                
                                return challenges;
                            });
                })
                .thenAccept(challenges -> {
                    // Store generated challenges
                    storeChallenges(playerId, challenges);
                    
                    // Notify listener
                    if (generationListener != null) {
                        generationListener.onChallengesGenerated(playerId, challenges);
                    }
                    
                    future.complete(challenges);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to generate challenges", throwable);
                    if (generationListener != null) {
                        generationListener.onGenerationError(playerId, throwable.getMessage());
                    }
                    future.completeExceptionally(throwable);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Generates adaptive challenges based on real-time performance
     */
    public CompletableFuture<PersonalizedChallenge> generateAdaptiveChallenge(String playerId, 
                                                                            String currentChallengeId, 
                                                                            double currentPerformance) {
        CompletableFuture<PersonalizedChallenge> future = new CompletableFuture<>();
        
        behaviorAnalyzer.getBehaviorProfile(playerId)
                .thenCompose(behaviorProfile -> {
                    return behaviorAnalyzer.generateFeatureVector(playerId)
                            .thenApply(featureVector -> {
                                // Adjust difficulty based on current performance
                                double performanceRatio = currentPerformance / 0.7; // Target 70% success rate
                                
                                // Determine if we need to increase or decrease difficulty
                                DifficultyLevel newDifficulty;
                                if (performanceRatio > 1.2) {
                                    // Player is performing too well, increase difficulty
                                    newDifficulty = increaseDifficulty(getCurrentDifficulty(currentChallengeId));
                                } else if (performanceRatio < 0.8) {
                                    // Player is struggling, decrease difficulty
                                    newDifficulty = decreaseDifficulty(getCurrentDifficulty(currentChallengeId));
                                } else {
                                    // Performance is in target range, maintain difficulty
                                    newDifficulty = getCurrentDifficulty(currentChallengeId);
                                }
                                
                                // Generate new challenge with adjusted difficulty
                                ChallengeType adaptiveType = selectAdaptiveChallengeType(featureVector, behaviorProfile);
                                return generateSpecificChallenge(playerId, adaptiveType, newDifficulty, 
                                        behaviorProfile, null);
                            });
                })
                .thenAccept(challenge -> {
                    if (challenge != null) {
                        challenge.setAdaptive(true);
                        challenge.setParentChallengeId(currentChallengeId);
                        
                        // Store adaptive challenge
                        storeChallenge(playerId, challenge);
                        
                        future.complete(challenge);
                    } else {
                        future.completeExceptionally(new Exception("Failed to generate adaptive challenge"));
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to generate adaptive challenge", throwable);
                    future.completeExceptionally(throwable);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Updates ML models with player feedback and performance data
     */
    public CompletableFuture<Void> updateModelsWithFeedback(String playerId, String challengeId, 
                                                           double completionRate, double satisfaction, 
                                                           Map<String, Object> feedbackData) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Collect training data
        Map<String, Object> trainingData = new HashMap<>();
        trainingData.put("playerId", playerId);
        trainingData.put("challengeId", challengeId);
        trainingData.put("completionRate", completionRate);
        trainingData.put("satisfaction", satisfaction);
        trainingData.put("feedbackData", feedbackData);
        trainingData.put("timestamp", System.currentTimeMillis());
        
        // Store training data for model updates
        firestore.collection("ml_training_data")
                .add(trainingData)
                .addOnSuccessListener(documentReference -> {
                    // Trigger model retraining if enough data collected
                    checkAndTriggerModelRetraining();
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to store training data", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Gets challenge generation analytics and insights
     */
    public CompletableFuture<Map<String, Object>> getGenerationAnalytics(String playerId) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Get player's challenge history
        firestore.collection("personalized_challenges")
                .whereEqualTo("playerId", playerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> challengeHistory = new ArrayList<>();
                    double totalSatisfaction = 0.0;
                    double totalCompletion = 0.0;
                    int completedChallenges = 0;
                    
                    Map<String, Integer> typeDistribution = new HashMap<>();
                    Map<String, Double> difficultyDistribution = new HashMap<>();
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        Map<String, Object> challengeData = document.getData();
                        challengeHistory.add(challengeData);
                        
                        // Aggregate statistics
                        String type = (String) challengeData.get("type");
                        if (type != null) {
                            typeDistribution.put(type, typeDistribution.getOrDefault(type, 0) + 1);
                        }
                        
                        Double difficulty = (Double) challengeData.get("difficultyScore");
                        if (difficulty != null) {
                            String difficultyRange = getDifficultyRange(difficulty);
                            difficultyDistribution.put(difficultyRange, 
                                    difficultyDistribution.getOrDefault(difficultyRange, 0.0) + 1.0);
                        }
                        
                        Boolean completed = (Boolean) challengeData.get("completed");
                        if (completed != null && completed) {
                            completedChallenges++;
                            
                            Double satisfaction = (Double) challengeData.get("satisfactionScore");
                            if (satisfaction != null) {
                                totalSatisfaction += satisfaction;
                            }
                            
                            Double completion = (Double) challengeData.get("completionScore");
                            if (completion != null) {
                                totalCompletion += completion;
                            }
                        }
                    }
                    
                    // Calculate analytics
                    analytics.put("challengeHistory", challengeHistory);
                    analytics.put("totalChallenges", querySnapshot.size());
                    analytics.put("completedChallenges", completedChallenges);
                    analytics.put("completionRate", querySnapshot.size() > 0 ? 
                            (double) completedChallenges / querySnapshot.size() : 0.0);
                    analytics.put("averageSatisfaction", completedChallenges > 0 ? 
                            totalSatisfaction / completedChallenges : 0.0);
                    analytics.put("averageCompletion", completedChallenges > 0 ? 
                            totalCompletion / completedChallenges : 0.0);
                    analytics.put("typeDistribution", typeDistribution);
                    analytics.put("difficultyDistribution", difficultyDistribution);
                    
                    future.complete(analytics);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get generation analytics", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Sets challenge generation listener
     */
    public void setChallengeGenerationListener(ChallengeGenerationListener listener) {
        this.generationListener = listener;
    }
    
    /**
     * Cleanup resources and models
     */
    public void cleanup() {
        if (challengeRecommendationModel != null) {
            challengeRecommendationModel.close();
        }
        if (difficultyPredictionModel != null) {
            difficultyPredictionModel.close();
        }
        if (rewardOptimizationModel != null) {
            rewardOptimizationModel.close();
        }
        
        Log.d(TAG, "AI challenge generator cleaned up");
    }

    // Private helper methods
    private void initializeChallengeTemplates() {
        // Initialize templates for each challenge type
        for (ChallengeType type : ChallengeType.values()) {
            challengeTemplates.put(type, createTemplatesForType(type));
        }

        // Initialize global popularity scores
        globalChallengePopularity.put("treasure_hunt", 0.8);
        globalChallengePopularity.put("exploration", 0.7);
        globalChallengePopularity.put("speed_challenge", 0.6);
        globalChallengePopularity.put("accuracy_challenge", 0.5);
        globalChallengePopularity.put("social_challenge", 0.4);
        globalChallengePopularity.put("collection_challenge", 0.7);
        globalChallengePopularity.put("distance_challenge", 0.6);
        globalChallengePopularity.put("time_challenge", 0.5);
        globalChallengePopularity.put("skill_challenge", 0.8);
        globalChallengePopularity.put("creative_challenge", 0.3);
    }

    private List<ChallengeTemplate> createTemplatesForType(ChallengeType type) {
        List<ChallengeTemplate> templates = new ArrayList<>();

        switch (type) {
            case TREASURE_HUNT:
                templates.add(new ChallengeTemplate("Find {count} {rarity} treasures",
                        Map.of("count", Arrays.asList(1, 2, 3, 5), "rarity", Arrays.asList("Common", "Uncommon", "Rare"))));
                templates.add(new ChallengeTemplate("Discover a {rarity} treasure within {time} minutes",
                        Map.of("rarity", Arrays.asList("Rare", "Epic", "Legendary"), "time", Arrays.asList(10, 15, 30))));
                break;

            case EXPLORATION:
                templates.add(new ChallengeTemplate("Explore {distance}m from your current location",
                        Map.of("distance", Arrays.asList(100, 250, 500, 1000))));
                templates.add(new ChallengeTemplate("Visit {count} new locations",
                        Map.of("count", Arrays.asList(2, 3, 5, 8))));
                break;

            case SPEED_CHALLENGE:
                templates.add(new ChallengeTemplate("Capture {count} treasures in under {time} minutes",
                        Map.of("count", Arrays.asList(2, 3, 5), "time", Arrays.asList(5, 10, 15))));
                templates.add(new ChallengeTemplate("Complete {count} challenges in {time} minutes",
                        Map.of("count", Arrays.asList(1, 2, 3), "time", Arrays.asList(10, 20, 30))));
                break;

            case ACCURACY_CHALLENGE:
                templates.add(new ChallengeTemplate("Achieve {accuracy}% accuracy in treasure detection",
                        Map.of("accuracy", Arrays.asList(80, 90, 95))));
                templates.add(new ChallengeTemplate("Complete {count} perfect captures",
                        Map.of("count", Arrays.asList(1, 2, 3))));
                break;

            case SOCIAL_CHALLENGE:
                templates.add(new ChallengeTemplate("Help {count} other players",
                        Map.of("count", Arrays.asList(1, 2, 3))));
                templates.add(new ChallengeTemplate("Send {count} messages in chat",
                        Map.of("count", Arrays.asList(5, 10, 15))));
                break;

            case COLLECTION_CHALLENGE:
                templates.add(new ChallengeTemplate("Collect {count} different treasure types",
                        Map.of("count", Arrays.asList(3, 5, 8))));
                templates.add(new ChallengeTemplate("Build a collection of {rarity} treasures",
                        Map.of("rarity", Arrays.asList("Uncommon", "Rare", "Epic"))));
                break;

            case DISTANCE_CHALLENGE:
                templates.add(new ChallengeTemplate("Walk {distance}km while playing",
                        Map.of("distance", Arrays.asList(1, 2, 5))));
                templates.add(new ChallengeTemplate("Travel {distance}m between treasures",
                        Map.of("distance", Arrays.asList(500, 1000, 2000))));
                break;

            case TIME_CHALLENGE:
                templates.add(new ChallengeTemplate("Play for {time} minutes today",
                        Map.of("time", Arrays.asList(15, 30, 60))));
                templates.add(new ChallengeTemplate("Maintain a {streak} day streak",
                        Map.of("streak", Arrays.asList(3, 5, 7))));
                break;

            case SKILL_CHALLENGE:
                templates.add(new ChallengeTemplate("Demonstrate mastery in {skill}",
                        Map.of("skill", Arrays.asList("treasure detection", "navigation", "efficiency"))));
                templates.add(new ChallengeTemplate("Achieve {level} skill level",
                        Map.of("level", Arrays.asList("Advanced", "Expert", "Master"))));
                break;

            case CREATIVE_CHALLENGE:
                templates.add(new ChallengeTemplate("Create your own treasure hunting route", Map.of()));
                templates.add(new ChallengeTemplate("Discover a unique treasure combination", Map.of()));
                break;
        }

        return templates;
    }

    private void loadMLModels() {
        try {
            // Load challenge recommendation model
            challengeRecommendationModel = new Interpreter(loadModelFile(CHALLENGE_RECOMMENDATION_MODEL));

            // Load difficulty prediction model
            difficultyPredictionModel = new Interpreter(loadModelFile(DIFFICULTY_PREDICTION_MODEL));

            // Load reward optimization model
            rewardOptimizationModel = new Interpreter(loadModelFile(REWARD_OPTIMIZATION_MODEL));

            Log.d(TAG, "ML models loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load ML models", e);
            // Continue without ML models - will use fallback methods
        }
    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd(modelName).getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelName).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelName).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<ChallengeType> recommendChallengeTypes(Map<String, Double> featureVector) {
        List<ChallengeType> recommendedTypes = new ArrayList<>();

        if (challengeRecommendationModel != null) {
            try {
                // Prepare input tensor
                float[][] input = new float[1][FEATURE_VECTOR_SIZE];
                int i = 0;
                for (String key : Arrays.asList("avg_session_duration", "sessions_per_week", "exploration_radius",
                        "treasure_hunting_frequency", "challenge_completion_rate", "avg_completion_time",
                        "accuracy_score", "current_streak", "improvement_rate", "difficulty_preference",
                        "competitiveness_level", "exploration_vs_efficiency", "social_interaction_preference",
                        "daily_engagement_score", "weekly_engagement_score", "consecutive_days_played",
                        "session_retention_rate", "churn_risk", "social_interaction_frequency", "friends_count",
                        "team_participation_rate", "chat_activity_level", "leaderboard_engagement",
                        "peak_activity_hour", "morning_activity_ratio")) {
                    input[0][i++] = featureVector.getOrDefault(key, 0.0).floatValue();
                }

                // Run inference
                float[][] output = new float[1][ChallengeType.values().length];
                challengeRecommendationModel.run(input, output);

                // Convert output to recommendations
                ChallengeType[] types = ChallengeType.values();
                for (int j = 0; j < output[0].length; j++) {
                    if (output[0][j] > 0.5) { // Threshold for recommendation
                        recommendedTypes.add(types[j]);
                    }
                }

                // Sort by confidence
                recommendedTypes.sort((t1, t2) -> {
                    int idx1 = Arrays.asList(types).indexOf(t1);
                    int idx2 = Arrays.asList(types).indexOf(t2);
                    return Float.compare(output[0][idx2], output[0][idx1]);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error running challenge recommendation model", e);
                // Fallback to rule-based recommendation
                recommendedTypes = getFallbackRecommendations(featureVector);
            }
        } else {
            // Fallback to rule-based recommendation
            recommendedTypes = getFallbackRecommendations(featureVector);
        }

        // Ensure we have at least some recommendations
        if (recommendedTypes.isEmpty()) {
            recommendedTypes.addAll(Arrays.asList(ChallengeType.TREASURE_HUNT, ChallengeType.EXPLORATION,
                    ChallengeType.COLLECTION_CHALLENGE));
        }

        return recommendedTypes;
    }

    private List<ChallengeType> getFallbackRecommendations(Map<String, Double> featureVector) {
        List<ChallengeType> recommendations = new ArrayList<>();

        double explorationPreference = featureVector.getOrDefault("exploration_vs_efficiency", 0.5);
        double socialPreference = featureVector.getOrDefault("social_interaction_preference", 0.5);
        double competitiveness = featureVector.getOrDefault("competitiveness_level", 0.5);
        double skillLevel = featureVector.getOrDefault("challenge_completion_rate", 0.5);

        // Rule-based recommendations
        if (explorationPreference > 0.6) {
            recommendations.add(ChallengeType.EXPLORATION);
            recommendations.add(ChallengeType.DISTANCE_CHALLENGE);
        }

        if (socialPreference > 0.6) {
            recommendations.add(ChallengeType.SOCIAL_CHALLENGE);
        }

        if (competitiveness > 0.7) {
            recommendations.add(ChallengeType.SPEED_CHALLENGE);
            recommendations.add(ChallengeType.ACCURACY_CHALLENGE);
        }

        if (skillLevel > 0.7) {
            recommendations.add(ChallengeType.SKILL_CHALLENGE);
            recommendations.add(ChallengeType.CREATIVE_CHALLENGE);
        }

        // Always include popular types
        recommendations.add(ChallengeType.TREASURE_HUNT);
        recommendations.add(ChallengeType.COLLECTION_CHALLENGE);

        return recommendations;
    }

    private DifficultyLevel predictOptimalDifficulty(Map<String, Double> featureVector, ChallengeType challengeType) {
        if (difficultyPredictionModel != null) {
            try {
                // Prepare input with feature vector + challenge type encoding
                float[][] input = new float[1][FEATURE_VECTOR_SIZE + ChallengeType.values().length];

                // Add feature vector
                int i = 0;
                for (String key : Arrays.asList("avg_session_duration", "sessions_per_week", "exploration_radius",
                        "treasure_hunting_frequency", "challenge_completion_rate", "avg_completion_time",
                        "accuracy_score", "current_streak", "improvement_rate", "difficulty_preference",
                        "competitiveness_level", "exploration_vs_efficiency", "social_interaction_preference",
                        "daily_engagement_score", "weekly_engagement_score", "consecutive_days_played",
                        "session_retention_rate", "churn_risk", "social_interaction_frequency", "friends_count",
                        "team_participation_rate", "chat_activity_level", "leaderboard_engagement",
                        "peak_activity_hour", "morning_activity_ratio")) {
                    input[0][i++] = featureVector.getOrDefault(key, 0.0).floatValue();
                }

                // Add challenge type one-hot encoding
                ChallengeType[] types = ChallengeType.values();
                for (int j = 0; j < types.length; j++) {
                    input[0][i++] = types[j] == challengeType ? 1.0f : 0.0f;
                }

                // Run inference
                float[][] output = new float[1][1]; // Single difficulty score
                difficultyPredictionModel.run(input, output);

                // Convert to difficulty level
                float difficultyScore = output[0][0];
                return getDifficultyLevelFromScore(difficultyScore);

            } catch (Exception e) {
                Log.e(TAG, "Error running difficulty prediction model", e);
            }
        }

        // Fallback to rule-based difficulty prediction
        return predictFallbackDifficulty(featureVector, challengeType);
    }

    private DifficultyLevel predictFallbackDifficulty(Map<String, Double> featureVector, ChallengeType challengeType) {
        double skillLevel = featureVector.getOrDefault("challenge_completion_rate", 0.5);
        double difficultyPreference = featureVector.getOrDefault("difficulty_preference", 0.5);
        double engagementScore = featureVector.getOrDefault("daily_engagement_score", 0.5);

        // Calculate base difficulty
        double baseDifficulty = (skillLevel * 0.4) + (difficultyPreference * 0.4) + (engagementScore * 0.2);

        // Adjust for challenge type
        switch (challengeType) {
            case SPEED_CHALLENGE:
            case ACCURACY_CHALLENGE:
            case SKILL_CHALLENGE:
                baseDifficulty += 0.1; // These are inherently more challenging
                break;
            case SOCIAL_CHALLENGE:
            case TIME_CHALLENGE:
                baseDifficulty -= 0.1; // These are more accessible
                break;
        }

        return getDifficultyLevelFromScore((float) baseDifficulty);
    }

    private DifficultyLevel getDifficultyLevelFromScore(float score) {
        if (score < 0.2) return DifficultyLevel.BEGINNER;
        else if (score < 0.4) return DifficultyLevel.NOVICE;
        else if (score < 0.6) return DifficultyLevel.INTERMEDIATE;
        else if (score < 0.8) return DifficultyLevel.ADVANCED;
        else if (score < 1.0) return DifficultyLevel.EXPERT;
        else return DifficultyLevel.MASTER;
    }
