package com.treasurehuntadventure.tha.leaderboard;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic ranking system with multiple categories and real-time score calculation
 */
public class RankingManager {
    private static final String TAG = "RankingManager";
    
    // Firestore collections
    private static final String PLAYERS_COLLECTION = "player_profiles";
    private static final String RANKINGS_COLLECTION = "rankings";
    private static final String RANKING_HISTORY_COLLECTION = "ranking_history";
    
    // Ranking categories
    public enum RankingCategory {
        OVERALL("overall", "Overall Score"),
        TREASURE_HUNTER("treasure_hunter", "Treasure Hunter"),
        EXPLORER("explorer", "Explorer"),
        SOCIAL("social", "Social Butterfly"),
        SPEEDSTER("speedster", "Speed Demon"),
        COLLECTOR("collector", "Treasure Collector"),
        ADVENTURER("adventurer", "Adventurer"),
        LEGEND("legend", "Living Legend");
        
        private final String id;
        private final String displayName;
        
        RankingCategory(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
    }
    
    // Ranking periods
    public enum RankingPeriod {
        ALL_TIME("all_time", "All Time"),
        MONTHLY("monthly", "This Month"),
        WEEKLY("weekly", "This Week"),
        DAILY("daily", "Today");
        
        private final String id;
        private final String displayName;
        
        RankingPeriod(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
    }
    
    // Score calculation weights
    private static final Map<String, Double> SCORE_WEIGHTS = new HashMap<>();
    
    static {
        // Base scoring weights
        SCORE_WEIGHTS.put("treasure_captured", 10.0);
        SCORE_WEIGHTS.put("distance_walked", 0.1);
        SCORE_WEIGHTS.put("time_played", 0.001);
        SCORE_WEIGHTS.put("level", 50.0);
        SCORE_WEIGHTS.put("achievements", 25.0);
        SCORE_WEIGHTS.put("consecutive_days", 15.0);
        SCORE_WEIGHTS.put("social_interactions", 5.0);
        SCORE_WEIGHTS.put("help_given", 20.0);
        
        // Rarity multipliers
        SCORE_WEIGHTS.put("common_multiplier", 1.0);
        SCORE_WEIGHTS.put("uncommon_multiplier", 2.0);
        SCORE_WEIGHTS.put("rare_multiplier", 5.0);
        SCORE_WEIGHTS.put("epic_multiplier", 10.0);
        SCORE_WEIGHTS.put("legendary_multiplier", 25.0);
        SCORE_WEIGHTS.put("mythic_multiplier", 50.0);
    }
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final Map<String, Long> scoreCache = new ConcurrentHashMap<>();
    
    // Ranking calculation listeners
    private RankingUpdateListener rankingUpdateListener;
    
    public interface RankingUpdateListener {
        void onRankingCalculated(String playerId, RankingCategory category, int newRank, long newScore);
        void onRankingError(String playerId, String error);
        void onBatchRankingComplete(RankingCategory category, int playersUpdated);
    }
    
    public RankingManager(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        
        Log.d(TAG, "Ranking manager initialized");
    }
    
