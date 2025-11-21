package edu.univ.erp.service;

import edu.univ.erp.auth.PermissionChecker;
import edu.univ.erp.auth.PermissionException;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.data.DatabaseConnection;

public class EnrollmentService {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

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

        // Check add/drop deadline
        try {
            Settings settings = settingsService.getSettings();
            LocalDateTime addDropDeadline = settings.getAddDropDeadline();
            if (addDropDeadline != null && LocalDateTime.now().isAfter(addDropDeadline)) {
                logger.info("Enrollment blocked for student {} - add/drop deadline passed", studentId);
                return "Add/drop deadline has passed. Registration is closed.";
            }
        } catch (Exception e) {
            logger.warn("Error checking add/drop deadline, allowing enrollment to proceed", e);
            // Continue with enrollment if deadline check fails (fail-safe approach)
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

        // Check add/drop deadline
        try {
            Settings settings = settingsService.getSettings();
            LocalDateTime addDropDeadline = settings.getAddDropDeadline();
            if (addDropDeadline != null && LocalDateTime.now().isAfter(addDropDeadline)) {
                logger.info("Drop blocked for student {} - add/drop deadline passed", studentId);
                return "Add/drop deadline has passed. Cannot drop courses.";
            }
        } catch (Exception e) {
            logger.warn("Error checking add/drop deadline, allowing drop to proceed", e);
            // Continue with drop if deadline check fails (fail-safe approach)
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
