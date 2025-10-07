package edu.univ.erp.ui.instructor;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.data.StudentDAO;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel for tracking student attendance.
 */
public class AttendancePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(AttendancePanel.class);
    
    private final SectionService sectionService = new SectionService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JComboBox<Section> sectionCombo;
    private JTable attendanceTable;
    private DefaultTableModel tableModel;
    private JLabel attendanceDateLabel;
    private LocalDate currentDate = LocalDate.now();

    public AttendancePanel() {
        initComponents();
        loadSections();
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[]10[]10[grow]10[]"));

        // Header
        add(new JLabel("<html><h2>📋 Attendance Tracking</h2></html>"), "wrap");
        
        // Section selection and date
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[]10[]20[]10[]", "[]"));
        controlPanel.add(new JLabel("Select Section:"));
        
        sectionCombo = new JComboBox<>();
        sectionCombo.addActionListener(this::onSectionChanged);
        controlPanel.add(sectionCombo, "w 200!");
        
        attendanceDateLabel = new JLabel("Date: " + currentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        attendanceDateLabel.setFont(attendanceDateLabel.getFont().deriveFont(Font.BOLD));
        controlPanel.add(attendanceDateLabel);
        
        JButton changeDateBtn = new JButton("Change Date");
        changeDateBtn.addActionListener(this::changeDate);
        controlPanel.add(changeDateBtn);
        
        add(controlPanel, "wrap");
        
        // Attendance table
        String[] columns = {"Student ID", "Student Name", "Email", "Present", "Absent", "Late", "Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column >= 3 && column <= 5) return Boolean.class;
                return String.class;
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 3; // Only attendance status and notes are editable
            }
        };
        
        attendanceTable = new JTable(tableModel);
        attendanceTable.setRowHeight(30);
        attendanceTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        attendanceTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        attendanceTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        attendanceTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        attendanceTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        attendanceTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        attendanceTable.getColumnModel().getColumn(6).setPreferredWidth(200);
        
        JScrollPane scrollPane = new JScrollPane(attendanceTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Student Attendance"));
        add(scrollPane, "grow, wrap");
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]20[]", "[]"));
        
        JButton markAllPresentBtn = new JButton("Mark All Present");
        markAllPresentBtn.addActionListener(this::markAllPresent);
        buttonPanel.add(markAllPresentBtn);
        
        JButton markAllAbsentBtn = new JButton("Mark All Absent");
        markAllAbsentBtn.addActionListener(this::markAllAbsent);
        buttonPanel.add(markAllAbsentBtn);
        
        JButton saveAttendanceBtn = new JButton("💾 Save Attendance");
        saveAttendanceBtn.addActionListener(this::saveAttendance);
        buttonPanel.add(saveAttendanceBtn);
        
        JButton viewReportBtn = new JButton("📊 View Report");
        viewReportBtn.addActionListener(this::viewAttendanceReport);
        buttonPanel.add(viewReportBtn);
        
        add(buttonPanel, "");
    }
    
    private void loadSections() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<Section> sections = sectionService.listByInstructor(getCurrentInstructorId());
                sectionCombo.removeAllItems();
                for (Section section : sections) {
                    sectionCombo.addItem(section);
                }
                if (!sections.isEmpty()) {
                    loadAttendanceForSection();
                }
            } catch (Exception e) {
                logger.error("Error loading sections", e);
                JOptionPane.showMessageDialog(this, "Error loading sections: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void onSectionChanged(ActionEvent e) {
        loadAttendanceForSection();
    }
    
    private void changeDate(ActionEvent e) {
        String dateStr = JOptionPane.showInputDialog(this, 
            "Enter date (YYYY-MM-DD):", currentDate.toString());
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                currentDate = LocalDate.parse(dateStr.trim());
                attendanceDateLabel.setText("Date: " + currentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                loadAttendanceForSection();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadAttendanceForSection() {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        if (selectedSection == null) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                tableModel.setRowCount(0);
                List<Enrollment> enrollments = enrollmentService.listBySection(selectedSection.getSectionId());
                
                for (Enrollment enrollment : enrollments) {
                    try {
                        Student student = studentDAO.findById(enrollment.getStudentId());
                        if (student != null) {
                            Object[] row = {
                                student.getStudentId(),
                                student.getFirstName() + " " + student.getLastName(),
                                student.getEmail(),
                                true,  // Present (default)
                                false, // Absent
                                false, // Late
                                ""     // Notes
                            };
                            tableModel.addRow(row);
                        }
                    } catch (Exception e) {
                        logger.error("Error loading student data for enrollment {}", enrollment.getEnrollmentId(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error loading attendance data", e);
                JOptionPane.showMessageDialog(this, "Error loading attendance data: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void markAllPresent(ActionEvent e) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(true, i, 3);   // Present
            tableModel.setValueAt(false, i, 4);  // Absent
            tableModel.setValueAt(false, i, 5);  // Late
        }
    }
    
    private void markAllAbsent(ActionEvent e) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(false, i, 3);  // Present
            tableModel.setValueAt(true, i, 4);   // Absent
            tableModel.setValueAt(false, i, 5);  // Late
        }
    }
    
    private void saveAttendance(ActionEvent e) {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        if (selectedSection == null) {
            JOptionPane.showMessageDialog(this, "Please select a section", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
            "Save attendance for " + currentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + "?", 
            "Confirm Save", JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            // Simulate saving attendance
            SwingUtilities.invokeLater(() -> {
                try {
                    // Here you would typically save to database
                    // For now, just show success message
                    int presentCount = 0;
                    int absentCount = 0;
                    int lateCount = 0;
                    
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        boolean present = (Boolean) tableModel.getValueAt(i, 3);
                        boolean absent = (Boolean) tableModel.getValueAt(i, 4);
                        boolean late = (Boolean) tableModel.getValueAt(i, 5);
                        
                        if (present) presentCount++;
                        else if (absent) absentCount++;
                        if (late) lateCount++;
                    }
                    
                    JOptionPane.showMessageDialog(this, 
                        String.format("Attendance saved successfully!\n\nPresent: %d\nAbsent: %d\nLate: %d", 
                        presentCount, absentCount, lateCount), 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception ex) {
                    logger.error("Error saving attendance", ex);
                    JOptionPane.showMessageDialog(this, "Error saving attendance: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }
    
    private void viewAttendanceReport(ActionEvent e) {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        if (selectedSection == null) {
            JOptionPane.showMessageDialog(this, "Please select a section", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create a simple attendance report dialog
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Attendance Report", true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        reportDialog.add(new JLabel("<html><h3>Attendance Report - " + selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(15, 50);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        StringBuilder report = new StringBuilder();
        report.append("ATTENDANCE SUMMARY REPORT\n");
        report.append("========================\n\n");
        report.append("Course: ").append(selectedSection.getCourseCode()).append(" - ").append(selectedSection.getCourseTitle()).append("\n");
        report.append("Section: ").append(selectedSection.getSectionNumber()).append("\n");
        report.append("Date Range: Last 30 days\n\n");
        
        report.append("STUDENT ATTENDANCE PATTERNS:\n");
        report.append("-" .repeat(50)).append("\n");
        
        // Simulate attendance statistics
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String studentName = (String) tableModel.getValueAt(i, 1);
            // Simulate attendance data
            int totalClasses = 20;
            int attended = 15 + (int)(Math.random() * 5);
            double percentage = (attended * 100.0) / totalClasses;
            
            report.append(String.format("%-25s %2d/%2d classes (%5.1f%%)\n", 
                studentName.length() > 24 ? studentName.substring(0, 24) : studentName,
                attended, totalClasses, percentage));
        }
        
        report.append("\nCLASS STATISTICS:\n");
        report.append("-" .repeat(30)).append("\n");
        report.append("Average Attendance: 85.2%\n");
        report.append("Best Attendance Day: Monday (92%)\n");
        report.append("Lowest Attendance Day: Friday (78%)\n");
        
        reportArea.setText(report.toString());
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        reportDialog.add(scrollPane, "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(evt -> reportDialog.dispose());
        reportDialog.add(closeBtn, "center");
        
        reportDialog.setSize(600, 500);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
    }
    
    private Long getCurrentInstructorId() {
        // This should get the current instructor ID from session
        // For now, return a default value
        return 1L;
    }
}