package com.treasurehuntadventure.tha;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ChallengeSeeder {
    private static final String[] DAILY_CHALLENGES = {
        "Find 3 treasures",
        "Walk 1000 meters",
        "Spend 30 minutes exploring",
        "Capture 1 rare treasure",
        "Visit 5 different locations"
    };

    private static final String[] WEEKLY_CHALLENGES = {
        "Find 20 treasures",
        "Walk 10,000 meters",
        "Spend 5 hours exploring",
        "Capture 3 rare treasures",
        "Capture 1 legendary treasure"
    };

    private static final int[] DAILY_TARGETS = {3, 1000, 30, 1, 5};
    private static final int[] WEEKLY_TARGETS = {20, 10000, 300, 3, 1};

    public static void seedChallenges() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Seed daily challenges
        for (int i = 0; i < DAILY_CHALLENGES.length; i++) {
            Map<String, Object> challenge = new HashMap<>();
            challenge.put("description", DAILY_CHALLENGES[i]);
            challenge.put("type", "daily");
            challenge.put("target", DAILY_TARGETS[i]);
            challenge.put("progress", 0);
            challenge.put("completed", false);
            challenge.put("userId", userId);
            challenge.put("timestamp", System.currentTimeMillis());
            
            db.collection("challenges").add(challenge);
        }
        
        // Seed weekly challenges
        for (int i = 0; i < WEEKLY_CHALLENGES.length; i++) {
            Map<String, Object> challenge = new HashMap<>();
            challenge.put("description", WEEKLY_CHALLENGES[i]);
            challenge.put("type", "weekly");
            challenge.put("target", WEEKLY_TARGETS[i]);
            challenge.put("progress", 0);
            challenge.put("completed", false);
            challenge.put("userId", userId);
            challenge.put("timestamp", System.currentTimeMillis());
            
            db.collection("challenges").add(challenge);
        }
    }
}
