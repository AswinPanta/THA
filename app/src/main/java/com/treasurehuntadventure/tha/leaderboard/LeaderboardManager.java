package com.treasurehuntadventure.tha.leaderboard;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time leaderboard manager with Firebase Firestore integration and efficient pagination
 */
public class LeaderboardManager {
    private static final String TAG = "LeaderboardManager";
    
    // Pagination settings
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    // Cache settings
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final RankingManager rankingManager;
    
    // Real-time listeners
    private final Map<String, ListenerRegistration> activeListeners = new ConcurrentHashMap<>();
    
    // Pagination state
    private final Map<String, PaginationState> paginationStates = new ConcurrentHashMap<>();
    
    // Cache
    private final Map<String, CachedLeaderboard> leaderboardCache = new ConcurrentHashMap<>();
    
    // Event listeners
    private LeaderboardEventListener eventListener;
    
    public interface LeaderboardEventListener {
        void onLeaderboardUpdated(RankingManager.RankingCategory category, 
                                RankingManager.RankingPeriod period, 
                                List<RankingManager.RankedPlayer> players);
        void onPlayerRankChanged(String playerId, int oldRank, int newRank, 
                               RankingManager.RankingCategory category);
        void onLeaderboardError(String error);
        void onLoadingStateChanged(boolean isLoading);
    }
    
    public LeaderboardManager(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.rankingManager = new RankingManager(context);
        
        // Setup ranking manager listener
        rankingManager.setRankingUpdateListener(new RankingManager.RankingUpdateListener() {
            @Override
            public void onRankingCalculated(String playerId, RankingManager.RankingCategory category, 
                                          int newRank, long newScore) {
                invalidateCache(category, RankingManager.RankingPeriod.ALL_TIME);
                if (eventListener != null) {
                    // We don't have old rank here, so pass -1
                    eventListener.onPlayerRankChanged(playerId, -1, newRank, category);
                }
            }
            
            @Override
            public void onRankingError(String playerId, String error) {
                if (eventListener != null) {
                    eventListener.onLeaderboardError("Ranking error for " + playerId + ": " + error);
                }
            }
            
            @Override
            public void onBatchRankingComplete(RankingManager.RankingCategory category, int playersUpdated) {
                Log.d(TAG, "Batch ranking complete for " + category + ": " + playersUpdated + " players");
                invalidateCache(category, RankingManager.RankingPeriod.ALL_TIME);
            }
        });
        
        Log.d(TAG, "Leaderboard manager initialized");
    }
    
    /**
     * Gets leaderboard with pagination support
     */
    public CompletableFuture<LeaderboardPage> getLeaderboard(RankingManager.RankingCategory category,
                                                           RankingManager.RankingPeriod period,
                                                           int pageSize) {
        return getLeaderboard(category, period, pageSize, null);
    }
    
    /**
     * Gets leaderboard page with cursor-based pagination
     */
    public CompletableFuture<LeaderboardPage> getLeaderboard(RankingManager.RankingCategory category,
                                                           RankingManager.RankingPeriod period,
                                                           int pageSize,
                                                           String cursor) {
        CompletableFuture<LeaderboardPage> future = new CompletableFuture<>();
        
        // Validate page size
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        
        // Check cache first
        String cacheKey = getCacheKey(category, period, pageSize, cursor);
        CachedLeaderboard cached = leaderboardCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            future.complete(cached.getLeaderboardPage());
            return future;
        }
        
        if (eventListener != null) {
            eventListener.onLoadingStateChanged(true);
        }
        
        // Build query
        String collectionPath = "rankings/" + category.getId() + "/" + period.getId();
        Query query = firestore.collection(collectionPath)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("lastUpdated", Query.Direction.ASCENDING);
        
        // Apply cursor if provided
        if (cursor != null) {
            PaginationState state = paginationStates.get(cacheKey);
            if (state != null && state.getLastDocument() != null) {
                query = query.startAfter(state.getLastDocument());
            }
        }
        
        query = query.limit(pageSize);
        
        // Execute query
        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RankingManager.RankedPlayer> players = new ArrayList<>();
                    DocumentSnapshot lastDocument = null;
                    int startRank = cursor != null ? getStartRankFromCursor(cursor) : 1;
                    
