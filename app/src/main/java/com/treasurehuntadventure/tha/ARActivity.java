package com.treasurehuntadventure.tha;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.math.Vector3;
import com.treasurehuntadventure.tha.ar.ARCaptureManager;
import com.treasurehuntadventure.tha.ar.ARCoreEngine;
import com.treasurehuntadventure.tha.ar.ARPlacementManager;
import com.treasurehuntadventure.tha.ar.AR3DTreasureRenderer;
import com.treasurehuntadventure.tha.ar.ObjectRecognitionManager;

import java.util.List;

import io.github.sceneview.ar.ArSceneView;
import timber.log.Timber;

public class ARActivity extends AppCompatActivity implements
        ARCoreEngine.AREngineListener,
        ARCoreEngine.ARTrackingListener,
        ARPlacementManager.PlacementListener,
        AR3DTreasureRenderer.TreasureRenderListener,
        ARCaptureManager.CaptureListener,
        ObjectRecognitionManager.ObjectRecognitionListener {

    private static final String TAG = "ARActivity";

    // UI Components
    private ArSceneView arSceneView;
    private ImageView treasureImageView;
    private TextView arTitle;
    private TextView arStatusText;
    private TextView captureInstructionText;
    private ProgressBar captureProgressBar;
    private View arOverlayContainer;
    private View objectDetectionIndicator;

    // AR System Components
    private ARCoreEngine arEngine;
    private ARPlacementManager placementManager;
    private AR3DTreasureRenderer treasureRenderer;
    private ARCaptureManager captureManager;
    private ObjectRecognitionManager objectRecognition;

    // State
    private String treasureId;
    private TreasureRarity treasureRarity;
    private String treasureModel;
    private boolean arInitialized = false;
    private boolean treasurePlaced = false;
    private String currentAnchorId;

    // Animation and effects
    private ObjectAnimator pulseAnimator;
    private ObjectAnimator pulseY;
    private Handler mainHandler;
    private MediaPlayer soundPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Timber for better logging
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }

        Timber.d("Enhanced ARActivity onCreate");
        mainHandler = new Handler(android.os.Looper.getMainLooper());

        // Get treasure information from intent
        extractTreasureInfo();

        // Check ARCore availability and set appropriate layout
        boolean arAvailable = isArCoreSupported();
        setContentView(arAvailable ? R.layout.activity_ar_enhanced : R.layout.activity_ar_simple);

        // Initialize views and AR system
        initializeViews();

        if (arAvailable) {
            initializeARSystem();
        } else {
            setupFallbackExperience();
        }

        Timber.d("AR Activity initialized - Treasure: %s (%s)", treasureId, treasureRarity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (arEngine != null) {
            arEngine.resume();
        }

        if (arSceneView != null) {
            arSceneView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (arEngine != null) {
            arEngine.pause();
        }

        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (arEngine != null) {
            arEngine.close();
        }

        if (treasureRenderer != null) {
            treasureRenderer.cleanup();
        }

        if (soundPlayer != null) {
            soundPlayer.release();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        // Pass touch events to capture manager
        if (captureManager != null) {
            handled = captureManager.onTouchEvent(event);
        }

        // Handle AR placement if not in capture mode
        if (!handled && arInitialized && !treasurePlaced) {
            handled = handleARPlacement(event);
        }

        return handled || super.onTouchEvent(event);
    }
    
    private void extractTreasureInfo() {
        Intent intent = getIntent();
        treasureId = intent.getStringExtra("treasureId");
        treasureModel = intent.getStringExtra("glbFile");
        String rarityString = intent.getStringExtra("treasureRarity");

        if (treasureId == null) {
            treasureId = "demo_treasure_" + System.currentTimeMillis();
        }

        if (treasureModel == null) {
            treasureModel = "treasure_chest.glb";
        }

        if (rarityString != null) {
            try {
                treasureRarity = TreasureRarity.valueOf(rarityString);
            } catch (IllegalArgumentException e) {
                treasureRarity = TreasureRarity.COMMON;
            }
        } else {
            treasureRarity = TreasureRarity.COMMON;
        }
    }

    private void initializeViews() {
        // Find views
        arSceneView = findViewById(R.id.ar_scene_view);
        treasureImageView = findViewById(R.id.ar_image);
        arTitle = findViewById(R.id.ar_title);
        arStatusText = findViewById(R.id.ar_status_text);
        captureInstructionText = findViewById(R.id.capture_instruction_text);
        captureProgressBar = findViewById(R.id.capture_progress_bar);
        arOverlayContainer = findViewById(R.id.ar_overlay_container);
        objectDetectionIndicator = findViewById(R.id.object_detection_indicator);

        // Set initial UI state
        arTitle.setText("🏴‍☠️ AR Treasure Hunt");
        updateStatusText("Initializing AR...");

        // Set up treasure image based on rarity
        setTreasureImageForRarity();

        // Set up click listeners
        findViewById(R.id.close_button).setOnClickListener(v -> finish());

        // Hide capture UI initially
        if (captureInstructionText != null) captureInstructionText.setVisibility(View.GONE);
        if (captureProgressBar != null) captureProgressBar.setVisibility(View.GONE);
        if (objectDetectionIndicator != null) objectDetectionIndicator.setVisibility(View.GONE);
    }

    private void initializeARSystem() {
        try {
            // Initialize AR engine
            arEngine = new ARCoreEngine(this);
            arEngine.initialize(this);
            arEngine.setTrackingListener(this);

            // Initialize placement manager
            placementManager = new ARPlacementManager(arEngine);
            placementManager.setPlacementListener(this);

            // Initialize object recognition
            objectRecognition = new ObjectRecognitionManager(this);

            // Initialize treasure renderer
            treasureRenderer = new AR3DTreasureRenderer(this, arSceneView);
            treasureRenderer.setRenderListener(this);

            // Initialize capture manager
            captureManager = new ARCaptureManager(this, arEngine, objectRecognition, treasureRenderer);
            captureManager.setCaptureListener(this);

            updateStatusText("AR system initialized");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AR system", e);
            updateStatusText("AR initialization failed");
            showError("Failed to initialize AR: " + e.getMessage());
        }
    }

    private void setupFallbackExperience() {
        updateStatusText("AR not available - using fallback mode");

        if (treasureImageView != null) {
            treasureImageView.setOnClickListener(v -> captureTreasureFallback());
            startTreasureAnimation();
        }

        // Simulate treasure detection after delay
        mainHandler.postDelayed(() -> {
            Toast.makeText(this, "🏴‍☠️ Treasure detected nearby!", Toast.LENGTH_SHORT).show();
            if (treasureImageView != null) {
                treasureImageView.setVisibility(View.VISIBLE);
            }
        }, 1500);
    }
    
    private boolean handleARPlacement(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return false;
        }

        try {
            // Perform hit test
            List<HitResult> hitResults = arEngine.hitTest(event.getX(), event.getY());

            if (!hitResults.isEmpty()) {
                HitResult hit = hitResults.get(0);

                // Evaluate placement
                ARPlacementManager.PlacementCandidate candidate =
                        placementManager.evaluatePlacement(hit, treasureId);

                if (candidate != null && candidate.quality != ARPlacementManager.PlacementQuality.UNSUITABLE) {
                    // Confirm placement
                    String anchorId = placementManager.confirmPlacement(candidate.candidateId);

                    if (anchorId != null) {
                        currentAnchorId = anchorId;
                        placeTreasureAtAnchor(anchorId);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling AR placement", e);
        }

        return false;
    }

    private void placeTreasureAtAnchor(String anchorId) {
        try {
            // Get anchor from AR engine
            var anchors = arEngine.getActiveAnchors();
            var anchor = anchors.get(anchorId);

            if (anchor != null) {
                // Render treasure at anchor
                treasureRenderer.renderTreasure(treasureId, anchor, treasureRarity, true)
                        .thenAccept(treasureNode -> {
                            treasurePlaced = true;
                            updateStatusText("Treasure placed! Approach to capture.");

                            // Start capture sequence
                            Vector3 treasurePosition = treasureNode.getWorldPosition();
                            captureManager.startCapture(treasureId, treasureRarity, treasurePosition);
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Failed to render treasure", throwable);
                            showError("Failed to place treasure");
                            return null;
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error placing treasure at anchor", e);
            showError("Failed to place treasure");
        }
    }

    private void updateARFrame() {
        if (arEngine == null) return;

        try {
            Frame frame = arEngine.updateSession();
            if (frame != null) {
                // Update environmental conditions
                LightEstimate lightEstimate = frame.getLightEstimate();
                ARCoreEngine.EnvironmentalCondition condition = getCurrentEnvironmentalCondition();
                placementManager.updateEnvironmentalConditions(lightEstimate, condition);

                // Process frame for object recognition if in capture mode
                if (treasurePlaced && captureManager != null) {
                    // Convert frame to bitmap for processing (simplified)
                    processFrameForObjectRecognition(frame);
                }
            }
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available", e);
            showError("Camera not available for AR");
        }
    }

    private void processFrameForObjectRecognition(Frame frame) {
        // In a real implementation, this would convert the AR frame to a format
        // suitable for ML Kit processing
        // For now, we'll simulate object recognition
        mainHandler.postDelayed(() -> {
            if (Math.random() < 0.1) { // 10% chance per frame
                simulateObjectDetection();
            }
        }, 100);
    }

    private void simulateObjectDetection() {
        // Simulate finding a treasure clue
        ObjectRecognitionManager.TreasureClue clue = new ObjectRecognitionManager.TreasureClue(
                ObjectRecognitionManager.TreasureClueType.ANCIENT_TEXT,
                "Mysterious inscription",
                0.8f,
                null,
                System.currentTimeMillis()
        );

        // Create a mock recognition session
        ObjectRecognitionManager.RecognitionSession session =
                new ObjectRecognitionManager.RecognitionSession(System.currentTimeMillis());
        session.treasureClues.add(clue);

        // Process the result
        captureManager.processRecognitionResult(session);
    }

    private ARCoreEngine.EnvironmentalCondition getCurrentEnvironmentalCondition() {
        // Simplified environmental condition assessment
        // In a real implementation, this would analyze the current AR frame
        return ARCoreEngine.EnvironmentalCondition.GOOD;
    }

    // ARCoreEngine.AREngineListener implementation
    @Override
    public void onARInitialized() {
        runOnUiThread(() -> {
            arInitialized = true;
            updateStatusText("AR ready - Tap to place treasure");

            // Start AR frame updates
            startARFrameUpdates();
        });
    }

    @Override
    public void onARReady() {
        runOnUiThread(() -> updateStatusText("AR tracking active"));
    }

    @Override
    public void onARPaused() {
        runOnUiThread(() -> updateStatusText("AR paused"));
    }

    @Override
    public void onARResumed() {
        runOnUiThread(() -> updateStatusText("AR resumed"));
    }

    @Override
    public void onARError(String error) {
        runOnUiThread(() -> {
            updateStatusText("AR Error: " + error);
            showError(error);
        });
    }

    @Override
    public void onEnvironmentalConditionChanged(ARCoreEngine.EnvironmentalCondition condition) {
        runOnUiThread(() -> {
            String message = getEnvironmentalMessage(condition);
            if (message != null) {
                updateStatusText(message);
            }
        });
    }

    // ARCoreEngine.ARTrackingListener implementation
    @Override
    public void onPlaneDetected(com.google.ar.core.Plane plane) {
        runOnUiThread(() -> {
            if (!treasurePlaced) {
                updateStatusText("Surface detected - Tap to place treasure");
            }
        });
    }

    @Override
    public void onPlaneUpdated(com.google.ar.core.Plane plane) {
        // Plane updated - no UI action needed
    }

    @Override
    public void onPlaneLost(com.google.ar.core.Plane plane) {
        // Plane lost - no immediate UI action needed
    }

    @Override
    public void onAnchorCreated(String anchorId, com.google.ar.core.Anchor anchor) {
        Log.d(TAG, "Anchor created: " + anchorId);
    }

    @Override
    public void onAnchorUpdated(String anchorId, com.google.ar.core.Anchor anchor) {
        // Anchor updated - no immediate action needed
    }

    @Override
    public void onAnchorLost(String anchorId) {
        runOnUiThread(() -> {
            if (anchorId.equals(currentAnchorId)) {
                showError("Treasure anchor lost - please restart");
            }
        });
    }

    @Override
    public void onTrackingStateChanged(com.google.ar.core.TrackingState state) {
        runOnUiThread(() -> {
            switch (state) {
                case TRACKING:
                    updateStatusText("AR tracking active");
                    break;
                case PAUSED:
                    updateStatusText("AR tracking paused - move device");
                    break;
                case STOPPED:
                    updateStatusText("AR tracking stopped");
                    break;
            }
        });
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
        
        pulseY = ObjectAnimator.ofFloat(treasureImageView, "scaleY", 1.0f, 1.3f, 1.0f);
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
                        mainHandler.postDelayed(() -> {
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
        if (pulseY != null) {
            pulseY.cancel();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        Timber.d("Enhanced ARActivity destroyed");
    }

    // Utility methods
    private void updateStatusText(String text) {
        if (arStatusText != null) {
            arStatusText.setText(text);
        }
    }

    private void updateCaptureInstruction(String instruction) {
        if (captureInstructionText != null) {
            captureInstructionText.setText(instruction);
        }
    }

    private void showCaptureUI(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (captureInstructionText != null) captureInstructionText.setVisibility(visibility);
        if (captureProgressBar != null) captureProgressBar.setVisibility(visibility);
    }

    private void showObjectDetectionIndicator(boolean show) {
        if (objectDetectionIndicator != null) {
            objectDetectionIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showGestureHint(ARCaptureManager.CaptureGesture gesture) {
        // Show visual hint for the required gesture
        Toast.makeText(this, "Perform: " + gesture.getDescription(), Toast.LENGTH_LONG).show();
    }

    private void showCaptureSuccess(ARCaptureManager.CaptureResult result) {
        String message = String.format("🎉 Treasure Captured!\nScore: %.0f\nTime: %.1fs",
                result.score, result.captureTime / 1000f);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        arTitle.setText("🎉 Mission Complete!");
    }

    private void showError(String error) {
        Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
    }

    private void setTreasureImageForRarity() {
        if (treasureImageView == null) return;

        int imageResource;
        switch (treasureRarity) {
            case UNCOMMON:
                imageResource = R.drawable.treasure_star;
                break;
            case RARE:
                imageResource = R.drawable.treasure_star;
                break;
            case EPIC:
                imageResource = R.drawable.treasure_star;
                break;
            case LEGENDARY:
                imageResource = R.drawable.treasure_star;
                break;
            case COMMON:
            default:
                imageResource = R.drawable.jollyroger;
                break;
        }

        treasureImageView.setImageResource(imageResource);
        treasureImageView.setVisibility(View.GONE); // Hidden initially
    }

    private void captureTreasureFallback() {
        // Fallback capture for non-AR devices
        Timber.d("Capturing treasure with fallback method");

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
                        mainHandler.postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 2000);
                    });
            });
    }

    private void startARFrameUpdates() {
        // Start regular AR frame updates
        Runnable frameUpdater = new Runnable() {
            @Override
            public void run() {
                if (arInitialized && !isFinishing()) {
                    updateARFrame();
                    mainHandler.postDelayed(this, 33); // ~30 FPS
                }
            }
        };
        mainHandler.post(frameUpdater);
    }

    private String getEnvironmentalMessage(ARCoreEngine.EnvironmentalCondition condition) {
        switch (condition) {
            case INSUFFICIENT_LIGHT:
                return "Need more light for AR tracking";
            case EXCESSIVE_MOTION:
                return "Hold device steady";
            case INSUFFICIENT_FEATURES:
                return "Point at textured surfaces";
            case POOR:
                return "Poor AR conditions";
            case FAIR:
                return "Fair AR conditions";
            case GOOD:
                return "Good AR conditions";
            case EXCELLENT:
                return "Excellent AR conditions";
            default:
                return null;
        }
    }

    private void playSuccessSound() {
        // Play success sound effect
        // Implementation would play actual sound
    }

    private void playDiscoverySound() {
        // Play discovery sound effect
        // Implementation would play actual sound
    }

    private void playVictorySound() {
        // Play victory sound effect
        // Implementation would play actual sound
    }

    private boolean isArCoreSupported() {
        try {
            ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
            return availability.isSupported();
        } catch (Throwable t) {
            Timber.w(t, "ARCore availability check failed");
            return false;
        }
    }

    // Remaining interface implementations (simplified for brevity)
    @Override public void onPlacementCandidateFound(ARPlacementManager.PlacementCandidate candidate) {}
    @Override public void onPlacementQualityChanged(String candidateId, ARPlacementManager.PlacementQuality quality) {}
    @Override public void onPlacementStabilized(String candidateId, com.google.ar.core.Anchor anchor) {}
    @Override public void onPlacementFailed(String candidateId, String reason) {}
    @Override public void onTreasureRendered(String treasureId, AR3DTreasureRenderer.TreasureNode node) {}
    @Override public void onTreasureAnimationComplete(String treasureId, AR3DTreasureRenderer.AnimationType animationType) {}
    @Override public void onTreasureInteraction(String treasureId, AR3DTreasureRenderer.InteractionType interactionType) {}
    @Override public void onRenderingError(String treasureId, String error) {}

    public void onBackPressed(android.view.View view) {
        finish();
    }
}
