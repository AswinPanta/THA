package com.treasurehuntadventure.tha.ar;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Vector3;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interactive AR capture system with gesture recognition and multi-step capture sequences
 */
public class ARCaptureManager {
    private static final String TAG = "ARCaptureManager";
    
    // Capture constants
    private static final float CAPTURE_DISTANCE_THRESHOLD = 1.0f; // 1 meter
    private static final long CAPTURE_SEQUENCE_TIMEOUT = 10000; // 10 seconds
    private static final long GESTURE_RECOGNITION_TIMEOUT = 3000; // 3 seconds
    private static final int MIN_GESTURE_POINTS = 5;
    
    // Capture sequence steps
    public enum CaptureStep {
        APPROACH("Approach the treasure"),
        DETECT_CLUE("Find the treasure clue"),
        PERFORM_GESTURE("Perform the capture gesture"),
        CONFIRM_CAPTURE("Confirm treasure capture"),
        COMPLETE("Capture complete!");
        
        private final String description;
        
        CaptureStep(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    // Gesture types for different rarities
    public enum CaptureGesture {
        TAP("Simple tap", TreasureRarity.COMMON),
        DOUBLE_TAP("Double tap", TreasureRarity.UNCOMMON),
        CIRCLE("Draw a circle", TreasureRarity.RARE),
        STAR("Draw a star", TreasureRarity.EPIC),
        COMPLEX_PATTERN("Draw the treasure symbol", TreasureRarity.LEGENDARY);
        
        private final String description;
        private final TreasureRarity requiredRarity;
        
        CaptureGesture(String description, TreasureRarity requiredRarity) {
            this.description = description;
            this.requiredRarity = requiredRarity;
        }
        
        public String getDescription() { return description; }
        public TreasureRarity getRequiredRarity() { return requiredRarity; }
    }
    
    private final Context context;
    private final ARCoreEngine arEngine;
    private final ObjectRecognitionManager objectRecognition;
    private final AR3DTreasureRenderer treasureRenderer;
    private final GestureDetector gestureDetector;
    private final Vibrator vibrator;
    private final Handler mainHandler;
    
    // Capture state
    private final Map<String, CaptureSession> activeSessions = new ConcurrentHashMap<>();
    private final GestureRecognizer gestureRecognizer;
    private CaptureListener captureListener;
    
    // Performance tracking
    private final Map<String, Long> captureMetrics = new HashMap<>();
    
    public interface CaptureListener {
        void onCaptureStarted(String treasureId, CaptureSession session);
        void onCaptureStepChanged(String treasureId, CaptureStep step, String instruction);
        void onGestureRequired(String treasureId, CaptureGesture gesture);
        void onGestureRecognized(String treasureId, CaptureGesture gesture, float confidence);
        void onClueDetected(String treasureId, ObjectRecognitionManager.TreasureClue clue);
        void onCaptureProgress(String treasureId, float progress);
        void onCaptureComplete(String treasureId, CaptureResult result);
        void onCaptureFailed(String treasureId, String reason);
        void onCaptureTimeout(String treasureId);
    }
    
    public ARCaptureManager(Context context, ARCoreEngine arEngine, 
                          ObjectRecognitionManager objectRecognition, 
                          AR3DTreasureRenderer treasureRenderer) {
        this.context = context;
        this.arEngine = arEngine;
        this.objectRecognition = objectRecognition;
        this.treasureRenderer = treasureRenderer;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        this.gestureRecognizer = new GestureRecognizer();
        this.gestureDetector = new GestureDetector(context, new CaptureGestureListener());
    }
    
    /**
     * Starts capture sequence for a treasure
     */
    public boolean startCapture(String treasureId, TreasureRarity rarity, Vector3 treasurePosition) {
        if (activeSessions.containsKey(treasureId)) {
            Log.w(TAG, "Capture already in progress for treasure: " + treasureId);
            return false;
        }
        
        CaptureSession session = new CaptureSession(treasureId, rarity, treasurePosition);
        activeSessions.put(treasureId, session);
        
        // Start with approach step
        session.currentStep = CaptureStep.APPROACH;
        
        if (captureListener != null) {
            captureListener.onCaptureStarted(treasureId, session);
            captureListener.onCaptureStepChanged(treasureId, CaptureStep.APPROACH, 
                    "Move closer to the treasure to begin capture");
        }
        
        // Start timeout timer
        mainHandler.postDelayed(() -> {
            if (activeSessions.containsKey(treasureId)) {
                timeoutCapture(treasureId);
            }
        }, CAPTURE_SEQUENCE_TIMEOUT);
        
        Log.d(TAG, "Capture started for treasure: " + treasureId + " (" + rarity + ")");
        return true;
    }
    
    /**
     * Updates player position for proximity checking
     */
    public void updatePlayerPosition(Vector3 playerPosition) {
        for (CaptureSession session : activeSessions.values()) {
            float distance = Vector3.subtract(session.treasurePosition, playerPosition).length();
            session.distanceToTreasure = distance;
            
            updateCaptureProgress(session, playerPosition);
        }
    }
    
    /**
     * Processes touch events for gesture recognition
     */
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        
        // Pass to gesture detector
        if (gestureDetector.onTouchEvent(event)) {
            handled = true;
        }
        
        // Process for active capture sessions
        for (CaptureSession session : activeSessions.values()) {
            if (session.currentStep == CaptureStep.PERFORM_GESTURE) {
                if (gestureRecognizer.processTouch(event, session)) {
                    handled = true;
                }
            }
        }
        
        return handled;
    }
    
