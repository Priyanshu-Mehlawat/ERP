package edu.univ.erp.data;

import edu.univ.erp.domain.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    private static final Logger logger = LoggerFactory.getLogger(StudentDAO.class);

    private static final String BASE_SELECT = "SELECT student_id, user_id, roll_no, first_name, last_name, email, program, year, phone_number FROM students";

    public Student findByUserId(Long userId) throws SQLException {
        String sql = BASE_SELECT + " WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs);}        }
        return null;
    }

    public Student findById(Long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs);}        }
        return null;
    }

    public List<Student> listByProgram(String program) {
        List<Student> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE program = ? ORDER BY year, roll_no";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, program);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { logger.error("Error listing students by program {}", program, e); }
        return list;
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getLong("student_id"));
        s.setUserId(rs.getLong("user_id"));
        s.setRollNo(rs.getString("roll_no"));
        s.setFirstName(rs.getString("first_name"));
        s.setLastName(rs.getString("last_name"));
        s.setEmail(rs.getString("email"));
        s.setProgram(rs.getString("program"));
        s.setYear(rs.getInt("year"));
        s.setPhoneNumber(rs.getString("phone_number"));
        return s;
    }
}
