package com.treasurehuntadventure.tha.challenges;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.treasurehuntadventure.tha.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Advanced RecyclerView adapter for personalized challenges with animations and interactions
 */
public class PersonalizedChallengeAdapter extends RecyclerView.Adapter<PersonalizedChallengeAdapter.ChallengeViewHolder> {
    
    private final Context context;
    private List<PersonalizedChallenge> challenges;
    private final ChallengeInteractionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public interface ChallengeInteractionListener {
        void onChallengeStarted(PersonalizedChallenge challenge);
        void onChallengeCompleted(PersonalizedChallenge challenge);
        void onChallengeAbandoned(PersonalizedChallenge challenge);
        void onChallengeDetailsRequested(PersonalizedChallenge challenge);
    }
    
    public PersonalizedChallengeAdapter(Context context, List<PersonalizedChallenge> challenges, 
                                      ChallengeInteractionListener listener) {
        this.context = context;
        this.challenges = challenges;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_personalized_challenge, parent, false);
        return new ChallengeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        PersonalizedChallenge challenge = challenges.get(position);
        holder.bind(challenge, position);
    }
    
    @Override
    public int getItemCount() {
        return challenges.size();
    }
    
    public void updateChallenges(List<PersonalizedChallenge> newChallenges) {
        this.challenges = newChallenges;
        notifyDataSetChanged();
    }
    
    public void updateChallenge(PersonalizedChallenge updatedChallenge) {
        for (int i = 0; i < challenges.size(); i++) {
            if (challenges.get(i).getChallengeId().equals(updatedChallenge.getChallengeId())) {
                challenges.set(i, updatedChallenge);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    class ChallengeViewHolder extends RecyclerView.ViewHolder {
        
        // Main card and layout
        private final MaterialCardView challengeCard;
        private final View cardContent;
        
        // Header section
        private final MaterialTextView challengeTitle;
        private final MaterialTextView challengeDescription;
        private final ImageView challengeTypeIcon;
        private final Chip difficultyChip;
        private final Chip statusChip;
        
        // AI insights section
        private final View aiInsightsSection;
        private final MaterialTextView engagementScore;
        private final MaterialTextView personalizationScore;
        private final ChipGroup aiTagsGroup;
        
        // Progress section
        private final View progressSection;
        private final LinearProgressIndicator progressBar;
        private final MaterialTextView progressText;
        private final MaterialTextView timeRemainingText;
        private final CircularProgressIndicator timeProgressIndicator;
        
        // Milestones section
        private final View milestonesSection;
        private final RecyclerView milestonesRecyclerView;
        
        // Rewards section
        private final View rewardsSection;
        private final MaterialTextView experienceReward;
        private final MaterialTextView coinsReward;
        private final MaterialTextView bonusRewardText;
        private final LottieAnimationView rewardAnimation;
        
        // Action buttons
        private final MaterialButton primaryActionButton;
        private final MaterialButton secondaryActionButton;
        private final MaterialButton detailsButton;
        
        // Animation views
        private final LottieAnimationView completionAnimation;
        private final View shimmerOverlay;
        
        public ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Initialize views
            challengeCard = itemView.findViewById(R.id.challenge_card);
            cardContent = itemView.findViewById(R.id.card_content);
            
            // Header views
            challengeTitle = itemView.findViewById(R.id.challenge_title);
            challengeDescription = itemView.findViewById(R.id.challenge_description);
            challengeTypeIcon = itemView.findViewById(R.id.challenge_type_icon);
            difficultyChip = itemView.findViewById(R.id.difficulty_chip);
            statusChip = itemView.findViewById(R.id.status_chip);
            
            // AI insights views
            aiInsightsSection = itemView.findViewById(R.id.ai_insights_section);
            engagementScore = itemView.findViewById(R.id.engagement_score);
            personalizationScore = itemView.findViewById(R.id.personalization_score);
            aiTagsGroup = itemView.findViewById(R.id.ai_tags_group);
            
            // Progress views
            progressSection = itemView.findViewById(R.id.progress_section);
            progressBar = itemView.findViewById(R.id.progress_bar);
            progressText = itemView.findViewById(R.id.progress_text);
            timeRemainingText = itemView.findViewById(R.id.time_remaining_text);
            timeProgressIndicator = itemView.findViewById(R.id.time_progress_indicator);
            
            // Milestones views
            milestonesSection = itemView.findViewById(R.id.milestones_section);
            milestonesRecyclerView = itemView.findViewById(R.id.milestones_recycler_view);
            
            // Rewards views
            rewardsSection = itemView.findViewById(R.id.rewards_section);
            experienceReward = itemView.findViewById(R.id.experience_reward);
            coinsReward = itemView.findViewById(R.id.coins_reward);
            bonusRewardText = itemView.findViewById(R.id.bonus_reward_text);
            rewardAnimation = itemView.findViewById(R.id.reward_animation);
            
            // Action buttons
            primaryActionButton = itemView.findViewById(R.id.primary_action_button);
            secondaryActionButton = itemView.findViewById(R.id.secondary_action_button);
            detailsButton = itemView.findViewById(R.id.details_button);
            
            // Animation views
            completionAnimation = itemView.findViewById(R.id.completion_animation);
            shimmerOverlay = itemView.findViewById(R.id.shimmer_overlay);
        }
        
        public void bind(PersonalizedChallenge challenge, int position) {
            // Animate card entrance
            animateCardEntrance(position);
            
            // Bind basic information
            bindBasicInfo(challenge);
            
            // Bind AI insights
            bindAIInsights(challenge);
            
            // Bind progress information
            bindProgress(challenge);
            
            // Bind milestones
            bindMilestones(challenge);
            
            // Bind rewards
            bindRewards(challenge);
            
            // Setup action buttons
            setupActionButtons(challenge);
            
            // Apply challenge state styling
            applyChallengeStateStyling(challenge);
            
            // Setup click listeners
            setupClickListeners(challenge);
        }
        
        private void animateCardEntrance(int position) {
            // Staggered animation for smooth list appearance
            long delay = position * 100L;
            
            cardContent.setAlpha(0f);
            cardContent.setTranslationY(50f);
            
            cardContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(delay)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        
        private void bindBasicInfo(PersonalizedChallenge challenge) {
            // Set title and description
            challengeTitle.setText(challenge.getTitle());
            challengeDescription.setText(challenge.getDescription());
            
            // Set type icon
            setTypeIcon(challenge.getType());
            
            // Set difficulty chip
            setupDifficultyChip(challenge);
            
            // Set status chip
            setupStatusChip(challenge);
        }
        
        private void setTypeIcon(String challengeType) {
            int iconRes;
            switch (challengeType) {
                case "treasure_hunt":
                    iconRes = R.drawable.ic_treasure_chest;
                    break;
                case "exploration":
                    iconRes = R.drawable.ic_compass;
                    break;
                case "speed_challenge":
                    iconRes = R.drawable.ic_speed;
                    break;
                case "accuracy_challenge":
                    iconRes = R.drawable.ic_target;
                    break;
                case "social_challenge":
                    iconRes = R.drawable.ic_people;
                    break;
                case "collection_challenge":
                    iconRes = R.drawable.ic_collection;
                    break;
                case "distance_challenge":
                    iconRes = R.drawable.ic_walk;
                    break;
                case "time_challenge":
                    iconRes = R.drawable.ic_timer;
                    break;
                case "skill_challenge":
                    iconRes = R.drawable.ic_skill;
                    break;
                case "creative_challenge":
                    iconRes = R.drawable.ic_creative;
                    break;
                default:
                    iconRes = R.drawable.ic_star;
                    break;
            }
            challengeTypeIcon.setImageResource(iconRes);
        }
        
        private void setupDifficultyChip(PersonalizedChallenge challenge) {
            difficultyChip.setText(challenge.getDifficulty());
            difficultyChip.setChipIcon(ContextCompat.getDrawable(context, getDifficultyIcon(challenge.getDifficulty())));
            
            // Set difficulty color
            int colorRes = getDifficultyColor(challenge.getDifficulty());
            difficultyChip.setChipBackgroundColor(ContextCompat.getColorStateList(context, colorRes));
        }
        
        private void setupStatusChip(PersonalizedChallenge challenge) {
            if (challenge.isCompleted()) {
                statusChip.setText("Completed");
                statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.success_color));
                statusChip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_check_circle));
            } else if (challenge.isFailed()) {
                statusChip.setText("Failed");
                statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.error_color));
                statusChip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_cancel));
            } else if (challenge.isActive()) {
                statusChip.setText("Active");
                statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.primary_color));
                statusChip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_play_arrow));
            } else {
                statusChip.setText("Available");
                statusChip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.secondary_color));
                statusChip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_available));
            }
            statusChip.setVisibility(View.VISIBLE);
        }
        
        private void bindAIInsights(PersonalizedChallenge challenge) {
            // Show AI insights section
            aiInsightsSection.setVisibility(View.VISIBLE);
            
            // Set engagement score
            double engagement = challenge.getPredictedEngagement();
            engagementScore.setText(String.format(Locale.getDefault(), "%.0f%%", engagement * 100));
            setScoreColor(engagementScore, engagement);
            
            // Set personalization score
            double personalization = challenge.getPersonalizationScore();
            personalizationScore.setText(String.format(Locale.getDefault(), "%.0f%%", personalization * 100));
            setScoreColor(personalizationScore, personalization);
            
            // Set AI tags
            setupAITags(challenge.getAiTags());
        }
        
        private void setScoreColor(MaterialTextView textView, double score) {
            int colorRes;
            if (score >= 0.8) {
                colorRes = R.color.success_color;
            } else if (score >= 0.6) {
                colorRes = R.color.warning_color;
            } else {
                colorRes = R.color.error_color;
            }
            textView.setTextColor(ContextCompat.getColor(context, colorRes));
        }
        
        private void setupAITags(List<String> aiTags) {
            aiTagsGroup.removeAllViews();
            
            for (String tag : aiTags) {
                Chip tagChip = new Chip(context);
                tagChip.setText(tag.replace("_", " ").toUpperCase());
                tagChip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.ai_tag_background));
                tagChip.setTextColor(ContextCompat.getColor(context, R.color.ai_tag_text));
                tagChip.setTextSize(10f);
                aiTagsGroup.addView(tagChip);
            }
        }
        
        private void bindProgress(PersonalizedChallenge challenge) {
            PersonalizedChallenge.ChallengeProgress progress = challenge.getProgress();
            
            if (progress != null && challenge.isActive()) {
                progressSection.setVisibility(View.VISIBLE);
                
                // Set progress bar
                int progressPercent = (int) (progress.getCompletionPercentage() * 100);
                progressBar.setProgress(progressPercent);
                progressText.setText(String.format(Locale.getDefault(), "%d%% Complete", progressPercent));
                
                // Set time remaining
                setupTimeRemaining(challenge);
                
                // Animate progress if recently updated
                if (System.currentTimeMillis() - progress.getLastUpdated() < 5000) {
                    animateProgressUpdate(progressPercent);
                }
            } else {
                progressSection.setVisibility(View.GONE);
            }
        }
        
        private void setupTimeRemaining(PersonalizedChallenge challenge) {
            long remainingTime = challenge.getRemainingTime();
            
            if (remainingTime != Long.MAX_VALUE) {
                long hours = TimeUnit.MILLISECONDS.toHours(remainingTime);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
                
                if (hours > 0) {
                    timeRemainingText.setText(String.format(Locale.getDefault(), "%dh %dm remaining", hours, minutes));
                } else {
                    timeRemainingText.setText(String.format(Locale.getDefault(), "%dm remaining", minutes));
                }
                
                // Set time progress indicator
                double timeProgress = challenge.getTimeProgress();
                timeProgressIndicator.setProgress((int) (timeProgress * 100));
                
                // Color code based on urgency
                if (timeProgress > 0.8) {
                    timeRemainingText.setTextColor(ContextCompat.getColor(context, R.color.error_color));
                } else if (timeProgress > 0.6) {
                    timeRemainingText.setTextColor(ContextCompat.getColor(context, R.color.warning_color));
                } else {
                    timeRemainingText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                }
                
                timeRemainingText.setVisibility(View.VISIBLE);
                timeProgressIndicator.setVisibility(View.VISIBLE);
            } else {
                timeRemainingText.setVisibility(View.GONE);
                timeProgressIndicator.setVisibility(View.GONE);
            }
        }
        
        private void bindMilestones(PersonalizedChallenge challenge) {
            List<PersonalizedChallenge.Milestone> milestones = challenge.getMilestones();
            
            if (milestones != null && !milestones.isEmpty()) {
                milestonesSection.setVisibility(View.VISIBLE);
                
                // Setup milestones adapter
                MilestoneAdapter milestoneAdapter = new MilestoneAdapter(context, milestones);
                milestonesRecyclerView.setLayoutManager(
                        new androidx.recyclerview.widget.LinearLayoutManager(context, 
                                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
                milestonesRecyclerView.setAdapter(milestoneAdapter);
            } else {
                milestonesSection.setVisibility(View.GONE);
            }
        }
        
        private void bindRewards(PersonalizedChallenge challenge) {
            PersonalizedChallenge.ChallengeRewards rewards = challenge.getRewards();
            
            if (rewards != null) {
                rewardsSection.setVisibility(View.VISIBLE);
                
                // Set base rewards
                experienceReward.setText(String.format(Locale.getDefault(), "+%d XP", rewards.getBaseExperience()));
                coinsReward.setText(String.format(Locale.getDefault(), "+%d", rewards.getBaseCoins()));
                
                // Set bonus rewards
                if (challenge.isBonusEarned() || !challenge.getBonusConditions().isEmpty()) {
                    bonusRewardText.setVisibility(View.VISIBLE);
                    if (challenge.isBonusEarned()) {
                        bonusRewardText.setText("🎉 Bonus Earned!");
                        bonusRewardText.setTextColor(ContextCompat.getColor(context, R.color.success_color));
                    } else {
                        bonusRewardText.setText("Bonus Available");
                        bonusRewardText.setTextColor(ContextCompat.getColor(context, R.color.warning_color));
                    }
                } else {
                    bonusRewardText.setVisibility(View.GONE);
                }
                
                // Setup reward animation
                if (challenge.isCompleted()) {
                    rewardAnimation.setAnimation("reward_celebration.json");
                    rewardAnimation.setVisibility(View.VISIBLE);
                    rewardAnimation.playAnimation();
                } else {
                    rewardAnimation.setVisibility(View.GONE);
                }
            } else {
                rewardsSection.setVisibility(View.GONE);
            }
        }
        
        private void setupActionButtons(PersonalizedChallenge challenge) {
            if (challenge.isCompleted()) {
                primaryActionButton.setText("View Rewards");
                primaryActionButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_gift));
                secondaryActionButton.setVisibility(View.GONE);
            } else if (challenge.isFailed()) {
                primaryActionButton.setText("Retry");
                primaryActionButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_refresh));
                secondaryActionButton.setVisibility(View.GONE);
            } else if (challenge.isActive()) {
                primaryActionButton.setText("Continue");
                primaryActionButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_play_arrow));
                secondaryActionButton.setText("Abandon");
                secondaryActionButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_stop));
                secondaryActionButton.setVisibility(View.VISIBLE);
            } else {
                primaryActionButton.setText("Start Challenge");
                primaryActionButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_start));
                secondaryActionButton.setVisibility(View.GONE);
            }
            
            detailsButton.setText("Details");
            detailsButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_info));
        }
        
        private void applyChallengeStateStyling(PersonalizedChallenge challenge) {
            if (challenge.isCompleted()) {
                challengeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_background));
                challengeCard.setStrokeColor(ContextCompat.getColor(context, R.color.success_color));
                challengeCard.setStrokeWidth(4);
            } else if (challenge.isFailed()) {
                challengeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.error_background));
                challengeCard.setStrokeColor(ContextCompat.getColor(context, R.color.error_color));
                challengeCard.setStrokeWidth(2);
            } else if (challenge.isActive()) {
                challengeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_background));
                challengeCard.setStrokeColor(ContextCompat.getColor(context, R.color.primary_color));
                challengeCard.setStrokeWidth(3);
                
                // Add pulsing animation for active challenges
                addPulsingAnimation();
            } else {
                challengeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
                challengeCard.setStrokeColor(ContextCompat.getColor(context, R.color.outline));
                challengeCard.setStrokeWidth(1);
            }
        }
        
        private void setupClickListeners(PersonalizedChallenge challenge) {
            primaryActionButton.setOnClickListener(v -> {
                if (challenge.isCompleted()) {
                    // Show rewards
                    showRewardsDialog(challenge);
                } else if (challenge.isFailed()) {
                    // Retry challenge
                    if (listener != null) {
                        listener.onChallengeStarted(challenge);
                    }
                } else if (challenge.isActive()) {
                    // Continue challenge
                    if (listener != null) {
                        listener.onChallengeDetailsRequested(challenge);
                    }
                } else {
                    // Start challenge
                    if (listener != null) {
                        listener.onChallengeStarted(challenge);
                    }
                    animateStartChallenge();
                }
            });
            
            secondaryActionButton.setOnClickListener(v -> {
                if (challenge.isActive() && listener != null) {
                    listener.onChallengeAbandoned(challenge);
                }
            });
            
            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChallengeDetailsRequested(challenge);
                }
            });
            
            // Card click for details
            challengeCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChallengeDetailsRequested(challenge);
                }
            });
        }
        
        // Animation methods
        private void animateProgressUpdate(int newProgress) {
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 
                    progressBar.getProgress(), newProgress);
            progressAnimator.setDuration(500);
            progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            progressAnimator.start();
        }
        
        private void addPulsingAnimation() {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(challengeCard, "scaleX", 1.0f, 1.02f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(challengeCard, "scaleY", 1.0f, 1.02f, 1.0f);
            
            AnimatorSet pulseAnimator = new AnimatorSet();
            pulseAnimator.playTogether(scaleX, scaleY);
            pulseAnimator.setDuration(2000);
            pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            pulseAnimator.start();
        }
        
        private void animateStartChallenge() {
            // Scale animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(primaryActionButton, "scaleX", 1.0f, 0.9f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(primaryActionButton, "scaleY", 1.0f, 0.9f, 1.0f);
            
            AnimatorSet scaleAnimator = new AnimatorSet();
            scaleAnimator.playTogether(scaleX, scaleY);
            scaleAnimator.setDuration(200);
            scaleAnimator.start();
        }
        
        private void showRewardsDialog(PersonalizedChallenge challenge) {
            // This would typically show a dialog with detailed rewards
            // For now, we'll just show the completion animation
            completionAnimation.setAnimation("treasure_reward.json");
            completionAnimation.setVisibility(View.VISIBLE);
            completionAnimation.playAnimation();
            
            // Hide animation after completion
            completionAnimation.postDelayed(() -> {
                completionAnimation.setVisibility(View.GONE);
            }, 3000);
        }
        
        // Helper methods
        private int getDifficultyIcon(String difficulty) {
            switch (difficulty) {
                case "BEGINNER": return R.drawable.ic_difficulty_beginner;
                case "NOVICE": return R.drawable.ic_difficulty_novice;
                case "INTERMEDIATE": return R.drawable.ic_difficulty_intermediate;
                case "ADVANCED": return R.drawable.ic_difficulty_advanced;
                case "EXPERT": return R.drawable.ic_difficulty_expert;
                case "MASTER": return R.drawable.ic_difficulty_master;
                default: return R.drawable.ic_difficulty_unknown;
            }
        }
        
        private int getDifficultyColor(String difficulty) {
            switch (difficulty) {
                case "BEGINNER": return R.color.difficulty_beginner;
                case "NOVICE": return R.color.difficulty_novice;
                case "INTERMEDIATE": return R.color.difficulty_intermediate;
                case "ADVANCED": return R.color.difficulty_advanced;
                case "EXPERT": return R.color.difficulty_expert;
                case "MASTER": return R.color.difficulty_master;
                default: return R.color.difficulty_unknown;
            }
        }
    }
}