    /**
     * Calculates comprehensive score for a player
     */
    public CompletableFuture<Long> calculatePlayerScore(PlayerProfile player, RankingCategory category) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        
        try {
            long score = 0;
            PlayerProfile.PlayerStatistics stats = player.getStatistics();
            
            switch (category) {
                case OVERALL:
                    score = calculateOverallScore(player);
                    break;
                    
                case TREASURE_HUNTER:
                    score = calculateTreasureHunterScore(stats);
                    break;
                    
                case EXPLORER:
                    score = calculateExplorerScore(stats);
                    break;
                    
                case SOCIAL:
                    score = calculateSocialScore(stats);
                    break;
                    
                case SPEEDSTER:
                    score = calculateSpeedsterScore(stats);
                    break;
                    
                case COLLECTOR:
                    score = calculateCollectorScore(stats);
                    break;
                    
                case ADVENTURER:
                    score = calculateAdventurerScore(stats);
                    break;
                    
                case LEGEND:
                    score = calculateLegendScore(player);
                    break;
            }
            
            // Cache the score
            scoreCache.put(player.getPlayerId() + "_" + category.getId(), score);
            
            future.complete(score);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating score for player: " + player.getPlayerId(), e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Updates player ranking in specific category
     */
    public CompletableFuture<Integer> updatePlayerRanking(String playerId, RankingCategory category, 
                                                        RankingPeriod period) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Get player profile first
        firestore.collection(PLAYERS_COLLECTION).document(playerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PlayerProfile player = documentSnapshot.toObject(PlayerProfile.class);
                        if (player != null) {
                            calculatePlayerScore(player, category)
                                    .thenAccept(score -> {
                                        updateRankingInFirestore(playerId, category, period, score, future);
                                    })
                                    .exceptionally(throwable -> {
                                        Log.e(TAG, "Failed to calculate score", throwable);
                                        future.completeExceptionally(throwable);
                                        return null;
                                    });
                        } else {
                            future.completeExceptionally(new Exception("Player profile not found"));
                        }
                    } else {
                        future.completeExceptionally(new Exception("Player document not found"));
                    }
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Recalculates rankings for all players in a category
     */
    public CompletableFuture<Integer> recalculateRankings(RankingCategory category, RankingPeriod period) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Get all players
        firestore.collection(PLAYERS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<CompletableFuture<Void>> updateFutures = new ArrayList<>();
                    
                    querySnapshot.getDocuments().forEach(document -> {
                        PlayerProfile player = document.toObject(PlayerProfile.class);
                        if (player != null) {
                            CompletableFuture<Void> updateFuture = calculatePlayerScore(player, category)
                                    .thenCompose(score -> {
                                        CompletableFuture<Void> updateComplete = new CompletableFuture<>();
                                        updateRankingInFirestore(player.getPlayerId(), category, period, score, 
                                                new CompletableFuture<Integer>() {{
                                                    whenComplete((rank, throwable) -> {
                                                        if (throwable != null) {
                                                            updateComplete.completeExceptionally(throwable);
                                                        } else {
                                                            updateComplete.complete(null);
                                                        }
                                                    });
                                                }});
                                        return updateComplete;
                                    });
                            updateFutures.add(updateFuture);
                        }
                    });
                    
                    // Wait for all updates to complete
                    CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0]))
                            .thenRun(() -> {
                                if (rankingUpdateListener != null) {
                                    rankingUpdateListener.onBatchRankingComplete(category, updateFutures.size());
                                }
                                future.complete(updateFutures.size());
                            })
                            .exceptionally(throwable -> {
                                Log.e(TAG, "Batch ranking update failed", throwable);
                                future.completeExceptionally(throwable);
                                return null;
                            });
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Gets top players for a category and period
     */
    public CompletableFuture<List<RankedPlayer>> getTopPlayers(RankingCategory category, 
                                                             RankingPeriod period, int limit) {
        CompletableFuture<List<RankedPlayer>> future = new CompletableFuture<>();
        
        String collectionPath = RANKINGS_COLLECTION + "/" + category.getId() + "/" + period.getId();
        
        firestore.collection(collectionPath)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("lastUpdated", Query.Direction.ASCENDING) // Tie-breaker
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RankedPlayer> rankedPlayers = new ArrayList<>();
                    int rank = 1;
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        RankedPlayer rankedPlayer = document.toObject(RankedPlayer.class);
                        rankedPlayer.setRank(rank++);
                        rankedPlayers.add(rankedPlayer);
                    }
                    
                    future.complete(rankedPlayers);
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Gets player's rank in specific category
     */
    public CompletableFuture<Integer> getPlayerRank(String playerId, RankingCategory category, 
                                                  RankingPeriod period) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        String collectionPath = RANKINGS_COLLECTION + "/" + category.getId() + "/" + period.getId();
        
