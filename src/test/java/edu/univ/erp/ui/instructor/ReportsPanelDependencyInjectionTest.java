package edu.univ.erp.ui.instructor;

import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportsPanel dependency injection functionality.
 * Demonstrates how constructor injection enables proper testing.
 * 
 * NOTE: These tests verify that ReportsPanel supports constructor injection by checking
 * that the constructors work properly. The actual service method calls happen asynchronously
 * via SwingUtilities.invokeLater() and require a valid SessionManager session (logged-in user),
 * so we don't test the async behavior here. The key test is that the panel can be
 * constructed with injected dependencies, which enables proper mocking in integration tests.
 */
class ReportsPanelDependencyInjectionTest {

    @Test
    void testConstructorInjection() throws Exception {
        // Test that ReportsPanel accepts injected dependencies via constructor
        // This is the key test: Can we pass in our own SectionService?
        
        MockSectionService customMock = new MockSectionService();
        customMock.setSectionsToReturn(Arrays.asList(createMockSection(1L, "CS101", "A")));
        
        // The constructor injection pattern is validated by successful construction
        assertDoesNotThrow(() -> {
            ReportsPanel testPanel = new ReportsPanel(customMock);
            assertNotNull(testPanel, "ReportsPanel should be constructed with injected SectionService");
        }, "ReportsPanel should accept a custom SectionService via constructor injection");
        
        // Note: loadSections() is called async via SwingUtilities.invokeLater() in the constructor
        // and requires SessionManager.getCurrentUser() which returns null in tests.
        // The injection pattern is proven by the constructor accepting the dependency.
    }

    @Test
    void testBackwardCompatibilityConstructor() {
        // Test that the no-arg constructor still works (creates default SectionService internally)
        assertDoesNotThrow(() -> {
            ReportsPanel defaultPanel = new ReportsPanel();
            assertNotNull(defaultPanel, "No-arg constructor should create a working ReportsPanel");
        });
    }

    @Test
    void testFullDependencyInjectionConstructor() {
        // Test the full constructor that accepts all 5 dependencies
        // This demonstrates complete control for testing
        MockSectionService customMock = new MockSectionService();
        customMock.setSectionsToReturn(Arrays.asList(createMockSection(1L, "CS101", "A")));
        
        assertDoesNotThrow(() -> {
            // Pass null for the DAOs since we're just testing constructor acceptance
            ReportsPanel testPanel = new ReportsPanel(customMock, null, null, null, null);
            assertNotNull(testPanel, "ReportsPanel should accept full constructor injection");
        }, "ReportsPanel should support full dependency injection with 5 parameters");
    }

    /**
     * Mock SectionService for testing purposes.
     * Demonstrates how dependency injection enables proper unit testing.
     */
    private static class MockSectionService extends SectionService {
        private java.util.List<Section> sectionsToReturn = new java.util.ArrayList<>();

        void setSectionsToReturn(java.util.List<Section> sections) {
            this.sectionsToReturn = sections;
        }

        @Override
        public java.util.List<Section> listByInstructor(Long instructorId) {
            return sectionsToReturn;
        }
    }

    private Section createMockSection(Long id, String courseCode, String sectionNumber) {
        Section section = new Section();
        section.setSectionId(id);
        section.setCourseCode(courseCode);
        section.setSectionNumber(sectionNumber);
        return section;
    }
}