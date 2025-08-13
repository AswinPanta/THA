package com.treasurehuntadventure.tha;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.treasurehuntadventure.tha.leaderboard.PlayerProfile;
import com.treasurehuntadventure.tha.leaderboard.RankingManager;
import com.treasurehuntadventure.tha.leaderboard.StatisticsManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Detailed player profile activity showing stats, achievements, badges, and recent activity
 */
public class PlayerProfileActivity extends AppCompatActivity {
    private static final String TAG = "PlayerProfileActivity";
    
    // UI Components
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView profileAvatarImage;
    private ImageView profileBannerImage;
    private TextView playerNameText;
    private TextView playerTitleText;
    private TextView playerLevelText;
    private TextView totalScoreText;
    private TextView globalRankText;
    private TextView joinDateText;
    private TextView lastActiveText;
    
    // Statistics cards
    private TextView treasuresCapturedText;
    private TextView distanceWalkedText;
    private TextView timePlayedText;
    private TextView achievementsUnlockedText;
    private TextView consecutiveDaysText;
    private TextView socialInteractionsText;
    
    // Progress bars
    private ProgressBar levelProgressBar;
    private TextView levelProgressText;
    
    // RecyclerViews
    private RecyclerView badgesRecyclerView;
    private RecyclerView achievementsRecyclerView;
    private RecyclerView recentActivityRecyclerView;
    
    // Loading and error states
    private ProgressBar loadingProgressBar;
    private CardView errorCard;
    private TextView errorText;
    
    // Data
    private String playerId;
    private String playerName;
    private PlayerProfile playerProfile;
    private StatisticsManager statisticsManager;
    private RankingManager rankingManager;
    
