package edu.univ.erp.domain;

/**
 * Grade entity representing assessment scores and final grades.
 */
public class Grade {
    private Long gradeId;
    private Long enrollmentId;
    private String component; // e.g., "Quiz", "Midterm", "End-Sem", "Final"
    private Double score;
    private Double maxScore;
    private Double weight; // Percentage weight (e.g., 20.0 for 20%)

    public Grade() {
    }

    public Grade(Long enrollmentId, String component, Double maxScore, Double weight) {
        this.enrollmentId = enrollmentId;
        this.component = component;
        this.maxScore = maxScore;
        this.weight = weight;
    }

    // Getters and Setters
    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getPercentage() {
        if (score == null || maxScore == null || maxScore == 0) {
            return null;
        }
        return (score / maxScore) * 100.0;
    }

    public Double getWeightedScore() {
        if (score == null || maxScore == null || weight == null || maxScore == 0) {
            return null;
        }
        return (score / maxScore) * weight;
    }

    @Override
    public String toString() {
        return "Grade{" +
                "component='" + component + '\'' +
                ", score=" + score + "/" + maxScore +
                ", weight=" + weight + "%" +
                '}';
    }
}
