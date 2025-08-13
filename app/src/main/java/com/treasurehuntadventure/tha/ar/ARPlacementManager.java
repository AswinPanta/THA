package com.treasurehuntadventure.tha.ar;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced AR placement manager for stable treasure positioning with environmental adaptation
 */
public class ARPlacementManager {
    private static final String TAG = "ARPlacementManager";
    
    // Placement quality thresholds
    private static final float MIN_PLANE_SIZE = 0.5f; // 0.5m x 0.5m minimum
    private static final float MIN_LIGHT_INTENSITY = 0.3f;
    private static final float MAX_LIGHT_INTENSITY = 1.5f;
    private static final float MIN_TRACKING_CONFIDENCE = 0.7f;
    private static final float PLACEMENT_STABILITY_THRESHOLD = 0.02f; // 2cm movement threshold
    private static final long STABILITY_CHECK_DURATION = 2000; // 2 seconds
    
    // Surface type preferences
    private static final Map<Plane.Type, Float> SURFACE_TYPE_SCORES = new HashMap<>();
    
    static {
        SURFACE_TYPE_SCORES.put(Plane.Type.HORIZONTAL_UPWARD_FACING, 1.0f);
        SURFACE_TYPE_SCORES.put(Plane.Type.HORIZONTAL_DOWNWARD_FACING, 0.3f);
        SURFACE_TYPE_SCORES.put(Plane.Type.VERTICAL, 0.7f);
    }
    
    public enum PlacementQuality {
        EXCELLENT(0.9f, "Perfect conditions for AR placement"),
        GOOD(0.7f, "Good conditions for AR placement"),
        FAIR(0.5f, "Fair conditions - placement may be less stable"),
        POOR(0.3f, "Poor conditions - consider moving to better lighting"),
        UNSUITABLE(0.0f, "Unsuitable for AR placement");
        
        private final float score;
        private final String description;
        
        PlacementQuality(float score, String description) {
            this.score = score;
            this.description = description;
        }
        
        public float getScore() { return score; }
        public String getDescription() { return description; }
    }
    
    public enum SurfaceType {
        TABLE("Table Surface", 1.0f),
        FLOOR("Floor Surface", 0.9f),
        WALL("Wall Surface", 0.7f),
        OUTDOOR_GROUND("Outdoor Ground", 0.8f),
        UNEVEN_SURFACE("Uneven Surface", 0.5f),
        UNKNOWN("Unknown Surface", 0.3f);
        
        private final String displayName;
        private final float stabilityScore;
        
        SurfaceType(String displayName, float stabilityScore) {
            this.displayName = displayName;
            this.stabilityScore = stabilityScore;
        }
        
        public String getDisplayName() { return displayName; }
        public float getStabilityScore() { return stabilityScore; }
    }
    
    private final ARCoreEngine arEngine;
    private final Map<String, PlacementCandidate> placementCandidates = new ConcurrentHashMap<>();
    private final Map<String, StabilityTracker> stabilityTrackers = new ConcurrentHashMap<>();
    private PlacementListener placementListener;
    
    // Environmental adaptation
    private float currentLightIntensity = 1.0f;
    private ARCoreEngine.EnvironmentalCondition currentCondition = ARCoreEngine.EnvironmentalCondition.GOOD;
    
    public interface PlacementListener {
        void onPlacementCandidateFound(PlacementCandidate candidate);
        void onPlacementQualityChanged(String candidateId, PlacementQuality quality);
        void onPlacementStabilized(String candidateId, Anchor anchor);
        void onPlacementFailed(String candidateId, String reason);
        void onEnvironmentalConditionChanged(ARCoreEngine.EnvironmentalCondition condition);
    }
    
    public ARPlacementManager(ARCoreEngine arEngine) {
        this.arEngine = arEngine;
    }
    
