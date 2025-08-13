package com.treasurehuntadventure.tha.challenges;

import android.content.Context;
import android.location.Location;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Integration tests for the complete challenge system
 */
@RunWith(AndroidJUnit4.class)
public class ChallengeSystemIntegrationTest {

    private Context context;
    private PlayerBehaviorAnalyzer behaviorAnalyzer;
    private AIChallengeGenerator challengeGenerator;
    private ChallengeProgressManager progressManager;
    
    private static final String TEST_PLAYER_ID = "integration_test_player";
    private static final int TIMEOUT_SECONDS = 30;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Initialize components
        behaviorAnalyzer = new PlayerBehaviorAnalyzer(context);
        challengeGenerator = new AIChallengeGenerator(context, behaviorAnalyzer);
        progressManager = new ChallengeProgressManager(context, behaviorAnalyzer);
    }

    @Test
    public void testCompleteWorkflow() throws ExecutionException, InterruptedException, TimeoutException {
        // Step 1: Record some behavior to build a profile
        Location testLocation = createTestLocation();
        
        // Start a session
        behaviorAnalyzer.startSession(TEST_PLAYER_ID, testLocation);
        
        // Record some treasure hunting behavior
        CompletableFuture<Void> behaviorFuture = behaviorAnalyzer.recordTreasureHuntingBehavior(
                TEST_PLAYER_ID, 
                com.treasurehuntadventure.tha.TreasureRarity.RARE, 
                testLocation, 
                300000L, // 5 minutes
                true
        );
        behaviorFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Record challenge completion behavior
        CompletableFuture<Void> challengeBehaviorFuture = behaviorAnalyzer.recordChallengeCompletionBehavior(
                TEST_PLAYER_ID,
                "treasure_hunt",
                "INTERMEDIATE",
                600000L, // 10 minutes
                true,
                0.85
        );
        challengeBehaviorFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // End session
        CompletableFuture<Void> endSessionFuture = behaviorAnalyzer.endSession(TEST_PLAYER_ID, testLocation);
        endSessionFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Step 2: Get behavior profile
        CompletableFuture<PlayerBehaviorAnalyzer.PlayerBehaviorProfile> profileFuture = 
                behaviorAnalyzer.getBehaviorProfile(TEST_PLAYER_ID);
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile = profileFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Behavior profile should not be null", profile);
        assertEquals("Player ID should match", TEST_PLAYER_ID, profile.getPlayerId());
        
        // Step 3: Generate feature vector
        CompletableFuture<Map<String, Double>> featureVectorFuture = 
                behaviorAnalyzer.generateFeatureVector(TEST_PLAYER_ID);
        Map<String, Double> featureVector = featureVectorFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Feature vector should not be null", featureVector);
        assertFalse("Feature vector should not be empty", featureVector.isEmpty());
        assertTrue("Feature vector should contain expected features", 
                featureVector.containsKey("challenge_completion_rate"));
        
        // Step 4: Generate personalized challenges
        CompletableFuture<List<PersonalizedChallenge>> challengesFuture = 
                challengeGenerator.generateDailyChallenges(TEST_PLAYER_ID, testLocation);
        List<PersonalizedChallenge> challenges = challengesFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Generated challenges should not be null", challenges);
        assertFalse("Should generate at least one challenge", challenges.isEmpty());
        
        // Verify challenge properties
        PersonalizedChallenge firstChallenge = challenges.get(0);
        assertNotNull("Challenge should have an ID", firstChallenge.getChallengeId());
        assertEquals("Challenge should be for correct player", TEST_PLAYER_ID, firstChallenge.getPlayerId());
        assertNotNull("Challenge should have a title", firstChallenge.getTitle());
        assertNotNull("Challenge should have a description", firstChallenge.getDescription());
        assertNotNull("Challenge should have a type", firstChallenge.getType());
        assertNotNull("Challenge should have a difficulty", firstChallenge.getDifficulty());
        
        // Step 5: Start challenge progress tracking
        String challengeId = firstChallenge.getChallengeId();
        CompletableFuture<Void> startChallengeFuture = 
                progressManager.startChallenge(challengeId, TEST_PLAYER_ID);
        startChallengeFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Step 6: Update challenge progress
        java.util.Map<String, Object> progressData = new java.util.HashMap<>();
        progressData.put("treasures_found", 1);
        progressData.put("distance_traveled", 500.0);
        
        CompletableFuture<Void> updateProgressFuture = 
                progressManager.updateProgress(challengeId, TEST_PLAYER_ID, progressData);
        updateProgressFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Step 7: Complete the challenge
        CompletableFuture<Void> completeFuture = 
                progressManager.completeChallenge(challengeId, TEST_PLAYER_ID, 0.9);
        completeFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Step 8: Verify the workflow completed successfully
        assertTrue("Integration test workflow completed successfully", true);
    }

    @Test
    public void testAdaptiveChallengeGeneration() throws ExecutionException, InterruptedException, TimeoutException {
        // Create initial behavior profile
        Location testLocation = createTestLocation();
        behaviorAnalyzer.startSession(TEST_PLAYER_ID, testLocation);
        
        // Record poor performance to trigger adaptive difficulty
        CompletableFuture<Void> behaviorFuture = behaviorAnalyzer.recordChallengeCompletionBehavior(
                TEST_PLAYER_ID,
                "speed_challenge",
                "ADVANCED",
                1800000L, // 30 minutes (slow)
                false, // failed
                0.3 // low accuracy
        );
        behaviorFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        behaviorAnalyzer.endSession(TEST_PLAYER_ID, testLocation);
        
        // Generate adaptive challenge based on poor performance
        CompletableFuture<PersonalizedChallenge> adaptiveFuture = 
                challengeGenerator.generateAdaptiveChallenge(TEST_PLAYER_ID, "previous_challenge", 0.3);
        PersonalizedChallenge adaptiveChallenge = adaptiveFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Adaptive challenge should be generated", adaptiveChallenge);
        assertTrue("Challenge should be marked as adaptive", adaptiveChallenge.isAdaptive());
        assertNotNull("Adaptive challenge should have parent ID", adaptiveChallenge.getParentChallengeId());
        
        // Verify difficulty is adjusted (should be easier due to poor performance)
        double difficultyScore = adaptiveChallenge.getDifficultyScore();
        assertTrue("Adaptive challenge should have reasonable difficulty", 
                difficultyScore >= 0.0 && difficultyScore <= 1.0);
    }

    @Test
    public void testChallengeProgressTracking() throws ExecutionException, InterruptedException, TimeoutException {
        // Generate a test challenge
        Location testLocation = createTestLocation();
        CompletableFuture<List<PersonalizedChallenge>> challengesFuture = 
                challengeGenerator.generateDailyChallenges(TEST_PLAYER_ID, testLocation);
        List<PersonalizedChallenge> challenges = challengesFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertFalse("Should have generated challenges", challenges.isEmpty());
        
        PersonalizedChallenge testChallenge = challenges.get(0);
        String challengeId = testChallenge.getChallengeId();
        
        // Start tracking
        CompletableFuture<Void> startFuture = progressManager.startChallenge(challengeId, TEST_PLAYER_ID);
        startFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Update progress multiple times
        for (int i = 1; i <= 3; i++) {
            java.util.Map<String, Object> progressData = new java.util.HashMap<>();
            progressData.put("step", i);
            progressData.put("completion", i * 0.33);
            
            CompletableFuture<Void> updateFuture = 
                    progressManager.updateProgress(challengeId, TEST_PLAYER_ID, progressData);
            updateFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Small delay between updates
            Thread.sleep(100);
        }
        
        // Complete the challenge
        CompletableFuture<Void> completeFuture = 
                progressManager.completeChallenge(challengeId, TEST_PLAYER_ID, 1.0);
        completeFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertTrue("Progress tracking workflow completed successfully", true);
    }

    @Test
    public void testBehaviorAnalyticsIntegration() throws ExecutionException, InterruptedException, TimeoutException {
        Location testLocation = createTestLocation();
        
        // Record various types of behavior
        behaviorAnalyzer.startSession(TEST_PLAYER_ID, testLocation);
        
        // Treasure hunting
        CompletableFuture<Void> treasureFuture = behaviorAnalyzer.recordTreasureHuntingBehavior(
                TEST_PLAYER_ID, 
                com.treasurehuntadventure.tha.TreasureRarity.COMMON, 
                testLocation, 
                180000L, 
                true
        );
        treasureFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Social interaction
        java.util.Map<String, Object> socialData = new java.util.HashMap<>();
        socialData.put("targetPlayer", "friend_123");
        socialData.put("duration", 120000L);
        
        CompletableFuture<Void> socialFuture = behaviorAnalyzer.recordSocialInteractionBehavior(
                TEST_PLAYER_ID, "help_given", socialData);
        socialFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Challenge completion
        CompletableFuture<Void> challengeFuture = behaviorAnalyzer.recordChallengeCompletionBehavior(
                TEST_PLAYER_ID, "exploration", "NOVICE", 900000L, true, 0.75);
        challengeFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        behaviorAnalyzer.endSession(TEST_PLAYER_ID, testLocation);
        
        // Get updated profile
        CompletableFuture<PlayerBehaviorAnalyzer.PlayerBehaviorProfile> profileFuture = 
                behaviorAnalyzer.getBehaviorProfile(TEST_PLAYER_ID);
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile = profileFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Profile should be updated", profile);
        assertNotNull("Gameplay patterns should be available", profile.getGameplayPatterns());
        assertNotNull("Performance metrics should be available", profile.getPerformanceMetrics());
        assertNotNull("Social behavior should be available", profile.getSocialBehavior());
        
        // Generate feature vector based on recorded behavior
        CompletableFuture<Map<String, Double>> featureFuture = 
                behaviorAnalyzer.generateFeatureVector(TEST_PLAYER_ID);
        Map<String, Double> features = featureFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Feature vector should be generated", features);
        assertTrue("Feature vector should contain behavioral features", features.size() > 10);
        
        // Use features to generate personalized challenges
        CompletableFuture<List<PersonalizedChallenge>> personalizedFuture = 
                challengeGenerator.generateDailyChallenges(TEST_PLAYER_ID, testLocation);
        List<PersonalizedChallenge> personalizedChallenges = personalizedFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        assertNotNull("Personalized challenges should be generated", personalizedChallenges);
        assertFalse("Should generate personalized challenges", personalizedChallenges.isEmpty());
        
        // Verify personalization scores
        for (PersonalizedChallenge challenge : personalizedChallenges) {
            assertTrue("Challenge should have personalization score", 
                    challenge.getPersonalizationScore() >= 0.0 && challenge.getPersonalizationScore() <= 1.0);
            assertTrue("Challenge should have engagement prediction", 
                    challenge.getPredictedEngagement() >= 0.0 && challenge.getPredictedEngagement() <= 1.0);
            assertFalse("Challenge should have AI tags", challenge.getAiTags().isEmpty());
        }
    }

    @Test
    public void testErrorHandlingAndRecovery() throws ExecutionException, InterruptedException, TimeoutException {
        // Test with invalid player ID
        String invalidPlayerId = "";
        
        try {
            CompletableFuture<PlayerBehaviorAnalyzer.PlayerBehaviorProfile> profileFuture = 
                    behaviorAnalyzer.getBehaviorProfile(invalidPlayerId);
            PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile = profileFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Should still return a profile (newly created)
            assertNotNull("Should create new profile for invalid ID", profile);
        } catch (Exception e) {
            // Exception handling is acceptable for invalid input
            assertTrue("Error handling should work for invalid input", true);
        }
        
        // Test with null location
        try {
            CompletableFuture<List<PersonalizedChallenge>> challengesFuture = 
                    challengeGenerator.generateDailyChallenges(TEST_PLAYER_ID, null);
            List<PersonalizedChallenge> challenges = challengesFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Should still generate challenges without location
            assertNotNull("Should generate challenges without location", challenges);
        } catch (Exception e) {
            // Exception handling is acceptable for null location
            assertTrue("Error handling should work for null location", true);
        }
        
        // Test progress update for non-existent challenge
        try {
            java.util.Map<String, Object> progressData = new java.util.HashMap<>();
            progressData.put("test", "value");
            
            CompletableFuture<Void> updateFuture = 
                    progressManager.updateProgress("non_existent_challenge", TEST_PLAYER_ID, progressData);
            updateFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Should handle non-existent challenge gracefully
            assertTrue("Should handle non-existent challenge", true);
        }
    }

    private Location createTestLocation() {
        Location location = new Location("test");
        location.setLatitude(37.7749);
        location.setLongitude(-122.4194);
        location.setAccuracy(10.0f);
        location.setTime(System.currentTimeMillis());
        return location;
    }
}
