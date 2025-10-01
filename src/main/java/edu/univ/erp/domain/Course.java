package edu.univ.erp.domain;

/**
 * Course entity representing courses in the ERP DB.
 */
public class Course {
    private Long courseId;
    private String code; // e.g., "CSE201"
    private String title;
    private String description;
    private int credits;
    private String department;

    public Course() {
    }

    public Course(String code, String title, int credits) {
        this.code = code;
        this.title = title;
        this.credits = credits;
    }

    // Getters and Setters
    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId=" + courseId +
                ", code='" + code + '\'' +
                ", title='" + title + '\'' +
                ", credits=" + credits +
                '}';
    }
}
