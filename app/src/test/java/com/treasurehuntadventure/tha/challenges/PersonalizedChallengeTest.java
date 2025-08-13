package com.treasurehuntadventure.tha.challenges;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for PersonalizedChallenge and related classes
 */
@RunWith(RobolectricTestRunner.class)
public class PersonalizedChallengeTest {

    private PersonalizedChallenge challenge;
    private static final String TEST_PLAYER_ID = "test_player_123";
    private static final String TEST_CHALLENGE_TITLE = "Find 3 Rare Treasures";
    private static final String TEST_CHALLENGE_DESCRIPTION = "Discover and capture 3 rare treasures within 2 hours";
    private static final String TEST_CHALLENGE_TYPE = "treasure_hunt";
    private static final String TEST_DIFFICULTY = "INTERMEDIATE";

    @Before
    public void setUp() {
        challenge = new PersonalizedChallenge(
                TEST_PLAYER_ID, 
                TEST_CHALLENGE_TITLE, 
                TEST_CHALLENGE_DESCRIPTION, 
                TEST_CHALLENGE_TYPE, 
                TEST_DIFFICULTY
        );
    }

    @Test
    public void testChallengeCreation() {
        // Assert basic properties
        assertNotNull("Challenge ID should not be null", challenge.getChallengeId());
        assertEquals("Player ID should match", TEST_PLAYER_ID, challenge.getPlayerId());
        assertEquals("Title should match", TEST_CHALLENGE_TITLE, challenge.getTitle());
        assertEquals("Description should match", TEST_CHALLENGE_DESCRIPTION, challenge.getDescription());
        assertEquals("Type should match", TEST_CHALLENGE_TYPE, challenge.getType());
        assertEquals("Difficulty should match", TEST_DIFFICULTY, challenge.getDifficulty());
        
        // Assert default values
        assertFalse("Challenge should not be completed by default", challenge.isCompleted());
        assertFalse("Challenge should not be failed by default", challenge.isFailed());
        assertFalse("Challenge should not be adaptive by default", challenge.isAdaptive());
        assertFalse("Challenge should not be multiplayer by default", challenge.isMultiplayer());
        assertFalse("Challenge should not be competitive by default", challenge.isCompetitive());
        assertFalse("Bonus should not be earned by default", challenge.isBonusEarned());
        
        // Assert collections are initialized
        assertNotNull("AI tags should not be null", challenge.getAiTags());
        assertNotNull("Parameters should not be null", challenge.getParameters());
        assertNotNull("Constraints should not be null", challenge.getConstraints());
        assertNotNull("Prerequisites should not be null", challenge.getPrerequisites());
        assertNotNull("Context should not be null", challenge.getContext());
        assertNotNull("Milestones should not be null", challenge.getMilestones());
        assertNotNull("Bonus conditions should not be null", challenge.getBonusConditions());
        assertNotNull("Adaptation history should not be null", challenge.getAdaptationHistory());
        assertNotNull("Participant IDs should not be null", challenge.getParticipantIds());
        assertNotNull("Social context should not be null", challenge.getSocialContext());
        assertNotNull("Analytics data should not be null", challenge.getAnalyticsData());
        assertNotNull("Player feedback should not be null", challenge.getPlayerFeedback());
        assertNotNull("Metadata should not be null", challenge.getMetadata());
        
        // Assert nested objects are initialized
        assertNotNull("Progress should not be null", challenge.getProgress());
        assertNotNull("Rewards should not be null", challenge.getRewards());
    }

    @Test
    public void testChallengeProgress() {
        PersonalizedChallenge.ChallengeProgress progress = challenge.getProgress();
        
        // Test initial state
        assertEquals("Initial completion percentage should be 0", 0.0, progress.getCompletionPercentage(), 0.001);
        assertNotNull("Current values should not be null", progress.getCurrentValues());
        assertNotNull("Target values should not be null", progress.getTargetValues());
        assertNotNull("Completed steps should not be null", progress.getCompletedSteps());
        assertNotNull("Progress data should not be null", progress.getProgressData());
        
        // Test setting progress
        progress.setCompletionPercentage(0.65);
        progress.setCurrentStep("step_2");
        progress.getCompletedSteps().add("step_1");
        progress.getCurrentValues().put("treasures_found", 2);
        progress.getTargetValues().put("treasures_found", 3);
        
        assertEquals("Completion percentage should be updated", 0.65, progress.getCompletionPercentage(), 0.001);
        assertEquals("Current step should be updated", "step_2", progress.getCurrentStep());
        assertEquals("Completed steps should contain step_1", 1, progress.getCompletedSteps().size());
        assertEquals("Current treasures found should be 2", 2, progress.getCurrentValues().get("treasures_found"));
        assertEquals("Target treasures should be 3", 3, progress.getTargetValues().get("treasures_found"));
    }

