package com.treasurehuntadventure.tha;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class THAMessagingService extends FirebaseMessagingService {
    private static final String TAG = "THAMessagingService";
    private static final String CHANNEL_ID = "THA_NOTIFICATIONS";
    private static final String CHANNEL_NAME = "Treasure Hunt Adventure";
    private static final String CHANNEL_DESCRIPTION = "Notifications for treasure alerts, achievements, and game updates";
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
        
        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }
    }
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Send token to your app server
        sendTokenToServer(token);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void showNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add extra data to intent
        if (data != null) {
            for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.jollyroger)
            .setContentTitle(title != null ? title : "Treasure Hunt Adventure")
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        // Set different icons and colors based on notification type
        String type = data != null ? data.get("type") : null;
        if (type != null) {
            switch (type) {
                case "treasure":
                    notificationBuilder.setColor(getResources().getColor(R.color.treasure_gold));
                    break;
                case "achievement":
                    notificationBuilder.setColor(getResources().getColor(R.color.achievement_gold));
                    break;
                case "level_up":
                    notificationBuilder.setColor(getResources().getColor(R.color.level_up_blue));
                    break;
                default:
                    notificationBuilder.setColor(getResources().getColor(R.color.pirate_blue));
                    break;
            }
        }
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(0, notificationBuilder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for notification", e);
        }
    }
    
    private void handleDataMessage(java.util.Map<String, String> data) {
        String type = data.get("type");
        
        if (type != null) {
            switch (type) {
                case "treasure_nearby":
                    handleTreasureNearbyNotification(data);
                    break;
                case "achievement_unlocked":
                    handleAchievementNotification(data);
                    break;
                case "level_up":
                    handleLevelUpNotification(data);
                    break;
                case "daily_reward":
                    handleDailyRewardNotification(data);
                    break;
                default:
                    Log.w(TAG, "Unknown notification type: " + type);
                    break;
            }
        }
    }
    
    private void handleTreasureNearbyNotification(java.util.Map<String, String> data) {
        String title = "🏴‍☠️ Treasure Nearby!";
        String body = "A treasure has been spotted nearby. Set sail to claim it!";
        
        String treasureType = data.get("treasure_type");
        if (treasureType != null) {
            body = "A " + treasureType + " treasure awaits you nearby!";
        }
        
        showNotification(title, body, data);
    }
    
    private void handleAchievementNotification(java.util.Map<String, String> data) {
        String title = "🏆 Achievement Unlocked!";
        String achievementName = data.get("achievement_name");
        String body = achievementName != null ? 
            "You've unlocked: " + achievementName : 
            "You've unlocked a new achievement!";
        
        showNotification(title, body, data);
    }
    
    private void handleLevelUpNotification(java.util.Map<String, String> data) {
        String title = "⚡ Level Up!";
        String level = data.get("level");
        String body = level != null ? 
            "Congratulations! You've reached level " + level : 
            "You've leveled up, Captain!";
        
        showNotification(title, body, data);
    }
    
    private void handleDailyRewardNotification(java.util.Map<String, String> data) {
        String title = "💰 Daily Reward Available!";
        String body = "Don't forget to claim your daily treasure reward!";
        
        showNotification(title, body, data);
    }
    
    private void sendTokenToServer(String token) {
        // In a real app, you would send this token to your server
        // For now, we'll just store it locally
        getSharedPreferences("TreasureHuntPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply();
        
        Log.d(TAG, "FCM Token stored locally: " + token);
    }
}
