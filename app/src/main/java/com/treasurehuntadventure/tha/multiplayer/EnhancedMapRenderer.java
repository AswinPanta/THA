package com.treasurehuntadventure.tha.multiplayer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;

import androidx.core.content.ContextCompat;

import com.treasurehuntadventure.tha.R;
import com.treasurehuntadventure.tha.TreasureRarity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced map renderer with advanced visualizations for multiplayer treasure hunting
 */
public class EnhancedMapRenderer {
    private static final String TAG = "EnhancedMapRenderer";
    
    // Animation constants
    private static final long TREASURE_SPAWN_ANIMATION_DURATION = 1000;
    private static final long TREASURE_PULSE_DURATION = 2000;
    private static final long PLAYER_MOVEMENT_ANIMATION_DURATION = 500;
    private static final long CAPTURE_ANIMATION_DURATION = 1500;
    
    // Visual constants
    private static final float TREASURE_SCALE_FACTOR = 1.2f;
    private static final float PLAYER_SCALE_FACTOR = 1.0f;
    private static final int PROXIMITY_RING_ALPHA = 80;
    private static final int DIRECTION_ARROW_ALPHA = 150;
    
    private final Context context;
    private final MapView mapView;
    private final Map<String, EnhancedTreasureMarker> treasureMarkers;
    private final Map<String, EnhancedPlayerMarker> playerMarkers;
    private final Map<TreasureRarity, TreasureVisualConfig> rarityConfigs;
    private final ProximityRingOverlay proximityOverlay;
    private final DirectionArrowOverlay directionOverlay;
    
    public EnhancedMapRenderer(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.treasureMarkers = new ConcurrentHashMap<>();
        this.playerMarkers = new ConcurrentHashMap<>();
        this.rarityConfigs = new HashMap<>();
        this.proximityOverlay = new ProximityRingOverlay();
        this.directionOverlay = new DirectionArrowOverlay();
        
        initializeRarityConfigs();
        setupMapOverlays();
    }
    
    /**
     * Adds or updates a treasure marker with enhanced visuals
     */
    public void addTreasureMarker(String treasureId, GeoPoint location, TreasureRarity rarity, 
                                String title, boolean animate) {
        EnhancedTreasureMarker marker = treasureMarkers.get(treasureId);
        
        if (marker == null) {
            marker = createTreasureMarker(treasureId, location, rarity, title);
            treasureMarkers.put(treasureId, marker);
            mapView.getOverlays().add(marker);
            
            if (animate) {
                animateTreasureSpawn(marker);
            }
        } else {
            // Update existing marker
            marker.setPosition(location);
            marker.setTitle(title);
        }
        
        mapView.invalidate();
    }
    
    /**
     * Removes a treasure marker with animation
     */
    public void removeTreasureMarker(String treasureId, boolean animate) {
        EnhancedTreasureMarker marker = treasureMarkers.remove(treasureId);
        if (marker != null) {
            if (animate) {
                animateTreasureCapture(marker, () -> {
                    mapView.getOverlays().remove(marker);
                    mapView.invalidate();
                });
            } else {
                mapView.getOverlays().remove(marker);
                mapView.invalidate();
            }
        }
    }
    
    /**
     * Adds or updates a player marker
     */
    public void addPlayerMarker(String playerId, GeoPoint location, String displayName, boolean animate) {
        EnhancedPlayerMarker marker = playerMarkers.get(playerId);
        
        if (marker == null) {
            marker = createPlayerMarker(playerId, location, displayName);
            playerMarkers.put(playerId, marker);
            mapView.getOverlays().add(marker);
        } else {
            GeoPoint oldPosition = marker.getPosition();
            marker.setPosition(location);
            
            if (animate && !oldPosition.equals(location)) {
                animatePlayerMovement(marker, oldPosition, location);
            }
        }
        
        mapView.invalidate();
    }
    
    /**
     * Removes a player marker
     */
    public void removePlayerMarker(String playerId) {
        EnhancedPlayerMarker marker = playerMarkers.remove(playerId);
        if (marker != null) {
            mapView.getOverlays().remove(marker);
            mapView.invalidate();
        }
    }
    
    /**
     * Updates treasure proximity visualization
     */
    public void updateTreasureProximity(String treasureId, ProximityDetectionSystem.ProximityZone zone, 
                                      float distance) {
        EnhancedTreasureMarker marker = treasureMarkers.get(treasureId);
        if (marker != null) {
            marker.setProximityZone(zone);
            marker.setDistance(distance);
            
            // Update visual effects based on proximity
            switch (zone) {
                case CAPTURE:
                    startTreasurePulseAnimation(marker);
                    break;
                case INTERACTION:
                    marker.setGlowEffect(true);
                    break;
                case AWARENESS:
                    marker.setHighlighted(true);
                    break;
                default:
                    marker.setGlowEffect(false);
                    marker.setHighlighted(false);
                    stopTreasurePulseAnimation(marker);
                    break;
            }
        }
    }
    
