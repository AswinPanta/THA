package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.treasurehuntadventure.tha.chat.ChatSuggestionEngine;
import com.treasurehuntadventure.tha.chat.ContentModerationManager;
import com.treasurehuntadventure.tha.chat.EnhancedChatManager;
import com.treasurehuntadventure.tha.chat.EnhancedChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements EnhancedChatManager.ChatEventListener {
    private static final String TAG = "ChatActivity";

    // UI Components
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton translateButton;
    private ImageButton suggestionsButton;
    private FloatingActionButton channelSelectorFab;
    private ChipGroup suggestionChipGroup;
    private LinearLayout typingIndicatorLayout;
    private TextView typingIndicatorText;
    private ProgressBar translationProgressBar;
    private CardView moderationWarningCard;
    private TextView moderationWarningText;

    // Enhanced chat system
    private EnhancedChatManager chatManager;
    private EnhancedChatAdapter chatAdapter;
    private List<EnhancedChatMessage> messageList = new ArrayList<>();

    // State
    private String currentChannelId = "global_chat";
    private String userId;
    private String userName;
    private boolean isTranslationEnabled = false;
    private boolean isSuggestionsVisible = false;
    private Handler typingHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_chat);

        // Initialize user info
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (userName == null) userName = "Anonymous";
        } else {
            finish();
            return;
        }

        // Initialize UI
        initializeViews();
        setupRecyclerView();
        setupEventListeners();

        // Initialize enhanced chat system
        initializeChatSystem();

        Log.d(TAG, "Enhanced chat activity created for user: " + userName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chatManager != null) {
            chatManager.joinChannel(currentChannelId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatManager != null) {
            chatManager.updateTypingIndicator(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatManager != null) {
            chatManager.cleanup();
        }
    }

    private void initializeViews() {
        // Find views
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        translateButton = findViewById(R.id.translate_button);
        suggestionsButton = findViewById(R.id.suggestions_button);
        channelSelectorFab = findViewById(R.id.channel_selector_fab);
        suggestionChipGroup = findViewById(R.id.suggestion_chip_group);
        typingIndicatorLayout = findViewById(R.id.typing_indicator_layout);
        typingIndicatorText = findViewById(R.id.typing_indicator_text);
        translationProgressBar = findViewById(R.id.translation_progress_bar);
        moderationWarningCard = findViewById(R.id.moderation_warning_card);
        moderationWarningText = findViewById(R.id.moderation_warning_text);

        // Set initial states
        suggestionChipGroup.setVisibility(View.GONE);
        typingIndicatorLayout.setVisibility(View.GONE);
        translationProgressBar.setVisibility(View.GONE);
        moderationWarningCard.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);

        chatAdapter = new EnhancedChatAdapter(messageList, userId, isTranslationEnabled);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupEventListeners() {
        // Send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Translation toggle
        translateButton.setOnClickListener(v -> toggleTranslation());

        // Suggestions toggle
        suggestionsButton.setOnClickListener(v -> toggleSuggestions());

        // Channel selector
        channelSelectorFab.setOnClickListener(v -> showChannelSelector());

        // Message input with typing indicator
        messageEditText.addTextChangedListener(new TextWatcher() {
            private Runnable typingRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chatManager != null) {
                    chatManager.updateTypingIndicator(s.length() > 0);
                }

                // Cancel previous typing timeout
                if (typingRunnable != null) {
                    typingHandler.removeCallbacks(typingRunnable);
                }

                // Set new typing timeout
                if (s.length() > 0) {
                    typingRunnable = () -> {
                        if (chatManager != null) {
                            chatManager.updateTypingIndicator(false);
                        }
                    };
                    typingHandler.postDelayed(typingRunnable, 3000);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeChatSystem() {
        chatManager = new EnhancedChatManager(this);
        chatManager.setChatEventListener(this);

        chatManager.initialize()
                .thenAccept(success -> {
                    if (success) {
                        runOnUiThread(() -> {
                            chatManager.joinChannel(currentChannelId);
                            updateSuggestions();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to initialize chat system", Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) return;

        messageEditText.setText("");

        if (chatManager != null) {
            chatManager.sendMessage(messageText)
                    .thenAccept(success -> {
                        if (!success) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        }
    }

    // Enhanced Chat Manager Event Listeners
    @Override
    public void onMessageReceived(EnhancedChatMessage message) {
        runOnUiThread(() -> {
            messageList.add(message);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.scrollToPosition(messageList.size() - 1);
        });
    }

    @Override
    public void onMessageSent(EnhancedChatMessage message) {
        runOnUiThread(() -> {
            // Message already added when sending, just update status
            chatAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onMessageModerated(String messageId, ContentModerationManager.ModerationResult result) {
        runOnUiThread(() -> {
            if (!result.isApproved()) {
                showModerationWarning("Message blocked: " + result.getReason());
            }
        });
    }

    @Override
    public void onTranslationComplete(String messageId, String translatedText) {
        runOnUiThread(() -> {
            translationProgressBar.setVisibility(View.GONE);
            chatAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onUserJoined(String channelId, String userId, String userName) {
        runOnUiThread(() -> {
            Toast.makeText(this, userName + " joined the chat", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onUserLeft(String channelId, String userId) {
        runOnUiThread(() -> {
            // Handle user leaving
        });
    }

    @Override
    public void onTypingStarted(String channelId, String userId) {
        runOnUiThread(() -> {
            typingIndicatorText.setText(userId + " is typing...");
            typingIndicatorLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onTypingStopped(String channelId, String userId) {
        runOnUiThread(() -> {
            typingIndicatorLayout.setVisibility(View.GONE);
        });
    }

    @Override
    public void onSuggestionsUpdated(List<ChatSuggestionEngine.ChatSuggestion> suggestions) {
        runOnUiThread(() -> {
            updateSuggestionChips(suggestions);
        });
    }

    @Override
    public void onChatError(String error) {
        runOnUiThread(() -> {
            Snackbar.make(chatRecyclerView, "Chat error: " + error, Snackbar.LENGTH_LONG).show();
        });
    }

    // Helper methods
    private void toggleTranslation() {
        isTranslationEnabled = !isTranslationEnabled;
        translateButton.setSelected(isTranslationEnabled);
        chatAdapter.setTranslationEnabled(isTranslationEnabled);

        if (isTranslationEnabled) {
            translationProgressBar.setVisibility(View.VISIBLE);
            translateExistingMessages();
        } else {
            translationProgressBar.setVisibility(View.GONE);
        }

        Toast.makeText(this, isTranslationEnabled ? "Translation enabled" : "Translation disabled",
                Toast.LENGTH_SHORT).show();
    }

    private void toggleSuggestions() {
        isSuggestionsVisible = !isSuggestionsVisible;
        suggestionChipGroup.setVisibility(isSuggestionsVisible ? View.VISIBLE : View.GONE);
        suggestionsButton.setSelected(isSuggestionsVisible);

        if (isSuggestionsVisible) {
            updateSuggestions();
        }
    }

    private void showChannelSelector() {
        String[] channels = {"Global Chat", "Team Chat", "Help Channel", "Local Area"};
        String[] channelIds = {"global_chat", "team_chat", "help_chat", "local_chat"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Chat Channel")
                .setItems(channels, (dialog, which) -> {
                    switchChannel(channelIds[which]);
                })
                .show();
    }

    private void switchChannel(String newChannelId) {
        if (chatManager != null) {
            chatManager.leaveChannel(currentChannelId);
            currentChannelId = newChannelId;
            messageList.clear();
            chatAdapter.notifyDataSetChanged();
            chatManager.joinChannel(currentChannelId);
            updateSuggestions();
        }
    }

    private void updateSuggestions() {
        if (chatManager != null) {
            List<ChatSuggestionEngine.ChatSuggestion> suggestions = chatManager.getCurrentSuggestions();
            updateSuggestionChips(suggestions);
        }
    }

    private void updateSuggestionChips(List<ChatSuggestionEngine.ChatSuggestion> suggestions) {
        suggestionChipGroup.removeAllViews();

        for (ChatSuggestionEngine.ChatSuggestion suggestion : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(suggestion.getText());
            chip.setOnClickListener(v -> {
                if (chatManager != null) {
                    chatManager.useSuggestion(suggestion);
                }
            });
            suggestionChipGroup.addView(chip);
        }
    }

    private void translateExistingMessages() {
        if (chatManager == null) return;

        for (EnhancedChatMessage message : messageList) {
            chatManager.translateMessage(message)
                    .thenAccept(translatedText -> {
                        runOnUiThread(() -> {
                            chatAdapter.notifyDataSetChanged();
                        });
                    });
        }

        translationProgressBar.setVisibility(View.GONE);
    }

    private void showModerationWarning(String warning) {
        moderationWarningText.setText(warning);
        moderationWarningCard.setVisibility(View.VISIBLE);

        // Auto-hide after 5 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            moderationWarningCard.setVisibility(View.GONE);
        }, 5000);
    }

    // Enhanced Chat Adapter
    static class EnhancedChatAdapter extends RecyclerView.Adapter<EnhancedChatAdapter.ViewHolder> {
        private final List<EnhancedChatMessage> messages;
        private final String currentUserId;
        private boolean translationEnabled;

        EnhancedChatAdapter(List<EnhancedChatMessage> messages, String currentUserId, boolean translationEnabled) {
            this.messages = messages;
            this.currentUserId = currentUserId;
            this.translationEnabled = translationEnabled;
        }

        public void setTranslationEnabled(boolean enabled) {
            this.translationEnabled = enabled;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_enhanced_chat_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnhancedChatMessage message = messages.get(position);

            // Set sender name
            holder.senderText.setText(message.getSenderName());

            // Set message text (translated if enabled)
            String displayText = translationEnabled ? message.getDisplayText() : message.getOriginalText();
            holder.messageText.setText(displayText);

            // Set timestamp
            holder.timestampText.setText(formatTimestamp(message.getTimestamp()));

            // Show translation indicator
            holder.translationIndicator.setVisibility(
                    translationEnabled && message.isTranslated() ? View.VISIBLE : View.GONE);

            // Show moderation indicator
            if (message.getModerationResult() != null && !message.getModerationResult().isApproved()) {
                holder.moderationIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.moderationIndicator.setVisibility(View.GONE);
            }

            // Set message alignment based on sender
            boolean isOwnMessage = message.getSenderId().equals(currentUserId);
            holder.messageContainer.setBackgroundResource(
                    isOwnMessage ? R.drawable.chat_bubble_sent : R.drawable.chat_bubble_received);

            // Animate message appearance
            holder.itemView.setAlpha(0f);
            holder.itemView.animate().alpha(1f).setDuration(300).start();
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private String formatTimestamp(long timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView senderText, messageText, timestampText;
            ImageView translationIndicator, moderationIndicator;
            View messageContainer;

            ViewHolder(View itemView) {
                super(itemView);
                senderText = itemView.findViewById(R.id.sender_text);
                messageText = itemView.findViewById(R.id.message_text);
                timestampText = itemView.findViewById(R.id.timestamp_text);
                translationIndicator = itemView.findViewById(R.id.translation_indicator);
                moderationIndicator = itemView.findViewById(R.id.moderation_indicator);
                messageContainer = itemView.findViewById(R.id.message_container);
            }
        }
    }
}