                    int rank = startRank;
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        RankingManager.RankedPlayer player = document.toObject(RankingManager.RankedPlayer.class);
                        player.setRank(rank++);
                        players.add(player);
                        lastDocument = document;
                    }
                    
                    // Create pagination info
                    boolean hasNextPage = querySnapshot.size() == pageSize;
                    String nextCursor = hasNextPage ? createCursor(lastDocument, rank - 1) : null;
                    
                    LeaderboardPage page = new LeaderboardPage(players, nextCursor, hasNextPage, 
                            startRank, category, period);
                    
                    // Update pagination state
                    if (hasNextPage) {
                        paginationStates.put(cacheKey, new PaginationState(lastDocument, rank - 1));
                    }
                    
                    // Cache result
                    leaderboardCache.put(cacheKey, new CachedLeaderboard(page));
                    
                    if (eventListener != null) {
                        eventListener.onLoadingStateChanged(false);
                        eventListener.onLeaderboardUpdated(category, period, players);
                    }
                    
                    future.complete(page);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load leaderboard", e);
                    if (eventListener != null) {
                        eventListener.onLoadingStateChanged(false);
                        eventListener.onLeaderboardError("Failed to load leaderboard: " + e.getMessage());
                    }
                    future.completeExceptionally(e);
                });
        
        return future;
    }
    
    /**
     * Sets up real-time listener for leaderboard updates
     */
    public void startRealtimeUpdates(RankingManager.RankingCategory category,
                                   RankingManager.RankingPeriod period,
                                   int topPlayersCount) {
        String listenerKey = category.getId() + "_" + period.getId();
        
        // Remove existing listener if any
        stopRealtimeUpdates(category, period);
        
        String collectionPath = "rankings/" + category.getId() + "/" + period.getId();
        
        ListenerRegistration listener = firestore.collection(collectionPath)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("lastUpdated", Query.Direction.ASCENDING)
                .limit(topPlayersCount)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Real-time listener error", e);
                        if (eventListener != null) {
                            eventListener.onLeaderboardError("Real-time update failed: " + e.getMessage());
                        }
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<RankingManager.RankedPlayer> players = new ArrayList<>();
                        int rank = 1;
                        
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            RankingManager.RankedPlayer player = document.toObject(RankingManager.RankedPlayer.class);
                            player.setRank(rank++);
                            players.add(player);
                        }
                        
                        // Invalidate cache for this category/period
                        invalidateCache(category, period);
                        
                        if (eventListener != null) {
                            eventListener.onLeaderboardUpdated(category, period, players);
                        }
                        
                        Log.d(TAG, "Real-time leaderboard updated: " + players.size() + " players");
                    }
                });
        
        activeListeners.put(listenerKey, listener);
        Log.d(TAG, "Started real-time updates for " + category + " - " + period);
    }
    
    /**
     * Stops real-time updates for specific category and period
     */
    public void stopRealtimeUpdates(RankingManager.RankingCategory category,
                                  RankingManager.RankingPeriod period) {
        String listenerKey = category.getId() + "_" + period.getId();
        ListenerRegistration listener = activeListeners.remove(listenerKey);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "Stopped real-time updates for " + category + " - " + period);
        }
    }
    
    /**
     * Gets player's position around them in leaderboard
     */
    public CompletableFuture<LeaderboardPage> getPlayerContext(String playerId,
                                                             RankingManager.RankingCategory category,
                                                             RankingManager.RankingPeriod period,
                                                             int contextSize) {
        CompletableFuture<LeaderboardPage> future = new CompletableFuture<>();
        
        // First get player's rank
        rankingManager.getPlayerRank(playerId, category, period)
                .thenAccept(playerRank -> {
                    if (playerRank <= 0) {
                        future.completeExceptionally(new Exception("Player not found in rankings"));
                        return;
                    }
                    
                    // Calculate range around player
                    int halfContext = contextSize / 2;
                    int startRank = Math.max(1, playerRank - halfContext);
                    int endRank = playerRank + halfContext;
                    
                    // Get players in range
                    String collectionPath = "rankings/" + category.getId() + "/" + period.getId();
                    firestore.collection(collectionPath)
                            .orderBy("rank")
                            .whereGreaterThanOrEqualTo("rank", startRank)
                            .whereLessThanOrEqualTo("rank", endRank)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<RankingManager.RankedPlayer> players = new ArrayList<>();
                                
                                for (QueryDocumentSnapshot document : querySnapshot) {
                                    RankingManager.RankedPlayer player = document.toObject(RankingManager.RankedPlayer.class);
                                    players.add(player);
                                }
                                
                                // Sort by rank
                                players.sort((p1, p2) -> Integer.compare(p1.getRank(), p2.getRank()));
                                
                                LeaderboardPage page = new LeaderboardPage(players, null, false, 
                                        startRank, category, period);
                                page.setPlayerContext(true);
                                page.setContextPlayerId(playerId);
                                
                                future.complete(page);
                            })
                            .addOnFailureListener(future::completeExceptionally);
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
        
        return future;
    }
    
    /**
     * Searches for players in leaderboard
     */
    public CompletableFuture<List<RankingManager.RankedPlayer>> searchPlayers(String query,
                                                                            RankingManager.RankingCategory category,
                                                                            RankingManager.RankingPeriod period,
                                                                            int limit) {
        CompletableFuture<List<RankingManager.RankedPlayer>> future = new CompletableFuture<>();
        
        String collectionPath = "rankings/" + category.getId() + "/" + period.getId();
        
        // Search by display name (case-insensitive)
        firestore.collection(collectionPath)
                .whereGreaterThanOrEqualTo("displayName", query.toLowerCase())
                .whereLessThanOrEqualTo("displayName", query.toLowerCase() + "\uf8ff")
                .orderBy("displayName")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RankingManager.RankedPlayer> players = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        RankingManager.RankedPlayer player = document.toObject(RankingManager.RankedPlayer.class);
                        players.add(player);
                    }
                    
                    future.complete(players);
                })
                .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Updates player's ranking data
     */
    public CompletableFuture<Void> updatePlayerRanking(String playerId,
                                                     RankingManager.RankingCategory category,
                                                     RankingManager.RankingPeriod period) {
        return rankingManager.updatePlayerRanking(playerId, category, period)
                .thenAccept(newRank -> {
                    // Invalidate relevant caches
                    invalidateCache(category, period);
                    Log.d(TAG, "Updated ranking for player " + playerId + " to rank " + newRank);
                });
    }
    
    /**
     * Sets event listener
     */
    public void setEventListener(LeaderboardEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Clears all caches
     */
    public void clearCache() {
        leaderboardCache.clear();
        paginationStates.clear();
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Remove all listeners
        for (ListenerRegistration listener : activeListeners.values()) {
            listener.remove();
        }
        activeListeners.clear();
        
        // Clear caches
        clearCache();
        
        Log.d(TAG, "Leaderboard manager cleaned up");
    }
    
    // Private helper methods
    private String getCacheKey(RankingManager.RankingCategory category,
                             RankingManager.RankingPeriod period,
                             int pageSize, String cursor) {
        return category.getId() + "_" + period.getId() + "_" + pageSize + "_" + 
               (cursor != null ? cursor : "first");
    }
    
    private void invalidateCache(RankingManager.RankingCategory category,
                               RankingManager.RankingPeriod period) {
        String prefix = category.getId() + "_" + period.getId();
        leaderboardCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        paginationStates.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }
    
    private String createCursor(DocumentSnapshot document, int rank) {
        if (document != null) {
            return document.getId() + "_" + rank;
        }
        return null;
    }
    
    private int getStartRankFromCursor(String cursor) {
        if (cursor != null && cursor.contains("_")) {
            try {
                String[] parts = cursor.split("_");
                return Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid cursor format: " + cursor);
            }
        }
        return 1;
    }
    
    /**
     * Leaderboard page data class
     */
    public static class LeaderboardPage {
        private final List<RankingManager.RankedPlayer> players;
        private final String nextCursor;
        private final boolean hasNextPage;
        private final int startRank;
        private final RankingManager.RankingCategory category;
        private final RankingManager.RankingPeriod period;
        private boolean isPlayerContext = false;
        private String contextPlayerId;
        
        public LeaderboardPage(List<RankingManager.RankedPlayer> players, String nextCursor,
                             boolean hasNextPage, int startRank,
                             RankingManager.RankingCategory category,
                             RankingManager.RankingPeriod period) {
            this.players = players;
            this.nextCursor = nextCursor;
            this.hasNextPage = hasNextPage;
            this.startRank = startRank;
            this.category = category;
            this.period = period;
        }
        
        // Getters
        public List<RankingManager.RankedPlayer> getPlayers() { return players; }
        public String getNextCursor() { return nextCursor; }
        public boolean hasNextPage() { return hasNextPage; }
        public int getStartRank() { return startRank; }
        public RankingManager.RankingCategory getCategory() { return category; }
        public RankingManager.RankingPeriod getPeriod() { return period; }
        public boolean isPlayerContext() { return isPlayerContext; }
        public String getContextPlayerId() { return contextPlayerId; }
        
        public void setPlayerContext(boolean playerContext) { this.isPlayerContext = playerContext; }
        public void setContextPlayerId(String contextPlayerId) { this.contextPlayerId = contextPlayerId; }
    }
    
    /**
     * Pagination state for cursor-based pagination
     */
    private static class PaginationState {
        private final DocumentSnapshot lastDocument;
        private final int lastRank;
        
        public PaginationState(DocumentSnapshot lastDocument, int lastRank) {
            this.lastDocument = lastDocument;
            this.lastRank = lastRank;
        }
        
        public DocumentSnapshot getLastDocument() { return lastDocument; }
        public int getLastRank() { return lastRank; }
    }
    
    /**
     * Cached leaderboard with expiry
     */
    private static class CachedLeaderboard {
        private final LeaderboardPage leaderboardPage;
        private final long timestamp;
        
        public CachedLeaderboard(LeaderboardPage leaderboardPage) {
            this.leaderboardPage = leaderboardPage;
            this.timestamp = System.currentTimeMillis();
        }
        
        public LeaderboardPage getLeaderboardPage() { return leaderboardPage; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
}
