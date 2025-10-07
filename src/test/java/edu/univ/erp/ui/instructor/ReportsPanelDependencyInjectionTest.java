package edu.univ.erp.ui.instructor;

import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportsPanel dependency injection functionality.
 * Demonstrates how constructor injection enables proper testing.
 */
class ReportsPanelDependencyInjectionTest {

    private MockSectionService mockSectionService;
    private ReportsPanel reportsPanel;

    @BeforeEach
    void setUp() {
        mockSectionService = new MockSectionService();
        reportsPanel = new ReportsPanel(mockSectionService);
    }

    @Test
    void testConstructorInjection() throws Exception {
        // Behavioral test: Verify that dependency injection works by testing the observable behavior
        // Instead of inspecting private fields, we test that the injected service is actually used
        
        // Arrange: Setup mock service to return test data
        Section testSection = createMockSection(1L, "CS101", "A");
        mockSectionService.setSectionsToReturn(Arrays.asList(testSection));
        
        // Reset the mock state and create a new ReportsPanel to test fresh injection
        MockSectionService freshMock = new MockSectionService();
        freshMock.setSectionsToReturn(Arrays.asList(testSection));
        ReportsPanel testPanel = new ReportsPanel(freshMock);
        
        // Act: Wait for async operations to complete
        Thread.sleep(200);
        
        // Assert: Verify the injected service was actually called (behavioral verification)
        assertTrue(freshMock.wasListMethodCalled(), 
            "Constructor should use the injected SectionService, proving dependency injection works");
        
        // Additional verification: Ensure the panel was created successfully
        assertNotNull(testPanel, "ReportsPanel should be constructed with injected dependencies");
    }

    @Test
    void testBackwardCompatibilityConstructor() {
        // Test that the no-arg constructor still works
        assertDoesNotThrow(() -> {
            ReportsPanel defaultPanel = new ReportsPanel();
            assertNotNull(defaultPanel, "No-arg constructor should create a working ReportsPanel");
        });
    }

    @Test
    void testMockServiceIntegration() {
        // Verify that our mock service can be used for testing
        mockSectionService.setSectionsToReturn(Arrays.asList(
            createMockSection(1L, "CS101", "01"),
            createMockSection(2L, "CS102", "02")
        ));
        
        // The panel should be created successfully with mock data
        assertNotNull(reportsPanel, "ReportsPanel should be created with mock service");
        assertTrue(mockSectionService.wasListMethodCalled(), 
            "Mock service should be called during panel initialization");
    }

    /**
     * Mock SectionService for testing purposes.
     * Demonstrates how dependency injection enables proper unit testing.
     */
    private static class MockSectionService extends SectionService {
        private java.util.List<Section> sectionsToReturn = Collections.emptyList();
        private boolean listMethodCalled = false;

        void setSectionsToReturn(java.util.List<Section> sections) {
            this.sectionsToReturn = sections;
        }

        boolean wasListMethodCalled() {
            return listMethodCalled;
        }

        @Override
        public java.util.List<Section> listByInstructor(Long instructorId) {
            listMethodCalled = true;
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