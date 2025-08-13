package com.treasurehuntadventure.tha;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.treasurehuntadventure.tha.auth.AuthenticationManager;
import com.treasurehuntadventure.tha.auth.AuthValidationUtils;

/**
 * Activity for managing user account settings including email updates, password changes, and account deletion
 */
public class AccountManagementActivity extends AppCompatActivity {
    
    private TextView userEmailText, errorTextView, successTextView;
    private MaterialButton updateEmailButton, changePasswordButton, signOutButton, deleteAccountButton;
    private ImageButton backButton;
    private FrameLayout loadingOverlay;
    private CircularProgressIndicator loadingIndicator;
    
    private AuthenticationManager authManager;
    private FirebaseUser currentUser;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_management);
        
        initializeViews();
        setupAuthManager();
        setupClickListeners();
        loadUserInfo();
    }
    
    private void initializeViews() {
        userEmailText = findViewById(R.id.userEmailText);
        errorTextView = findViewById(R.id.errorTextView);
        successTextView = findViewById(R.id.successTextView);
        updateEmailButton = findViewById(R.id.updateEmailButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        signOutButton = findViewById(R.id.signOutButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        backButton = findViewById(R.id.backButton);
        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }
    
    private void setupAuthManager() {
        authManager = AuthenticationManager.getInstance(this);
        currentUser = authManager.getCurrentUser();
        
        if (currentUser == null) {
            // User not authenticated, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        updateEmailButton.setOnClickListener(v -> showUpdateEmailDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        signOutButton.setOnClickListener(v -> showSignOutConfirmation());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountConfirmation());
    }
    
    private void loadUserInfo() {
        if (currentUser != null) {
            userEmailText.setText(currentUser.getEmail());
        }
    }
    
    private void showUpdateEmailDialog() {
        // Create dialog view programmatically since we don't have the layout yet
        View dialogView = createUpdateEmailDialogView();
        TextInputEditText newEmailEdit = dialogView.findViewById(R.id.newEmailEditText);
        TextInputEditText passwordEdit = dialogView.findViewById(R.id.passwordEditText);
        TextInputLayout newEmailLayout = dialogView.findViewById(R.id.newEmailInputLayout);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.passwordInputLayout);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Email Address")
                .setMessage("Enter your new email address and current password to confirm the change.")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newEmail = newEmailEdit.getText().toString().trim();
                    String password = passwordEdit.getText().toString().trim();
                    
                    // Clear previous errors
                    newEmailLayout.setError(null);
                    passwordLayout.setError(null);
                    
                    // Validate inputs
                    boolean isValid = true;
                    if (newEmail.isEmpty()) {
                        newEmailLayout.setError("New email is required");
                        isValid = false;
                    } else if (!AuthValidationUtils.isValidEmail(newEmail)) {
                        newEmailLayout.setError("Please enter a valid email address");
                        isValid = false;
                    }
                    
                    if (password.isEmpty()) {
                        passwordLayout.setError("Current password is required");
                        isValid = false;
                    }
                    
                    if (isValid) {
                        updateEmail(newEmail, password);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showChangePasswordDialog() {
        View dialogView = createChangePasswordDialogView();
        TextInputEditText currentPasswordEdit = dialogView.findViewById(R.id.currentPasswordEditText);
        TextInputEditText newPasswordEdit = dialogView.findViewById(R.id.newPasswordEditText);
        TextInputEditText confirmPasswordEdit = dialogView.findViewById(R.id.confirmPasswordEditText);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setMessage("Enter your current password and choose a new one.")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordEdit.getText().toString().trim();
                    String newPassword = newPasswordEdit.getText().toString().trim();
                    String confirmPassword = confirmPasswordEdit.getText().toString().trim();
                    
                    if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                        changePassword(currentPassword, newPassword);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showSignOutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of your account?")
                .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDeleteAccountConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone. All your progress, treasures, and data will be permanently deleted.\n\nAre you absolutely sure?")
                .setPositiveButton("Delete Forever", (dialog, which) -> showFinalDeleteConfirmation())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showFinalDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Final Confirmation")
                .setMessage("This will permanently delete your account and all data. This action cannot be undone.")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateEmail(String newEmail, String password) {
        showLoading();
        hideMessages();
        
        // Re-authenticate user first, then update email
        authManager.signInWithEmailAndPassword(currentUser.getEmail(), password, new AuthenticationManager.AuthResultCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                // Now update the email
                user.updateEmail(newEmail)
                        .addOnCompleteListener(task -> {
                            hideLoading();
                            if (task.isSuccessful()) {
                                showSuccess("Email address updated successfully!");
                                userEmailText.setText(newEmail);
                            } else {
                                showError("Failed to update email: " + AuthValidationUtils.getAuthErrorMessage(task.getException()));
                            }
                        });
            }
            
            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                showError("Authentication failed: " + AuthValidationUtils.getAuthErrorMessage(exception));
            }
        });
    }
    
    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            showError("Current password is required");
            return false;
        }
        
        if (newPassword.isEmpty()) {
            showError("New password is required");
            return false;
        }
        
        if (!AuthValidationUtils.isValidPassword(newPassword)) {
            showError("New password must be at least 6 characters");
            return false;
        }
        
        if (!AuthValidationUtils.doPasswordsMatch(newPassword, confirmPassword)) {
            showError("New passwords do not match");
            return false;
        }
        
        return true;
    }
    
    private void changePassword(String currentPassword, String newPassword) {
        showLoading();
        hideMessages();
        
        // Re-authenticate user first, then update password
        authManager.signInWithEmailAndPassword(currentUser.getEmail(), currentPassword, new AuthenticationManager.AuthResultCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                // Now update the password
                user.updatePassword(newPassword)
                        .addOnCompleteListener(task -> {
                            hideLoading();
                            if (task.isSuccessful()) {
                                showSuccess("Password changed successfully!");
                            } else {
                                showError("Failed to change password: " + AuthValidationUtils.getAuthErrorMessage(task.getException()));
                            }
                        });
            }
            
            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                showError("Authentication failed: " + AuthValidationUtils.getAuthErrorMessage(exception));
            }
        });
    }
    
    private void signOut() {
        authManager.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void deleteAccount() {
        showLoading();
        hideMessages();
        
        authManager.deleteAccount(new AuthenticationManager.AuthResultCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                hideLoading();
                Toast.makeText(AccountManagementActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                
                // Navigate to login screen
                Intent intent = new Intent(AccountManagementActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                showError("Failed to delete account: " + AuthValidationUtils.getAuthErrorMessage(exception));
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
        updateEmailButton.setEnabled(false);
        changePasswordButton.setEnabled(false);
        signOutButton.setEnabled(false);
        deleteAccountButton.setEnabled(false);
    }
    
    private void hideLoading() {
        if (isFinishing()) return;
        loadingOverlay.setVisibility(View.GONE);
        updateEmailButton.setEnabled(true);
        changePasswordButton.setEnabled(true);
        signOutButton.setEnabled(true);
        deleteAccountButton.setEnabled(true);
    }

    private View createUpdateEmailDialogView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        TextInputLayout newEmailLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        newEmailLayout.setHint("New Email Address");
        newEmailLayout.setStartIconDrawable(R.drawable.ic_email);
        TextInputEditText newEmailEdit = new TextInputEditText(newEmailLayout.getContext());
        newEmailEdit.setId(R.id.newEmailEditText);
        newEmailEdit.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        newEmailLayout.addView(newEmailEdit);
        newEmailLayout.setId(R.id.newEmailInputLayout);

        TextInputLayout passwordLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        passwordLayout.setHint("Current Password");
        passwordLayout.setStartIconDrawable(R.drawable.ic_lock);
        passwordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText passwordEdit = new TextInputEditText(passwordLayout.getContext());
        passwordEdit.setId(R.id.passwordEditText);
        passwordEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordLayout.addView(passwordEdit);
        passwordLayout.setId(R.id.passwordInputLayout);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 32);

        layout.addView(newEmailLayout, params);
        layout.addView(passwordLayout, params);

        return layout;
    }

    private View createChangePasswordDialogView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        TextInputLayout currentPasswordLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        currentPasswordLayout.setHint("Current Password");
        currentPasswordLayout.setStartIconDrawable(R.drawable.ic_lock);
        currentPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText currentPasswordEdit = new TextInputEditText(currentPasswordLayout.getContext());
        currentPasswordEdit.setId(R.id.currentPasswordEditText);
        currentPasswordEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordLayout.addView(currentPasswordEdit);

        TextInputLayout newPasswordLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        newPasswordLayout.setHint("New Password");
        newPasswordLayout.setStartIconDrawable(R.drawable.ic_lock);
        newPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText newPasswordEdit = new TextInputEditText(newPasswordLayout.getContext());
        newPasswordEdit.setId(R.id.newPasswordEditText);
        newPasswordEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordLayout.addView(newPasswordEdit);

        TextInputLayout confirmPasswordLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        confirmPasswordLayout.setHint("Confirm New Password");
        confirmPasswordLayout.setStartIconDrawable(R.drawable.ic_lock);
        confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText confirmPasswordEdit = new TextInputEditText(confirmPasswordLayout.getContext());
        confirmPasswordEdit.setId(R.id.confirmPasswordEditText);
        confirmPasswordEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordLayout.addView(confirmPasswordEdit);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 32);

        layout.addView(currentPasswordLayout, params);
        layout.addView(newPasswordLayout, params);
        layout.addView(confirmPasswordLayout, params);

        return layout;
    }
}
