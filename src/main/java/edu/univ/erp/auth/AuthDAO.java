package edu.univ.erp.auth;

import edu.univ.erp.data.DatabaseConnection;
import edu.univ.erp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Data Access Object for user authentication.
 */
public class AuthDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuthDAO.class);

    /**
     * Find user by username in Auth DB.
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, role, password_hash, status, failed_login_attempts, last_login " +
                     "FROM users_auth WHERE username = ?";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setStatus(rs.getString("status"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(lastLogin.toLocalDateTime());
                }
                return user;
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
            throw e;
        }
        return null;
    }

    /**
     * Update last login time.
     */
    public void updateLastLogin(Long userId) throws SQLException {
        String sql = "UPDATE users_auth SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last login for user_id: {}", userId, e);
            throw e;
        }
    }

    /**
     * Update failed login attempts.
     */
    public void updateFailedLoginAttempts(Long userId, int attempts) throws SQLException {
        String sql = "UPDATE users_auth SET failed_login_attempts = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, attempts);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating failed login attempts for user_id: {}", userId, e);
            throw e;
        }
    }

    /**
     * Update user status (ACTIVE, INACTIVE, LOCKED).
     */
    public void updateStatus(Long userId, String status) throws SQLException {
        String sql = "UPDATE users_auth SET status = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating status for user_id: {}", userId, e);
            throw e;
        }
    }

    /**
     * Create a new user in Auth DB.
     */
    public Long createUser(String username, String role, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users_auth (username, role, password_hash, status, failed_login_attempts) " +
                     "VALUES (?, ?, ?, 'ACTIVE', 0)";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, username);
            stmt.setString(2, role);
            stmt.setString(3, passwordHash);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error creating user: {}", username, e);
            throw e;
        }
        return null;
    }

    /**
     * Change password for a user.
     */
    public void changePassword(Long userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users_auth SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newPasswordHash);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error changing password for user_id: {}", userId, e);
            throw e;
        }
    }

    /**
     * Get all users from the auth database.
     */
    public java.util.List<User> getAllUsers() throws SQLException {
        String sql = "SELECT user_id, username, role, password_hash, status, failed_login_attempts, last_login " +
                     "FROM users_auth ORDER BY username";
        
        java.util.List<User> users = new java.util.ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setStatus(rs.getString("status"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(lastLogin.toLocalDateTime());
                }
                users.add(user);
            }
        } catch (SQLException e) {
            logger.error("Error getting all users", e);
            throw e;
        }
        return users;
    }

    /**
     * Find user by ID.
     */
    public User findById(Long userId) throws SQLException {
        String sql = "SELECT user_id, username, role, password_hash, status, failed_login_attempts, last_login " +
                     "FROM users_auth WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setStatus(rs.getString("status"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(lastLogin.toLocalDateTime());
                }
                return user;
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID: {}", userId, e);
            throw e;
        }
        return null;
    }

    /**
     * Delete a user by ID.
     */
    public void deleteUser(Long userId) throws SQLException {
        String sql = "DELETE FROM users_auth WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting user with ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Reset password for a user.
     */
    public void resetPassword(Long userId, String newPassword) throws SQLException {
        // Hash the new password using the same utility as during user creation
        String hashedPassword = edu.univ.erp.auth.PasswordUtil.hashPassword(newPassword);
        changePassword(userId, hashedPassword);
    }

    /**
     * Update user information.
     */
    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE users_auth SET username = ?, role = ?, status = ? WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getRole());
            stmt.setString(3, user.getStatus());
            stmt.setLong(4, user.getUserId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating user with ID: {}", user.getUserId(), e);
            throw e;
        }
    }
}
