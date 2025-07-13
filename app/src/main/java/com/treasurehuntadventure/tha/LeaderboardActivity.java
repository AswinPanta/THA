package com.treasurehuntadventure.tha;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<Player> playerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        recyclerView = findViewById(R.id.leaderboardRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(playerList);
        recyclerView.setAdapter(adapter);
        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        FirebaseFirestore.getInstance().collection("players")
            .orderBy("treasuresCaptured", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                playerList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Player p = new Player();
                    p.id = doc.getId();
                    p.latitude = doc.getDouble("latitude") != null ? doc.getDouble("latitude") : 0;
                    p.longitude = doc.getDouble("longitude") != null ? doc.getDouble("longitude") : 0;
                    p.lastUpdated = doc.getLong("lastUpdated") != null ? doc.getLong("lastUpdated") : 0;
                    p.treasuresCaptured = doc.getLong("treasuresCaptured") != null ? doc.getLong("treasuresCaptured") : 0;
                    playerList.add(p);
                }
                adapter.notifyDataSetChanged();
            });
    }

    static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private final List<Player> players;
        LeaderboardAdapter(List<Player> players) { this.players = players; }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Player p = players.get(position);
            holder.rank.setText(String.valueOf(position + 1));
            holder.playerId.setText(p.id);
            holder.treasures.setText(String.valueOf(p.treasuresCaptured));
            holder.itemView.setAlpha(0f);
            holder.itemView.animate().alpha(1f).setDuration(400).start();
        }
        @Override
        public int getItemCount() { return players.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView rank, playerId, treasures;
            ViewHolder(View v) {
                super(v);
                rank = v.findViewById(R.id.rankTextView);
                playerId = v.findViewById(R.id.playerIdTextView);
                treasures = v.findViewById(R.id.treasuresTextView);
            }
        }
    }
} 