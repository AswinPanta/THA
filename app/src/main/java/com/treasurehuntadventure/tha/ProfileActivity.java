package com.treasurehuntadventure.tha;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private Set<CapturedTreasure> capturedTreasures = new HashSet<>();
    private SharedPreferences sharedPreferences;
    private ImageView avatarImageView, bannerImageView;
    private TextView playerName, bountyAmount, playerRank;
    private RequestQueue requestQueue;
    
    // One Piece themed avatar and banner images
    private String[] onePieceAvatars = {
        "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=400&fit=crop",
        "https://images.unsplash.com/photo-1550684376-efcbd6e3f031?w=400&h=400&fit=crop",
        "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=400&fit=crop"
    };
    
    private String[] onePieceBanners = {
        "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&h=200&fit=crop",
        "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=200&fit=crop",
        "https://images.unsplash.com/photo-1520637836862-4d197d17c93a?w=800&h=200&fit=crop"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        TextView capturedTreasuresCount = findViewById(R.id.captured_treasures_count);
        GridView treasureGrid = findViewById(R.id.treasure_grid);
        avatarImageView = findViewById(R.id.avatarImageView);
        bannerImageView = findViewById(R.id.bannerImageView);
        playerName = findViewById(R.id.player_name);
        bountyAmount = findViewById(R.id.bounty_amount);
        playerRank = findViewById(R.id.player_rank);

        sharedPreferences = getSharedPreferences("TreasureHuntPrefs", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        
        loadCapturedTreasures();
        MainActivity.capturedTreasuresCount = capturedTreasures.size();

        // Update profile information
        updateProfileInfo();
        capturedTreasuresCount.setText(String.valueOf(capturedTreasures.size()));

        // Load One Piece themed images
        loadOnePieceImages();

        TreasureGridAdapter adapter = new TreasureGridAdapter(this, new ArrayList<>(capturedTreasures));
        treasureGrid.setAdapter(adapter);

        treasureGrid.setOnItemClickListener((parent, view, position, id) -> {
            CapturedTreasure clickedTreasure = (CapturedTreasure) parent.getItemAtPosition(position);
            TreasureDetailsDialog dialog = new TreasureDetailsDialog(ProfileActivity.this, clickedTreasure);
            dialog.show();
        });
    }

    private void loadCapturedTreasures() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString("capturedTreasures", null);
        Type type = new TypeToken<HashSet<CapturedTreasure>>() {}.getType();
        capturedTreasures = gson.fromJson(json, type);
        if (capturedTreasures == null) {
            capturedTreasures = new HashSet<>();
        }
    }

    private void updateProfileInfo() {
        // Calculate total bounty and pirate rank
        int totalBounty = calculateTotalBounty();
        String pirateRank = determinePirateRank(totalBounty);

        // Update UI with bounty and pirate rank
        bountyAmount.setText(String.valueOf(totalBounty));
        playerRank.setText(pirateRank);
    }

    private int calculateTotalBounty() {
        int sum = 0;
        for (CapturedTreasure treasure : capturedTreasures) {
            // Handle bounty field - it could be either String or null
            String bountyStr = treasure.getBounty();
            if (bountyStr != null && !bountyStr.isEmpty()) {
                try {
                    sum += Integer.parseInt(bountyStr);
                } catch (NumberFormatException e) {
                    // Skip if bounty is not a valid number
                }
            }
        }
        return sum;
    }

    private String determinePirateRank(int bounty) {
        if (bounty < 1000) return "Novice";
        else if (bounty < 5000) return "Experienced";
        else if (bounty < 10000) return "Veteran";
        else return "Legendary";
    }

    private void loadOnePieceImages() {
        Random random = new Random();

        // Load random avatar
        String avatarUrl = onePieceAvatars[random.nextInt(onePieceAvatars.length)];
        Glide.with(this).load(avatarUrl).into(avatarImageView);

        // Load random banner
        String bannerUrl = onePieceBanners[random.nextInt(onePieceBanners.length)];
        Glide.with(this).load(bannerUrl).into(bannerImageView);
    }
}