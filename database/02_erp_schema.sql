-- =====================================================
-- ERP Database Schema for University ERP System
-- Database: erp_main
-- Purpose: Store all application data (students, courses, etc.)
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS erp_main;
USE erp_main;

-- Students table
CREATE TABLE IF NOT EXISTS students (
    student_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    roll_no VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    program VARCHAR(100) NOT NULL,
    year INT NOT NULL CHECK (year BETWEEN 1 AND 6),
    phone_number VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_roll_no (roll_no),
    INDEX idx_program (program)
) ENGINE=InnoDB;

-- Instructors table
CREATE TABLE IF NOT EXISTS instructors (
    instructor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    employee_id VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    department VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_department (department)
) ENGINE=InnoDB;

-- Courses table
CREATE TABLE IF NOT EXISTS courses (
    course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    credits INT NOT NULL CHECK (credits > 0),
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_department (department)
) ENGINE=InnoDB;

-- Sections table
CREATE TABLE IF NOT EXISTS sections (
    section_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    instructor_id BIGINT,
    section_number VARCHAR(10) NOT NULL,
    day_of_week VARCHAR(50),
    start_time TIME,
    end_time TIME,
    room VARCHAR(50),
    capacity INT NOT NULL CHECK (capacity > 0),
    enrolled INT DEFAULT 0 CHECK (enrolled >= 0),
    semester ENUM('Fall', 'Spring', 'Summer') NOT NULL,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES instructors(instructor_id) ON DELETE SET NULL,
    UNIQUE KEY unique_section (course_id, section_number, semester, year),
    INDEX idx_semester_year (semester, year),
    INDEX idx_instructor_id (instructor_id),
    INDEX idx_course_id (course_id),
    CONSTRAINT check_capacity CHECK (enrolled <= capacity)
) ENGINE=InnoDB;

-- Enrollments table
CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    status ENUM('ENROLLED', 'DROPPED', 'COMPLETED') DEFAULT 'ENROLLED',
    enrolled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dropped_date TIMESTAMP NULL,
    final_grade VARCHAR(5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES sections(section_id) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (student_id, section_id),
    INDEX idx_student_id (student_id),
    INDEX idx_section_id (section_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Grades table (assessment components)
CREATE TABLE IF NOT EXISTS grades (
    grade_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    component VARCHAR(50) NOT NULL,
    score DECIMAL(5,2),
    max_score DECIMAL(5,2) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
    INDEX idx_enrollment_id (enrollment_id),
    CONSTRAINT check_score CHECK (score IS NULL OR (score >= 0 AND score <= max_score)),
    CONSTRAINT check_weight CHECK (weight >= 0 AND weight <= 100)
) ENGINE=InnoDB;

-- Settings table (for maintenance mode, etc.)
CREATE TABLE IF NOT EXISTS settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Insert default settings
INSERT INTO settings (setting_key, setting_value) VALUES
    ('maintenance_mode', 'false'),
    ('current_semester', 'Fall'),
    ('current_year', '2025'),
    ('drop_deadline_days', '7'),
    ('registration_enabled', 'true')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