    // Formatters
    private NumberFormat numberFormat;
    private SimpleDateFormat dateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_profile);
        
        // Get player info from intent
        playerId = getIntent().getStringExtra("playerId");
        playerName = getIntent().getStringExtra("playerName");
        
        if (playerId == null) {
            Toast.makeText(this, "Player ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize formatters
        numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        // Initialize managers
        statisticsManager = new StatisticsManager(this);
        rankingManager = new RankingManager(this);
        
        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerViews();
        
        // Load player data
        loadPlayerProfile();
        
        Log.d(TAG, "Player profile activity created for: " + playerId);
    }
    
    private void initializeViews() {
        // Toolbar and header
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        profileAvatarImage = findViewById(R.id.profile_avatar_image);
        profileBannerImage = findViewById(R.id.profile_banner_image);
        playerNameText = findViewById(R.id.player_name_text);
        playerTitleText = findViewById(R.id.player_title_text);
        playerLevelText = findViewById(R.id.player_level_text);
        totalScoreText = findViewById(R.id.total_score_text);
        globalRankText = findViewById(R.id.global_rank_text);
        joinDateText = findViewById(R.id.join_date_text);
        lastActiveText = findViewById(R.id.last_active_text);
        
        // Statistics
        treasuresCapturedText = findViewById(R.id.treasures_captured_text);
        distanceWalkedText = findViewById(R.id.distance_walked_text);
        timePlayedText = findViewById(R.id.time_played_text);
        achievementsUnlockedText = findViewById(R.id.achievements_unlocked_text);
        consecutiveDaysText = findViewById(R.id.consecutive_days_text);
        socialInteractionsText = findViewById(R.id.social_interactions_text);
        
        // Progress
        levelProgressBar = findViewById(R.id.level_progress_bar);
        levelProgressText = findViewById(R.id.level_progress_text);
        
        // RecyclerViews
        badgesRecyclerView = findViewById(R.id.badges_recycler_view);
        achievementsRecyclerView = findViewById(R.id.achievements_recycler_view);
        recentActivityRecyclerView = findViewById(R.id.recent_activity_recycler_view);
        
        // Loading and error states
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        errorCard = findViewById(R.id.error_card);
        errorText = findViewById(R.id.error_text);
        
        // Set initial states
        showLoading(true);
        errorCard.setVisibility(View.GONE);
    }
    
    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Set initial title
        collapsingToolbar.setTitle(playerName != null ? playerName : "Player Profile");
    }
    
    private void setupRecyclerViews() {
        // Badges - horizontal grid
        badgesRecyclerView.setLayoutManager(new GridLayoutManager(this, 4, GridLayoutManager.HORIZONTAL, false));
        
        // Achievements - vertical list
        achievementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        achievementsRecyclerView.setNestedScrollingEnabled(false);
        
        // Recent activity - vertical list
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityRecyclerView.setNestedScrollingEnabled(false);
    }
    
    private void loadPlayerProfile() {
        showLoading(true);
        
        FirebaseFirestore.getInstance()
                .collection("player_profiles")
                .document(playerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        playerProfile = documentSnapshot.toObject(PlayerProfile.class);
                        if (playerProfile != null) {
                            displayPlayerProfile();
                            loadPlayerRanking();
                        } else {
                            showError("Failed to parse player profile");
                        }
                    } else {
                        showError("Player profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load player profile", e);
                    showError("Failed to load player profile: " + e.getMessage());
                });
    }
    
    private void loadPlayerRanking() {
        rankingManager.getPlayerRank(playerId, RankingManager.RankingCategory.OVERALL, 
                RankingManager.RankingPeriod.ALL_TIME)
                .thenAccept(rank -> {
                    runOnUiThread(() -> {
                        if (rank > 0) {
                            globalRankText.setText("Global Rank: #" + numberFormat.format(rank));
                            globalRankText.setVisibility(View.VISIBLE);
                        } else {
                            globalRankText.setVisibility(View.GONE);
                        }
                        showLoading(false);
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        globalRankText.setVisibility(View.GONE);
                        showLoading(false);
                    });
                    return null;
                });
    }
    
    private void displayPlayerProfile() {
        if (playerProfile == null) return;
        
        // Header information
        playerNameText.setText(playerProfile.getDisplayName() != null ? 
                playerProfile.getDisplayName() : "Anonymous Pirate");
        playerTitleText.setText(playerProfile.getCurrentTitle() != null ? 
                playerProfile.getCurrentTitle() : "Rookie Pirate");
        playerLevelText.setText("Level " + playerProfile.getLevel());
        
        // Load avatar
        if (playerProfile.getAvatarUrl() != null && !playerProfile.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(playerProfile.getAvatarUrl())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(profileAvatarImage);
        }
        
        // Load banner (if available)
        if (profileBannerImage != null) {
            // Set a default pirate-themed banner
            profileBannerImage.setImageResource(R.drawable.profile_banner_default);
        }
        
        // Score and ranking
        if (playerProfile.getRankings() != null) {
            totalScoreText.setText(numberFormat.format(playerProfile.getRankings().getTotalScore()) + " points");
        }
        
        // Dates
        if (playerProfile.getCreatedAt() > 0) {
            joinDateText.setText("Joined " + dateFormat.format(new Date(playerProfile.getCreatedAt())));
        }
        
        if (playerProfile.getLastActiveAt() > 0) {
            long timeDiff = System.currentTimeMillis() - playerProfile.getLastActiveAt();
            String lastActiveStr = formatTimeAgo(timeDiff);
            lastActiveText.setText("Last active " + lastActiveStr);
        }
        
        // Statistics
        displayStatistics();
        
        // Level progress
        displayLevelProgress();
        
        // Badges and achievements
        displayBadges();
        displayAchievements();
        displayRecentActivity();
    }
    
    private void displayStatistics() {
        if (playerProfile.getStatistics() == null) return;
        
        PlayerProfile.PlayerStatistics stats = playerProfile.getStatistics();
        
        treasuresCapturedText.setText(numberFormat.format(stats.getTreasuresCaptured()));
        distanceWalkedText.setText(String.format(Locale.getDefault(), "%.1f km", 
                stats.getDistanceWalked() / 1000.0));
        
        // Format time played
        long hours = stats.getTimePlayed() / (1000 * 60 * 60);
        timePlayedText.setText(hours + " hours");
        
        achievementsUnlockedText.setText(String.valueOf(playerProfile.getUnlockedAchievements().size()));
        consecutiveDaysText.setText(String.valueOf(stats.getConsecutiveDays()));
        socialInteractionsText.setText(String.valueOf(stats.getChatMessagesSent()));
    }
    
    private void displayLevelProgress() {
        int currentLevel = playerProfile.getLevel();
        long currentExp = playerProfile.getExperience();
        
        // Calculate experience needed for next level (simplified formula)
        long expForNextLevel = (currentLevel + 1) * 1000L;
        long expForCurrentLevel = currentLevel * 1000L;
        long expInCurrentLevel = currentExp - expForCurrentLevel;
        long expNeededForNext = expForNextLevel - expForCurrentLevel;
        
        if (expNeededForNext > 0) {
            int progress = (int) ((expInCurrentLevel * 100) / expNeededForNext);
            levelProgressBar.setProgress(Math.max(0, Math.min(100, progress)));
            levelProgressText.setText(numberFormat.format(expInCurrentLevel) + " / " + 
                    numberFormat.format(expNeededForNext) + " XP");
        } else {
            levelProgressBar.setProgress(100);
            levelProgressText.setText("Max Level");
        }
    }
    
    private void displayBadges() {
        // TODO: Implement badges adapter
        // For now, hide the badges section if no badges
        if (playerProfile.getBadges().isEmpty()) {
            findViewById(R.id.badges_section).setVisibility(View.GONE);
        }
    }
    
    private void displayAchievements() {
        // TODO: Implement achievements adapter
        // For now, hide the achievements section if no achievements
        if (playerProfile.getUnlockedAchievements().isEmpty()) {
            findViewById(R.id.achievements_section).setVisibility(View.GONE);
        }
    }
    
    private void displayRecentActivity() {
        // TODO: Implement recent activity adapter
        // For now, hide the recent activity section if no activities
        if (playerProfile.getRecentActivity() == null || 
            playerProfile.getRecentActivity().getRecentActivities().isEmpty()) {
            findViewById(R.id.recent_activity_section).setVisibility(View.GONE);
        }
    }
    
    private void showLoading(boolean show) {
        loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void showError(String message) {
        showLoading(false);
        errorText.setText(message);
        errorCard.setVisibility(View.VISIBLE);
    }
    
    private String formatTimeAgo(long timeDiff) {
        if (timeDiff < 60 * 1000) {
            return "just now";
        } else if (timeDiff < 60 * 60 * 1000) {
            return (timeDiff / (60 * 1000)) + " minutes ago";
        } else if (timeDiff < 24 * 60 * 60 * 1000) {
            return (timeDiff / (60 * 60 * 1000)) + " hours ago";
        } else {
            return (timeDiff / (24 * 60 * 60 * 1000)) + " days ago";
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statisticsManager != null) {
            statisticsManager.clearCache();
        }
    }
}
