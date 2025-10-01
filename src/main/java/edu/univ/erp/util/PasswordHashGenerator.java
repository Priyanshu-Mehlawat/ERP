package edu.univ.erp.util;

import edu.univ.erp.auth.PasswordUtil;

/**
 * Utility to generate BCrypt password hashes for database seeding.
 * Run this class to generate hashes for your seed data.
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        System.out.println("=== BCrypt Password Hash Generator ===\n");
        
        // Generate hashes for default passwords
        String[] passwords = {
            "password123",
            "admin123",
            "instructor123",
            "student123"
        };

        for (String password : passwords) {
            String hash = PasswordUtil.hashPassword(password);
            System.out.println("Password: " + password);
            System.out.println("Hash: " + hash);
            System.out.println();
        }

        System.out.println("\n=== Verification Test ===\n");
        
        // Test verification
        String testPassword = "password123";
        String testHash = PasswordUtil.hashPassword(testPassword);
        
        boolean valid = PasswordUtil.verifyPassword(testPassword, testHash);
        boolean invalid = PasswordUtil.verifyPassword("wrongpassword", testHash);
        
        System.out.println("Original: " + testPassword);
        System.out.println("Hash: " + testHash);
        System.out.println("Correct password verified: " + valid);
        System.out.println("Wrong password rejected: " + !invalid);
        
        System.out.println("\n=== Copy this hash to your seed SQL ===");
        System.out.println("For password 'password123':");
        System.out.println(PasswordUtil.hashPassword("password123"));
    }
}
