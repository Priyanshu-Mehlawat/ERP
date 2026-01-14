package edu.univ.erp.data;

import edu.univ.erp.auth.AuthDAO;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InstructorDAO Tests")
class InstructorDAOTest extends BaseDAOTest {
    private Long testUserId1;
    private Long testUserId2;
    private Long testInstructorId1;
    private Long testInstructorId2;
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdInstructorIds = new ArrayList<>();

    @BeforeEach
    void setupTestData() throws SQLException {
        AuthDAO authDAO = new AuthDAO();
        
        // Create two test instructors with unique employee IDs using timestamp
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        String testPassword = "TestPass@" + uniqueSuffix;
        
        testUserId1 = authDAO.createUser("inst_test_1_" + uniqueSuffix, "INSTRUCTOR", PasswordUtil.hashPassword(testPassword));
        createdUserIds.add(testUserId1);
        executeCleanupSQL("INSERT INTO instructors (user_id, employee_id, first_name, last_name, email, department, phone_number) " +
                "VALUES (" + testUserId1 + ", 'EMP" + uniqueSuffix + "1', 'John', 'Doe', 'john" + uniqueSuffix + "@test.com', 'CSE', '1234567890')");
        testInstructorId1 = executeQueryForId("SELECT instructor_id FROM instructors WHERE user_id = " + testUserId1);
        createdInstructorIds.add(testInstructorId1);

        testUserId2 = authDAO.createUser("inst_test_2_" + uniqueSuffix, "INSTRUCTOR", PasswordUtil.hashPassword(testPassword));
        createdUserIds.add(testUserId2);
        executeCleanupSQL("INSERT INTO instructors (user_id, employee_id, first_name, last_name, email, department, phone_number) " +
                "VALUES (" + testUserId2 + ", 'EMP" + uniqueSuffix + "2', 'Jane', 'Smith', 'jane" + uniqueSuffix + "@test.com', 'CSE', '0987654321')");
        testInstructorId2 = executeQueryForId("SELECT instructor_id FROM instructors WHERE user_id = " + testUserId2);
        createdInstructorIds.add(testInstructorId2);
    }

    @AfterEach
    void cleanupTestData() {
        for (Long instructorId : createdInstructorIds) {
            executeCleanupSQL("DELETE FROM instructors WHERE instructor_id = " + instructorId);
        }
        createdInstructorIds.clear();

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
    @DisplayName("Find instructor by user ID")
    void testFindByUserId() throws SQLException {
        InstructorDAO dao = new InstructorDAO();
        Instructor instructor = dao.findByUserId(testUserId1);
        assertNotNull(instructor);
        assertEquals(testInstructorId1, instructor.getInstructorId());
        assertEquals("John", instructor.getFirstName());
        assertEquals("Doe", instructor.getLastName());
        assertTrue(instructor.getEmployeeId().startsWith("EMP"));
        assertEquals("CSE", instructor.getDepartment());
    }

    @Test
    @DisplayName("Find instructor by instructor ID")
    void testFindById() throws SQLException {
        InstructorDAO dao = new InstructorDAO();
        Instructor instructor = dao.findById(testInstructorId2);
        assertNotNull(instructor);
        assertEquals(testUserId2, instructor.getUserId());
        assertEquals("Jane", instructor.getFirstName());
        assertEquals("Smith", instructor.getLastName());
        assertTrue(instructor.getEmployeeId().startsWith("EMP"));
    }

    @Test
    @DisplayName("List instructors by department")
    void testListByDepartment() {
        InstructorDAO dao = new InstructorDAO();
        List<Instructor> instructors = dao.listByDepartment("CSE");
        assertNotNull(instructors);
        // Should include at least our test instructors
        assertTrue(instructors.stream().anyMatch(i -> i.getInstructorId().equals(testInstructorId1)));
        assertTrue(instructors.stream().anyMatch(i -> i.getInstructorId().equals(testInstructorId2)));
    }

    @Test
    @DisplayName("Find all instructors")
    void testFindAll() throws SQLException {
        InstructorDAO dao = new InstructorDAO();
        List<Instructor> instructors = dao.findAll();
        assertNotNull(instructors);
        // Should include at least our test instructors
        assertTrue(instructors.stream().anyMatch(i -> i.getInstructorId().equals(testInstructorId1)));
        assertTrue(instructors.stream().anyMatch(i -> i.getInstructorId().equals(testInstructorId2)));
    }

    @Test
    @DisplayName("Find by non-existent user ID returns null")
    void testFindByUserIdNotFound() throws SQLException {
        InstructorDAO dao = new InstructorDAO();
        Instructor instructor = dao.findByUserId(999999L);
        assertNull(instructor);
    }

    @Test
    @DisplayName("Find by non-existent instructor ID returns null")
    void testFindByIdNotFound() throws SQLException {
        InstructorDAO dao = new InstructorDAO();
        Instructor instructor = dao.findById(999999L);
        assertNull(instructor);
    }

    @Test
    @DisplayName("List by non-existent department returns empty")
    void testListByDepartmentEmpty() {
        InstructorDAO dao = new InstructorDAO();
        List<Instructor> instructors = dao.listByDepartment("NONEXISTENT");
        assertNotNull(instructors);
        assertTrue(instructors.isEmpty());
    }
}
