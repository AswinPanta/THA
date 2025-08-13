package com.treasurehuntadventure.tha.challenges;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.treasurehuntadventure.tha.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced activity for displaying and managing personalized AI-generated challenges
 */
public class PersonalizedChallengeActivity extends AppCompatActivity implements 
        AIChallengeGenerator.ChallengeGenerationListener,
        PersonalizedChallengeAdapter.ChallengeInteractionListener {
    
    private static final String TAG = "PersonalizedChallengeActivity";
    
    // UI Components
    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView challengesRecyclerView;
    private PersonalizedChallengeAdapter challengeAdapter;
    private CircularProgressIndicator loadingIndicator;
    private LottieAnimationView emptyStateAnimation;
    private MaterialTextView emptyStateText;
    private MaterialCardView statsCard;
    private MaterialTextView completedChallengesText;
    private MaterialTextView streakText;
    private MaterialTextView engagementScoreText;
    private LinearProgressIndicator dailyProgressIndicator;
    private ChipGroup filterChipGroup;
    private FloatingActionButton generateChallengesFab;
    private MaterialButton viewAnalyticsFab;
    
    // Challenge system components
    private AIChallengeGenerator challengeGenerator;
    private PlayerBehaviorAnalyzer behaviorAnalyzer;
    private ChallengeProgressManager progressManager;
    
    // Data
    private List<PersonalizedChallenge> activeChallenges;
    private String currentPlayerId;
    private Location currentLocation;
    private String selectedFilter = "all";
    
    // State
    private boolean isLoading = false;
    private boolean isGeneratingChallenges = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personalized_challenge);
        
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
        setupRecyclerView();
        setupEventListeners();
        
        // Load initial data
        loadActiveChallenges();
        loadPlayerStats();
        
        Log.d(TAG, "Personalized Challenge Activity initialized");
    }
    
    private void initializeViews() {
        // Toolbar and collapsing layout
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Main content views
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        challengesRecyclerView = findViewById(R.id.challenges_recycler_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        emptyStateAnimation = findViewById(R.id.empty_state_animation);
        emptyStateText = findViewById(R.id.empty_state_text);
        
        // Stats card
        statsCard = findViewById(R.id.stats_card);
        completedChallengesText = findViewById(R.id.completed_challenges_text);
        streakText = findViewById(R.id.streak_text);
        engagementScoreText = findViewById(R.id.engagement_score_text);
        dailyProgressIndicator = findViewById(R.id.daily_progress_indicator);
        
        // Filter chips
        filterChipGroup = findViewById(R.id.filter_chip_group);
        
        // Action buttons
        generateChallengesFab = findViewById(R.id.generate_challenges_fab);
        viewAnalyticsFab = findViewById(R.id.view_analytics_button);
        
        // Initialize data
        activeChallenges = new ArrayList<>();
        
        // Setup initial UI state
        setupInitialUIState();
    }
    
    private void setupInitialUIState() {
        // Setup collapsing toolbar
        collapsingToolbar.setTitle("Daily Challenges");
        collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.white, getTheme()));
        collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.white, getTheme()));
        
        // Setup empty state
        emptyStateAnimation.setAnimation("treasure_chest_empty.json");
        emptyStateText.setText("No challenges available yet!\nTap the compass to generate personalized challenges.");
        
        // Setup filter chips
        setupFilterChips();
        
        // Setup loading state
        showLoadingState();
    }
    
    private void setupFilterChips() {
        String[] filters = {"All", "Active", "Completed", "Treasure Hunt", "Exploration", "Social", "Speed"};
        String[] filterValues = {"all", "active", "completed", "treasure_hunt", "exploration", "social", "speed"};
        
        for (int i = 0; i < filters.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(filters[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0); // First chip selected by default
            
            final String filterValue = filterValues[i];
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedFilter = filterValue;
                    filterChallenges();
                    
                    // Uncheck other chips
                    for (int j = 0; j < filterChipGroup.getChildCount(); j++) {
                        Chip otherChip = (Chip) filterChipGroup.getChildAt(j);
                        if (otherChip != chip) {
                            otherChip.setChecked(false);
                        }
                    }
                }
            });
            
            filterChipGroup.addView(chip);
        }
    }
    
    private void initializeChallengeSystem() {
        // Initialize behavior analyzer
        behaviorAnalyzer = new PlayerBehaviorAnalyzer(this);
        behaviorAnalyzer.setBehaviorAnalysisListener(new PlayerBehaviorAnalyzer.BehaviorAnalysisListener() {
            @Override
            public void onBehaviorProfileUpdated(String playerId, PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile) {
                if (playerId.equals(currentPlayerId)) {
                    updateEngagementScore(profile.getEngagementScore());
                }
            }
            
            @Override
            public void onPatternDetected(String playerId, String pattern, double confidence) {
                Log.d(TAG, "Pattern detected: " + pattern + " with confidence: " + confidence);
            }
            
            @Override
            public void onEngagementScoreChanged(String playerId, double oldScore, double newScore) {
                if (playerId.equals(currentPlayerId)) {
                    animateEngagementScoreChange(oldScore, newScore);
                }
            }
            
            @Override
            public void onChurnRiskDetected(String playerId, double riskLevel) {
                if (playerId.equals(currentPlayerId) && riskLevel > 0.7) {
                    showChurnRiskNotification();
                }
            }
        });
        
        // Initialize challenge generator
        challengeGenerator = new AIChallengeGenerator(this, behaviorAnalyzer);
        challengeGenerator.setChallengeGenerationListener(this);
        
        // Initialize progress manager
        progressManager = new ChallengeProgressManager(this, behaviorAnalyzer);
    }
    
    private void setupRecyclerView() {
        challengeAdapter = new PersonalizedChallengeAdapter(this, activeChallenges, this);
        challengesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        challengesRecyclerView.setAdapter(challengeAdapter);
        
        // Add item animations
        challengesRecyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }
    
    private void setupEventListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshChallenges);
        
        // Generate challenges FAB
        generateChallengesFab.setOnClickListener(v -> generateNewChallenges());
        
        // View analytics button
        viewAnalyticsFab.setOnClickListener(v -> openAnalytics());
        
        // Toolbar navigation
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void loadActiveChallenges() {
        if (isLoading) return;
        
        isLoading = true;
        showLoadingState();
        
        // Load challenges from Firebase or generate new ones
        progressManager.getActiveChallenges(currentPlayerId)
                .thenAccept(challenges -> {
                    runOnUiThread(() -> {
                        activeChallenges.clear();
                        activeChallenges.addAll(challenges);
                        
                        if (challenges.isEmpty()) {
                            showEmptyState();
                        } else {
                            showChallengesContent();
                            challengeAdapter.notifyDataSetChanged();
                        }
                        
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Failed to load challenges", throwable);
                        showErrorState();
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                    });
                    return null;
                });
    }
    
    private void loadPlayerStats() {
        behaviorAnalyzer.getBehaviorProfile(currentPlayerId)
                .thenAccept(profile -> {
                    runOnUiThread(() -> updateStatsCard(profile));
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to load player stats", throwable);
                    return null;
                });
    }
    
    private void generateNewChallenges() {
        if (isGeneratingChallenges) return;
        
        isGeneratingChallenges = true;
        generateChallengesFab.setEnabled(false);
        
        // Show generation animation
        showGenerationAnimation();
        
        challengeGenerator.generateDailyChallenges(currentPlayerId, currentLocation)
                .thenAccept(newChallenges -> {
                    runOnUiThread(() -> {
                        // Add new challenges to the list
                        int insertPosition = activeChallenges.size();
                        activeChallenges.addAll(newChallenges);
                        challengeAdapter.notifyItemRangeInserted(insertPosition, newChallenges.size());
                        
                        // Show success message
                        Toast.makeText(this, "Generated " + newChallenges.size() + " personalized challenges!", 
                                Toast.LENGTH_SHORT).show();
                        
                        // Update UI state
                        if (!activeChallenges.isEmpty()) {
                            showChallengesContent();
                        }
                        
                        isGeneratingChallenges = false;
                        generateChallengesFab.setEnabled(true);
                        hideGenerationAnimation();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Failed to generate challenges", throwable);
                        Toast.makeText(this, "Failed to generate challenges. Please try again.", 
                                Toast.LENGTH_SHORT).show();
                        
                        isGeneratingChallenges = false;
                        generateChallengesFab.setEnabled(true);
                        hideGenerationAnimation();
                    });
                    return null;
                });
    }
    
    private void refreshChallenges() {
        loadActiveChallenges();
        loadPlayerStats();
    }
    
    private void filterChallenges() {
        List<PersonalizedChallenge> filteredChallenges = new ArrayList<>();
        
        for (PersonalizedChallenge challenge : activeChallenges) {
            boolean shouldInclude = false;
            
            switch (selectedFilter) {
                case "all":
                    shouldInclude = true;
                    break;
                case "active":
                    shouldInclude = challenge.isActive();
                    break;
                case "completed":
                    shouldInclude = challenge.isCompleted();
                    break;
                default:
                    shouldInclude = challenge.getType().equals(selectedFilter);
                    break;
            }
            
            if (shouldInclude) {
                filteredChallenges.add(challenge);
            }
        }
        
        challengeAdapter.updateChallenges(filteredChallenges);
    }
    
    private void openAnalytics() {
        Intent intent = new Intent(this, ChallengeAnalyticsActivity.class);
        intent.putExtra("playerId", currentPlayerId);
        startActivity(intent);
    }
    
    // UI State Management
    private void showLoadingState() {
        loadingIndicator.setVisibility(View.VISIBLE);
        challengesRecyclerView.setVisibility(View.GONE);
        emptyStateAnimation.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
    }
    
    private void showChallengesContent() {
        loadingIndicator.setVisibility(View.GONE);
        challengesRecyclerView.setVisibility(View.VISIBLE);
        emptyStateAnimation.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        loadingIndicator.setVisibility(View.GONE);
        challengesRecyclerView.setVisibility(View.GONE);
        emptyStateAnimation.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateAnimation.playAnimation();
    }
    
    private void showErrorState() {
        loadingIndicator.setVisibility(View.GONE);
        challengesRecyclerView.setVisibility(View.GONE);
        emptyStateAnimation.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateAnimation.setAnimation("error_animation.json");
        emptyStateText.setText("Failed to load challenges.\nPull down to refresh.");
        emptyStateAnimation.playAnimation();
    }
    
    private void showGenerationAnimation() {
        // Animate FAB
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(generateChallengesFab, "rotation", 0f, 360f);
        rotationAnimator.setDuration(1000);
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotationAnimator.start();
    }
    
    private void hideGenerationAnimation() {
        generateChallengesFab.clearAnimation();
        generateChallengesFab.setRotation(0f);
    }
    
    private void updateStatsCard(PlayerBehaviorAnalyzer.PlayerBehaviorProfile profile) {
        // Update completed challenges count
        int completedCount = (int) activeChallenges.stream().filter(PersonalizedChallenge::isCompleted).count();
        completedChallengesText.setText(String.valueOf(completedCount));
        
        // Update streak
        PlayerBehaviorAnalyzer.PerformanceMetrics performance = profile.getPerformanceMetrics();
        streakText.setText(String.valueOf(performance.getCurrentStreak()));
        
        // Update engagement score
        double engagementScore = profile.getEngagementScore();
        engagementScoreText.setText(String.format("%.1f%%", engagementScore * 100));
        
        // Update daily progress
        int totalChallenges = activeChallenges.size();
        int progress = totalChallenges > 0 ? (completedCount * 100) / totalChallenges : 0;
        dailyProgressIndicator.setProgress(progress);
        
        // Animate stats card
        animateStatsCard();
    }
    
    private void updateEngagementScore(double newScore) {
        engagementScoreText.setText(String.format("%.1f%%", newScore * 100));
    }
    
    private void animateEngagementScoreChange(double oldScore, double newScore) {
        // Create value animator for smooth score transition
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat((float) oldScore, (float) newScore);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float animatedValue = (Float) animation.getAnimatedValue();
            engagementScoreText.setText(String.format("%.1f%%", animatedValue * 100));
        });
        animator.start();
    }
    
    private void animateStatsCard() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(statsCard, "scaleX", 0.95f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(statsCard, "scaleY", 0.95f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(statsCard, "alpha", 0.7f, 1.0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }
    
    private void showChurnRiskNotification() {
        Toast.makeText(this, "We miss you! Here are some easier challenges to get back into the game.", 
                Toast.LENGTH_LONG).show();
    }
    
    // Challenge Generation Listener Implementation
    @Override
    public void onChallengesGenerated(String playerId, List<PersonalizedChallenge> challenges) {
        if (playerId.equals(currentPlayerId)) {
            runOnUiThread(() -> {
                Log.d(TAG, "Generated " + challenges.size() + " challenges for player");
            });
        }
    }
    
    @Override
    public void onGenerationError(String playerId, String error) {
        if (playerId.equals(currentPlayerId)) {
            runOnUiThread(() -> {
                Log.e(TAG, "Challenge generation error: " + error);
                Toast.makeText(this, "Failed to generate personalized challenges", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    @Override
    public void onModelUpdated(String modelName, double accuracy) {
        Log.d(TAG, "ML model updated: " + modelName + " with accuracy: " + accuracy);
    }
    
    @Override
    public void onPersonalizationImproved(String playerId, double improvementScore) {
        if (playerId.equals(currentPlayerId)) {
            Log.d(TAG, "Personalization improved by: " + improvementScore);
        }
    }
    
    // Challenge Interaction Listener Implementation
    @Override
    public void onChallengeStarted(PersonalizedChallenge challenge) {
        // Start tracking challenge progress
        progressManager.startChallenge(challenge.getChallengeId(), currentPlayerId);
        
        // Record behavior event
        behaviorAnalyzer.recordChallengeCompletionBehavior(currentPlayerId, challenge.getType(), 
                challenge.getDifficulty(), 0, false, 0.0);
        
        Toast.makeText(this, "Challenge started: " + challenge.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onChallengeCompleted(PersonalizedChallenge challenge) {
        // Update challenge progress
        progressManager.completeChallenge(challenge.getChallengeId(), currentPlayerId, 1.0);
        
        // Record behavior event
        behaviorAnalyzer.recordChallengeCompletionBehavior(currentPlayerId, challenge.getType(), 
                challenge.getDifficulty(), System.currentTimeMillis() - challenge.getStartTime(), 
                true, challenge.getCompletionScore());
        
        // Show completion animation
        showChallengeCompletionAnimation(challenge);
        
        // Refresh stats
        loadPlayerStats();
    }
    
    @Override
    public void onChallengeAbandoned(PersonalizedChallenge challenge) {
        // Record abandonment
        progressManager.abandonChallenge(challenge.getChallengeId(), currentPlayerId);
        
        Toast.makeText(this, "Challenge abandoned", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onChallengeDetailsRequested(PersonalizedChallenge challenge) {
        // Open challenge details activity
        Intent intent = new Intent(this, ChallengeDetailsActivity.class);
        intent.putExtra("challengeId", challenge.getChallengeId());
        startActivity(intent);
    }
    
    private void showChallengeCompletionAnimation(PersonalizedChallenge challenge) {
        // Create celebration animation
        Toast.makeText(this, "🎉 Challenge completed! +" + challenge.getRewards().getBaseExperience() + " XP", 
                Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Start behavior tracking session
        if (currentLocation != null) {
            behaviorAnalyzer.startSession(currentPlayerId, currentLocation);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // End behavior tracking session
        if (currentLocation != null) {
            behaviorAnalyzer.endSession(currentPlayerId, currentLocation);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup resources
        if (challengeGenerator != null) {
            challengeGenerator.cleanup();
        }
        if (behaviorAnalyzer != null) {
            behaviorAnalyzer.clearCache();
        }
    }
    
    // Location update method (to be called from location service)
    public void updateLocation(Location location) {
        this.currentLocation = location;
    }
}
