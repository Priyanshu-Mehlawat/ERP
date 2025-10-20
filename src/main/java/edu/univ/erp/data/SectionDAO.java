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

    public List<Section> listByInstructor(Long instructorId) {
        List<Section> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE s.instructor_id = ? ORDER BY c.code, s.section_number";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { logger.error("Error listing sections by instructor {}", instructorId, e); }
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

    public List<Section> findAll() throws SQLException {
        List<Section> list = new ArrayList<>();
        String sql = BASE_SELECT + " ORDER BY s.semester, s.year, c.code, s.section_number";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Long save(Section section) throws SQLException {
        // Input validation for required fields
        if (section == null) {
            throw new IllegalArgumentException("Section object must not be null");
        }
        if (section.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (section.getSectionNumber() == null || section.getSectionNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Section number is required");
        }
        if (section.getDayOfWeek() == null || section.getDayOfWeek().trim().isEmpty()) {
            throw new IllegalArgumentException("Day of week is required");
        }
        if (section.getRoom() == null || section.getRoom().trim().isEmpty()) {
            throw new IllegalArgumentException("Room is required");
        }
        if (section.getSemester() == null || section.getSemester().trim().isEmpty()) {
            throw new IllegalArgumentException("Semester is required");
        }
        String sql = "INSERT INTO sections (course_id, instructor_id, section_number, day_of_week, start_time, end_time, room, capacity, enrolled, semester, year) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, section.getCourseId());
            if (section.getInstructorId() != null) {
                ps.setLong(2, section.getInstructorId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, section.getSectionNumber());
            ps.setString(4, section.getDayOfWeek());
            if (section.getStartTime() != null) {
                try {
                    ps.setTime(5, Time.valueOf(section.getStartTime()));
                } catch (IllegalArgumentException ex) {
                    throw new SQLException("Invalid startTime format for Section: '" + section.getStartTime() + "'", ex);
                }
            } else {
                ps.setNull(5, Types.TIME);
            }
            if (section.getEndTime() != null) {
                try {
                    ps.setTime(6, Time.valueOf(section.getEndTime()));
                } catch (IllegalArgumentException ex) {
                    throw new SQLException("Invalid endTime format for Section: '" + section.getEndTime() + "'", ex);
                }
            } else {
                ps.setNull(6, Types.TIME);
            }
            ps.setString(7, section.getRoom());
            ps.setInt(8, section.getCapacity());
            ps.setInt(9, section.getEnrolled());
            ps.setString(10, section.getSemester());
            ps.setInt(11, section.getYear());
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                } else {
                    throw new SQLException("Failed to retrieve generated section ID after insert");
                }
            }
        }
    }

    public void update(Section section) throws SQLException {
        String sql = "UPDATE sections SET course_id = ?, instructor_id = ?, section_number = ?, day_of_week = ?, start_time = ?, end_time = ?, room = ?, capacity = ?, enrolled = ?, semester = ?, year = ? WHERE section_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, section.getCourseId());
            if (section.getInstructorId() != null) {
                ps.setLong(2, section.getInstructorId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, section.getSectionNumber());
            ps.setString(4, section.getDayOfWeek());
            if (section.getStartTime() != null) {
                try {
                    ps.setTime(5, Time.valueOf(section.getStartTime()));
                } catch (IllegalArgumentException ex) {
                    throw new SQLException("Invalid startTime format for Section: '" + section.getStartTime() + "'", ex);
                }
            } else {
                ps.setNull(5, Types.TIME);
            }
            if (section.getEndTime() != null) {
                try {
                    ps.setTime(6, Time.valueOf(section.getEndTime()));
                } catch (IllegalArgumentException ex) {
                    throw new SQLException("Invalid endTime format for Section: '" + section.getEndTime() + "'", ex);
                }
            } else {
                ps.setNull(6, Types.TIME);
            }
            ps.setString(7, section.getRoom());
            ps.setInt(8, section.getCapacity());
            ps.setInt(9, section.getEnrolled());
            ps.setString(10, section.getSemester());
            ps.setInt(11, section.getYear());
            ps.setLong(12, section.getSectionId());
            int result = ps.executeUpdate();
            if (result == 0) {
                throw new SQLException("No section exists with sectionId=" + section.getSectionId() + ". Update failed.");
            }
        }
    }

    public void delete(Long sectionId) throws SQLException {
        String sql = "DELETE FROM sections WHERE section_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            int result = ps.executeUpdate();
            if (result == 0) {
                throw new SQLException("No section with id: " + sectionId);
            }
        }
    }

    public void assignInstructor(Long sectionId, Long instructorId) throws SQLException {
        String sql = "UPDATE sections SET instructor_id = ? WHERE section_id = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (instructorId != null) {
                ps.setLong(1, instructorId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setLong(2, sectionId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No section found with id: " + sectionId);
            }
        }
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
