package edu.univ.erp.ui.student;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel to display student's current course enrollments.
 */
public class MyCoursesPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MyCoursesPanel.class);

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final StudentDAO studentDAO = new StudentDAO();
    private JTable table;
    private DefaultTableModel model;
    private JLabel statusLabel;
    private Student currentStudent;

    public MyCoursesPanel() {
        loadCurrentStudent();
        initComponents();
        loadEnrollments();
    }

    private void loadCurrentStudent() {
        try {
            Long userId = SessionManager.getInstance().getCurrentUser().getUserId();
            currentStudent = studentDAO.findByUserId(userId);
        } catch (SQLException e) {
            logger.error("Error loading current student", e);
        }
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][grow][]"));

        // Header
        add(new JLabel("<html><h2>My Courses</h2></html>"), "wrap");

        // Table
        model = new DefaultTableModel(new Object[]{"Course", "Section", "Instructor", "Status", "Final Grade", "Action"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 5; }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);

        // Add button column for drop action
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor());

        add(new JScrollPane(table), "grow, wrap");

        // Status and controls
        JPanel bottomPanel = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
        statusLabel = new JLabel("Loading...");
        bottomPanel.add(statusLabel, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadEnrollments());
        bottomPanel.add(refreshBtn);
        
        add(bottomPanel, "growx");
    }

    private void loadEnrollments() {
        if (currentStudent == null) {
            statusLabel.setText("Student data not available");
            return;
        }

        SwingWorker<List<Enrollment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Enrollment> doInBackground() {
                return enrollmentService.listByStudent(currentStudent.getStudentId());
            }

            @Override
            protected void done() {
                try {
                    List<Enrollment> enrollments = get();
                    model.setRowCount(0);
                    
                    int activeCount = 0;
                    for (Enrollment e : enrollments) {
                        String courseInfo = e.getCourseCode() + " - " + e.getCourseTitle();
                        String instructor = e.getInstructorName() != null ? e.getInstructorName() : "TBA";
                        String finalGrade = e.getFinalGrade() != null ? e.getFinalGrade() : "-";
                        
                        String action = "";
                        if ("ENROLLED".equals(e.getStatus())) {
                            action = "Drop";
                            activeCount++;
                        }
                        
                        model.addRow(new Object[]{
                            courseInfo,
                            e.getSectionNumber(),
                            instructor,
                            e.getStatus(),
                            finalGrade,
                            action
                        });
                    }
                    
                    statusLabel.setText("Total: " + enrollments.size() + " courses (" + activeCount + " active)");
                } catch (Exception e) {
                    logger.error("Failed to load enrollments", e);
                    statusLabel.setText("Error loading courses: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void dropCourse(int row) {
        if (currentStudent == null) return;

        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to drop this course?\nThis action cannot be undone.",
            "Confirm Drop", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (result != JOptionPane.YES_OPTION) return;

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    List<Enrollment> enrollments = enrollmentService.listByStudent(currentStudent.getStudentId());
                    if (row < enrollments.size()) {
                        Enrollment enrollment = enrollments.get(row);
                        return enrollmentService.drop(currentStudent.getStudentId(), enrollment.getSectionId());
                    }
                    return "Enrollment not found";
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if ("DROPPED".equals(result)) {
                        JOptionPane.showMessageDialog(MyCoursesPanel.this,
                            "Course dropped successfully", "Drop Success", JOptionPane.INFORMATION_MESSAGE);
                        loadEnrollments(); // Refresh
                    } else {
                        JOptionPane.showMessageDialog(MyCoursesPanel.this,
                            "Drop failed: " + result, "Drop Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MyCoursesPanel.this,
                        "Drop error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Button renderer
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setEnabled("Drop".equals(value));
            if ("Drop".equals(value)) {
                setForeground(Color.RED);
            }
            return this;
        }
    }

    // Button editor
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
            button.setEnabled("Drop".equals(label));
            if ("Drop".equals(label)) {
                button.setForeground(Color.RED);
            }
            isPushed = true;
            currentRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && "Drop".equals(label)) {
                dropCourse(currentRow);
            }
            isPushed = false;
            return label;
        }
    }
}