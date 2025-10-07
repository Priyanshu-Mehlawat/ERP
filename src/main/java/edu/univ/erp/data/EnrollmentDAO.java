package edu.univ.erp.data;

import edu.univ.erp.domain.Enrollment;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import java.sql.*;import java.util.ArrayList;import java.util.List;

public class EnrollmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentDAO.class);

    private static final String BASE_SELECT = "SELECT e.enrollment_id, e.student_id, e.section_id, e.status, e.enrolled_date, e.dropped_date, e.final_grade, c.code AS course_code, c.title AS course_title, s.section_number, CONCAT(i.first_name,' ',i.last_name) AS instructor_name FROM enrollments e JOIN sections s ON e.section_id = s.section_id JOIN courses c ON s.course_id = c.course_id LEFT JOIN instructors i ON s.instructor_id = i.instructor_id";

    public Enrollment find(Long studentId, Long sectionId) throws SQLException {
        String sql = BASE_SELECT + " WHERE e.student_id = ? AND e.section_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, studentId); ps.setLong(2, sectionId); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs);} }
        return null;
    }

    public List<Enrollment> listByStudent(Long studentId) {
        List<Enrollment> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE e.student_id = ? ORDER BY e.enrolled_date DESC";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, studentId); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); } } catch (SQLException e) { logger.error("Error listing enrollments for student {}", studentId, e); }
        return list;
    }

    public Long create(Long studentId, Long sectionId) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, 'ENROLLED')";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { ps.setLong(1, studentId); ps.setLong(2, sectionId); ps.executeUpdate(); try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1);} }
        return null;
    }

    public boolean markDropped(Long enrollmentId) throws SQLException {
        String sql = "UPDATE enrollments SET status='DROPPED', dropped_date=CURRENT_TIMESTAMP WHERE enrollment_id = ? AND status='ENROLLED'";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, enrollmentId); return ps.executeUpdate() == 1; }
    }

    public boolean updateFinalGrade(Long enrollmentId, String finalGrade) throws SQLException {
        String sql = "UPDATE enrollments SET final_grade = ?, status = CASE WHEN status='ENROLLED' THEN 'COMPLETED' ELSE status END WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, finalGrade); ps.setLong(2, enrollmentId); return ps.executeUpdate() == 1; }
    }

    public List<Enrollment> listBySection(Long sectionId) {
        List<Enrollment> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE e.section_id = ? AND e.status IN ('ENROLLED', 'COMPLETED') ORDER BY e.enrolled_date";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { 
            ps.setLong(1, sectionId); 
            try (ResultSet rs = ps.executeQuery()) { 
                while (rs.next()) {
                    list.add(map(rs)); 
                } 
            } 
        } catch (SQLException e) { 
            logger.error("Error listing enrollments for section {}", sectionId, e); 
        }
        return list;
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setEnrollmentId(rs.getLong("enrollment_id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setSectionId(rs.getLong("section_id"));
        e.setStatus(rs.getString("status"));
        Timestamp ed = rs.getTimestamp("enrolled_date"); if (ed != null) e.setEnrolledDate(ed.toLocalDateTime());
        Timestamp dd = rs.getTimestamp("dropped_date"); if (dd != null) e.setDroppedDate(dd.toLocalDateTime());
        e.setFinalGrade(rs.getString("final_grade"));
        e.setCourseCode(rs.getString("course_code"));
        e.setCourseTitle(rs.getString("course_title"));
        e.setSectionNumber(rs.getString("section_number"));
        e.setInstructorName(rs.getString("instructor_name"));
        return e;
    }
}
