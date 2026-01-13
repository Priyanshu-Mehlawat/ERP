package edu.univ.erp.auth;

import edu.univ.erp.domain.User;

/**
 * Session manager to track the currently logged-in user.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private Long studentId; // For student role
    private Long instructorId; // For instructor role

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Set the current logged-in user.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Get the current logged-in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if a user is logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
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
