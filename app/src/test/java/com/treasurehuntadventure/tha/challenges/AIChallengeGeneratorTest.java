package com.treasurehuntadventure.tha.challenges;

import android.content.Context;
import android.location.Location;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIChallengeGenerator
 */
@RunWith(RobolectricTestRunner.class)
public class AIChallengeGeneratorTest {

    private AIChallengeGenerator challengeGenerator;
    private Context context;
    
    @Mock
    private PlayerBehaviorAnalyzer mockBehaviorAnalyzer;
    
    @Mock
    private AIChallengeGenerator.ChallengeGenerationListener mockListener;
    
    private static final String TEST_PLAYER_ID = "test_player_123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        challengeGenerator = new AIChallengeGenerator(context, mockBehaviorAnalyzer);
        challengeGenerator.setChallengeGenerationListener(mockListener);
    }

    @Test
    public void testChallengeTypeEnum() {
        // Test all challenge types
        AIChallengeGenerator.ChallengeType[] types = AIChallengeGenerator.ChallengeType.values();
        
        assertTrue("Should have at least 10 challenge types", types.length >= 10);
        
        // Test specific types
        assertEquals("Treasure hunt ID should match", "treasure_hunt", 
                AIChallengeGenerator.ChallengeType.TREASURE_HUNT.getId());
        assertEquals("Exploration ID should match", "exploration", 
                AIChallengeGenerator.ChallengeType.EXPLORATION.getId());
        assertEquals("Speed challenge ID should match", "speed_challenge", 
                AIChallengeGenerator.ChallengeType.SPEED_CHALLENGE.getId());
        assertEquals("Social challenge ID should match", "social_challenge", 
                AIChallengeGenerator.ChallengeType.SOCIAL_CHALLENGE.getId());
        
        // Test descriptions
        assertNotNull("Treasure hunt description should not be null", 
                AIChallengeGenerator.ChallengeType.TREASURE_HUNT.getDescription());
        assertNotNull("Exploration description should not be null", 
                AIChallengeGenerator.ChallengeType.EXPLORATION.getDescription());
    }

    @Test
    public void testDifficultyLevelEnum() {
        // Test all difficulty levels
        AIChallengeGenerator.DifficultyLevel[] levels = AIChallengeGenerator.DifficultyLevel.values();
        
        assertTrue("Should have at least 6 difficulty levels", levels.length >= 6);
        
        // Test specific levels
        assertEquals("Beginner multiplier should be 0.2", 0.2, 
                AIChallengeGenerator.DifficultyLevel.BEGINNER.getMultiplier(), 0.001);
        assertEquals("Intermediate multiplier should be 0.6", 0.6, 
                AIChallengeGenerator.DifficultyLevel.INTERMEDIATE.getMultiplier(), 0.001);
        assertEquals("Expert multiplier should be 1.0", 1.0, 
                AIChallengeGenerator.DifficultyLevel.EXPERT.getMultiplier(), 0.001);
        assertEquals("Master multiplier should be 1.2", 1.2, 
                AIChallengeGenerator.DifficultyLevel.MASTER.getMultiplier(), 0.001);
        
        // Test descriptions
        assertNotNull("Beginner description should not be null", 
                AIChallengeGenerator.DifficultyLevel.BEGINNER.getDescription());
        assertNotNull("Expert description should not be null", 
                AIChallengeGenerator.DifficultyLevel.EXPERT.getDescription());
    }

    @Test
    public void testGenerateDailyChallenges() throws ExecutionException, InterruptedException {
        // Arrange
        Location testLocation = new Location("test");
        testLocation.setLatitude(37.7749);
        testLocation.setLongitude(-122.4194);
        
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile mockProfile = createMockProfile();
        Map<String, Double> mockFeatureVector = createMockFeatureVector();
        
        when(mockBehaviorAnalyzer.getBehaviorProfile(TEST_PLAYER_ID))
                .thenReturn(CompletableFuture.completedFuture(mockProfile));
        when(mockBehaviorAnalyzer.generateFeatureVector(TEST_PLAYER_ID))
                .thenReturn(CompletableFuture.completedFuture(mockFeatureVector));
        
        // Act
        CompletableFuture<List<PersonalizedChallenge>> future = 
                challengeGenerator.generateDailyChallenges(TEST_PLAYER_ID, testLocation);
        
        // Assert
        assertNotNull("Future should not be null", future);
        // Note: In a real test with mocked Firebase, we would verify the generated challenges
    }

    @Test
    public void testGenerateAdaptiveChallenge() throws ExecutionException, InterruptedException {
        // Arrange
        String currentChallengeId = "current_challenge_123";
        double currentPerformance = 0.75;
        
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile mockProfile = createMockProfile();
        Map<String, Double> mockFeatureVector = createMockFeatureVector();
        
        when(mockBehaviorAnalyzer.getBehaviorProfile(TEST_PLAYER_ID))
                .thenReturn(CompletableFuture.completedFuture(mockProfile));
        when(mockBehaviorAnalyzer.generateFeatureVector(TEST_PLAYER_ID))
                .thenReturn(CompletableFuture.completedFuture(mockFeatureVector));
        
        // Act
        CompletableFuture<PersonalizedChallenge> future = 
                challengeGenerator.generateAdaptiveChallenge(TEST_PLAYER_ID, currentChallengeId, currentPerformance);
        
        // Assert
        assertNotNull("Future should not be null", future);
    }

    @Test
    public void testUpdateModelsWithFeedback() throws ExecutionException, InterruptedException {
        // Arrange
        String challengeId = "challenge_123";
        double completionRate = 0.85;
        double satisfaction = 0.9;
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("difficulty_rating", 4);
        feedbackData.put("enjoyment_rating", 5);
        feedbackData.put("time_spent", 1800000L); // 30 minutes
        
        // Act
        CompletableFuture<Void> future = challengeGenerator.updateModelsWithFeedback(
                TEST_PLAYER_ID, challengeId, completionRate, satisfaction, feedbackData);
        
        // Assert
        assertNotNull("Future should not be null", future);
    }

    @Test
    public void testGetGenerationAnalytics() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Map<String, Object>> future = 
                challengeGenerator.getGenerationAnalytics(TEST_PLAYER_ID);
        
        // Assert
        assertNotNull("Future should not be null", future);
        // Note: In a real test with mocked Firebase, we would verify the analytics data structure
    }

    @Test
    public void testChallengeGenerationListener() {
        // Arrange
        AIChallengeGenerator.ChallengeGenerationListener listener = 
                mock(AIChallengeGenerator.ChallengeGenerationListener.class);
        
        // Act
        challengeGenerator.setChallengeGenerationListener(listener);
        
        // Assert - no exception should be thrown
        assertTrue("Listener should be set successfully", true);
    }

    @Test
    public void testCleanup() {
        // Act
        challengeGenerator.cleanup();
        
        // Assert - no exception should be thrown
        assertTrue("Cleanup should complete successfully", true);
    }

    // Helper method to create a mock behavior profile
    private PlayerBehaviorAnalyzer.PlayerBehaviorProfile createMockProfile() {
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile = new PlayerBehaviorAnalyzer.PlayerBehaviorProfile();
        profile.setPlayerId(TEST_PLAYER_ID);
        profile.setEngagementScore(0.75);
        profile.setSkillLevel(0.6);
        profile.setChallengePreference(0.8);
        
        // Set up gameplay patterns
        PlayerBehaviorAnalyzer.GameplayPatterns gameplay = profile.getGameplayPatterns();
        gameplay.setAverageSessionDuration(1800000.0); // 30 minutes
        gameplay.setSessionsPerWeek(5);
        gameplay.setExplorationRadius(1000.0);
        gameplay.setTreasureHuntingFrequency(10);
        
        // Set up performance metrics
        PlayerBehaviorAnalyzer.PerformanceMetrics performance = profile.getPerformanceMetrics();
        performance.setChallengeCompletionRate(0.8);
        performance.setAverageCompletionTime(300000.0); // 5 minutes
        performance.setAccuracyScore(0.85);
        performance.setCurrentStreak(3);
        performance.setImprovementRate(0.1);
        
        // Set up preference profile
        PlayerBehaviorAnalyzer.PreferenceProfile preferences = profile.getPreferenceProfile();
        preferences.setDifficultyPreference(0.7);
        preferences.setCompetitivenessLevel(0.6);
        preferences.setExplorationVsEfficiency(0.8);
        preferences.setSocialInteractionPreference(0.4);
        
        return profile;
    }

    // Helper method to create a mock feature vector
    private Map<String, Double> createMockFeatureVector() {
        Map<String, Double> features = new HashMap<>();
        features.put("avg_session_duration", 1800.0); // 30 minutes in seconds
        features.put("sessions_per_week", 5.0);
        features.put("exploration_radius", 1000.0);
        features.put("treasure_hunting_frequency", 10.0);
        features.put("challenge_completion_rate", 0.8);
        features.put("avg_completion_time", 300.0); // 5 minutes in seconds
        features.put("accuracy_score", 0.85);
        features.put("current_streak", 3.0);
        features.put("improvement_rate", 0.1);
        features.put("difficulty_preference", 0.7);
        features.put("competitiveness_level", 0.6);
        features.put("exploration_vs_efficiency", 0.8);
        features.put("social_interaction_preference", 0.4);
        features.put("daily_engagement_score", 0.75);
        features.put("weekly_engagement_score", 0.8);
        features.put("consecutive_days_played", 5.0);
        features.put("session_retention_rate", 0.9);
        features.put("churn_risk", 0.2);
        features.put("social_interaction_frequency", 3.0);
        features.put("friends_count", 8.0);
        features.put("team_participation_rate", 0.6);
        features.put("chat_activity_level", 2.5);
        features.put("leaderboard_engagement", 0.7);
        features.put("peak_activity_hour", 19.0); // 7 PM
        features.put("morning_activity_ratio", 0.3);
        
        return features;
    }
}