    /**
     * Processes object recognition results for clue detection
     */
    public void processRecognitionResult(ObjectRecognitionManager.RecognitionSession recognitionSession) {
        for (ObjectRecognitionManager.TreasureClue clue : recognitionSession.treasureClues) {
            // Find sessions waiting for clues
            for (CaptureSession session : activeSessions.values()) {
                if (session.currentStep == CaptureStep.DETECT_CLUE) {
                    session.detectedClues.put(clue.type, clue);
                    
                    if (captureListener != null) {
                        captureListener.onClueDetected(session.treasureId, clue);
                    }
                    
                    // Check if we have enough clues to proceed
                    if (hasRequiredClues(session)) {
                        advanceToNextStep(session);
                    }
                }
            }
        }
    }
    
    /**
     * Cancels capture session
     */
    public void cancelCapture(String treasureId) {
        CaptureSession session = activeSessions.remove(treasureId);
        if (session != null) {
            if (captureListener != null) {
                captureListener.onCaptureFailed(treasureId, "Capture cancelled");
            }
            Log.d(TAG, "Capture cancelled for treasure: " + treasureId);
        }
    }
    
    /**
     * Gets active capture session
     */
    public CaptureSession getCaptureSession(String treasureId) {
        return activeSessions.get(treasureId);
    }
    
    /**
     * Sets capture listener
     */
    public void setCaptureListener(CaptureListener listener) {
        this.captureListener = listener;
    }
    
