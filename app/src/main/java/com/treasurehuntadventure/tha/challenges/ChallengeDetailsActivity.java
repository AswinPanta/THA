package com.treasurehuntadventure.tha.challenges;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.treasurehuntadventure.tha.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Detailed view for personalized challenges with progress tracking and insights
 */
public class ChallengeDetailsActivity extends AppCompatActivity implements 
        ChallengeProgressManager.ProgressTrackingListener {
    
    private static final String TAG = "ChallengeDetailsActivity";
    
    // UI Components
    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView challengeTypeImage;
    private MaterialTextView challengeTitle;
    private MaterialTextView challengeDescription;
    private Chip difficultyChip;
    private Chip statusChip;
    private ChipGroup aiTagsGroup;
    
    // Progress section
    private MaterialCardView progressCard;
    private LinearProgressIndicator progressBar;
    private MaterialTextView progressText;
    private MaterialTextView timeRemainingText;
    private CircularProgressIndicator timeProgressIndicator;
    
    // AI Insights section
    private MaterialCardView aiInsightsCard;
    private MaterialTextView engagementScore;
    private MaterialTextView personalizationScore;
    private MaterialTextView difficultyScore;
    private MaterialTextView confidenceText;
    
    // Milestones section
    private MaterialCardView milestonesCard;
    private RecyclerView milestonesRecyclerView;
    private MilestoneAdapter milestoneAdapter;
    
    // Rewards section
    private MaterialCardView rewardsCard;
    private MaterialTextView experienceReward;
    private MaterialTextView coinsReward;
    private MaterialTextView bonusRewardText;
    private LottieAnimationView rewardAnimation;
    
    // Parameters section
    private MaterialCardView parametersCard;
    private MaterialTextView parametersText;
    
    // Action buttons
    private MaterialButton primaryActionButton;
    private MaterialButton secondaryActionButton;
    private MaterialButton shareButton;
    
    // Challenge system components
    private ChallengeProgressManager progressManager;
    private PlayerBehaviorAnalyzer behaviorAnalyzer;
    
    // Data
    private PersonalizedChallenge challenge;
    private String challengeId;
    private String currentPlayerId;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_details);
        
        // Get challenge ID from intent
        challengeId = getIntent().getStringExtra("challengeId");
        if (challengeId == null) {
            finish();
            return;
        }
        
        // Initialize Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        currentPlayerId = currentUser.getUid();
        
        // Initialize components
        initializeViews();
        initializeChallengeSystem();
        setupEventListeners();
        
        // Load challenge data
        loadChallengeDetails();
        
        Log.d(TAG, "Challenge Details Activity initialized for challenge: " + challengeId);
    }
    
    private void initializeViews() {
        // Toolbar and collapsing layout
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Header views
        challengeTypeImage = findViewById(R.id.challenge_type_image);
        challengeTitle = findViewById(R.id.challenge_title);
        challengeDescription = findViewById(R.id.challenge_description);
        difficultyChip = findViewById(R.id.difficulty_chip);
        statusChip = findViewById(R.id.status_chip);
        aiTagsGroup = findViewById(R.id.ai_tags_group);
        
        // Progress views
        progressCard = findViewById(R.id.progress_card);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        timeRemainingText = findViewById(R.id.time_remaining_text);
        timeProgressIndicator = findViewById(R.id.time_progress_indicator);
        
        // AI insights views
        aiInsightsCard = findViewById(R.id.ai_insights_card);
        engagementScore = findViewById(R.id.engagement_score);
        personalizationScore = findViewById(R.id.personalization_score);
        difficultyScore = findViewById(R.id.difficulty_score);
        confidenceText = findViewById(R.id.confidence_text);
        
        // Milestones views
        milestonesCard = findViewById(R.id.milestones_card);
        milestonesRecyclerView = findViewById(R.id.milestones_recycler_view);
        
        // Rewards views
        rewardsCard = findViewById(R.id.rewards_card);
        experienceReward = findViewById(R.id.experience_reward);
        coinsReward = findViewById(R.id.coins_reward);
        bonusRewardText = findViewById(R.id.bonus_reward_text);
        rewardAnimation = findViewById(R.id.reward_animation);
        
        // Parameters views
        parametersCard = findViewById(R.id.parameters_card);
        parametersText = findViewById(R.id.parameters_text);
        
        // Action buttons
        primaryActionButton = findViewById(R.id.primary_action_button);
        secondaryActionButton = findViewById(R.id.secondary_action_button);
        shareButton = findViewById(R.id.share_button);
    }
    
    private void initializeChallengeSystem() {
        // Initialize behavior analyzer
        behaviorAnalyzer = new PlayerBehaviorAnalyzer(this);
        
        // Initialize progress manager
        progressManager = new ChallengeProgressManager(this, behaviorAnalyzer);
        progressManager.setProgressTrackingListener(this);
    }
    
    private void setupEventListeners() {
        // Toolbar navigation
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Action buttons
        primaryActionButton.setOnClickListener(v -> handlePrimaryAction());
        secondaryActionButton.setOnClickListener(v -> handleSecondaryAction());
        shareButton.setOnClickListener(v -> shareChallenge());
    }
    
    private void loadChallengeDetails() {
        // Load challenge from Firestore
        FirebaseFirestore.getInstance()
                .collection("personalized_challenges")
                .document(challengeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        challenge = documentSnapshot.toObject(PersonalizedChallenge.class);
                        if (challenge != null) {
                            displayChallengeDetails();
                        } else {
                            showError("Failed to load challenge data");
                        }
                    } else {
                        showError("Challenge not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load challenge details", e);
                    showError("Failed to load challenge: " + e.getMessage());
                });
    }
    
    private void displayChallengeDetails() {
        // Set basic information
        challengeTitle.setText(challenge.getTitle());
        challengeDescription.setText(challenge.getDescription());
        collapsingToolbar.setTitle(challenge.getTitle());
        
        // Set challenge type image
        setChallengeTypeImage(challenge.getType());
        
        // Set difficulty chip
        setupDifficultyChip();
        
        // Set status chip
        setupStatusChip();
        
        // Set AI tags
        setupAITags();
        
        // Display progress
        displayProgress();
        
        // Display AI insights
        displayAIInsights();
        
        // Display milestones
        displayMilestones();
        
        // Display rewards
        displayRewards();
        
        // Display parameters
        displayParameters();
        
        // Setup action buttons
        setupActionButtons();
        
        // Start progress tracking if active
        if (challenge.isActive()) {
            progressManager.startChallenge(challengeId, currentPlayerId);
        }
    }
    
    private void setChallengeTypeImage(String challengeType) {
        int imageRes;
        switch (challengeType) {
            case "treasure_hunt":
                imageRes = R.drawable.challenge_treasure_hunt;
                break;
            case "exploration":
                imageRes = R.drawable.challenge_exploration;
                break;
            case "speed_challenge":
                imageRes = R.drawable.challenge_speed;
                break;
            case "accuracy_challenge":
                imageRes = R.drawable.challenge_accuracy;
                break;
            case "social_challenge":
                imageRes = R.drawable.challenge_social;
                break;
            case "collection_challenge":
                imageRes = R.drawable.challenge_collection;
                break;
            case "distance_challenge":
                imageRes = R.drawable.challenge_distance;
                break;
            case "time_challenge":
                imageRes = R.drawable.challenge_time;
                break;
            case "skill_challenge":
                imageRes = R.drawable.challenge_skill;
                break;
            case "creative_challenge":
                imageRes = R.drawable.challenge_creative;
                break;
            default:
                imageRes = R.drawable.challenge_default;
                break;
        }
        challengeTypeImage.setImageResource(imageRes);
    }
    
    private void setupDifficultyChip() {
        difficultyChip.setText(challenge.getDifficulty());
        difficultyChip.setChipIcon(ContextCompat.getDrawable(this, getDifficultyIcon(challenge.getDifficulty())));
        
        int colorRes = getDifficultyColor(challenge.getDifficulty());
        difficultyChip.setChipBackgroundColor(ContextCompat.getColorStateList(this, colorRes));
    }
    
    private void setupStatusChip() {
        if (challenge.isCompleted()) {
            statusChip.setText("Completed");
            statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.success_color));
            statusChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_circle));
        } else if (challenge.isFailed()) {
            statusChip.setText("Failed");
            statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.error_color));
            statusChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_cancel));
        } else if (challenge.isActive()) {
            statusChip.setText("Active");
            statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.primary_color));
            statusChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow));
        } else {
            statusChip.setText("Available");
            statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.secondary_color));
            statusChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_available));
        }
    }
    
    private void setupAITags() {
        aiTagsGroup.removeAllViews();
        
        for (String tag : challenge.getAiTags()) {
            Chip tagChip = new Chip(this);
            tagChip.setText(tag.replace("_", " ").toUpperCase());
            tagChip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.ai_tag_background));
            tagChip.setTextColor(ContextCompat.getColor(this, R.color.ai_tag_text));
            tagChip.setTextSize(10f);
            aiTagsGroup.addView(tagChip);
        }
    }
    
    private void displayProgress() {
        PersonalizedChallenge.ChallengeProgress progress = challenge.getProgress();
        
        if (progress != null && challenge.isActive()) {
            progressCard.setVisibility(View.VISIBLE);
            
            // Set progress bar
            int progressPercent = (int) (progress.getCompletionPercentage() * 100);
            progressBar.setProgress(progressPercent);
            progressText.setText(String.format(Locale.getDefault(), "%d%% Complete", progressPercent));
            
            // Set time remaining
            setupTimeRemaining();
            
            // Animate progress
            animateProgress(progressPercent);
        } else {
            progressCard.setVisibility(View.GONE);
        }
    }
    
    private void setupTimeRemaining() {
        long remainingTime = challenge.getRemainingTime();
        
        if (remainingTime != Long.MAX_VALUE) {
            long hours = TimeUnit.MILLISECONDS.toHours(remainingTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
            
            if (hours > 0) {
                timeRemainingText.setText(String.format(Locale.getDefault(), "%dh %dm remaining", hours, minutes));
            } else {
                timeRemainingText.setText(String.format(Locale.getDefault(), "%dm remaining", minutes));
            }
            
            // Set time progress indicator
            double timeProgress = challenge.getTimeProgress();
            timeProgressIndicator.setProgress((int) (timeProgress * 100));
            
            // Color code based on urgency
            if (timeProgress > 0.8) {
                timeRemainingText.setTextColor(ContextCompat.getColor(this, R.color.error_color));
            } else if (timeProgress > 0.6) {
                timeRemainingText.setTextColor(ContextCompat.getColor(this, R.color.warning_color));
            } else {
                timeRemainingText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
            
            timeRemainingText.setVisibility(View.VISIBLE);
            timeProgressIndicator.setVisibility(View.VISIBLE);
        } else {
            timeRemainingText.setVisibility(View.GONE);
            timeProgressIndicator.setVisibility(View.GONE);
        }
    }
    
    private void displayAIInsights() {
        aiInsightsCard.setVisibility(View.VISIBLE);
        
        // Set engagement score
        double engagement = challenge.getPredictedEngagement();
        engagementScore.setText(String.format(Locale.getDefault(), "%.0f%%", engagement * 100));
        setScoreColor(engagementScore, engagement);
        
        // Set personalization score
        double personalization = challenge.getPersonalizationScore();
        personalizationScore.setText(String.format(Locale.getDefault(), "%.0f%%", personalization * 100));
        setScoreColor(personalizationScore, personalization);
        
        // Set difficulty score
        double difficulty = challenge.getDifficultyScore();
        difficultyScore.setText(String.format(Locale.getDefault(), "%.0f%%", difficulty * 100));
        setScoreColor(difficultyScore, difficulty);
        
        // Set confidence text
        Map<String, Double> confidenceScores = challenge.getConfidenceScores();
        if (!confidenceScores.isEmpty()) {
            double avgConfidence = confidenceScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            confidenceText.setText(String.format(Locale.getDefault(), "AI Confidence: %.0f%%", avgConfidence * 100));
        } else {
            confidenceText.setText("AI Confidence: N/A");
        }
    }
    
    private void displayMilestones() {
        if (challenge.getMilestones() != null && !challenge.getMilestones().isEmpty()) {
            milestonesCard.setVisibility(View.VISIBLE);
            
            milestoneAdapter = new MilestoneAdapter(this, challenge.getMilestones());
            milestonesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            milestonesRecyclerView.setAdapter(milestoneAdapter);
        } else {
            milestonesCard.setVisibility(View.GONE);
        }
    }
    
    private void displayRewards() {
        PersonalizedChallenge.ChallengeRewards rewards = challenge.getRewards();
        
        if (rewards != null) {
            rewardsCard.setVisibility(View.VISIBLE);
            
            // Set base rewards
            experienceReward.setText(String.format(Locale.getDefault(), "+%d XP", rewards.getBaseExperience()));
            coinsReward.setText(String.format(Locale.getDefault(), "+%d", rewards.getBaseCoins()));
            
            // Set bonus rewards
            if (challenge.isBonusEarned() || !challenge.getBonusConditions().isEmpty()) {
                bonusRewardText.setVisibility(View.VISIBLE);
                if (challenge.isBonusEarned()) {
                    bonusRewardText.setText("🎉 Bonus Earned!");
                    bonusRewardText.setTextColor(ContextCompat.getColor(this, R.color.success_color));
                } else {
                    bonusRewardText.setText("Bonus Available");
                    bonusRewardText.setTextColor(ContextCompat.getColor(this, R.color.warning_color));
                }
            } else {
                bonusRewardText.setVisibility(View.GONE);
            }
            
            // Setup reward animation
            if (challenge.isCompleted()) {
                rewardAnimation.setAnimation("reward_celebration.json");
                rewardAnimation.setVisibility(View.VISIBLE);
                rewardAnimation.playAnimation();
            } else {
                rewardAnimation.setVisibility(View.GONE);
            }
        } else {
            rewardsCard.setVisibility(View.GONE);
        }
    }
    
    private void displayParameters() {
        Map<String, Object> parameters = challenge.getParameters();
        
        if (parameters != null && !parameters.isEmpty()) {
            parametersCard.setVisibility(View.VISIBLE);
            
            StringBuilder parametersBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey().replace("_", " ").toUpperCase();
                String value = entry.getValue().toString();
                parametersBuilder.append("• ").append(key).append(": ").append(value).append("\n");
            }
            
            parametersText.setText(parametersBuilder.toString().trim());
        } else {
            parametersCard.setVisibility(View.GONE);
        }
    }
    
    private void setupActionButtons() {
        if (challenge.isCompleted()) {
            primaryActionButton.setText("View Rewards");
            primaryActionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_gift));
            secondaryActionButton.setVisibility(View.GONE);
        } else if (challenge.isFailed()) {
            primaryActionButton.setText("Retry");
            primaryActionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_refresh));
            secondaryActionButton.setVisibility(View.GONE);
        } else if (challenge.isActive()) {
            primaryActionButton.setText("Continue");
            primaryActionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow));
            secondaryActionButton.setText("Abandon");
            secondaryActionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_stop));
            secondaryActionButton.setVisibility(View.VISIBLE);
        } else {
            primaryActionButton.setText("Start Challenge");
            primaryActionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_start));
            secondaryActionButton.setVisibility(View.GONE);
        }
    }
    
    private void handlePrimaryAction() {
        if (challenge.isCompleted()) {
            // Show rewards dialog
            showRewardsDialog();
        } else if (challenge.isFailed()) {
            // Retry challenge
            retryChallenge();
        } else if (challenge.isActive()) {
            // Continue challenge - go back to main activity
            finish();
        } else {
            // Start challenge
            startChallenge();
        }
    }
    
    private void handleSecondaryAction() {
        if (challenge.isActive()) {
            // Abandon challenge
            abandonChallenge();
        }
    }
    
    private void startChallenge() {
        progressManager.startChallenge(challengeId, currentPlayerId)
                .thenRun(() -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Challenge started!", Toast.LENGTH_SHORT).show();
                        // Refresh the display
                        loadChallengeDetails();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to start challenge", Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }
    
    private void abandonChallenge() {
        progressManager.abandonChallenge(challengeId, currentPlayerId)
                .thenRun(() -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Challenge abandoned", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to abandon challenge", Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }
    
    private void retryChallenge() {
        // Reset challenge and start again
        Map<String, Object> updates = new HashMap<>();
        updates.put("failed", false);
        updates.put("completed", false);
        updates.put("startTime", 0);
        updates.put("endTime", 0);
        updates.put("updatedAt", System.currentTimeMillis());
        
        FirebaseFirestore.getInstance()
                .collection("personalized_challenges")
                .document(challengeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Challenge reset! Ready to try again.", Toast.LENGTH_SHORT).show();
                    loadChallengeDetails();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reset challenge", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void shareChallenge() {
        String shareText = "Check out this personalized challenge: " + challenge.getTitle() + 
                "\n" + challenge.getDescription() + 
                "\nDifficulty: " + challenge.getDifficulty() +
                "\nRewards: +" + challenge.getRewards().getBaseExperience() + " XP, +" + 
                challenge.getRewards().getBaseCoins() + " coins";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Challenge"));
    }
    
    private void showRewardsDialog() {
        // This would typically show a detailed rewards dialog
        Toast.makeText(this, "Rewards: +" + challenge.getRewards().getBaseExperience() + " XP, +" + 
                challenge.getRewards().getBaseCoins() + " coins", Toast.LENGTH_LONG).show();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
    
    // Animation methods
    private void animateProgress(int progress) {
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, progress);
        progressAnimator.setDuration(1000);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.start();
    }
    
    private void setScoreColor(MaterialTextView textView, double score) {
        int colorRes;
        if (score >= 0.8) {
            colorRes = R.color.success_color;
        } else if (score >= 0.6) {
            colorRes = R.color.warning_color;
        } else {
            colorRes = R.color.error_color;
        }
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
    }
    
    // Helper methods
    private int getDifficultyIcon(String difficulty) {
        switch (difficulty) {
            case "BEGINNER": return R.drawable.ic_difficulty_beginner;
            case "NOVICE": return R.drawable.ic_difficulty_novice;
            case "INTERMEDIATE": return R.drawable.ic_difficulty_intermediate;
            case "ADVANCED": return R.drawable.ic_difficulty_advanced;
            case "EXPERT": return R.drawable.ic_difficulty_expert;
            case "MASTER": return R.drawable.ic_difficulty_master;
            default: return R.drawable.ic_difficulty_unknown;
        }
    }
    
    private int getDifficultyColor(String difficulty) {
        switch (difficulty) {
            case "BEGINNER": return R.color.difficulty_beginner;
            case "NOVICE": return R.color.difficulty_novice;
            case "INTERMEDIATE": return R.color.difficulty_intermediate;
            case "ADVANCED": return R.color.difficulty_advanced;
            case "EXPERT": return R.color.difficulty_expert;
            case "MASTER": return R.color.difficulty_master;
            default: return R.color.difficulty_unknown;
        }
    }
    
    // Progress Tracking Listener Implementation
    @Override
    public void onProgressUpdated(String challengeId, double progress) {
        if (challengeId.equals(this.challengeId)) {
            runOnUiThread(() -> {
                // Update progress display
                int progressPercent = (int) (progress * 100);
                progressBar.setProgress(progressPercent);
                progressText.setText(String.format(Locale.getDefault(), "%d%% Complete", progressPercent));
            });
        }
    }
    
    @Override
    public void onMilestoneReached(String challengeId, PersonalizedChallenge.Milestone milestone) {
        if (challengeId.equals(this.challengeId)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "🎉 Milestone reached: " + milestone.getName(), Toast.LENGTH_LONG).show();
                // Refresh milestones display
                if (milestoneAdapter != null) {
                    milestoneAdapter.notifyDataSetChanged();
                }
            });
        }
    }
    
    @Override
    public void onChallengeCompleted(String challengeId, PersonalizedChallenge challenge) {
        if (challengeId.equals(this.challengeId)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "🎉 Challenge completed!", Toast.LENGTH_LONG).show();
                // Refresh the entire display
                loadChallengeDetails();
            });
        }
    }
    
    @Override
    public void onChallengeFailed(String challengeId, String reason) {
        if (challengeId.equals(this.challengeId)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Challenge failed: " + reason, Toast.LENGTH_LONG).show();
                // Refresh the display
                loadChallengeDetails();
            });
        }
    }
    
    @Override
    public void onHintAvailable(String challengeId, String hint) {
        if (challengeId.equals(this.challengeId)) {
            runOnUiThread(() -> {
                Toast.makeText(this, hint, Toast.LENGTH_LONG).show();
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup resources
        if (progressManager != null) {
            progressManager.cleanup();
        }
        if (behaviorAnalyzer != null) {
            behaviorAnalyzer.clearCache();
        }
    }
}
