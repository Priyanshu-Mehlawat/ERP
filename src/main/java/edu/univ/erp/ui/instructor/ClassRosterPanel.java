package edu.univ.erp.ui.instructor;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.util.List;

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
        
        SwingWorker<List<Enrollment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Enrollment> doInBackground() {
                try {
                    return enrollmentService.listBySection(section.getSectionId());
                } catch (Exception e) {
                    logger.error("Error loading roster", e);
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Enrollment> enrollments = get();
                    studentsModel.setRowCount(0);
                    
                    for (Enrollment enrollment : enrollments) {
                        // Get student information separately
                        Student student = null;
                        try {
                            student = studentDAO.findById(enrollment.getStudentId());
                        } catch (SQLException e) {
                            logger.error("Error loading student data for ID: {}", enrollment.getStudentId(), e);
                        }
                        
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
        
        JOptionPane.showMessageDialog(this, 
            "Email functionality will be implemented in future version.",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportRoster() {
        if (studentsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(this, 
            "Export functionality will be implemented in future version.",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showClassStats() {
        if (studentsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data available for statistics.", "Statistics", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int totalStudents = studentsModel.getRowCount();
        int enrolledCount = 0;
        int droppedCount = 0;
        
        for (int i = 0; i < totalStudents; i++) {
            String status = (String) studentsModel.getValueAt(i, 5);
            if ("ENROLLED".equals(status)) {
                enrolledCount++;
            } else if ("DROPPED".equals(status)) {
                droppedCount++;
            }
        }
        
        String stats = String.format(
            "Class Statistics:\n\n" +
            "Total Students: %d\n" +
            "Currently Enrolled: %d\n" +
            "Dropped: %d\n",
            totalStudents, enrolledCount, droppedCount
        );
        
        JOptionPane.showMessageDialog(this, stats, "Class Statistics", JOptionPane.INFORMATION_MESSAGE);
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
}