    /**
     * Shows direction arrow to distant treasures
     */
    public void showDirectionArrow(GeoPoint playerLocation, GeoPoint treasureLocation, 
                                 TreasureRarity rarity, float distance) {
        directionOverlay.addArrow(playerLocation, treasureLocation, rarity, distance);
        mapView.invalidate();
    }
    
    /**
     * Hides direction arrow
     */
    public void hideDirectionArrow() {
        directionOverlay.clearArrows();
        mapView.invalidate();
    }
    
    /**
     * Shows proximity rings around player
     */
    public void showProximityRings(GeoPoint playerLocation, boolean show) {
        proximityOverlay.setPlayerLocation(playerLocation);
        proximityOverlay.setVisible(show);
        mapView.invalidate();
    }
    
    /**
     * Triggers treasure capture animation
     */
    public void animateTreasureCapture(String treasureId, Runnable onComplete) {
        EnhancedTreasureMarker marker = treasureMarkers.get(treasureId);
        if (marker != null) {
            animateTreasureCapture(marker, onComplete);
        }
    }
    
    private void initializeRarityConfigs() {
        rarityConfigs.put(TreasureRarity.COMMON, new TreasureVisualConfig(
                R.drawable.treasure_common, Color.parseColor("#9E9E9E"), 1.0f, false));
        rarityConfigs.put(TreasureRarity.UNCOMMON, new TreasureVisualConfig(
                R.drawable.treasure_uncommon, Color.parseColor("#4CAF50"), 1.1f, false));
        rarityConfigs.put(TreasureRarity.RARE, new TreasureVisualConfig(
                R.drawable.treasure_rare, Color.parseColor("#2196F3"), 1.2f, true));
        rarityConfigs.put(TreasureRarity.EPIC, new TreasureVisualConfig(
                R.drawable.treasure_epic, Color.parseColor("#9C27B0"), 1.3f, true));
        rarityConfigs.put(TreasureRarity.LEGENDARY, new TreasureVisualConfig(
                R.drawable.treasure_legendary, Color.parseColor("#FF9800"), 1.5f, true));
    }
    
    private void setupMapOverlays() {
        // Add proximity overlay
        mapView.getOverlays().add(proximityOverlay);
        
        // Add direction arrow overlay
        mapView.getOverlays().add(directionOverlay);
    }
    
    private EnhancedTreasureMarker createTreasureMarker(String treasureId, GeoPoint location, 
                                                      TreasureRarity rarity, String title) {
        EnhancedTreasureMarker marker = new EnhancedTreasureMarker(mapView, treasureId, rarity);
        marker.setPosition(location);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        TreasureVisualConfig config = rarityConfigs.get(rarity);
        if (config != null) {
            Drawable icon = ContextCompat.getDrawable(context, config.iconResource);
            marker.setIcon(icon);
            marker.setRarityColor(config.color);
            marker.setScale(config.scale);
            marker.setHasGlow(config.hasGlow);
        }
        
        return marker;
    }
    
    private EnhancedPlayerMarker createPlayerMarker(String playerId, GeoPoint location, String displayName) {
        EnhancedPlayerMarker marker = new EnhancedPlayerMarker(mapView, playerId);
        marker.setPosition(location);
        marker.setTitle(displayName);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.player_icon);
        marker.setIcon(icon);
        marker.setScale(PLAYER_SCALE_FACTOR);
        
