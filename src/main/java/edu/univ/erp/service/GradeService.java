package edu.univ.erp.service;

import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.domain.Grade;

import java.sql.SQLException;
import java.util.List;

public class GradeService {
    private final GradeDAO gradeDAO = new GradeDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public List<Grade> listComponents(Long enrollmentId) { return gradeDAO.listByEnrollment(enrollmentId); }

    public String addComponent(Long enrollmentId, String component, Double score, double maxScore, double weight) {
        try {
            double total = gradeDAO.totalWeight(enrollmentId) + weight;
            if (total > 100.0 + 0.0001) return "Total weight exceeds 100%";
            gradeDAO.addComponent(enrollmentId, component, score, maxScore, weight);
            if (Math.abs(total - 100.0) < 0.0001) computeAndStoreFinal(enrollmentId);
            return "ADDED";
        } catch (SQLException e) { return "Error: " + e.getMessage(); }
    }

    public String updateScore(Long gradeId, Double score) {
        try { return gradeDAO.updateScore(gradeId, score) ? "UPDATED" : "Not found"; } catch (SQLException e) { return "Error: " + e.getMessage(); }
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
