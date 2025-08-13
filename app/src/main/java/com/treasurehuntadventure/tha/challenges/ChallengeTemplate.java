package com.treasurehuntadventure.tha.challenges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template for generating personalized challenges with dynamic parameters
 */
public class ChallengeTemplate {
    
    private String templateText;
    private Map<String, List<Object>> parameterOptions;
    private Map<String, String> parameterTypes;
    private List<String> requiredParameters;
    private Map<String, Object> defaultValues;
    private double baseComplexity;
    private List<String> tags;
    
    // Template generation settings
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{(\\w+)\\}");
    private static final Random random = new Random();
    
    public ChallengeTemplate() {
        this.parameterOptions = new HashMap<>();
        this.parameterTypes = new HashMap<>();
        this.requiredParameters = new ArrayList<>();
        this.defaultValues = new HashMap<>();
        this.tags = new ArrayList<>();
        this.baseComplexity = 0.5;
    }
    
    public ChallengeTemplate(String templateText, Map<String, List<Object>> parameterOptions) {
        this();
        this.templateText = templateText;
        this.parameterOptions = parameterOptions;
        
        // Extract required parameters from template
        extractRequiredParameters();
        
        // Set default parameter types
        setDefaultParameterTypes();
    }
    
    public ChallengeTemplate(String templateText, Map<String, List<Object>> parameterOptions, 
                           double baseComplexity, List<String> tags) {
        this(templateText, parameterOptions);
        this.baseComplexity = baseComplexity;
        this.tags = tags;
    }
    
    /**
     * Generates a personalized challenge from this template
     */
    public PersonalizedChallenge generateChallenge(String playerId, 
                                                 AIChallengeGenerator.ChallengeType challengeType,
                                                 AIChallengeGenerator.DifficultyLevel difficulty,
                                                 PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile) {
        
        // Select parameters based on player behavior and difficulty
        Map<String, Object> selectedParameters = selectPersonalizedParameters(behaviorProfile, difficulty);
        
        // Generate challenge text
        String challengeTitle = generateChallengeText(selectedParameters);
        String challengeDescription = generateChallengeDescription(selectedParameters, challengeType);
        
        // Create personalized challenge
        PersonalizedChallenge challenge = new PersonalizedChallenge(
                playerId, challengeTitle, challengeDescription, 
                challengeType.getId(), difficulty.name());
        
        // Set challenge parameters
        challenge.setParameters(selectedParameters);
        challenge.setDifficultyScore(calculateDifficultyScore(selectedParameters, difficulty));
        challenge.setPredictedEngagement(calculatePredictedEngagement(behaviorProfile, selectedParameters));
        challenge.setPersonalizationScore(calculatePersonalizationScore(behaviorProfile, selectedParameters));
        
        // Set AI tags
        List<String> aiTags = new ArrayList<>(tags);
        aiTags.addAll(generateContextualTags(behaviorProfile, selectedParameters));
        challenge.setAiTags(aiTags);
        
        // Set time limit based on challenge type and difficulty
        challenge.setTimeLimit(calculateTimeLimit(challengeType, difficulty, selectedParameters));
        
        // Generate milestones
        challenge.setMilestones(generateMilestones(selectedParameters, difficulty));
        
        // Set constraints
        challenge.setConstraints(generateConstraints(selectedParameters, difficulty));
        
        return challenge;
    }
    
    /**
     * Validates if this template can generate a challenge for the given parameters
     */
    public boolean canGenerate(PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                             AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Check if all required parameters can be satisfied
        for (String param : requiredParameters) {
            if (!parameterOptions.containsKey(param) || parameterOptions.get(param).isEmpty()) {
                return false;
            }
        }
        
        // Check if difficulty is appropriate for template complexity
        double playerSkillLevel = behaviorProfile.getSkillLevel();
        double templateComplexity = baseComplexity * difficulty.getMultiplier();
        
        // Template should be within reasonable range of player skill
        return Math.abs(templateComplexity - playerSkillLevel) <= 0.4;
    }
    
