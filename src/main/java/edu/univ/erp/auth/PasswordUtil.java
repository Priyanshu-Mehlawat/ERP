package edu.univ.erp.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility for password hashing and verification using BCrypt.
 */
public class PasswordUtil {
    
    // BCrypt strength (10 is standard, higher = more secure but slower)
    private static final int BCRYPT_ROUNDS = 10;

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
}
