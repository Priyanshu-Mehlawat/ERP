package edu.univ.erp.ui.admin;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.auth.AuthDAO;
import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.auth.UserRole;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;

/**
 * User Management Panel for Admin
 * Allows CRUD operations on user accounts
 */
public class UserManagementPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementPanel.class);
    
    private final AuthDAO authDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> roleFilterCombo;
    
    public UserManagementPanel() {
        this.authDAO = new AuthDAO();
        initComponents();
        loadUsers();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("User Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        // Top toolbar with search and filters
        JPanel toolbarPanel = createToolbarPanel();
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        
        // User table
        JPanel tablePanel = createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        // Action buttons panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[grow][]", ""));
        
        // Search field
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        searchField.addActionListener(e -> filterUsers());
        searchPanel.add(searchField);
        
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> filterUsers());
        searchPanel.add(searchBtn);
        
        // Role filter
        searchPanel.add(Box.createHorizontalStrut(20));
        searchPanel.add(new JLabel("Role:"));
        roleFilterCombo = new JComboBox<>(new String[]{"All", "ADMIN", "INSTRUCTOR", "STUDENT"});
        roleFilterCombo.addActionListener(e -> filterUsers());
        searchPanel.add(roleFilterCombo);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadUsers());
        searchPanel.add(refreshBtn);
        
        panel.add(searchPanel, "wrap");
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columns = {"User ID", "Username", "Role", "Status", "Failed Attempts", "Last Login"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(25);
        userTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        userTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton addBtn = new JButton("Add User");
        addBtn.addActionListener(e -> showAddUserDialog());
        panel.add(addBtn);
        
        JButton editBtn = new JButton("Edit User");
        editBtn.addActionListener(e -> showEditUserDialog());
        panel.add(editBtn);
        
        JButton deleteBtn = new JButton("Delete User");
        deleteBtn.addActionListener(e -> deleteUser());
        panel.add(deleteBtn);
        
        JButton resetPasswordBtn = new JButton("Reset Password");
        resetPasswordBtn.addActionListener(e -> resetUserPassword());
        panel.add(resetPasswordBtn);
        
        JButton unlockBtn = new JButton("Unlock Account");
        unlockBtn.addActionListener(e -> unlockAccount());
        panel.add(unlockBtn);
        
        return panel;
    }
    
    private void loadUsers() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return authDAO.getAllUsers();
            }
            
            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    displayUsers(users);
                } catch (Exception e) {
                    logger.error("Error loading users", e);
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Error loading users: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void displayUsers(List<User> users) {
        tableModel.setRowCount(0);
        for (User user : users) {
            tableModel.addRow(new Object[]{
                user.getUserId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getFailedLoginAttempts(),
                user.getLastLogin() != null ? user.getLastLogin().toString() : "Never"
            });
        }
    }
    
    private void filterUsers() {
        String searchText = searchField.getText().trim().toLowerCase();
        String roleFilter = (String) roleFilterCombo.getSelectedItem();
        
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                List<User> allUsers = authDAO.getAllUsers();
                return allUsers.stream()
                    .filter(u -> {
                        boolean matchesSearch = searchText.isEmpty() || 
                            u.getUsername().toLowerCase().contains(searchText);
                        boolean matchesRole = "All".equals(roleFilter) || 
                            u.getRole().equals(roleFilter);
                        return matchesSearch && matchesRole;
                    })
                    .toList();
            }
            
            @Override
            protected void done() {
                try {
                    displayUsers(get());
                } catch (Exception e) {
                    logger.error("Error filtering users", e);
                }
            }
        };
        worker.execute();
    }
    
    private void showAddUserDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Add New User", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        // Form fields
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{
            UserRole.STUDENT, UserRole.INSTRUCTOR, UserRole.ADMIN
        });
        
        dialog.add(new JLabel("Username:"), "");
        dialog.add(usernameField, "wrap");
        
        dialog.add(new JLabel("Password:"), "");
        dialog.add(passwordField, "wrap");
        
        dialog.add(new JLabel("Confirm Password:"), "");
        dialog.add(confirmPasswordField, "wrap");
        
        dialog.add(new JLabel("Role:"), "");
        dialog.add(roleCombo, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Create User");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            
            // Validation
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Username and password cannot be empty",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog,
                    "Passwords do not match",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dialog,
                    "Password must be at least 6 characters",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create user
            createUser(username, password, role);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, wrap");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void createUser(String username, String password, String role) {
        SwingWorker<Long, Void> worker = new SwingWorker<>() {
            @Override
            protected Long doInBackground() throws Exception {
                String hashedPassword = PasswordUtil.hashPassword(password);
                Long userId = authDAO.createUser(username, role, hashedPassword);
                logger.info("User created with ID: {}", userId);
                // Give database a moment to commit
                Thread.sleep(100);
                return userId;
            }
            
            @Override
            protected void done() {
                try {
                    Long userId = get();
                    if (userId != null) {
                        JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "User '" + username + "' created successfully!\nUser ID: " + userId,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        // Refresh after a short delay to ensure database has updated
                        SwingUtilities.invokeLater(() -> {
                            try {
                                Thread.sleep(200);
                                loadUsers();
                            } catch (InterruptedException ex) {
                                loadUsers();
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.error("Error creating user", e);
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Error creating user: " + errorMsg,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void showEditUserDialog() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to edit",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long userId = (Long) tableModel.getValueAt(selectedRow, 0);
        String currentUsername = (String) tableModel.getValueAt(selectedRow, 1);
        String currentRole = (String) tableModel.getValueAt(selectedRow, 2);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Edit User", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        JTextField usernameField = new JTextField(currentUsername, 20);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{
            UserRole.STUDENT, UserRole.INSTRUCTOR, UserRole.ADMIN
        });
        roleCombo.setSelectedItem(currentRole);
        
        dialog.add(new JLabel("Username:"), "");
        dialog.add(usernameField, "wrap");
        
        dialog.add(new JLabel("Role:"), "");
        dialog.add(roleCombo, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String newUsername = usernameField.getText().trim();
            String newRole = (String) roleCombo.getSelectedItem();
            
            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Username cannot be empty",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            updateUser(userId, newUsername, newRole);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, wrap");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void updateUser(Long userId, String username, String role) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                authDAO.updateUser(userId, username, role);
                logger.info("User updated - ID: {}, Username: {}, Role: {}", userId, username, role);
                // Give database a moment to commit
                Thread.sleep(100);
                return true;
            }
            
            @Override
            protected void done() {
                try {
                    Boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "User '" + username + "' updated successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        // Refresh after a short delay to ensure database has updated
                        SwingUtilities.invokeLater(() -> {
                            try {
                                Thread.sleep(200);
                                loadUsers();
                            } catch (InterruptedException ex) {
                                loadUsers();
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.error("Error updating user", e);
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Error updating user: " + errorMsg,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to delete",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long userId = (Long) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete user '" + username + "'?\n" +
            "This action cannot be undone.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    authDAO.deleteUser(userId);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "User deleted successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        loadUsers();
                    } catch (Exception e) {
                        logger.error("Error deleting user", e);
                        JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "Error deleting user: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void resetUserPassword() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to reset password",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long userId = (Long) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Reset Password for " + username, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        JPasswordField newPasswordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);
        
        dialog.add(new JLabel("New Password:"), "");
        dialog.add(newPasswordField, "wrap");
        
        dialog.add(new JLabel("Confirm Password:"), "");
        dialog.add(confirmPasswordField, "wrap");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton resetBtn = new JButton("Reset Password");
        JButton cancelBtn = new JButton("Cancel");
        
        resetBtn.addActionListener(e -> {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Password cannot be empty",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog,
                    "Passwords do not match",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(dialog,
                    "Password must be at least 6 characters",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            resetPassword(userId, username, newPassword);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(resetBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, wrap");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void resetPassword(Long userId, String username, String newPassword) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String hashedPassword = PasswordUtil.hashPassword(newPassword);
                authDAO.resetPassword(userId, hashedPassword);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Password reset successfully for user '" + username + "'",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error resetting password", e);
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Error resetting password: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void unlockAccount() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to unlock",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long userId = (Long) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                authDAO.unlockAccount(userId);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Account unlocked successfully for user '" + username + "'",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadUsers();
                } catch (Exception e) {
                    logger.error("Error unlocking account", e);
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Error unlocking account: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