        firestore.collection(collectionPath)
                .document(playerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long rank = documentSnapshot.getLong("rank");
                        future.complete(rank != null ? rank.intValue() : -1);
                    } else {
                        future.complete(-1);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get player rank", e);
                    future.complete(-1);
                });
        
        return future;
    }
    
    /**
     * Sets ranking update listener
     */
    public void setRankingUpdateListener(RankingUpdateListener listener) {
        this.rankingUpdateListener = listener;
    }
    
    // Private helper methods
    private long calculateOverallScore(PlayerProfile player) {
        PlayerProfile.PlayerStatistics stats = player.getStatistics();
        long score = 0;
        
        // Base stats
        score += (long) (stats.getTreasuresCaptured() * SCORE_WEIGHTS.get("treasure_captured"));
        score += (long) (stats.getDistanceWalked() * SCORE_WEIGHTS.get("distance_walked"));
        score += (long) (stats.getTimePlayed() * SCORE_WEIGHTS.get("time_played"));
        score += (long) (player.getLevel() * SCORE_WEIGHTS.get("level"));
        score += (long) (player.getUnlockedAchievements().size() * SCORE_WEIGHTS.get("achievements"));
        score += (long) (stats.getConsecutiveDays() * SCORE_WEIGHTS.get("consecutive_days"));
        score += (long) (stats.getChatMessagesSent() * SCORE_WEIGHTS.get("social_interactions"));
        score += (long) (stats.getHelpRequestsAnswered() * SCORE_WEIGHTS.get("help_given"));
        
        // Rarity bonuses
        Map<String, Integer> treasuresByRarity = stats.getTreasuresByRarity();
        if (treasuresByRarity != null) {
            for (TreasureRarity rarity : TreasureRarity.values()) {
                int count = treasuresByRarity.getOrDefault(rarity.name(), 0);
                String multiplierKey = rarity.name().toLowerCase() + "_multiplier";
                Double multiplier = SCORE_WEIGHTS.get(multiplierKey);
                if (multiplier != null) {
                    score += (long) (count * SCORE_WEIGHTS.get("treasure_captured") * multiplier);
                }
            }
        }
        
        return score;
    }
    
    private long calculateTreasureHunterScore(PlayerProfile.PlayerStatistics stats) {
        long score = stats.getTreasuresCaptured() * 100;
        score += stats.getTreasuresWon() * 50;
        score += stats.getPerfectCaptureStreak() * 25;
        score += (stats.getFastestCaptureTime() > 0 ? (60000 / stats.getFastestCaptureTime()) : 0);
        return score;
    }
    
    private long calculateExplorerScore(PlayerProfile.PlayerStatistics stats) {
        long score = (long) (stats.getDistanceWalked() * 10);
        score += stats.getLocationsVisited() * 50;
        score += stats.getLandmarksDiscovered() * 100;
        score += (long) (stats.getMaxDistanceFromHome() * 5);
        return score;
    }
    
    private long calculateSocialScore(PlayerProfile.PlayerStatistics stats) {
        long score = stats.getChatMessagesSent() * 5;
        score += stats.getHelpRequestsAnswered() * 50;
        return score;
    }
    
    private long calculateSpeedsterScore(PlayerProfile.PlayerStatistics stats) {
        long score = stats.getCapturesInOneDay() * 100;
        score += (stats.getFastestCaptureTime() > 0 ? (300000 / stats.getFastestCaptureTime()) : 0);
        score += (long) (stats.getAverageCaptureTime() > 0 ? (60000 / stats.getAverageCaptureTime()) : 0);
        return score;
    }
    
    private long calculateCollectorScore(PlayerProfile.PlayerStatistics stats) {
        long score = 0;
        Map<String, Integer> treasuresByRarity = stats.getTreasuresByRarity();
        if (treasuresByRarity != null) {
            for (TreasureRarity rarity : TreasureRarity.values()) {
                int count = treasuresByRarity.getOrDefault(rarity.name(), 0);
                score += count * getRarityMultiplier(rarity);
            }
        }
        return score;
    }
    
    private long calculateAdventurerScore(PlayerProfile.PlayerStatistics stats) {
        long score = stats.getNightTimeCaptures() * 50;
        score += stats.getWeatherChallengeCaptures() * 75;
        score += stats.getConsecutiveDays() * 25;
        return score;
    }
    
    private long calculateLegendScore(PlayerProfile player) {
        long score = calculateOverallScore(player);
        score += player.getBadges().size() * 100;
        score += (player.getLevel() >= 50 ? 10000 : 0);
        return score;
    }
    
    private int getRarityMultiplier(TreasureRarity rarity) {
        switch (rarity) {
            case COMMON: return 1;
            case UNCOMMON: return 3;
            case RARE: return 10;
            case EPIC: return 25;
            case LEGENDARY: return 100;
            case MYTHIC: return 500;
            default: return 1;
        }
    }
    
    private void updateRankingInFirestore(String playerId, RankingCategory category, 
                                        RankingPeriod period, long score, 
                                        CompletableFuture<Integer> future) {
        String collectionPath = RANKINGS_COLLECTION + "/" + category.getId() + "/" + period.getId();
        
        // First, get current rankings to determine new rank
        firestore.collection(collectionPath)
                .whereGreaterThan("score", score)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int newRank = querySnapshot.size() + 1;
                    
                    // Create ranking entry
                    Map<String, Object> rankingData = new HashMap<>();
                    rankingData.put("playerId", playerId);
                    rankingData.put("score", score);
                    rankingData.put("rank", newRank);
                    rankingData.put("category", category.getId());
                    rankingData.put("period", period.getId());
                    rankingData.put("lastUpdated", System.currentTimeMillis());
                    
                    // Update ranking document
                    firestore.collection(collectionPath)
                            .document(playerId)
                            .set(rankingData)
                            .addOnSuccessListener(aVoid -> {
                                // Update ranks for players with lower scores
                                updateLowerRanks(collectionPath, score, newRank);
                                
                                if (rankingUpdateListener != null) {
                                    rankingUpdateListener.onRankingCalculated(playerId, category, newRank, score);
                                }
                                
                                future.complete(newRank);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update ranking", e);
                                if (rankingUpdateListener != null) {
                                    rankingUpdateListener.onRankingError(playerId, e.getMessage());
                                }
                                future.completeExceptionally(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to calculate rank", e);
                    future.completeExceptionally(e);
                });
    }
    
    private void updateLowerRanks(String collectionPath, long score, int newRank) {
        firestore.collection(collectionPath)
                .whereLessThanOrEqualTo("score", score)
                .whereGreaterThanOrEqualTo("rank", newRank)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = firestore.batch();
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        Long currentRank = document.getLong("rank");
                        if (currentRank != null && currentRank >= newRank) {
                            batch.update(document.getReference(), "rank", currentRank + 1);
                        }
                    }
                    
                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Lower ranks updated successfully"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update lower ranks", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to query lower ranks", e));
    }
    
    /**
     * Ranked player data class
     */
    public static class RankedPlayer {
        private String playerId;
        private String displayName;
        private String avatarUrl;
        private long score;
        private int rank;
        private int level;
        private String currentTitle;
        private long lastUpdated;
        private List<PlayerProfile.PlayerBadge> topBadges;
        
        public RankedPlayer() {
            this.topBadges = new ArrayList<>();
        }
        
        // Getters and setters
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
        public long getScore() { return score; }
        public void setScore(long score) { this.score = score; }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        
        public String getCurrentTitle() { return currentTitle; }
        public void setCurrentTitle(String currentTitle) { this.currentTitle = currentTitle; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public List<PlayerProfile.PlayerBadge> getTopBadges() { return topBadges; }
        public void setTopBadges(List<PlayerProfile.PlayerBadge> topBadges) { this.topBadges = topBadges; }
    }
}