    /**
     * Evaluates hit result for treasure placement suitability
     */
    public PlacementCandidate evaluatePlacement(HitResult hitResult, String treasureId) {
        if (!(hitResult.getTrackable() instanceof Plane)) {
            return null;
        }
        
        Plane plane = (Plane) hitResult.getTrackable();
        Pose hitPose = hitResult.getHitPose();
        
        // Calculate placement quality
        PlacementQuality quality = calculatePlacementQuality(plane, hitPose);
        
        // Determine surface type
        SurfaceType surfaceType = determineSurfaceType(plane);
        
        // Create placement candidate
        PlacementCandidate candidate = new PlacementCandidate(
                generateCandidateId(),
                treasureId,
                hitResult,
                plane,
                hitPose,
                quality,
                surfaceType,
                System.currentTimeMillis()
        );
        
        placementCandidates.put(candidate.candidateId, candidate);
        
        if (placementListener != null) {
            placementListener.onPlacementCandidateFound(candidate);
        }
        
        Log.d(TAG, "Placement candidate evaluated: " + quality + " on " + surfaceType);
        return candidate;
    }
    
    /**
     * Confirms placement and creates stable anchor
     */
    public String confirmPlacement(String candidateId) {
        PlacementCandidate candidate = placementCandidates.get(candidateId);
        if (candidate == null) {
            if (placementListener != null) {
                placementListener.onPlacementFailed(candidateId, "Candidate not found");
            }
            return null;
        }
        
        // Check if placement is still valid
        if (!isPlacementStillValid(candidate)) {
            if (placementListener != null) {
                placementListener.onPlacementFailed(candidateId, "Placement no longer valid");
            }
            return null;
        }
        
        // Create anchor with environmental adaptation
        Pose adaptedPose = adaptPoseForEnvironment(candidate.hitPose, candidate.surfaceType);
        String anchorId = arEngine.createAnchor(adaptedPose, "treasure_" + candidate.treasureId);
        
        if (anchorId != null) {
            // Start stability tracking
            StabilityTracker tracker = new StabilityTracker(anchorId, adaptedPose);
            stabilityTrackers.put(anchorId, tracker);
            
            // Clean up candidate
            placementCandidates.remove(candidateId);
            
            Log.d(TAG, "Placement confirmed with anchor: " + anchorId);
            return anchorId;
        } else {
            if (placementListener != null) {
                placementListener.onPlacementFailed(candidateId, "Failed to create anchor");
            }
            return null;
        }
    }
    
    /**
     * Updates environmental conditions and adapts placements
     */
    public void updateEnvironmentalConditions(LightEstimate lightEstimate, 
                                            ARCoreEngine.EnvironmentalCondition condition) {
        this.currentLightIntensity = lightEstimate.getPixelIntensity();
        this.currentCondition = condition;
        
        // Re-evaluate existing candidates
        for (PlacementCandidate candidate : placementCandidates.values()) {
            PlacementQuality newQuality = calculatePlacementQuality(candidate.plane, candidate.hitPose);
            if (newQuality != candidate.quality) {
                candidate.quality = newQuality;
                if (placementListener != null) {
                    placementListener.onPlacementQualityChanged(candidate.candidateId, newQuality);
                }
            }
        }
        
        // Update stability trackers
        updateStabilityTrackers();
        
        if (placementListener != null) {
            placementListener.onEnvironmentalConditionChanged(condition);
        }
    }
    
