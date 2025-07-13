package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailEdit, passwordEdit, confirmEdit;
    private Button registerBtn;
    private TextView loginLink;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailEdit = findViewById(R.id.registerEmailEditText);
        passwordEdit = findViewById(R.id.registerPasswordEditText);
        confirmEdit = findViewById(R.id.registerConfirmEditText);
        registerBtn = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLinkTextView);
        mAuth = FirebaseAuth.getInstance();
        registerBtn.setOnClickListener(v -> registerUser());
        loginLink.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }
    private void registerUser() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String confirm = confirmEdit.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFinishing()) return;
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = mAuth.getCurrentUser().getUid();
                HashMap<String, Object> player = new HashMap<>();
                player.put("email", email);
                player.put("treasuresCaptured", 0);
                player.put("distanceWalked", 0);
                player.put("timePlayed", 0);
                FirebaseFirestore.getInstance().collection("players").document(userId).set(player)
                    .addOnSuccessListener(aVoid -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            } else {
                if (task.getException() instanceof com.google.firebase.FirebaseNetworkException) {
                    Toast.makeText(this, "Registration failed: Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
} 