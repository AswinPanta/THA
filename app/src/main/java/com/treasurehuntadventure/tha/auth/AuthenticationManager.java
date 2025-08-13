package com.treasurehuntadventure.tha.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.treasurehuntadventure.tha.ChallengeSeeder;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized authentication manager for handling all authentication operations
 * including email/password, Google Sign-In, and user session management.
 */
public class AuthenticationManager {
    private static final String TAG = "AuthenticationManager";
    private static final int RC_SIGN_IN = 9001;
    
    private static AuthenticationManager instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final Context context;
    private GoogleSignInClient googleSignInClient;
    
    // Authentication state listeners
    public interface AuthStateListener {
        void onAuthStateChanged(@Nullable FirebaseUser user);
    }
    
    public interface AuthResultCallback {
        void onSuccess(@NonNull FirebaseUser user);
        void onFailure(@NonNull Exception exception);
    }
    
    public interface PasswordResetCallback {
        void onSuccess();
        void onFailure(@NonNull Exception exception);
    }
    
    private AuthenticationManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        
        // Configure Google Sign-In
        // Note: You need to add a web client ID to your google-services.json for Google Sign-In to work
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("845518374234-web-client-id.apps.googleusercontent.com") // TODO: Replace with actual web client ID from Firebase Console
                .requestEmail()
                .build();
        
        this.googleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    public static synchronized AuthenticationManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthenticationManager(context);
        }
        return instance;
    }
    
    /**
     * Get current authenticated user
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Check if user is currently authenticated
     */
    public boolean isUserAuthenticated() {
        return getCurrentUser() != null;
    }
    
    /**
     * Sign in with email and password
     */
    public void signInWithEmailAndPassword(String email, String password, AuthResultCallback callback) {
        if (!AuthValidationUtils.isValidEmail(email)) {
            callback.onFailure(new IllegalArgumentException("Invalid email format"));
            return;
        }
        
        if (!AuthValidationUtils.isValidPassword(password)) {
            callback.onFailure(new IllegalArgumentException("Password must be at least 6 characters"));
            return;
        }
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Email sign-in successful: " + user.getEmail());
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("Authentication succeeded but user is null"));
                        }
                    } else {
                        Log.w(TAG, "Email sign-in failed", task.getException());
                        callback.onFailure(task.getException() != null ? task.getException() : 
                                new Exception("Sign-in failed"));
                    }
                });
    }
    
    /**
     * Create account with email and password
     */
    public void createUserWithEmailAndPassword(String email, String password, String confirmPassword, 
                                             AuthResultCallback callback) {
        // Validate inputs
        if (!AuthValidationUtils.isValidEmail(email)) {
            callback.onFailure(new IllegalArgumentException("Invalid email format"));
            return;
        }
        
        if (!AuthValidationUtils.isValidPassword(password)) {
            callback.onFailure(new IllegalArgumentException("Password must be at least 6 characters"));
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            callback.onFailure(new IllegalArgumentException("Passwords do not match"));
            return;
        }
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Account creation successful: " + user.getEmail());
                            createUserProfile(user, callback);
                        } else {
                            callback.onFailure(new Exception("Account creation succeeded but user is null"));
                        }
                    } else {
                        Log.w(TAG, "Account creation failed", task.getException());
                        callback.onFailure(task.getException() != null ? task.getException() : 
                                new Exception("Account creation failed"));
                    }
                });
    }
    
    /**
     * Get Google Sign-In intent
     */
    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }
    
    /**
     * Handle Google Sign-In result
     */
    public void handleGoogleSignInResult(Intent data, AuthResultCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account, callback);
            } else {
                callback.onFailure(new Exception("Google Sign-In account is null"));
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign-in failed", e);
            callback.onFailure(e);
        }
    }
    
    /**
     * Authenticate with Firebase using Google credentials
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account, AuthResultCallback callback) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
        
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            // Check if this is a new user
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                createUserProfile(user, callback);
                            } else {
                                callback.onSuccess(user);
                            }
                        } else {
                            callback.onFailure(new Exception("Google authentication succeeded but user is null"));
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        callback.onFailure(task.getException() != null ? task.getException() : 
                                new Exception("Google authentication failed"));
                    }
                });
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, PasswordResetCallback callback) {
        if (!AuthValidationUtils.isValidEmail(email)) {
            callback.onFailure(new IllegalArgumentException("Invalid email format"));
            return;
        }
        
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + email);
                        callback.onSuccess();
                    } else {
                        Log.w(TAG, "Failed to send password reset email", task.getException());
                        callback.onFailure(task.getException() != null ? task.getException() : 
                                new Exception("Failed to send password reset email"));
                    }
                });
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        firebaseAuth.signOut();
        googleSignInClient.signOut();
        Log.d(TAG, "User signed out");
    }
    
    /**
     * Add authentication state listener
     */
    public void addAuthStateListener(AuthStateListener listener) {
        firebaseAuth.addAuthStateListener(firebaseUser -> listener.onAuthStateChanged(firebaseUser));
    }
    
    /**
     * Create user profile in Firestore for new users
     */
    private void createUserProfile(FirebaseUser user, AuthResultCallback callback) {
        String userId = user.getUid();
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("email", user.getEmail());
        playerData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "Pirate Captain");
        playerData.put("treasuresCaptured", 0);
        playerData.put("distanceWalked", 0);
        playerData.put("timePlayed", 0);
        playerData.put("level", 1);
        playerData.put("experience", 0);
        playerData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection("players").document(userId).set(playerData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created successfully");
                    // Seed initial challenges for new user
                    ChallengeSeeder.seedChallenges();
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user profile", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Delete user account
     */
    public void deleteAccount(AuthResultCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("No user is currently signed in"));
            return;
        }
        
        String userId = user.getUid();
        
        // Delete user data from Firestore first
        firestore.collection("players").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete Firebase Auth account
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User account deleted successfully");
                                    callback.onSuccess(null);
                                } else {
                                    Log.e(TAG, "Failed to delete user account", task.getException());
                                    callback.onFailure(task.getException() != null ? task.getException() : 
                                            new Exception("Failed to delete account"));
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete user data", e);
                    callback.onFailure(e);
                });
    }
}
