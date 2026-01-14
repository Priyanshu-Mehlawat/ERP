package edu.univ.erp.data;

import edu.univ.erp.auth.AuthDAO;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Grade;
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

@DisplayName("GradeDAO Tests")
class GradeDAOTest extends BaseDAOTest {
    private Long testCourseId;
    private Long testSectionId;
    private Long testStudentUserId;
    private Long testStudentId;
    private Long testEnrollmentId;
    private final List<Long> createdGrades = new ArrayList<>();

    @BeforeEach
    void setupTestData() throws SQLException {
        // Create course
        CourseDAO courseDAO = new CourseDAO();
        Course c = new Course();
        c.setCode("GRD001");
        c.setTitle("Grade Test Course");
        c.setDescription("Test");
        c.setCredits(3);
        c.setDepartment("TEST");
        testCourseId = courseDAO.save(c);

        // Create section
        SectionDAO sectionDAO = new SectionDAO();
        Section s = new Section();
        s.setCourseId(testCourseId);
        s.setInstructorId(null);
        s.setSectionNumber("G1");
        s.setDayOfWeek("Tuesday");
        s.setStartTime(LocalTime.of(14, 0));
        s.setEndTime(LocalTime.of(15, 0));
        s.setRoom("G101");
        s.setCapacity(10);
        s.setEnrolled(0);
        s.setSemester("Fall");
        s.setYear(2025);
        testSectionId = sectionDAO.save(s);

        // Create student user
        AuthDAO authDAO = new AuthDAO();
        String testPassword = "TestPass@" + System.currentTimeMillis();
        testStudentUserId = authDAO.createUser("grade_test_student", "STUDENT", PasswordUtil.hashPassword(testPassword));

        // Create student record
        executeCleanupSQL("INSERT INTO students (user_id, roll_no, first_name, last_name, email, program, year) " +
                "VALUES (" + testStudentUserId + ", 'GRDTEST01', 'Grade', 'Student', 'grade@test.com', 'B.Tech', 1)");
        testStudentId = executeQueryForId("SELECT student_id FROM students WHERE user_id = " + testStudentUserId);

        // Create enrollment
        EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
        testEnrollmentId = enrollmentDAO.create(testStudentId, testSectionId);
    }

    @AfterEach
    void cleanupTestData() {
        // Clean grades
        for (Long gradeId : createdGrades) {
            executeCleanupSQL("DELETE FROM grades WHERE grade_id = " + gradeId);
        }
        createdGrades.clear();

        // Clean enrollment
        if (testEnrollmentId != null) {
            executeCleanupSQL("DELETE FROM enrollments WHERE enrollment_id = " + testEnrollmentId);
        }
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
    @DisplayName("Add grade component with null score")
    void testAddComponentNullScore() throws SQLException {
        GradeDAO dao = new GradeDAO();
        Long gradeId = dao.addComponent(testEnrollmentId, "Quiz1", null, 10.0, 10.0);
        assertNotNull(gradeId);
        createdGrades.add(gradeId);

        List<Grade> grades = dao.listByEnrollment(testEnrollmentId);
        assertEquals(1, grades.size());
        assertEquals("Quiz1", grades.get(0).getComponent());
        assertNull(grades.get(0).getScore());
        assertEquals(10.0, grades.get(0).getMaxScore());
        assertEquals(10.0, grades.get(0).getWeight());
    }

    @Test
    @DisplayName("Add grade component with score and update it")
    void testAddAndUpdateScore() throws SQLException {
        GradeDAO dao = new GradeDAO();
        Long gradeId = dao.addComponent(testEnrollmentId, "Midterm", 75.0, 100.0, 30.0);
        assertNotNull(gradeId);
        createdGrades.add(gradeId);

        List<Grade> grades = dao.listByEnrollment(testEnrollmentId);
        assertEquals(1, grades.size());
        assertEquals(75.0, grades.get(0).getScore());

        boolean updated = dao.updateScore(gradeId, 85.0);
        assertTrue(updated);

        grades = dao.listByEnrollment(testEnrollmentId);
        assertEquals(85.0, grades.get(0).getScore());
    }

    @Test
    @DisplayName("Calculate total weight for enrollment")
    void testTotalWeight() throws SQLException {
        GradeDAO dao = new GradeDAO();
        Long g1 = dao.addComponent(testEnrollmentId, "Quiz", 8.0, 10.0, 10.0);
        Long g2 = dao.addComponent(testEnrollmentId, "Midterm", 80.0, 100.0, 30.0);
        Long g3 = dao.addComponent(testEnrollmentId, "Final", 90.0, 100.0, 60.0);
        createdGrades.add(g1);
        createdGrades.add(g2);
        createdGrades.add(g3);

        double totalWeight = dao.totalWeight(testEnrollmentId);
        assertEquals(100.0, totalWeight, 0.01);
    }

    @Test
    @DisplayName("Get distinct components for section")
    void testGetDistinctComponentsForSection() throws SQLException {
        GradeDAO dao = new GradeDAO();
        Long g1 = dao.addComponent(testEnrollmentId, "Quiz1", 10.0, 10.0, 5.0);
        Long g2 = dao.addComponent(testEnrollmentId, "Quiz2", 9.0, 10.0, 5.0);
        Long g3 = dao.addComponent(testEnrollmentId, "Midterm", 85.0, 100.0, 30.0);
        createdGrades.add(g1);
        createdGrades.add(g2);
        createdGrades.add(g3);

        List<String> components = dao.getDistinctComponentsForSection(testSectionId);
        assertNotNull(components);
        assertTrue(components.contains("Quiz1"));
        assertTrue(components.contains("Quiz2"));
        assertTrue(components.contains("Midterm"));
    }

    @Test
    @DisplayName("List by enrollment returns empty for new enrollment")
    void testListByEnrollmentEmpty() {
        GradeDAO dao = new GradeDAO();
        List<Grade> grades = dao.listByEnrollment(testEnrollmentId);
        assertNotNull(grades);
        assertTrue(grades.isEmpty());
    }

    @Test
    @DisplayName("Update score to null")
    void testUpdateScoreToNull() throws SQLException {
        GradeDAO dao = new GradeDAO();
        Long gradeId = dao.addComponent(testEnrollmentId, "Assignment", 50.0, 50.0, 15.0);
        createdGrades.add(gradeId);

        boolean updated = dao.updateScore(gradeId, null);
        assertTrue(updated);

        List<Grade> grades = dao.listByEnrollment(testEnrollmentId);
        assertEquals(1, grades.size());
        assertNull(grades.get(0).getScore());
    }
}
