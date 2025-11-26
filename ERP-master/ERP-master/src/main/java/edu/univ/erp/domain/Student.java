package edu.univ.erp.domain;

/**
 * Student entity representing student profiles in the ERP DB.
 */
public class Student {
    private Long studentId;
    private Long userId;
    private String rollNo;
    private String firstName;
    private String lastName;
    private String email;
    private String program; // e.g., "B.Tech CSE", "M.Tech AI"
    private int year; // 1, 2, 3, 4
    private String phoneNumber;

    public Student() {
    }

    public Student(Long userId, String rollNo, String firstName, String lastName, String program, int year) {
        this.userId = userId;
        this.rollNo = rollNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.program = program;
        this.year = year;
    }

    // Getters and Setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId=" + studentId +
                ", rollNo='" + rollNo + '\'' +
                ", name='" + getFullName() + '\'' +
                ", program='" + program + '\'' +
                ", year=" + year +
                '}';
    }
}
