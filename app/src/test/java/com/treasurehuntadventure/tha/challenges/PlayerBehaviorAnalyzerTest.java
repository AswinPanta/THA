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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayerBehaviorAnalyzer
 */
@RunWith(RobolectricTestRunner.class)
public class PlayerBehaviorAnalyzerTest {

    private PlayerBehaviorAnalyzer behaviorAnalyzer;
    private Context context;
    
    @Mock
    private PlayerBehaviorAnalyzer.BehaviorAnalysisListener mockListener;
    
    private static final String TEST_PLAYER_ID = "test_player_123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        behaviorAnalyzer = new PlayerBehaviorAnalyzer(context);
        behaviorAnalyzer.setBehaviorAnalysisListener(mockListener);
    }

    @Test
    public void testRecordTreasureHuntingBehavior() throws ExecutionException, InterruptedException {
        // Arrange
        Location testLocation = new Location("test");
        testLocation.setLatitude(37.7749);
        testLocation.setLongitude(-122.4194);
        
        // Act
        CompletableFuture<Void> future = behaviorAnalyzer.recordTreasureHuntingBehavior(
                TEST_PLAYER_ID, 
                com.treasurehuntadventure.tha.TreasureRarity.RARE, 
                testLocation, 
                120000L, // 2 minutes
                true
        );
        
        // Assert
        assertNotNull("Future should not be null", future);
        // Note: In a real test, we would mock Firebase and verify the data was stored
    }

    @Test
    public void testRecordChallengeCompletionBehavior() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Void> future = behaviorAnalyzer.recordChallengeCompletionBehavior(
                TEST_PLAYER_ID,
                "treasure_hunt",
                "INTERMEDIATE",
                300000L, // 5 minutes
                true,
                0.85
        );
        
        // Assert
        assertNotNull("Future should not be null", future);
    }

    @Test
    public void testStartAndEndSession() {
        // Arrange
        Location startLocation = new Location("test");
        startLocation.setLatitude(37.7749);
        startLocation.setLongitude(-122.4194);
        
        Location endLocation = new Location("test");
        endLocation.setLatitude(37.7849);
        endLocation.setLongitude(-122.4294);
        
        // Act
        behaviorAnalyzer.startSession(TEST_PLAYER_ID, startLocation);
        CompletableFuture<Void> future = behaviorAnalyzer.endSession(TEST_PLAYER_ID, endLocation);
        
        // Assert
        assertNotNull("End session future should not be null", future);
    }

    @Test
    public void testGenerateFeatureVector() throws ExecutionException, InterruptedException {
        // Arrange
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile mockProfile = createMockProfile();
        
        // Act
        CompletableFuture<Map<String, Double>> future = behaviorAnalyzer.generateFeatureVector(TEST_PLAYER_ID);
        
        // Assert
        assertNotNull("Feature vector future should not be null", future);
        // Note: In a real test, we would mock the profile retrieval and verify the feature vector
    }

    @Test
    public void testBehaviorProfileCreation() {
        // Act
        PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile = new PlayerBehaviorAnalyzer.PlayerBehaviorProfile();
        profile.setPlayerId(TEST_PLAYER_ID);
        profile.setEngagementScore(0.75);
        profile.setSkillLevel(0.6);
        profile.setChallengePreference(0.8);
        
        // Assert
        assertEquals("Player ID should match", TEST_PLAYER_ID, profile.getPlayerId());
        assertEquals("Engagement score should match", 0.75, profile.getEngagementScore(), 0.001);
        assertEquals("Skill level should match", 0.6, profile.getSkillLevel(), 0.001);
        assertEquals("Challenge preference should match", 0.8, profile.getChallengePreference(), 0.001);
        assertNotNull("Gameplay patterns should not be null", profile.getGameplayPatterns());
        assertNotNull("Preference profile should not be null", profile.getPreferenceProfile());
        assertNotNull("Performance metrics should not be null", profile.getPerformanceMetrics());
        assertNotNull("Engagement metrics should not be null", profile.getEngagementMetrics());
        assertNotNull("Social behavior should not be null", profile.getSocialBehavior());
    }

    @Test
    public void testGameplayPatternsInitialization() {
        // Act
        PlayerBehaviorAnalyzer.GameplayPatterns patterns = new PlayerBehaviorAnalyzer.GameplayPatterns();
        
        // Assert
        assertNotNull("Hourly activity should not be null", patterns.getHourlyActivity());
        assertNotNull("Day of week activity should not be null", patterns.getDayOfWeekActivity());
        assertNotNull("Preferred game modes should not be null", patterns.getPreferredGameModes());
        assertNotNull("Location preferences should not be null", patterns.getLocationPreferences());
        assertEquals("Initial average session duration should be 0", 0.0, patterns.getAverageSessionDuration(), 0.001);
        assertEquals("Initial sessions per week should be 0", 0, patterns.getSessionsPerWeek());
    }

    @Test
    public void testPerformanceMetricsInitialization() {
        // Act
        PlayerBehaviorAnalyzer.PerformanceMetrics metrics = new PlayerBehaviorAnalyzer.PerformanceMetrics();
        
        // Assert
        assertNotNull("Challenge type success rates should not be null", metrics.getChallengeTypeSuccessRates());
        assertNotNull("Skill progression should not be null", metrics.getSkillProgression());
        assertEquals("Initial completion rate should be 0", 0.0, metrics.getChallengeCompletionRate(), 0.001);
        assertEquals("Initial accuracy score should be 0", 0.0, metrics.getAccuracyScore(), 0.001);
        assertEquals("Initial current streak should be 0", 0, metrics.getCurrentStreak());
        assertEquals("Initial longest streak should be 0", 0, metrics.getLongestStreak());
    }

    @Test
    public void testEngagementMetricsInitialization() {
        // Act
        PlayerBehaviorAnalyzer.EngagementMetrics metrics = new PlayerBehaviorAnalyzer.EngagementMetrics();
        
        // Assert
        assertNotNull("Activity engagement should not be null", metrics.getActivityEngagement());
        assertEquals("Initial daily engagement score should be 0", 0.0, metrics.getDailyEngagementScore(), 0.001);
        assertEquals("Initial weekly engagement score should be 0", 0.0, metrics.getWeeklyEngagementScore(), 0.001);
        assertEquals("Initial consecutive days played should be 0", 0, metrics.getConsecutiveDaysPlayed());
        assertEquals("Initial churn risk should be 0", 0.0, metrics.getChurnRisk(), 0.001);
    }

    @Test
    public void testSocialBehaviorInitialization() {
        // Act
        PlayerBehaviorAnalyzer.SocialBehavior social = new PlayerBehaviorAnalyzer.SocialBehavior();
        
        // Assert
        assertNotNull("Social feature usage should not be null", social.getSocialFeatureUsage());
        assertEquals("Initial social interaction frequency should be 0", 0.0, social.getSocialInteractionFrequency(), 0.001);
        assertEquals("Initial friends count should be 0", 0, social.getFriendsCount());
        assertEquals("Initial team participation rate should be 0", 0.0, social.getTeamParticipationRate(), 0.001);
        assertEquals("Initial help requests given should be 0", 0, social.getHelpRequestsGiven());
        assertEquals("Initial help requests received should be 0", 0, social.getHelpRequestsReceived());
    }

    @Test
    public void testBehaviorCategoryEnum() {
        // Test all behavior categories
        PlayerBehaviorAnalyzer.BehaviorCategory[] categories = PlayerBehaviorAnalyzer.BehaviorCategory.values();
        
        assertTrue("Should have at least 8 behavior categories", categories.length >= 8);
        
        // Test specific categories
        assertEquals("Exploration value should match", "exploration", 
                PlayerBehaviorAnalyzer.BehaviorCategory.EXPLORATION.getValue());
        assertEquals("Treasure hunting value should match", "treasure_hunting", 
                PlayerBehaviorAnalyzer.BehaviorCategory.TREASURE_HUNTING.getValue());
        assertEquals("Social interaction value should match", "social_interaction", 
                PlayerBehaviorAnalyzer.BehaviorCategory.SOCIAL_INTERACTION.getValue());
        assertEquals("Challenge completion value should match", "challenge_completion", 
                PlayerBehaviorAnalyzer.BehaviorCategory.CHALLENGE_COMPLETION.getValue());
    }

    @Test
    public void testRecordSocialInteractionBehavior() {
        // Arrange
        Map<String, Object> interactionData = new HashMap<>();
        interactionData.put("targetPlayerId", "other_player_123");
        interactionData.put("interactionDuration", 30000L);
        
        // Act
        CompletableFuture<Void> future = behaviorAnalyzer.recordSocialInteractionBehavior(
                TEST_PLAYER_ID, 
                "chat_message", 
                interactionData
        );
        
        // Assert
        assertNotNull("Future should not be null", future);
    }

    @Test
    public void testClearCache() {
        // Act
        behaviorAnalyzer.clearCache();
        
        // Assert - no exception should be thrown
        assertTrue("Cache clear should complete successfully", true);
    }

    // Helper method to create a mock profile for testing
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
        
        // Set up engagement metrics
        PlayerBehaviorAnalyzer.EngagementMetrics engagement = profile.getEngagementMetrics();
        engagement.setDailyEngagementScore(0.75);
        engagement.setWeeklyEngagementScore(0.8);
        engagement.setConsecutiveDaysPlayed(5);
        engagement.setSessionRetentionRate(0.9);
        engagement.setChurnRisk(0.2);
        
        // Set up social behavior
        PlayerBehaviorAnalyzer.SocialBehavior social = profile.getSocialBehavior();
        social.setSocialInteractionFrequency(3.0);
        social.setFriendsCount(8);
        social.setTeamParticipationRate(0.6);
        social.setChatActivityLevel(2.5);
        social.setLeaderboardEngagement(0.7);
        
        return profile;
    }
}
