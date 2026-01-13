package edu.univ.erp.auth;

/**
 * Constants for user roles in the ERP system.
 * Provides centralized definition of role strings to avoid hardcoding throughout the application.
 */
public final class UserRole {
    
    /**
     * Administrator role - full system access
     */
    public static final String ADMIN = "ADMIN";
    
    /**
     * Instructor role - can manage courses and grades
     */
    public static final String INSTRUCTOR = "INSTRUCTOR";
    
    /**
     * Student role - can view grades and enroll in courses
     */
    public static final String STUDENT = "STUDENT";
    
    /**
     * Private constructor to prevent instantiation of utility class
     */
    private UserRole() {
        throw new UnsupportedOperationException("UserRole is a utility class and cannot be instantiated");
    }
}