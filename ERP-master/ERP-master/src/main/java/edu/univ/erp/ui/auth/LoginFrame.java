package edu.univ.erp.ui.auth;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.auth.AuthService;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.auth.UserRole;
import edu.univ.erp.data.DatabaseConnection;
import edu.univ.erp.data.SettingsDAO;
import edu.univ.erp.domain.Settings;
import edu.univ.erp.domain.User;
import edu.univ.erp.ui.admin.AdminDashboard;
import edu.univ.erp.ui.instructor.InstructorDashboard;
import edu.univ.erp.ui.student.StudentDashboard;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Login frame for user authentication.
 */
public class LoginFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(LoginFrame.class);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private AuthService authService;

    public LoginFrame() {
        this.authService = new AuthService();
        initComponents();
        setupFrame();
    }

    private void initComponents() {
        setTitle("University ERP - Login");
        setLayout(new BorderLayout());

        // Create main panel with padding
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]20[]10[]10[]20[]10[]"));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 20");

        // Title
        JLabel titleLabel = new JLabel("University ERP System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, "align center, wrap");

        // Subtitle
        JLabel subtitleLabel = new JLabel("Please login to continue");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subtitleLabel.setForeground(Color.GRAY);
        mainPanel.add(subtitleLabel, "align center, wrap");

        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        mainPanel.add(usernameLabel, "wrap");
        
        usernameField = new JTextField(20);
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter username");
        mainPanel.add(usernameField, "growx, wrap");

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        mainPanel.add(passwordLabel, "wrap");
        
        passwordField = new JPasswordField(20);
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter password");
        passwordField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        mainPanel.add(passwordField, "growx, wrap");

        // Login button
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, "borderless");
        loginButton.addActionListener(e -> handleLogin());
        mainPanel.add(loginButton, "growx, wrap");

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel, "align center, wrap");

        // Add panels to frame - removed info panel with credentials
        add(mainPanel, BorderLayout.CENTER);

        // Enter key to login
        passwordField.addActionListener(e -> handleLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 550);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        // Test database connection first
        if (!DatabaseConnection.testConnections()) {
            showError("Cannot connect to database. Please check configuration.");
            logger.error("Database connection test failed");
            return;
        }

        // Disable button and show loading
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");
        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(Color.BLUE);

        // Perform authentication in background
        SwingWorker<AuthService.AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthService.AuthResult doInBackground() {
                return authService.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    AuthService.AuthResult result = get();
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");

                    if (result.isSuccess()) {
                        User user = result.getUser();
                        SessionManager.getInstance().setCurrentUser(user);
                        logger.info("Login successful for user: {} (role: {})", user.getUsername(), user.getRole());
                        
                        // Check maintenance mode before opening dashboard
                        if (!checkMaintenanceMode(user)) {
                            // Maintenance mode blocked the login
                            loginButton.setEnabled(true);
                            loginButton.setText("Login");
                            SessionManager.getInstance().logout();
                            return;
                        }
                        
                        showSuccess("Login successful! Opening dashboard...");
                        
                        // Open appropriate dashboard after short delay
                        Timer timer = new Timer(500, e -> openDashboard(user));
                        timer.setRepeats(false);
                        timer.start();
                        
                    } else {
                        showError(result.getMessage());
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception e) {
                    logger.error("Error during login", e);
                    showError("An error occurred during login");
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            }
        };
        worker.execute();
    }

    /**
     * Check if maintenance mode is enabled and block non-admin users.
     * 
     * @param user The authenticated user
     * @return true if user is allowed to proceed, false if blocked
     */
    private boolean checkMaintenanceMode(User user) {
        // Admins always bypass maintenance mode
        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }
        
        try {
            SettingsDAO settingsDAO = new SettingsDAO();
            Settings settings = settingsDAO.getSettings();
            
            if (settings.isMaintenanceMode()) {
                logger.warn("Maintenance mode blocked login for user: {} (role: {})", 
                        user.getUsername(), user.getRole());
                
                JOptionPane.showMessageDialog(this,
                        "<html><h3>System Under Maintenance</h3>" +
                        "<p>The system is currently undergoing maintenance.</p>" +
                        "<p>Please try again later.</p>" +
                        "<p><small>If you need immediate access, please contact your system administrator.</small></p></html>",
                        "Maintenance Mode",
                        JOptionPane.WARNING_MESSAGE);
                
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error checking maintenance mode", e);
            // On error, allow login (fail open to avoid locking everyone out)
            return true;
        }
    }

    private void openDashboard(User user) {
        dispose();
        
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame dashboard;
                switch (user.getRole()) {
                    case UserRole.ADMIN:
                        dashboard = new AdminDashboard();
                        break;
                    case UserRole.INSTRUCTOR:
                        dashboard = InstructorDashboard.create();
                        if (dashboard == null) {
                            JOptionPane.showMessageDialog(null,
                                    "Failed to initialize instructor dashboard",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        break;
                    case UserRole.STUDENT:
                        dashboard = new StudentDashboard();
                        break;
                    default:
                        JOptionPane.showMessageDialog(null,
                                "Unknown role: " + user.getRole(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                }
                dashboard.setVisible(true);
            } catch (Exception e) {
                logger.error("Error opening dashboard", e);
                JOptionPane.showMessageDialog(null,
                        "Error opening dashboard: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                // Reopen login
                new LoginFrame().setVisible(true);
            }
        });
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.RED);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(34, 139, 34));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LoginFrame().setVisible(true);
        });
    }
}