    @Test
    public void testChallengeMilestones() {
        // Create test milestones
        PersonalizedChallenge.Milestone milestone1 = new PersonalizedChallenge.Milestone(
                "milestone_1", "First Treasure", "Find your first treasure", 0.33);
        PersonalizedChallenge.Milestone milestone2 = new PersonalizedChallenge.Milestone(
                "milestone_2", "Second Treasure", "Find your second treasure", 0.66);
        
        List<PersonalizedChallenge.Milestone> milestones = new ArrayList<>();
        milestones.add(milestone1);
        milestones.add(milestone2);
        
        challenge.setMilestones(milestones);
        
        // Test milestone properties
        assertEquals("Should have 2 milestones", 2, challenge.getMilestones().size());
        
        PersonalizedChallenge.Milestone firstMilestone = challenge.getMilestones().get(0);
        assertEquals("First milestone ID should match", "milestone_1", firstMilestone.getMilestoneId());
        assertEquals("First milestone name should match", "First Treasure", firstMilestone.getName());
        assertEquals("First milestone description should match", "Find your first treasure", firstMilestone.getDescription());
        assertEquals("First milestone target should be 0.33", 0.33, firstMilestone.getTargetValue(), 0.001);
        assertFalse("First milestone should not be completed", firstMilestone.isCompleted());
        
        // Test milestone completion
        firstMilestone.setCompleted(true);
        firstMilestone.setCompletedAt(System.currentTimeMillis());
        
        assertTrue("First milestone should be completed", firstMilestone.isCompleted());
        assertTrue("Completed at should be set", firstMilestone.getCompletedAt() > 0);
    }

    @Test
    public void testChallengeRewards() {
        PersonalizedChallenge.ChallengeRewards rewards = challenge.getRewards();
        
        // Test initial state
        assertEquals("Initial base experience should be 0", 0, rewards.getBaseExperience());
        assertEquals("Initial base coins should be 0", 0, rewards.getBaseCoins());
        assertEquals("Initial reward multiplier should be 1.0", 1.0, rewards.getRewardMultiplier(), 0.001);
        assertFalse("Should not have rare reward initially", rewards.isHasRareReward());
        assertNotNull("Items list should not be null", rewards.getItems());
        assertNotNull("Badges list should not be null", rewards.getBadges());
        assertNotNull("Bonus rewards should not be null", rewards.getBonusRewards());
        
        // Test setting rewards
        rewards.setBaseExperience(150);
        rewards.setBaseCoins(75);
        rewards.setRewardMultiplier(1.5);
        rewards.setHasRareReward(true);
        rewards.setSpecialReward("Golden Compass");
        rewards.getItems().add("Treasure Map");
        rewards.getBadges().add("Treasure Hunter");
        rewards.getBonusRewards().put("bonus_experience", 50);
        
        assertEquals("Base experience should be updated", 150, rewards.getBaseExperience());
        assertEquals("Base coins should be updated", 75, rewards.getBaseCoins());
        assertEquals("Reward multiplier should be updated", 1.5, rewards.getRewardMultiplier(), 0.001);
        assertTrue("Should have rare reward", rewards.isHasRareReward());
        assertEquals("Special reward should be set", "Golden Compass", rewards.getSpecialReward());
        assertEquals("Should have 1 item", 1, rewards.getItems().size());
        assertEquals("Should have 1 badge", 1, rewards.getBadges().size());
        assertEquals("Should have bonus experience", Integer.valueOf(50), rewards.getBonusRewards().get("bonus_experience"));
    }

    @Test
    public void testChallengeUtilityMethods() {
        // Test isActive method
        assertFalse("Challenge should not be active initially", challenge.isActive());
        
        // Set start time and make it active
        challenge.setStartTime(System.currentTimeMillis());
        challenge.setTimeLimit(3600000L); // 1 hour
        assertTrue("Challenge should be active after setting start time", challenge.isActive());
        
        // Test isExpired method
        assertFalse("Challenge should not be expired", challenge.isExpired());
        
        // Test getRemainingTime method
        long remainingTime = challenge.getRemainingTime();
        assertTrue("Remaining time should be positive", remainingTime > 0);
        assertTrue("Remaining time should be less than time limit", remainingTime <= 3600000L);
        
        // Test getTimeProgress method
        double timeProgress = challenge.getTimeProgress();
        assertTrue("Time progress should be between 0 and 1", timeProgress >= 0.0 && timeProgress <= 1.0);
        
        // Test completion
        challenge.setCompleted(true);
        assertFalse("Completed challenge should not be active", challenge.isActive());
        
        // Test failure
        challenge.setCompleted(false);
        challenge.setFailed(true);
        assertFalse("Failed challenge should not be active", challenge.isActive());
    }

