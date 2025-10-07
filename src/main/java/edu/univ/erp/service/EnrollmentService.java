package edu.univ.erp.service;

import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Section;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import edu.univ.erp.data.DatabaseConnection;

public class EnrollmentService {
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final SectionDAO sectionDAO = new SectionDAO();

    /**
     * Enroll a student into a section with basic validations.
     */
    public synchronized String enroll(Long studentId, Long sectionId) {
        try {
            // Validate not already enrolled
            if (enrollmentDAO.find(studentId, sectionId) != null) {
                return "Already enrolled in this section";
            }
            // Validate section has seat
            Section section = sectionDAO.findById(sectionId);
            if (section == null) return "Section not found";
            if (!section.hasAvailableSeats()) return "Section is full";

            // Transaction-like sequence (same connection for atomicity)
            try (Connection conn = DatabaseConnection.getErpConnection()) {
                try {
                    conn.setAutoCommit(false);
                    Long enrollmentId = enrollmentDAO.create(studentId, sectionId);
                    boolean incremented = sectionDAO.incrementEnrolled(sectionId);
                    if (enrollmentId == null || !incremented) {
                        conn.rollback();
                        return "Failed to enroll (capacity changed)";
                    }
                    conn.commit();
                    return "ENROLLED";
                } catch (Exception ex) {
                    conn.rollback();
                    return "Enrollment failed: " + ex.getMessage();
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String drop(Long studentId, Long sectionId) {
        try {
            Enrollment enrollment = enrollmentDAO.find(studentId, sectionId);
            if (enrollment == null) return "Not enrolled";
            if (!"ENROLLED".equals(enrollment.getStatus())) return "Cannot drop (status: " + enrollment.getStatus() + ")";
            try (Connection conn = DatabaseConnection.getErpConnection()) {
                try {
                    conn.setAutoCommit(false);
                    boolean dropped = enrollmentDAO.markDropped(enrollment.getEnrollmentId());
                    boolean decremented = sectionDAO.decrementEnrolled(enrollment.getSectionId());
                    if (!dropped || !decremented) { conn.rollback(); return "Drop failed"; }
                    conn.commit();
                    return "DROPPED";
                } catch (Exception ex) {
                    conn.rollback();
                    return "Error dropping: " + ex.getMessage();
                } finally { conn.setAutoCommit(true); }
            }
        } catch (SQLException e) { return "Error: " + e.getMessage(); }
    }

    public List<Enrollment> listByStudent(Long studentId) { return enrollmentDAO.listByStudent(studentId); }

    public List<Enrollment> listBySection(Long sectionId) { return enrollmentDAO.listBySection(sectionId); }

    public boolean updateFinalGrade(Long enrollmentId, String finalGrade) {
        try {
            return enrollmentDAO.updateFinalGrade(enrollmentId, finalGrade);
        } catch (SQLException e) {
            return false;
        }
    }
}