    /**
     * Calculates template compatibility score for a player
     */
    public double calculateCompatibilityScore(PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                            AIChallengeGenerator.ChallengeType challengeType) {
        
        double compatibilityScore = 0.0;
        
        // Base compatibility from template tags and player preferences
        PlayerBehaviorAnalyzer.PreferenceProfile preferences = behaviorProfile.getPreferenceProfile();
        
        for (String tag : tags) {
            switch (tag) {
                case "exploration":
                    compatibilityScore += preferences.getExplorationVsEfficiency() * 0.2;
                    break;
                case "social":
                    compatibilityScore += preferences.getSocialInteractionPreference() * 0.2;
                    break;
                case "competitive":
                    compatibilityScore += preferences.getCompetitivenessLevel() * 0.2;
                    break;
                case "collection":
                    // Check rarity preferences
                    double collectionScore = preferences.getRarityPreferences().values().stream()
                            .mapToDouble(Double::doubleValue).average().orElse(0.5);
                    compatibilityScore += collectionScore * 0.2;
                    break;
                case "speed":
                    // Check if player likes speed challenges
                    Map<String, Double> challengePrefs = preferences.getChallengeTypePreferences();
                    compatibilityScore += challengePrefs.getOrDefault("speed_challenge", 0.5) * 0.2;
                    break;
            }
        }
        
        // Adjust for challenge type match
        if (tags.contains(challengeType.getId())) {
            compatibilityScore += 0.3;
        }
        
        // Adjust for player skill level and template complexity
        double skillMatch = 1.0 - Math.abs(behaviorProfile.getSkillLevel() - baseComplexity);
        compatibilityScore += skillMatch * 0.3;
        
        return Math.max(0.0, Math.min(1.0, compatibilityScore));
    }
    
    // Private helper methods
    private void extractRequiredParameters() {
        requiredParameters.clear();
        
        Matcher matcher = PARAMETER_PATTERN.matcher(templateText);
        while (matcher.find()) {
            String parameter = matcher.group(1);
            if (!requiredParameters.contains(parameter)) {
                requiredParameters.add(parameter);
            }
        }
    }
    
    private void setDefaultParameterTypes() {
        for (String param : requiredParameters) {
            if (parameterOptions.containsKey(param) && !parameterOptions.get(param).isEmpty()) {
                Object firstOption = parameterOptions.get(param).get(0);
                if (firstOption instanceof Integer) {
                    parameterTypes.put(param, "integer");
                } else if (firstOption instanceof Double) {
                    parameterTypes.put(param, "double");
                } else if (firstOption instanceof String) {
                    parameterTypes.put(param, "string");
                } else {
                    parameterTypes.put(param, "object");
                }
            }
        }
    }
    
    private Map<String, Object> selectPersonalizedParameters(PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                                           AIChallengeGenerator.DifficultyLevel difficulty) {
        Map<String, Object> selectedParams = new HashMap<>();
        
        for (String param : requiredParameters) {
            List<Object> options = parameterOptions.get(param);
            if (options != null && !options.isEmpty()) {
                Object selectedValue = selectParameterValue(param, options, behaviorProfile, difficulty);
                selectedParams.put(param, selectedValue);
            } else if (defaultValues.containsKey(param)) {
                selectedParams.put(param, defaultValues.get(param));
            }
        }
        
        return selectedParams;
    }
    
    private Object selectParameterValue(String paramName, List<Object> options,
                                      PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                      AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Personalize parameter selection based on player behavior
        switch (paramName) {
            case "count":
                return selectCountParameter(options, behaviorProfile, difficulty);
            case "time":
                return selectTimeParameter(options, behaviorProfile, difficulty);
            case "distance":
                return selectDistanceParameter(options, behaviorProfile, difficulty);
            case "rarity":
                return selectRarityParameter(options, behaviorProfile, difficulty);
            case "accuracy":
                return selectAccuracyParameter(options, behaviorProfile, difficulty);
            default:
                // Default random selection with difficulty bias
                return selectWithDifficultyBias(options, difficulty);
        }
    }
    
    private Object selectCountParameter(List<Object> options, 
                                      PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                      AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Adjust count based on player skill and difficulty
        double skillLevel = behaviorProfile.getSkillLevel();
        double difficultyMultiplier = difficulty.getMultiplier();
        
        // Find appropriate count based on skill and difficulty
        List<Integer> counts = options.stream()
                .filter(o -> o instanceof Integer)
                .map(o -> (Integer) o)
                .sorted()
                .toList();
        
        if (counts.isEmpty()) return options.get(0);
        
        // Select count based on combined skill and difficulty
        double targetIndex = (skillLevel * 0.6 + difficultyMultiplier * 0.4) * (counts.size() - 1);
        int index = Math.max(0, Math.min(counts.size() - 1, (int) Math.round(targetIndex)));
        
        return counts.get(index);
    }
    
