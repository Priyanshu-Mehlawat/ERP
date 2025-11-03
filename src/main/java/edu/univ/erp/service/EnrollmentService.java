package edu.univ.erp.service;

import edu.univ.erp.auth.PermissionChecker;
import edu.univ.erp.auth.PermissionException;
import edu.univ.erp.auth.SessionManager;
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
    private final SettingsService settingsService = new SettingsService();
    private final PermissionChecker permissionChecker = new PermissionChecker();

    /**
     * Enroll a student into a section with basic validations.
     */
    public synchronized String enroll(Long studentId, Long sectionId) {
        // Check if registration is enabled
        if (!settingsService.isRegistrationEnabled()) {
            return "Registration is currently disabled";
        }
        
        // Check permission - students can only enroll themselves
        try {
            permissionChecker.requireStudentDataAccess(studentId);
        } catch (PermissionException e) {
            return "Permission denied: " + e.getMessage();
        }
        
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
        // Check if registration is enabled (affects drops too)
        if (!settingsService.isRegistrationEnabled()) {
            return "Registration is currently disabled";
        }
        
        // Check permission - students can only drop themselves
        try {
            permissionChecker.requireStudentDataAccess(studentId);
        } catch (PermissionException e) {
            return "Permission denied: " + e.getMessage();
        }
        
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

    public List<Enrollment> listByStudent(Long studentId) { 
        try {
            permissionChecker.requireStudentDataAccess(studentId);
        } catch (PermissionException e) {
            // Return empty list rather than throwing exception to maintain API compatibility
            return List.of();
        }
        return enrollmentDAO.listByStudent(studentId); 
    }

    public List<Enrollment> listBySection(Long sectionId) { 
        if (sectionId == null) {
            return List.of(); // Return empty list for null input
        }
        
        try {
            permissionChecker.requireSectionOwnership(sectionId);
        } catch (PermissionException e) {
            // Return empty list rather than throwing exception to maintain API compatibility
            return List.of();
        }
        
        return enrollmentDAO.listBySection(sectionId); 
    }

    public boolean updateFinalGrade(Long enrollmentId, String finalGrade) {
        try {
            permissionChecker.requireEnrollmentOwnership(enrollmentId);
        } catch (PermissionException e) {
            return false; // Return false to indicate failure
        }
        
        try {
            return enrollmentDAO.updateFinalGrade(enrollmentId, finalGrade);
        } catch (SQLException e) {
            return false;
        }
    }
}
