package edu.univ.erp.service;

import edu.univ.erp.auth.PermissionException;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.Section;
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
 * Tests for SectionService covering business logic and permissions.
 */
@DisplayName("SectionService Tests")
class SectionServiceTest extends BaseDAOTest {

    private SectionService sectionService;
    private SessionManager sessionManager;

    @BeforeEach
    void setUpService() {
        sectionService = new SectionService();
        sessionManager = SessionManager.getInstance();
    }

    @AfterEach
    void cleanUpSession() {
        sessionManager.logout();
    }

    @Test
    @DisplayName("Should get section by ID")
    void testGetSection() throws SQLException {
        // Act
        Section section = sectionService.get(1L);

        // Assert
        assertNotNull(section, "Should retrieve section by ID");
        assertEquals(1L, section.getSectionId(), "Section ID should match");
    }

    @Test
    @DisplayName("Should list sections by course")
    void testListByCourse() {
        // Act
        List<Section> sections = sectionService.listByCourse(1L, "Fall", 2025);

        // Assert
        assertNotNull(sections, "Section list should not be null");
        assertFalse(sections.isEmpty(), "Should find sections for course 1 in Fall 2025");
    }

    @Test
    @DisplayName("Should list open sections")
    void testListOpenSections() {
        // Act
        List<Section> openSections = sectionService.listOpen("Fall", 2025);

        // Assert
        assertNotNull(openSections, "Open sections list should not be null");
        // Verify all returned sections have available seats
        assertTrue(openSections.stream().allMatch(Section::hasAvailableSeats),
                "All open sections should have available seats");
    }

    @Test
    @DisplayName("Should require instructor role to list instructor sections")
    void testListByInstructorRequiresPermission() {
        // Arrange: No logged-in user
        sessionManager.logout();

        // Act & Assert
        assertThrows(PermissionException.class, () -> {
            sectionService.listByInstructor(1L);
        }, "Should require instructor permission");
    }

    @Test
    @DisplayName("Should list sections for instructor when logged in as instructor")
    void testListByInstructorWithPermission() throws SQLException {
        // Arrange: Mock logged-in instructor
        User instructor = new User();
        instructor.setUserId(2L);
        instructor.setUsername("inst1");
        instructor.setRole("INSTRUCTOR");
        sessionManager.setCurrentUser(instructor);

        // Act
        List<Section> sections = sectionService.listByInstructor(1L);

        // Assert
        assertNotNull(sections, "Should return sections for instructor");
    }

    @Test
    @DisplayName("Should require admin role to list all sections")
    void testListAllRequiresAdmin() {
        // Arrange: Mock logged-in student (not admin)
        User student = new User();
        student.setUserId(3L);
        student.setUsername("stu1");
        student.setRole("STUDENT");
        sessionManager.setCurrentUser(student);

        // Act & Assert
        assertThrows(PermissionException.class, () -> {
            sectionService.listAllSections();
        }, "Should require admin permission to list all sections");
    }

    @Test
    @DisplayName("Should list all sections when logged in as admin")
    void testListAllWithAdminPermission() throws SQLException {
        // Arrange: Mock logged-in admin
        User admin = new User();
        admin.setUserId(1L);
        admin.setUsername("admin1");
        admin.setRole("ADMIN");
        sessionManager.setCurrentUser(admin);

        // Act
        List<Section> sections = sectionService.listAllSections();

        // Assert
        assertNotNull(sections, "Should return all sections for admin");
        assertFalse(sections.isEmpty(), "Should have sections in seed data");
    }

    @Test
    @DisplayName("Should return empty list when no sections match course criteria")
    void testListByCourseNoResults() {
        // Act
        List<Section> sections = sectionService.listByCourse(999L, "Summer", 2099);

        // Assert
        assertNotNull(sections, "Should return empty list, not null");
        assertTrue(sections.isEmpty(), "Should be empty for non-existent course");
    }

    @Test
    @DisplayName("Should handle null course ID gracefully")
    void testListByNullCourse() {
        // Act & Assert - null courseId causes NullPointerException in DAO
        assertThrows(NullPointerException.class, () -> {
            sectionService.listByCourse(null, "Fall", 2025);
        }, "Should throw NullPointerException for null course ID");
    }
}