    private Object selectTimeParameter(List<Object> options,
                                     PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                     AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Adjust time based on player's average session duration and difficulty
        PlayerBehaviorAnalyzer.GameplayPatterns gameplay = behaviorProfile.getGameplayPatterns();
        double avgSessionMinutes = gameplay.getAverageSessionDuration() / 60000.0; // Convert to minutes
        
        List<Integer> times = options.stream()
                .filter(o -> o instanceof Integer)
                .map(o -> (Integer) o)
                .sorted()
                .toList();
        
        if (times.isEmpty()) return options.get(0);
        
        // Select time that's reasonable for player's typical session length
        // Higher difficulty = less time allowed
        double timeRatio = Math.max(0.2, 1.0 - (difficulty.getMultiplier() - 0.2) * 0.5);
        double targetTime = Math.min(avgSessionMinutes * 0.3, 60) * timeRatio;
        
        // Find closest time option
        return times.stream()
                .min((t1, t2) -> Double.compare(Math.abs(t1 - targetTime), Math.abs(t2 - targetTime)))
                .orElse(times.get(0));
    }
    
    private Object selectDistanceParameter(List<Object> options,
                                         PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                         AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Adjust distance based on player's exploration patterns
        PlayerBehaviorAnalyzer.GameplayPatterns gameplay = behaviorProfile.getGameplayPatterns();
        double explorationRadius = gameplay.getExplorationRadius();
        
        List<Integer> distances = options.stream()
                .filter(o -> o instanceof Integer)
                .map(o -> (Integer) o)
                .sorted()
                .toList();
        
        if (distances.isEmpty()) return options.get(0);
        
        // Select distance based on player's typical exploration range and difficulty
        double targetDistance = explorationRadius * difficulty.getMultiplier();
        
        return distances.stream()
                .min((d1, d2) -> Double.compare(Math.abs(d1 - targetDistance), Math.abs(d2 - targetDistance)))
                .orElse(distances.get(0));
    }
    
    private Object selectRarityParameter(List<Object> options,
                                       PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                       AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Select rarity based on player preferences and difficulty
        PlayerBehaviorAnalyzer.PreferenceProfile preferences = behaviorProfile.getPreferenceProfile();
        Map<com.treasurehuntadventure.tha.TreasureRarity, Double> rarityPrefs = preferences.getRarityPreferences();
        
        List<String> rarities = options.stream()
                .filter(o -> o instanceof String)
                .map(o -> (String) o)
                .toList();
        
        if (rarities.isEmpty()) return options.get(0);
        
        // Weight rarities by preference and difficulty
        String bestRarity = rarities.get(0);
        double bestScore = 0.0;
        
        for (String rarity : rarities) {
            try {
                com.treasurehuntadventure.tha.TreasureRarity treasureRarity = 
                        com.treasurehuntadventure.tha.TreasureRarity.valueOf(rarity.toUpperCase());
                double preferenceScore = rarityPrefs.getOrDefault(treasureRarity, 0.5);
                double difficultyScore = getRarityDifficultyScore(rarity) * difficulty.getMultiplier();
                double totalScore = preferenceScore * 0.7 + difficultyScore * 0.3;
                
                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestRarity = rarity;
                }
            } catch (IllegalArgumentException e) {
                // Invalid rarity, skip
            }
        }
        
