package com.treasurehuntadventure.tha.chat;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real-time translation service with automatic language detection and caching
 */
public class TranslationService {
    private static final String TAG = "TranslationService";
    
    // Translation constants
    private static final float MIN_LANGUAGE_CONFIDENCE = 0.5f;
    private static final int CACHE_SIZE = 1000; // Number of cached translations
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours
    
    // Supported languages for gaming context
    private static final Map<String, String> SUPPORTED_LANGUAGES = new HashMap<>();
    
    static {
        SUPPORTED_LANGUAGES.put("en", "English");
        SUPPORTED_LANGUAGES.put("es", "Spanish");
        SUPPORTED_LANGUAGES.put("fr", "French");
        SUPPORTED_LANGUAGES.put("de", "German");
        SUPPORTED_LANGUAGES.put("it", "Italian");
        SUPPORTED_LANGUAGES.put("pt", "Portuguese");
        SUPPORTED_LANGUAGES.put("ru", "Russian");
        SUPPORTED_LANGUAGES.put("ja", "Japanese");
        SUPPORTED_LANGUAGES.put("ko", "Korean");
        SUPPORTED_LANGUAGES.put("zh", "Chinese");
        SUPPORTED_LANGUAGES.put("ar", "Arabic");
        SUPPORTED_LANGUAGES.put("hi", "Hindi");
        SUPPORTED_LANGUAGES.put("th", "Thai");
        SUPPORTED_LANGUAGES.put("vi", "Vietnamese");
        SUPPORTED_LANGUAGES.put("tr", "Turkish");
    }
    
    private final Context context;
    private final LanguageIdentifier languageIdentifier;
    private final Map<String, Translator> translators;
    private final LruCache<String, CachedTranslation> translationCache;
    private final ExecutorService executorService;
    
    // Translation callbacks
    private TranslationListener translationListener;
    
    // Performance metrics
    private final Map<String, Long> performanceMetrics = new ConcurrentHashMap<>();
    
    public interface TranslationListener {
        void onLanguageDetected(String text, String languageCode, float confidence);
        void onTranslationComplete(String originalText, String translatedText, 
                                 String sourceLanguage, String targetLanguage);
        void onTranslationError(String text, String error);
        void onTranslationCached(String cacheKey, boolean fromCache);
    }
    
    public TranslationService(Context context) {
        this.context = context;
        this.languageIdentifier = LanguageIdentification.getClient();
        this.translators = new ConcurrentHashMap<>();
        this.translationCache = new LruCache<String, CachedTranslation>(CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, CachedTranslation translation) {
                return translation.originalText.length() + translation.translatedText.length();
            }
        };
        this.executorService = Executors.newFixedThreadPool(3);
        
