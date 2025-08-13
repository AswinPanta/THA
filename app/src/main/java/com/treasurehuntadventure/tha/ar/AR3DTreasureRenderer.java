package com.treasurehuntadventure.tha.ar;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color as SceneformColor;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.treasurehuntadventure.tha.TreasureRarity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.github.sceneview.ar.ArSceneView;
import io.github.sceneview.ar.node.ArModelNode;

/**
 * Advanced 3D treasure renderer with animations and visual effects for AR
 */
public class AR3DTreasureRenderer {
    private static final String TAG = "AR3DTreasureRenderer";
    
    // Animation constants
    private static final long SPAWN_ANIMATION_DURATION = 2000;
    private static final long IDLE_ANIMATION_DURATION = 3000;
    private static final long CAPTURE_ANIMATION_DURATION = 1500;
    private static final long GLOW_ANIMATION_DURATION = 2000;
    
    // Scale factors for different rarities
    private static final Map<TreasureRarity, Float> RARITY_SCALES = new HashMap<>();
    private static final Map<TreasureRarity, SceneformColor> RARITY_COLORS = new HashMap<>();
    private static final Map<TreasureRarity, String> RARITY_MODELS = new HashMap<>();
    
    static {
        // Scale factors
        RARITY_SCALES.put(TreasureRarity.COMMON, 0.8f);
        RARITY_SCALES.put(TreasureRarity.UNCOMMON, 1.0f);
        RARITY_SCALES.put(TreasureRarity.RARE, 1.2f);
        RARITY_SCALES.put(TreasureRarity.EPIC, 1.4f);
        RARITY_SCALES.put(TreasureRarity.LEGENDARY, 1.6f);
        
        // Colors for glow effects
        RARITY_COLORS.put(TreasureRarity.COMMON, new SceneformColor(0.7f, 0.7f, 0.7f, 1.0f));
        RARITY_COLORS.put(TreasureRarity.UNCOMMON, new SceneformColor(0.0f, 1.0f, 0.0f, 1.0f));
        RARITY_COLORS.put(TreasureRarity.RARE, new SceneformColor(0.0f, 0.5f, 1.0f, 1.0f));
        RARITY_COLORS.put(TreasureRarity.EPIC, new SceneformColor(0.8f, 0.0f, 1.0f, 1.0f));
        RARITY_COLORS.put(TreasureRarity.LEGENDARY, new SceneformColor(1.0f, 0.8f, 0.0f, 1.0f));
        
        // 3D model paths (these would be actual model files in assets)
        RARITY_MODELS.put(TreasureRarity.COMMON, "models/treasure_chest_common.glb");
        RARITY_MODELS.put(TreasureRarity.UNCOMMON, "models/treasure_chest_uncommon.glb");
        RARITY_MODELS.put(TreasureRarity.RARE, "models/treasure_chest_rare.glb");
        RARITY_MODELS.put(TreasureRarity.EPIC, "models/treasure_chest_epic.glb");
        RARITY_MODELS.put(TreasureRarity.LEGENDARY, "models/treasure_chest_legendary.glb");
    }
    
    private final Context context;
    private final ArSceneView arSceneView;
    private final Map<String, TreasureNode> activeTreasures = new ConcurrentHashMap<>();
    private final Map<TreasureRarity, ModelRenderable> treasureModels = new HashMap<>();
    private final Map<TreasureRarity, Material> glowMaterials = new HashMap<>();
    
    // Particle effects
    private final ParticleEffectManager particleManager;
    
    // Rendering callbacks
    private TreasureRenderListener renderListener;
    
    public interface TreasureRenderListener {
        void onTreasureRendered(String treasureId, TreasureNode node);
        void onTreasureAnimationComplete(String treasureId, AnimationType animationType);
        void onTreasureInteraction(String treasureId, InteractionType interactionType);
        void onRenderingError(String treasureId, String error);
    }
    
    public enum AnimationType {
        SPAWN, IDLE, CAPTURE, GLOW, PULSE, DISAPPEAR
    }
    
    public enum InteractionType {
        TOUCH, PROXIMITY_ENTER, PROXIMITY_EXIT, CAPTURE_START, CAPTURE_COMPLETE
    }
    
    public AR3DTreasureRenderer(Context context, ArSceneView arSceneView) {
        this.context = context;
        this.arSceneView = arSceneView;
        this.particleManager = new ParticleEffectManager(context);
        
        initializeModelsAndMaterials();
    }
    
