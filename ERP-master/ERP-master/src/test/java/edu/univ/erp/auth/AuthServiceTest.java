package edu.univ.erp.auth;

import edu.univ.erp.domain.User;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService Tests")
class AuthServiceTest extends BaseDAOTest {
    private AuthService authService;
    private AuthDAO authDAO;
    private Long testUserId;
    private static final String TEST_USERNAME = "auth_svc_test";
    private static final String TEST_PASSWORD = "TestPassword123!";

    @BeforeEach
    void setupTestData() throws SQLException {
        authService = new AuthService();
        authDAO = new AuthDAO();
        
        // Create test user with known password
        String passwordHash = PasswordUtil.hashPassword(TEST_PASSWORD);
        testUserId = authDAO.createUser(TEST_USERNAME, "STUDENT", passwordHash);
    }

    @AfterEach
    void cleanupTestData() {
        if (testUserId != null) {
            executeAuthCleanupSQL("DELETE FROM users_auth WHERE user_id = " + testUserId);
        }
    }

    @Test
    @DisplayName("Successful authentication with correct credentials")
    void testAuthenticateSuccess() throws SQLException {
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, TEST_PASSWORD);
        
        assertTrue(result.isSuccess());
        assertEquals("Login successful", result.getMessage());
        assertNotNull(result.getUser());
        assertEquals(TEST_USERNAME, result.getUser().getUsername());
        
        // Verify lastLogin was updated
        User user = authDAO.findById(testUserId);
        assertNotNull(user.getLastLogin());
        assertTrue(user.getLastLogin().isAfter(LocalDateTime.now().minusMinutes(1)));
        
        // Verify failed attempts reset to 0
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Failed authentication with wrong password")
    void testAuthenticateWrongPassword() throws SQLException {
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, "WrongPassword");
        
        assertFalse(result.isSuccess());
        assertEquals("Incorrect username or password", result.getMessage());
        assertNull(result.getUser());
        
        // Verify failed attempts incremented
        User user = authDAO.findById(testUserId);
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Failed authentication with non-existent username")
    void testAuthenticateNonExistentUser() {
        AuthService.AuthResult result = authService.authenticate("nonexistent_user", TEST_PASSWORD);
        
        assertFalse(result.isSuccess());
        assertEquals("Incorrect username or password", result.getMessage());
        assertNull(result.getUser());
    }

    @Test
    @DisplayName("Multiple failed attempts locks account")
    void testAccountLockAfterMaxAttempts() throws SQLException {
        // Make 5 failed attempts (maxLoginAttempts from config is 5)
        for (int i = 1; i <= 5; i++) {
            AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, "WrongPassword");
            assertFalse(result.isSuccess());
            
            if (i < 5) {
                assertEquals("Incorrect username or password", result.getMessage());
            } else {
                assertEquals("Account locked due to too many failed login attempts.", result.getMessage());
            }
        }
        
        // Verify account is locked
        User user = authDAO.findById(testUserId);
        assertEquals("LOCKED", user.getStatus());
        assertEquals(5, user.getFailedLoginAttempts());
        
        // Try correct password now - should still fail with locked message
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, TEST_PASSWORD);
        assertFalse(result.isSuccess());
        assertEquals("Account is locked. Contact administrator.", result.getMessage());
    }

    @Test
    @DisplayName("Cannot authenticate with locked account even with correct password")
    void testAuthenticateLockedAccount() throws SQLException {
        // Lock the account manually
        authDAO.updateStatus(testUserId, "LOCKED");
        
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, TEST_PASSWORD);
        
        assertFalse(result.isSuccess());
        assertEquals("Account is locked. Contact administrator.", result.getMessage());
        assertNull(result.getUser());
    }

    @Test
    @DisplayName("Cannot authenticate with inactive account")
    void testAuthenticateInactiveAccount() throws SQLException {
        // Set account to inactive
        authDAO.updateStatus(testUserId, "INACTIVE");
        
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, TEST_PASSWORD);
        
        assertFalse(result.isSuccess());
        assertEquals("Account is inactive. Contact administrator.", result.getMessage());
        assertNull(result.getUser());
    }

    @Test
    @DisplayName("Failed attempts counter increments correctly")
    void testFailedAttemptsIncrement() throws SQLException {
        // Make 3 failed attempts
        for (int i = 1; i <= 3; i++) {
            authService.authenticate(TEST_USERNAME, "WrongPassword");
            User user = authDAO.findById(testUserId);
            assertEquals(i, user.getFailedLoginAttempts());
        }
        
        // Successful login should reset counter
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(result.isSuccess());
        
        User user = authDAO.findById(testUserId);
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("LastLogin timestamp updates only on successful login")
    void testLastLoginUpdate() throws SQLException {
        User userBefore = authDAO.findById(testUserId);
        LocalDateTime lastLoginBefore = userBefore.getLastLogin();
        
        // Failed login should not update lastLogin
        authService.authenticate(TEST_USERNAME, "WrongPassword");
        User userAfterFailed = authDAO.findById(testUserId);
        assertEquals(lastLoginBefore, userAfterFailed.getLastLogin());
        
        // Wait a moment to ensure timestamp difference
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        // Successful login should update lastLogin
        AuthService.AuthResult result = authService.authenticate(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(result.isSuccess());
        
        User userAfterSuccess = authDAO.findById(testUserId);
        assertNotNull(userAfterSuccess.getLastLogin());
        if (lastLoginBefore != null) {
            assertTrue(userAfterSuccess.getLastLogin().isAfter(lastLoginBefore) || 
                      userAfterSuccess.getLastLogin().isEqual(lastLoginBefore));
        }
    }
}
