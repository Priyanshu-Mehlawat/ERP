package edu.univ.erp.domain;

import java.time.LocalDateTime;

/**
 * User entity representing users in the Auth DB.
 */
public class User {
    private Long userId;
    private String username;
    private String role; // STUDENT, INSTRUCTOR, ADMIN
    private String passwordHash;
    private String status; // ACTIVE, INACTIVE, LOCKED
    private LocalDateTime lastLogin;
    private int failedLoginAttempts;

    public User() {
    }

    public User(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.status = "ACTIVE";
        this.failedLoginAttempts = 0;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
