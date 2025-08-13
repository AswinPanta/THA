package com.treasurehuntadventure.tha.chat;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * AI-powered content moderation system with profanity filtering and toxicity detection
 */
public class ContentModerationManager {
    private static final String TAG = "ContentModeration";
    
    // Moderation thresholds
    private static final float TOXICITY_THRESHOLD = 0.7f;
    private static final float SEVERE_TOXICITY_THRESHOLD = 0.9f;
    private static final float PROFANITY_THRESHOLD = 0.8f;
    
    // Gaming-specific inappropriate content patterns
    private static final Set<String> GAMING_PROFANITY = new HashSet<>(Arrays.asList(
        "cheat", "hack", "exploit", "noob", "scrub", "trash", "garbage", "suck", "fail"
    ));
    
    private static final Set<String> SEVERE_PROFANITY = new HashSet<>(Arrays.asList(
        // This would contain actual profanity words - using placeholders for demo
        "badword1", "badword2", "badword3"
    ));
    
    private static final Pattern[] SPAM_PATTERNS = {
        Pattern.compile("(.)\\1{4,}"), // Repeated characters
        Pattern.compile("([A-Z]){5,}"), // All caps
        Pattern.compile("(.)\\s*\\1\\s*\\1\\s*\\1"), // Spaced repetition
        Pattern.compile("\\b(\\w+)\\s+\\1\\s+\\1\\b") // Word repetition
    };
    
    // Context-aware moderation rules
    private static final Map<String, Float> CONTEXT_MODIFIERS = new HashMap<>();
    
    static {
        CONTEXT_MODIFIERS.put("treasure_hunt", 0.8f); // More lenient for game context
        CONTEXT_MODIFIERS.put("team_chat", 0.9f); // More lenient for team communication
        CONTEXT_MODIFIERS.put("global_chat", 1.0f); // Standard moderation
        CONTEXT_MODIFIERS.put("help_request", 0.7f); // More lenient for help requests
    }
    
    public enum ModerationAction {
        APPROVE("approve"),
        WARN("warn"),
        FILTER("filter"),
        BLOCK("block"),
        ESCALATE("escalate");
        
        private final String value;
        
