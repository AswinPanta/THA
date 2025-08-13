package com.treasurehuntadventure.tha.ar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI-powered object recognition manager using ML Kit for treasure clue detection
 */
public class ObjectRecognitionManager {
    private static final String TAG = "ObjectRecognition";
    
    // Recognition confidence thresholds
    private static final float MIN_OBJECT_CONFIDENCE = 0.5f;
    private static final float MIN_LABEL_CONFIDENCE = 0.7f;
    private static final float MIN_TEXT_CONFIDENCE = 0.8f;
    
    // Treasure clue categories
    private static final Map<String, TreasureClueType> TREASURE_CLUE_OBJECTS = new HashMap<>();
    private static final Map<String, TreasureClueType> TREASURE_CLUE_LABELS = new HashMap<>();
    private static final String[] TREASURE_KEYWORDS = {
        "treasure", "gold", "coin", "chest", "map", "compass", "anchor", "ship", "pirate", "skull"
    };
    
    static {
        // Object-based clues
        TREASURE_CLUE_OBJECTS.put("Book", TreasureClueType.ANCIENT_TEXT);
        TREASURE_CLUE_OBJECTS.put("Bottle", TreasureClueType.MESSAGE_IN_BOTTLE);
        TREASURE_CLUE_OBJECTS.put("Clock", TreasureClueType.TIME_KEEPER);
        TREASURE_CLUE_OBJECTS.put("Plant", TreasureClueType.NATURE_MARKER);
        TREASURE_CLUE_OBJECTS.put("Stone", TreasureClueType.LANDMARK);
        TREASURE_CLUE_OBJECTS.put("Tree", TreasureClueType.NATURE_MARKER);
        TREASURE_CLUE_OBJECTS.put("Building", TreasureClueType.LANDMARK);
        TREASURE_CLUE_OBJECTS.put("Statue", TreasureClueType.MONUMENT);
        
        // Label-based clues
        TREASURE_CLUE_LABELS.put("Antique", TreasureClueType.ANCIENT_ARTIFACT);
        TREASURE_CLUE_LABELS.put("Metal", TreasureClueType.METALLIC_OBJECT);
        TREASURE_CLUE_LABELS.put("Wood", TreasureClueType.WOODEN_OBJECT);
        TREASURE_CLUE_LABELS.put("Paper", TreasureClueType.ANCIENT_TEXT);
        TREASURE_CLUE_LABELS.put("Stone", TreasureClueType.LANDMARK);
        TREASURE_CLUE_LABELS.put("Water", TreasureClueType.WATER_FEATURE);
    }
    
    public enum TreasureClueType {
        ANCIENT_TEXT("Ancient Text", 0.8f),
        MESSAGE_IN_BOTTLE("Message in Bottle", 0.9f),
        TIME_KEEPER("Time Keeper", 0.7f),
        NATURE_MARKER("Nature Marker", 0.6f),
        LANDMARK("Landmark", 0.8f),
        MONUMENT("Monument", 0.9f),
        ANCIENT_ARTIFACT("Ancient Artifact", 0.85f),
        METALLIC_OBJECT("Metallic Object", 0.7f),
        WOODEN_OBJECT("Wooden Object", 0.6f),
        WATER_FEATURE("Water Feature", 0.75f),
        TREASURE_SYMBOL("Treasure Symbol", 1.0f);
        
        private final String displayName;
        private final float treasureRelevance;
        
        TreasureClueType(String displayName, float treasureRelevance) {
            this.displayName = displayName;
            this.treasureRelevance = treasureRelevance;
        }
        
        public String getDisplayName() { return displayName; }
        public float getTreasureRelevance() { return treasureRelevance; }
    }
    
    private final Context context;
    private final ObjectDetector objectDetector;
    private final ImageLabeler imageLabeler;
    private final TextRecognizer textRecognizer;
    
