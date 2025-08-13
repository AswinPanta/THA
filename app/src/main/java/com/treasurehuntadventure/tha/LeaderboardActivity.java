package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.treasurehuntadventure.tha.leaderboard.LeaderboardManager;
import com.treasurehuntadventure.tha.leaderboard.PlayerProfile;
import com.treasurehuntadventure.tha.leaderboard.RankingManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaderboardActivity extends AppCompatActivity implements LeaderboardManager.LeaderboardEventListener {
    private static final String TAG = "LeaderboardActivity";

    // UI Components
    private RecyclerView leaderboardRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout categoryTabLayout;
    private ChipGroup periodChipGroup;
    private ProgressBar loadingProgressBar;
    private FloatingActionButton myRankFab;
    private TextView emptyStateText;

    // Enhanced leaderboard system
    private LeaderboardManager leaderboardManager;
    private EnhancedLeaderboardAdapter adapter;
    private List<RankingManager.RankedPlayer> playerList = new ArrayList<>();

    // Current state
    private RankingManager.RankingCategory currentCategory = RankingManager.RankingCategory.OVERALL;
    private RankingManager.RankingPeriod currentPeriod = RankingManager.RankingPeriod.ALL_TIME;
    private String currentUserId;
    private boolean isLoading = false;
    private String nextCursor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_leaderboard);

        // Initialize user
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Initialize UI
        initializeViews();
        setupTabs();
        setupRecyclerView();
        setupEventListeners();

        // Initialize leaderboard system
        initializeLeaderboardSystem();

        // Load initial data
        loadLeaderboard(false);

        Log.d(TAG, "Enhanced leaderboard activity created");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (leaderboardManager != null) {
            // Start real-time updates for top 20 players
            leaderboardManager.startRealtimeUpdates(currentCategory, currentPeriod, 20);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (leaderboardManager != null) {
            leaderboardManager.stopRealtimeUpdates(currentCategory, currentPeriod);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leaderboardManager != null) {
            leaderboardManager.cleanup();
        }
    }

    private void initializeViews() {
        leaderboardRecyclerView = findViewById(R.id.leaderboard_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        categoryTabLayout = findViewById(R.id.category_tab_layout);
        periodChipGroup = findViewById(R.id.period_chip_group);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        myRankFab = findViewById(R.id.my_rank_fab);
        emptyStateText = findViewById(R.id.empty_state_text);

        // Set initial states
        emptyStateText.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void setupTabs() {
        // Add category tabs
        for (RankingManager.RankingCategory category : RankingManager.RankingCategory.values()) {
            TabLayout.Tab tab = categoryTabLayout.newTab();
            tab.setText(category.getDisplayName());
            tab.setTag(category);
            categoryTabLayout.addTab(tab);
        }

        // Setup period chips
        for (RankingManager.RankingPeriod period : RankingManager.RankingPeriod.values()) {
            Chip chip = new Chip(this);
            chip.setText(period.getDisplayName());
            chip.setTag(period);
            chip.setCheckable(true);
            chip.setChecked(period == currentPeriod);
            periodChipGroup.addView(chip);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        leaderboardRecyclerView.setLayoutManager(layoutManager);

        adapter = new EnhancedLeaderboardAdapter(playerList, currentUserId);
        leaderboardRecyclerView.setAdapter(adapter);

        // Add scroll listener for pagination
        leaderboardRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && nextCursor != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMorePlayers();
                    }
                }
            }
        });
    }

    private void setupEventListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> loadLeaderboard(true));

        // Category tab selection
        categoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                RankingManager.RankingCategory category = (RankingManager.RankingCategory) tab.getTag();
                if (category != null && category != currentCategory) {
                    currentCategory = category;
                    loadLeaderboard(true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Period chip selection
        periodChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip checkedChip = findViewById(checkedId);
            if (checkedChip != null) {
                RankingManager.RankingPeriod period = (RankingManager.RankingPeriod) checkedChip.getTag();
                if (period != null && period != currentPeriod) {
                    currentPeriod = period;
                    loadLeaderboard(true);
                }
            }
        });

        // My rank FAB
        myRankFab.setOnClickListener(v -> showMyRank());
    }

    private void initializeLeaderboardSystem() {
        leaderboardManager = new LeaderboardManager(this);
        leaderboardManager.setEventListener(this);
    }

    private void loadLeaderboard(boolean refresh) {
        if (isLoading) return;

        if (refresh) {
            nextCursor = null;
            playerList.clear();
            adapter.notifyDataSetChanged();
        }

        isLoading = true;
        loadingProgressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        leaderboardManager.getLeaderboard(currentCategory, currentPeriod, 20, nextCursor)
                .thenAccept(leaderboardPage -> {
                    runOnUiThread(() -> {
                        isLoading = false;
                        loadingProgressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        if (refresh) {
                            playerList.clear();
                        }

                        playerList.addAll(leaderboardPage.getPlayers());
                        nextCursor = leaderboardPage.getNextCursor();

                        adapter.notifyDataSetChanged();

                        if (playerList.isEmpty()) {
                            emptyStateText.setVisibility(View.VISIBLE);
                            emptyStateText.setText("No players found for " + currentCategory.getDisplayName());
                        }

                        Log.d(TAG, "Loaded " + leaderboardPage.getPlayers().size() + " players");
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        isLoading = false;
                        loadingProgressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        Toast.makeText(this, "Failed to load leaderboard: " + throwable.getMessage(),
                                Toast.LENGTH_LONG).show();

                        if (playerList.isEmpty()) {
                            emptyStateText.setVisibility(View.VISIBLE);
                            emptyStateText.setText("Failed to load leaderboard");
                        }
                    });
                    return null;
                });
    }

    private void loadMorePlayers() {
        if (isLoading || nextCursor == null) return;

        isLoading = true;

        leaderboardManager.getLeaderboard(currentCategory, currentPeriod, 20, nextCursor)
                .thenAccept(leaderboardPage -> {
                    runOnUiThread(() -> {
                        isLoading = false;

                        int oldSize = playerList.size();
                        playerList.addAll(leaderboardPage.getPlayers());
                        nextCursor = leaderboardPage.getNextCursor();

                        adapter.notifyItemRangeInserted(oldSize, leaderboardPage.getPlayers().size());

                        Log.d(TAG, "Loaded " + leaderboardPage.getPlayers().size() + " more players");
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        isLoading = false;
                        Toast.makeText(this, "Failed to load more players", Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }

    private void showMyRank() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to see your rank", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);

        leaderboardManager.getPlayerContext(currentUserId, currentCategory, currentPeriod, 10)
                .thenAccept(contextPage -> {
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);

                        // Replace current list with context
                        playerList.clear();
                        playerList.addAll(contextPage.getPlayers());
                        nextCursor = null; // No pagination for context view

                        adapter.notifyDataSetChanged();

                        // Scroll to current player
                        for (int i = 0; i < playerList.size(); i++) {
                            if (playerList.get(i).getPlayerId().equals(currentUserId)) {
                                leaderboardRecyclerView.scrollToPosition(i);
                                break;
                            }
                        }

                        Toast.makeText(this, "Showing your ranking context", Toast.LENGTH_SHORT).show();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not find your rank", Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }

    // LeaderboardManager.LeaderboardEventListener implementation
    @Override
    public void onLeaderboardUpdated(RankingManager.RankingCategory category,
                                   RankingManager.RankingPeriod period,
                                   List<RankingManager.RankedPlayer> players) {
        if (category == currentCategory && period == currentPeriod) {
            runOnUiThread(() -> {
                // Update only if this is a real-time update (not initial load)
                if (!playerList.isEmpty()) {
                    playerList.clear();
                    playerList.addAll(players);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(this, "Leaderboard updated", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onPlayerRankChanged(String playerId, int oldRank, int newRank,
                                  RankingManager.RankingCategory category) {
        if (category == currentCategory) {
            runOnUiThread(() -> {
                if (playerId.equals(currentUserId)) {
                    Toast.makeText(this, "Your rank changed to #" + newRank, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onLeaderboardError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Leaderboard error: " + error, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onLoadingStateChanged(boolean isLoading) {
        runOnUiThread(() -> {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    // Enhanced Leaderboard Adapter
    static class EnhancedLeaderboardAdapter extends RecyclerView.Adapter<EnhancedLeaderboardAdapter.ViewHolder> {
        private final List<RankingManager.RankedPlayer> players;
        private final String currentUserId;
        private final NumberFormat numberFormat;
        private final SimpleDateFormat dateFormat;

        EnhancedLeaderboardAdapter(List<RankingManager.RankedPlayer> players, String currentUserId) {
            this.players = players;
            this.currentUserId = currentUserId;
            this.numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_enhanced_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RankingManager.RankedPlayer player = players.get(position);

            // Set rank with special styling for top 3
            holder.rankText.setText(String.valueOf(player.getRank()));
            if (player.getRank() <= 3) {
                holder.rankBadge.setVisibility(View.VISIBLE);
                holder.rankText.setVisibility(View.GONE);

                switch (player.getRank()) {
                    case 1:
                        holder.rankBadge.setImageResource(R.drawable.ic_trophy_gold);
                        break;
                    case 2:
                        holder.rankBadge.setImageResource(R.drawable.ic_trophy_silver);
                        break;
                    case 3:
                        holder.rankBadge.setImageResource(R.drawable.ic_trophy_bronze);
                        break;
                }
            } else {
                holder.rankBadge.setVisibility(View.GONE);
                holder.rankText.setVisibility(View.VISIBLE);
            }

            // Set player info
            holder.playerNameText.setText(player.getDisplayName() != null ?
                    player.getDisplayName() : "Anonymous Pirate");
            holder.playerTitleText.setText(player.getCurrentTitle() != null ?
                    player.getCurrentTitle() : "Rookie Pirate");
            holder.playerLevelText.setText("Level " + player.getLevel());

            // Set score
            holder.scoreText.setText(numberFormat.format(player.getScore()));

            // Set avatar
            if (player.getAvatarUrl() != null && !player.getAvatarUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(player.getAvatarUrl())
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .circleCrop()
                        .into(holder.avatarImage);
            } else {
                holder.avatarImage.setImageResource(R.drawable.default_avatar);
            }

            // Highlight current user
            boolean isCurrentUser = player.getPlayerId().equals(currentUserId);
            holder.playerCard.setCardBackgroundColor(holder.itemView.getContext().getColor(
                    isCurrentUser ? R.color.pirate_gold_transparent : R.color.pirate_black_transparent));

            // Set last updated
            if (player.getLastUpdated() > 0) {
                holder.lastActiveText.setText("Active " +
                        dateFormat.format(new Date(player.getLastUpdated())));
                holder.lastActiveText.setVisibility(View.VISIBLE);
            } else {
                holder.lastActiveText.setVisibility(View.GONE);
            }

            // Display top badges
            displayTopBadges(holder, player.getTopBadges());

            // Click listener for player profile
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), PlayerProfileActivity.class);
                intent.putExtra("playerId", player.getPlayerId());
                intent.putExtra("playerName", player.getDisplayName());
                v.getContext().startActivity(intent);
            });

            // Animate item appearance
            holder.itemView.setAlpha(0f);
            holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(position * 50L)
                    .start();
        }

        @Override
        public int getItemCount() {
            return players.size();
        }

        private void displayTopBadges(ViewHolder holder, List<PlayerProfile.PlayerBadge> badges) {
            // Hide all badge views first
            holder.badge1.setVisibility(View.GONE);
            holder.badge2.setVisibility(View.GONE);
            holder.badge3.setVisibility(View.GONE);

            if (badges != null && !badges.isEmpty()) {
                ImageView[] badgeViews = {holder.badge1, holder.badge2, holder.badge3};

                for (int i = 0; i < Math.min(badges.size(), 3); i++) {
                    PlayerProfile.PlayerBadge badge = badges.get(i);
                    ImageView badgeView = badgeViews[i];

                    badgeView.setVisibility(View.VISIBLE);

                    if (badge.getIconUrl() != null && !badge.getIconUrl().isEmpty()) {
                        Glide.with(holder.itemView.getContext())
                                .load(badge.getIconUrl())
                                .placeholder(R.drawable.ic_badge_default)
                                .error(R.drawable.ic_badge_default)
                                .into(badgeView);
                    } else {
                        badgeView.setImageResource(R.drawable.ic_badge_default);
                    }
                }
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CardView playerCard;
            TextView rankText;
            ImageView rankBadge;
            ImageView avatarImage;
            TextView playerNameText;
            TextView playerTitleText;
            TextView playerLevelText;
            TextView scoreText;
            TextView lastActiveText;
            ImageView badge1, badge2, badge3;

            ViewHolder(View itemView) {
                super(itemView);
                playerCard = itemView.findViewById(R.id.player_card);
                rankText = itemView.findViewById(R.id.rank_text);
                rankBadge = itemView.findViewById(R.id.rank_badge);
                avatarImage = itemView.findViewById(R.id.avatar_image);
                playerNameText = itemView.findViewById(R.id.player_name_text);
                playerTitleText = itemView.findViewById(R.id.player_title_text);
                playerLevelText = itemView.findViewById(R.id.player_level_text);
                scoreText = itemView.findViewById(R.id.score_text);
                lastActiveText = itemView.findViewById(R.id.last_active_text);
                badge1 = itemView.findViewById(R.id.badge_1);
                badge2 = itemView.findViewById(R.id.badge_2);
                badge3 = itemView.findViewById(R.id.badge_3);
            }
        }
    }
}