    /**
     * Gets capture performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeSessions", activeSessions.size());
        metrics.putAll(captureMetrics);
        return metrics;
    }
    
    private void updateCaptureProgress(CaptureSession session, Vector3 playerPosition) {
        switch (session.currentStep) {
            case APPROACH:
                if (session.distanceToTreasure <= CAPTURE_DISTANCE_THRESHOLD) {
                    advanceToNextStep(session);
                }
                break;
                
            case DETECT_CLUE:
                // Progress based on clues found
                float clueProgress = (float) session.detectedClues.size() / getRequiredClueCount(session.rarity);
                updateProgress(session, Math.min(0.9f, clueProgress));
                break;
                
            case PERFORM_GESTURE:
                // Progress based on gesture completion
                updateProgress(session, session.gestureProgress);
                break;
                
            case CONFIRM_CAPTURE:
                // Waiting for confirmation
                break;
        }
    }
    
    private void advanceToNextStep(CaptureSession session) {
        switch (session.currentStep) {
            case APPROACH:
                session.currentStep = CaptureStep.DETECT_CLUE;
                if (captureListener != null) {
                    captureListener.onCaptureStepChanged(session.treasureId, CaptureStep.DETECT_CLUE,
                            "Look around for treasure clues using your camera");
                }
                break;
                
            case DETECT_CLUE:
                session.currentStep = CaptureStep.PERFORM_GESTURE;
                CaptureGesture requiredGesture = getRequiredGesture(session.rarity);
                session.requiredGesture = requiredGesture;
                
                if (captureListener != null) {
                    captureListener.onCaptureStepChanged(session.treasureId, CaptureStep.PERFORM_GESTURE,
                            "Perform the capture gesture: " + requiredGesture.getDescription());
                    captureListener.onGestureRequired(session.treasureId, requiredGesture);
                }
                break;
                
            case PERFORM_GESTURE:
                session.currentStep = CaptureStep.CONFIRM_CAPTURE;
                if (captureListener != null) {
                    captureListener.onCaptureStepChanged(session.treasureId, CaptureStep.CONFIRM_CAPTURE,
                            "Tap to confirm treasure capture");
                }
                break;
                
            case CONFIRM_CAPTURE:
                completeCapture(session);
                break;
        }
    }
    
    private void completeCapture(CaptureSession session) {
        session.currentStep = CaptureStep.COMPLETE;
        session.endTime = System.currentTimeMillis();
        
        // Calculate capture result
        CaptureResult result = new CaptureResult(
                session.treasureId,
                session.rarity,
                session.endTime - session.startTime,
                session.detectedClues.size(),
                session.gestureAccuracy,
                calculateCaptureScore(session)
        );
        
        // Remove from active sessions
        activeSessions.remove(session.treasureId);
        
        // Trigger haptic feedback
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        
        // Update metrics
        captureMetrics.put("totalCaptures", captureMetrics.getOrDefault("totalCaptures", 0L) + 1);
        captureMetrics.put("averageCaptureTime", 
                (captureMetrics.getOrDefault("averageCaptureTime", 0L) + result.captureTime) / 2);
        
        if (captureListener != null) {
            captureListener.onCaptureComplete(session.treasureId, result);
        }
        
        Log.d(TAG, "Capture completed for treasure: " + session.treasureId + 
                " in " + result.captureTime + "ms");
    }
    
    private void timeoutCapture(String treasureId) {
        CaptureSession session = activeSessions.remove(treasureId);
        if (session != null) {
            if (captureListener != null) {
                captureListener.onCaptureTimeout(treasureId);
            }
            Log.d(TAG, "Capture timed out for treasure: " + treasureId);
        }
    }
    
    private void updateProgress(CaptureSession session, float progress) {
        session.progress = progress;
        if (captureListener != null) {
            captureListener.onCaptureProgress(session.treasureId, progress);
        }
    }
    
    private boolean hasRequiredClues(CaptureSession session) {
        return session.detectedClues.size() >= getRequiredClueCount(session.rarity);
    }
    
    private int getRequiredClueCount(TreasureRarity rarity) {
        switch (rarity) {
            case COMMON: return 1;
            case UNCOMMON: return 1;
            case RARE: return 2;
            case EPIC: return 2;
            case LEGENDARY: return 3;
            default: return 1;
        }
    }
    
    private CaptureGesture getRequiredGesture(TreasureRarity rarity) {
        for (CaptureGesture gesture : CaptureGesture.values()) {
            if (gesture.getRequiredRarity() == rarity) {
                return gesture;
            }
        }
        return CaptureGesture.TAP; // Fallback
    }
    
    private float calculateCaptureScore(CaptureSession session) {
        float score = 100f;
        
        // Time bonus (faster = better)
        long captureTime = session.endTime - session.startTime;
        if (captureTime < 30000) { // Under 30 seconds
            score += 50f;
        } else if (captureTime < 60000) { // Under 1 minute
            score += 25f;
        }
        
        // Clue bonus
        score += session.detectedClues.size() * 10f;
        
        // Gesture accuracy bonus
        score += session.gestureAccuracy * 50f;
        
        // Rarity multiplier
        switch (session.rarity) {
            case UNCOMMON: score *= 1.2f; break;
            case RARE: score *= 1.5f; break;
            case EPIC: score *= 2.0f; break;
            case LEGENDARY: score *= 3.0f; break;
        }
        
        return score;
    }
    
    /**
     * Capture session data
     */
    public static class CaptureSession {
        public final String treasureId;
        public final TreasureRarity rarity;
        public final Vector3 treasurePosition;
        public final long startTime;
        public long endTime;
        
