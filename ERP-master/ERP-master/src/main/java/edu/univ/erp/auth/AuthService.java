package edu.univ.erp.auth;

import edu.univ.erp.domain.User;
import edu.univ.erp.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication service for login and password management.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final AuthDAO authDAO;
    private final int maxLoginAttempts;

    public AuthService() {
        this.authDAO = new AuthDAO();
        this.maxLoginAttempts = ConfigUtil.getIntProperty("security.max.login.attempts", 5);
    }

    /**
     * Authenticate user with username and password.
     * Returns User object if successful, null otherwise.
     */
    public AuthResult authenticate(String username, String password) {
        try {
            // Find user
            User user = authDAO.findByUsername(username);
            if (user == null) {
                logger.warn("Login attempt for non-existent user: {}", username);
                return new AuthResult(false, "Incorrect username or password", null);
            }

            // Check if account is locked
            if ("LOCKED".equals(user.getStatus())) {
                logger.warn("Login attempt for locked account: {}", username);
                return new AuthResult(false, "Account is locked. Contact administrator.", null);
            }

            // Check if account is inactive
            if ("INACTIVE".equals(user.getStatus())) {
                logger.warn("Login attempt for inactive account: {}", username);
                return new AuthResult(false, "Account is inactive. Contact administrator.", null);
            }

            // Verify password
            boolean passwordValid = PasswordUtil.verifyPassword(password, user.getPasswordHash());

            if (passwordValid) {
                // Successful login
                authDAO.updateLastLogin(user.getUserId());
                authDAO.updateFailedLoginAttempts(user.getUserId(), 0);
                logger.info("Successful login for user: {}", username);
                return new AuthResult(true, "Login successful", user);
            } else {
                // Failed login - increment failed attempts
                int failedAttempts = user.getFailedLoginAttempts() + 1;
                authDAO.updateFailedLoginAttempts(user.getUserId(), failedAttempts);

                // Lock account if max attempts exceeded
                if (failedAttempts >= maxLoginAttempts) {
                    authDAO.updateStatus(user.getUserId(), "LOCKED");
                    logger.warn("Account locked due to too many failed attempts: {}", username);
                    return new AuthResult(false, "Account locked due to too many failed login attempts.", null);
                }

                logger.warn("Failed login attempt for user: {} (attempt {}/{})", username, failedAttempts, maxLoginAttempts);
                return new AuthResult(false, "Incorrect username or password", null);
            }

        } catch (Exception e) {
            logger.error("Authentication error for user: {}", username, e);
            return new AuthResult(false, "An error occurred during login. Please try again.", null);
        }
    }

    /**
     * Change password for a user.
     */
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        try {
            // Get user
            User user = authDAO.findByUsername(SessionManager.getInstance().getCurrentUser().getUsername());
            if (user == null) {
                return false;
            }

            // Verify current password
            if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
                logger.warn("Failed password change attempt - incorrect current password");
                return false;
            }

            // Hash new password and update
            String newHash = PasswordUtil.hashPassword(newPassword);
            authDAO.changePassword(userId, newHash);
            logger.info("Password changed successfully for user_id: {}", userId);
            return true;

        } catch (Exception e) {
            logger.error("Error changing password for user_id: {}", userId, e);
            return false;
        }
    }

    /**
     * Result of authentication attempt.
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        public AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }
}
