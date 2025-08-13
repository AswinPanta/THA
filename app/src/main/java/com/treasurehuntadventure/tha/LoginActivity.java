package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.treasurehuntadventure.tha.auth.AuthenticationManager;
import com.treasurehuntadventure.tha.auth.AuthValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailEditText, passwordEditText;
    private Button loginButton, registerButton, googleSignInButton;
    private TextView errorTextView, forgotPasswordText;
    private FrameLayout loadingOverlay;
    private CircularProgressIndicator loadingIndicator;

    private AuthenticationManager authManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupAuthManager();
        setupClickListeners();
        setupGoogleSignInLauncher();

        // Check if user is already authenticated
        if (authManager.isUserAuthenticated()) {
            goToMain();
        }
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        errorTextView = findViewById(R.id.errorTextView);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    private void setupAuthManager() {
        authManager = AuthenticationManager.getInstance(this);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        forgotPasswordText.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleGoogleSignInResult(result.getData());
                    } else {
                        hideLoading();
                        showError("Google Sign-In was cancelled");
                    }
                }
        );
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
            return;
        }

        if (!AuthValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        showLoading();
        hideError();

        authManager.signInWithEmailAndPassword(email, password, new AuthenticationManager.AuthResultCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                hideLoading();
                Toast.makeText(LoginActivity.this, "Welcome back, " +
                        (user.getDisplayName() != null ? user.getDisplayName() : "Captain") + "!",
                        Toast.LENGTH_SHORT).show();
                goToMain();
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                showError(AuthValidationUtils.getAuthErrorMessage(exception));
            }
        });
    }

    private void signInWithGoogle() {
        showLoading();
        hideError();

        Intent signInIntent = authManager.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Intent data) {
        authManager.handleGoogleSignInResult(data, new AuthenticationManager.AuthResultCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                hideLoading();
                Toast.makeText(LoginActivity.this, "Welcome, " +
                        (user.getDisplayName() != null ? user.getDisplayName() : "Captain") + "!",
                        Toast.LENGTH_SHORT).show();
                goToMain();
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                showError("Google Sign-In failed: " + AuthValidationUtils.getAuthErrorMessage(exception));
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        if (isFinishing()) return;
        errorTextView.setText(message);
        findViewById(R.id.error_card).setVisibility(View.VISIBLE);
    }

    private void hideError() {
        findViewById(R.id.error_card).setVisibility(View.GONE);
    }

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        googleSignInButton.setEnabled(false);
        registerButton.setEnabled(false);
    }

    private void hideLoading() {
        if (isFinishing()) return;
        loadingOverlay.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        googleSignInButton.setEnabled(true);
        registerButton.setEnabled(true);
    }
}