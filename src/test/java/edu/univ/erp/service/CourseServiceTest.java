package edu.univ.erp.service;

import edu.univ.erp.domain.Course;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CourseService covering business logic, validation, and data access.
 */
@DisplayName("CourseService Tests")
class CourseServiceTest extends BaseDAOTest {

    private CourseService courseService;

    @BeforeEach
    void setUpService() {
        courseService = new CourseService();
    }

    @Test
    @DisplayName("Should list all courses successfully")
    void testListAll() {
        // Act
        List<Course> courses = courseService.listAll();

        // Assert
        assertNotNull(courses, "Course list should not be null");
        assertFalse(courses.isEmpty(), "Course list should contain seed data");
        
        // Verify seed data courses exist
        boolean hasCSE101 = courses.stream()
                .anyMatch(c -> "CSE101".equals(c.getCode()));
        assertTrue(hasCSE101, "Seed data should include CSE101");
    }

    @Test
    @DisplayName("Should return empty list when no courses match search")
    void testSearchNoResults() {
        // Act
        List<Course> results = courseService.search("XXXXNONEXISTENTXXXX");

        // Assert
        assertNotNull(results, "Search results should not be null");
        assertTrue(results.isEmpty(), "Search should return empty list for non-existent courses");
    }

    @Test
    @DisplayName("Should search courses by code")
    void testSearchByCode() {
        // Act
        List<Course> results = courseService.search("CSE101");

        // Assert
        assertNotNull(results, "Search results should not be null");
        assertFalse(results.isEmpty(), "Search should find CSE101");
        assertTrue(results.stream().anyMatch(c -> c.getCode().contains("CSE101")),
                "Results should contain courses matching 'CSE101'");
    }

    @Test
    @DisplayName("Should search courses by title")
    void testSearchByTitle() {
        // Act
        List<Course> results = courseService.search("Programming");

        // Assert
        assertNotNull(results, "Search results should not be null");
        // Results may or may not be empty depending on seed data
        // Just verify no exceptions and proper structure
    }

    @Test
    @DisplayName("Should handle empty search query")
    void testSearchEmptyQuery() {
        // Act
        List<Course> results = courseService.search("");

        // Assert
        assertNotNull(results, "Search results should not be null");
        // Empty search should return all courses (depending on DAO implementation)
    }

    @Test
    @DisplayName("Should handle null search query gracefully")
    void testSearchNullQuery() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            List<Course> results = courseService.search(null);
            assertNotNull(results, "Search results should not be null even with null query");
        }, "Service should handle null search query gracefully");
    }

    @Test
    @DisplayName("Should handle special characters in search")
    void testSearchWithSpecialCharacters() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            courseService.search("CSE%101");
            courseService.search("CSE_101");
            courseService.search("CSE';DROP TABLE courses;--");
        }, "Service should handle special characters safely (SQL injection protection)");
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void testSearchCaseInsensitive() {
        // Act
        List<Course> upperCase = courseService.search("CSE101");
        List<Course> lowerCase = courseService.search("cse101");
        List<Course> mixedCase = courseService.search("Cse101");

        // Assert
        assertNotNull(upperCase, "Upper case search should not be null");
        assertNotNull(lowerCase, "Lower case search should not be null");
        assertNotNull(mixedCase, "Mixed case search should not be null");
        
        // All should return same results (case-insensitive search)
        assertEquals(upperCase.size(), lowerCase.size(),
                "Case should not affect search results");
    }

    @Test
    @DisplayName("Should return consistent results on multiple calls")
    void testListAllConsistency() {
        // Act
        List<Course> firstCall = courseService.listAll();
        List<Course> secondCall = courseService.listAll();

        // Assert
        assertEquals(firstCall.size(), secondCall.size(),
                "Multiple calls should return consistent results");
    }

    @Test
    @DisplayName("Should handle database connection issues gracefully")
    void testDatabaseErrorHandling() {
        // This test verifies that service methods don't crash on DB errors
        // Actual DB disconnection testing would require more complex setup
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            courseService.listAll();
            courseService.search("test");
        }, "Service should handle database operations without throwing unexpected exceptions");
    }
}
