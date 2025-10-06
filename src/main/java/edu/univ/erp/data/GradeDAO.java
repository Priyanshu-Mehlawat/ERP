package edu.univ.erp.data;

import edu.univ.erp.domain.Grade;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import java.sql.*;import java.util.ArrayList;import java.util.List;

public class GradeDAO {
    private static final Logger logger = LoggerFactory.getLogger(GradeDAO.class);

    private static final String BASE_SELECT = "SELECT grade_id, enrollment_id, component, score, max_score, weight FROM grades";

    public List<Grade> listByEnrollment(Long enrollmentId) {
        List<Grade> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE enrollment_id = ? ORDER BY grade_id";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, enrollmentId); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); } } catch (SQLException e) { logger.error("Error listing grades for enrollment {}", enrollmentId, e); }
        return list;
    }

    public Long addComponent(Long enrollmentId, String component, Double score, double maxScore, double weight) throws SQLException {
        String sql = "INSERT INTO grades (enrollment_id, component, score, max_score, weight) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, enrollmentId); ps.setString(2, component); if (score == null) ps.setNull(3, Types.DECIMAL); else ps.setDouble(3, score);
            ps.setDouble(4, maxScore); ps.setDouble(5, weight); ps.executeUpdate(); try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1);} }
        return null;
    }

    public boolean updateScore(Long gradeId, Double score) throws SQLException {
        String sql = "UPDATE grades SET score = ? WHERE grade_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { if (score == null) ps.setNull(1, Types.DECIMAL); else ps.setDouble(1, score); ps.setLong(2, gradeId); return ps.executeUpdate() == 1; }
    }

    public double totalWeight(Long enrollmentId) {
        String sql = "SELECT COALESCE(SUM(weight),0) FROM grades WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, enrollmentId); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getDouble(1);} } catch (SQLException e) { logger.error("Error computing weight sum", e);} return 0.0;
    }

    private Grade map(ResultSet rs) throws SQLException {
        Grade g = new Grade();
        g.setGradeId(rs.getLong("grade_id"));
        g.setEnrollmentId(rs.getLong("enrollment_id"));
        g.setComponent(rs.getString("component"));
        g.setScore(rs.getBigDecimal("score") != null ? rs.getDouble("score") : null);
        g.setMaxScore(rs.getDouble("max_score"));
        g.setWeight(rs.getDouble("weight"));
        return g;
    }
}
