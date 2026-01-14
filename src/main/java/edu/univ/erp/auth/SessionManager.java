package edu.univ.erp.auth;

import edu.univ.erp.domain.User;
import edu.univ.erp.util.ConfigUtil;

/**
 * Session manager to track the currently logged-in user.
 * Handles session timeout for security.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private Long studentId;
    private Long instructorId;
    private long lastActivityTime;
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;

    private SessionManager() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public User getCurrentUser() {
        if (currentUser != null && isSessionExpired()) {
            logout();
            return null;
        }
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null && !isSessionExpired();
    }

    public boolean isSessionExpired() {
        return System.currentTimeMillis() - lastActivityTime > SESSION_TIMEOUT_MS;
    }

    public void updateActivity() {
        if (currentUser != null) {
            this.lastActivityTime = System.currentTimeMillis();
        }
    }

    public long getSessionTimeRemaining() {
        long elapsed = System.currentTimeMillis() - lastActivityTime;
        long remaining = SESSION_TIMEOUT_MS - elapsed;
        return Math.max(0, remaining);
    }

    public void setSessionTimeout(long timeoutMs) {
        // This could be enhanced to use ConfigUtil in production
    }

    /**
     * Get the current user's role.
     */
    public String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    /**
     * Get the current user's ID.
     */
    public Long getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    /**
     * Check if current user is admin.
     */
    public boolean isAdmin() {
        return "ADMIN".equals(getCurrentRole());
    }

    /**
     * Check if current user is instructor.
     */
    public boolean isInstructor() {
        return "INSTRUCTOR".equals(getCurrentRole());
    }

    /**
     * Check if current user is student.
     */
    public boolean isStudent() {
        return "STUDENT".equals(getCurrentRole());
    }

    /**
     * Set student ID (loaded from ERP DB).
     */
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    /**
     * Get student ID.
     */
    public Long getStudentId() {
        return studentId;
    }

    /**
     * Set instructor ID (loaded from ERP DB).
     */
    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    /**
     * Get instructor ID.
     */
    public Long getInstructorId() {
        return instructorId;
    }

    /**
     * Logout the current user.
     */
    public void logout() {
        this.currentUser = null;
        this.studentId = null;
        this.instructorId = null;
    }

    @Override
    public String toString() {
        if (currentUser == null) {
            return "No user logged in";
        }
        return "Session{user=" + currentUser.getUsername() + ", role=" + currentUser.getRole() + "}";
    }
}
