package com.treasurehuntadventure.tha;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

public class ARActivity extends AppCompatActivity {
    private static final String TAG = "ARActivity";
    private ImageView treasureImageView;
    private TextView arTitle;
    private String treasureModel;
    private ObjectAnimator pulseAnimator;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_simple);
        
        // Initialize Timber for better logging
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }
        
        Timber.d("Enhanced ARActivity onCreate");
        
        initializeViews();
        setupEnhancedARExperience();
        
        treasureModel = getIntent().getStringExtra("glbFile");
        if (treasureModel == null) {
            treasureModel = "treasure_chest.glb";
        }
        
        Timber.d("Treasure model: %s", treasureModel);
    }
    
    private void initializeViews() {
        treasureImageView = findViewById(R.id.ar_image);
        arTitle = findViewById(R.id.ar_title);
        
        arTitle.setText("🏴‍☠️ Enhanced AR Treasure Hunt");
        
        // Set up treasure image based on model type
        setTreasureImage();
        
        // Add click listener for capture
        treasureImageView.setOnClickListener(v -> captureTreasure());
        
        // Add pulsing animation to treasure image
        startTreasureAnimation();
    }
    
    private void setupEnhancedARExperience() {
        Timber.d("Setting up enhanced AR experience");
        
        // Simulate AR detection with enhanced animations
        new Handler().postDelayed(() -> {
            Toast.makeText(this, "🏴‍☠️ Treasure detected nearby!", Toast.LENGTH_SHORT).show();
            treasureImageView.setVisibility(View.VISIBLE);
        }, 1500);
    }
    
    private void setTreasureImage() {
        // Set treasure image based on model type
        switch (treasureModel) {
            case "gold_bar.glb":
                treasureImageView.setImageResource(R.drawable.treasure_star);
                break;
            case "gold_coin.glb":
                treasureImageView.setImageResource(R.drawable.treasure_star);
                break;
            case "treasure_chest.glb":
            default:
                treasureImageView.setImageResource(R.drawable.jollyroger);
                break;
        }
        treasureImageView.setVisibility(View.GONE); // Hidden initially
    }
    
    private void startTreasureAnimation() {
        // Create pulsing animation
        pulseAnimator = ObjectAnimator.ofFloat(treasureImageView, "scaleX", 1.0f, 1.3f, 1.0f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(treasureImageView, "scaleY", 1.0f, 1.3f, 1.0f);
        pulseY.setDuration(2000);
        pulseY.setRepeatCount(ObjectAnimator.INFINITE);
        pulseY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Start animations when treasure becomes visible
        treasureImageView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (treasureImageView.getVisibility() == View.VISIBLE) {
                pulseAnimator.start();
                pulseY.start();
            }
        });
    }
    
    private void captureTreasure() {
        Timber.d("Capturing treasure with enhanced effects");
        
        // Stop pulsing animation
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        
        // Animate capture effect
        treasureImageView.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .alpha(0.3f)
            .setDuration(300)
            .withEndAction(() -> {
                treasureImageView.animate()
                    .scaleX(0.0f)
                    .scaleY(0.0f)
                    .alpha(0.0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Show success message
                        Toast.makeText(this, "🎉 Treasure Captured! Well done, Captain!", Toast.LENGTH_LONG).show();
                        arTitle.setText("🎉 Mission Complete!");
                        
                        // Return success result after delay
                        new Handler().postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 2000);
                    });
            });
    }
    
    
    public void onBackPressed(android.view.View view) {
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        Timber.d("Enhanced ARActivity destroyed");
    }
}