        return bestRarity;
    }
    
    private Object selectAccuracyParameter(List<Object> options,
                                         PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                         AIChallengeGenerator.DifficultyLevel difficulty) {
        
        // Select accuracy target based on player's historical accuracy and difficulty
        PlayerBehaviorAnalyzer.PerformanceMetrics performance = behaviorProfile.getPerformanceMetrics();
        double playerAccuracy = performance.getAccuracyScore();
        
        List<Integer> accuracies = options.stream()
                .filter(o -> o instanceof Integer)
                .map(o -> (Integer) o)
                .sorted()
                .toList();
        
        if (accuracies.isEmpty()) return options.get(0);
        
        // Target accuracy should be challenging but achievable
        double targetAccuracy = Math.min(95, playerAccuracy * 100 + (difficulty.getMultiplier() - 0.5) * 20);
        
        return accuracies.stream()
                .min((a1, a2) -> Double.compare(Math.abs(a1 - targetAccuracy), Math.abs(a2 - targetAccuracy)))
                .orElse(accuracies.get(0));
    }
    
    private Object selectWithDifficultyBias(List<Object> options, AIChallengeGenerator.DifficultyLevel difficulty) {
        if (options.isEmpty()) return null;
        
        // Select option based on difficulty level
        double difficultyRatio = difficulty.getMultiplier();
        int targetIndex = (int) Math.round(difficultyRatio * (options.size() - 1));
        targetIndex = Math.max(0, Math.min(options.size() - 1, targetIndex));
        
        return options.get(targetIndex);
    }
    
    private String generateChallengeText(Map<String, Object> parameters) {
        String result = templateText;
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue().toString();
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    private String generateChallengeDescription(Map<String, Object> parameters, 
                                              AIChallengeGenerator.ChallengeType challengeType) {
        
        StringBuilder description = new StringBuilder();
        description.append(challengeType.getDescription()).append(". ");
        
        // Add parameter-specific descriptions
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            switch (entry.getKey()) {
                case "time":
                    description.append("You have ").append(entry.getValue()).append(" minutes to complete this challenge. ");
                    break;
                case "accuracy":
                    description.append("Maintain at least ").append(entry.getValue()).append("% accuracy. ");
                    break;
                case "distance":
                    description.append("Cover a distance of ").append(entry.getValue()).append(" meters. ");
                    break;
            }
        }
        
        return description.toString().trim();
    }
    
    private double calculateDifficultyScore(Map<String, Object> parameters, 
                                          AIChallengeGenerator.DifficultyLevel difficulty) {
        
        double baseScore = difficulty.getMultiplier();
        
        // Adjust based on parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            switch (entry.getKey()) {
                case "count":
                    if (entry.getValue() instanceof Integer) {
                        int count = (Integer) entry.getValue();
                        baseScore += count * 0.05; // Each additional item increases difficulty
                    }
                    break;
                case "time":
                    if (entry.getValue() instanceof Integer) {
                        int time = (Integer) entry.getValue();
                        baseScore += Math.max(0, (30 - time) * 0.01); // Less time = more difficult
                    }
                    break;
                case "accuracy":
                    if (entry.getValue() instanceof Integer) {
                        int accuracy = (Integer) entry.getValue();
                        baseScore += (accuracy - 70) * 0.005; // Higher accuracy = more difficult
                    }
                    break;
            }
        }
        
        return Math.max(0.0, Math.min(1.0, baseScore));
    }
    
    private double calculatePredictedEngagement(PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                              Map<String, Object> parameters) {
        
        double engagementScore = behaviorProfile.getEngagementScore();
        
        // Adjust based on personalization
        double personalizationBonus = calculatePersonalizationScore(behaviorProfile, parameters) * 0.2;
        
        // Adjust based on challenge novelty
        double noveltyBonus = calculateNoveltyScore(parameters) * 0.1;
        
        return Math.max(0.0, Math.min(1.0, engagementScore + personalizationBonus + noveltyBonus));
    }
    
    private double calculatePersonalizationScore(PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                               Map<String, Object> parameters) {
        
        double personalizationScore = 0.5; // Base score
        
        // Check how well parameters match player preferences
        PlayerBehaviorAnalyzer.PreferenceProfile preferences = behaviorProfile.getPreferenceProfile();
        
        // Time preference matching
        if (parameters.containsKey("time")) {
            PlayerBehaviorAnalyzer.GameplayPatterns gameplay = behaviorProfile.getGameplayPatterns();
            double avgSession = gameplay.getAverageSessionDuration() / 60000.0; // minutes
            int challengeTime = (Integer) parameters.get("time");
            
            if (challengeTime <= avgSession * 0.5) {
                personalizationScore += 0.2; // Good match for session length
            }
        }
        
        // Difficulty preference matching
        double difficultyMatch = Math.abs(preferences.getDifficultyPreference() - baseComplexity);
        personalizationScore += (1.0 - difficultyMatch) * 0.3;
        
        return Math.max(0.0, Math.min(1.0, personalizationScore));
    }
    
    private double calculateNoveltyScore(Map<String, Object> parameters) {
        // Simple novelty calculation based on parameter uniqueness
        // In a real implementation, this would check against recent challenges
        return 0.7; // Placeholder
    }
    
    private long calculateTimeLimit(AIChallengeGenerator.ChallengeType challengeType,
                                  AIChallengeGenerator.DifficultyLevel difficulty,
                                  Map<String, Object> parameters) {
        
        long baseTimeLimit = 24 * 60 * 60 * 1000; // 24 hours default
        
        // Adjust based on challenge type
        switch (challengeType) {
            case SPEED_CHALLENGE:
                baseTimeLimit = 60 * 60 * 1000; // 1 hour
                break;
            case TIME_CHALLENGE:
                baseTimeLimit = 8 * 60 * 60 * 1000; // 8 hours
                break;
            case SOCIAL_CHALLENGE:
                baseTimeLimit = 12 * 60 * 60 * 1000; // 12 hours
                break;
        }
        
        // Adjust based on specific time parameter
        if (parameters.containsKey("time")) {
            int timeParam = (Integer) parameters.get("time");
            baseTimeLimit = Math.min(baseTimeLimit, timeParam * 60 * 1000L * 2); // Double the challenge time
        }
        
        // Adjust based on difficulty
        baseTimeLimit = (long) (baseTimeLimit / difficulty.getMultiplier());
        
        return baseTimeLimit;
    }
    
    private List<PersonalizedChallenge.Milestone> generateMilestones(Map<String, Object> parameters,
                                                                   AIChallengeGenerator.DifficultyLevel difficulty) {
        
        List<PersonalizedChallenge.Milestone> milestones = new ArrayList<>();
        
        // Generate milestones based on parameters
        if (parameters.containsKey("count")) {
            int totalCount = (Integer) parameters.get("count");
            if (totalCount > 2) {
                // Create intermediate milestones
                int milestone1 = totalCount / 3;
                int milestone2 = (totalCount * 2) / 3;
                
                milestones.add(new PersonalizedChallenge.Milestone(
                        "milestone_1", "Getting Started", 
                        "Complete " + milestone1 + " items", milestone1));
                
                milestones.add(new PersonalizedChallenge.Milestone(
                        "milestone_2", "Making Progress", 
                        "Complete " + milestone2 + " items", milestone2));
            }
        }
        
        return milestones;
    }
    
    private Map<String, Object> generateConstraints(Map<String, Object> parameters,
                                                  AIChallengeGenerator.DifficultyLevel difficulty) {
        
        Map<String, Object> constraints = new HashMap<>();
        
        // Add time constraints
        if (parameters.containsKey("time")) {
            constraints.put("maxTime", parameters.get("time"));
        }
        
        // Add accuracy constraints
        if (parameters.containsKey("accuracy")) {
            constraints.put("minAccuracy", parameters.get("accuracy"));
        }
        
        // Add difficulty-based constraints
        constraints.put("maxAttempts", difficulty.getMultiplier() > 0.8 ? 1 : 3);
        constraints.put("allowHints", difficulty.getMultiplier() < 0.6);
        
        return constraints;
    }
    
    private List<String> generateContextualTags(PlayerBehaviorAnalyzer.PlayerBehaviorProfile behaviorProfile,
                                               Map<String, Object> parameters) {
        
        List<String> contextualTags = new ArrayList<>();
        
        // Add skill-based tags
        if (behaviorProfile.getSkillLevel() > 0.8) {
            contextualTags.add("expert_level");
        } else if (behaviorProfile.getSkillLevel() < 0.3) {
            contextualTags.add("beginner_friendly");
        }
        
        // Add engagement-based tags
        if (behaviorProfile.getEngagementScore() > 0.8) {
            contextualTags.add("high_engagement");
        }
        
        // Add parameter-based tags
        if (parameters.containsKey("time") && (Integer) parameters.get("time") < 15) {
            contextualTags.add("quick_challenge");
        }
        
        if (parameters.containsKey("count") && (Integer) parameters.get("count") > 5) {
            contextualTags.add("collection_focused");
        }
        
        return contextualTags;
    }
    
    private double getRarityDifficultyScore(String rarity) {
        switch (rarity.toLowerCase()) {
            case "common": return 0.2;
            case "uncommon": return 0.4;
            case "rare": return 0.6;
            case "epic": return 0.8;
            case "legendary": return 1.0;
            case "mythic": return 1.2;
            default: return 0.5;
        }
    }
    
    // Getters and setters
    public String getTemplateText() { return templateText; }
    public void setTemplateText(String templateText) { 
        this.templateText = templateText;
        extractRequiredParameters();
    }
    
    public Map<String, List<Object>> getParameterOptions() { return parameterOptions; }
    public void setParameterOptions(Map<String, List<Object>> parameterOptions) { 
        this.parameterOptions = parameterOptions;
        setDefaultParameterTypes();
    }
    
    public Map<String, String> getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(Map<String, String> parameterTypes) { this.parameterTypes = parameterTypes; }
    
    public List<String> getRequiredParameters() { return requiredParameters; }
    
    public Map<String, Object> getDefaultValues() { return defaultValues; }
    public void setDefaultValues(Map<String, Object> defaultValues) { this.defaultValues = defaultValues; }
    
    public double getBaseComplexity() { return baseComplexity; }
    public void setBaseComplexity(double baseComplexity) { this.baseComplexity = baseComplexity; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
