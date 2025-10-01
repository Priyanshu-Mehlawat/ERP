package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.ui.auth.LoginFrame;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Instructor dashboard - main interface for instructors.
 */
public class InstructorDashboard extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(InstructorDashboard.class);

    public InstructorDashboard() {
        initComponents();
        setupFrame();
    }

    private void initComponents() {
        setTitle("Instructor Dashboard - " + SessionManager.getInstance().getCurrentUser().getUsername());
        setLayout(new BorderLayout());

        // Top panel with welcome message
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel welcomeLabel = new JLabel("Welcome, Instructor!");
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
        JButton mySectionsBtn = createMenuButton("My Sections", "View assigned sections");
        JButton gradesBtn = createMenuButton("Manage Grades", "Enter and update grades");
        JButton statsBtn = createMenuButton("Class Statistics", "View class performance");
        JButton exportBtn = createMenuButton("Export Grades", "Export grades to CSV");

        centerPanel.add(mySectionsBtn, "grow");
        centerPanel.add(gradesBtn, "grow");
        centerPanel.add(statsBtn, "grow");
        centerPanel.add(exportBtn, "grow");

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
        setSize(700, 450);
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
