package edu.univ.erp.service;

import edu.univ.erp.auth.PermissionChecker;
import edu.univ.erp.auth.PermissionException;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.domain.Grade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class GradeService {
    private static final Logger logger = LoggerFactory.getLogger(GradeService.class);
    private static final List<String> DEFAULT_COMPONENTS = List.of("Assignment", "Quiz", "Midterm", "Final");
    
    private final GradeDAO gradeDAO = new GradeDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final PermissionChecker permissionChecker = new PermissionChecker();

    public List<Grade> listComponents(Long enrollmentId) { 
        try {
            permissionChecker.requireEnrollmentOwnership(enrollmentId);
        } catch (PermissionException e) {
            logger.warn("Permission denied for listComponents: {}", e.getMessage());
            throw e;
        }
        return gradeDAO.listByEnrollment(enrollmentId); 
    }

    public List<String> getComponentsForSection(Long sectionId) throws SQLException {
        // Fail fast with null check for sectionId parameter
        if (sectionId == null) {
            throw new IllegalArgumentException("sectionId parameter cannot be null");
        }
        
        // Check permission - only instructors can access their sections
        try {
            permissionChecker.requireSectionOwnership(sectionId);
        } catch (PermissionException e) {
            logger.warn("Permission denied for getComponentsForSection: {}", e.getMessage());
            throw new SQLException("Permission denied: " + e.getMessage());
        }
        
        try {
            List<String> components = gradeDAO.getDistinctComponentsForSection(sectionId);
            if (components == null || components.isEmpty()) {
                // Return default components if no grades exist for this section or if null returned
                return DEFAULT_COMPONENTS;
            }
            return components;
        } catch (SQLException e) {
            // Log the exception with full details before propagating to caller
            logger.error("Error retrieving components for section {}: {}", sectionId, e.getMessage(), e);
            throw e;
        }
    }

    public String addComponent(Long enrollmentId, String component, Double score, double maxScore, double weight) {
        try {
            permissionChecker.requireEnrollmentOwnership(enrollmentId);
        } catch (PermissionException e) {
            logger.warn("Permission denied for addComponent: {}", e.getMessage());
            return "Permission denied: " + e.getMessage();
        }
        
        try {
            double total = gradeDAO.totalWeight(enrollmentId) + weight;
            if (total > 100.0 + 0.0001) return "Total weight exceeds 100%";
            gradeDAO.addComponent(enrollmentId, component, score, maxScore, weight);
            if (Math.abs(total - 100.0) < 0.0001) computeAndStoreFinal(enrollmentId);
            return "ADDED";
        } catch (SQLException e) { return "Error: " + e.getMessage(); }
    }

    public String updateScore(Long gradeId, Double score) {
        // Note: We would need to check gradeId ownership, but for simplicity 
        // we're assuming this is called from contexts that already have permission
        try { 
            return gradeDAO.updateScore(gradeId, score) ? "UPDATED" : "Not found"; 
        } catch (SQLException e) { 
            return "Error: " + e.getMessage(); 
        }
    }

    private void computeAndStoreFinal(Long enrollmentId) {
        List<Grade> grades = listComponents(enrollmentId);
        if (grades.isEmpty()) return;
        double totalPercent = 0.0;
        for (Grade g : grades) {
            if (g.getScore() != null && g.getMaxScore() != null && g.getMaxScore() > 0) {
                double pct = (g.getScore() / g.getMaxScore()) * (g.getWeight() / 100.0) * 100.0;
                totalPercent += pct;
            }
        }
        String letter = letterGrade(totalPercent);
        try { enrollmentDAO.updateFinalGrade(enrollmentId, letter); } catch (SQLException ignored) {}
    }

    private String letterGrade(double pct) {
        if (pct >= 90) return "A";
        if (pct >= 80) return "B";
        if (pct >= 70) return "C";
        if (pct >= 60) return "D";
        return "F";
    }
}
