package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Panel for instructors to enter and manage student grades.
 */
public class GradeEntryPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(GradeEntryPanel.class);

    private final SectionService sectionService = new SectionService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final GradeService gradeService = new GradeService();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    
    private JComboBox<SectionItem> sectionCombo;
    private JTable gradesTable;
    private DefaultTableModel gradesModel;
    private JLabel statusLabel;
    private Instructor currentInstructor;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public GradeEntryPanel() {
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
        add(new JLabel("<html><h2>Grade Entry</h2></html>"), "wrap");

        // Section selection
        JPanel sectionPanel = new JPanel(new MigLayout("insets 0", "[]10[grow][]", ""));
        sectionPanel.add(new JLabel("Select Section:"));
        
        sectionCombo = new JComboBox<>();
        sectionCombo.addActionListener(e -> loadGrades());
        sectionPanel.add(sectionCombo, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadSections());
        sectionPanel.add(refreshBtn);
        
        add(sectionPanel, "growx, wrap");

        // Grades table
        String[] columns = {"Student ID", "Student Name", "Assignment", "Quiz", "Midterm", "Final", "Overall", "Letter Grade"};
        gradesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing of grade components but not calculated fields
                return column >= 2 && column <= 5;
            }
        };
        gradesTable = new JTable(gradesModel);
        gradesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(gradesTable);
        add(scrollPane, "grow, wrap");

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[grow][][][]", ""));
        
        statusLabel = new JLabel("Select a section to manage grades");
        controlPanel.add(statusLabel, "growx");
        
        JButton addComponentBtn = new JButton("Add Component");
        addComponentBtn.addActionListener(e -> addGradeComponent());
        controlPanel.add(addComponentBtn);
        
        JButton calculateBtn = new JButton("Calculate Finals");
        calculateBtn.addActionListener(e -> calculateFinalGrades());
        controlPanel.add(calculateBtn);
        
        JButton exportBtn = new JButton("Export Grades");
        exportBtn.addActionListener(e -> exportGrades());
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
                    sectionCombo.removeAllItems();
                    sectionCombo.addItem(new SectionItem(null, "-- Select Section --"));
                    
                    for (Section section : sections) {
                        String display = section.getCourseCode() + " - " + section.getSectionNumber();
                        sectionCombo.addItem(new SectionItem(section, display));
                    }
                    
                    statusLabel.setText(sections.isEmpty() ? "No sections assigned" : "Found " + sections.size() + " section(s)");
                } catch (Exception e) {
                    logger.error("Failed to load sections", e);
                    statusLabel.setText("Error loading sections: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadGrades() {
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.section == null) {
            gradesModel.setRowCount(0);
            statusLabel.setText("Select a section to manage grades");
            return;
        }

        Section section = selectedItem.section;
        
        SwingWorker<List<Enrollment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Enrollment> doInBackground() {
                try {
                    return enrollmentService.listByStudent(section.getSectionId()); // Will need to add listBySection method
                } catch (Exception e) {
                    logger.error("Error loading enrollments for grading", e);
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Enrollment> enrollments = get();
                    gradesModel.setRowCount(0);
                    
                    for (Enrollment enrollment : enrollments) {
                        // Load existing grades for this enrollment
                        List<Grade> grades = gradeService.listComponents(enrollment.getEnrollmentId());
                        
                        // Calculate grade components
                        double assignment = getGradeComponent(grades, "Assignment");
                        double quiz = getGradeComponent(grades, "Quiz");
                        double midterm = getGradeComponent(grades, "Midterm");
                        double finalExam = getGradeComponent(grades, "Final");
                        
                        // Calculate overall grade
                        double overall = (assignment * 0.2) + (quiz * 0.2) + (midterm * 0.3) + (finalExam * 0.3);
                        String letterGrade = convertToLetterGrade(overall);
                        
                        gradesModel.addRow(new Object[]{
                            enrollment.getStudentId(),
                            "Student " + enrollment.getStudentId(), // Will enhance with actual name
                            assignment > 0 ? df.format(assignment) : "",
                            quiz > 0 ? df.format(quiz) : "",
                            midterm > 0 ? df.format(midterm) : "",
                            finalExam > 0 ? df.format(finalExam) : "",
                            overall > 0 ? df.format(overall) : "",
                            letterGrade
                        });
                    }
                    
                    String courseInfo = section.getCourseCode() + " - " + section.getSectionNumber();
                    statusLabel.setText("Showing grades for " + enrollments.size() + " student(s) in " + courseInfo);
                } catch (Exception e) {
                    logger.error("Failed to load grades", e);
                    statusLabel.setText("Error loading grades: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private double getGradeComponent(List<Grade> grades, String component) {
        return grades.stream()
            .filter(g -> component.equals(g.getComponent()) && g.getScore() != null && g.getMaxScore() != null)
            .mapToDouble(g -> (g.getScore() / g.getMaxScore()) * 100)
            .findFirst()
            .orElse(0.0);
    }

    private String convertToLetterGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    private void addGradeComponent() {
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.section == null) {
            JOptionPane.showMessageDialog(this, "Please select a section first.", "No Section", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create dialog for adding grade component
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Add Grade Component", true);
        dialog.setLayout(new MigLayout("insets 20", "[][grow]", "[][][][][][]"));
        
        JTextField componentField = new JTextField();
        JTextField maxScoreField = new JTextField();
        JTextField weightField = new JTextField();
        
        dialog.add(new JLabel("Component Name:"));
        dialog.add(componentField, "growx, wrap");
        
        dialog.add(new JLabel("Max Score:"));
        dialog.add(maxScoreField, "growx, wrap");
        
        dialog.add(new JLabel("Weight (%):"));
        dialog.add(weightField, "growx, wrap");
        
        JButton addBtn = new JButton("Add Component");
        JButton cancelBtn = new JButton("Cancel");
        
        addBtn.addActionListener(e -> {
            try {
                String component = componentField.getText().trim();
                double maxScore = Double.parseDouble(maxScoreField.getText().trim());
                double weight = Double.parseDouble(weightField.getText().trim());
                
                if (component.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Component name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (maxScore <= 0 || weight <= 0 || weight > 100) {
                    JOptionPane.showMessageDialog(dialog, "Please enter valid max score and weight (0-100).", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Add component to all enrollments in this section
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        List<Enrollment> enrollments = enrollmentService.listByStudent(selectedItem.section.getSectionId());
                        for (Enrollment enrollment : enrollments) {
                            gradeService.addComponent(enrollment.getEnrollmentId(), component, null, maxScore, weight);
                        }
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get();
                            JOptionPane.showMessageDialog(dialog, "Grade component added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            loadGrades(); // Refresh the grades view
                        } catch (Exception ex) {
                            logger.error("Error adding grade component", ex);
                            JOptionPane.showMessageDialog(dialog, "Error adding component: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers for max score and weight.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
        buttonPanel.add(addBtn, "growx");
        buttonPanel.add(cancelBtn);
        
        dialog.add(buttonPanel, "span 2, growx, wrap");
        
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void calculateFinalGrades() {
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.section == null) {
            JOptionPane.showMessageDialog(this, "Please select a section first.", "No Section", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "This will calculate and store final grades for all students in this section.\n" +
            "Are you sure you want to continue?",
            "Calculate Final Grades", JOptionPane.YES_NO_OPTION);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<Enrollment> enrollments = enrollmentService.listByStudent(selectedItem.section.getSectionId());
                int calculatedCount = 0;
                
                for (Enrollment enrollment : enrollments) {
                    List<Grade> grades = gradeService.listComponents(enrollment.getEnrollmentId());
                    
                    // Check if all components have grades
                    boolean allGraded = grades.stream().allMatch(g -> g.getScore() != null);
                    
                    if (allGraded && !grades.isEmpty()) {
                        // Calculate weighted final grade
                        double totalWeight = grades.stream().mapToDouble(Grade::getWeight).sum();
                        
                        if (Math.abs(totalWeight - 100.0) < 0.01) { // Weights must sum to 100%
                            double finalGrade = grades.stream()
                                .mapToDouble(g -> (g.getScore() / g.getMaxScore()) * g.getWeight())
                                .sum();
                            
                            String letterGrade = convertToLetterGrade(finalGrade);
                            // Update the enrollment with final grade
                            enrollmentService.updateFinalGrade(enrollment.getEnrollmentId(), letterGrade);
                            calculatedCount++;
                        }
                    }
                }
                
                return calculatedCount;
            }
            
            @Override
            protected void done() {
                try {
                    Integer count = get();
                    JOptionPane.showMessageDialog(GradeEntryPanel.this,
                        "Final grades calculated for " + count + " student(s).",
                        "Calculation Complete", JOptionPane.INFORMATION_MESSAGE);
                    loadGrades(); // Refresh the view
                } catch (Exception ex) {
                    logger.error("Error calculating final grades", ex);
                    JOptionPane.showMessageDialog(GradeEntryPanel.this,
                        "Error calculating final grades: " + ex.getMessage(),
                        "Calculation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void exportGrades() {
        if (gradesModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No grades to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(this, 
            "Export grades functionality will be implemented in future version.",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
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