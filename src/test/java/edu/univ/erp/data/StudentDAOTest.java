package edu.univ.erp.data;

import edu.univ.erp.auth.AuthDAO;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.domain.Student;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentDAO Tests")
class StudentDAOTest extends BaseDAOTest {
    private Long testUserId1;
    private Long testUserId2;
    private Long testStudentId1;
    private Long testStudentId2;
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdStudentIds = new ArrayList<>();

    @BeforeEach
    void setupTestData() throws SQLException {
        AuthDAO authDAO = new AuthDAO();
        
        // Create two test students
        testUserId1 = authDAO.createUser("stud_test_1", "STUDENT", PasswordUtil.hashPassword("password"));
        createdUserIds.add(testUserId1);
        executeCleanupSQL("INSERT INTO students (user_id, roll_no, first_name, last_name, email, program, year) " +
                "VALUES (" + testUserId1 + ", 'STUD001', 'Alice', 'Smith', 'alice@test.com', 'B.Tech CSE', 2)");
        testStudentId1 = executeQueryForId("SELECT student_id FROM students WHERE user_id = " + testUserId1);
        createdStudentIds.add(testStudentId1);

        testUserId2 = authDAO.createUser("stud_test_2", "STUDENT", PasswordUtil.hashPassword("password"));
        createdUserIds.add(testUserId2);
        executeCleanupSQL("INSERT INTO students (user_id, roll_no, first_name, last_name, email, program, year) " +
                "VALUES (" + testUserId2 + ", 'STUD002', 'Bob', 'Jones', 'bob@test.com', 'B.Tech CSE', 3)");
        testStudentId2 = executeQueryForId("SELECT student_id FROM students WHERE user_id = " + testUserId2);
        createdStudentIds.add(testStudentId2);
    }

    @AfterEach
    void cleanupTestData() {
        for (Long studentId : createdStudentIds) {
            executeCleanupSQL("DELETE FROM students WHERE student_id = " + studentId);
        }
        createdStudentIds.clear();

        for (Long userId : createdUserIds) {
            executeAuthCleanupSQL("DELETE FROM users_auth WHERE user_id = " + userId);
        }
        createdUserIds.clear();
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
    @DisplayName("Find student by user ID")
    void testFindByUserId() throws SQLException {
        StudentDAO dao = new StudentDAO();
        Student student = dao.findByUserId(testUserId1);
        assertNotNull(student);
        assertEquals(testStudentId1, student.getStudentId());
        assertEquals("Alice", student.getFirstName());
        assertEquals("Smith", student.getLastName());
        assertEquals("STUD001", student.getRollNo());
    }

    @Test
    @DisplayName("Find student by student ID")
    void testFindById() throws SQLException {
        StudentDAO dao = new StudentDAO();
        Student student = dao.findById(testStudentId2);
        assertNotNull(student);
        assertEquals(testUserId2, student.getUserId());
        assertEquals("Bob", student.getFirstName());
        assertEquals("B.Tech CSE", student.getProgram());
        assertEquals(3, student.getYear());
    }

    @Test
    @DisplayName("Find students by IDs batch")
    void testFindByIds() throws SQLException {
        StudentDAO dao = new StudentDAO();
        List<Long> ids = List.of(testStudentId1, testStudentId2);
        List<Student> students = dao.findByIds(ids);
        assertNotNull(students);
        assertEquals(2, students.size());
        assertTrue(students.stream().anyMatch(s -> s.getStudentId().equals(testStudentId1)));
        assertTrue(students.stream().anyMatch(s -> s.getStudentId().equals(testStudentId2)));
    }

    @Test
    @DisplayName("Find students by IDs returns empty for empty list")
    void testFindByIdsEmpty() throws SQLException {
        StudentDAO dao = new StudentDAO();
        List<Student> students = dao.findByIds(List.of());
        assertNotNull(students);
        assertTrue(students.isEmpty());
    }

    @Test
    @DisplayName("List students by program")
    void testListByProgram() {
        StudentDAO dao = new StudentDAO();
        List<Student> students = dao.listByProgram("B.Tech CSE");
        assertNotNull(students);
        // Should include at least our test students
        assertTrue(students.stream().anyMatch(s -> s.getStudentId().equals(testStudentId1)));
        assertTrue(students.stream().anyMatch(s -> s.getStudentId().equals(testStudentId2)));
    }

    @Test
    @DisplayName("Find by non-existent user ID returns null")
    void testFindByUserIdNotFound() throws SQLException {
        StudentDAO dao = new StudentDAO();
        Student student = dao.findByUserId(999999L);
        assertNull(student);
    }

    @Test
    @DisplayName("Find by non-existent student ID returns null")
    void testFindByIdNotFound() throws SQLException {
        StudentDAO dao = new StudentDAO();
        Student student = dao.findById(999999L);
        assertNull(student);
    }
}