        public CaptureStep currentStep = CaptureStep.APPROACH;
        public float distanceToTreasure = Float.MAX_VALUE;
        public float progress = 0f;
        
        public final Map<ObjectRecognitionManager.TreasureClueType, ObjectRecognitionManager.TreasureClue> detectedClues = new HashMap<>();
        public CaptureGesture requiredGesture;
        public float gestureProgress = 0f;
        public float gestureAccuracy = 0f;
        
        public CaptureSession(String treasureId, TreasureRarity rarity, Vector3 treasurePosition) {
            this.treasureId = treasureId;
            this.rarity = rarity;
            this.treasurePosition = treasurePosition;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Capture result data
     */
    public static class CaptureResult {
        public final String treasureId;
        public final TreasureRarity rarity;
        public final long captureTime;
        public final int cluesFound;
        public final float gestureAccuracy;
        public final float score;
        
        public CaptureResult(String treasureId, TreasureRarity rarity, long captureTime, 
                           int cluesFound, float gestureAccuracy, float score) {
            this.treasureId = treasureId;
            this.rarity = rarity;
            this.captureTime = captureTime;
            this.cluesFound = cluesFound;
            this.gestureAccuracy = gestureAccuracy;
            this.score = score;
        }
    }
    
    /**
     * Gesture listener for basic touch events
     */
    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // Handle tap gestures for capture confirmation
            for (CaptureSession session : activeSessions.values()) {
                if (session.currentStep == CaptureStep.CONFIRM_CAPTURE ||
                    (session.currentStep == CaptureStep.PERFORM_GESTURE && 
                     session.requiredGesture == CaptureGesture.TAP)) {
                    
                    if (session.requiredGesture == CaptureGesture.TAP) {
                        session.gestureAccuracy = 1.0f;
                        if (captureListener != null) {
                            captureListener.onGestureRecognized(session.treasureId, 
                                    CaptureGesture.TAP, 1.0f);
                        }
                        advanceToNextStep(session);
                    } else if (session.currentStep == CaptureStep.CONFIRM_CAPTURE) {
                        completeCapture(session);
                    }
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Handle double tap gestures
            for (CaptureSession session : activeSessions.values()) {
                if (session.currentStep == CaptureStep.PERFORM_GESTURE && 
                    session.requiredGesture == CaptureGesture.DOUBLE_TAP) {
                    
                    session.gestureAccuracy = 1.0f;
                    if (captureListener != null) {
                        captureListener.onGestureRecognized(session.treasureId, 
                                CaptureGesture.DOUBLE_TAP, 1.0f);
                    }
                    advanceToNextStep(session);
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Advanced gesture recognizer for complex patterns
     */
    private static class GestureRecognizer {
        // Implementation would include pattern recognition algorithms
        // for circles, stars, and complex patterns
        
        public boolean processTouch(MotionEvent event, CaptureSession session) {
            // Simplified implementation - would include actual gesture recognition
            if (session.requiredGesture == CaptureGesture.CIRCLE ||
                session.requiredGesture == CaptureGesture.STAR ||
                session.requiredGesture == CaptureGesture.COMPLEX_PATTERN) {
                
                // Simulate gesture progress
                session.gestureProgress += 0.1f;
                
                if (session.gestureProgress >= 1.0f) {
                    session.gestureAccuracy = 0.8f; // Simulated accuracy
                    return true;
                }
            }
            return false;
        }
    }
}
