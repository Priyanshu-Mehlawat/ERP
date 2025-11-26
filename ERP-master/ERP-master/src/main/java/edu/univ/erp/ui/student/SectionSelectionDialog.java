package edu.univ.erp.ui.student;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Dialog to show available sections for a course and allow enrollment.
 */
public class SectionSelectionDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(SectionSelectionDialog.class);
    
    private final Course course;
    private final SectionService sectionService = new SectionService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JTable table;
    private DefaultTableModel model;
    private JLabel statusLabel;
    private Student currentStudent;

    public SectionSelectionDialog(Window parent, Course course) {
        super(parent, "Available Sections - " + course.getCode(), ModalityType.APPLICATION_MODAL);
        this.course = course;
        loadCurrentStudent();
        initComponents();
        loadSections();
    }

    private void loadCurrentStudent() {
        try {
            Long userId = SessionManager.getInstance().getCurrentUser().getUserId();
            if (userId == null) {
                logger.warn("User ID is null, cannot load student data");
                return;
            }
            currentStudent = studentDAO.findByUserId(userId);
            if (currentStudent == null) {
                logger.warn("No student found for user ID: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error loading current student", e);
            JOptionPane.showMessageDialog(this, "Error loading student data: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setSize(800, 400);
        setLocationRelativeTo(getParent());

        // Course info panel
        JPanel infoPanel = new JPanel(new MigLayout("insets 10", "[grow]", "[][]"));
        infoPanel.add(new JLabel("<html><b>" + course.getCode() + " - " + course.getTitle() + "</b></html>"), "wrap");
        if (course.getDescription() != null) {
            infoPanel.add(new JLabel("<html><i>" + course.getDescription() + "</i></html>"), "wrap");
        }
        infoPanel.add(new JLabel("Credits: " + course.getCredits() + 
            (course.getDepartment() != null ? " | Department: " + course.getDepartment() : "")));
        add(infoPanel, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel(new Object[]{"Section", "Instructor", "Schedule", "Room", "Enrolled", "Available", "Action"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 6; }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        
        // Add button column
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor());
        
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready");
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadSections() {
        SwingWorker<List<Section>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Section> doInBackground() {
                return sectionService.listByCourse(course.getCourseId(), "Fall", 2025); // Current semester
            }

            @Override
            protected void done() {
                try {
                    List<Section> sections = get();
                    model.setRowCount(0);
                    for (Section s : sections) {
                        String schedule = "";
                        if (s.getDayOfWeek() != null && s.getStartTime() != null && s.getEndTime() != null) {
                            schedule = s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime();
                        }
                        
                        String instructor = s.getInstructorName() != null ? s.getInstructorName() : "TBA";
                        String room = s.getRoom() != null ? s.getRoom() : "TBA";
                        
                        model.addRow(new Object[]{
                            s.getSectionNumber(),
                            instructor,
                            schedule,
                            room,
                            s.getEnrolled() + "/" + s.getCapacity(),
                            s.getAvailableSeats(),
                            s.hasAvailableSeats() ? "Enroll" : "Full"
                        });
                    }
                    statusLabel.setText("Found " + sections.size() + " section(s)");
                } catch (Exception e) {
                    logger.error("Failed to load sections", e);
                    statusLabel.setText("Error loading sections: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void enrollInSection(int row) {
        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, "Student data not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (row < 0) {
            logger.warn("Invalid row index for enrollment: {}", row);
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    List<Section> sections = sectionService.listByCourse(course.getCourseId(), "Fall", 2025);
                    if (row < sections.size()) {
                        Section section = sections.get(row);
                        if (section == null) {
                            return "ERROR: Section not found";
                        }
                        return enrollmentService.enroll(currentStudent.getStudentId(), section.getSectionId());
                    } else {
                        return "ERROR: Invalid section selection";
                    }
                } catch (Exception e) {
                    logger.error("Error during enrollment", e);
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if ("ENROLLED".equals(result)) {
                        JOptionPane.showMessageDialog(SectionSelectionDialog.this, 
                            "Successfully enrolled in " + course.getCode() + "!", 
                            "Enrollment Success", JOptionPane.INFORMATION_MESSAGE);
                        loadSections(); // Refresh to show updated capacity
                    } else {
                        JOptionPane.showMessageDialog(SectionSelectionDialog.this, 
                            "Enrollment failed: " + result, 
                            "Enrollment Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SectionSelectionDialog.this, 
                        "Enrollment error: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Button renderer for table
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setEnabled("Enroll".equals(value));
            return this;
        }
    }

    // Button editor for table
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = value != null ? value.toString() : "";
            button.setText(label);
            button.setEnabled("Enroll".equals(label));
            isPushed = true;
            currentRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && "Enroll".equals(label)) {
                enrollInSection(currentRow);
            }
            isPushed = false;
            return label;
        }
    }
}