    /**
     * Gets best placement candidate for a treasure
     */
    public PlacementCandidate getBestPlacementCandidate(String treasureId) {
        PlacementCandidate best = null;
        float bestScore = 0f;
        
        for (PlacementCandidate candidate : placementCandidates.values()) {
            if (candidate.treasureId.equals(treasureId)) {
                float score = calculateCandidateScore(candidate);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        
        return best;
    }
    
    /**
     * Gets all placement candidates
     */
    public List<PlacementCandidate> getAllCandidates() {
        return new ArrayList<>(placementCandidates.values());
    }
    
    /**
     * Removes placement candidate
     */
    public void removePlacementCandidate(String candidateId) {
        placementCandidates.remove(candidateId);
    }
    
    /**
     * Sets placement listener
     */
    public void setPlacementListener(PlacementListener listener) {
        this.placementListener = listener;
    }
    
    /**
     * Gets placement recommendations based on current conditions
     */
    public List<String> getPlacementRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        switch (currentCondition) {
            case INSUFFICIENT_LIGHT:
                recommendations.add("Move to a brighter area for better AR tracking");
                recommendations.add("Turn on additional lights if indoors");
                break;
            case EXCESSIVE_MOTION:
                recommendations.add("Hold device steady while placing treasures");
                recommendations.add("Wait for movement to settle before placement");
                break;
            case INSUFFICIENT_FEATURES:
                recommendations.add("Point camera at textured surfaces");
                recommendations.add("Avoid blank walls or uniform surfaces");
                break;
            case POOR:
                recommendations.add("Look for flat, well-lit surfaces");
                recommendations.add("Ensure good lighting and minimal movement");
                break;
            case FAIR:
                recommendations.add("Current conditions are acceptable");
                recommendations.add("For best results, find a flat surface with good lighting");
                break;
            case GOOD:
            case EXCELLENT:
                recommendations.add("Excellent conditions for AR placement!");
                break;
        }
        
        return recommendations;
    }
    
    private PlacementQuality calculatePlacementQuality(Plane plane, Pose pose) {
        float score = 1.0f;
        
        // Check plane size
        float[] polygon = plane.getPolygon().array();
        float planeArea = calculatePolygonArea(polygon);
        if (planeArea < MIN_PLANE_SIZE) {
            score *= 0.5f;
        }
        
        // Check surface type
        Float surfaceScore = SURFACE_TYPE_SCORES.get(plane.getType());
        if (surfaceScore != null) {
            score *= surfaceScore;
        }
        
        // Check lighting conditions
        if (currentLightIntensity < MIN_LIGHT_INTENSITY) {
            score *= 0.3f;
        } else if (currentLightIntensity > MAX_LIGHT_INTENSITY) {
            score *= 0.7f;
        }
        
        // Check tracking state
        if (plane.getTrackingState() != TrackingState.TRACKING) {
            score *= 0.2f;
        }
        
        // Check environmental condition
        switch (currentCondition) {
            case EXCELLENT:
                score *= 1.0f;
                break;
            case GOOD:
                score *= 0.9f;
                break;
            case FAIR:
                score *= 0.7f;
                break;
            case POOR:
                score *= 0.5f;
                break;
            default:
                score *= 0.3f;
                break;
        }
        
        // Determine quality level
        if (score >= 0.9f) return PlacementQuality.EXCELLENT;
        if (score >= 0.7f) return PlacementQuality.GOOD;
        if (score >= 0.5f) return PlacementQuality.FAIR;
        if (score >= 0.3f) return PlacementQuality.POOR;
        return PlacementQuality.UNSUITABLE;
    }
    
    private SurfaceType determineSurfaceType(Plane plane) {
        switch (plane.getType()) {
            case HORIZONTAL_UPWARD_FACING:
                // Could be table or floor - use height to determine
                float[] center = plane.getCenterPose().getTranslation();
                float height = center[1]; // Y coordinate
                
                if (height > 0.5f && height < 1.2f) {
                    return SurfaceType.TABLE;
                } else if (height < 0.2f) {
                    return SurfaceType.FLOOR;
                } else {
                    return SurfaceType.OUTDOOR_GROUND;
                }
                
            case VERTICAL:
                return SurfaceType.WALL;
                
            case HORIZONTAL_DOWNWARD_FACING:
                return SurfaceType.UNEVEN_SURFACE;
                
            default:
                return SurfaceType.UNKNOWN;
        }
    }
    
    private Pose adaptPoseForEnvironment(Pose originalPose, SurfaceType surfaceType) {
        float[] translation = originalPose.getTranslation();
        float[] rotation = originalPose.getRotationQuaternion();
        
        // Adjust height based on surface type
        switch (surfaceType) {
            case TABLE:
                translation[1] += 0.05f; // Slightly above table surface
                break;
            case FLOOR:
                translation[1] += 0.02f; // Just above floor
                break;
            case WALL:
                // Move slightly away from wall
                translation[2] += 0.05f;
                break;
        }
        
        // Adjust for lighting conditions
        if (currentLightIntensity < MIN_LIGHT_INTENSITY) {
            // In low light, place treasures slightly higher for better visibility
            translation[1] += 0.03f;
        }
        
        return new Pose(translation, rotation);
    }
    
    private boolean isPlacementStillValid(PlacementCandidate candidate) {
        // Check if plane is still being tracked
        if (candidate.plane.getTrackingState() != TrackingState.TRACKING) {
            return false;
        }
        
        // Check if environmental conditions haven't degraded too much
        PlacementQuality currentQuality = calculatePlacementQuality(candidate.plane, candidate.hitPose);
        return currentQuality != PlacementQuality.UNSUITABLE;
    }
    
    private float calculateCandidateScore(PlacementCandidate candidate) {
        float score = candidate.quality.getScore();
        score *= candidate.surfaceType.getStabilityScore();
        
        // Prefer more recent candidates
        long age = System.currentTimeMillis() - candidate.timestamp;
        if (age < 5000) { // Less than 5 seconds old
            score *= 1.1f;
        }
        
        return score;
    }
    
    private void updateStabilityTrackers() {
        List<String> stableAnchors = new ArrayList<>();
        
        for (Map.Entry<String, StabilityTracker> entry : stabilityTrackers.entrySet()) {
            StabilityTracker tracker = entry.getValue();
            
            // Get current anchor pose
            Anchor anchor = arEngine.getActiveAnchors().get(entry.getKey());
            if (anchor != null && anchor.getTrackingState() == TrackingState.TRACKING) {
                Pose currentPose = anchor.getPose();
                
                if (tracker.updatePose(currentPose)) {
                    // Anchor is stable
                    stableAnchors.add(entry.getKey());
                    
                    if (placementListener != null) {
                        placementListener.onPlacementStabilized(entry.getKey(), anchor);
                    }
                }
            }
        }
        
        // Remove stable anchors from tracking
        for (String anchorId : stableAnchors) {
            stabilityTrackers.remove(anchorId);
        }
    }
    
    private float calculatePolygonArea(float[] polygon) {
        if (polygon.length < 6) return 0f; // Need at least 3 points (x,z pairs)
        
        float area = 0f;
        int n = polygon.length / 2;
        
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += polygon[i * 2] * polygon[j * 2 + 1];
            area -= polygon[j * 2] * polygon[i * 2 + 1];
        }
        
        return Math.abs(area) / 2f;
    }
    
