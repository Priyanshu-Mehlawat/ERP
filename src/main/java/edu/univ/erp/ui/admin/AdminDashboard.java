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
    
    /**
     * Enum for strongly-typed admin actions to avoid string-based switching
     */
    private enum AdminAction {
        MANAGE_USERS,
        MANAGE_COURSES, 
        MANAGE_SECTIONS,
        ASSIGN_INSTRUCTORS,
        MAINTENANCE_MODE,
        BACKUP_RESTORE
    }
    
    // Dialog size constants
    private static final int DIALOG_WIDTH = 600;
    private static final int DIALOG_HEIGHT = 500;
    
    // Track active database operations to prevent premature dialog closure
    // Volatile for memory visibility across threads
    private volatile SwingWorker<Boolean, String> currentDatabaseWorker = null;

    public AdminDashboard() {
        initComponents();
        setupFrame();
    }

    // Synchronized helper methods for thread-safe access to currentDatabaseWorker
    
    /**
     * Atomically checks if a database worker is currently active and running
     * @return true if a worker is active and not done, false otherwise
     */
    private synchronized boolean isDatabaseWorkerActive() {
        return currentDatabaseWorker != null && !currentDatabaseWorker.isDone();
    }
    
    /**
     * Atomically cancels the current database worker and clears the reference
     * @return true if a worker was cancelled, false if no active worker
     */
    private synchronized boolean cancelCurrentDatabaseWorker() {
        if (currentDatabaseWorker != null && !currentDatabaseWorker.isDone()) {
            currentDatabaseWorker.cancel(true);
            currentDatabaseWorker = null;
            return true;
        }
        return false;
    }
    
    /**
     * Atomically sets and starts a new database worker
     * @param worker the SwingWorker to set and execute
     * @return true if the worker was set and started, false if refused due to active worker
     */
    private synchronized boolean setAndExecuteDatabaseWorker(SwingWorker<Boolean, String> worker) {
        // Check if there's already an active worker running
        if (currentDatabaseWorker != null && !currentDatabaseWorker.isDone()) {
            logger.warn("Refused to start new database worker - another worker is already active");
            return false;
        }
        
        // Safe to set and execute the new worker
        currentDatabaseWorker = worker;
        currentDatabaseWorker.execute();
        return true;
    }
    
    /**
     * Atomically clears the current database worker reference
     */
    private synchronized void clearDatabaseWorker() {
        currentDatabaseWorker = null;
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

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        
        JButton changePasswordBtn = new JButton("Change Password");
        changePasswordBtn.addActionListener(e -> openChangePasswordDialog());
        rightPanel.add(changePasswordBtn);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        rightPanel.add(logoutButton);
        
        topPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with menu buttons
        JPanel centerPanel = new JPanel(new MigLayout("fillx, wrap 3", "[grow][grow][grow]", ""));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Menu buttons
        JButton manageUsersBtn = createMenuButton(AdminAction.MANAGE_USERS, "Manage Users", "Add/edit users");
        JButton manageCoursesBtn = createMenuButton(AdminAction.MANAGE_COURSES, "Manage Courses", "Add/edit courses");
        JButton manageSectionsBtn = createMenuButton(AdminAction.MANAGE_SECTIONS, "Manage Sections", "Add/edit sections");
        JButton assignInstructorBtn = createMenuButton(AdminAction.ASSIGN_INSTRUCTORS, "Assign Instructors", "Assign to sections");
        JButton maintenanceBtn = createMenuButton(AdminAction.MAINTENANCE_MODE, "Maintenance Mode", "Toggle maintenance");
        JButton backupBtn = createMenuButton(AdminAction.BACKUP_RESTORE, "Backup/Restore", "Database backup");

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

    private JButton createMenuButton(AdminAction action, String title, String description) {
        JButton button = new JButton("<html><b>" + title + "</b><br><small>" + description + "</small></html>");
        button.setPreferredSize(new Dimension(180, 60));
        
        // Store the action as a client property for strongly-typed access
        button.putClientProperty("adminAction", action);
        
        // Add real functionality based on the action enum
        button.addActionListener(e -> {
            AdminAction buttonAction = (AdminAction) button.getClientProperty("adminAction");
            if (buttonAction != null) {
                switch (buttonAction) {
                    case MANAGE_USERS:
                        openUserManagement();
                        break;
                    case MANAGE_COURSES:
                        openCourseManagement();
                        break;
                    case MANAGE_SECTIONS:
                        openSectionManagement();
                        break;
                    case ASSIGN_INSTRUCTORS:
                        openInstructorAssignment();
                        break;
                    case MAINTENANCE_MODE:
                        toggleMaintenanceMode();
                        break;
                    case BACKUP_RESTORE:
                        openBackupRestore();
                        break;
                    default:
                        // This should not happen with properly defined enum
                        logger.warn("Unknown admin action: {}", buttonAction);
                        JOptionPane.showMessageDialog(this,
                                "Unknown action requested.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Fallback for null action (should not happen)
                logger.error("Button action is null for button: {}", title);
                JOptionPane.showMessageDialog(this,
                        "Action not properly configured.",
                        "Configuration Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        return button;
    }
    
    /**
     * Helper method to show information dialogs with consistent UI structure
     */
    private void showInformationDialog(String title, String content) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][grow][]"));
        
        panel.add(new JLabel("<html><h3>" + title + "</h3></html>"), "wrap");
        
        JTextArea infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setText(content);
        
        panel.add(new JScrollPane(infoArea), "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, "right");
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openUserManagement() {
        JDialog dialog = new JDialog(this, "User Management", true);
        dialog.setLayout(new BorderLayout());
        
        // Create and add the user management panel
        UserManagementPanel userPanel = new UserManagementPanel();
        dialog.add(userPanel, BorderLayout.CENTER);
        
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openCourseManagement() {
        JDialog dialog = new JDialog(this, "Course Catalog Management", true);
        dialog.setLayout(new BorderLayout());
        
        CourseManagementPanel coursePanel = new CourseManagementPanel();
        dialog.add(coursePanel, BorderLayout.CENTER);
        
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openSectionManagement() {
        JDialog dialog = new JDialog(this, "Section Management", true);
        dialog.setLayout(new BorderLayout());
        
        SectionManagementPanel sectionPanel = new SectionManagementPanel();
        dialog.add(sectionPanel, BorderLayout.CENTER);
        
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openInstructorAssignment() {
        showInformationDialog("Instructor Assignment",
            "Instructor Assignment Features:\n\n" +
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
    }
    
    private void toggleMaintenanceMode() {
        // Open settings panel where maintenance mode can be configured
        JDialog dialog = new JDialog(this, "System Settings", true);
        dialog.setLayout(new BorderLayout());
        
        SettingsPanel settingsPanel = new SettingsPanel();
        dialog.add(settingsPanel, BorderLayout.CENTER);
        
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void openBackupRestore() {
        JDialog dialog = new JDialog(this, "Database Backup & Restore", true); // Modal - prevents parent interaction during operations
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[][][][][grow][]"));
        
        panel.add(new JLabel("<html><h3>Database Backup & Restore</h3></html>"), "wrap");
        
        // Progress bar for operations
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);
        panel.add(progressBar, "growx, wrap");
        
        JButton backupBtn = new JButton("Create Backup");
        JButton restoreBtn = new JButton("Restore from Backup");
        
        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        logArea.setText("Database operations log will appear here...\n");
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        backupBtn.addActionListener(e -> {
            performBackupOperation(dialog, backupBtn, restoreBtn, progressBar, logArea);
        });
        panel.add(backupBtn, "wrap");
        
        restoreBtn.addActionListener(e -> {
            performRestoreOperation(dialog, backupBtn, restoreBtn, progressBar, logArea);
        });
        panel.add(restoreBtn, "wrap");
        
        panel.add(scrollPane, "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> {
            if (isDatabaseWorkerActive()) {
                int choice = JOptionPane.showConfirmDialog(dialog,
                    "A database operation is currently in progress.\n" +
                    "Closing this dialog will cancel the operation.\n\n" +
                    "Are you sure you want to cancel and close?",
                    "Operation in Progress",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (choice == JOptionPane.YES_OPTION) {
                    cancelCurrentDatabaseWorker();
                    dialog.dispose();
                }
            } else {
                dialog.dispose();
            }
        });
        panel.add(closeBtn, "right");

        // Add window listener to handle dialog close button (X)
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (isDatabaseWorkerActive()) {
                    int choice = JOptionPane.showConfirmDialog(dialog,
                        "A database operation is currently in progress.\n" +
                        "Closing this dialog will cancel the operation.\n\n" +
                        "Are you sure you want to cancel and close?",
                        "Operation in Progress",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        cancelCurrentDatabaseWorker();
                        dialog.dispose();
                    }
                    // If NO, do nothing - dialog stays open
                } else {
                    dialog.dispose();
                }
            }
        });

        dialog.add(panel, BorderLayout.CENTER);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void performBackupOperation(JDialog dialog, JButton backupBtn, JButton restoreBtn, 
                                      JProgressBar progressBar, JTextArea logArea) {
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish("Starting database backup...");
                if (isCancelled()) return false;
                Thread.sleep(500); // Simulate initialization
                
                publish("Connecting to database...");
                if (isCancelled()) return false;
                Thread.sleep(1000); // Simulate connection
                
                publish("Backing up auth schema...");
                if (isCancelled()) return false;
                Thread.sleep(2000); // Simulate backup work
                
                publish("Backing up ERP schema...");
                if (isCancelled()) return false;
                Thread.sleep(2000); // Simulate backup work
                
                publish("Compressing backup file...");
                if (isCancelled()) return false;
                Thread.sleep(1500); // Simulate compression
                
                publish("Backup completed successfully!");
                return true;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(java.time.LocalTime.now().toString() + " - " + message + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                progressBar.setVisible(false);
                backupBtn.setEnabled(true);
                restoreBtn.setEnabled(true);
                clearDatabaseWorker(); // Thread-safe clear worker reference
                
                try {
                    if (isCancelled()) {
                        progressBar.setString("Backup cancelled");
                        logArea.append(java.time.LocalTime.now().toString() + " - Backup operation was cancelled\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                        return;
                    }
                    
                    Boolean result = get();
                    if (result) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setString("Backup completed");
                            JOptionPane.showMessageDialog(dialog,
                                "Database backup completed successfully!\n" +
                                "Note: This is a demonstration. In production, this would\n" +
                                "create actual database backups with proper file handling.",
                                "Backup Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Backup failed");
                        logArea.append(java.time.LocalTime.now().toString() + " - Error: " + e.getMessage() + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                        JOptionPane.showMessageDialog(dialog,
                            "Backup operation failed: " + e.getMessage(),
                            "Backup Error",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        };
        
        // Disable buttons and show progress
        backupBtn.setEnabled(false);
        restoreBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Backing up...");
        
        if (!setAndExecuteDatabaseWorker(worker)) {
            // Failed to start worker - restore UI state and show error
            backupBtn.setEnabled(true);
            restoreBtn.setEnabled(true);
            progressBar.setVisible(false);
            progressBar.setString("");
            JOptionPane.showMessageDialog(dialog,
                "Cannot start backup operation.\n" +
                "Another database operation is currently in progress.\n" +
                "Please wait for it to complete before starting a new operation.",
                "Operation in Progress",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void performRestoreOperation(JDialog dialog, JButton backupBtn, JButton restoreBtn, 
                                       JProgressBar progressBar, JTextArea logArea) {
        // First show file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select Backup File to Restore");
        
        int result = fileChooser.showOpenDialog(dialog);
        if (result != JFileChooser.APPROVE_OPTION) {
            return; // User cancelled
        }
        
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish("Starting database restore...");
                if (isCancelled()) return false;
                Thread.sleep(500); // Simulate initialization
                
                publish("Validating backup file...");
                if (isCancelled()) return false;
                Thread.sleep(1000); // Simulate validation
                
                publish("Stopping services...");
                if (isCancelled()) return false;
                Thread.sleep(1500); // Simulate service stop
                
                publish("Restoring auth schema...");
                if (isCancelled()) return false;
                Thread.sleep(2500); // Simulate restore work
                
                publish("Restoring ERP schema...");
                if (isCancelled()) return false;
                Thread.sleep(2500); // Simulate restore work
                
                publish("Rebuilding indices...");
                if (isCancelled()) return false;
                Thread.sleep(1500); // Simulate index rebuild
                
                publish("Starting services...");
                if (isCancelled()) return false;
                Thread.sleep(1000); // Simulate service start
                
                publish("Restore completed successfully!");
                return true;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(java.time.LocalTime.now().toString() + " - " + message + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                progressBar.setVisible(false);
                backupBtn.setEnabled(true);
                restoreBtn.setEnabled(true);
                clearDatabaseWorker(); // Thread-safe clear worker reference
                
                try {
                    if (isCancelled()) {
                        progressBar.setString("Restore cancelled");
                        logArea.append(java.time.LocalTime.now().toString() + " - Restore operation was cancelled\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                        return;
                    }
                    
                    Boolean success = get();
                    if (success) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setString("Restore completed");
                            JOptionPane.showMessageDialog(dialog,
                                "Database restore completed successfully!\n" +
                                "Note: This is a demonstration. In production, this would\n" +
                                "perform actual database restoration with proper validation.",
                                "Restore Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Restore failed");
                        logArea.append(java.time.LocalTime.now().toString() + " - Error: " + e.getMessage() + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                        JOptionPane.showMessageDialog(dialog,
                            "Restore operation failed: " + e.getMessage(),
                            "Restore Error",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        };
        
        // Disable buttons and show progress
        backupBtn.setEnabled(false);
        restoreBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Restoring...");
        
        if (!setAndExecuteDatabaseWorker(worker)) {
            // Failed to start worker - restore UI state and show error
            backupBtn.setEnabled(true);
            restoreBtn.setEnabled(true);
            progressBar.setVisible(false);
            progressBar.setString("");
            JOptionPane.showMessageDialog(dialog,
                "Cannot start restore operation.\n" +
                "Another database operation is currently in progress.\n" +
                "Please wait for it to complete before starting a new operation.",
                "Operation in Progress",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
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
}
