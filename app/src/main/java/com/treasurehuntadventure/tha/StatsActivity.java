package com.treasurehuntadventure.tha;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class StatsActivity extends AppCompatActivity {
    private TextView treasuresText, distanceText, timeText, challengesText;
    private String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        treasuresText = findViewById(R.id.statsTreasuresTextView);
        distanceText = findViewById(R.id.statsDistanceTextView);
        timeText = findViewById(R.id.statsTimeTextView);
        challengesText = findViewById(R.id.statsChallengesTextView);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchStats();
    }
    private void fetchStats() {
        FirebaseFirestore.getInstance().collection("players").document(userId).get().addOnSuccessListener(doc -> {
            long treasures = doc.getLong("treasuresCaptured") != null ? doc.getLong("treasuresCaptured") : 0;
            double distance = doc.getDouble("distanceWalked") != null ? doc.getDouble("distanceWalked") : 0;
            long timePlayed = doc.getLong("timePlayed") != null ? doc.getLong("timePlayed") : 0;
            treasuresText.setText("Treasures Found: " + treasures);
            distanceText.setText("Distance Walked: " + String.format("%.2f m", distance));
            timeText.setText("Time Played: " + (timePlayed / 60000) + " min");
        });
        FirebaseFirestore.getInstance().collection("challenges")
            .whereEqualTo("userId", userId)
            .whereEqualTo("completed", true)
            .get().addOnSuccessListener(queryDocumentSnapshots -> {
                int completed = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) completed++;
                challengesText.setText("Challenges Completed: " + completed);
            });
    }
} 