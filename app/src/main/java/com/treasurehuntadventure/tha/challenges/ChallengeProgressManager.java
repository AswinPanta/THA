package com.treasurehuntadventure.tha.challenges;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages challenge progress tracking, milestone detection, and adaptive hints
 */
public class ChallengeProgressManager {
    private static final String TAG = "ChallengeProgressManager";
    
    // Firestore collections
    private static final String CHALLENGES_COLLECTION = "personalized_challenges";
    private static final String PROGRESS_COLLECTION = "challenge_progress";
    private static final String MILESTONES_COLLECTION = "challenge_milestones";
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final PlayerBehaviorAnalyzer behaviorAnalyzer;
    
    // Active challenge tracking
    private final Map<String, PersonalizedChallenge> activeChallenges = new ConcurrentHashMap<>();
    private final Map<String, ListenerRegistration> progressListeners = new ConcurrentHashMap<>();
    
    // Progress tracking listeners
    private ProgressTrackingListener progressListener;
    
    public interface ProgressTrackingListener {
        void onProgressUpdated(String challengeId, double progress);
        void onMilestoneReached(String challengeId, PersonalizedChallenge.Milestone milestone);
        void onChallengeCompleted(String challengeId, PersonalizedChallenge challenge);
        void onChallengeFailed(String challengeId, String reason);
        void onHintAvailable(String challengeId, String hint);
    }
    
    public ChallengeProgressManager(Context context, PlayerBehaviorAnalyzer behaviorAnalyzer) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.behaviorAnalyzer = behaviorAnalyzer;
        
