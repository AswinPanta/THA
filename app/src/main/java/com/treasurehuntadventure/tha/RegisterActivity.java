package com.treasurehuntadventure.tha;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.treasurehuntadventure.tha.auth.AuthenticationManager;
import com.treasurehuntadventure.tha.auth.AuthValidationUtils;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText emailEdit, passwordEdit, confirmEdit;
    private TextInputLayout emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private MaterialButton registerBtn;
    private ImageButton backButton;
    private TextView loginLink, errorTextView, passwordStrengthText;
    private FrameLayout loadingOverlay;
    private CircularProgressIndicator loadingIndicator;
    private LinearLayout passwordStrengthLayout;
    private ProgressBar passwordStrengthBar;

    private AuthenticationManager authManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupAuthManager();
        setupClickListeners();
        setupPasswordStrengthIndicator();
    }

    private void initializeViews() {
        emailEdit = findViewById(R.id.registerEmailEditText);
        passwordEdit = findViewById(R.id.registerPasswordEditText);
        confirmEdit = findViewById(R.id.registerConfirmEditText);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        registerBtn = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.backButton);
        loginLink = findViewById(R.id.loginLinkTextView);
        errorTextView = findViewById(R.id.errorTextView);
        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingIndicator = findViewById(R.id.loading_indicator);
        passwordStrengthLayout = findViewById(R.id.passwordStrengthLayout);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);
    }

    private void setupAuthManager() {
        authManager = AuthenticationManager.getInstance(this);
    }

    private void setupClickListeners() {
        registerBtn.setOnClickListener(v -> registerUser());
        backButton.setOnClickListener(v -> finish());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    private void setupPasswordStrengthIndicator() {
        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthLayout.setVisibility(View.GONE);
            return;
        }

        passwordStrengthLayout.setVisibility(View.VISIBLE);

        AuthValidationUtils.PasswordStrength strength = AuthValidationUtils.getPasswordStrength(password);
        int score = AuthValidationUtils.getPasswordStrengthScore(password);

        passwordStrengthBar.setProgress(score);
        passwordStrengthText.setText(strength.getLabel());

        // Update colors based on strength
        int color = Color.parseColor(strength.getColor());
        passwordStrengthBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        passwordStrengthText.setTextColor(color);
    }

    private void registerUser() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String confirm = confirmEdit.getText().toString().trim();

        // Clear previous errors
        clearErrors();

        // Validate inputs
        if (!validateInputs(email, password, confirm)) {
            return;
        }

        showLoading();

        authManager.createUserWithEmailAndPassword(email, password, confirm, new AuthenticationManager.AuthResultCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                hideLoading();
                Toast.makeText(RegisterActivity.this, "Welcome to the crew, " +
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

    private boolean validateInputs(String email, String password, String confirm) {
        boolean isValid = true;

        // Validate email
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            isValid = false;
        } else if (!AuthValidationUtils.isValidEmail(email)) {
            emailInputLayout.setError("Please enter a valid email address");
            isValid = false;
        } else if (!AuthValidationUtils.isValidEmailDomain(email)) {
            emailInputLayout.setError("Please use a valid email provider");
            isValid = false;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordInputLayout.setError("Password is required");
            isValid = false;
        } else if (!AuthValidationUtils.isValidPassword(password)) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        // Validate confirm password
        if (confirm.isEmpty()) {
            confirmPasswordInputLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!AuthValidationUtils.doPasswordsMatch(password, confirm)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        hideError();
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
        registerBtn.setEnabled(false);
    }

    private void hideLoading() {
        if (isFinishing()) return;
        loadingOverlay.setVisibility(View.GONE);
        registerBtn.setEnabled(true);
    }

    private void goToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate back to login activity
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}