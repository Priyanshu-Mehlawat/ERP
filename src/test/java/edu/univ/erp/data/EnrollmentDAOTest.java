package edu.univ.erp.data;

import edu.univ.erp.auth.AuthDAO;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Section;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnrollmentDAO Tests")
class EnrollmentDAOTest extends BaseDAOTest {
    private Long testCourseId;
    private Long testSectionId;
    private Long testStudentUserId;
    private Long testStudentId;
    private final List<Long> createdEnrollments = new ArrayList<>();

    @BeforeEach
    void setupTestData() throws SQLException {
        // Create a course
        CourseDAO courseDAO = new CourseDAO();
        Course c = new Course();
        c.setCode("ENRL001");
        c.setTitle("Enrollment Test Course");
        c.setDescription("Test");
        c.setCredits(3);
        c.setDepartment("TEST");
        testCourseId = courseDAO.save(c);

        // Create a section
        SectionDAO sectionDAO = new SectionDAO();
        Section s = new Section();
        s.setCourseId(testCourseId);
        s.setInstructorId(null);
        s.setSectionNumber("T1");
        s.setDayOfWeek("Monday");
        s.setStartTime(LocalTime.of(10, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setRoom("T101");
        s.setCapacity(10);
        s.setEnrolled(0);
        s.setSemester("Fall");
        s.setYear(2025);
        testSectionId = sectionDAO.save(s);

        // Create a student user in auth DB
        AuthDAO authDAO = new AuthDAO();
        String testPassword = "TestPass@" + System.currentTimeMillis();
        testStudentUserId = authDAO.createUser("enroll_test_student", "STUDENT", PasswordUtil.hashPassword(testPassword));

        // Create student record in ERP DB
        executeCleanupSQL("INSERT INTO students (user_id, roll_no, first_name, last_name, email, program, year) " +
                "VALUES (" + testStudentUserId + ", 'ENRLTEST01', 'Test', 'Student', 'test@test.com', 'B.Tech', 1)");
        testStudentId = executeQueryForId("SELECT student_id FROM students WHERE user_id = " + testStudentUserId);
    }

    @AfterEach
    void cleanupTestData() {
        // Clean enrollments
        for (Long enrollmentId : createdEnrollments) {
            executeCleanupSQL("DELETE FROM enrollments WHERE enrollment_id = " + enrollmentId);
        }
        createdEnrollments.clear();

        // Clean student
        if (testStudentId != null) {
            executeCleanupSQL("DELETE FROM students WHERE student_id = " + testStudentId);
        }
        // Clean auth user
        if (testStudentUserId != null) {
            executeAuthCleanupSQL("DELETE FROM users_auth WHERE user_id = " + testStudentUserId);
        }
        // Clean section
        if (testSectionId != null) {
            executeCleanupSQL("DELETE FROM sections WHERE section_id = " + testSectionId);
        }
        // Clean course
        if (testCourseId != null) {
            executeCleanupSQL("DELETE FROM courses WHERE course_id = " + testCourseId);
        }
    }

    private Long executeQueryForId(String sql) {
        try (var conn = edu.univ.erp.data.DatabaseConnection.getErpConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Test
    @DisplayName("Create and find enrollment by student and section")
    void testCreateAndFind() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        assertNotNull(enrollmentId);
        createdEnrollments.add(enrollmentId);

        Enrollment found = dao.find(testStudentId, testSectionId);
        assertNotNull(found);
        assertEquals(enrollmentId, found.getEnrollmentId());
        assertEquals("ENROLLED", found.getStatus());
        assertNotNull(found.getEnrolledDate());
    }

    @Test
    @DisplayName("Find enrollment by ID")
    void testFindById() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        createdEnrollments.add(enrollmentId);

        Enrollment fetched = dao.findById(enrollmentId);
        assertNotNull(fetched);
        assertEquals(enrollmentId, fetched.getEnrollmentId());
        assertEquals(testStudentId, fetched.getStudentId());
        assertEquals(testSectionId, fetched.getSectionId());
    }

    @Test
    @DisplayName("List enrollments by student")
    void testListByStudent() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        createdEnrollments.add(enrollmentId);

        List<Enrollment> list = dao.listByStudent(testStudentId);
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(e -> e.getEnrollmentId().equals(enrollmentId)));
    }

    @Test
    @DisplayName("List enrollments by section")
    void testListBySection() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        createdEnrollments.add(enrollmentId);

        List<Enrollment> list = dao.listBySection(testSectionId);
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(e -> e.getEnrollmentId().equals(enrollmentId)));
    }

    @Test
    @DisplayName("Mark enrollment as dropped")
    void testMarkDropped() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        createdEnrollments.add(enrollmentId);

        boolean dropped = dao.markDropped(enrollmentId);
        assertTrue(dropped);

        Enrollment fetched = dao.findById(enrollmentId);
        assertEquals("DROPPED", fetched.getStatus());
        assertNotNull(fetched.getDroppedDate());
    }

    @Test
    @DisplayName("Update final grade and transition to COMPLETED")
    void testUpdateFinalGrade() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        createdEnrollments.add(enrollmentId);

        boolean updated = dao.updateFinalGrade(enrollmentId, "A");
        assertTrue(updated);

        Enrollment fetched = dao.findById(enrollmentId);
        assertEquals("A", fetched.getFinalGrade());
        assertEquals("COMPLETED", fetched.getStatus());
    }

    @Test
    @DisplayName("Mark dropped on already dropped enrollment returns false")
    void testMarkDroppedIdempotency() throws SQLException {
        EnrollmentDAO dao = new EnrollmentDAO();
        Long enrollmentId = dao.create(testStudentId, testSectionId);
        createdEnrollments.add(enrollmentId);

        assertTrue(dao.markDropped(enrollmentId));
        assertFalse(dao.markDropped(enrollmentId)); // second call should fail
    }
}
