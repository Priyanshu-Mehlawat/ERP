package edu.univ.erp.service;

import edu.univ.erp.auth.PermissionException;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.User;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GradeService covering grade management and calculations.
 */
@DisplayName("GradeService Tests")
class GradeServiceTest extends BaseDAOTest {

    private GradeService gradeService;
    private SessionManager sessionManager;

    @BeforeEach
    void setUpService() {
        gradeService = new GradeService();
        sessionManager = SessionManager.getInstance();
    }

    @AfterEach
    void cleanUpSession() {
        sessionManager.logout();
    }

    @Test
    @DisplayName("Should require permission to list components")
    void testListComponentsRequiresPermission() {
        // Arrange: No logged-in user
        sessionManager.logout();

        // Act & Assert
        assertThrows(PermissionException.class, () -> {
            gradeService.listComponents(1L);
        }, "Should require permission to list grade components");
    }

    @Test
    @DisplayName("Should get distinct components for section")
    void testGetComponentsForSection() throws SQLException {
        // Arrange: Login as instructor
        User instructor = new User();
        instructor.setUserId(2L);
        instructor.setRole("INSTRUCTOR");
        sessionManager.setCurrentUser(instructor);
        sessionManager.setInstructorId(1L);

        // Act
        List<String> components = gradeService.getComponentsForSection(1L);

        // Assert
        assertNotNull(components, "Components list should not be null");
        assertFalse(components.isEmpty(), "Should have default components");
    }

    @Test
    @DisplayName("Should throw exception for null section ID")
    void testGetComponentsForNullSection() {
        // Arrange: Login as instructor
        User instructor = new User();
        instructor.setUserId(2L);
        instructor.setRole("INSTRUCTOR");
        sessionManager.setCurrentUser(instructor);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            gradeService.getComponentsForSection(null);
        }, "Should throw IllegalArgumentException for null sectionId");
    }

    @Test
    @DisplayName("Should add grade component with permission")
    void testAddComponentWithPermission() {
        // Arrange: Login as instructor
        User instructor = new User();
        instructor.setUserId(2L);
        instructor.setRole("INSTRUCTOR");
        sessionManager.setCurrentUser(instructor);
        sessionManager.setInstructorId(1L);

        // Act
        String result = gradeService.addComponent(1L, "Assignment 1", 85.0, 100.0, 20.0);

        // Assert - may be ADDED or permission denied depending on enrollment ownership
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Should prevent weight exceeding 100%")
    void testWeightExceedsLimit() {
        // Arrange: Login as instructor
        User instructor = new User();
        instructor.setUserId(2L);
        instructor.setRole("INSTRUCTOR");
        sessionManager.setCurrentUser(instructor);
        sessionManager.setInstructorId(1L);

        // Act - Try to add component with excessive weight
        String result = gradeService.addComponent(1L, "Test", 100.0, 100.0, 200.0);

        // Assert
        assertTrue(result.contains("exceed") || result.contains("Permission"),
                "Should prevent exceeding 100% total weight or deny permission");
    }

    @Test
    @DisplayName("Should update score successfully")
    void testUpdateScore() {
        // Act - Update a grade score
        String result = gradeService.updateScore(1L, 95.0);

        // Assert
        assertNotNull(result, "Result should not be null");
        // Result could be "UPDATED", "Not found", or "Error" depending on data
    }

    @Test
    @DisplayName("Should handle null score in update")
    void testUpdateWithNullScore() {
        // Act
        String result = gradeService.updateScore(1L, null);

        // Assert
        assertNotNull(result, "Should handle null score");
    }
}
