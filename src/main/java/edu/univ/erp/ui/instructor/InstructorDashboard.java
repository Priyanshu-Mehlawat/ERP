package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.auth.UserRole;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.ui.auth.LoginFrame;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Instructor dashboard - main interface for faculty members and administrators.
 * Supports both instructor and admin user roles with appropriate access levels.
 */
public class InstructorDashboard extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(InstructorDashboard.class);
    
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final GradeDAO gradeDAO = new GradeDAO();
    private final SectionDAO sectionDAO = new SectionDAO();
    private final SectionService sectionService = new SectionService();
    private Instructor currentInstructor;
    private boolean isAdminUser = false;

    /**
     * Private constructor to prevent direct instantiation.
     * Use create() factory method instead.
     */
    private InstructorDashboard() {
        // Constructor now only handles basic object initialization
        // No I/O operations performed here
    }
    
    /**
     * Factory method to create and initialize InstructorDashboard.
     * Handles database I/O and UI initialization.
     * 
     * @return initialized InstructorDashboard instance, or null if initialization fails
     */
    public static InstructorDashboard create() {
        InstructorDashboard dashboard = new InstructorDashboard();
        
        try {
            // Perform I/O operations after object construction
            if (!dashboard.loadCurrentInstructor()) {
                logger.error("Failed to load instructor data");
                return null;
            }
            
            // Initialize UI components
            dashboard.initComponents();
            dashboard.setupFrame();
            
            return dashboard;
        } catch (Exception e) {
            logger.error("Failed to create InstructorDashboard", e);
            return null;
        }
    }

    private boolean loadCurrentInstructor() {
        try {
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                logger.warn("Current user is null, cannot load user data");
                return false;
            }
            
            // Check if user is admin - use null-safe comparison
            String userRole = currentUser.getRole();
            if (userRole != null && userRole.equals(UserRole.ADMIN)) {
                isAdminUser = true;
                logger.info("Admin user detected: {}", currentUser.getUsername());
                // Admin users can access instructor features but don't need instructor profile
                return true;
            }
            
            Long userId = currentUser.getUserId();
            if (userId == null) {
                logger.warn("User ID is null, cannot load instructor data");
                return false;
            }
            
            // Load instructor profile for INSTRUCTOR role users - use null-safe comparison
            if (userRole != null && userRole.equals(UserRole.INSTRUCTOR)) {
                currentInstructor = instructorDAO.findByUserId(userId);
                if (currentInstructor == null) {
                    logger.warn("No instructor found for user ID: {}", userId);
                    return false;
                }
                return true;
            } else {
                logger.warn("User role '{}' is not supported in InstructorDashboard", userRole);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error loading current instructor", e);
            return false;
        }
    }

    private void initComponents() {
        var currentUser = SessionManager.getInstance().getCurrentUser();
        String title = isAdminUser ? "Admin Dashboard (Instructor View)" : "Instructor Dashboard";
        
        if (currentInstructor != null) {
            title += " - " + currentInstructor.getFirstName() + " " + currentInstructor.getLastName();
        } else if (currentUser != null) {
            title += " - " + currentUser.getUsername();
            if (isAdminUser) {
                title += " (Administrator)";
            }
        } else {
            title += " - Unknown User";
        }
        setTitle(title);
        setLayout(new BorderLayout());

        // Top panel with welcome message
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel welcomeLabel;
        if (isAdminUser) {
            welcomeLabel = new JLabel("<html><h2>Welcome, Administrator!</h2><small>Instructor features enabled</small></html>");
        } else if (currentInstructor != null) {
            welcomeLabel = new JLabel("<html><h2>Welcome, " + currentInstructor.getFirstName() + "!</h2></html>");
        } else {
            welcomeLabel = new JLabel("<html><h2>Welcome, Instructor!</h2></html>");
        }
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        // Right side buttons panel
        JPanel topButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton changePasswordBtn = new JButton("Change Password");
        changePasswordBtn.addActionListener(e -> openChangePasswordDialog());
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        topButtonsPanel.add(changePasswordBtn);
        topButtonsPanel.add(logoutButton);
        topPanel.add(topButtonsPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with menu buttons
        JPanel centerPanel = new JPanel(new MigLayout("fillx, wrap " + (isAdminUser ? "3" : "2"), "[grow][grow]" + (isAdminUser ? "[grow]" : ""), ""));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Standard instructor menu buttons
        JButton coursesBtn = createMenuButton("My Courses", "Manage course sections");
        coursesBtn.addActionListener(e -> openCourseManagement());

        JButton rosterBtn = createMenuButton("Class Roster", "View enrolled students");
        rosterBtn.addActionListener(e -> openClassRoster());

        JButton gradesBtn = createMenuButton("Grade Entry", "Enter and manage grades");
        gradesBtn.addActionListener(e -> openGradeEntry());

        JButton attendanceBtn = createMenuButton("Attendance", "Track student attendance");
        attendanceBtn.addActionListener(e -> openAttendance());

        JButton reportsBtn = createMenuButton("Reports", "Generate grade reports");
        reportsBtn.addActionListener(e -> openReports());

        JButton scheduleBtn = createMenuButton("My Schedule", "View teaching schedule");
        scheduleBtn.addActionListener(e -> openSchedule());

        centerPanel.add(coursesBtn, "grow");
        centerPanel.add(rosterBtn, "grow");
        centerPanel.add(gradesBtn, "grow");
        centerPanel.add(attendanceBtn, "grow");
        centerPanel.add(reportsBtn, "grow");
        centerPanel.add(scheduleBtn, "grow");

        // Add admin-specific buttons if user is admin
        if (isAdminUser) {
            JButton manageUsersBtn = createAdminMenuButton("Manage Users", "Add/edit system users");
            manageUsersBtn.addActionListener(e -> openUserManagement());

            JButton manageAllCoursesBtn = createAdminMenuButton("All Courses", "Manage all courses");
            manageAllCoursesBtn.addActionListener(e -> openAllCoursesManagement());

            JButton systemSettingsBtn = createAdminMenuButton("System Settings", "Configure system");
            systemSettingsBtn.addActionListener(e -> openSystemSettings());

            centerPanel.add(manageUsersBtn, "grow");
            centerPanel.add(manageAllCoursesBtn, "grow");
            centerPanel.add(systemSettingsBtn, "grow");
        }

        add(centerPanel, BorderLayout.CENTER);

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        String statusText = "Ready";
        if (isAdminUser) {
            statusText += " - Administrative Access";
        } else if (currentInstructor != null) {
            statusText += " - Department: " + currentInstructor.getDepartment();
        }
        JLabel statusBar = new JLabel(statusText);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(statusBar, BorderLayout.WEST);
        
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JButton createMenuButton(String title, String description) {
        JButton button = new JButton("<html><b>" + title + "</b><br><small>" + description + "</small></html>");
        button.setPreferredSize(new Dimension(250, 80));
        button.setFont(button.getFont().deriveFont(14f));
        return button;
    }

    private JButton createAdminMenuButton(String title, String description) {
        JButton button = new JButton("<html><b>🔧 " + title + "</b><br><small>" + description + "</small></html>");
        button.setPreferredSize(new Dimension(250, 80));
        button.setFont(button.getFont().deriveFont(14f));
        button.setBackground(new Color(255, 240, 240)); // Light red background for admin buttons
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 0, 0), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return button;
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void openChangePasswordDialog() {
        new edu.univ.erp.ui.auth.ChangePasswordDialog(this).setVisible(true);
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void openDialog(String title, int width, int height, JPanel panel) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openCourseManagement() {
        openDialog("Course Management", 1000, 700, new CourseManagementPanel());
    }

    private void openClassRoster() {
        openDialog("Class Roster", 900, 600, new ClassRosterPanel());
    }

    private void openGradeEntry() {
        openDialog("Grade Entry", 1100, 700, new GradeEntryPanel());
    }

    private void openAttendance() {
        openDialog("Attendance Tracking", 1000, 600, new AttendancePanel());
    }

    private void openReports() {
        openDialog("Reports", 900, 600, new ReportsPanel(sectionService, instructorDAO, enrollmentDAO, gradeDAO, sectionDAO));
    }

    private void openSchedule() {
        openDialog("Teaching Schedule", 900, 600, new InstructorSchedulePanel(sectionService, instructorDAO));
    }

    // Admin-specific methods
    private void openUserManagement() {
        if (!isAdminUser) {
            JOptionPane.showMessageDialog(this, 
                "Access denied. Administrative privileges required.", 
                "Access Denied", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "User Management", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>🔧 Admin: User Management</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(12, 45);
        infoArea.setEditable(false);
        infoArea.setText("User Management Features:\n\n" +
            "✓ View all system users (Students, Instructors, Admins)\n" +
            "✓ Create new user accounts with appropriate roles\n" +
            "✓ Edit existing user information and profiles\n" +
            "✓ Reset user passwords and account recovery\n" +
            "✓ Activate/deactivate user accounts\n" +
            "✓ Search and filter users by role or department\n" +
            "✓ Manage user permissions and access levels\n" +
            "✓ Generate user activity and access reports\n\n" +
            "This comprehensive user administration system provides\n" +
            "full control over user accounts and system access.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(550, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openAllCoursesManagement() {
        if (!isAdminUser) {
            JOptionPane.showMessageDialog(this, 
                "Access denied. Administrative privileges required.", 
                "Access Denied", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "All Courses Management", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>🔧 Admin: All Courses Management</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(12, 45);
        infoArea.setEditable(false);
        infoArea.setText("All Courses Management Features:\n\n" +
            "✓ Create new courses across all departments\n" +
            "✓ Modify course details, descriptions, and requirements\n" +
            "✓ Set prerequisites and corequisites\n" +
            "✓ Manage course codes and credit assignments\n" +
            "✓ Configure course offerings by semester\n" +
            "✓ Assign qualified instructors to courses\n" +
            "✓ Monitor course enrollment and capacity\n" +
            "✓ Generate comprehensive course catalogs\n\n" +
            "This system-wide course management provides\n" +
            "administrative control over the entire academic catalog.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(550, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openSystemSettings() {
        if (!isAdminUser) {
            JOptionPane.showMessageDialog(this, 
                "Access denied. Administrative privileges required.", 
                "Access Denied", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "System Settings", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>🔧 Admin: System Settings</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(12, 45);
        infoArea.setEditable(false);
        infoArea.setText("System Settings Features:\n\n" +
            "✓ Configure system-wide parameters and policies\n" +
            "✓ Manage database connections and performance\n" +
            "✓ Set up automated backup and maintenance schedules\n" +
            "✓ Configure security policies and access controls\n" +
            "✓ Manage system notifications and alerts\n" +
            "✓ Set academic calendar and semester dates\n" +
            "✓ Configure reporting and analytics settings\n" +
            "✓ Monitor system performance and resource usage\n\n" +
            "This administrative control center provides comprehensive\n" +
            "system configuration and maintenance capabilities.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(550, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
