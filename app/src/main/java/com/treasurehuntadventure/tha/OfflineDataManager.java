package com.treasurehuntadventure.tha;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OfflineDataManager {
    private static final String TAG = "OfflineDataManager";
    private static final String PREFS_NAME = "OfflineDataPrefs";
    private static final String KEY_CACHED_TREASURES = "cached_treasures";
    private static final String KEY_PENDING_ACTIONS = "pending_actions";
    private static final String KEY_PLAYER_PROGRESS = "player_progress";
    private static final String KEY_OFFLINE_IMAGES = "offline_images";
    
    private static OfflineDataManager instance;
    private Context context;
    private SharedPreferences prefs;
    private Gson gson;
    private Queue<PendingAction> pendingActions;
    private Map<String, CachedTreasure> treasureCache;
    private Map<String, String> imageCache;
    private boolean isOnline;
    
    public static class PendingAction {
        public String type;
        public String data;
        public long timestamp;
        
        public PendingAction(String type, String data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class CachedTreasure {
        public String id;
        public String title;
        public String imageUrl;
        public String rarity;
        public double latitude;
        public double longitude;
        public long cacheTime;
        
        public CachedTreasure(String id, String title, String imageUrl, String rarity, 
                             double latitude, double longitude) {
            this.id = id;
            this.title = title;
            this.imageUrl = imageUrl;
            this.rarity = rarity;
            this.latitude = latitude;
            this.longitude = longitude;
            this.cacheTime = System.currentTimeMillis();
        }
    }
    
    private OfflineDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.pendingActions = new ConcurrentLinkedQueue<>();
        this.treasureCache = new HashMap<>();
        this.imageCache = new HashMap<>();
        this.isOnline = true;
        
        loadOfflineData();
    }
    
    public static synchronized OfflineDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineDataManager(context);
        }
        return instance;
    }
    
    public void setOnlineStatus(boolean isOnline) {
        this.isOnline = isOnline;
        Log.d(TAG, "Network status changed: " + (isOnline ? "Online" : "Offline"));
        
        if (isOnline) {
            syncPendingActions();
        }
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void cacheTreasure(Treasure treasure) {
        CachedTreasure cached = new CachedTreasure(
            treasure.getId(),
            treasure.getTitle(),
            treasure.getImageUrl(),
            treasure.getRarity().name(),
            treasure.getLocation().getLatitude(),
            treasure.getLocation().getLongitude()
        );
        
        treasureCache.put(treasure.getId(), cached);
        saveCachedTreasures();
        Log.d(TAG, "Treasure cached: " + treasure.getTitle());
    }
    
    public List<CachedTreasure> getCachedTreasures() {
        return new ArrayList<>(treasureCache.values());
    }
    
    public void addPendingAction(String type, String data) {
        PendingAction action = new PendingAction(type, data);
        pendingActions.offer(action);
        savePendingActions();
        Log.d(TAG, "Added pending action: " + type);
        
        // Try to sync immediately if online
        if (isOnline) {
            syncPendingActions();
        }
    }
    
    public void cacheImage(String url, String base64Data) {
        imageCache.put(url, base64Data);
        saveImageCache();
        Log.d(TAG, "Image cached: " + url);
    }
    
    public String getCachedImage(String url) {
        return imageCache.get(url);
    }
    
    public void cachePlayerProgress(int level, int experience, int coins, int treasureCount) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("level", level);
        progress.put("experience", experience);
        progress.put("coins", coins);
        progress.put("treasureCount", treasureCount);
        progress.put("timestamp", System.currentTimeMillis());
        
        String json = gson.toJson(progress);
        prefs.edit().putString(KEY_PLAYER_PROGRESS, json).apply();
        Log.d(TAG, "Player progress cached");
    }
    
    public Map<String, Object> getCachedPlayerProgress() {
        String json = prefs.getString(KEY_PLAYER_PROGRESS, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new HashMap<>();
    }
    
    private void syncPendingActions() {
        if (pendingActions.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Syncing " + pendingActions.size() + " pending actions");
        
        while (!pendingActions.isEmpty()) {
            PendingAction action = pendingActions.poll();
            processPendingAction(action);
        }
        
        savePendingActions();
    }
    
    private void processPendingAction(PendingAction action) {
        switch (action.type) {
            case "treasure_capture":
                syncTreasureCapture(action.data);
                break;
            case "level_up":
                syncLevelUp(action.data);
                break;
            case "achievement_unlock":
                syncAchievementUnlock(action.data);
                break;
            case "chat_message":
                syncChatMessage(action.data);
                break;
            default:
                Log.w(TAG, "Unknown pending action type: " + action.type);
                break;
        }
    }
    
    private void syncTreasureCapture(String data) {
        // In a real implementation, this would sync with Firebase
        Log.d(TAG, "Syncing treasure capture: " + data);
        // Add Firebase sync logic here
    }
    
    private void syncLevelUp(String data) {
        // In a real implementation, this would sync with Firebase
        Log.d(TAG, "Syncing level up: " + data);
        // Add Firebase sync logic here
    }
    
    private void syncAchievementUnlock(String data) {
        // In a real implementation, this would sync with Firebase
        Log.d(TAG, "Syncing achievement unlock: " + data);
        // Add Firebase sync logic here
    }
    
    private void syncChatMessage(String data) {
        // In a real implementation, this would sync with Firebase
        Log.d(TAG, "Syncing chat message: " + data);
        // Add Firebase sync logic here
    }
    
    public void clearExpiredCache() {
        long currentTime = System.currentTimeMillis();
        long cacheExpiryTime = 24 * 60 * 60 * 1000; // 24 hours
        
        // Clear expired treasures
        treasureCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().cacheTime > cacheExpiryTime);
        
        // Clear expired images (keep for 7 days)
        long imageExpiryTime = 7 * 24 * 60 * 60 * 1000; // 7 days
        // Note: For simplicity, we're not tracking image timestamps here
        // In a real implementation, you would track image cache times
        
        saveCachedTreasures();
        saveImageCache();
        Log.d(TAG, "Expired cache cleared");
    }
    
    private void loadOfflineData() {
        loadCachedTreasures();
        loadPendingActions();
        loadImageCache();
        Log.d(TAG, "Offline data loaded");
    }
    
    private void loadCachedTreasures() {
        String json = prefs.getString(KEY_CACHED_TREASURES, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, CachedTreasure>>(){}.getType();
            treasureCache = gson.fromJson(json, type);
            if (treasureCache == null) {
                treasureCache = new HashMap<>();
            }
        }
    }
    
    private void saveCachedTreasures() {
        String json = gson.toJson(treasureCache);
        prefs.edit().putString(KEY_CACHED_TREASURES, json).apply();
    }
    
    private void loadPendingActions() {
        String json = prefs.getString(KEY_PENDING_ACTIONS, null);
        if (json != null) {
            Type type = new TypeToken<Queue<PendingAction>>(){}.getType();
            Queue<PendingAction> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                pendingActions.addAll(loaded);
            }
        }
    }
    
    private void savePendingActions() {
        String json = gson.toJson(pendingActions);
        prefs.edit().putString(KEY_PENDING_ACTIONS, json).apply();
    }
    
    private void loadImageCache() {
        String json = prefs.getString(KEY_OFFLINE_IMAGES, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            imageCache = gson.fromJson(json, type);
            if (imageCache == null) {
                imageCache = new HashMap<>();
            }
        }
    }
    
    private void saveImageCache() {
        String json = gson.toJson(imageCache);
        prefs.edit().putString(KEY_OFFLINE_IMAGES, json).apply();
    }
    
    public void clearAllCache() {
        treasureCache.clear();
        imageCache.clear();
        pendingActions.clear();
        
        prefs.edit()
            .remove(KEY_CACHED_TREASURES)
            .remove(KEY_PENDING_ACTIONS)
            .remove(KEY_OFFLINE_IMAGES)
            .remove(KEY_PLAYER_PROGRESS)
            .apply();
            
        Log.d(TAG, "All cache cleared");
    }
}
