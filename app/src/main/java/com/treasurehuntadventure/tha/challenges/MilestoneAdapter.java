package com.treasurehuntadventure.tha.challenges;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.treasurehuntadventure.tha.R;

import java.util.List;

/**
 * Adapter for displaying challenge milestones in a horizontal RecyclerView
 */
public class MilestoneAdapter extends RecyclerView.Adapter<MilestoneAdapter.MilestoneViewHolder> {
    
    private final Context context;
    private final List<PersonalizedChallenge.Milestone> milestones;
    
    public MilestoneAdapter(Context context, List<PersonalizedChallenge.Milestone> milestones) {
        this.context = context;
        this.milestones = milestones;
    }
    
    @NonNull
    @Override
    public MilestoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_milestone, parent, false);
        return new MilestoneViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MilestoneViewHolder holder, int position) {
        PersonalizedChallenge.Milestone milestone = milestones.get(position);
        holder.bind(milestone, position);
    }
    
    @Override
    public int getItemCount() {
        return milestones.size();
    }
    
    class MilestoneViewHolder extends RecyclerView.ViewHolder {
        
        private final MaterialCardView milestoneCard;
        private final ImageView milestoneIcon;
        private final MaterialTextView milestoneName;
        private final MaterialTextView milestoneDescription;
        private final View completionIndicator;
        private final View connectionLine;
        
        public MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            
            milestoneCard = itemView.findViewById(R.id.milestone_card);
            milestoneIcon = itemView.findViewById(R.id.milestone_icon);
            milestoneName = itemView.findViewById(R.id.milestone_name);
            milestoneDescription = itemView.findViewById(R.id.milestone_description);
            completionIndicator = itemView.findViewById(R.id.completion_indicator);
            connectionLine = itemView.findViewById(R.id.connection_line);
        }
        
        public void bind(PersonalizedChallenge.Milestone milestone, int position) {
            // Set milestone information
            milestoneName.setText(milestone.getName());
            milestoneDescription.setText(milestone.getDescription());
            
            // Set completion state
            if (milestone.isCompleted()) {
                milestoneCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_background));
                milestoneCard.setStrokeColor(ContextCompat.getColor(context, R.color.success_color));
                milestoneIcon.setImageResource(R.drawable.ic_check_circle);
                milestoneIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_color));
                completionIndicator.setVisibility(View.VISIBLE);
                completionIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.success_color));
            } else {
                milestoneCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
                milestoneCard.setStrokeColor(ContextCompat.getColor(context, R.color.outline));
                milestoneIcon.setImageResource(R.drawable.ic_milestone);
                milestoneIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                completionIndicator.setVisibility(View.GONE);
            }
            
            // Show connection line except for last item
            if (position < milestones.size() - 1) {
                connectionLine.setVisibility(View.VISIBLE);
                if (milestone.isCompleted()) {
                    connectionLine.setBackgroundColor(ContextCompat.getColor(context, R.color.success_color));
                } else {
                    connectionLine.setBackgroundColor(ContextCompat.getColor(context, R.color.outline));
                }
            } else {
                connectionLine.setVisibility(View.GONE);
            }
        }
    }
}
