package edu.univ.erp.ui.student;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.GradeService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.text.DecimalFormat;

/**
 * Panel to display student's grades across all enrolled courses.
 */
public class MyGradesPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MyGradesPanel.class);

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final GradeService gradeService = new GradeService();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JTable coursesTable;
    private DefaultTableModel coursesModel;
    private JTable gradesTable;
    private DefaultTableModel gradesModel;
    private JLabel gpaLabel;
    private JLabel statusLabel;
    private Student currentStudent;
    
    private final DecimalFormat df = new DecimalFormat("#.##");

    public MyGradesPanel() {
        loadCurrentStudent();
        initComponents();
        loadCourses();
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
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][][grow][]"));

        // Header
        add(new JLabel("<html><h2>My Grades</h2></html>"), "wrap");

        // GPA display
        JPanel gpaPanel = new JPanel(new MigLayout("insets 0", "[][]", ""));
        gpaPanel.add(new JLabel("Current GPA: "));
        gpaLabel = new JLabel("Calculating...");
        gpaLabel.setFont(gpaLabel.getFont().deriveFont(Font.BOLD, 16f));
        gpaLabel.setForeground(new Color(0, 100, 0));
        gpaPanel.add(gpaLabel);
        add(gpaPanel, "wrap");

        // Split pane for courses and grades
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.4);

        // Left side - Courses table
        JPanel coursesPanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow]"));
        coursesPanel.add(new JLabel("Enrolled Courses:"), "wrap");
        
        String[] courseColumns = {"Course", "Section", "Credits", "Final Grade", "Status"};
        coursesModel = new DefaultTableModel(courseColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        coursesTable = new JTable(coursesModel);
        coursesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        coursesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadGradesForSelectedCourse();
            }
        });
        
        // Custom renderer for final grades
        coursesTable.getColumnModel().getColumn(3).setCellRenderer(new GradeCellRenderer());
        
        coursesPanel.add(new JScrollPane(coursesTable), "grow");
        splitPane.setLeftComponent(coursesPanel);

        // Right side - Grade components table
        JPanel gradesPanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow]"));
        gradesPanel.add(new JLabel("Grade Components: (Select a course to view)"), "wrap");
        
        String[] gradeColumns = {"Component", "Score", "Max Score", "Weight %", "Weighted Score"};
        gradesModel = new DefaultTableModel(gradeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        gradesTable = new JTable(gradesModel);
        gradesTable.getColumnModel().getColumn(1).setCellRenderer(new GradeCellRenderer());
        gradesTable.getColumnModel().getColumn(4).setCellRenderer(new GradeCellRenderer());
        
        gradesPanel.add(new JScrollPane(gradesTable), "grow");
        splitPane.setRightComponent(gradesPanel);

        add(splitPane, "grow, wrap");

        // Status bar
        JPanel bottomPanel = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
        statusLabel = new JLabel("Loading...");
        bottomPanel.add(statusLabel, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadCourses());
        bottomPanel.add(refreshBtn);
        
        add(bottomPanel, "growx");
    }

    private void loadCourses() {
        if (currentStudent == null) {
            statusLabel.setText("Student data not available");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    List<Enrollment> enrollments = enrollmentService.listByStudent(currentStudent.getStudentId());
                    
                    SwingUtilities.invokeLater(() -> {
                        coursesModel.setRowCount(0);
                        
                        double totalGradePoints = 0.0;
                        int totalCredits = 0;
                        int gradedCourses = 0;
                        
                        for (Enrollment enrollment : enrollments) {
                            String finalGradeStr = "In Progress";
                            String finalGrade = enrollment.getFinalGrade();
                            
                            if (finalGrade != null && !finalGrade.isEmpty()) {
                                finalGradeStr = finalGrade;
                                // Convert to GPA points (assuming 4.0 scale)
                                double gradePoints = MyGradesPanel.this.convertLetterToGPA(finalGrade);
                                totalGradePoints += gradePoints * 3; // Assuming 3 credits per course
                                totalCredits += 3;
                                gradedCourses++;
                            }
                            
                            coursesModel.addRow(new Object[]{
                                enrollment.getCourseCode(),
                                enrollment.getSectionNumber(),
                                "3", // Default credits
                                finalGradeStr,
                                enrollment.getStatus()
                            });
                        }
                        
                        // Update GPA
                        if (totalCredits > 0) {
                            double gpa = totalGradePoints / totalCredits;
                            gpaLabel.setText(df.format(gpa) + " / 4.0");
                        } else {
                            gpaLabel.setText("N/A");
                        }
                        
                        statusLabel.setText("Showing " + enrollments.size() + " enrollments (" + 
                                          gradedCourses + " with final grades)");
                    });
                    
                } catch (Exception e) {
                    logger.error("Error loading courses", e);
                    SwingUtilities.invokeLater(() -> 
                        statusLabel.setText("Error loading courses: " + e.getMessage()));
                }
                return null;
            }
        };
        worker.execute();
    }

    private void loadGradesForSelectedCourse() {
        int selectedRow = coursesTable.getSelectedRow();
        if (selectedRow == -1) {
            gradesModel.setRowCount(0);
            return;
        }

        final String courseCode = (String) coursesModel.getValueAt(selectedRow, 0);
        final String sectionNumber = (String) coursesModel.getValueAt(selectedRow, 1);
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Find the enrollment
                    List<Enrollment> enrollments = enrollmentService.listByStudent(currentStudent.getStudentId());
                    Enrollment targetEnrollment = null;
                    
                    for (Enrollment enrollment : enrollments) {
                        if (courseCode.equals(enrollment.getCourseCode()) && 
                            sectionNumber.equals(enrollment.getSectionNumber())) {
                            targetEnrollment = enrollment;
                            break;
                        }
                    }
                    
                    if (targetEnrollment != null) {
                        List<Grade> grades = gradeService.listComponents(targetEnrollment.getEnrollmentId());
                        
                        SwingUtilities.invokeLater(() -> {
                            gradesModel.setRowCount(0);
                            
                            double totalWeightedScore = 0.0;
                            double totalWeight = 0.0;
                            
                            for (Grade grade : grades) {
                                double weightedScore = 0.0;
                                String scoreStr = "Not Graded";
                                
                                if (grade.getScore() != null) {
                                    scoreStr = df.format(grade.getScore());
                                    double percentage = (grade.getScore() / grade.getMaxScore()) * 100;
                                    weightedScore = percentage * grade.getWeight() / 100;
                                    totalWeightedScore += weightedScore;
                                }
                                
                                totalWeight += grade.getWeight();
                                
                                gradesModel.addRow(new Object[]{
                                    grade.getComponent(),
                                    scoreStr,
                                    df.format(grade.getMaxScore()),
                                    df.format(grade.getWeight()) + "%",
                                    grade.getScore() != null ? df.format(weightedScore) + "%" : "N/A"
                                });
                            }
                            
                            // Add summary row if there are grades
                            if (!grades.isEmpty()) {
                                gradesModel.addRow(new Object[]{
                                    "TOTAL",
                                    "",
                                    "",
                                    df.format(totalWeight) + "%",
                                    df.format(totalWeightedScore) + "%"
                                });
                            }
                        });
                    }
                    
                } catch (Exception e) {
                    logger.error("Error loading grades", e);
                }
                return null;
            }
        };
        worker.execute();
    }

    private double convertLetterToGPA(String letterGrade) {
        if (letterGrade == null) return 0.0;
        switch (letterGrade.toUpperCase()) {
            case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.3;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.3;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.3;
            case "D": return 1.0;
            default: return 0.0;
        }
    }

    // Custom cell renderer for grades
    private static class GradeCellRenderer extends JLabel implements TableCellRenderer {
        public GradeCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            setText(value != null ? value.toString() : "");
            
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
                
                // Color code grades
                String text = getText();
                if (text.contains("%") && !text.equals("N/A")) {
                    try {
                        double value_num = Double.parseDouble(text.replace("%", ""));
                        if (value_num >= 90) {
                            setForeground(new Color(0, 120, 0)); // Green for A
                        } else if (value_num >= 80) {
                            setForeground(new Color(0, 0, 200)); // Blue for B
                        } else if (value_num >= 70) {
                            setForeground(new Color(200, 100, 0)); // Orange for C
                        } else if (value_num >= 60) {
                            setForeground(new Color(200, 0, 0)); // Red for D/F
                        }
                    } catch (NumberFormatException ignored) {
                        // Keep default color
                    }
                }
            }
            
            return this;
        }
    }
}