package com.treasurehuntadventure.tha;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {
    private static final String TAG = "SecurityManager";
    private static final String PREFS_NAME = "SecurityPrefs";
    private static final int MAX_ACTIONS_PER_MINUTE = 30;
    private static final int MAX_TREASURE_CAPTURES_PER_HOUR = 20;
    private static final float MIN_MOVEMENT_DISTANCE = 5.0f; // meters
    private static final long MIN_TIME_BETWEEN_CAPTURES = 30000; // 30 seconds
    
    private static SecurityManager instance;
    private Context context;
    private SharedPreferences prefs;
    private Map<String, ActionCounter> actionCounters;
    private Map<String, Long> lastActionTimes;
    private Location lastValidLocation;
    private long lastLocationUpdateTime;
    private String deviceFingerprint;
    
    private static class ActionCounter {
        private long windowStart;
        private int count;
        
        public ActionCounter() {
            this.windowStart = System.currentTimeMillis();
            this.count = 0;
        }
        
        public boolean canPerformAction(int maxActions, long windowSizeMs) {
            long now = System.currentTimeMillis();
            
            // Reset window if expired
            if (now - windowStart > windowSizeMs) {
                windowStart = now;
                count = 0;
            }
            
            if (count >= maxActions) {
                return false;
            }
            
            count++;
            return true;
        }
    }
    
    private SecurityManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.actionCounters = new ConcurrentHashMap<>();
        this.lastActionTimes = new ConcurrentHashMap<>();
        this.deviceFingerprint = generateDeviceFingerprint();
        
        Log.d(TAG, "Security Manager initialized");
    }
    
    public static synchronized SecurityManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecurityManager(context);
        }
        return instance;
    }
    
    public boolean validateTreasureCapture(Location playerLocation, Location treasureLocation, String treasureId) {
        // Check rate limiting
        if (!checkRateLimit("treasure_capture", MAX_TREASURE_CAPTURES_PER_HOUR, 60 * 60 * 1000)) {
            Log.w(TAG, "Treasure capture rate limit exceeded");
            return false;
        }
        
        // Check minimum time between captures
        if (!checkMinimumTimeBetweenActions("treasure_capture", MIN_TIME_BETWEEN_CAPTURES)) {
            Log.w(TAG, "Treasure capture too soon after previous capture");
            return false;
        }
        
        // Validate distance to treasure
        if (!validateProximity(playerLocation, treasureLocation)) {
            Log.w(TAG, "Player too far from treasure location");
            return false;
        }
        
        // Check for location spoofing
        if (detectLocationSpoofing(playerLocation)) {
            Log.w(TAG, "Potential location spoofing detected");
            return false;
        }
        
        // Validate treasure exists and is not already captured
        if (!validateTreasureExists(treasureId)) {
            Log.w(TAG, "Invalid treasure ID or already captured");
            return false;
        }
        
        // Update last valid location
        lastValidLocation = playerLocation;
        lastLocationUpdateTime = System.currentTimeMillis();
        
        Log.d(TAG, "Treasure capture validated successfully");
        return true;
    }
    
    public boolean validateLevelUp(int currentLevel, int newLevel, int experience) {
        // Check if level progression is reasonable
        if (newLevel != currentLevel + 1) {
            Log.w(TAG, "Invalid level progression: " + currentLevel + " -> " + newLevel);
            return false;
        }
        
        // Check if experience is sufficient for level up
        PlayerLevel playerLevel = new PlayerLevel(context);
        int requiredExp = playerLevel.getExperienceForLevel(newLevel);
        
        if (experience < requiredExp) {
            Log.w(TAG, "Insufficient experience for level up");
            return false;
        }
        
        // Check rate limiting for level ups
        if (!checkRateLimit("level_up", 10, 60 * 60 * 1000)) { // Max 10 level ups per hour
            Log.w(TAG, "Level up rate limit exceeded");
            return false;
        }
        
        Log.d(TAG, "Level up validated successfully");
        return true;
    }
    
    public boolean validateMovement(Location previousLocation, Location currentLocation) {
        if (previousLocation == null) {
            return true; // First location update
        }
        
        float distance = previousLocation.distanceTo(currentLocation);
        long timeDiff = System.currentTimeMillis() - lastLocationUpdateTime;
        
        // Check for impossible movement speed (faster than 200 km/h)
        if (timeDiff > 0) {
            float speed = (distance / (timeDiff / 1000.0f)) * 3.6f; // Convert to km/h
            if (speed > 200) {
                Log.w(TAG, "Impossible movement speed detected: " + speed + " km/h");
                return false;
            }
        }
        
        // Check minimum movement distance to prevent micro-movements
        if (distance < MIN_MOVEMENT_DISTANCE && timeDiff < 60000) { // Less than 1 minute
            return false;
        }
        
        return true;
    }
    
    public boolean validateAPICall(String apiEndpoint) {
        // Check general API rate limiting
        if (!checkRateLimit("api_call", MAX_ACTIONS_PER_MINUTE, 60 * 1000)) {
            Log.w(TAG, "API call rate limit exceeded");
            return false;
        }
        
        // Check specific endpoint rate limiting
        if (!checkRateLimit("api_" + apiEndpoint, 10, 60 * 1000)) {
            Log.w(TAG, "Specific API endpoint rate limit exceeded: " + apiEndpoint);
            return false;
        }
        
        return true;
    }
    
    private boolean checkRateLimit(String actionType, int maxActions, long windowSizeMs) {
        ActionCounter counter = actionCounters.get(actionType);
        if (counter == null) {
            counter = new ActionCounter();
            actionCounters.put(actionType, counter);
        }
        
        return counter.canPerformAction(maxActions, windowSizeMs);
    }
    
    private boolean checkMinimumTimeBetweenActions(String actionType, long minInterval) {
        Long lastTime = lastActionTimes.get(actionType);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime != null && currentTime - lastTime < minInterval) {
            return false;
        }
        
        lastActionTimes.put(actionType, currentTime);
        return true;
    }
    
    private boolean validateProximity(Location playerLocation, Location treasureLocation) {
        float distance = playerLocation.distanceTo(treasureLocation);
        return distance <= 50.0f; // 50 meters maximum
    }
    
    private boolean detectLocationSpoofing(Location location) {
        // Check if location has mock location flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (location.isFromMockProvider()) {
                Log.w(TAG, "Mock location detected");
                return true;
            }
        }
        
        // Check if location accuracy is suspiciously high
        if (location.hasAccuracy() && location.getAccuracy() < 1.0f) {
            Log.w(TAG, "Suspiciously high accuracy detected");
            return true;
        }
        
        // Check for impossible location jumps
        if (lastValidLocation != null) {
            float distance = lastValidLocation.distanceTo(location);
            long timeDiff = System.currentTimeMillis() - lastLocationUpdateTime;
            
            if (timeDiff > 0) {
                float speed = (distance / (timeDiff / 1000.0f)) * 3.6f; // km/h
                if (speed > 500) { // Faster than airplane
                    Log.w(TAG, "Impossible location jump detected");
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean validateTreasureExists(String treasureId) {
        // In a real implementation, this would check with Firebase
        // For now, we'll just check if the ID is valid format
        return treasureId != null && !treasureId.isEmpty() && treasureId.length() > 10;
    }
    
    private String generateDeviceFingerprint() {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceInfo = Build.MANUFACTURER + Build.MODEL + Build.VERSION.RELEASE;
        String combined = androidId + deviceInfo;
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error generating device fingerprint", e);
            return androidId; // Fallback to Android ID
        }
    }
    
    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }
    
    public void logSecurityEvent(String eventType, String details) {
        long timestamp = System.currentTimeMillis();
        String logEntry = timestamp + "," + eventType + "," + details + "," + deviceFingerprint;
        
        // Store security log locally
        String existingLogs = prefs.getString("security_logs", "");
        String newLogs = existingLogs + "\n" + logEntry;
        
        // Keep only last 100 entries
        String[] lines = newLogs.split("\n");
        if (lines.length > 100) {
            StringBuilder trimmed = new StringBuilder();
            for (int i = lines.length - 100; i < lines.length; i++) {
                trimmed.append(lines[i]).append("\n");
            }
            newLogs = trimmed.toString();
        }
        
        prefs.edit().putString("security_logs", newLogs).apply();
        Log.d(TAG, "Security event logged: " + eventType);
    }
    
    public void clearSecurityLogs() {
        prefs.edit().remove("security_logs").apply();
        Log.d(TAG, "Security logs cleared");
    }
    
    public boolean isDeviceRooted() {
        // Check for common root indicators
        String[] rootIndicators = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };
        
        for (String indicator : rootIndicators) {
            if (new java.io.File(indicator).exists()) {
                Log.w(TAG, "Root indicator found: " + indicator);
                return true;
            }
        }
        
        return false;
    }
}
