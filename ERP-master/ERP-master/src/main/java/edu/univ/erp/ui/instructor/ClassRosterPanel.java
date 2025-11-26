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
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;

/**
 * Panel for instructors to view class rosters and student information.
 */
public class ClassRosterPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ClassRosterPanel.class);

    private final SectionService sectionService = new SectionService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JComboBox<SectionItem> sectionCombo;
    private JTable studentsTable;
    private DefaultTableModel studentsModel;
    private JLabel statusLabel;
    private Instructor currentInstructor;

    public ClassRosterPanel() {
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
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][][grow][]"));

        // Header
        add(new JLabel("<html><h2>Class Roster</h2></html>"), "wrap");

        // Section selection
        JPanel sectionPanel = new JPanel(new MigLayout("insets 0", "[]10[grow][]", ""));
        sectionPanel.add(new JLabel("Select Section:"));
        
        sectionCombo = new JComboBox<>();
        sectionCombo.addActionListener(e -> loadRoster());
        sectionPanel.add(sectionCombo, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadSections());
        sectionPanel.add(refreshBtn);
        
        add(sectionPanel, "growx, wrap");

        // Students table
        String[] columns = {"Student ID", "Name", "Email", "Program", "Year", "Status", "Enrolled Date"};
        studentsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentsTable = new JTable(studentsModel);
        studentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentsTable.setAutoCreateRowSorter(true);
        
        // Set column widths
        studentsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        studentsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        studentsTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        studentsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        studentsTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        studentsTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        studentsTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(studentsTable);
        add(scrollPane, "grow, wrap");

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[grow][][][]", ""));
        
        statusLabel = new JLabel("Select a section to view roster");
        controlPanel.add(statusLabel, "growx");
        
        JButton emailBtn = new JButton("Email Students");
        emailBtn.addActionListener(e -> emailStudents());
        controlPanel.add(emailBtn);
        
        JButton exportBtn = new JButton("Export Roster");
        exportBtn.addActionListener(e -> exportRoster());
        controlPanel.add(exportBtn);
        
        JButton statsBtn = new JButton("Class Stats");
        statsBtn.addActionListener(e -> showClassStats());
        controlPanel.add(statsBtn);
        
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
                    sectionCombo.removeAllItems();
                    sectionCombo.addItem(new SectionItem(null, "-- Select Section --"));
                    
                    for (Section section : sections) {
                        String display = section.getCourseCode() + " - " + section.getSectionNumber() + 
                                       " (" + section.getEnrolled() + " students)";
                        sectionCombo.addItem(new SectionItem(section, display));
                    }
                    
                    if (sections.isEmpty()) {
                        statusLabel.setText("No sections assigned");
                    } else {
                        statusLabel.setText("Found " + sections.size() + " section(s)");
                    }
                } catch (Exception e) {
                    logger.error("Failed to load sections", e);
                    statusLabel.setText("Error loading sections: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadRoster() {
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.section == null) {
            studentsModel.setRowCount(0);
            statusLabel.setText("Select a section to view roster");
            return;
        }

        Section section = selectedItem.section;
        
        SwingWorker<RosterData, Void> worker = new SwingWorker<>() {
            @Override
            protected RosterData doInBackground() {
                try {
                    // Fetch enrollments first
                    List<Enrollment> enrollments = enrollmentService.listBySection(section.getSectionId());
                    
                    if (enrollments.isEmpty()) {
                        return new RosterData(enrollments, Map.of());
                    }
                    
                    // Extract all student IDs for batch fetch
                    List<Long> studentIds = enrollments.stream()
                        .map(Enrollment::getStudentId)
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());
                    
                    // Batch fetch all students in a single query
                    Map<Long, Student> studentMap = new java.util.HashMap<>();
                    try {
                        List<Student> students = studentDAO.findByIds(studentIds);
                        for (Student student : students) {
                            studentMap.put(student.getStudentId(), student);
                        }
                    } catch (SQLException e) {
                        logger.error("Error batch loading student data for IDs: {}", studentIds, e);
                        // Continue with empty map - will show N/A values
                    }
                    
                    return new RosterData(enrollments, studentMap);
                    
                } catch (Exception e) {
                    logger.error("Error loading roster data", e);
                    return new RosterData(List.of(), Map.of());
                }
            }

            @Override
            protected void done() {
                try {
                    RosterData rosterData = get();
                    List<Enrollment> enrollments = rosterData.enrollments;
                    Map<Long, Student> studentMap = rosterData.studentMap;
                    
                    studentsModel.setRowCount(0);
                    
                    for (Enrollment enrollment : enrollments) {
                        // Get pre-fetched student information from map
                        Student student = studentMap.get(enrollment.getStudentId());
                        
                        String studentName = "N/A";
                        String studentEmail = "N/A";
                        String studentProgram = "N/A";
                        String studentYear = "N/A";
                        
                        if (student != null) {
                            studentName = student.getFirstName() + " " + student.getLastName();
                            studentEmail = student.getEmail() != null ? student.getEmail() : "N/A";
                            studentProgram = student.getProgram() != null ? student.getProgram() : "N/A";
                            studentYear = String.valueOf(student.getYear());
                        }
                        
                        studentsModel.addRow(new Object[]{
                            enrollment.getStudentId(),
                            studentName,
                            studentEmail,
                            studentProgram,
                            studentYear,
                            enrollment.getStatus(),
                            enrollment.getEnrolledDate() != null ? 
                                enrollment.getEnrolledDate().toLocalDate().toString() : "N/A"
                        });
                    }
                    
                    String courseInfo = section.getCourseCode() + " - " + section.getSectionNumber();
                    statusLabel.setText("Showing " + enrollments.size() + " student(s) in " + courseInfo);
                } catch (Exception e) {
                    logger.error("Failed to load roster", e);
                    statusLabel.setText("Error loading roster: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void emailStudents() {
        if (studentsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No students to email.", "Email", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        String sectionInfo = selectedItem != null && selectedItem.section != null ? 
            selectedItem.section.getCourseCode() + " - " + selectedItem.section.getSectionNumber() : "Selected Section";
        
        // Collect all email addresses
        StringBuilder emailList = new StringBuilder();
        int validEmailCount = 0;
        for (int i = 0; i < studentsModel.getRowCount(); i++) {
            String email = (String) studentsModel.getValueAt(i, 2);
            if (email != null && !email.equals("N/A") && email.contains("@")) {
                if (emailList.length() > 0) {
                    emailList.append("; ");
                }
                emailList.append(email);
                validEmailCount++;
            }
        }
        
        if (validEmailCount == 0) {
            JOptionPane.showMessageDialog(this, "No valid email addresses found for students.", "Email", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        final int finalValidEmails = validEmailCount;
        
        // Show email composition dialog
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Email Students - " + sectionInfo, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new MigLayout("fillx, insets 15", "[right]rel[grow,fill]", "[]10[]10[grow]10[]"));
        
        formPanel.add(new JLabel("To:"), "");
        JTextArea toField = new JTextArea(emailList.toString(), 2, 40);
        toField.setEditable(false);
        toField.setLineWrap(true);
        toField.setWrapStyleWord(true);
        toField.setBackground(new Color(245, 245, 245));
        JScrollPane toScroll = new JScrollPane(toField);
        formPanel.add(toScroll, "wrap");
        
        formPanel.add(new JLabel("Subject:"), "");
        JTextField subjectField = new JTextField("[" + sectionInfo + "] ", 40);
        formPanel.add(subjectField, "wrap");
        
        formPanel.add(new JLabel("Message:"), "top");
        JTextArea messageArea = new JTextArea(10, 40);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);
        formPanel.add(messageScroll, "wrap, grow");
        
        formPanel.add(new JLabel(""), "");
        JLabel infoLabel = new JLabel("<html><i>Sending to " + finalValidEmails + " student(s)</i></html>");
        infoLabel.setForeground(Color.GRAY);
        formPanel.add(infoLabel, "wrap");
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton sendBtn = new JButton("Send Email");
        JButton copyBtn = new JButton("Copy Emails");
        JButton cancelBtn = new JButton("Cancel");
        
        sendBtn.addActionListener(e -> {
            String subject = subjectField.getText().trim();
            String message = messageArea.getText().trim();
            
            if (subject.isEmpty() || message.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter both subject and message.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Simulate sending email (in production, integrate with actual email service)
            JOptionPane.showMessageDialog(dialog, 
                "Email prepared successfully!\n\n" +
                "In a production environment, this would send emails via SMTP.\n" +
                "Recipients: " + finalValidEmails + " student(s)\n" +
                "Subject: " + subject + "\n\n" +
                "For now, emails have been copied to clipboard for manual sending.",
                "Email Ready",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Copy to clipboard for manual sending
            String fullEmail = "To: " + emailList.toString() + "\nSubject: " + subject + "\n\n" + message;
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(fullEmail), null);
            
            dialog.dispose();
        });
        
        copyBtn.addActionListener(e -> {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(emailList.toString()), null);
            JOptionPane.showMessageDialog(dialog, "Email addresses copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(sendBtn);
        buttonPanel.add(copyBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setSize(550, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportRoster() {
        if (studentsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        String defaultFileName = "class_roster";
        if (selectedItem != null && selectedItem.section != null) {
            defaultFileName = selectedItem.section.getCourseCode() + "_" + 
                selectedItem.section.getSectionNumber() + "_roster";
            defaultFileName = defaultFileName.replace(" ", "_");
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Class Roster");
        fileChooser.setSelectedFile(new java.io.File(defaultFileName + ".csv"));
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
            // Write section info header
            if (selectedItem != null && selectedItem.section != null) {
                Section section = selectedItem.section;
                writer.println("# Class Roster Export");
                writer.println("# Course: " + section.getCourseCode() + " - " + section.getCourseTitle());
                writer.println("# Section: " + section.getSectionNumber());
                writer.println("# Semester: " + section.getSemester() + " " + section.getYear());
                writer.println("# Export Date: " + java.time.LocalDate.now());
                writer.println();
            }
            
            // Write column headers
            writer.println("Student ID,Name,Email,Program,Year,Status,Enrolled Date");
            
            // Write data
            for (int i = 0; i < studentsModel.getRowCount(); i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < studentsModel.getColumnCount(); j++) {
                    if (j > 0) line.append(",");
                    Object value = studentsModel.getValueAt(i, j);
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
                "Class roster exported successfully!\n\nFile: " + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            logger.error("Error exporting roster", e);
            JOptionPane.showMessageDialog(this,
                "Error exporting file: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showClassStats() {
        if (studentsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data available for statistics.", "Statistics", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        Section section = selectedItem != null ? selectedItem.section : null;
        
        // Count status
        int totalStudents = studentsModel.getRowCount();
        int enrolledCount = 0;
        int droppedCount = 0;
        int completedCount = 0;
        
        // Program distribution
        Map<String, Integer> programCounts = new java.util.HashMap<>();
        
        // Year distribution
        Map<String, Integer> yearCounts = new java.util.HashMap<>();
        
        for (int i = 0; i < totalStudents; i++) {
            String status = (String) studentsModel.getValueAt(i, 5);
            String program = (String) studentsModel.getValueAt(i, 3);
            String year = String.valueOf(studentsModel.getValueAt(i, 4));
            
            if ("ENROLLED".equals(status)) {
                enrolledCount++;
            } else if ("DROPPED".equals(status)) {
                droppedCount++;
            } else if ("COMPLETED".equals(status)) {
                completedCount++;
            }
            
            if (program != null && !program.equals("N/A")) {
                programCounts.merge(program, 1, Integer::sum);
            }
            
            if (year != null && !year.equals("N/A")) {
                yearCounts.merge("Year " + year, 1, Integer::sum);
            }
        }
        
        // Build stats dialog
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Class Statistics", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new MigLayout("fillx, insets 15", "[grow]", "[][grow][]"));
        
        // Section info
        String headerText = "<html><h3>Class Statistics</h3>";
        if (section != null) {
            headerText += "<p>" + section.getCourseCode() + " - " + section.getCourseTitle() + 
                " (Section " + section.getSectionNumber() + ")</p>";
        }
        headerText += "</html>";
        mainPanel.add(new JLabel(headerText), "wrap");
        
        // Stats panel
        JPanel statsPanel = new JPanel(new MigLayout("fillx", "[grow][grow]", ""));
        
        // Enrollment stats
        JPanel enrollmentPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]", ""));
        enrollmentPanel.setBorder(BorderFactory.createTitledBorder("Enrollment Statistics"));
        
        enrollmentPanel.add(new JLabel("Total Students:"), "");
        JLabel totalLabel = new JLabel(String.valueOf(totalStudents));
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));
        enrollmentPanel.add(totalLabel, "wrap");
        
        enrollmentPanel.add(new JLabel("Currently Enrolled:"), "");
        JLabel enrolledLabel = new JLabel(String.valueOf(enrolledCount));
        enrolledLabel.setForeground(new Color(0, 128, 0));
        enrollmentPanel.add(enrolledLabel, "wrap");
        
        enrollmentPanel.add(new JLabel("Dropped:"), "");
        JLabel droppedLabel = new JLabel(String.valueOf(droppedCount));
        if (droppedCount > 0) droppedLabel.setForeground(Color.RED);
        enrollmentPanel.add(droppedLabel, "wrap");
        
        enrollmentPanel.add(new JLabel("Completed:"), "");
        enrollmentPanel.add(new JLabel(String.valueOf(completedCount)), "wrap");
        
        if (section != null) {
            enrollmentPanel.add(new JLabel("Section Capacity:"), "");
            enrollmentPanel.add(new JLabel(section.getEnrolled() + " / " + section.getCapacity()), "wrap");
            
            int availableSeats = section.getCapacity() - section.getEnrolled();
            enrollmentPanel.add(new JLabel("Available Seats:"), "");
            JLabel seatsLabel = new JLabel(String.valueOf(availableSeats));
            if (availableSeats == 0) {
                seatsLabel.setForeground(Color.RED);
                seatsLabel.setText("0 (FULL)");
            } else if (availableSeats <= 5) {
                seatsLabel.setForeground(Color.ORANGE);
            }
            enrollmentPanel.add(seatsLabel, "wrap");
        }
        
        statsPanel.add(enrollmentPanel, "grow");
        
        // Distribution panel
        JPanel distPanel = new JPanel(new MigLayout("fillx", "[grow]", ""));
        distPanel.setBorder(BorderFactory.createTitledBorder("Student Distribution"));
        
        // Program distribution
        if (!programCounts.isEmpty()) {
            distPanel.add(new JLabel("<html><b>By Program:</b></html>"), "wrap");
            for (Map.Entry<String, Integer> entry : programCounts.entrySet()) {
                distPanel.add(new JLabel("  • " + entry.getKey() + ": " + entry.getValue()), "wrap");
            }
            distPanel.add(new JLabel(" "), "wrap");
        }
        
        // Year distribution
        if (!yearCounts.isEmpty()) {
            distPanel.add(new JLabel("<html><b>By Year:</b></html>"), "wrap");
            java.util.List<String> sortedYears = new java.util.ArrayList<>(yearCounts.keySet());
            java.util.Collections.sort(sortedYears);
            for (String year : sortedYears) {
                distPanel.add(new JLabel("  • " + year + ": " + yearCounts.get(year)), "wrap");
            }
        }
        
        statsPanel.add(distPanel, "grow");
        
        mainPanel.add(statsPanel, "grow, wrap");
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        
        // Close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Helper class for combo box items
    private static class SectionItem {
        final Section section;
        final String display;

        SectionItem(Section section, String display) {
            this.section = section;
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }
    
    // Data class to hold roster information from background thread
    private static class RosterData {
        final List<Enrollment> enrollments;
        final Map<Long, Student> studentMap;
        
        RosterData(List<Enrollment> enrollments, Map<Long, Student> studentMap) {
            this.enrollments = enrollments;
            this.studentMap = studentMap;
        }
    }
}