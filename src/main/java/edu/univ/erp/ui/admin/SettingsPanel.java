package edu.univ.erp.ui.admin;

import edu.univ.erp.data.SettingsDAO;
import edu.univ.erp.domain.Settings;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;

/**
 * Settings Panel for Admin
 * Allows configuration of system-wide settings
 */
public class SettingsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SettingsPanel.class);
    
    private final SettingsDAO settingsDAO;
    
    // UI Components
    private JCheckBox maintenanceModeCheckbox;
    private JCheckBox registrationEnabledCheckbox;
    private JTextField addDropDeadlineField;
    private JTextField withdrawalDeadlineField;
    private JTextField currentSemesterField;
    private JSpinner currentYearSpinner;
    private JTextArea announcementArea;
    
    private Settings currentSettings;
    
    /**
     * Constructor with dependency injection for SettingsDAO.
     * Preferred constructor for testing and when DAO instance needs to be controlled.
     * 
     * @param settingsDAO the SettingsDAO instance to use for database operations
     */
    public SettingsPanel(SettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
        initComponents();
        loadSettings();
    }
    
    /**
     * No-arg constructor for backward compatibility.
     * Creates a new SettingsDAO instance internally.
     * Delegates to the main constructor.
     */
    public SettingsPanel() {
        this(new SettingsDAO());
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("System Settings");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Main content panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // General Settings Tab
        tabbedPane.addTab("General", createGeneralSettingsPanel());
        
        // Academic Calendar Tab
        tabbedPane.addTab("Academic Calendar", createAcademicCalendarPanel());
        
        // Announcements Tab
        tabbedPane.addTab("Announcements", createAnnouncementsPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton saveBtn = new JButton("Save All Settings");
        saveBtn.addActionListener(e -> saveSettings());
        buttonPanel.add(saveBtn);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadSettings());
        buttonPanel.add(refreshBtn);
        
        JButton resetBtn = new JButton("Reset to Defaults");
        resetBtn.addActionListener(e -> resetToDefaults());
        buttonPanel.add(resetBtn);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createGeneralSettingsPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Maintenance Mode
        panel.add(new JLabel("Maintenance Mode:"), "");
        maintenanceModeCheckbox = new JCheckBox("Enable maintenance mode");
        maintenanceModeCheckbox.setToolTipText("When enabled, only admins can access the system");
        panel.add(maintenanceModeCheckbox, "wrap");
        
        panel.add(new JLabel(""), "");
        JLabel maintenanceInfo = new JLabel("<html><i>When maintenance mode is enabled, students and instructors will see<br>" +
                                            "a \"System Under Maintenance\" message and cannot access features.</i></html>");
        maintenanceInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        maintenanceInfo.setForeground(Color.GRAY);
        panel.add(maintenanceInfo, "wrap, gapbottom 15");
        
        // Registration Enabled
        panel.add(new JLabel("Student Registration:"), "");
        registrationEnabledCheckbox = new JCheckBox("Enable course registration");
        registrationEnabledCheckbox.setToolTipText("Allow students to register for courses");
        panel.add(registrationEnabledCheckbox, "wrap");
        
        panel.add(new JLabel(""), "");
        JLabel regInfo = new JLabel("<html><i>When disabled, students can view courses but cannot register or drop.</i></html>");
        regInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        regInfo.setForeground(Color.GRAY);
        panel.add(regInfo, "wrap, gapbottom 15");
        
        // Current Semester/Year
        panel.add(new JLabel("Current Semester:"), "");
        JPanel semesterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        currentSemesterField = new JTextField(10);
        currentYearSpinner = new JSpinner(new SpinnerNumberModel(2025, 2024, 2030, 1));
        semesterPanel.add(currentSemesterField);
        semesterPanel.add(new JLabel("Year:"));
        semesterPanel.add(currentYearSpinner);
        panel.add(semesterPanel, "wrap");
        
        panel.add(new JLabel(""), "");
        JLabel semInfo = new JLabel("<html><i>Example: \"Fall\", \"Spring\", \"Summer\"</i></html>");
        semInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        semInfo.setForeground(Color.GRAY);
        panel.add(semInfo, "wrap, gapbottom 15");
        
        return panel;
    }
    
    private JPanel createAcademicCalendarPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Add/Drop Deadline
        panel.add(new JLabel("Add/Drop Deadline:"), "");
        addDropDeadlineField = new JTextField(20);
        panel.add(addDropDeadlineField, "wrap");
        
        panel.add(new JLabel(""), "");
        JLabel addDropInfo = new JLabel("<html><i>Format: YYYY-MM-DD HH:MM (e.g., \"2025-09-15 23:59\")</i></html>");
        addDropInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        addDropInfo.setForeground(Color.GRAY);
        panel.add(addDropInfo, "wrap, gapbottom 15");
        
        // Withdrawal Deadline
        panel.add(new JLabel("Withdrawal Deadline:"), "");
        withdrawalDeadlineField = new JTextField(20);
        panel.add(withdrawalDeadlineField, "wrap");
        
        panel.add(new JLabel(""), "");
        JLabel withdrawInfo = new JLabel("<html><i>Format: YYYY-MM-DD HH:MM (e.g., \"2025-11-15 23:59\")</i></html>");
        withdrawInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        withdrawInfo.setForeground(Color.GRAY);
        panel.add(withdrawInfo, "wrap, gapbottom 15");
        
        // Info section
        panel.add(new JLabel(""), "");
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Deadline Information"));
        JTextArea infoText = new JTextArea(
            "Add/Drop Deadline:\n" +
            "  - Students can add or drop courses without penalty before this date\n" +
            "  - After this date, dropped courses appear on transcript with 'W' grade\n\n" +
            "Withdrawal Deadline:\n" +
            "  - Final date for withdrawing from courses\n" +
            "  - After this date, students receive letter grades (cannot withdraw)"
        );
        infoText.setEditable(false);
        infoText.setBackground(panel.getBackground());
        infoText.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(infoText, BorderLayout.CENTER);
        panel.add(infoPanel, "span, grow, wrap");
        
        return panel;
    }
    
    private JPanel createAnnouncementsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("System Announcement (displayed on all dashboards):");
        label.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(label, BorderLayout.NORTH);
        
        announcementArea = new JTextArea(10, 50);
        announcementArea.setLineWrap(true);
        announcementArea.setWrapStyleWord(true);
        announcementArea.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(announcementArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("<html><i>Leave empty to hide announcement banner</i></html>");
        infoLabel.setForeground(Color.GRAY);
        infoPanel.add(infoLabel);
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void loadSettings() {
        SwingWorker<Settings, Void> worker = new SwingWorker<>() {
            @Override
            protected Settings doInBackground() throws Exception {
                return settingsDAO.getSettings();
            }
            
            @Override
            protected void done() {
                try {
                    currentSettings = get();
                    if (currentSettings != null) {
                        displaySettings(currentSettings);
                    } else {
                        logger.warn("No settings found in database, creating and persisting defaults");
                        currentSettings = createDefaultSettings();
                        
                        // Persist default settings to database
                        try {
                            settingsDAO.updateSettings(currentSettings);
                            logger.info("Successfully persisted default settings to database");
                        } catch (Exception persistEx) {
                            logger.error("Failed to persist default settings to database", persistEx);
                            JOptionPane.showMessageDialog(SettingsPanel.this,
                                "Warning: Default settings could not be saved to database.\n" +
                                "They will be used for this session only.\nError: " + persistEx.getMessage(),
                                "Persistence Warning",
                                JOptionPane.WARNING_MESSAGE);
                        }
                        
                        displaySettings(currentSettings);
                    }
                } catch (Exception e) {
                    logger.error("Error loading settings", e);
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                        "Error loading settings: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void displaySettings(Settings settings) {
        maintenanceModeCheckbox.setSelected(settings.isMaintenanceMode());
        registrationEnabledCheckbox.setSelected(settings.isRegistrationEnabled());
        
        if (settings.getCurrentSemester() != null) {
            currentSemesterField.setText(settings.getCurrentSemester());
        }
        currentYearSpinner.setValue(settings.getCurrentYear());
        
        if (settings.getAddDropDeadline() != null) {
            addDropDeadlineField.setText(settings.getAddDropDeadline().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        
        if (settings.getWithdrawalDeadline() != null) {
            withdrawalDeadlineField.setText(settings.getWithdrawalDeadline().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        
        if (settings.getAnnouncement() != null) {
            announcementArea.setText(settings.getAnnouncement());
        }
    }
    
    private void saveSettings() {
        // Use existing settings object to preserve ID/key, or create new if none exists
        Settings settings;
        if (currentSettings != null) {
            // Update existing settings in-place to preserve ID/key fields
            settings = currentSettings;
        } else {
            // Create new settings only if no current settings exist
            settings = new Settings();
            logger.warn("No current settings object exists, creating new settings record");
        }
        
        // Validate and update settings fields from form
        settings.setMaintenanceMode(maintenanceModeCheckbox.isSelected());
        settings.setRegistrationEnabled(registrationEnabledCheckbox.isSelected());
        
        // Validate semester field - must not be empty
        String semester = currentSemesterField.getText().trim();
        if (semester.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Current Semester cannot be empty.\nPlease enter a semester (e.g., Fall, Spring, Summer).",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            currentSemesterField.requestFocusInWindow();
            return;
        }
        settings.setCurrentSemester(semester);
        settings.setCurrentYear((Integer) currentYearSpinner.getValue());
        
        // Parse deadlines
        try {
            String addDropStr = addDropDeadlineField.getText().trim();
            if (!addDropStr.isEmpty()) {
                settings.setAddDropDeadline(LocalDateTime.parse(addDropStr, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            
            String withdrawalStr = withdrawalDeadlineField.getText().trim();
            if (!withdrawalStr.isEmpty()) {
                settings.setWithdrawalDeadline(LocalDateTime.parse(withdrawalStr, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Invalid date format. Please use: YYYY-MM-DD HH:MM",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate deadline order: withdrawal must be after add/drop
        if (settings.getAddDropDeadline() != null && settings.getWithdrawalDeadline() != null) {
            if (!settings.getWithdrawalDeadline().isAfter(settings.getAddDropDeadline())) {
                JOptionPane.showMessageDialog(this,
                    "Withdrawal deadline must be after add/drop deadline.\n" +
                    "Please adjust the deadlines so withdrawal comes later.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        String announcement = announcementArea.getText().trim();
        settings.setAnnouncement(announcement.isEmpty() ? null : announcement);
        
        // Save to database
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                settingsDAO.updateSettings(settings);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    currentSettings = settings;
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                        "Settings saved successfully!\n\nNote: Some changes (like maintenance mode) take effect immediately.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error saving settings", e);
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                        "Error saving settings: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void resetToDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to reset all settings to defaults?\n" +
            "This will:\n" +
            "  • Disable maintenance mode\n" +
            "  • Enable registration\n" +
            "  • Clear deadlines and announcements\n" +
            "  • Reset semester to Fall 2025",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            Settings defaults = createDefaultSettings();
            
            // Preserve any ID or key fields from current settings if they exist
            // This ensures the reset updates existing records rather than creating new ones
            if (currentSettings != null) {
                if (currentSettings.getKey() != null) {
                    defaults.setKey(currentSettings.getKey());
                }
            }
            
            displaySettings(defaults);
            
            // Save defaults
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    settingsDAO.updateSettings(defaults);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        currentSettings = defaults;
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Settings reset to defaults successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Error resetting settings", e);
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Error resetting settings: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private Settings createDefaultSettings() {
        Settings settings = new Settings();
        settings.setMaintenanceMode(false);
        settings.setRegistrationEnabled(true);
        settings.setCurrentSemester("Fall");
        settings.setCurrentYear(Year.now().getValue());
        settings.setAddDropDeadline(null);
        settings.setWithdrawalDeadline(null);
        settings.setAnnouncement(null);
        return settings;
    }
}