    // Recognition state
    private final Map<String, RecognitionResult> recentRecognitions = new ConcurrentHashMap<>();
    private final Map<TreasureClueType, Integer> clueTypeCount = new ConcurrentHashMap<>();
    private ObjectRecognitionListener recognitionListener;
    
    // Performance tracking
    private long lastRecognitionTime = 0;
    private int recognitionCount = 0;
    private float averageRecognitionTime = 0f;
    
    public interface ObjectRecognitionListener {
        void onObjectDetected(DetectedObjectInfo objectInfo);
        void onTreasureClueFound(TreasureClue clue);
        void onTextDetected(DetectedTextInfo textInfo);
        void onRecognitionComplete(RecognitionSession session);
        void onRecognitionError(String error);
    }
    
    public ObjectRecognitionManager(Context context) {
        this.context = context;
        
        // Configure object detector
        ObjectDetectorOptions objectOptions = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .enableMultipleObjects()
                .build();
        this.objectDetector = ObjectDetection.getClient(objectOptions);
        
        // Configure image labeler
        ImageLabelerOptions labelOptions = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(MIN_LABEL_CONFIDENCE)
                .build();
        this.imageLabeler = ImageLabeling.getClient(labelOptions);
        
        // Configure text recognizer
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        
        Log.d(TAG, "Object recognition manager initialized");
    }
    
