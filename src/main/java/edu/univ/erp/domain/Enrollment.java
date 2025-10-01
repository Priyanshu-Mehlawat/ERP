package edu.univ.erp.domain;

import java.time.LocalDateTime;

/**
 * Enrollment entity representing student enrollments in sections.
 */
public class Enrollment {
    private Long enrollmentId;
    private Long studentId;
    private Long sectionId;
    private String status; // ENROLLED, DROPPED, COMPLETED
    private LocalDateTime enrolledDate;
    private LocalDateTime droppedDate;
    private String finalGrade; // A, A-, B+, B, etc.

    // Joined fields for display
    private String courseCode;
    private String courseTitle;
    private String sectionNumber;
    private String instructorName;

    public Enrollment() {
    }

    public Enrollment(Long studentId, Long sectionId) {
        this.studentId = studentId;
        this.sectionId = sectionId;
        this.status = "ENROLLED";
        this.enrolledDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEnrolledDate() {
        return enrolledDate;
    }

    public void setEnrolledDate(LocalDateTime enrolledDate) {
        this.enrolledDate = enrolledDate;
    }

    public LocalDateTime getDroppedDate() {
        return droppedDate;
    }

    public void setDroppedDate(LocalDateTime droppedDate) {
        this.droppedDate = droppedDate;
    }

    public String getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(String finalGrade) {
        this.finalGrade = finalGrade;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getSectionNumber() {
        return sectionNumber;
    }

    public void setSectionNumber(String sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", sectionId=" + sectionId +
                ", status='" + status + '\'' +
                ", finalGrade='" + finalGrade + '\'' +
                '}';
    }
}
