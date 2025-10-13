package edu.univ.erp.data;

import edu.univ.erp.auth.AuthDAO;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.domain.User;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for AuthDAO.
 * Tests user authentication, creation, updates, and error handling.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthDAOTest extends BaseDAOTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthDAOTest.class);
    
    private AuthDAO authDAO;
    private String testUsername;
    private String testPassword;
    private String testHashedPassword;
    
    @BeforeEach
    void setUpAuthDAO() {
        authDAO = new AuthDAO();
        testUsername = "test_user_" + System.currentTimeMillis();
        testPassword = "test_password_123";
        testHashedPassword = PasswordUtil.hashPassword(testPassword);
        logger.info("Test setup complete for username: {}", testUsername);
    }
    
    @AfterEach
    void cleanUpTestUser() {
        // Clean up test user if it exists
        executeAuthCleanupSQL("DELETE FROM users_auth WHERE username = '" + testUsername + "'");
        logger.info("Cleanup completed for username: {}", testUsername);
    }

    @Test
    @Order(1)
    @DisplayName("Find user by username - existing user")
    void testFindByUsername_ExistingUser() throws SQLException {
        // First create a test user
    Long userId = authDAO.createUser(testUsername, "STUDENT", testHashedPassword);
        assertNotNull(userId, "User creation should return a valid ID");
        
        // Test finding the user
        User user = authDAO.findByUsername(testUsername);
        assertNotNull(user, "Should find existing user");
        assertEquals(testUsername, user.getUsername(), "Username should match");
    assertEquals("STUDENT", user.getRole(), "Role should match");
    assertEquals("ACTIVE", user.getStatus(), "New user should be ACTIVE");
    assertEquals(0, user.getFailedLoginAttempts(), "Failed attempts should start at 0");
        
        logger.info("Successfully found user: {}", user.getUsername());
    }

    @Test
    @Order(2)
    @DisplayName("Find user by username - non-existing user")
    void testFindByUsername_NonExistingUser() throws SQLException {
        String nonExistentUsername = "definitely_does_not_exist_" + System.currentTimeMillis();
        
        User user = authDAO.findByUsername(nonExistentUsername);
        assertNull(user, "Should not find non-existent user");
        
        logger.info("Correctly returned null for non-existent username: {}", nonExistentUsername);
    }

    @Test
    @Order(3)
    @DisplayName("Find user by username - null input")
    void testFindByUsername_NullInput() throws SQLException {
        User user = authDAO.findByUsername(null);
        assertNull(user, "Should handle null username gracefully");
        
        logger.info("Correctly handled null username input");
    }

    @Test
    @Order(4)
    @DisplayName("Find user by username - empty input")
    void testFindByUsername_EmptyInput() throws SQLException {
        User user = authDAO.findByUsername("");
        assertNull(user, "Should handle empty username gracefully");
        
        user = authDAO.findByUsername("   ");
        assertNull(user, "Should handle whitespace-only username gracefully");
        
        logger.info("Correctly handled empty/whitespace username input");
    }

    @Test
    @Order(5)
    @DisplayName("Create user - valid input")
    void testCreateUser_ValidInput() throws SQLException {
    Long userId = authDAO.createUser(testUsername, "INSTRUCTOR", testHashedPassword);
        
        assertNotNull(userId, "Create user should return valid ID");
        assertTrue(userId > 0, "User ID should be positive");
        
        // Verify user was actually created
        User createdUser = authDAO.findByUsername(testUsername);
        assertNotNull(createdUser, "Created user should be findable");
        assertEquals(testUsername, createdUser.getUsername(), "Username should match");
        assertEquals("INSTRUCTOR", createdUser.getRole(), "Role should match");
        
        logger.info("Successfully created user with ID: {}", userId);
    }

    @Test
    @Order(6)
    @DisplayName("Create user - duplicate username")
    void testCreateUser_DuplicateUsername() throws SQLException {
        // Create first user
    Long firstUserId = authDAO.createUser(testUsername, "STUDENT", testHashedPassword);
        assertNotNull(firstUserId, "First user creation should succeed");
        
        // Try to create duplicate
        assertThrows(SQLException.class, () -> {
            authDAO.createUser(testUsername, "INSTRUCTOR", testHashedPassword);
        }, "Creating duplicate username should throw SQLException");
        
        logger.info("Correctly prevented duplicate username creation");
    }

    @Test
    @Order(7)
    @DisplayName("Create user - invalid role")
    void testCreateUser_InvalidRole() throws SQLException {
        assertThrows(SQLException.class, () -> {
            authDAO.createUser(testUsername, "INVALID_ROLE", testHashedPassword);
        }, "Creating user with invalid role should throw SQLException");
        
        logger.info("Correctly rejected invalid role");
    }

    @Test
    @Order(8)
    @DisplayName("Create user - null parameters")
    void testCreateUser_NullParameters() {
        assertThrows(SQLException.class, () -> {
            authDAO.createUser(null, "STUDENT", testHashedPassword);
        }, "Creating user with null username should throw SQLException");
        
        assertThrows(SQLException.class, () -> {
            authDAO.createUser(testUsername, "STUDENT", null);
        }, "Creating user with null password should throw SQLException");
        
        assertThrows(SQLException.class, () -> {
            authDAO.createUser(testUsername, null, testHashedPassword);
        }, "Creating user with null role should throw SQLException");
        
        logger.info("Correctly rejected null parameters");
    }

    @Test
    @Order(9)
    @DisplayName("Update user - valid input")
    void testUpdateUser_ValidInput() throws SQLException {
        // Create test user
    Long userId = authDAO.createUser(testUsername, "STUDENT", testHashedPassword);
        
        // Update user
        String newUsername = testUsername + "_updated";
    authDAO.updateUser(userId, newUsername, "INSTRUCTOR");
        
        // Verify update
        User updatedUser = authDAO.findByUsername(newUsername);
        assertNotNull(updatedUser, "Updated user should be findable by new username");
        assertEquals(newUsername, updatedUser.getUsername(), "Username should be updated");
        assertEquals("INSTRUCTOR", updatedUser.getRole(), "Role should be updated");
        assertEquals(userId, updatedUser.getUserId(), "User ID should remain the same");
        
        // Old username should not exist
        User oldUser = authDAO.findByUsername(testUsername);
        assertNull(oldUser, "Old username should no longer exist");
        
        // Update test username for cleanup
        testUsername = newUsername;
        
        logger.info("Successfully updated user ID: {}", userId);
    }

    @Test
    @Order(10)
    @DisplayName("Update user - non-existent user")
    void testUpdateUser_NonExistentUser() throws SQLException {
        Long nonExistentUserId = 999999L;
        
    // Attempt update; should not throw, but also not create a user
    authDAO.updateUser(nonExistentUserId, "new_username", "STUDENT");
    assertNull(authDAO.findByUsername("new_username"), "Non-existent user should not be updated");
        
        logger.info("Correctly handled non-existent user update");
    }

    @Test
    @Order(11)
    @DisplayName("Reset password - valid input")
    void testResetPassword_ValidInput() throws SQLException {
        // Create test user
    Long userId = authDAO.createUser(testUsername, "STUDENT", testHashedPassword);
        
        // Reset password
        String newPassword = "new_password_456";
        String newHashedPassword = PasswordUtil.hashPassword(newPassword);
    authDAO.resetPassword(userId, newHashedPassword);
        
        // Verify password was changed by attempting authentication with new password
        User user = authDAO.findByUsername(testUsername);
        assertNotNull(user, "User should still exist");
    assertTrue(PasswordUtil.verifyPassword(newPassword, user.getPasswordHash()), 
                "New password should authenticate");
    assertFalse(PasswordUtil.verifyPassword(testPassword, user.getPasswordHash()), 
                "Old password should no longer authenticate");
        
        logger.info("Successfully reset password for user ID: {}", userId);
    }

    @Test
    @Order(12)
    @DisplayName("Reset password - non-existent user")
    void testResetPassword_NonExistentUser() throws SQLException {
        Long nonExistentUserId = 999999L;
        
    // Should not throw, but should have no effect
    authDAO.resetPassword(nonExistentUserId, testHashedPassword);
        
        logger.info("Correctly handled non-existent user password reset");
    }

    @Test
    @Order(13)
    @DisplayName("Unlock account - valid input")
    void testUnlockAccount_ValidInput() throws SQLException {
        // Create test user
    Long userId = authDAO.createUser(testUsername, "STUDENT", testHashedPassword);
        
        // Note: We cannot easily test actual account locking without modifying the schema
        // or having a method to lock accounts, but we can test the unlock method
    // Simulate locked account and failed attempts
    authDAO.updateStatus(userId, "LOCKED");
    authDAO.updateFailedLoginAttempts(userId, 3);
        
    authDAO.unlockAccount(userId);
        
    User user = authDAO.findByUsername(testUsername);
    assertNotNull(user);
    assertEquals("ACTIVE", user.getStatus(), "Status should be ACTIVE after unlock");
    assertEquals(0, user.getFailedLoginAttempts(), "Failed attempts should reset after unlock");
        
        logger.info("Successfully unlocked account for user ID: {}", userId);
    }

    @Test
    @Order(14)
    @DisplayName("Unlock account - non-existent user")
    void testUnlockAccount_NonExistentUser() throws SQLException {
    Long nonExistentUserId = 999999L;
    // Should not throw
    authDAO.unlockAccount(nonExistentUserId);
        
        logger.info("Correctly handled non-existent user unlock");
    }

    @Test
    @Order(15)
    @DisplayName("Password verification - BCrypt integration")
    void testPasswordVerification_BCryptIntegration() throws SQLException {
        // Create user with known password
    Long userId = authDAO.createUser(testUsername, "STUDENT", testHashedPassword);
        
        // Retrieve user and verify password
        User user = authDAO.findByUsername(testUsername);
        assertNotNull(user, "User should exist");
        
        // Test correct password
    assertTrue(PasswordUtil.verifyPassword(testPassword, user.getPasswordHash()),
                "Correct password should authenticate");
        
        // Test incorrect password
    assertFalse(PasswordUtil.verifyPassword("wrong_password", user.getPasswordHash()),
                "Incorrect password should not authenticate");
        
        // Test empty password
    assertFalse(PasswordUtil.verifyPassword("", user.getPasswordHash()),
                "Empty password should not authenticate");
        
        // Test null password
    assertFalse(PasswordUtil.verifyPassword(null, user.getPasswordHash()),
                "Null password should not authenticate");
        
        logger.info("BCrypt password verification working correctly");
    }

    @Test
    @Order(16)
    @DisplayName("User roles - all valid roles")
    void testUserRoles_AllValidRoles() throws SQLException {
        String[] validRoles = {"ADMIN", "INSTRUCTOR", "STUDENT"};
        
        for (int i = 0; i < validRoles.length; i++) {
            String username = testUsername + "_role_" + i;
            String role = validRoles[i];
            
            Long userId = authDAO.createUser(username, role, testHashedPassword);
            assertNotNull(userId, "Should create user with role: " + role);
            
            User user = authDAO.findByUsername(username);
            assertNotNull(user, "Should find user with role: " + role);
            assertEquals(role, user.getRole(), "Role should match: " + role);
            
            // Clean up
            executeAuthCleanupSQL("DELETE FROM users_auth WHERE username = '" + username + "'");
        }
        
        logger.info("All valid user roles work correctly");
    }

    @Test
    @Order(17)
    @DisplayName("Concurrent access - thread safety")
    void testConcurrentAccess_ThreadSafety() throws InterruptedException {
        // This is a basic thread safety test
        // In production, more comprehensive concurrency testing would be needed
        
        Runnable createUserTask = () -> {
            try {
                String threadUsername = testUsername + "_" + Thread.currentThread().getId();
                AuthDAO dao = new AuthDAO();
                Long userId = dao.createUser(threadUsername, "STUDENT", testHashedPassword);
                assertNotNull(userId, "Concurrent user creation should succeed");
                
                User user = dao.findByUsername(threadUsername);
                assertNotNull(user, "Should find concurrently created user");
                
                // Clean up
                executeAuthCleanupSQL("DELETE FROM users_auth WHERE username = '" + threadUsername + "'");
            } catch (SQLException e) {
                fail("Concurrent operation should not fail: " + e.getMessage());
            }
        };
        
        // Run multiple threads concurrently
        Thread[] threads = new Thread[3];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(createUserTask);
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        logger.info("Concurrent access test completed successfully");
    }

    @Test
    @Order(18)
    @DisplayName("Edge cases - special characters in username")
    void testEdgeCases_SpecialCharactersInUsername() throws SQLException {
        // Test usernames with special characters
        String specialUsername = "test_user@domain.com";
        
    Long userId = authDAO.createUser(specialUsername, "STUDENT", testHashedPassword);
        assertNotNull(userId, "Should create user with email-like username");
        
        User user = authDAO.findByUsername(specialUsername);
        assertNotNull(user, "Should find user with special characters");
        assertEquals(specialUsername, user.getUsername(), "Special character username should be preserved");
        
        // Clean up
    executeAuthCleanupSQL("DELETE FROM users_auth WHERE username = '" + specialUsername + "'");
        
        logger.info("Special character username handling works correctly");
    }

    @Test
    @Order(19)
    @DisplayName("Edge cases - long username")
    void testEdgeCases_LongUsername() throws SQLException {
        // Test maximum length username (assuming VARCHAR(50) in database)
        String longUsername = "a".repeat(50); // Exactly 50 characters
        
    Long userId = authDAO.createUser(longUsername, "STUDENT", testHashedPassword);
        assertNotNull(userId, "Should create user with maximum length username");
        
        User user = authDAO.findByUsername(longUsername);
        assertNotNull(user, "Should find user with long username");
        assertEquals(longUsername, user.getUsername(), "Long username should be preserved");
        
        // Clean up
    executeAuthCleanupSQL("DELETE FROM users_auth WHERE username = '" + longUsername + "'");
        
        // Test username that's too long (should fail)
        String tooLongUsername = "a".repeat(51); // 51 characters
        assertThrows(SQLException.class, () -> {
            authDAO.createUser(tooLongUsername, "STUDENT", testHashedPassword);
        }, "Username longer than database limit should fail");
        
        logger.info("Username length handling works correctly");
    }
}