    /**
     * Processes camera frame for object recognition
     */
    public void processFrame(ImageProxy imageProxy, ObjectRecognitionListener listener) {
        this.recognitionListener = listener;
        
        long startTime = System.currentTimeMillis();
        
        try {
            @SuppressWarnings("UnsafeOptInUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                
                RecognitionSession session = new RecognitionSession(startTime);
                
                // Run all recognition tasks in parallel
                processObjectDetection(image, session);
                processImageLabeling(image, session);
                processTextRecognition(image, session);
                
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
            if (listener != null) {
                listener.onRecognitionError("Frame processing failed: " + e.getMessage());
            }
        } finally {
            imageProxy.close();
        }
    }
    
    /**
     * Processes bitmap for object recognition
     */
    public void processBitmap(Bitmap bitmap, ObjectRecognitionListener listener) {
        this.recognitionListener = listener;
        
        long startTime = System.currentTimeMillis();
        
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            RecognitionSession session = new RecognitionSession(startTime);
            
            // Run all recognition tasks
            processObjectDetection(image, session);
            processImageLabeling(image, session);
            processTextRecognition(image, session);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap", e);
            if (listener != null) {
                listener.onRecognitionError("Bitmap processing failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets recent recognition results
     */
    public Map<String, RecognitionResult> getRecentRecognitions() {
        return new HashMap<>(recentRecognitions);
    }
    
    /**
     * Gets clue type statistics
     */
    public Map<TreasureClueType, Integer> getClueTypeStatistics() {
        return new HashMap<>(clueTypeCount);
    }
    
    /**
     * Clears recognition history
     */
    public void clearRecognitionHistory() {
        recentRecognitions.clear();
        clueTypeCount.clear();
    }
    
    /**
     * Gets performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("recognitionCount", recognitionCount);
        metrics.put("averageRecognitionTime", averageRecognitionTime);
        metrics.put("recentRecognitions", recentRecognitions.size());
        metrics.put("lastRecognitionTime", lastRecognitionTime);
        return metrics;
    }
    
    private void processObjectDetection(InputImage image, RecognitionSession session) {
        objectDetector.process(image)
                .addOnSuccessListener(objects -> {
                    session.objectsDetected = objects.size();
                    
                    for (DetectedObject obj : objects) {
                        if (obj.getLabels().isEmpty()) continue;
                        
                        DetectedObject.Label topLabel = obj.getLabels().get(0);
                        if (topLabel.getConfidence() >= MIN_OBJECT_CONFIDENCE) {
                            
                            DetectedObjectInfo objectInfo = new DetectedObjectInfo(
                                    topLabel.getText(),
                                    topLabel.getConfidence(),
                                    obj.getBoundingBox(),
                                    obj.getTrackingId()
                            );
                            
                            // Check if this is a treasure clue
                            TreasureClueType clueType = TREASURE_CLUE_OBJECTS.get(topLabel.getText());
                            if (clueType != null) {
                                TreasureClue clue = new TreasureClue(
                                        clueType,
                                        topLabel.getText(),
                                        topLabel.getConfidence() * clueType.getTreasureRelevance(),
                                        obj.getBoundingBox(),
                                        System.currentTimeMillis()
                                );
                                
                                session.treasureClues.add(clue);
                                clueTypeCount.merge(clueType, 1, Integer::sum);
                                
                                if (recognitionListener != null) {
                                    recognitionListener.onTreasureClueFound(clue);
                                }
                            }
                            
                            if (recognitionListener != null) {
                                recognitionListener.onObjectDetected(objectInfo);
                            }
                        }
                    }
                    
                    session.completeTask("objectDetection");
                    checkSessionComplete(session);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Object detection failed", e);
                    session.completeTask("objectDetection");
                    checkSessionComplete(session);
                });
    }
    
    private void processImageLabeling(InputImage image, RecognitionSession session) {
        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    session.labelsDetected = labels.size();
                    
                    for (ImageLabel label : labels) {
                        if (label.getConfidence() >= MIN_LABEL_CONFIDENCE) {
                            
                            // Check if this is a treasure clue
                            TreasureClueType clueType = TREASURE_CLUE_LABELS.get(label.getText());
                            if (clueType != null) {
                                TreasureClue clue = new TreasureClue(
                                        clueType,
                                        label.getText(),
                                        label.getConfidence() * clueType.getTreasureRelevance(),
                                        null, // Labels don't have bounding boxes
                                        System.currentTimeMillis()
                                );
                                
                                session.treasureClues.add(clue);
                                clueTypeCount.merge(clueType, 1, Integer::sum);
                                
                                if (recognitionListener != null) {
                                    recognitionListener.onTreasureClueFound(clue);
                                }
                            }
                        }
                    }
                    
                    session.completeTask("imageLabeling");
                    checkSessionComplete(session);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image labeling failed", e);
                    session.completeTask("imageLabeling");
                    checkSessionComplete(session);
                });
    }
    
    private void processTextRecognition(InputImage image, RecognitionSession session) {
        textRecognizer.process(image)
                .addOnSuccessListener(text -> {
                    session.textBlocksDetected = text.getTextBlocks().size();
                    
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        String blockText = block.getText().toLowerCase();
                        
                        // Check for treasure-related keywords
                        for (String keyword : TREASURE_KEYWORDS) {
                            if (blockText.contains(keyword)) {
                                TreasureClue clue = new TreasureClue(
                                        TreasureClueType.TREASURE_SYMBOL,
                                        "Text: " + keyword,
                                        MIN_TEXT_CONFIDENCE * TreasureClueType.TREASURE_SYMBOL.getTreasureRelevance(),
                                        block.getBoundingBox(),
                                        System.currentTimeMillis()
                                );
                                
                                session.treasureClues.add(clue);
                                clueTypeCount.merge(TreasureClueType.TREASURE_SYMBOL, 1, Integer::sum);
                                
                                if (recognitionListener != null) {
                                    recognitionListener.onTreasureClueFound(clue);
                                }
                                break;
                            }
                        }
                        
                        DetectedTextInfo textInfo = new DetectedTextInfo(
                                block.getText(),
                                block.getConfidence() != null ? block.getConfidence() : 1.0f,
                                block.getBoundingBox()
                        );
                        
                        if (recognitionListener != null) {
                            recognitionListener.onTextDetected(textInfo);
                        }
                    }
                    
                    session.completeTask("textRecognition");
                    checkSessionComplete(session);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    session.completeTask("textRecognition");
                    checkSessionComplete(session);
                });
    }
    
    private void checkSessionComplete(RecognitionSession session) {
        if (session.isComplete()) {
            long processingTime = System.currentTimeMillis() - session.startTime;
            
            // Update performance metrics
            recognitionCount++;
            averageRecognitionTime = (averageRecognitionTime * 0.9f) + (processingTime * 0.1f);
            lastRecognitionTime = System.currentTimeMillis();
            
            // Store recognition result
            String sessionId = "session_" + session.startTime;
            RecognitionResult result = new RecognitionResult(
                    session.startTime,
                    processingTime,
                    session.objectsDetected,
                    session.labelsDetected,
                    session.textBlocksDetected,
                    session.treasureClues.size()
            );
            recentRecognitions.put(sessionId, result);
            
            // Clean up old results (keep only last 50)
            if (recentRecognitions.size() > 50) {
                String oldestKey = recentRecognitions.keySet().iterator().next();
                recentRecognitions.remove(oldestKey);
            }
            
            if (recognitionListener != null) {
                recognitionListener.onRecognitionComplete(session);
            }
            
            Log.d(TAG, "Recognition session complete: " + processingTime + "ms, " + 
                    session.treasureClues.size() + " clues found");
        }
    }
    
    /**
     * Recognition session data
     */
    public static class RecognitionSession {
        public final long startTime;
        public final List<TreasureClue> treasureClues = new ArrayList<>();
        public int objectsDetected = 0;
        public int labelsDetected = 0;
        public int textBlocksDetected = 0;
        
        private final Map<String, Boolean> completedTasks = new HashMap<>();
        
        public RecognitionSession(long startTime) {
            this.startTime = startTime;
            completedTasks.put("objectDetection", false);
            completedTasks.put("imageLabeling", false);
            completedTasks.put("textRecognition", false);
        }
        
        public void completeTask(String taskName) {
            completedTasks.put(taskName, true);
        }
        
        public boolean isComplete() {
            return completedTasks.values().stream().allMatch(Boolean::booleanValue);
        }
    }
    
    /**
     * Detected object information
     */
    public static class DetectedObjectInfo {
        public final String label;
        public final float confidence;
        public final Rect boundingBox;
        public final Integer trackingId;
        
        public DetectedObjectInfo(String label, float confidence, Rect boundingBox, Integer trackingId) {
            this.label = label;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
            this.trackingId = trackingId;
        }
    }
    
    /**
     * Detected text information
     */
    public static class DetectedTextInfo {
        public final String text;
        public final float confidence;
        public final Rect boundingBox;
        
        public DetectedTextInfo(String text, float confidence, Rect boundingBox) {
            this.text = text;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
        }
    }
    
    /**
     * Treasure clue information
     */
    public static class TreasureClue {
        public final TreasureClueType type;
        public final String description;
        public final float confidence;
        public final Rect boundingBox;
        public final long detectionTime;
        
        public TreasureClue(TreasureClueType type, String description, float confidence, 
                          Rect boundingBox, long detectionTime) {
            this.type = type;
            this.description = description;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
            this.detectionTime = detectionTime;
        }
    }
    
    /**
     * Recognition result summary
     */
    public static class RecognitionResult {
        public final long timestamp;
        public final long processingTime;
        public final int objectsDetected;
        public final int labelsDetected;
        public final int textBlocksDetected;
        public final int treasureCluesFound;
        
        public RecognitionResult(long timestamp, long processingTime, int objectsDetected, 
                               int labelsDetected, int textBlocksDetected, int treasureCluesFound) {
            this.timestamp = timestamp;
            this.processingTime = processingTime;
            this.objectsDetected = objectsDetected;
            this.labelsDetected = labelsDetected;
            this.textBlocksDetected = textBlocksDetected;
            this.treasureCluesFound = treasureCluesFound;
        }
    }
}