    @Test
    public void testChallengeEmojiMethods() {
        // Test difficulty emoji
        challenge.setDifficulty("BEGINNER");
        assertEquals("Beginner difficulty should have green emoji", "🟢", challenge.getDifficultyEmoji());
        
        challenge.setDifficulty("EXPERT");
        assertEquals("Expert difficulty should have red emoji", "🔴", challenge.getDifficultyEmoji());
        
        challenge.setDifficulty("UNKNOWN");
        assertEquals("Unknown difficulty should have white emoji", "⚪", challenge.getDifficultyEmoji());
        
        // Test type emoji
        challenge.setType("treasure_hunt");
        assertEquals("Treasure hunt should have money emoji", "💰", challenge.getTypeEmoji());
        
        challenge.setType("exploration");
        assertEquals("Exploration should have map emoji", "🗺️", challenge.getTypeEmoji());
        
        challenge.setType("social_challenge");
        assertEquals("Social challenge should have people emoji", "👥", challenge.getTypeEmoji());
        
        challenge.setType("unknown_type");
        assertEquals("Unknown type should have star emoji", "⭐", challenge.getTypeEmoji());
    }

    @Test
    public void testChallengeFirebaseMap() {
        // Set up challenge with various properties
        challenge.setDifficultyScore(0.7);
        challenge.setPredictedEngagement(0.85);
        challenge.setPersonalizationScore(0.9);
        challenge.getAiTags().add("high_engagement");
        challenge.getAiTags().add("personalized");
        challenge.getParameters().put("count", 3);
        challenge.getParameters().put("rarity", "RARE");
        challenge.setStartTime(System.currentTimeMillis());
        challenge.setTimeLimit(7200000L); // 2 hours
        
        // Convert to Firebase map
        Map<String, Object> firebaseMap = challenge.toFirebaseMap();
        
        // Verify map contents
        assertNotNull("Firebase map should not be null", firebaseMap);
        assertEquals("Player ID should be in map", TEST_PLAYER_ID, firebaseMap.get("playerId"));
        assertEquals("Title should be in map", TEST_CHALLENGE_TITLE, firebaseMap.get("title"));
        assertEquals("Description should be in map", TEST_CHALLENGE_DESCRIPTION, firebaseMap.get("description"));
        assertEquals("Type should be in map", TEST_CHALLENGE_TYPE, firebaseMap.get("type"));
        assertEquals("Difficulty should be in map", TEST_DIFFICULTY, firebaseMap.get("difficulty"));
        assertEquals("Difficulty score should be in map", 0.7, (Double) firebaseMap.get("difficultyScore"), 0.001);
        assertEquals("Predicted engagement should be in map", 0.85, (Double) firebaseMap.get("predictedEngagement"), 0.001);
        assertEquals("Personalization score should be in map", 0.9, (Double) firebaseMap.get("personalizationScore"), 0.001);
        
        // Verify collections are included
        assertNotNull("AI tags should be in map", firebaseMap.get("aiTags"));
        assertNotNull("Parameters should be in map", firebaseMap.get("parameters"));
        assertNotNull("Progress should be in map", firebaseMap.get("progress"));
        assertNotNull("Rewards should be in map", firebaseMap.get("rewards"));
        
        // Verify timestamps
        assertNotNull("Created at should be in map", firebaseMap.get("createdAt"));
        assertNotNull("Updated at should be in map", firebaseMap.get("updatedAt"));
        assertTrue("Start time should be in map", (Long) firebaseMap.get("startTime") > 0);
        assertEquals("Time limit should be in map", Long.valueOf(7200000L), firebaseMap.get("timeLimit"));
    }

    @Test
    public void testDefaultConstructor() {
        PersonalizedChallenge defaultChallenge = new PersonalizedChallenge();
        
        // Verify default initialization
        assertNotNull("AI tags should be initialized", defaultChallenge.getAiTags());
        assertNotNull("Parameters should be initialized", defaultChallenge.getParameters());
        assertNotNull("Progress should be initialized", defaultChallenge.getProgress());
        assertNotNull("Rewards should be initialized", defaultChallenge.getRewards());
        assertNotNull("Milestones should be initialized", defaultChallenge.getMilestones());
        
        assertTrue("Created at should be set", defaultChallenge.getCreatedAt() > 0);
        assertTrue("Updated at should be set", defaultChallenge.getUpdatedAt() > 0);
        assertEquals("Created by should be AI", "AI", defaultChallenge.getCreatedBy());
        assertEquals("Version should be 1.0", "1.0", defaultChallenge.getVersion());
    }
}
