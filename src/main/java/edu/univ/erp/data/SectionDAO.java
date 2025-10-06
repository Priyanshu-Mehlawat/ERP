package edu.univ.erp.data;

import edu.univ.erp.domain.Section;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import java.sql.*;import java.util.ArrayList;import java.util.List;

public class SectionDAO {
    private static final Logger logger = LoggerFactory.getLogger(SectionDAO.class);

    private static final String BASE_SELECT = "SELECT s.section_id, s.course_id, s.instructor_id, s.section_number, s.day_of_week, s.start_time, s.end_time, s.room, s.capacity, s.enrolled, s.semester, s.year, c.code AS course_code, c.title AS course_title, CONCAT(i.first_name,' ',i.last_name) AS instructor_name FROM sections s JOIN courses c ON s.course_id = c.course_id LEFT JOIN instructors i ON s.instructor_id = i.instructor_id";

    public Section findById(Long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE s.section_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs);} }
        return null;
    }

    public List<Section> listByCourse(Long courseId, String semester, Integer year) {
        List<Section> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE s.course_id = ?" + (semester != null ? " AND s.semester = ?" : "") + (year != null ? " AND s.year = ?" : "") + " ORDER BY s.section_number";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1; ps.setLong(idx++, courseId); if (semester != null) ps.setString(idx++, semester); if (year != null) ps.setInt(idx++, year);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { logger.error("Error listing sections by course {}", courseId, e); }
        return list;
    }

    public List<Section> listOpenSections(String semester, int year) {
        List<Section> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE s.semester = ? AND s.year = ? AND s.enrolled < s.capacity ORDER BY c.code, s.section_number";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, semester); ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { logger.error("Error listing open sections", e); }
        return list;
    }

    public boolean incrementEnrolled(Long sectionId) throws SQLException {
        String sql = "UPDATE sections SET enrolled = enrolled + 1 WHERE section_id = ? AND enrolled < capacity";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, sectionId); return ps.executeUpdate() == 1; }
    }

    public boolean decrementEnrolled(Long sectionId) throws SQLException {
        String sql = "UPDATE sections SET enrolled = GREATEST(0, enrolled - 1) WHERE section_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, sectionId); return ps.executeUpdate() == 1; }
    }

    private Section map(ResultSet rs) throws SQLException {
        Section s = new Section();
        s.setSectionId(rs.getLong("section_id"));
        s.setCourseId(rs.getLong("course_id"));
        long instId = rs.getLong("instructor_id"); if (!rs.wasNull()) s.setInstructorId(instId);
        s.setSectionNumber(rs.getString("section_number"));
        s.setDayOfWeek(rs.getString("day_of_week"));
        Time st = rs.getTime("start_time"); if (st != null) s.setStartTime(st.toLocalTime());
        Time et = rs.getTime("end_time"); if (et != null) s.setEndTime(et.toLocalTime());
        s.setRoom(rs.getString("room"));
        s.setCapacity(rs.getInt("capacity"));
        s.setEnrolled(rs.getInt("enrolled"));
        s.setSemester(rs.getString("semester"));
        s.setYear(rs.getInt("year"));
        s.setCourseCode(rs.getString("course_code"));
        s.setCourseTitle(rs.getString("course_title"));
        s.setInstructorName(rs.getString("instructor_name"));
        return s;
    }
}