        Log.d(TAG, "Translation service initialized");
    }
    
    /**
     * Detects language of the given text
     */
    public CompletableFuture<LanguageDetectionResult> detectLanguage(String text) {
        CompletableFuture<LanguageDetectionResult> future = new CompletableFuture<>();
        
        if (text == null || text.trim().isEmpty()) {
            future.complete(new LanguageDetectionResult("en", 0.0f, "Empty text"));
            return future;
        }
        
        long startTime = System.currentTimeMillis();
        
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    performanceMetrics.put("languageDetectionTime", processingTime);
                    
                    if (languageCode.equals("und")) {
                        // Undetermined language
                        future.complete(new LanguageDetectionResult("en", 0.0f, "Language undetermined"));
                    } else {
                        // Get confidence score (simplified - ML Kit doesn't provide confidence directly)
                        float confidence = calculateLanguageConfidence(text, languageCode);
                        
                        LanguageDetectionResult result = new LanguageDetectionResult(
                                languageCode, confidence, "Detected");
                        
                        if (translationListener != null) {
                            translationListener.onLanguageDetected(text, languageCode, confidence);
                        }
                        
                        future.complete(result);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Language detection failed", e);
                    future.complete(new LanguageDetectionResult("en", 0.0f, "Detection failed"));
                    
                    if (translationListener != null) {
                        translationListener.onTranslationError(text, "Language detection failed: " + e.getMessage());
                    }
                });
        
        return future;
    }
    
    /**
     * Translates text from source language to target language
     */
    public CompletableFuture<TranslationResult> translateText(String text, String sourceLanguage, 
                                                            String targetLanguage) {
        CompletableFuture<TranslationResult> future = new CompletableFuture<>();
        
        if (text == null || text.trim().isEmpty()) {
            future.complete(new TranslationResult(text, text, sourceLanguage, targetLanguage, 
                    false, "Empty text"));
            return future;
        }
        
        if (sourceLanguage.equals(targetLanguage)) {
            future.complete(new TranslationResult(text, text, sourceLanguage, targetLanguage, 
                    false, "Same language"));
            return future;
        }
        
        // Check cache first
        String cacheKey = generateCacheKey(text, sourceLanguage, targetLanguage);
        CachedTranslation cached = translationCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            if (translationListener != null) {
                translationListener.onTranslationCached(cacheKey, true);
                translationListener.onTranslationComplete(text, cached.translatedText, 
                        sourceLanguage, targetLanguage);
            }
            
            future.complete(new TranslationResult(text, cached.translatedText, 
                    sourceLanguage, targetLanguage, true, "From cache"));
            return future;
        }
        
        // Perform translation
        executorService.execute(() -> {
            try {
                performTranslation(text, sourceLanguage, targetLanguage, future, cacheKey);
            } catch (Exception e) {
                Log.e(TAG, "Translation execution failed", e);
                future.complete(new TranslationResult(text, text, sourceLanguage, targetLanguage, 
                        false, "Translation failed: " + e.getMessage()));
            }
        });
        
        return future;
    }
    
    /**
     * Translates text with automatic language detection
     */
    public CompletableFuture<TranslationResult> translateTextAuto(String text, String targetLanguage) {
        return detectLanguage(text)
                .thenCompose(detection -> {
                    if (detection.confidence < MIN_LANGUAGE_CONFIDENCE) {
                        // Use English as fallback
                        return translateText(text, "en", targetLanguage);
                    } else {
                        return translateText(text, detection.languageCode, targetLanguage);
                    }
                });
    }
    
    /**
     * Batch translates multiple texts
     */
    public CompletableFuture<List<TranslationResult>> translateBatch(List<String> texts, 
                                                                   String sourceLanguage, 
                                                                   String targetLanguage) {
        List<CompletableFuture<TranslationResult>> futures = new ArrayList<>();
        
        for (String text : texts) {
            futures.add(translateText(text, sourceLanguage, targetLanguage));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<TranslationResult> results = new ArrayList<>();
                    for (CompletableFuture<TranslationResult> future : futures) {
                        try {
                            results.add(future.get());
                        } catch (Exception e) {
                            Log.e(TAG, "Batch translation item failed", e);
                            results.add(new TranslationResult("", "", sourceLanguage, targetLanguage, 
                                    false, "Batch item failed"));
                        }
                    }
                    return results;
                });
    }
    
    /**
     * Gets supported languages
     */
    public Map<String, String> getSupportedLanguages() {
        return new HashMap<>(SUPPORTED_LANGUAGES);
    }
    
    /**
     * Checks if language is supported
     */
    public boolean isLanguageSupported(String languageCode) {
        return SUPPORTED_LANGUAGES.containsKey(languageCode);
    }
    
    /**
     * Gets user's preferred language
     */
    public String getUserPreferredLanguage() {
        return Locale.getDefault().getLanguage();
    }
    
    /**
     * Sets translation listener
     */
    public void setTranslationListener(TranslationListener listener) {
        this.translationListener = listener;
    }
    
    /**
     * Gets performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cacheSize", translationCache.size());
        metrics.put("cacheHitRate", calculateCacheHitRate());
        metrics.put("activeTranslators", translators.size());
        metrics.putAll(performanceMetrics);
        return metrics;
    }
    
    /**
     * Clears translation cache
     */
    public void clearCache() {
        translationCache.evictAll();
        Log.d(TAG, "Translation cache cleared");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Close all translators
        for (Translator translator : translators.values()) {
            translator.close();
        }
        translators.clear();
        
        // Clear cache
        translationCache.evictAll();
        
        // Shutdown executor
        executorService.shutdown();
        
        Log.d(TAG, "Translation service cleaned up");
    }
    
    private void performTranslation(String text, String sourceLanguage, String targetLanguage, 
                                  CompletableFuture<TranslationResult> future, String cacheKey) {
        long startTime = System.currentTimeMillis();
        
        // Get or create translator
        String translatorKey = sourceLanguage + "-" + targetLanguage;
        Translator translator = translators.get(translatorKey);
        
        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage))
                    .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage))
                    .build();
            
            translator = Translation.getClient(options);
            translators.put(translatorKey, translator);
        }
        
        // Download model if needed and translate
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(v -> {
                    translator.translate(text)
                            .addOnSuccessListener(translatedText -> {
                                long processingTime = System.currentTimeMillis() - startTime;
                                performanceMetrics.put("translationTime", processingTime);
                                
                                // Cache the result
                                CachedTranslation cached = new CachedTranslation(
                                        text, translatedText, sourceLanguage, targetLanguage);
                                translationCache.put(cacheKey, cached);
                                
                                if (translationListener != null) {
                                    translationListener.onTranslationCached(cacheKey, false);
                                    translationListener.onTranslationComplete(text, translatedText, 
                                            sourceLanguage, targetLanguage);
                                }
                                
                                future.complete(new TranslationResult(text, translatedText, 
                                        sourceLanguage, targetLanguage, false, "Success"));
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Translation failed", e);
                                future.complete(new TranslationResult(text, text, sourceLanguage, 
                                        targetLanguage, false, "Translation failed: " + e.getMessage()));
                                
                                if (translationListener != null) {
                                    translationListener.onTranslationError(text, e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed", e);
                    future.complete(new TranslationResult(text, text, sourceLanguage, targetLanguage, 
                            false, "Model download failed: " + e.getMessage()));
                    
                    if (translationListener != null) {
                        translationListener.onTranslationError(text, "Model download failed: " + e.getMessage());
                    }
                });
    }
    
    private float calculateLanguageConfidence(String text, String languageCode) {
        // Simplified confidence calculation based on text characteristics
        // In a real implementation, this would use more sophisticated analysis
        
        if (text.length() < 10) {
            return 0.6f; // Lower confidence for short texts
        }
        
        if (SUPPORTED_LANGUAGES.containsKey(languageCode)) {
            return 0.8f; // Higher confidence for supported languages
        }
        
        return 0.5f; // Default confidence
    }
    
    private String generateCacheKey(String text, String sourceLanguage, String targetLanguage) {
        return text.hashCode() + "_" + sourceLanguage + "_" + targetLanguage;
    }
    
    private float calculateCacheHitRate() {
        long hits = performanceMetrics.getOrDefault("cacheHits", 0L);
        long misses = performanceMetrics.getOrDefault("cacheMisses", 0L);
        long total = hits + misses;
        
        return total > 0 ? (float) hits / total : 0f;
    }
    
    /**
     * Language detection result
     */
    public static class LanguageDetectionResult {
        public final String languageCode;
        public final float confidence;
        public final String status;
        
        public LanguageDetectionResult(String languageCode, float confidence, String status) {
            this.languageCode = languageCode;
            this.confidence = confidence;
            this.status = status;
        }
    }
    
    /**
     * Translation result
     */
    public static class TranslationResult {
        public final String originalText;
        public final String translatedText;
        public final String sourceLanguage;
        public final String targetLanguage;
        public final boolean fromCache;
        public final String status;
        
        public TranslationResult(String originalText, String translatedText, String sourceLanguage, 
                               String targetLanguage, boolean fromCache, String status) {
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.fromCache = fromCache;
            this.status = status;
        }
    }
    
    /**
     * Cached translation entry
     */
    private static class CachedTranslation {
        public final String originalText;
        public final String translatedText;
        public final String sourceLanguage;
        public final String targetLanguage;
        public final long timestamp;
        
        public CachedTranslation(String originalText, String translatedText, 
                               String sourceLanguage, String targetLanguage) {
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
}
