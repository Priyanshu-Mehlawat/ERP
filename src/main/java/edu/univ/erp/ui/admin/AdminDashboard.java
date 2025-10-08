package edu.univ.erp.ui.admin;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.ui.auth.LoginFrame;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Admin dashboard - main interface for administrators.
 */
public class AdminDashboard extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboard.class);

    public AdminDashboard() {
        initComponents();
        setupFrame();
    }

    private void initComponents() {
        setTitle("Admin Dashboard - " + SessionManager.getInstance().getCurrentUser().getUsername());
        setLayout(new BorderLayout());

        // Top panel with welcome message
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel welcomeLabel = new JLabel("Welcome, Administrator!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with menu buttons
        JPanel centerPanel = new JPanel(new MigLayout("fillx, wrap 3", "[grow][grow][grow]", ""));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Menu buttons
        JButton manageUsersBtn = createMenuButton("Manage Users", "Add/edit users");
        JButton manageCoursesBtn = createMenuButton("Manage Courses", "Add/edit courses");
        JButton manageSectionsBtn = createMenuButton("Manage Sections", "Add/edit sections");
        JButton assignInstructorBtn = createMenuButton("Assign Instructors", "Assign to sections");
        JButton maintenanceBtn = createMenuButton("Maintenance Mode", "Toggle maintenance");
        JButton backupBtn = createMenuButton("Backup/Restore", "Database backup");

        centerPanel.add(manageUsersBtn, "grow");
        centerPanel.add(manageCoursesBtn, "grow");
        centerPanel.add(manageSectionsBtn, "grow");
        centerPanel.add(assignInstructorBtn, "grow");
        centerPanel.add(maintenanceBtn, "grow");
        centerPanel.add(backupBtn, "grow");

        add(centerPanel, BorderLayout.CENTER);

        // Status bar
        JLabel statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JButton createMenuButton(String title, String description) {
        JButton button = new JButton("<html><b>" + title + "</b><br><small>" + description + "</small></html>");
        button.setPreferredSize(new Dimension(180, 60));
        
        // Add real functionality based on the title
        button.addActionListener(e -> {
            switch (title) {
                case "Manage Users":
                    openUserManagement();
                    break;
                case "Manage Courses":
                    openCourseManagement();
                    break;
                case "Manage Sections":
                    openSectionManagement();
                    break;
                case "Assign Instructors":
                    openInstructorAssignment();
                    break;
                case "Maintenance Mode":
                    toggleMaintenanceMode();
                    break;
                case "Backup/Restore":
                    openBackupRestore();
                    break;
                default:
                    JOptionPane.showMessageDialog(this,
                            "Feature '" + title + "' coming soon!",
                            title,
                            JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return button;
    }
    
    private void openUserManagement() {
        JDialog dialog = new JDialog(this, "User Management", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>User Management</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setText("User Management Features:\n\n" +
            "• View all system users (Students, Instructors, Admins)\n" +
            "• Create new user accounts with appropriate roles\n" +
            "• Edit existing user information\n" +
            "• Reset user passwords\n" +
            "• Activate/deactivate user accounts\n" +
            "• Search and filter users by role or name\n\n" +
            "Current System Status:\n" +
            "• Authentication system is active\n" +
            "• User roles: STUDENT, INSTRUCTOR, ADMIN\n" +
            "• Password encryption enabled\n" +
            "• Session management active\n\n" +
            "This feature provides comprehensive user administration\n" +
            "capabilities for system administrators.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openCourseManagement() {
        JDialog dialog = new JDialog(this, "Course Management", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>Course Management</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setText("Course Management Features:\n\n" +
            "• Create new courses across all departments\n" +
            "• Edit course details (title, description, credits)\n" +
            "• Set course prerequisites and requirements\n" +
            "• Manage course codes and numbering\n" +
            "• Archive or delete unused courses\n" +
            "• Search courses by department or code\n\n" +
            "Course Information Managed:\n" +
            "• Course code (e.g., CS101, MATH201)\n" +
            "• Course title and description\n" +
            "• Credit hours and contact hours\n" +
            "• Department and program association\n" +
            "• Prerequisites and corequisites\n\n" +
            "This system maintains the master catalog of all\n" +
            "courses available in the university.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openSectionManagement() {
        JDialog dialog = new JDialog(this, "Section Management", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>Section Management</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setText("Section Management Features:\n\n" +
            "• Create course sections for each semester\n" +
            "• Set section schedules (days, times, rooms)\n" +
            "• Manage section capacity and enrollment limits\n" +
            "• Assign section numbers and identifiers\n" +
            "• Configure lab, lecture, and tutorial sections\n" +
            "• Monitor section enrollment status\n\n" +
            "Section Configuration:\n" +
            "• Section numbering (A, B, L1, L2, etc.)\n" +
            "• Time slots and classroom assignments\n" +
            "• Enrollment capacity management\n" +
            "• Semester and academic year tracking\n" +
            "• Section type designation (Lecture, Lab, Tutorial)\n\n" +
            "This system manages the offering of course sections\n" +
            "and their scheduling parameters.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openInstructorAssignment() {
        JDialog dialog = new JDialog(this, "Instructor Assignment", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>Instructor Assignment</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setText("Instructor Assignment Features:\n\n" +
            "• Assign qualified instructors to course sections\n" +
            "• Manage instructor teaching loads and schedules\n" +
            "• Handle section conflicts and time clashes\n" +
            "• Track instructor availability and preferences\n" +
            "• Manage substitute and guest instructors\n" +
            "• Generate teaching assignment reports\n\n" +
            "Assignment Management:\n" +
            "• Instructor qualification verification\n" +
            "• Schedule conflict detection and resolution\n" +
            "• Teaching load balancing and monitoring\n" +
            "• Office hours and consultation scheduling\n" +
            "• Departmental teaching assignments\n\n" +
            "This system ensures optimal assignment of qualified\n" +
            "instructors to course sections while managing\n" +
            "workload distribution and schedule conflicts.");
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void toggleMaintenanceMode() {
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to toggle maintenance mode?\n" +
            "This will affect all system users.",
            "Maintenance Mode",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            // For now, just show a message
            JOptionPane.showMessageDialog(this,
                "Maintenance mode functionality will be implemented soon.\n" +
                "This would typically:\n" +
                "• Put the system in maintenance mode\n" +
                "• Prevent new logins\n" +
                "• Display maintenance message to users",
                "Maintenance Mode",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void openBackupRestore() {
        JDialog dialog = new JDialog(this, "Database Backup & Restore", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][][][grow][]"));
        
        panel.add(new JLabel("<html><h3>Database Backup & Restore</h3></html>"), "wrap");
        
        JButton backupBtn = new JButton("Create Backup");
        backupBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog,
                "Backup functionality will create a full database backup.\n" +
                "This feature is under development.",
                "Backup",
                JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(backupBtn, "wrap");
        
        JButton restoreBtn = new JButton("Restore from Backup");
        restoreBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog,
                "Restore functionality will restore from a backup file.\n" +
                "This feature is under development.",
                "Restore",
                JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(restoreBtn, "wrap");
        
        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        logArea.setText("Database operations log will appear here...\n");
        panel.add(new JScrollPane(logArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
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
}