    /**
     * Renders a treasure at the specified anchor
     */
    public CompletableFuture<TreasureNode> renderTreasure(String treasureId, Anchor anchor, 
                                                        TreasureRarity rarity, boolean animate) {
        CompletableFuture<TreasureNode> future = new CompletableFuture<>();
        
        try {
            // Create anchor node
            AnchorNode anchorNode = new AnchorNode(anchor);
            arSceneView.getScene().addChild(anchorNode);
            
            // Get or create treasure model
            ModelRenderable model = treasureModels.get(rarity);
            if (model == null) {
                // Create fallback procedural model
                createProceduralTreasureModel(rarity).thenAccept(proceduralModel -> {
                    TreasureNode treasureNode = createTreasureNode(treasureId, proceduralModel, rarity, anchorNode);
                    completeTreasureCreation(treasureId, treasureNode, animate, future);
                });
            } else {
                TreasureNode treasureNode = createTreasureNode(treasureId, model, rarity, anchorNode);
                completeTreasureCreation(treasureId, treasureNode, animate, future);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error rendering treasure: " + treasureId, e);
            future.completeExceptionally(e);
            if (renderListener != null) {
                renderListener.onRenderingError(treasureId, e.getMessage());
            }
        }
        
        return future;
    }
    
    /**
     * Updates treasure based on proximity
     */
    public void updateTreasureProximity(String treasureId, float distance, boolean inCaptureRange) {
        TreasureNode treasureNode = activeTreasures.get(treasureId);
        if (treasureNode == null) return;
        
        if (inCaptureRange) {
            startCaptureReadyAnimation(treasureNode);
            if (renderListener != null) {
                renderListener.onTreasureInteraction(treasureId, InteractionType.PROXIMITY_ENTER);
            }
        } else {
            stopCaptureReadyAnimation(treasureNode);
            if (renderListener != null) {
                renderListener.onTreasureInteraction(treasureId, InteractionType.PROXIMITY_EXIT);
            }
        }
        
        // Update glow intensity based on distance
        float glowIntensity = Math.max(0.2f, 1.0f - (distance / 100.0f)); // Max glow at 0m, min at 100m
        updateGlowIntensity(treasureNode, glowIntensity);
    }
    
    /**
     * Starts treasure capture animation
     */
    public void startCaptureAnimation(String treasureId, Runnable onComplete) {
        TreasureNode treasureNode = activeTreasures.get(treasureId);
        if (treasureNode == null) return;
        
        // Stop any existing animations
        stopAllAnimations(treasureNode);
        
        // Create capture animation sequence
        AnimatorSet captureSequence = new AnimatorSet();
        
        // Scale up animation
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(treasureNode, "scaleX", 
                treasureNode.getLocalScale().x, treasureNode.getLocalScale().x * 1.5f);
        scaleUp.setDuration(CAPTURE_ANIMATION_DURATION / 3);
        
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(treasureNode, "scaleY", 
                treasureNode.getLocalScale().y, treasureNode.getLocalScale().y * 1.5f);
        scaleUpY.setDuration(CAPTURE_ANIMATION_DURATION / 3);
        
        ObjectAnimator scaleUpZ = ObjectAnimator.ofFloat(treasureNode, "scaleZ", 
                treasureNode.getLocalScale().z, treasureNode.getLocalScale().z * 1.5f);
        scaleUpZ.setDuration(CAPTURE_ANIMATION_DURATION / 3);
        
        // Rotation animation
        ObjectAnimator rotate = ObjectAnimator.ofFloat(treasureNode, "rotationY", 0f, 360f);
        rotate.setDuration(CAPTURE_ANIMATION_DURATION);
        rotate.setRepeatCount(2);
        
        // Fade out animation
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(treasureNode, "alpha", 1.0f, 0.0f);
        fadeOut.setDuration(CAPTURE_ANIMATION_DURATION / 2);
        fadeOut.setStartDelay(CAPTURE_ANIMATION_DURATION / 2);
        
        captureSequence.playTogether(scaleUp, scaleUpY, scaleUpZ, rotate);
        captureSequence.play(fadeOut).after(CAPTURE_ANIMATION_DURATION / 2);
        
        captureSequence.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Trigger particle effects
                particleManager.createCaptureEffect(treasureNode.getWorldPosition(), treasureNode.rarity);
                
                // Remove treasure
                removeTreasure(treasureId);
                
                if (onComplete != null) {
                    onComplete.run();
                }
                
                if (renderListener != null) {
                    renderListener.onTreasureAnimationComplete(treasureId, AnimationType.CAPTURE);
                }
            }
        });
        
        captureSequence.start();
        treasureNode.currentAnimation = captureSequence;
        
        if (renderListener != null) {
            renderListener.onTreasureInteraction(treasureId, InteractionType.CAPTURE_START);
        }
    }
    
    /**
     * Removes treasure from scene
     */
    public void removeTreasure(String treasureId) {
        TreasureNode treasureNode = activeTreasures.remove(treasureId);
        if (treasureNode != null) {
            stopAllAnimations(treasureNode);
            
            if (treasureNode.anchorNode != null) {
                arSceneView.getScene().removeChild(treasureNode.anchorNode);
            }
            
            Log.d(TAG, "Treasure removed: " + treasureId);
        }
    }
    
    /**
     * Gets active treasure node
     */
    public TreasureNode getTreasureNode(String treasureId) {
        return activeTreasures.get(treasureId);
    }
    
    /**
     * Sets render listener
     */
    public void setRenderListener(TreasureRenderListener listener) {
        this.renderListener = listener;
    }
    
    /**
     * Cleans up resources
     */
    public void cleanup() {
        for (TreasureNode node : activeTreasures.values()) {
            stopAllAnimations(node);
        }
        activeTreasures.clear();
        particleManager.cleanup();
    }
    
    private void initializeModelsAndMaterials() {
        // Initialize glow materials for each rarity
        for (TreasureRarity rarity : TreasureRarity.values()) {
            SceneformColor color = RARITY_COLORS.get(rarity);
            MaterialFactory.makeOpaqueWithColor(context, color)
                    .thenAccept(material -> glowMaterials.put(rarity, material));
        }
        
        // Load 3D models (in a real implementation, these would be loaded from assets)
        // For now, we'll create procedural models
        for (TreasureRarity rarity : TreasureRarity.values()) {
            createProceduralTreasureModel(rarity)
                    .thenAccept(model -> treasureModels.put(rarity, model));
        }
    }
    
    private CompletableFuture<ModelRenderable> createProceduralTreasureModel(TreasureRarity rarity) {
        SceneformColor color = RARITY_COLORS.get(rarity);
        
        return MaterialFactory.makeOpaqueWithColor(context, color)
                .thenApply(material -> {
                    // Create a treasure chest shape (simplified as a cube for now)
                    return ShapeFactory.makeCube(
                            new Vector3(0.1f, 0.08f, 0.1f), // Size based on rarity
                            Vector3.zero(),
                            material
                    );
                });
    }
    
    private TreasureNode createTreasureNode(String treasureId, ModelRenderable model, 
                                          TreasureRarity rarity, AnchorNode anchorNode) {
        TreasureNode treasureNode = new TreasureNode(treasureId, rarity, anchorNode);
        treasureNode.setRenderable(model);
        
        // Set scale based on rarity
        Float scale = RARITY_SCALES.get(rarity);
        if (scale != null) {
            treasureNode.setLocalScale(new Vector3(scale, scale, scale));
        }
        
        // Add to anchor node
        anchorNode.addChild(treasureNode);
        
        return treasureNode;
    }
    
    private void completeTreasureCreation(String treasureId, TreasureNode treasureNode, 
                                        boolean animate, CompletableFuture<TreasureNode> future) {
        activeTreasures.put(treasureId, treasureNode);
        
        if (animate) {
            startSpawnAnimation(treasureNode);
        }
        
        // Start idle animation
        startIdleAnimation(treasureNode);
        
        future.complete(treasureNode);
        
        if (renderListener != null) {
            renderListener.onTreasureRendered(treasureId, treasureNode);
        }
        
        Log.d(TAG, "Treasure rendered: " + treasureId + " (" + treasureNode.rarity + ")");
    }
    
    private void startSpawnAnimation(TreasureNode treasureNode) {
        // Start with zero scale
        treasureNode.setLocalScale(Vector3.zero());
        
        // Animate to full scale with bounce
        Vector3 targetScale = new Vector3(
                RARITY_SCALES.get(treasureNode.rarity),
                RARITY_SCALES.get(treasureNode.rarity),
                RARITY_SCALES.get(treasureNode.rarity)
        );
        
        ObjectAnimator scaleAnimator = ObjectAnimator.ofObject(treasureNode, "localScale", 
                new Vector3Evaluator(), Vector3.zero(), targetScale);
        scaleAnimator.setDuration(SPAWN_ANIMATION_DURATION);
        scaleAnimator.setInterpolator(new BounceInterpolator());
        
        // Add particle effects
        particleManager.createSpawnEffect(treasureNode.getWorldPosition(), treasureNode.rarity);
        
        scaleAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (renderListener != null) {
                    renderListener.onTreasureAnimationComplete(treasureNode.treasureId, AnimationType.SPAWN);
                }
            }
        });
        
        scaleAnimator.start();
        treasureNode.currentAnimation = scaleAnimator;
    }
    
    private void startIdleAnimation(TreasureNode treasureNode) {
        // Gentle floating animation
        ObjectAnimator floatAnimator = ObjectAnimator.ofFloat(treasureNode, "localPositionY", 
                0f, 0.05f);
        floatAnimator.setDuration(IDLE_ANIMATION_DURATION);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Gentle rotation
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(treasureNode, "localRotationY", 
                0f, 360f);
        rotateAnimator.setDuration(IDLE_ANIMATION_DURATION * 3);
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        AnimatorSet idleSet = new AnimatorSet();
        idleSet.playTogether(floatAnimator, rotateAnimator);
        idleSet.start();
        
        treasureNode.idleAnimation = idleSet;
    }
    
    private void startCaptureReadyAnimation(TreasureNode treasureNode) {
        if (treasureNode.captureReadyAnimation != null) return; // Already running
        
        // Pulsing glow effect
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(treasureNode, "glowIntensity", 
                0.5f, 1.0f);
        pulseAnimator.setDuration(GLOW_ANIMATION_DURATION / 2);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        pulseAnimator.start();
        treasureNode.captureReadyAnimation = pulseAnimator;
    }
    
    private void stopCaptureReadyAnimation(TreasureNode treasureNode) {
        if (treasureNode.captureReadyAnimation != null) {
            treasureNode.captureReadyAnimation.cancel();
            treasureNode.captureReadyAnimation = null;
        }
    }
    
    private void updateGlowIntensity(TreasureNode treasureNode, float intensity) {
        // This would update the glow material properties
        // Implementation depends on the specific material system used
        treasureNode.glowIntensity = intensity;
    }
    
    private void stopAllAnimations(TreasureNode treasureNode) {
        if (treasureNode.currentAnimation != null) {
            treasureNode.currentAnimation.cancel();
            treasureNode.currentAnimation = null;
        }
        
        if (treasureNode.idleAnimation != null) {
            treasureNode.idleAnimation.cancel();
            treasureNode.idleAnimation = null;
        }
        
        if (treasureNode.captureReadyAnimation != null) {
            treasureNode.captureReadyAnimation.cancel();
            treasureNode.captureReadyAnimation = null;
        }
    }
    
    /**
     * Custom treasure node class
     */
    public static class TreasureNode extends Node {
        public final String treasureId;
        public final TreasureRarity rarity;
        public final AnchorNode anchorNode;
        
        public android.animation.Animator currentAnimation;
        public AnimatorSet idleAnimation;
        public ObjectAnimator captureReadyAnimation;
        public float glowIntensity = 0.5f;
        
        public TreasureNode(String treasureId, TreasureRarity rarity, AnchorNode anchorNode) {
            this.treasureId = treasureId;
            this.rarity = rarity;
            this.anchorNode = anchorNode;
        }
        
        public void setLocalPositionY(float y) {
            Vector3 currentPos = getLocalPosition();
            setLocalPosition(new Vector3(currentPos.x, y, currentPos.z));
        }
        
        public void setLocalRotationY(float degrees) {
            setLocalRotation(Quaternion.axisAngle(Vector3.up(), degrees));
        }
    }
    
    /**
     * Vector3 evaluator for animations
     */
    private static class Vector3Evaluator implements android.animation.TypeEvaluator<Vector3> {
        @Override
        public Vector3 evaluate(float fraction, Vector3 startValue, Vector3 endValue) {
            return Vector3.lerp(startValue, endValue, fraction);
        }
    }
    
    /**
     * Particle effect manager for treasure effects
     */
    private static class ParticleEffectManager {
        private final Context context;
        
        public ParticleEffectManager(Context context) {
            this.context = context;
        }
        
        public void createSpawnEffect(Vector3 position, TreasureRarity rarity) {
            // Create spawn particle effect
            Log.d(TAG, "Creating spawn effect for " + rarity + " at " + position);
            // Implementation would create actual particle systems
        }
        
        public void createCaptureEffect(Vector3 position, TreasureRarity rarity) {
            // Create capture particle effect
            Log.d(TAG, "Creating capture effect for " + rarity + " at " + position);
            // Implementation would create actual particle systems
        }
        
        public void cleanup() {
            // Clean up particle systems
        }
    }
}
