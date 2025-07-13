package com.treasurehuntadventure.tha;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ChallengesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChallengeAdapter adapter;
    private List<Challenge> challengeList = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenges);
        recyclerView = findViewById(R.id.challengesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChallengeAdapter(challengeList);
        recyclerView.setAdapter(adapter);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchChallenges();
    }

    private void fetchChallenges() {
        FirebaseFirestore.getInstance().collection("challenges")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                challengeList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Challenge c = doc.toObject(Challenge.class);
                    challengeList.add(c);
                }
                adapter.notifyDataSetChanged();
            });
    }

    static class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ViewHolder> {
        private final List<Challenge> challenges;
        ChallengeAdapter(List<Challenge> challenges) { this.challenges = challenges; }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ChallengeAdapter.ViewHolder holder, int position) {
            Challenge c = challenges.get(position);
            holder.desc.setText(c.description);
            holder.progressBar.setMax(c.target);
            holder.progressBar.setProgress(c.progress);
            holder.progressText.setText(c.progress + "/" + c.target);
            holder.completeBtn.setEnabled(!c.completed && c.progress >= c.target);
            holder.completeBtn.setVisibility(c.completed ? View.GONE : View.VISIBLE);
            holder.completeBtn.setOnClickListener(v -> {
                c.completed = true;
                FirebaseFirestore.getInstance().collection("challenges").document(c.id)
                    .update("completed", true);
                holder.completeBtn.setEnabled(false);
                holder.completeBtn.setVisibility(View.GONE);
            });
            holder.itemView.setAlpha(0f);
            holder.itemView.animate().alpha(1f).setDuration(400).start();
        }
        @Override
        public int getItemCount() { return challenges.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView desc, progressText;
            ProgressBar progressBar;
            Button completeBtn;
            ViewHolder(View v) {
                super(v);
                desc = v.findViewById(R.id.challengeDescTextView);
                progressText = v.findViewById(R.id.challengeProgressTextView);
                progressBar = v.findViewById(R.id.challengeProgressBar);
                completeBtn = v.findViewById(R.id.challengeCompleteButton);
            }
        }
    }
} 