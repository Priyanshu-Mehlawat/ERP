package edu.univ.erp.data;

import edu.univ.erp.domain.Instructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InstructorDAO {
    private static final Logger logger = LoggerFactory.getLogger(InstructorDAO.class);
    private static final String BASE_SELECT = "SELECT instructor_id, user_id, employee_id, first_name, last_name, email, department, phone_number FROM instructors";

    public Instructor findByUserId(Long userId) throws SQLException {
        String sql = BASE_SELECT + " WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs);}        }
        return null;
    }

    public Instructor findById(Long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE instructor_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs);}        }
        return null;
    }

    public List<Instructor> listByDepartment(String dept) {
        List<Instructor> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE department = ? ORDER BY last_name";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { logger.error("Error listing instructors by department {}", dept, e); }
        return list;
    }

    private Instructor map(ResultSet rs) throws SQLException {
        Instructor i = new Instructor();
        i.setInstructorId(rs.getLong("instructor_id"));
        i.setUserId(rs.getLong("user_id"));
        i.setEmployeeId(rs.getString("employee_id"));
        i.setFirstName(rs.getString("first_name"));
        i.setLastName(rs.getString("last_name"));
        i.setEmail(rs.getString("email"));
        i.setDepartment(rs.getString("department"));
        i.setPhoneNumber(rs.getString("phone_number"));
        return i;
    }
}
