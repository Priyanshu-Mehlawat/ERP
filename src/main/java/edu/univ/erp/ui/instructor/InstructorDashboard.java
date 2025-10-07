package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.ui.auth.LoginFrame;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Instructor dashboard - main interface for faculty members.
 */
public class InstructorDashboard extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(InstructorDashboard.class);
    
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private Instructor currentInstructor;

    public InstructorDashboard() {
        loadCurrentInstructor();
        initComponents();
        setupFrame();
    }

    private void loadCurrentInstructor() {
        try {
            Long userId = SessionManager.getInstance().getCurrentUser().getUserId();
            if (userId == null) {
                logger.warn("User ID is null, cannot load instructor data");
                return;
            }
            currentInstructor = instructorDAO.findByUserId(userId);
            if (currentInstructor == null) {
                logger.warn("No instructor found for user ID: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error loading current instructor", e);
        }
    }

    private void initComponents() {
        String title = "Instructor Dashboard";
        if (currentInstructor != null) {
            title += " - " + currentInstructor.getFirstName() + " " + currentInstructor.getLastName();
        } else {
            title += " - " + SessionManager.getInstance().getCurrentUser().getUsername();
        }
        setTitle(title);
        setLayout(new BorderLayout());

        // Top panel with welcome message
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel welcomeLabel = new JLabel("<html><h2>Welcome, Instructor!</h2></html>");
        if (currentInstructor != null) {
            welcomeLabel.setText("<html><h2>Welcome, " + currentInstructor.getFirstName() + "!</h2></html>");
        }
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with menu buttons
        JPanel centerPanel = new JPanel(new MigLayout("fillx, wrap 2", "[grow][grow]", ""));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Menu buttons for Week 5-6 features
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

        add(centerPanel, BorderLayout.CENTER);

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        String statusText = "Ready";
        if (currentInstructor != null) {
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

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
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

    private void openCourseManagement() {
        JDialog dialog = new JDialog(this, "Course Management", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(1000, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new CourseManagementPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openClassRoster() {
        JDialog dialog = new JDialog(this, "Class Roster", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new ClassRosterPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openGradeEntry() {
        JDialog dialog = new JDialog(this, "Grade Entry", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(1100, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new GradeEntryPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openAttendance() {
        JDialog dialog = new JDialog(this, "Attendance Tracking", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new AttendancePanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openReports() {
        JDialog dialog = new JDialog(this, "Reports", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new ReportsPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openSchedule() {
        JDialog dialog = new JDialog(this, "Teaching Schedule", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new InstructorSchedulePanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}
