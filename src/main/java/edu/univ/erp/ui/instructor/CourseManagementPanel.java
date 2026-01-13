package edu.univ.erp.ui.instructor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;

/**
 * Panel for instructors to manage their assigned course sections.
 */
public class CourseManagementPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(CourseManagementPanel.class);

    private final SectionService sectionService = new SectionService();
    private final SectionDAO sectionDAO = new SectionDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    
    private JTable sectionsTable;
    private DefaultTableModel sectionsModel;
    private JLabel statusLabel;
    private Instructor currentInstructor;
    
    // Store loaded sections for easy access
    private List<Section> loadedSections = new ArrayList<>();

    public CourseManagementPanel() {
        loadCurrentInstructor();
        initComponents();
        loadSections();
    }

    private void loadCurrentInstructor() {
        try {
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                logger.warn("No current user in session, cannot load instructor data");
                return;
            }
            Long userId = currentUser.getUserId();
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
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][grow][]"));

        // Header
        add(new JLabel("<html><h2>My Course Sections</h2></html>"), "wrap");

        // Sections table
        String[] columns = {"Course Code", "Course Title", "Section", "Schedule", "Room", "Enrolled", "Capacity", "Status"};
        sectionsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sectionsTable = new JTable(sectionsModel);
        sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionsTable.setAutoCreateRowSorter(true);
        
        // Set column widths
        sectionsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        sectionsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        sectionsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        sectionsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        sectionsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        sectionsTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        sectionsTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        sectionsTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(sectionsTable);
        add(scrollPane, "grow, wrap");

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[grow][][][][]", ""));
        
        statusLabel = new JLabel("Loading...");
        controlPanel.add(statusLabel, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadSections());
        controlPanel.add(refreshBtn);
        
        JButton editBtn = new JButton("Edit Section");
        editBtn.addActionListener(e -> editSelectedSection());
        controlPanel.add(editBtn);
        
        JButton settingsBtn = new JButton("Section Settings");
        settingsBtn.addActionListener(e -> openSectionSettings());
        controlPanel.add(settingsBtn);
        
        JButton exportBtn = new JButton("Export List");
        exportBtn.addActionListener(e -> exportSectionsList());
        controlPanel.add(exportBtn);
        
        add(controlPanel, "growx");
    }

    private void loadSections() {
        if (currentInstructor == null) {
            statusLabel.setText("Instructor data not available");
            return;
        }

        SwingWorker<List<Section>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Section> doInBackground() {
                try {
                    return sectionService.listByInstructor(currentInstructor.getInstructorId());
                } catch (Exception e) {
                    logger.error("Error loading sections", e);
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Section> sections = get();
                    loadedSections.clear();
                    loadedSections.addAll(sections);
                    sectionsModel.setRowCount(0);
                    
                    for (Section section : sections) {
                        String schedule = "";
                        if (section.getDayOfWeek() != null && section.getStartTime() != null && section.getEndTime() != null) {
                            schedule = section.getDayOfWeek() + " " + section.getStartTime() + "-" + section.getEndTime();
                        }
                        
                        String room = section.getRoom() != null ? section.getRoom() : "TBA";
                        String status = section.getEnrolled() >= section.getCapacity() ? "Full" : "Open";
                        
                        sectionsModel.addRow(new Object[]{
                            section.getCourseCode(),
                            section.getCourseTitle(),
                            section.getSectionNumber(),
                            schedule,
                            room,
                            section.getEnrolled(),
                            section.getCapacity(),
                            status
                        });
                    }
                    
                    statusLabel.setText("Showing " + sections.size() + " section(s)");
                } catch (Exception e) {
                    logger.error("Failed to load sections", e);
                    statusLabel.setText("Error loading sections: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void editSelectedSection() {
        int selectedRow = sectionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int modelRow = sectionsTable.convertRowIndexToModel(selectedRow);
        if (modelRow >= loadedSections.size()) {
            JOptionPane.showMessageDialog(this, "Error: Section data not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Section section = loadedSections.get(modelRow);
        showEditSectionDialog(section);
    }
    
    private void showEditSectionDialog(Section section) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Edit Section - " + section.getCourseCode() + " " + section.getSectionNumber(), 
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new MigLayout("fillx, insets 20", "[right]rel[grow,fill]", "[]10[]10[]10[]10[]"));
        
        // Course info (read-only)
        formPanel.add(new JLabel("Course:"), "");
        JLabel courseLabel = new JLabel(section.getCourseCode() + " - " + section.getCourseTitle());
        courseLabel.setFont(courseLabel.getFont().deriveFont(Font.BOLD));
        formPanel.add(courseLabel, "wrap");
        
        formPanel.add(new JLabel("Section:"), "");
        JLabel sectionLabel = new JLabel(section.getSectionNumber());
        formPanel.add(sectionLabel, "wrap");
        
        // Editable fields
        formPanel.add(new JLabel("Day(s):"), "");
        JComboBox<String> dayCombo = new JComboBox<>(new String[]{
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
            "Monday,Wednesday", "Tuesday,Thursday", "Monday,Wednesday,Friday"
        });
        dayCombo.setEditable(true);
        if (section.getDayOfWeek() != null) {
            dayCombo.setSelectedItem(section.getDayOfWeek());
        }
        formPanel.add(dayCombo, "wrap");
        
        formPanel.add(new JLabel("Start Time:"), "");
        JTextField startTimeField = new JTextField(10);
        if (section.getStartTime() != null) {
            startTimeField.setText(section.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        startPanel.add(startTimeField);
        startPanel.add(new JLabel("  (HH:mm format, e.g., 09:00)"));
        formPanel.add(startPanel, "wrap");
        
        formPanel.add(new JLabel("End Time:"), "");
        JTextField endTimeField = new JTextField(10);
        if (section.getEndTime() != null) {
            endTimeField.setText(section.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        endPanel.add(endTimeField);
        endPanel.add(new JLabel("  (HH:mm format, e.g., 10:30)"));
        formPanel.add(endPanel, "wrap");
        
        formPanel.add(new JLabel("Room:"), "");
        JTextField roomField = new JTextField(section.getRoom() != null ? section.getRoom() : "", 20);
        formPanel.add(roomField, "wrap");
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            // Validate and save
            String dayOfWeek = (String) dayCombo.getSelectedItem();
            String startTimeStr = startTimeField.getText().trim();
            String endTimeStr = endTimeField.getText().trim();
            String room = roomField.getText().trim();
            
            if (dayOfWeek == null || dayOfWeek.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select day(s) of week.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            LocalTime startTime = null;
            LocalTime endTime = null;
            
            try {
                if (!startTimeStr.isEmpty()) {
                    startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
                }
                if (!endTimeStr.isEmpty()) {
                    endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid time format. Use HH:mm (e.g., 09:00)", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
                JOptionPane.showMessageDialog(dialog, "End time must be after start time.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (room.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a room.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Update section
            section.setDayOfWeek(dayOfWeek);
            section.setStartTime(startTime);
            section.setEndTime(endTime);
            section.setRoom(room);
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    sectionDAO.update(section);
                    return true;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(dialog, "Section updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadSections();
                    } catch (Exception ex) {
                        logger.error("Error updating section", ex);
                        JOptionPane.showMessageDialog(dialog, "Error updating section: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openSectionSettings() {
        int selectedRow = sectionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int modelRow = sectionsTable.convertRowIndexToModel(selectedRow);
        if (modelRow >= loadedSections.size()) {
            JOptionPane.showMessageDialog(this, "Error: Section data not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Section section = loadedSections.get(modelRow);
        showSectionSettingsDialog(section);
    }
    
    private void showSectionSettingsDialog(Section section) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Section Settings - " + section.getCourseCode() + " " + section.getSectionNumber(), 
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new MigLayout("fillx, insets 20", "[grow]", "[][grow][]"));
        
        // Section Info Panel
        JPanel infoPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Section Information"));
        
        infoPanel.add(new JLabel("Course:"), "");
        infoPanel.add(new JLabel(section.getCourseCode() + " - " + section.getCourseTitle()), "wrap");
        
        infoPanel.add(new JLabel("Section:"), "");
        infoPanel.add(new JLabel(section.getSectionNumber()), "wrap");
        
        infoPanel.add(new JLabel("Semester:"), "");
        infoPanel.add(new JLabel(section.getSemester() + " " + section.getYear()), "wrap");
        
        infoPanel.add(new JLabel("Schedule:"), "");
        String schedule = "Not set";
        if (section.getDayOfWeek() != null && section.getStartTime() != null && section.getEndTime() != null) {
            schedule = section.getDayOfWeek() + " " + section.getStartTime() + " - " + section.getEndTime();
        }
        infoPanel.add(new JLabel(schedule), "wrap");
        
        infoPanel.add(new JLabel("Room:"), "");
        infoPanel.add(new JLabel(section.getRoom() != null ? section.getRoom() : "TBA"), "wrap");
        
        mainPanel.add(infoPanel, "growx, wrap");
        
        // Capacity Settings Panel
        JPanel capacityPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        capacityPanel.setBorder(BorderFactory.createTitledBorder("Capacity Settings"));
        
        capacityPanel.add(new JLabel("Current Enrollment:"), "");
        JLabel enrolledLabel = new JLabel(String.valueOf(section.getEnrolled()));
        enrolledLabel.setFont(enrolledLabel.getFont().deriveFont(Font.BOLD));
        capacityPanel.add(enrolledLabel, "wrap");
        
        capacityPanel.add(new JLabel("Maximum Capacity:"), "");
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(section.getCapacity(), section.getEnrolled(), 500, 1));
        capacityPanel.add(capacitySpinner, "wrap");
        
        capacityPanel.add(new JLabel("Available Seats:"), "");
        JLabel availableLabel = new JLabel(String.valueOf(section.getAvailableSeats()));
        if (section.getAvailableSeats() == 0) {
            availableLabel.setForeground(Color.RED);
        } else if (section.getAvailableSeats() <= 5) {
            availableLabel.setForeground(Color.ORANGE);
        } else {
            availableLabel.setForeground(new Color(0, 128, 0));
        }
        capacityPanel.add(availableLabel, "wrap");
        
        capacityPanel.add(new JLabel("Status:"), "");
        String status = section.getEnrolled() >= section.getCapacity() ? "FULL" : "OPEN";
        JLabel statusLbl = new JLabel(status);
        statusLbl.setFont(statusLbl.getFont().deriveFont(Font.BOLD));
        statusLbl.setForeground(status.equals("FULL") ? Color.RED : new Color(0, 128, 0));
        capacityPanel.add(statusLbl, "wrap");
        
        // Add note about capacity
        capacityPanel.add(new JLabel(""), "");
        JLabel noteLabel = new JLabel("<html><i>Note: Capacity cannot be set below current enrollment.</i></html>");
        noteLabel.setForeground(Color.GRAY);
        capacityPanel.add(noteLabel, "wrap");
        
        mainPanel.add(capacityPanel, "growx, wrap");
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveBtn = new JButton("Save Capacity");
        JButton closeBtn = new JButton("Close");
        
        saveBtn.addActionListener(e -> {
            int newCapacity = (Integer) capacitySpinner.getValue();
            
            if (newCapacity < section.getEnrolled()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Capacity cannot be less than current enrollment (" + section.getEnrolled() + ").", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            section.setCapacity(newCapacity);
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    sectionDAO.update(section);
                    return true;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(dialog, "Capacity updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadSections();
                    } catch (Exception ex) {
                        logger.error("Error updating capacity", ex);
                        JOptionPane.showMessageDialog(dialog, "Error updating capacity: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });
        
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(closeBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportSectionsList() {
        if (sectionsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Sections List");
        fileChooser.setSelectedFile(new java.io.File("my_sections.csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        java.io.File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new java.io.File(file.getAbsolutePath() + ".csv");
        }
        
        // Check if file exists
        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                "File already exists. Do you want to overwrite it?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header
            writer.println("Course Code,Course Title,Section,Schedule,Room,Enrolled,Capacity,Status");
            
            // Write data
            for (int i = 0; i < sectionsModel.getRowCount(); i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < sectionsModel.getColumnCount(); j++) {
                    if (j > 0) line.append(",");
                    Object value = sectionsModel.getValueAt(i, j);
                    String cellValue = value != null ? value.toString() : "";
                    // Escape commas and quotes in CSV
                    if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                        cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                    }
                    line.append(cellValue);
                }
                writer.println(line);
            }
            
            JOptionPane.showMessageDialog(this,
                "Sections list exported successfully!\n\nFile: " + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            logger.error("Error exporting sections list", e);
            JOptionPane.showMessageDialog(this,
                "Error exporting file: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}