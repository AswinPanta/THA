package com.treasurehuntadventure.tha;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.view.ViewGroup;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        recyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listenForMessages();
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void listenForMessages() {
        FirebaseFirestore.getInstance().collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                for (DocumentChange dc : snap.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                        messageList.add(msg);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                }
            });
    }

    private void sendMessage() {
        String text = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", userId);
        msg.put("text", text);
        msg.put("timestamp", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("chats").add(msg);
        messageEditText.setText("");
    }

    static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private final List<ChatMessage> messages;
        ChatAdapter(List<ChatMessage> messages) { this.messages = messages; }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = View.inflate(parent.getContext(), R.layout.item_chat_message, null);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ChatAdapter.ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.text.setText(msg.text);
            holder.sender.setText(msg.senderId);
            holder.itemView.setAlpha(0f);
            holder.itemView.animate().alpha(1f).setDuration(400).start();
        }
        @Override
        public int getItemCount() { return messages.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView sender, text;
            ViewHolder(View v) {
                super(v);
                sender = v.findViewById(R.id.senderTextView);
                text = v.findViewById(R.id.messageTextView);
            }
        }
    }
} 