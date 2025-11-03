package edu.univ.erp.data;

import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Course;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SectionDAO Tests")
class SectionDAOTest extends BaseDAOTest {

    @Test
    @Order(1)
    @DisplayName("Create, fetch, update, and delete section; increment/decrement enrolled; list open")
    void testSectionLifecycle() throws SQLException {
        // Create a backing course first to satisfy FK
        CourseDAO courseDAO = new CourseDAO();
        Course c = new Course();
        c.setCode("TST100");
        c.setTitle("Temp Test Course");
        c.setDescription("Temp Desc");
        c.setCredits(3);
        c.setDepartment("TEST");
        Long courseId = courseDAO.save(c);
        assertNotNull(courseId);

        SectionDAO dao = new SectionDAO();
        Section s = new Section();
        s.setCourseId(courseId);
        s.setInstructorId(null); // optional
        s.setSectionNumber("A");
        s.setDayOfWeek("Monday");
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(10, 0));
        s.setRoom("R101");
        s.setCapacity(2);
        s.setEnrolled(0);
        s.setSemester("Fall");
        s.setYear(2025);

        Long sectionId = dao.save(s);
        assertNotNull(sectionId);

        Section fetched = dao.findById(sectionId);
        assertNotNull(fetched);
        assertEquals("A", fetched.getSectionNumber());
        assertEquals(0, fetched.getEnrolled());

        // update
        fetched.setRoom("R102");
        fetched.setCapacity(3);
        dao.update(fetched);
        Section updated = dao.findById(sectionId);
        assertEquals("R102", updated.getRoom());
        assertEquals(3, updated.getCapacity());

        // increment/decrement
        assertTrue(dao.incrementEnrolled(sectionId));
        assertTrue(dao.incrementEnrolled(sectionId));
        // capacity was 3, so one more increments should succeed
        assertTrue(dao.incrementEnrolled(sectionId));
        // now should be full, next increment should fail
        assertFalse(dao.incrementEnrolled(sectionId));
        assertTrue(dao.decrementEnrolled(sectionId));

        // list open sections should include it (since enrolled < capacity after decrement)
        List<Section> open = dao.listOpenSections("Fall", 2025);
        assertNotNull(open);
        assertTrue(open.stream().anyMatch(sec -> sec.getSectionId().equals(sectionId)));

        // list by course should include it
        List<Section> byCourse = dao.listByCourse(courseId, "Fall", 2025);
        assertNotNull(byCourse);
        assertTrue(byCourse.stream().anyMatch(sec -> sec.getSectionId().equals(sectionId)));

        // assign instructor as null (no-op but should not throw)
        dao.assignInstructor(sectionId, null);

        // cleanup
        dao.delete(sectionId);
        courseDAO.delete(courseId);

        assertNull(dao.findById(sectionId));
        executeCleanupSQL("DELETE FROM sections WHERE section_id = " + sectionId);
        executeCleanupSQL("DELETE FROM courses WHERE course_id = " + courseId);
    }
}
