package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.treasurehuntadventure.tha.auth.AuthenticationManager;
import com.treasurehuntadventure.tha.auth.AuthValidationUtils;

/**
 * Activity for handling password reset functionality
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    
    private TextInputEditText emailEditText;
    private MaterialButton sendResetButton;
    private ImageButton backButton;
    private TextView backToLoginText, errorTextView, successTextView;
    private FrameLayout loadingOverlay;
    private CircularProgressIndicator loadingIndicator;
    
    private AuthenticationManager authManager;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        initializeViews();
        setupAuthManager();
        setupClickListeners();
    }
    
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        sendResetButton = findViewById(R.id.sendResetButton);
        backButton = findViewById(R.id.backButton);
        backToLoginText = findViewById(R.id.backToLoginText);
        errorTextView = findViewById(R.id.errorTextView);
        successTextView = findViewById(R.id.successTextView);
        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }
    
    private void setupAuthManager() {
        authManager = AuthenticationManager.getInstance(this);
    }
    
    private void setupClickListeners() {
        sendResetButton.setOnClickListener(v -> sendPasswordResetEmail());
        backButton.setOnClickListener(v -> finish());
        backToLoginText.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    
    private void sendPasswordResetEmail() {
        String email = emailEditText.getText().toString().trim();
        
        // Validate email input
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address");
            return;
        }
        
        if (!AuthValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }
        
        showLoading();
        hideMessages();
        
        authManager.sendPasswordResetEmail(email, new AuthenticationManager.PasswordResetCallback() {
            @Override
            public void onSuccess() {
                hideLoading();
                showSuccess("Password reset email sent! Check your inbox and follow the instructions to reset your password.");
                
                // Clear the email field
                emailEditText.setText("");
                
                // Optionally navigate back to login after a delay
                emailEditText.postDelayed(() -> {
                    startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                    finish();
                }, 3000);
            }
            
            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                String errorMessage = AuthValidationUtils.getAuthErrorMessage(exception);
                
                // Provide more specific error messages for password reset
                if (exception.getMessage() != null) {
                    if (exception.getMessage().contains("ERROR_USER_NOT_FOUND")) {
                        errorMessage = "No account found with this email address. Please check your email or create a new account.";
                    } else if (exception.getMessage().contains("ERROR_INVALID_EMAIL")) {
                        errorMessage = "The email address format is invalid. Please enter a valid email.";
                    } else if (exception.getMessage().contains("ERROR_TOO_MANY_REQUESTS")) {
                        errorMessage = "Too many password reset requests. Please wait a few minutes before trying again.";
                    }
                }
                
                showError(errorMessage);
            }
        });
    }
    
    private void showError(String message) {
        if (isFinishing()) return;
        errorTextView.setText(message);
        findViewById(R.id.error_card).setVisibility(View.VISIBLE);
        findViewById(R.id.success_card).setVisibility(View.GONE);
    }
    
    private void showSuccess(String message) {
        if (isFinishing()) return;
        successTextView.setText(message);
        findViewById(R.id.success_card).setVisibility(View.VISIBLE);
        findViewById(R.id.error_card).setVisibility(View.GONE);
    }
    
    private void hideMessages() {
        findViewById(R.id.error_card).setVisibility(View.GONE);
        findViewById(R.id.success_card).setVisibility(View.GONE);
    }
    
    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        sendResetButton.setEnabled(false);
    }
    
    private void hideLoading() {
        if (isFinishing()) return;
        loadingOverlay.setVisibility(View.GONE);
        sendResetButton.setEnabled(true);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate back to login activity
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
