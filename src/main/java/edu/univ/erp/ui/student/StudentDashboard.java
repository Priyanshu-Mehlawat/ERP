package edu.univ.erp.ui.student;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.ui.auth.LoginFrame;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.*;

/**
 * Student dashboard - main interface for students.
 */
public class StudentDashboard extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(StudentDashboard.class);

    public StudentDashboard() {
        initComponents();
        setupFrame();
    }

    private void initComponents() {
        setTitle("Student Dashboard - " + SessionManager.getInstance().getCurrentUser().getUsername());
        setLayout(new BorderLayout());

        // Top panel with welcome message
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel welcomeLabel = new JLabel("Welcome, Student!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with menu buttons
        JPanel centerPanel = new JPanel(new MigLayout("fillx, wrap 2", "[grow][grow]", ""));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Menu buttons
        JButton browseCatalogBtn = createMenuButton("Browse Course Catalog", "View available courses");
        // Replace default placeholder action with real action
        for (ActionListener al : browseCatalogBtn.getActionListeners()) {
            browseCatalogBtn.removeActionListener(al);
        }
        browseCatalogBtn.addActionListener(e -> openCourseCatalog());

        JButton myCoursesBtn = createMenuButton("My Courses", "View registered courses");
        for (ActionListener al : myCoursesBtn.getActionListeners()) {
            myCoursesBtn.removeActionListener(al);
        }
        myCoursesBtn.addActionListener(e -> openMyCourses());

        JButton timetableBtn = createMenuButton("My Timetable", "View class schedule");
        for (ActionListener al : timetableBtn.getActionListeners()) {
            timetableBtn.removeActionListener(al);
        }
        timetableBtn.addActionListener(e -> openTimetable());

        JButton gradesBtn = createMenuButton("My Grades", "View grades and scores");
        for (ActionListener al : gradesBtn.getActionListeners()) {
            gradesBtn.removeActionListener(al);
        }
        gradesBtn.addActionListener(e -> openGrades());

        JButton transcriptBtn = createMenuButton("Download Transcript", "Export transcript");
        for (ActionListener al : transcriptBtn.getActionListeners()) {
            transcriptBtn.removeActionListener(al);
        }
        transcriptBtn.addActionListener(e -> openTranscript());

        centerPanel.add(browseCatalogBtn, "grow");
        centerPanel.add(myCoursesBtn, "grow");
        centerPanel.add(timetableBtn, "grow");
        centerPanel.add(gradesBtn, "grow");
        centerPanel.add(transcriptBtn, "span 2, grow");

        add(centerPanel, BorderLayout.CENTER);

        // Status bar
        JLabel statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JButton createMenuButton(String title, String description) {
        JButton button = new JButton("<html><b>" + title + "</b><br><small>" + description + "</small></html>");
        button.setPreferredSize(new Dimension(200, 60));
        button.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Feature '" + title + "' coming soon!",
                    title,
                    JOptionPane.INFORMATION_MESSAGE);
        });
        return button;
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
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

    private void openCourseCatalog() {
        JDialog dialog = new JDialog(this, "Course Catalog", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new CourseCatalogPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openMyCourses() {
        JDialog dialog = new JDialog(this, "My Courses", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new MyCoursesPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openTimetable() {
        JDialog dialog = new JDialog(this, "My Timetable", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new MyTimetablePanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openGrades() {
        JDialog dialog = new JDialog(this, "My Grades", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new MyGradesPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openTranscript() {
        JDialog dialog = new JDialog(this, "Official Transcript", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(new TranscriptPanel(), BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}
