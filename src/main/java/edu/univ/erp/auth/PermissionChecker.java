package edu.univ.erp.auth;

import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Utility class for checking user permissions.
 */
public class PermissionChecker {
    private static final Logger logger = LoggerFactory.getLogger(PermissionChecker.class);
    private final SectionDAO sectionDAO = new SectionDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    /**
     * Check if the current user is an admin.
     * 
     * @throws PermissionException if user is not an admin
     */
    public void requireAdmin() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            logger.warn("Permission check failed: No user logged in");
            throw new PermissionException("You must be logged in to perform this action");
        }
        
        if (!UserRole.ADMIN.equals(user.getRole())) {
            logger.warn("Permission denied: User {} (role: {}) attempted admin-only operation", 
                    user.getUsername(), user.getRole());
            throw new PermissionException("This operation requires administrator privileges");
        }
    }

    /**
     * Check if the current user (instructor) is the owner of the specified section.
     * 
     * @param sectionId The section ID to check ownership for
     * @throws PermissionException if user is not the instructor of this section
     */
    public void requireSectionOwnership(Long sectionId) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new PermissionException("You must be logged in to perform this action");
        }

        // Admins bypass ownership checks
        if (UserRole.ADMIN.equals(user.getRole())) {
            return;
        }

        // For instructors, verify they own this section
        if (UserRole.INSTRUCTOR.equals(user.getRole())) {
            Long instructorId = SessionManager.getInstance().getInstructorId();
            if (instructorId == null) {
                logger.error("Instructor user {} has no instructor ID in session", user.getUsername());
                throw new PermissionException("Instructor profile not found");
            }

            try {
                Section section = sectionDAO.findById(sectionId);
                if (section == null) {
                    throw new PermissionException("Section not found");
                }

                if (!instructorId.equals(section.getInstructorId())) {
                    logger.warn("Permission denied: Instructor {} attempted to access section {} owned by instructor {}", 
                            instructorId, sectionId, section.getInstructorId());
                    throw new PermissionException("You can only access sections you are teaching");
                }
            } catch (SQLException e) {
                logger.error("Error checking section ownership for section {}", sectionId, e);
                throw new PermissionException("Error verifying permissions: " + e.getMessage());
            }
        } else {
            // Students and other roles cannot access instructor operations
            logger.warn("Permission denied: User {} (role: {}) attempted instructor operation on section {}", 
                    user.getUsername(), user.getRole(), sectionId);
            throw new PermissionException("You do not have permission to access this section");
        }
    }

    /**
     * Check if the current user (student) owns the specified enrollment.
     * 
     * @param enrollmentId The enrollment ID to check ownership for
     * @throws PermissionException if user is not the student of this enrollment
     */
    public void requireEnrollmentOwnership(Long enrollmentId) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new PermissionException("You must be logged in to perform this action");
        }

        // Admins bypass ownership checks
        if (UserRole.ADMIN.equals(user.getRole())) {
            return;
        }

        // For instructors, check if they own the section this enrollment belongs to
        if (UserRole.INSTRUCTOR.equals(user.getRole())) {
            Long instructorId = SessionManager.getInstance().getInstructorId();
            if (instructorId == null) {
                throw new PermissionException("Instructor profile not found");
            }

            try {
                Enrollment enrollment = enrollmentDAO.findById(enrollmentId);
                if (enrollment == null) {
                    throw new PermissionException("Enrollment not found");
                }

                Section section = sectionDAO.findById(enrollment.getSectionId());
                if (section == null || !instructorId.equals(section.getInstructorId())) {
                    logger.warn("Permission denied: Instructor {} attempted to access enrollment {} in section {}", 
                            instructorId, enrollmentId, enrollment.getSectionId());
                    throw new PermissionException("You can only access enrollments in sections you are teaching");
                }
            } catch (SQLException e) {
                logger.error("Error checking enrollment ownership for enrollment {}", enrollmentId, e);
                throw new PermissionException("Error verifying permissions: " + e.getMessage());
            }
            return;
        }

        // For students, verify they own this enrollment
        if (UserRole.STUDENT.equals(user.getRole())) {
            Long studentId = SessionManager.getInstance().getStudentId();
            if (studentId == null) {
                logger.error("Student user {} has no student ID in session", user.getUsername());
                throw new PermissionException("Student profile not found");
            }

            try {
                Enrollment enrollment = enrollmentDAO.findById(enrollmentId);
                if (enrollment == null) {
                    throw new PermissionException("Enrollment not found");
                }

                if (!studentId.equals(enrollment.getStudentId())) {
                    logger.warn("Permission denied: Student {} attempted to access enrollment {} owned by student {}", 
                            studentId, enrollmentId, enrollment.getStudentId());
                    throw new PermissionException("You can only access your own enrollment records");
                }
            } catch (SQLException e) {
                logger.error("Error checking enrollment ownership for enrollment {}", enrollmentId, e);
                throw new PermissionException("Error verifying permissions: " + e.getMessage());
            }
        } else {
            // Other roles cannot access enrollments
            logger.warn("Permission denied: User {} (role: {}) attempted to access enrollment {}", 
                    user.getUsername(), user.getRole(), enrollmentId);
            throw new PermissionException("You do not have permission to access this enrollment");
        }
    }

    /**
     * Check if the current user (student) can access data for the specified student ID.
     * 
     * @param studentId The student ID to check access for
     * @throws PermissionException if user cannot access this student's data
     */
    public void requireStudentDataAccess(Long studentId) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new PermissionException("You must be logged in to perform this action");
        }

        // Admins can access all student data
        if (UserRole.ADMIN.equals(user.getRole())) {
            return;
        }

        // Instructors can access their students' data (checked elsewhere via section ownership)
        if (UserRole.INSTRUCTOR.equals(user.getRole())) {
            return;
        }

        // Students can only access their own data
        if (UserRole.STUDENT.equals(user.getRole())) {
            Long currentStudentId = SessionManager.getInstance().getStudentId();
            if (currentStudentId == null) {
                logger.error("Student user {} has no student ID in session", user.getUsername());
                throw new PermissionException("Student profile not found");
            }

            if (!currentStudentId.equals(studentId)) {
                logger.warn("Permission denied: Student {} attempted to access data for student {}", 
                        currentStudentId, studentId);
                throw new PermissionException("You can only access your own student data");
            }
        } else {
            // Other roles cannot access student data
            logger.warn("Permission denied: User {} (role: {}) attempted to access student data for student {}", 
                    user.getUsername(), user.getRole(), studentId);
            throw new PermissionException("You do not have permission to access student data");
        }
    }

    /**
     * Check if the current user is an instructor.
     * 
     * @throws PermissionException if user is not an instructor or admin
     */
    public void requireInstructor() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new PermissionException("You must be logged in to perform this action");
        }

        if (!UserRole.INSTRUCTOR.equals(user.getRole()) && !UserRole.ADMIN.equals(user.getRole())) {
            logger.warn("Permission denied: User {} (role: {}) attempted instructor operation", 
                    user.getUsername(), user.getRole());
            throw new PermissionException("This operation requires instructor privileges");
        }
    }

    /**
     * Check if the current user is a student.
     * 
     * @throws PermissionException if user is not a student
     */
    public void requireStudent() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new PermissionException("You must be logged in to perform this action");
        }

        if (!UserRole.STUDENT.equals(user.getRole())) {
            logger.warn("Permission denied: User {} (role: {}) attempted student operation", 
                    user.getUsername(), user.getRole());
            throw new PermissionException("This operation requires student privileges");
        }
    }
}
