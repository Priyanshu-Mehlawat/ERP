-- =====================================================
-- Seed Data for ERP Database
-- Sample data for students, instructors, courses, sections
-- =====================================================

USE erp_main;

-- Clear existing data (for testing)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE grades;
TRUNCATE TABLE enrollments;
TRUNCATE TABLE sections;
TRUNCATE TABLE courses;
TRUNCATE TABLE instructors;
TRUNCATE TABLE students;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert Students (linked to user_id from auth DB)
INSERT INTO students (student_id, user_id, roll_no, first_name, last_name, email, program, year, phone_number) VALUES
(1, 3, '2021001', 'Alice', 'Johnson', 'alice.j@university.edu', 'B.Tech Computer Science', 3, '9876543210'),
(2, 4, '2021002', 'Bob', 'Smith', 'bob.s@university.edu', 'B.Tech Computer Science', 3, '9876543211');

-- Insert Instructors (linked to user_id from auth DB)
INSERT INTO instructors (instructor_id, user_id, employee_id, first_name, last_name, email, department, phone_number) VALUES
(1, 2, 'EMP001', 'Dr. Jane', 'Doe', 'jane.doe@university.edu', 'Computer Science', '9876543220');

-- Insert Courses
INSERT INTO courses (course_id, code, title, description, credits, department) VALUES
(1, 'CSE101', 'Introduction to Programming', 'Basic programming concepts using Java', 4, 'Computer Science'),
(2, 'CSE201', 'Data Structures', 'Study of fundamental data structures', 4, 'Computer Science'),
(3, 'CSE301', 'Database Systems', 'Introduction to database design and SQL', 3, 'Computer Science'),
(4, 'MAT101', 'Calculus I', 'Differential and integral calculus', 4, 'Mathematics'),
(5, 'PHY101', 'Physics I', 'Mechanics and thermodynamics', 4, 'Physics');

-- Insert Sections (Fall 2025)
INSERT INTO sections (section_id, course_id, instructor_id, section_number, day_of_week, start_time, end_time, room, capacity, enrolled, semester, year) VALUES
(1, 1, 1, 'A', 'Monday,Wednesday', '09:00:00', '10:30:00', 'Room 101', 40, 0, 'Fall', 2025),
(2, 1, 1, 'B', 'Tuesday,Thursday', '11:00:00', '12:30:00', 'Room 102', 40, 0, 'Fall', 2025),
(3, 2, 1, 'A', 'Monday,Wednesday', '14:00:00', '15:30:00', 'Room 201', 35, 0, 'Fall', 2025),
(4, 3, 1, 'A', 'Tuesday,Thursday', '09:00:00', '10:30:00', 'Room 301', 30, 0, 'Fall', 2025),
(5, 4, NULL, 'A', 'Monday,Wednesday', '11:00:00', '12:30:00', 'Room 401', 50, 0, 'Fall', 2025),
(6, 5, NULL, 'A', 'Tuesday,Thursday', '14:00:00', '15:30:00', 'Room 501', 45, 0, 'Fall', 2025);

-- Insert Sample Enrollments (student 1 enrolled in 2 courses)
INSERT INTO enrollments (student_id, section_id, status, enrolled_date) VALUES
(1, 1, 'ENROLLED', NOW()),
(1, 3, 'ENROLLED', NOW());

-- Update enrolled count in sections
UPDATE sections SET enrolled = 1 WHERE section_id IN (1, 3);

-- Insert sample grades for enrolled sections
-- Section 1 (CSE101-A) - Student 1
INSERT INTO grades (enrollment_id, component, score, max_score, weight) VALUES
(1, 'Quiz', 18.0, 20.0, 20.0),
(1, 'Midterm', 42.0, 50.0, 30.0),
(1, 'End-Sem', NULL, 100.0, 50.0);

-- Section 3 (CSE201-A) - Student 1
INSERT INTO grades (enrollment_id, component, score, max_score, weight) VALUES
(2, 'Quiz', NULL, 20.0, 20.0),
(2, 'Midterm', NULL, 50.0, 30.0),
(2, 'End-Sem', NULL, 100.0, 50.0);
