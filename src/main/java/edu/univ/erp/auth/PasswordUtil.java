package edu.univ.erp.auth;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for password hashing and verification using BCrypt.
 */
public class PasswordUtil {
    
    // BCrypt strength (10 is standard, higher = more secure but slower)
    private static final int BCRYPT_ROUNDS = 10;
    
    // Password complexity requirements
    private static final int MIN_LENGTH = 8;
    private static final int RECOMMENDED_LENGTH = 12;

    /**
     * Hash a plaintext password using BCrypt.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verify a plaintext password against a BCrypt hash.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a password hash needs to be rehashed (e.g., if algorithm changed).
     */
    public static boolean needsRehash(String hashedPassword) {
        // Simple check: if it doesn't start with $2a$, it's not BCrypt
        return !hashedPassword.startsWith("$2a$") && !hashedPassword.startsWith("$2b$");
    }

    /**
     * Validate password complexity requirements.
     * @param password The password to validate
     * @return ValidationResult containing success status and any error messages
     */
    public static ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null) {
            errors.add("Password cannot be null");
            return new ValidationResult(false, errors);
        }
        
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            errors.add("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            errors.add("Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Check if password meets minimum requirements (for backward compatibility).
     * @param password The password to check
     * @return true if password meets minimum requirements
     */
    public static boolean meetsMinimumRequirements(String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }

    /**
     * Get password strength score (0-100).
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int strength = 0;
        
        // Length score (max 40 points)
        if (password.length() >= 8) strength += 20;
        if (password.length() >= 12) strength += 10;
        if (password.length() >= 16) strength += 10;
        
        // Character variety (60 points)
        if (password.matches(".*[a-z].*")) strength += 15;
        if (password.matches(".*[A-Z].*")) strength += 15;
        if (password.matches(".*\\d.*")) strength += 15;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength += 15;
        
        return Math.min(100, strength);
    }

    /**
     * Result class for password validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
