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

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private Set<CapturedTreasure> capturedTreasures = new HashSet<>();
    private SharedPreferences sharedPreferences;
    private ImageView avatarImageView, bannerImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView capturedTreasuresCount = findViewById(R.id.captured_treasures_count);
        GridView treasureGrid = findViewById(R.id.treasure_grid);
        avatarImageView = findViewById(R.id.avatarImageView);
        bannerImageView = findViewById(R.id.bannerImageView);

        sharedPreferences = getSharedPreferences("TreasureHuntPrefs", MODE_PRIVATE);
        loadCapturedTreasures();
        MainActivity.capturedTreasuresCount = capturedTreasures.size();

        capturedTreasuresCount.setText(String.valueOf(capturedTreasures.size()));

        fetchPexelsImage("pirate avatar", avatarImageView);
        fetchPexelsImage("pirate banner", bannerImageView);

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

    private void fetchPexelsImage(String query, ImageView imageView) {
        String url = "https://api.pexels.com/v1/search?query=" + query + "&per_page=1";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                String imageUrl = response.getJSONArray("photos").getJSONObject(0).getJSONObject("src").getString("large");
                Glide.with(this).load(imageUrl).into(imageView);
            } catch (Exception e) {
                // fallback or ignore
            }
        }, error -> {}) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", BuildConfig.PEXELS_API_KEY);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }
}