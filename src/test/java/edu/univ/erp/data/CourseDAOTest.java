package edu.univ.erp.data;

import edu.univ.erp.domain.Course;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CourseDAO Tests")
class CourseDAOTest extends BaseDAOTest {
    @Test
    @Order(1)
    @DisplayName("Create, fetch, update, and delete course")
    void testCRUD() throws SQLException {
        CourseDAO dao = new CourseDAO();
        Course c = new Course();
        c.setCode("CSE999");
        c.setTitle("Test Course");
        c.setDescription("Test Description");
        c.setCredits(3);
        c.setDepartment("CSE");

        Long id = dao.save(c);
        assertNotNull(id);
        
        Course fetched = dao.findById(id);
        assertNotNull(fetched);
        assertEquals("CSE999", fetched.getCode());
        
        fetched.setTitle("Updated Title");
        dao.update(fetched);
        
        Course updated = dao.findById(id);
        assertEquals("Updated Title", updated.getTitle());
        
        dao.delete(id);
        assertNull(dao.findById(id));
        
        // cleanup just in case
        executeCleanupSQL("DELETE FROM courses WHERE course_id = " + id);
    }

    @Test
    @Order(2)
    @DisplayName("Search and findAll do not throw")
    void testSearchAndFindAll() {
        CourseDAO dao = new CourseDAO();
        List<Course> all = dao.findAll();
        assertNotNull(all);
        List<Course> search = dao.search("CSE");
        assertNotNull(search);
    }
}
