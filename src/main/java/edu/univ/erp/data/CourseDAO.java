package edu.univ.erp.data;

import edu.univ.erp.domain.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Course related queries.
 */
public class CourseDAO {
    private static final Logger logger = LoggerFactory.getLogger(CourseDAO.class);

    private static final String BASE_SELECT = "SELECT course_id, code, title, description, credits, department FROM courses";

    public List<Course> findAll() {
        String sql = BASE_SELECT + " ORDER BY code";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courses.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all courses", e);
        }
        return courses;
    }

    public List<Course> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String q = "%" + query.trim() + "%";
        String sql = BASE_SELECT + " WHERE code LIKE ? OR title LIKE ? OR department LIKE ? ORDER BY code";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching courses with query: {}", query, e);
        }
        return courses;
    }

    public Course findById(Long id) {
        if (id == null) return null;
        String sql = BASE_SELECT + " WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding course by id {}", id, e);
        }
        return null;
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setCourseId(rs.getLong("course_id"));
        c.setCode(rs.getString("code"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setCredits(rs.getInt("credits"));
        c.setDepartment(rs.getString("department"));
        return c;
    }
}
