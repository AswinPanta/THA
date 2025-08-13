package com.treasurehuntadventure.tha.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.treasurehuntadventure.tha.LoginActivity;
import com.treasurehuntadventure.tha.MainActivity;

/**
 * Utility class for managing authentication state across the application
 */
public class AuthStateManager {
    private static final String TAG = "AuthStateManager";
    
    private static AuthStateManager instance;
    private final FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    
    public interface AuthStateCallback {
        void onUserSignedIn(FirebaseUser user);
        void onUserSignedOut();
    }
    
    private AuthStateManager() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public static synchronized AuthStateManager getInstance() {
        if (instance == null) {
            instance = new AuthStateManager();
        }
        return instance;
    }
    
    /**
     * Check if user is currently authenticated
     */
    public boolean isUserAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    /**
     * Get current authenticated user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Set up authentication state listener
     */
    public void setupAuthStateListener(AuthStateCallback callback) {
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Log.d(TAG, "User signed in: " + user.getEmail());
                callback.onUserSignedIn(user);
            } else {
                Log.d(TAG, "User signed out");
                callback.onUserSignedOut();
            }
        };
        
        firebaseAuth.addAuthStateListener(authStateListener);
    }
    
    /**
     * Remove authentication state listener
     */
    public void removeAuthStateListener() {
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
            authStateListener = null;
        }
    }
    
    /**
     * Check authentication and redirect to appropriate activity
     */
    public void checkAuthAndRedirect(Activity currentActivity) {
        FirebaseUser user = getCurrentUser();
        
        if (user != null) {
            // User is authenticated
            if (!(currentActivity instanceof MainActivity)) {
                // Redirect to main activity if not already there
                Intent intent = new Intent(currentActivity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                currentActivity.startActivity(intent);
                currentActivity.finish();
            }
        } else {
            // User is not authenticated
            if (!(currentActivity instanceof LoginActivity)) {
                // Redirect to login activity if not already there
                Intent intent = new Intent(currentActivity, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                currentActivity.startActivity(intent);
                currentActivity.finish();
            }
        }
    }
    
    /**
     * Require authentication for an activity
     * Returns true if user is authenticated, false if redirected to login
     */
    public boolean requireAuthentication(Activity activity) {
        if (!isUserAuthenticated()) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return false;
        }
        return true;
    }
    
    /**
     * Sign out user and redirect to login
     */
    public void signOutAndRedirect(Context context) {
        AuthenticationManager.getInstance(context).signOut();
        
        if (context instanceof Activity) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            ((Activity) context).finish();
        }
    }
    
    /**
     * Get user display name or default
     */
    public String getUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }
            
            String email = user.getEmail();
            if (email != null) {
                // Extract name from email (before @)
                int atIndex = email.indexOf("@");
                if (atIndex > 0) {
                    return email.substring(0, atIndex);
                }
            }
        }
        return "Captain";
    }
    
    /**
     * Get user email or default
     */
    public String getUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : "unknown@treasurehunt.com";
    }
    
    /**
     * Check if user email is verified
     */
    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }
    
    /**
     * Send email verification
     */
    public void sendEmailVerification(Runnable onSuccess, Runnable onFailure) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email verification sent");
                            if (onSuccess != null) onSuccess.run();
                        } else {
                            Log.e(TAG, "Failed to send email verification", task.getException());
                            if (onFailure != null) onFailure.run();
                        }
                    });
        } else {
            if (onFailure != null) onFailure.run();
        }
    }
}
