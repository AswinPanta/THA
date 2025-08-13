package com.treasurehuntadventure.tha.ar;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive ARCore engine for managing AR session, plane detection, and environmental tracking
 */
public class ARCoreEngine {
    private static final String TAG = "ARCoreEngine";
    
    // AR session state
    public enum ARState {
        UNINITIALIZED,
        INITIALIZING,
        READY,
        PAUSED,
        ERROR
    }
    
    // Environmental conditions
    public enum EnvironmentalCondition {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        INSUFFICIENT_LIGHT,
        EXCESSIVE_MOTION,
        INSUFFICIENT_FEATURES
    }
    
    private final Activity activity;
    private Session arSession;
    private ARState currentState = ARState.UNINITIALIZED;
    private boolean installRequested = false;
    
    // Tracking and detection
    private final Map<String, Anchor> activeAnchors = new ConcurrentHashMap<>();
    private final List<Plane> detectedPlanes = new ArrayList<>();
    private final AREnvironmentalAnalyzer environmentalAnalyzer;
    private final ARPlaneTracker planeTracker;
    
    // Callbacks
    private AREngineListener engineListener;
    private ARTrackingListener trackingListener;
    
    // Performance metrics
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float averageFPS = 0f;
    private final Map<String, Long> performanceMetrics = new HashMap<>();
    
    public interface AREngineListener {
        void onARInitialized();
        void onARReady();
        void onARPaused();
        void onARResumed();
        void onARError(String error);
        void onEnvironmentalConditionChanged(EnvironmentalCondition condition);
    }
    
    public interface ARTrackingListener {
        void onPlaneDetected(Plane plane);
        void onPlaneUpdated(Plane plane);
        void onPlaneLost(Plane plane);
        void onAnchorCreated(String anchorId, Anchor anchor);
        void onAnchorUpdated(String anchorId, Anchor anchor);
        void onAnchorLost(String anchorId);
        void onTrackingStateChanged(TrackingState state);
    }
    
    public ARCoreEngine(Activity activity) {
        this.activity = activity;
        this.environmentalAnalyzer = new AREnvironmentalAnalyzer();
        this.planeTracker = new ARPlaneTracker();
    }
    
