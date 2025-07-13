package com.treasurehuntadventure.tha;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CaptureActivity extends AppCompatActivity {

    private ImageView treasureImage;
    private TextView treasureTitle;
    private TextView treasureRarity;
    private Button captureButton;

    private String treasureId;
    private String treasureImageUrl;
    private String treasureRarityValue;

    private CollectionReference treasuresCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        treasureImage = findViewById(R.id.capture_treasure_image);
        treasureTitle = findViewById(R.id.capture_treasure_title);
        treasureRarity = findViewById(R.id.capture_treasure_rarity);
        captureButton = findViewById(R.id.capture_button);

        Intent intent = getIntent();
        treasureId = intent.getStringExtra("treasureId");
        treasureImageUrl = intent.getStringExtra("treasureImageUrl");
        treasureRarityValue = intent.getStringExtra("treasureRarity");

        treasureTitle.setText(intent.getStringExtra("treasureTitle"));
        treasureRarity.setText(treasureRarityValue);

        Glide.with(this).load(treasureImageUrl).into(treasureImage);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        treasuresCollection = db.collection("treasures");

        captureButton.setOnClickListener(v -> captureTreasure());
    }

    private void captureTreasure() {
        // Disable the button to prevent multiple captures
        captureButton.setEnabled(false);

        // Update treasure status in Firestore
        treasuresCollection.document(treasureId)
                .update("captured", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CaptureActivity.this, "Treasure Captured!", Toast.LENGTH_SHORT).show();
                    // Return to MainActivity
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error updating treasure in Firestore", e);
                    Toast.makeText(CaptureActivity.this, "Failed to capture treasure.", Toast.LENGTH_SHORT).show();
                    // Re-enable the button on failure
                    captureButton.setEnabled(true);
                });
    }
}