        return marker;
    }
    
    private void animateTreasureSpawn(EnhancedTreasureMarker marker) {
        // Scale animation from 0 to full size
        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(marker, "scale", 0f, marker.getScale());
        scaleAnimator.setDuration(TREASURE_SPAWN_ANIMATION_DURATION);
        scaleAnimator.setInterpolator(new BounceInterpolator());
        
        // Alpha animation
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(marker, "alpha", 0f, 1f);
        alphaAnimator.setDuration(TREASURE_SPAWN_ANIMATION_DURATION / 2);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleAnimator, alphaAnimator);
        animatorSet.start();
    }
    
    private void animateTreasureCapture(EnhancedTreasureMarker marker, Runnable onComplete) {
        // Scale up then fade out
        ObjectAnimator scaleUpAnimator = ObjectAnimator.ofFloat(marker, "scale", 
                marker.getScale(), marker.getScale() * 1.5f);
        scaleUpAnimator.setDuration(CAPTURE_ANIMATION_DURATION / 3);
        
        ObjectAnimator scaleDownAnimator = ObjectAnimator.ofFloat(marker, "scale", 
                marker.getScale() * 1.5f, 0f);
        scaleDownAnimator.setDuration(CAPTURE_ANIMATION_DURATION * 2 / 3);
        
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(marker, "alpha", 1f, 0f);
        alphaAnimator.setDuration(CAPTURE_ANIMATION_DURATION / 2);
        alphaAnimator.setStartDelay(CAPTURE_ANIMATION_DURATION / 2);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleUpAnimator).before(scaleDownAnimator);
        animatorSet.play(alphaAnimator).with(scaleDownAnimator);
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        animatorSet.start();
    }
    
    private void animatePlayerMovement(EnhancedPlayerMarker marker, GeoPoint from, GeoPoint to) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(PLAYER_MOVEMENT_ANIMATION_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            double lat = from.getLatitude() + (to.getLatitude() - from.getLatitude()) * fraction;
            double lon = from.getLongitude() + (to.getLongitude() - from.getLongitude()) * fraction;
            marker.setPosition(new GeoPoint(lat, lon));
            mapView.invalidate();
        });
        
        animator.start();
    }
    
    private void startTreasurePulseAnimation(EnhancedTreasureMarker marker) {
        if (marker.getPulseAnimator() != null) {
            return; // Already pulsing
        }
        
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(marker, "scale", 
                marker.getBaseScale(), marker.getBaseScale() * 1.2f);
        pulseAnimator.setDuration(TREASURE_PULSE_DURATION);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        marker.setPulseAnimator(pulseAnimator);
        pulseAnimator.start();
    }
    
    private void stopTreasurePulseAnimation(EnhancedTreasureMarker marker) {
        ObjectAnimator pulseAnimator = marker.getPulseAnimator();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            marker.setPulseAnimator(null);
            marker.setScale(marker.getBaseScale());
        }
    }
    
    /**
     * Configuration for treasure visual appearance
     */
    private static class TreasureVisualConfig {
        final int iconResource;
        final int color;
        final float scale;
        final boolean hasGlow;
        
        TreasureVisualConfig(int iconResource, int color, float scale, boolean hasGlow) {
            this.iconResource = iconResource;
            this.color = color;
            this.scale = scale;
            this.hasGlow = hasGlow;
        }
    }
    
    /**
     * Enhanced treasure marker with additional visual features
     */
    private static class EnhancedTreasureMarker extends Marker {
        private final String treasureId;
        private final TreasureRarity rarity;
        private ProximityDetectionSystem.ProximityZone proximityZone = ProximityDetectionSystem.ProximityZone.NONE;
        private float distance = Float.MAX_VALUE;
        private int rarityColor = Color.WHITE;
        private float baseScale = 1.0f;
        private boolean hasGlow = false;
        private boolean glowEffect = false;
        private boolean highlighted = false;
        private ObjectAnimator pulseAnimator;
        
        public EnhancedTreasureMarker(MapView mapView, String treasureId, TreasureRarity rarity) {
            super(mapView);
            this.treasureId = treasureId;
            this.rarity = rarity;
        }
        
        public void setProximityZone(ProximityDetectionSystem.ProximityZone zone) {
            this.proximityZone = zone;
        }
        
        public void setDistance(float distance) {
            this.distance = distance;
        }
        
        public void setRarityColor(int color) {
            this.rarityColor = color;
        }
        
        public void setScale(float scale) {
            this.baseScale = scale;
            // Apply scale through transformation if needed
        }
        
        public float getBaseScale() {
            return baseScale;
        }
        
        public void setHasGlow(boolean hasGlow) {
            this.hasGlow = hasGlow;
        }
        
        public void setGlowEffect(boolean glowEffect) {
            this.glowEffect = glowEffect;
        }
        
        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }
        
        public ObjectAnimator getPulseAnimator() {
            return pulseAnimator;
        }
        
        public void setPulseAnimator(ObjectAnimator animator) {
            this.pulseAnimator = animator;
        }
        
        public String getTreasureId() {
            return treasureId;
        }
        
        public TreasureRarity getRarity() {
            return rarity;
        }
    }
    
    /**
     * Enhanced player marker
     */
    private static class EnhancedPlayerMarker extends Marker {
        private final String playerId;
        private float scale = 1.0f;
        
        public EnhancedPlayerMarker(MapView mapView, String playerId) {
            super(mapView);
            this.playerId = playerId;
        }
        
        public void setScale(float scale) {
            this.scale = scale;
        }
        
        public float getScale() {
            return scale;
        }
        
        public String getPlayerId() {
            return playerId;
        }
    }
    
    /**
     * Overlay for drawing proximity rings
     */
    private class ProximityRingOverlay extends Overlay {
        private GeoPoint playerLocation;
        private boolean visible = false;
        private final Paint ringPaint;
        
        public ProximityRingOverlay() {
            ringPaint = new Paint();
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(3f);
            ringPaint.setAlpha(PROXIMITY_RING_ALPHA);
        }
        
        public void setPlayerLocation(GeoPoint location) {
            this.playerLocation = location;
        }
        
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
        
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (!visible || playerLocation == null || shadow) {
                return;
            }
            
            // Draw proximity rings for different zones
            drawProximityRing(canvas, mapView, playerLocation, 50, Color.GREEN);    // Capture
            drawProximityRing(canvas, mapView, playerLocation, 100, Color.YELLOW);  // Interaction
            drawProximityRing(canvas, mapView, playerLocation, 300, Color.BLUE);    // Awareness
        }
        
        private void drawProximityRing(Canvas canvas, MapView mapView, GeoPoint center, 
                                     float radiusMeters, int color) {
            ringPaint.setColor(color);
            
            // Convert meters to pixels (approximate)
            float radiusPixels = radiusMeters / (float) mapView.getProjection().getMetersPerPixel();
            
            android.graphics.Point centerPoint = mapView.getProjection().toPixels(center, null);
            canvas.drawCircle(centerPoint.x, centerPoint.y, radiusPixels, ringPaint);
        }
    }
    
    /**
     * Overlay for drawing direction arrows to distant treasures
     */
    private class DirectionArrowOverlay extends Overlay {
        private final Map<String, DirectionArrow> arrows = new HashMap<>();
        private final Paint arrowPaint;
        private final Path arrowPath;
        
        public DirectionArrowOverlay() {
            arrowPaint = new Paint();
            arrowPaint.setStyle(Paint.Style.FILL);
            arrowPaint.setAlpha(DIRECTION_ARROW_ALPHA);
            arrowPath = new Path();
        }
        
        public void addArrow(GeoPoint from, GeoPoint to, TreasureRarity rarity, float distance) {
            String key = to.getLatitude() + "," + to.getLongitude();
            arrows.put(key, new DirectionArrow(from, to, rarity, distance));
        }
        
        public void clearArrows() {
            arrows.clear();
        }
        
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow || arrows.isEmpty()) {
                return;
            }
            
            for (DirectionArrow arrow : arrows.values()) {
                drawDirectionArrow(canvas, mapView, arrow);
            }
        }
        
        private void drawDirectionArrow(Canvas canvas, MapView mapView, DirectionArrow arrow) {
            TreasureVisualConfig config = rarityConfigs.get(arrow.rarity);
            if (config != null) {
                arrowPaint.setColor(config.color);
            }
            
            // Calculate arrow position and rotation
            android.graphics.Point fromPoint = mapView.getProjection().toPixels(arrow.from, null);
            android.graphics.Point toPoint = mapView.getProjection().toPixels(arrow.to, null);
            
            float angle = (float) Math.atan2(toPoint.y - fromPoint.y, toPoint.x - fromPoint.x);
            
            // Draw arrow at edge of screen pointing toward treasure
            int screenWidth = canvas.getWidth();
            int screenHeight = canvas.getHeight();
            int margin = 50;
            
            float arrowX, arrowY;
            if (toPoint.x < 0) {
                arrowX = margin;
            } else if (toPoint.x > screenWidth) {
                arrowX = screenWidth - margin;
            } else {
                arrowX = toPoint.x;
            }
            
            if (toPoint.y < 0) {
                arrowY = margin;
            } else if (toPoint.y > screenHeight) {
                arrowY = screenHeight - margin;
            } else {
                arrowY = toPoint.y;
            }
            
            // Draw arrow shape
            canvas.save();
            canvas.translate(arrowX, arrowY);
            canvas.rotate((float) Math.toDegrees(angle));
            
            arrowPath.reset();
            arrowPath.moveTo(0, 0);
            arrowPath.lineTo(-20, -10);
            arrowPath.lineTo(-15, 0);
            arrowPath.lineTo(-20, 10);
            arrowPath.close();
            
            canvas.drawPath(arrowPath, arrowPaint);
            canvas.restore();
        }
        
        private class DirectionArrow {
            final GeoPoint from;
            final GeoPoint to;
            final TreasureRarity rarity;
            final float distance;
            
            DirectionArrow(GeoPoint from, GeoPoint to, TreasureRarity rarity, float distance) {
                this.from = from;
                this.to = to;
                this.rarity = rarity;
                this.distance = distance;
            }
        }
    }
}
