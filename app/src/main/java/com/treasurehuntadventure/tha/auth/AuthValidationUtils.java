package com.treasurehuntadventure.tha.auth;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * Utility class for authentication input validation and password strength checking
 */
public class AuthValidationUtils {
    
    // Password strength patterns
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    );
    
    private static final Pattern WEAK_PASSWORD_PATTERN = Pattern.compile("^.{6,}$");
    private static final Pattern MEDIUM_PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    
    public enum PasswordStrength {
        WEAK("Weak", "#FF5722"),
        MEDIUM("Medium", "#FF9800"),
        STRONG("Strong", "#4CAF50"),
        VERY_STRONG("Very Strong", "#2E7D32");
        
        private final String label;
        private final String color;
        
        PasswordStrength(String label, String color) {
            this.label = label;
            this.color = color;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * Validate password minimum requirements (at least 6 characters)
     */
    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }
    
    /**
     * Check if password meets strong security requirements
     */
    public static boolean isStrongPassword(String password) {
        return !TextUtils.isEmpty(password) && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Get password strength level
     */
    public static PasswordStrength getPasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) {
            return PasswordStrength.WEAK;
        }
        
        if (password.length() >= 12 && STRONG_PASSWORD_PATTERN.matcher(password).matches() && 
            password.matches(".*[@#$%^&+=].*")) {
            return PasswordStrength.VERY_STRONG;
        } else if (STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            return PasswordStrength.STRONG;
        } else if (MEDIUM_PASSWORD_PATTERN.matcher(password).matches()) {
            return PasswordStrength.MEDIUM;
        } else {
            return PasswordStrength.WEAK;
        }
    }
    
    /**
     * Get password strength score (0-100)
     */
    public static int getPasswordStrengthScore(String password) {
        if (TextUtils.isEmpty(password)) {
            return 0;
        }
        
        int score = 0;
        
        // Length scoring
        if (password.length() >= 6) score += 20;
        if (password.length() >= 8) score += 10;
        if (password.length() >= 12) score += 10;
        
        // Character variety scoring
        if (password.matches(".*[a-z].*")) score += 15; // lowercase
        if (password.matches(".*[A-Z].*")) score += 15; // uppercase
        if (password.matches(".*[0-9].*")) score += 15; // numbers
        if (password.matches(".*[@#$%^&+=!?].*")) score += 15; // special chars
        
        return Math.min(score, 100);
    }
    
    /**
     * Get password requirements text
     */
    public static String getPasswordRequirements() {
        return "Password must contain:\n" +
               "• At least 6 characters\n" +
               "• For strong security: 8+ characters with uppercase, lowercase, numbers, and symbols";
    }
    
    /**
     * Validate display name
     */
    public static boolean isValidDisplayName(String displayName) {
        return !TextUtils.isEmpty(displayName) && 
               displayName.trim().length() >= 2 && 
               displayName.trim().length() <= 30;
    }
    
    /**
     * Sanitize display name
     */
    public static String sanitizeDisplayName(String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            return "Pirate Captain";
        }
        
        String sanitized = displayName.trim().replaceAll("[^a-zA-Z0-9\\s]", "");
        if (sanitized.length() < 2) {
            return "Pirate Captain";
        }
        
        return sanitized.length() > 30 ? sanitized.substring(0, 30) : sanitized;
    }
    
    /**
     * Check if passwords match
     */
    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        return !TextUtils.isEmpty(password) && 
               !TextUtils.isEmpty(confirmPassword) && 
               password.equals(confirmPassword);
    }
    
    /**
     * Get user-friendly error message for authentication exceptions
     */
    public static String getAuthErrorMessage(Exception exception) {
        if (exception == null) {
            return "An unknown error occurred";
        }
        
        String message = exception.getMessage();
        if (message == null) {
            return "An unknown error occurred";
        }
        
        // Firebase Auth error codes
        if (message.contains("ERROR_INVALID_EMAIL")) {
            return "The email address is not valid";
        } else if (message.contains("ERROR_USER_NOT_FOUND")) {
            return "No account found with this email address";
        } else if (message.contains("ERROR_WRONG_PASSWORD")) {
            return "Incorrect password. Please try again";
        } else if (message.contains("ERROR_EMAIL_ALREADY_IN_USE")) {
            return "An account with this email already exists";
        } else if (message.contains("ERROR_WEAK_PASSWORD")) {
            return "Password is too weak. Please choose a stronger password";
        } else if (message.contains("ERROR_USER_DISABLED")) {
            return "This account has been disabled";
        } else if (message.contains("ERROR_TOO_MANY_REQUESTS")) {
            return "Too many failed attempts. Please try again later";
        } else if (message.contains("ERROR_NETWORK_REQUEST_FAILED")) {
            return "Network error. Please check your internet connection";
        } else if (message.contains("ERROR_REQUIRES_RECENT_LOGIN")) {
            return "This operation requires recent authentication. Please sign in again";
        } else {
            return message;
        }
    }
    
    /**
     * Validate that email domain is not from temporary email services
     */
    public static boolean isValidEmailDomain(String email) {
        if (!isValidEmail(email)) {
            return false;
        }
        
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        
        // List of common temporary email domains to block
        String[] tempDomains = {
            "10minutemail.com", "guerrillamail.com", "mailinator.com",
            "tempmail.org", "throwaway.email", "temp-mail.org"
        };
        
        for (String tempDomain : tempDomains) {
            if (domain.equals(tempDomain)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if email is from a common provider (for UI hints)
     */
    public static boolean isCommonEmailProvider(String email) {
        if (!isValidEmail(email)) {
            return false;
        }
        
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        
        String[] commonProviders = {
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "icloud.com", "aol.com", "protonmail.com"
        };
        
        for (String provider : commonProviders) {
            if (domain.equals(provider)) {
                return true;
            }
        }
        
        return false;
    }
}