        ModerationAction(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    public enum ViolationType {
        PROFANITY("profanity"),
        TOXICITY("toxicity"),
        SPAM("spam"),
        HARASSMENT("harassment"),
        PERSONAL_INFO("personal_info"),
        EXTERNAL_LINKS("external_links"),
        GAMING_CHEATS("gaming_cheats"),
        INAPPROPRIATE_CONTENT("inappropriate_content");
        
        private final String value;
        
        ViolationType(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    private final Context context;
    private final ExecutorService executorService;
    private final Map<String, UserModerationHistory> userHistories;
    private final ToxicityDetector toxicityDetector;
    private final ProfanityFilter profanityFilter;
    private final SpamDetector spamDetector;
    
    // Moderation callbacks
    private ModerationListener moderationListener;
    
    // Performance metrics
    private final Map<String, Long> moderationMetrics = new ConcurrentHashMap<>();
    
    public interface ModerationListener {
        void onModerationComplete(String messageId, ModerationResult result);
        void onModerationError(String messageId, String error);
        void onUserFlagged(String userId, List<ViolationType> violations);
        void onEscalationRequired(String messageId, ModerationResult result);
    }
    
    public ContentModerationManager(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2);
        this.userHistories = new ConcurrentHashMap<>();
        this.toxicityDetector = new ToxicityDetector();
        this.profanityFilter = new ProfanityFilter();
        this.spamDetector = new SpamDetector();
        
        Log.d(TAG, "Content moderation manager initialized");
    }
    
    /**
     * Moderates a chat message with context awareness
     */
    public CompletableFuture<ModerationResult> moderateMessage(EnhancedChatMessage message, 
                                                             String gameContext) {
        CompletableFuture<ModerationResult> future = new CompletableFuture<>();
        
        if (message == null || message.getOriginalText() == null) {
            future.complete(new ModerationResult(true, 1.0f, new ArrayList<>(), 
                    ModerationAction.APPROVE, "Empty message"));
            return future;
        }
        
        // Skip moderation for system messages
        if (message.isSystemMessage()) {
            future.complete(new ModerationResult(true, 1.0f, new ArrayList<>(), 
                    ModerationAction.APPROVE, "System message"));
            return future;
        }
        
        executorService.execute(() -> {
            try {
                performModeration(message, gameContext, future);
            } catch (Exception e) {
                Log.e(TAG, "Moderation failed for message: " + message.getMessageId(), e);
                future.complete(new ModerationResult(false, 0.0f, 
                        Arrays.asList(ViolationType.INAPPROPRIATE_CONTENT), 
                        ModerationAction.BLOCK, "Moderation error: " + e.getMessage()));
            }
        });
        
        return future;
    }
    
    /**
     * Batch moderates multiple messages
     */
    public CompletableFuture<List<ModerationResult>> moderateBatch(List<EnhancedChatMessage> messages, 
                                                                 String gameContext) {
        List<CompletableFuture<ModerationResult>> futures = new ArrayList<>();
        
        for (EnhancedChatMessage message : messages) {
            futures.add(moderateMessage(message, gameContext));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ModerationResult> results = new ArrayList<>();
                    for (CompletableFuture<ModerationResult> future : futures) {
                        try {
                            results.add(future.get());
                        } catch (Exception e) {
                            Log.e(TAG, "Batch moderation item failed", e);
                            results.add(new ModerationResult(false, 0.0f, 
                                    Arrays.asList(ViolationType.INAPPROPRIATE_CONTENT), 
                                    ModerationAction.BLOCK, "Batch item failed"));
                        }
                    }
                    return results;
                });
    }
    
    /**
     * Gets user moderation history
     */
    public UserModerationHistory getUserHistory(String userId) {
        return userHistories.computeIfAbsent(userId, k -> new UserModerationHistory(userId));
    }
    
    /**
     * Updates user moderation history
     */
    public void updateUserHistory(String userId, ModerationResult result) {
        UserModerationHistory history = getUserHistory(userId);
        history.addViolation(result);
        
        // Check if user should be flagged
        if (history.shouldBeFlagged()) {
            if (moderationListener != null) {
                moderationListener.onUserFlagged(userId, history.getRecentViolations());
            }
        }
    }
    
    /**
     * Sets moderation listener
     */
    public void setModerationListener(ModerationListener listener) {
        this.moderationListener = listener;
    }
    
    /**
     * Gets moderation performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalModerations", moderationMetrics.getOrDefault("totalModerations", 0L));
        metrics.put("approvedMessages", moderationMetrics.getOrDefault("approvedMessages", 0L));
        metrics.put("blockedMessages", moderationMetrics.getOrDefault("blockedMessages", 0L));
        metrics.put("averageModerationTime", moderationMetrics.getOrDefault("averageModerationTime", 0L));
        metrics.put("flaggedUsers", userHistories.size());
        return metrics;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        userHistories.clear();
        Log.d(TAG, "Content moderation manager cleaned up");
    }
    
    private void performModeration(EnhancedChatMessage message, String gameContext, 
                                 CompletableFuture<ModerationResult> future) {
        long startTime = System.currentTimeMillis();
        String text = message.getOriginalText();
        String userId = message.getSenderId();
        
        List<ViolationType> violations = new ArrayList<>();
        float overallScore = 1.0f;
        
        // Get context modifier
        float contextModifier = CONTEXT_MODIFIERS.getOrDefault(gameContext, 1.0f);
        
        // Check profanity
        ProfanityResult profanityResult = profanityFilter.checkProfanity(text);
        if (profanityResult.score > PROFANITY_THRESHOLD * contextModifier) {
            violations.add(ViolationType.PROFANITY);
            overallScore = Math.min(overallScore, 1.0f - profanityResult.score);
        }
        
        // Check toxicity
        ToxicityResult toxicityResult = toxicityDetector.checkToxicity(text);
        if (toxicityResult.score > TOXICITY_THRESHOLD * contextModifier) {
            violations.add(ViolationType.TOXICITY);
            overallScore = Math.min(overallScore, 1.0f - toxicityResult.score);
        }
        
        // Check spam
        SpamResult spamResult = spamDetector.checkSpam(text, userId);
        if (spamResult.isSpam) {
            violations.add(ViolationType.SPAM);
            overallScore = Math.min(overallScore, 0.3f);
        }
        
        // Check for personal information
        if (containsPersonalInfo(text)) {
            violations.add(ViolationType.PERSONAL_INFO);
            overallScore = Math.min(overallScore, 0.2f);
        }
        
        // Check for external links
        if (containsExternalLinks(text)) {
            violations.add(ViolationType.EXTERNAL_LINKS);
            overallScore = Math.min(overallScore, 0.5f);
        }
        
        // Check user history
        UserModerationHistory userHistory = getUserHistory(userId);
        float historyModifier = userHistory.getReputationModifier();
        overallScore *= historyModifier;
        
        // Determine moderation action
        ModerationAction action = determineModerationAction(violations, overallScore, userHistory);
        
        // Create result
        ModerationResult result = new ModerationResult(
                action == ModerationAction.APPROVE,
                overallScore,
                violations,
                action,
                "Moderation complete"
        );
        
        // Update metrics
        long processingTime = System.currentTimeMillis() - startTime;
        moderationMetrics.merge("totalModerations", 1L, Long::sum);
        moderationMetrics.merge("averageModerationTime", processingTime, (old, new_) -> (old + new_) / 2);
        
        if (result.isApproved()) {
            moderationMetrics.merge("approvedMessages", 1L, Long::sum);
        } else {
            moderationMetrics.merge("blockedMessages", 1L, Long::sum);
        }
        
        // Update user history
        updateUserHistory(userId, result);
        
        // Notify listener
        if (moderationListener != null) {
            moderationListener.onModerationComplete(message.getMessageId(), result);
            
            if (action == ModerationAction.ESCALATE) {
                moderationListener.onEscalationRequired(message.getMessageId(), result);
            }
        }
        
        future.complete(result);
    }
    
    private ModerationAction determineModerationAction(List<ViolationType> violations, 
                                                     float overallScore, 
                                                     UserModerationHistory userHistory) {
        if (violations.isEmpty()) {
            return ModerationAction.APPROVE;
        }
        
        // Severe violations always block
        if (violations.contains(ViolationType.PERSONAL_INFO) || 
            violations.contains(ViolationType.HARASSMENT)) {
            return ModerationAction.BLOCK;
        }
        
        // Check severity based on score
        if (overallScore < 0.3f) {
            return ModerationAction.BLOCK;
        } else if (overallScore < 0.5f) {
            return userHistory.getViolationCount() > 3 ? ModerationAction.BLOCK : ModerationAction.FILTER;
        } else if (overallScore < 0.7f) {
            return ModerationAction.WARN;
        }
        
        // Check for repeat offenders
        if (userHistory.getViolationCount() > 5) {
            return ModerationAction.ESCALATE;
        }
        
        return ModerationAction.APPROVE;
    }
    
    private boolean containsPersonalInfo(String text) {
        // Simple patterns for personal information
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        Pattern phonePattern = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
        Pattern ssnPattern = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
        
        return emailPattern.matcher(text).find() || 
               phonePattern.matcher(text).find() || 
               ssnPattern.matcher(text).find();
    }
    
    private boolean containsExternalLinks(String text) {
        Pattern urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
        return urlPattern.matcher(text).find();
    }
    
    /**
     * Moderation result
     */
    public static class ModerationResult {
        private final boolean isApproved;
        private final float confidenceScore;
        private final List<ViolationType> detectedViolations;
        private final ModerationAction action;
        private final String reason;
        private final long timestamp;
        
        public ModerationResult(boolean isApproved, float confidenceScore, 
                              List<ViolationType> detectedViolations, 
                              ModerationAction action, String reason) {
            this.isApproved = isApproved;
            this.confidenceScore = confidenceScore;
            this.detectedViolations = detectedViolations;
            this.action = action;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public boolean isApproved() { return isApproved; }
        public float getConfidenceScore() { return confidenceScore; }
        public List<ViolationType> getDetectedViolations() { return detectedViolations; }
        public ModerationAction getAction() { return action; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * User moderation history
     */
    public static class UserModerationHistory {
        private final String userId;
        private final List<ModerationResult> violations;
        private int warningCount;
        private long lastViolationTime;
        
        public UserModerationHistory(String userId) {
            this.userId = userId;
            this.violations = new ArrayList<>();
            this.warningCount = 0;
            this.lastViolationTime = 0;
        }
        
        public void addViolation(ModerationResult result) {
            if (!result.isApproved()) {
                violations.add(result);
                lastViolationTime = System.currentTimeMillis();
                
                if (result.getAction() == ModerationAction.WARN) {
                    warningCount++;
                }
            }
        }
        
        public boolean shouldBeFlagged() {
            return violations.size() > 3 || warningCount > 5;
        }
        
        public float getReputationModifier() {
            if (violations.isEmpty()) {
                return 1.0f;
            }
            
            // Reduce score based on recent violations
            long recentViolations = violations.stream()
                    .mapToLong(v -> System.currentTimeMillis() - v.getTimestamp())
                    .filter(age -> age < 24 * 60 * 60 * 1000) // Last 24 hours
                    .count();
            
            return Math.max(0.3f, 1.0f - (recentViolations * 0.1f));
        }
        
        public List<ViolationType> getRecentViolations() {
            return violations.stream()
                    .filter(v -> System.currentTimeMillis() - v.getTimestamp() < 24 * 60 * 60 * 1000)
                    .flatMap(v -> v.getDetectedViolations().stream())
                    .distinct()
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        public int getViolationCount() { return violations.size(); }
        public int getWarningCount() { return warningCount; }
        public long getLastViolationTime() { return lastViolationTime; }
    }
    
    // Helper classes for different types of detection
    private static class ToxicityDetector {
        public ToxicityResult checkToxicity(String text) {
            // Simplified toxicity detection
            // In a real implementation, this would use ML models
            float score = 0.0f;
            
            String lowerText = text.toLowerCase();
            for (String word : SEVERE_PROFANITY) {
                if (lowerText.contains(word)) {
                    score = Math.max(score, 0.9f);
                }
            }
            
            return new ToxicityResult(score);
        }
    }
    
    private static class ToxicityResult {
        public final float score;
        
        public ToxicityResult(float score) {
            this.score = score;
        }
    }
    
    private static class ProfanityFilter {
        public ProfanityResult checkProfanity(String text) {
            float score = 0.0f;
            String lowerText = text.toLowerCase();
            
            for (String word : GAMING_PROFANITY) {
                if (lowerText.contains(word)) {
                    score = Math.max(score, 0.6f);
                }
            }
            
            for (String word : SEVERE_PROFANITY) {
                if (lowerText.contains(word)) {
                    score = Math.max(score, 0.9f);
                }
            }
            
            return new ProfanityResult(score);
        }
    }
    
    private static class ProfanityResult {
        public final float score;
        
        public ProfanityResult(float score) {
            this.score = score;
        }
    }
    
    private static class SpamDetector {
        private final Map<String, List<Long>> userMessageTimes = new ConcurrentHashMap<>();
        
        public SpamResult checkSpam(String text, String userId) {
            boolean isSpam = false;
            
            // Check for spam patterns
            for (Pattern pattern : SPAM_PATTERNS) {
                if (pattern.matcher(text).find()) {
                    isSpam = true;
                    break;
                }
            }
            
            // Check message frequency
            List<Long> messageTimes = userMessageTimes.computeIfAbsent(userId, k -> new ArrayList<>());
            long currentTime = System.currentTimeMillis();
            messageTimes.add(currentTime);
            
            // Remove old messages (older than 1 minute)
            messageTimes.removeIf(time -> currentTime - time > 60000);
            
            // Check if too many messages in short time
            if (messageTimes.size() > 10) { // More than 10 messages per minute
                isSpam = true;
            }
            
            return new SpamResult(isSpam);
        }
    }
    
    private static class SpamResult {
        public final boolean isSpam;
        
        public SpamResult(boolean isSpam) {
            this.isSpam = isSpam;
        }
    }
}