        Log.d(TAG, "Challenge progress manager initialized");
    }
    
    /**
     * Gets all active challenges for a player
     */
    public CompletableFuture<List<PersonalizedChallenge>> getActiveChallenges(String playerId) {
        CompletableFuture<List<PersonalizedChallenge>> future = new CompletableFuture<>();
        
        firestore.collection(CHALLENGES_COLLECTION)
                .whereEqualTo("playerId", playerId)
                .whereEqualTo("completed", false)
                .whereEqualTo("failed", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PersonalizedChallenge> challenges = new ArrayList<>();
                    
                    querySnapshot.forEach(document -> {
                        PersonalizedChallenge challenge = document.toObject(PersonalizedChallenge.class);
                        if (challenge != null) {
                            challenges.add(challenge);
                            activeChallenges.put(challenge.getChallengeId(), challenge);
                        }
                    });
                    
                    future.complete(challenges);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get active challenges", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Starts tracking progress for a challenge
     */
    public CompletableFuture<Void> startChallenge(String challengeId, String playerId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Update challenge start time
        Map<String, Object> updates = new HashMap<>();
        updates.put("startTime", System.currentTimeMillis());
        updates.put("updatedAt", System.currentTimeMillis());
        
        firestore.collection(CHALLENGES_COLLECTION)
                .document(challengeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Start real-time progress tracking
                    startProgressTracking(challengeId, playerId);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start challenge", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Updates challenge progress
     */
    public CompletableFuture<Void> updateProgress(String challengeId, String playerId, 
                                                Map<String, Object> progressData) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Get current challenge
        PersonalizedChallenge challenge = activeChallenges.get(challengeId);
        if (challenge == null) {
            future.completeExceptionally(new Exception("Challenge not found: " + challengeId));
            return future;
        }
        
        // Calculate new progress
        double newProgress = calculateProgress(challenge, progressData);
        
        // Update progress in challenge
        PersonalizedChallenge.ChallengeProgress progress = challenge.getProgress();
        if (progress == null) {
            progress = new PersonalizedChallenge.ChallengeProgress();
            challenge.setProgress(progress);
        }
        
        double oldProgress = progress.getCompletionPercentage();
        progress.setCompletionPercentage(newProgress);
        progress.setLastUpdated(System.currentTimeMillis());
        progress.getCurrentValues().putAll(progressData);
        
        // Check for milestone completion
        checkMilestones(challenge, newProgress);
        
        // Check for challenge completion
        if (newProgress >= 1.0 && !challenge.isCompleted()) {
            completeChallenge(challengeId, playerId, newProgress);
        }
        
        // Update in Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("progress", progress);
        updates.put("updatedAt", System.currentTimeMillis());
        
        firestore.collection(CHALLENGES_COLLECTION)
                .document(challengeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Notify progress listener
                    if (progressListener != null) {
                        progressListener.onProgressUpdated(challengeId, newProgress);
                    }
                    
                    // Check if hint should be provided
                    checkForHints(challenge, oldProgress, newProgress);
                    
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update progress", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Completes a challenge
     */
    public CompletableFuture<Void> completeChallenge(String challengeId, String playerId, double completionScore) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        PersonalizedChallenge challenge = activeChallenges.get(challengeId);
        if (challenge == null) {
            future.completeExceptionally(new Exception("Challenge not found: " + challengeId));
            return future;
        }
        
        // Update challenge completion
        challenge.setCompleted(true);
        challenge.setEndTime(System.currentTimeMillis());
        challenge.setCompletionScore(completionScore);
        challenge.setUpdatedAt(System.currentTimeMillis());
        
        // Calculate rewards
        calculateAndAwardRewards(challenge, completionScore);
        
        // Update in Firestore
        firestore.collection(CHALLENGES_COLLECTION)
                .document(challengeId)
                .set(challenge)
                .addOnSuccessListener(aVoid -> {
                    // Stop progress tracking
                    stopProgressTracking(challengeId);
                    
                    // Remove from active challenges
                    activeChallenges.remove(challengeId);
                    
                    // Notify completion
                    if (progressListener != null) {
                        progressListener.onChallengeCompleted(challengeId, challenge);
                    }
                    
                    // Record behavior analytics
                    recordChallengeCompletion(challenge, playerId);
                    
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to complete challenge", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Abandons a challenge
     */
    public CompletableFuture<Void> abandonChallenge(String challengeId, String playerId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        PersonalizedChallenge challenge = activeChallenges.get(challengeId);
        if (challenge == null) {
            future.completeExceptionally(new Exception("Challenge not found: " + challengeId));
            return future;
        }
        
        // Mark as failed
        challenge.setFailed(true);
        challenge.setEndTime(System.currentTimeMillis());
        challenge.setUpdatedAt(System.currentTimeMillis());
        
        // Update in Firestore
        firestore.collection(CHALLENGES_COLLECTION)
                .document(challengeId)
                .set(challenge)
                .addOnSuccessListener(aVoid -> {
                    // Stop progress tracking
                    stopProgressTracking(challengeId);
                    
                    // Remove from active challenges
                    activeChallenges.remove(challengeId);
                    
                    // Notify failure
                    if (progressListener != null) {
                        progressListener.onChallengeFailed(challengeId, "Abandoned by player");
                    }
                    
                    // Record behavior analytics
                    recordChallengeAbandonment(challenge, playerId);
                    
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to abandon challenge", e);
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Sets progress tracking listener
     */
    public void setProgressTrackingListener(ProgressTrackingListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Remove all progress listeners
        for (ListenerRegistration registration : progressListeners.values()) {
            registration.remove();
        }
        progressListeners.clear();
        activeChallenges.clear();
        
        Log.d(TAG, "Challenge progress manager cleaned up");
    }
    
    // Private helper methods
    private void startProgressTracking(String challengeId, String playerId) {
        // Set up real-time listener for challenge updates
        ListenerRegistration registration = firestore.collection(CHALLENGES_COLLECTION)
                .document(challengeId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Progress tracking error", e);
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        PersonalizedChallenge updatedChallenge = documentSnapshot.toObject(PersonalizedChallenge.class);
                        if (updatedChallenge != null) {
                            activeChallenges.put(challengeId, updatedChallenge);
                        }
                    }
                });
        
        progressListeners.put(challengeId, registration);
    }
    
    private void stopProgressTracking(String challengeId) {
        ListenerRegistration registration = progressListeners.remove(challengeId);
        if (registration != null) {
            registration.remove();
        }
    }
    
    private double calculateProgress(PersonalizedChallenge challenge, Map<String, Object> progressData) {
        // This is a simplified progress calculation
        // In a real implementation, this would be more sophisticated based on challenge type
        
        PersonalizedChallenge.ChallengeProgress progress = challenge.getProgress();
        Map<String, Object> targetValues = progress.getTargetValues();
        
        if (targetValues.isEmpty()) {
            // Initialize target values based on challenge parameters
            initializeTargetValues(challenge);
            targetValues = progress.getTargetValues();
        }
        
        double totalProgress = 0.0;
        int valueCount = 0;
        
        for (Map.Entry<String, Object> entry : targetValues.entrySet()) {
            String key = entry.getKey();
            Object targetValue = entry.getValue();
            Object currentValue = progressData.get(key);
            
            if (currentValue != null && targetValue instanceof Number && currentValue instanceof Number) {
                double target = ((Number) targetValue).doubleValue();
                double current = ((Number) currentValue).doubleValue();
                
                if (target > 0) {
                    double itemProgress = Math.min(1.0, current / target);
                    totalProgress += itemProgress;
                    valueCount++;
                }
            }
        }
        
        return valueCount > 0 ? totalProgress / valueCount : 0.0;
    }
    
    private void initializeTargetValues(PersonalizedChallenge challenge) {
        PersonalizedChallenge.ChallengeProgress progress = challenge.getProgress();
        Map<String, Object> targetValues = progress.getTargetValues();
        Map<String, Object> parameters = challenge.getParameters();
        
        // Set target values based on challenge parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            switch (key) {
                case "count":
                case "distance":
                case "time":
                case "accuracy":
                    targetValues.put(key, value);
                    break;
            }
        }
        
        // Set default current values
        Map<String, Object> currentValues = progress.getCurrentValues();
        for (String key : targetValues.keySet()) {
            currentValues.putIfAbsent(key, 0);
        }
    }
    
    private void checkMilestones(PersonalizedChallenge challenge, double newProgress) {
        List<PersonalizedChallenge.Milestone> milestones = challenge.getMilestones();
        
        for (PersonalizedChallenge.Milestone milestone : milestones) {
            if (!milestone.isCompleted() && newProgress >= milestone.getTargetValue()) {
                // Mark milestone as completed
                milestone.setCompleted(true);
                milestone.setCompletedAt(System.currentTimeMillis());
                
                // Notify listener
                if (progressListener != null) {
                    progressListener.onMilestoneReached(challenge.getChallengeId(), milestone);
                }
                
                Log.d(TAG, "Milestone reached: " + milestone.getName());
            }
        }
    }
    
    private void checkForHints(PersonalizedChallenge challenge, double oldProgress, double newProgress) {
        // Provide hints if player is struggling
        if (newProgress < 0.3 && System.currentTimeMillis() - challenge.getStartTime() > 600000) { // 10 minutes
            String hint = generateHint(challenge, newProgress);
            if (hint != null && progressListener != null) {
                progressListener.onHintAvailable(challenge.getChallengeId(), hint);
            }
        }
    }
    
    private String generateHint(PersonalizedChallenge challenge, double progress) {
        // Generate contextual hints based on challenge type and progress
        switch (challenge.getType()) {
            case "treasure_hunt":
                return "💡 Try exploring areas with higher treasure density. Check your map for hotspots!";
            case "exploration":
                return "💡 Consider visiting landmarks or popular locations to discover new areas faster.";
            case "speed_challenge":
                return "💡 Focus on efficiency over perfection. Quick decisions often lead to better times!";
            case "accuracy_challenge":
                return "💡 Take your time with each action. Accuracy is more important than speed here.";
            case "social_challenge":
                return "💡 Join active chat channels or help other players to boost your social interactions.";
            default:
                return "💡 Check your challenge details for specific tips and strategies.";
        }
    }
    
    private void calculateAndAwardRewards(PersonalizedChallenge challenge, double completionScore) {
        PersonalizedChallenge.ChallengeRewards rewards = challenge.getRewards();
        
        // Apply completion score multiplier
        int finalExperience = (int) (rewards.getBaseExperience() * completionScore * rewards.getRewardMultiplier());
        int finalCoins = (int) (rewards.getBaseCoins() * completionScore * rewards.getRewardMultiplier());
        
        rewards.setBaseExperience(finalExperience);
        rewards.setBaseCoins(finalCoins);
        
        // Check for bonus rewards
        if (completionScore >= 0.9 && !challenge.getBonusConditions().isEmpty()) {
            challenge.setBonusEarned(true);
            rewards.getBonusRewards().put("bonus_experience", (int) (finalExperience * 0.5));
            rewards.getBonusRewards().put("bonus_coins", (int) (finalCoins * 0.3));
        }
    }
    
    private void recordChallengeCompletion(PersonalizedChallenge challenge, String playerId) {
        long completionTime = challenge.getEndTime() - challenge.getStartTime();
        
        behaviorAnalyzer.recordChallengeCompletionBehavior(
                playerId,
                challenge.getType(),
                challenge.getDifficulty(),
                completionTime,
                true,
                challenge.getCompletionScore()
        );
    }
    
    private void recordChallengeAbandonment(PersonalizedChallenge challenge, String playerId) {
        long attemptTime = System.currentTimeMillis() - challenge.getStartTime();
        
        behaviorAnalyzer.recordChallengeCompletionBehavior(
                playerId,
                challenge.getType(),
                challenge.getDifficulty(),
                attemptTime,
                false,
                challenge.getProgress().getCompletionPercentage()
        );
    }
}
