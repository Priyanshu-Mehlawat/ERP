package edu.univ.erp.domain;

import java.time.LocalTime;

/**
 * Section entity representing class sections in the ERP DB.
 */
public class Section {
    private Long sectionId;
    private Long courseId;
    private Long instructorId;
    private String sectionNumber; // e.g., "A", "B", "L1"
    private String dayOfWeek; // e.g., "Monday", "Tuesday,Thursday"
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private int capacity;
    private int enrolled; // Current enrollment count
    private String semester; // e.g., "Fall", "Spring"
    private int year;

    // Joined fields (not stored in DB, loaded via joins)
    private String courseCode;
    private String courseTitle;
    private String instructorName;

    public Section() {
    }

    public Section(Long courseId, Long instructorId, String sectionNumber, String semester, int year) {
        this.courseId = courseId;
        this.instructorId = instructorId;
        this.sectionNumber = sectionNumber;
        this.semester = semester;
        this.year = year;
        this.enrolled = 0;
    }

    // Getters and Setters
    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public String getSectionNumber() {
        return sectionNumber;
    }

    public void setSectionNumber(String sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getEnrolled() {
        return enrolled;
    }

    public void setEnrolled(int enrolled) {
        this.enrolled = enrolled;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
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

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public boolean hasAvailableSeats() {
        return enrolled < capacity;
    }

    public int getAvailableSeats() {
        return Math.max(0, capacity - enrolled);
    }

    @Override
    public String toString() {
        return "Section{" +
                "sectionId=" + sectionId +
                ", courseCode='" + courseCode + '\'' +
                ", section='" + sectionNumber + '\'' +
                ", enrolled=" + enrolled + "/" + capacity +
                ", semester='" + semester + " " + year + '\'' +
                '}';
    }
}
