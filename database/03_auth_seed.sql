-- =====================================================
-- Seed Data for Auth Database
-- Sample users: admin1, inst1, stu1, stu2
-- Default password for all: "password123"
-- =====================================================

USE erp_auth;

-- Clear existing data (for testing)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE password_history;
TRUNCATE TABLE users_auth;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert sample users
-- Password: "password123" (hashed with BCrypt)
-- Hash: $2a$10$ZiL3d8W2Q4tYWXRmHQvHvOx3L5KN8xJ8YqKfXNmB4xO3Lj8YqKfXN (example)
-- Note: You'll need to generate actual BCrypt hashes in your application

INSERT INTO users_auth (user_id, username, role, password_hash, status, failed_login_attempts) VALUES
(1, 'admin1', 'ADMIN', '$2a$10$Q16DVbv.0cgmwrTT3cmMOe9Of8Bvc5EhHBHGzv7nZahn2iiTycxCy', 'ACTIVE', 0),
(2, 'inst1', 'INSTRUCTOR', '$2a$10$Q16DVbv.0cgmwrTT3cmMOe9Of8Bvc5EhHBHGzv7nZahn2iiTycxCy', 'ACTIVE', 0),
(3, 'stu1', 'STUDENT', '$2a$10$Q16DVbv.0cgmwrTT3cmMOe9Of8Bvc5EhHBHGzv7nZahn2iiTycxCy', 'ACTIVE', 0),
(4, 'stu2', 'STUDENT', '$2a$10$Q16DVbv.0cgmwrTT3cmMOe9Of8Bvc5EhHBHGzv7nZahn2iiTycxCy', 'ACTIVE', 0);

-- Note: The password hash above is for "password123"
-- In production, always generate fresh hashes using BCrypt