    /**
     * Initializes ARCore session with optimal configuration
     */
    public void initialize(AREngineListener listener) {
        this.engineListener = listener;
        currentState = ARState.INITIALIZING;
        
        Log.d(TAG, "Initializing ARCore engine");
        
        try {
            // Check ARCore availability
            ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(activity);
            if (availability.isTransient()) {
                // Continue to check availability
                new android.os.Handler().postDelayed(() -> initialize(listener), 200);
                return;
            }
            
            if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                handleARCoreInstallation();
                return;
            }
            
            // Create AR session
            arSession = new Session(activity);
            
            // Configure session
            configureSession();
            
            currentState = ARState.READY;
            if (engineListener != null) {
                engineListener.onARInitialized();
            }
            
            Log.d(TAG, "ARCore engine initialized successfully");
            
        } catch (UnavailableArcoreNotInstalledException e) {
            Log.e(TAG, "ARCore not installed", e);
            handleError("ARCore not installed. Please install ARCore to use AR features.");
        } catch (UnavailableDeviceNotCompatibleException e) {
            Log.e(TAG, "Device not compatible with ARCore", e);
            handleError("Your device is not compatible with ARCore.");
        } catch (UnavailableSdkTooOldException e) {
            Log.e(TAG, "ARCore SDK too old", e);
            handleError("Please update the app to use AR features.");
        } catch (UnavailableApkTooOldException e) {
            Log.e(TAG, "ARCore APK too old", e);
            handleError("Please update ARCore to use AR features.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ARCore", e);
            handleError("Failed to initialize AR: " + e.getMessage());
        }
    }
    
    /**
     * Resumes AR session
     */
    public void resume() {
        if (arSession == null) return;
        
        try {
            arSession.resume();
            currentState = ARState.READY;
            
            if (engineListener != null) {
                engineListener.onARResumed();
            }
            
            Log.d(TAG, "AR session resumed");
            
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available", e);
            handleError("Camera not available for AR");
        }
    }
    
    /**
     * Pauses AR session
     */
    public void pause() {
        if (arSession != null) {
            arSession.pause();
            currentState = ARState.PAUSED;
            
            if (engineListener != null) {
                engineListener.onARPaused();
            }
            
            Log.d(TAG, "AR session paused");
        }
    }
    
    /**
     * Closes AR session and cleans up resources
     */
    public void close() {
        if (arSession != null) {
            arSession.close();
            arSession = null;
        }
        
        activeAnchors.clear();
        detectedPlanes.clear();
        currentState = ARState.UNINITIALIZED;
        
        Log.d(TAG, "AR session closed");
    }
    
    /**
     * Updates AR session and processes frame
     */
    public Frame updateSession() throws CameraNotAvailableException {
        if (arSession == null || currentState != ARState.READY) {
            return null;
        }
        
        Frame frame = arSession.update();
        
        // Update performance metrics
        updatePerformanceMetrics();
        
        // Analyze environmental conditions
        EnvironmentalCondition condition = environmentalAnalyzer.analyzeFrame(frame);
        if (engineListener != null) {
            engineListener.onEnvironmentalConditionChanged(condition);
        }
        
        // Update plane tracking
        updatePlaneTracking(frame);
        
        // Update anchor tracking
        updateAnchorTracking();
        
        return frame;
    }
    
    /**
     * Creates an anchor at the specified pose
     */
    public String createAnchor(Pose pose, String anchorId) {
        if (arSession == null) return null;
        
        try {
            Anchor anchor = arSession.createAnchor(pose);
            String id = anchorId != null ? anchorId : generateAnchorId();
            
            activeAnchors.put(id, anchor);
            
            if (trackingListener != null) {
                trackingListener.onAnchorCreated(id, anchor);
            }
            
            Log.d(TAG, "Anchor created: " + id);
            return id;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create anchor", e);
            return null;
        }
    }
    
    /**
     * Creates an anchor from a hit result
     */
    public String createAnchorFromHit(HitResult hit, String anchorId) {
        if (hit.getTrackable() instanceof Plane && 
            ((Plane) hit.getTrackable()).isPoseInPolygon(hit.getHitPose())) {
            
            Anchor anchor = hit.createAnchor();
            String id = anchorId != null ? anchorId : generateAnchorId();
            
            activeAnchors.put(id, anchor);
            
            if (trackingListener != null) {
                trackingListener.onAnchorCreated(id, anchor);
            }
            
            Log.d(TAG, "Anchor created from hit: " + id);
            return id;
        }
        
        return null;
    }
    
    /**
     * Removes an anchor
     */
    public void removeAnchor(String anchorId) {
        Anchor anchor = activeAnchors.remove(anchorId);
        if (anchor != null) {
            anchor.detach();
            
            if (trackingListener != null) {
                trackingListener.onAnchorLost(anchorId);
            }
            
            Log.d(TAG, "Anchor removed: " + anchorId);
        }
    }
    
    /**
     * Gets all active anchors
     */
    public Map<String, Anchor> getActiveAnchors() {
        return new HashMap<>(activeAnchors);
    }
    
    /**
     * Gets detected planes
     */
    public List<Plane> getDetectedPlanes() {
        return new ArrayList<>(detectedPlanes);
    }
    
    /**
     * Performs hit test at screen coordinates
     */
    public List<HitResult> hitTest(float x, float y) {
        if (arSession == null) return new ArrayList<>();
        
        try {
            Frame frame = arSession.update();
            return frame.hitTest(x, y);
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available for hit test", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets current light estimation
     */
    public LightEstimate getLightEstimate() {
        if (arSession == null) return null;
        
        try {
            Frame frame = arSession.update();
            return frame.getLightEstimate();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available for light estimation", e);
            return null;
        }
    }
    
    /**
     * Gets current tracking state
     */
    public TrackingState getTrackingState() {
        if (arSession == null) return TrackingState.STOPPED;
        
        try {
            Frame frame = arSession.update();
            return frame.getCamera().getTrackingState();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available for tracking state", e);
            return TrackingState.STOPPED;
        }
    }
    
    /**
     * Sets tracking listener
     */
    public void setTrackingListener(ARTrackingListener listener) {
        this.trackingListener = listener;
    }
    
    /**
     * Gets current AR state
     */
    public ARState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("fps", averageFPS);
        metrics.put("frameCount", frameCount);
        metrics.put("activeAnchors", activeAnchors.size());
        metrics.put("detectedPlanes", detectedPlanes.size());
        metrics.putAll(performanceMetrics);
        return metrics;
    }
    
    private void configureSession() throws Exception {
        Config config = new Config(arSession);
        
        // Enable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
        
        // Enable light estimation
        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        
        // Enable depth if supported
        if (arSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
        
        // Enable instant placement if supported
        if (arSession.isInstantPlacementModeSupported(Config.InstantPlacementMode.LOCAL_Y_UP)) {
            config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
        }
        
        arSession.configure(config);
    }
    
    private void handleARCoreInstallation() {
        try {
            ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(activity, !installRequested);
            
            switch (installStatus) {
                case INSTALL_REQUESTED:
                    installRequested = true;
                    break;
                case INSTALLED:
                    initialize(engineListener);
                    break;
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            Log.e(TAG, "User declined ARCore installation", e);
            handleError("ARCore installation is required for AR features.");
        } catch (Exception e) {
            Log.e(TAG, "ARCore installation failed", e);
            handleError("Failed to install ARCore: " + e.getMessage());
        }
    }
    
    private void updatePlaneTracking(Frame frame) {
        Collection<Plane> updatedPlanes = frame.getUpdatedTrackables(Plane.class);
        
        for (Plane plane : updatedPlanes) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                if (!detectedPlanes.contains(plane)) {
                    detectedPlanes.add(plane);
                    if (trackingListener != null) {
                        trackingListener.onPlaneDetected(plane);
                    }
                } else {
                    if (trackingListener != null) {
                        trackingListener.onPlaneUpdated(plane);
                    }
                }
            } else if (plane.getTrackingState() == TrackingState.STOPPED) {
                if (detectedPlanes.remove(plane)) {
                    if (trackingListener != null) {
                        trackingListener.onPlaneLost(plane);
                    }
                }
            }
        }
    }
    
    private void updateAnchorTracking() {
        List<String> lostAnchors = new ArrayList<>();
        
        for (Map.Entry<String, Anchor> entry : activeAnchors.entrySet()) {
            Anchor anchor = entry.getValue();
            
            if (anchor.getTrackingState() == TrackingState.STOPPED) {
                lostAnchors.add(entry.getKey());
            } else if (trackingListener != null) {
                trackingListener.onAnchorUpdated(entry.getKey(), anchor);
            }
        }
        
        // Remove lost anchors
        for (String anchorId : lostAnchors) {
            removeAnchor(anchorId);
        }
    }
    
    private void updatePerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        frameCount++;
        
        if (lastFrameTime > 0) {
            long frameDelta = currentTime - lastFrameTime;
            if (frameDelta > 0) {
                float currentFPS = 1000f / frameDelta;
                averageFPS = (averageFPS * 0.9f) + (currentFPS * 0.1f);
            }
        }
        
        lastFrameTime = currentTime;
        
        // Update performance metrics every second
        if (frameCount % 60 == 0) {
            performanceMetrics.put("lastUpdateTime", currentTime);
            performanceMetrics.put("memoryUsage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        }
    }
    
    private void handleError(String error) {
        currentState = ARState.ERROR;
        if (engineListener != null) {
            engineListener.onARError(error);
        }
        Log.e(TAG, "AR Error: " + error);
    }
    
    private String generateAnchorId() {
        return "anchor_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Environmental analyzer for AR conditions
     */
    private static class AREnvironmentalAnalyzer {
        private static final float MIN_LIGHT_INTENSITY = 0.1f;
        private static final float MAX_LIGHT_INTENSITY = 2.0f;
        private static final int MIN_FEATURE_POINTS = 100;
        
        public EnvironmentalCondition analyzeFrame(Frame frame) {
            // Analyze lighting conditions
            LightEstimate lightEstimate = frame.getLightEstimate();
            float lightIntensity = lightEstimate.getPixelIntensity();
            
            if (lightIntensity < MIN_LIGHT_INTENSITY) {
                return EnvironmentalCondition.INSUFFICIENT_LIGHT;
            }
            
            if (lightIntensity > MAX_LIGHT_INTENSITY) {
                return EnvironmentalCondition.EXCESSIVE_MOTION;
            }
            
            // Analyze feature points
            PointCloud pointCloud = frame.acquirePointCloud();
            int featurePoints = pointCloud.getPoints().remaining() / 4; // 4 floats per point
            pointCloud.release();
            
            if (featurePoints < MIN_FEATURE_POINTS) {
                return EnvironmentalCondition.INSUFFICIENT_FEATURES;
            }
            
            // Analyze tracking quality
            TrackingState trackingState = frame.getCamera().getTrackingState();
            if (trackingState != TrackingState.TRACKING) {
                return EnvironmentalCondition.POOR;
            }
            
            // Overall assessment
            if (lightIntensity > 0.8f && featurePoints > 300) {
                return EnvironmentalCondition.EXCELLENT;
            } else if (lightIntensity > 0.5f && featurePoints > 200) {
                return EnvironmentalCondition.GOOD;
            } else {
                return EnvironmentalCondition.FAIR;
            }
        }
    }
    
    /**
     * Plane tracker for managing detected planes
     */
    private static class ARPlaneTracker {
        private final Map<Plane, Long> planeDetectionTimes = new HashMap<>();
        
        public void trackPlane(Plane plane) {
            if (!planeDetectionTimes.containsKey(plane)) {
                planeDetectionTimes.put(plane, System.currentTimeMillis());
            }
        }
        
        public void untrackPlane(Plane plane) {
            planeDetectionTimes.remove(plane);
        }
        
        public long getPlaneAge(Plane plane) {
            Long detectionTime = planeDetectionTimes.get(plane);
            return detectionTime != null ? System.currentTimeMillis() - detectionTime : 0;
        }
    }
}