    private String generateCandidateId() {
        return "candidate_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Placement candidate data class
     */
    public static class PlacementCandidate {
        public final String candidateId;
        public final String treasureId;
        public final HitResult hitResult;
        public final Plane plane;
        public final Pose hitPose;
        public PlacementQuality quality;
        public final SurfaceType surfaceType;
        public final long timestamp;
        
        public PlacementCandidate(String candidateId, String treasureId, HitResult hitResult, 
                                Plane plane, Pose hitPose, PlacementQuality quality, 
                                SurfaceType surfaceType, long timestamp) {
            this.candidateId = candidateId;
            this.treasureId = treasureId;
            this.hitResult = hitResult;
            this.plane = plane;
            this.hitPose = hitPose;
            this.quality = quality;
            this.surfaceType = surfaceType;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Stability tracker for anchors
     */
    private static class StabilityTracker {
        private final String anchorId;
        private final Pose initialPose;
        private final long startTime;
        private Pose lastPose;
        private int stableFrameCount = 0;
        
        public StabilityTracker(String anchorId, Pose initialPose) {
            this.anchorId = anchorId;
            this.initialPose = initialPose;
            this.lastPose = initialPose;
            this.startTime = System.currentTimeMillis();
        }
        
        public boolean updatePose(Pose currentPose) {
            float distance = calculateDistance(lastPose, currentPose);
            
            if (distance < PLACEMENT_STABILITY_THRESHOLD) {
                stableFrameCount++;
            } else {
                stableFrameCount = 0;
            }
            
            lastPose = currentPose;
            
            // Consider stable if it hasn't moved much for the required duration
            long elapsed = System.currentTimeMillis() - startTime;
            return elapsed > STABILITY_CHECK_DURATION && stableFrameCount > 30; // ~1 second at 30fps
        }
        
        private float calculateDistance(Pose pose1, Pose pose2) {
            float[] t1 = pose1.getTranslation();
            float[] t2 = pose2.getTranslation();
            
            float dx = t1[0] - t2[0];
            float dy = t1[1] - t2[1];
            float dz = t1[2] - t2[2];
            
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}
