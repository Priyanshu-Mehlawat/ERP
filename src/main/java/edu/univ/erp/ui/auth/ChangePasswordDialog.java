package edu.univ.erp.ui.auth;

import edu.univ.erp.auth.AuthService;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Change Password Dialog
 * Allows authenticated users to change their password
 */
public class ChangePasswordDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ChangePasswordDialog.class);
    
    private final AuthService authService;
    private final User currentUser;
    
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JLabel strengthLabel;
    private JProgressBar strengthBar;
    
    public ChangePasswordDialog(Frame parent) {
        super(parent, "Change Password", true);
        this.authService = new AuthService();
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel(new MigLayout("fillx, insets 20", "[right]rel[grow,fill]", ""));
        
        // Title
        JLabel titleLabel = new JLabel("<html><h2>Change Password</h2></html>");
        mainPanel.add(titleLabel, "span, wrap, gapbottom 15");
        
        // User info
        JLabel userLabel = new JLabel("User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(userLabel, "span, wrap, gapbottom 20");
        
        // Current password
        mainPanel.add(new JLabel("Current Password:"), "");
        currentPasswordField = new JPasswordField(20);
        mainPanel.add(currentPasswordField, "wrap");
        
        mainPanel.add(new JLabel(""), "");
        JLabel currentInfo = new JLabel("<html><i>Enter your current password to verify identity</i></html>");
        currentInfo.setForeground(Color.GRAY);
        currentInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        mainPanel.add(currentInfo, "wrap, gapbottom 15");
        
        // New password
        mainPanel.add(new JLabel("New Password:"), "");
        newPasswordField = new JPasswordField(20);
        newPasswordField.addCaretListener(e -> updatePasswordStrength());
        mainPanel.add(newPasswordField, "wrap");
        
        // Password strength indicator
        mainPanel.add(new JLabel("Strength:"), "");
        JPanel strengthPanel = new JPanel(new BorderLayout(5, 0));
        strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(true);
        strengthLabel = new JLabel("Weak");
        strengthPanel.add(strengthBar, BorderLayout.CENTER);
        strengthPanel.add(strengthLabel, BorderLayout.EAST);
        mainPanel.add(strengthPanel, "wrap");
        
        mainPanel.add(new JLabel(""), "");
        JLabel newInfo = new JLabel("<html><i>Minimum 8 characters required.<br>" +
                                     "Strong passwords include:<br>" +
                                     "• Uppercase and lowercase letters<br>" +
                                     "• Numbers and special characters</i></html>");
        newInfo.setForeground(Color.GRAY);
        newInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        mainPanel.add(newInfo, "wrap, gapbottom 15");
        
        // Confirm password
        mainPanel.add(new JLabel("Confirm Password:"), "");
        confirmPasswordField = new JPasswordField(20);
        mainPanel.add(confirmPasswordField, "wrap");
        
        mainPanel.add(new JLabel(""), "");
        JLabel confirmInfo = new JLabel("<html><i>Re-enter new password to confirm</i></html>");
        confirmInfo.setForeground(Color.GRAY);
        confirmInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        mainPanel.add(confirmInfo, "wrap, gapbottom 20");
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton changeBtn = new JButton("Change Password");
        changeBtn.addActionListener(e -> changePassword());
        buttonPanel.add(changeBtn);
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        buttonPanel.add(cancelBtn);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }
    
    private void updatePasswordStrength() {
        String password = new String(newPasswordField.getPassword());
        int strength = calculatePasswordStrength(password);
        
        strengthBar.setValue(strength);
        
        if (strength < 30) {
            strengthLabel.setText("Weak");
            strengthLabel.setForeground(Color.RED);
            strengthBar.setForeground(Color.RED);
        } else if (strength < 60) {
            strengthLabel.setText("Fair");
            strengthLabel.setForeground(Color.ORANGE);
            strengthBar.setForeground(Color.ORANGE);
        } else if (strength < 80) {
            strengthLabel.setText("Good");
            strengthLabel.setForeground(new Color(200, 150, 0));
            strengthBar.setForeground(new Color(200, 150, 0));
        } else {
            strengthLabel.setText("Strong");
            strengthLabel.setForeground(new Color(0, 150, 0));
            strengthBar.setForeground(new Color(0, 150, 0));
        }
    }
    
    private int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int strength = 0;
        
        // Length score (max 40 points)
        if (password.length() >= 6) strength += 20;
        if (password.length() >= 8) strength += 10;
        if (password.length() >= 12) strength += 10;
        
        // Character variety (60 points)
        if (password.matches(".*[a-z].*")) strength += 15; // lowercase
        if (password.matches(".*[A-Z].*")) strength += 15; // uppercase
        if (password.matches(".*\\d.*")) strength += 15; // digits
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength += 15; // special chars
        
        return Math.min(100, strength);
    }
    
    private void changePassword() {
        // Get password values
        String currentPassword = new String(currentPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Validate inputs
        if (currentPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter your current password",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            currentPasswordField.requestFocus();
            return;
        }
        
        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a new password",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            newPasswordField.requestFocus();
            return;
        }
        
        PasswordUtil.ValidationResult validation = PasswordUtil.validatePassword(newPassword);
        if (!validation.isValid()) {
            JOptionPane.showMessageDialog(this,
                "Password does not meet complexity requirements:\n" + validation.getErrorMessage(),
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            newPasswordField.requestFocus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                "New passwords do not match",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            confirmPasswordField.requestFocus();
            return;
        }
        
        if (currentPassword.equals(newPassword)) {
            JOptionPane.showMessageDialog(this,
                "New password must be different from current password",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            newPasswordField.requestFocus();
            return;
        }
        
        // Change password in background
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Change password (AuthService will verify current password internally)
                return authService.changePassword(currentUser.getUserId(), currentPassword, newPassword);
            }
            
            @Override
            protected void done() {
                try {
                    Boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                            "Password changed successfully!\n\n" +
                            "Please use your new password for future logins.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                            "Current password is incorrect.\n\n" +
                            "Please verify your current password and try again.",
                            "Authentication Failed",
                            JOptionPane.ERROR_MESSAGE);
                        currentPasswordField.requestFocus();
                        currentPasswordField.selectAll();
                    }
                } catch (Exception e) {
                    logger.error("Error changing password", e);
                    JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                        "Error changing password: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
