package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel for instructors to enter and manage student grades.
 */
public class GradeEntryPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(GradeEntryPanel.class);

    private final SectionService sectionService = new SectionService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final GradeService gradeService = new GradeService();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JComboBox<SectionItem> sectionCombo;
    private JTable gradesTable;
    private DefaultTableModel gradesModel;
    private JLabel statusLabel;
    private Instructor currentInstructor;
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    // Dynamic column management
    private List<String> gradeComponents = new ArrayList<>();
    private static final String[] FIXED_COLUMNS = {"Student ID", "Student Name"};
    private static final String[] CALCULATED_COLUMNS = {"Overall", "Letter Grade"};

    public GradeEntryPanel() {
        initComponents();
        loadCurrentInstructor();
    }

    private void loadCurrentInstructor() {
        // Disable sections combo until instructor is loaded
        if (sectionCombo != null) {
            sectionCombo.setEnabled(false);
        }
        
        SwingWorker<Instructor, Void> worker = new SwingWorker<>() {
            @Override
            protected Instructor doInBackground() throws Exception {
                try {
                    // Safely get current user and handle null case
                    var currentUser = SessionManager.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        logger.error("No current user session found. User must be logged in to access instructor features.");
                        throw new IllegalStateException("User session not found. Please log in again.");
                    }
                    
                    Long userId = currentUser.getUserId();
                    if (userId == null) {
                        logger.warn("User ID is null, cannot load instructor data");
                        return null;
                    }
                    
                    Instructor instructor = instructorDAO.findByUserId(userId);
                    if (instructor == null) {
                        logger.warn("No instructor found for user ID: {}", userId);
                    }
                    return instructor;
                } catch (SQLException e) {
                    logger.error("Error loading current instructor", e);
                    throw e;
                }
            }
            
            @Override
            protected void done() {
                try {
                    currentInstructor = get();
                    
                    if (currentInstructor != null) {
                        // Successfully loaded instructor - enable UI and load sections
                        if (sectionCombo != null) {
                            sectionCombo.setEnabled(true);
                        }
                        statusLabel.setText("Welcome, " + currentInstructor.getFirstName() + " " + currentInstructor.getLastName());
                        loadSections();
                    } else {
                        // No instructor found - show error and disable UI
                        statusLabel.setText("Error: Instructor data not found");
                        if (sectionCombo != null) {
                            sectionCombo.setEnabled(false);
                        }
                        JOptionPane.showMessageDialog(GradeEntryPanel.this, 
                            "Unable to load instructor information. Please contact system administrator.", 
                            "Access Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.error("Failed to load instructor data", e);
                    if (sectionCombo != null) {
                        sectionCombo.setEnabled(false);
                    }
                    
                    // Handle different types of errors with appropriate messages
                    if (e instanceof IllegalStateException) {
                        // Session/authentication error
                        statusLabel.setText("Authentication error - please log in");
                        JOptionPane.showMessageDialog(GradeEntryPanel.this, 
                            e.getMessage(), 
                            "Authentication Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        // Database or other error
                        statusLabel.setText("Error loading instructor data");
                        JOptionPane.showMessageDialog(GradeEntryPanel.this, 
                            "Database error while loading instructor information: " + e.getMessage(), 
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
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

        // Grades table - will be initialized dynamically
        initializeEmptyTable();
        gradesTable = new JTable(gradesModel);
        gradesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add TableModelListener to persist grade edits
        gradesModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    
                    // Only handle edits to grade component columns (dynamic check)
                    if (isGradeComponentColumn(column)) {
                        Object newValue = gradesModel.getValueAt(row, column);
                        GradeEntryPanel.this.saveGradeEdit(row, column, newValue);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(gradesTable);
        add(scrollPane, "grow, wrap");

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[grow][][][]", ""));
        
        statusLabel = new JLabel("Loading instructor information...");
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
    
    /**
     * Initialize empty table with basic structure
     */
    private void initializeEmptyTable() {
        gradeComponents = new ArrayList<>();
        String[] initialColumns = {"Student ID", "Student Name", "Overall", "Letter Grade"};
        gradesModel = new DefaultTableModel(initialColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing of grade component columns (between fixed and calculated columns)
                return isGradeComponentColumn(column);
            }
        };
    }
    
    /**
     * Check if a column is a grade component column (editable)
     */
    private boolean isGradeComponentColumn(int column) {
        return column >= FIXED_COLUMNS.length && column < FIXED_COLUMNS.length + gradeComponents.size();
    }
    
    /**
     * Rebuild table structure based on current grade components
     */
    private void rebuildTableStructure(List<String> components) {
        // Store current data
        Object[][] currentData = new Object[gradesModel.getRowCount()][gradesModel.getColumnCount()];
        for (int i = 0; i < gradesModel.getRowCount(); i++) {
            for (int j = 0; j < gradesModel.getColumnCount(); j++) {
                currentData[i][j] = gradesModel.getValueAt(i, j);
            }
        }
        
        // Update components list
        gradeComponents = new ArrayList<>(components);
        
        // Build new column structure
        List<String> newColumns = new ArrayList<>();
        for (String col : FIXED_COLUMNS) {
            newColumns.add(col);
        }
        newColumns.addAll(gradeComponents);
        for (String col : CALCULATED_COLUMNS) {
            newColumns.add(col);
        }
        
        // Create new table model
        String[] columnArray = newColumns.toArray(new String[0]);
        DefaultTableModel newModel = new DefaultTableModel(columnArray, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isGradeComponentColumn(column);
            }
        };
        
        // Add TableModelListener to new model
        newModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    
                    // Only handle edits to grade component columns
                    if (isGradeComponentColumn(column)) {
                        Object newValue = newModel.getValueAt(row, column);
                        GradeEntryPanel.this.saveGradeEdit(row, column, newValue);
                    }
                }
            }
        });
        
        // Update table model
        gradesModel = newModel;
        gradesTable.setModel(gradesModel);
        
        // Configure column widths
        configureColumnWidths();
    }
    
    /**
     * Configure column widths based on content type
     */
    private void configureColumnWidths() {
        if (gradesTable.getColumnModel().getColumnCount() > 0) {
            // Fixed columns
            gradesTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Student ID
            gradesTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Student Name
            
            // Grade component columns
            for (int i = 0; i < gradeComponents.size(); i++) {
                int columnIndex = FIXED_COLUMNS.length + i;
                if (columnIndex < gradesTable.getColumnModel().getColumnCount()) {
                    gradesTable.getColumnModel().getColumn(columnIndex).setPreferredWidth(80);
                }
            }
            
            // Calculated columns
            int overallIndex = FIXED_COLUMNS.length + gradeComponents.size();
            int letterIndex = overallIndex + 1;
            if (overallIndex < gradesTable.getColumnModel().getColumnCount()) {
                gradesTable.getColumnModel().getColumn(overallIndex).setPreferredWidth(80);  // Overall
            }
            if (letterIndex < gradesTable.getColumnModel().getColumnCount()) {
                gradesTable.getColumnModel().getColumn(letterIndex).setPreferredWidth(80);   // Letter Grade
            }
        }
    }

    private void loadSections() {
        if (currentInstructor == null) {
            statusLabel.setText("Loading instructor information...");
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
                    
                    // Update status to show section count, keeping instructor welcome if it was set
                    String currentStatus = statusLabel.getText();
                    if (currentStatus.startsWith("Welcome,")) {
                        statusLabel.setText(currentStatus + " | " + (sections.isEmpty() ? "No sections assigned" : sections.size() + " section(s) available"));
                    } else {
                        statusLabel.setText(sections.isEmpty() ? "No sections assigned" : "Found " + sections.size() + " section(s)");
                    }
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
                    return enrollmentService.listBySection(section.getSectionId());
                } catch (Exception e) {
                    logger.error("Error loading enrollments for grading", e);
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Enrollment> enrollments = get();
                    
                    // Get actual grade components for this section
                    List<String> sectionComponents = getGradeComponentsForSection(section.getSectionId());
                    
                    // Rebuild table structure if components have changed
                    if (!sectionComponents.equals(gradeComponents)) {
                        rebuildTableStructure(sectionComponents);
                    }
                    
                    // Clear existing data
                    gradesModel.setRowCount(0);
                    
                    // Batch-fetch student names to avoid per-row DB calls
                    Map<Long, String> studentNames = getStudentNamesMap(enrollments);
                    
                    for (Enrollment enrollment : enrollments) {
                        // Load existing grades for this enrollment
                        List<Grade> grades = gradeService.listComponents(enrollment.getEnrollmentId());
                        
                        // Create row data based on current table structure
                        Object[] rowData = new Object[gradesModel.getColumnCount()];
                        
                        // Fixed columns
                        rowData[0] = enrollment.getStudentId();
                        
                        // Get student name from pre-fetched map, with fallback to student ID
                        String studentName = studentNames.get(enrollment.getStudentId());
                        if (studentName == null || studentName.trim().isEmpty()) {
                            studentName = "Student " + enrollment.getStudentId(); // Fallback to ID
                            logger.warn("No name found for student ID: {}", enrollment.getStudentId());
                        }
                        rowData[1] = studentName;
                        
                        // Grade component columns
                        double totalScore = 0.0;
                        double totalWeight = 0.0;
                        
                        for (int i = 0; i < gradeComponents.size(); i++) {
                            String component = gradeComponents.get(i);
                            double score = getGradeComponent(grades, component);
                            int columnIndex = FIXED_COLUMNS.length + i;
                            
                            if (score > 0) {
                                rowData[columnIndex] = df.format(score);
                                
                                // Calculate weighted contribution using actual Grade weight
                                Grade gradeObj = getGradeByComponent(grades, component);
                                if (gradeObj != null && gradeObj.getWeight() != null && gradeObj.getWeight() > 0) {
                                    double weight = gradeObj.getWeight() / 100.0; // Convert percentage to decimal
                                    totalScore += score * weight;
                                    totalWeight += weight;
                                }
                                // If weight is null, missing, or zero, skip contributing to totals (avoid division by zero)
                            } else {
                                rowData[columnIndex] = "";
                            }
                        }
                        
                        // Calculated columns
                        double overall = totalWeight > 0 ? totalScore : 0.0;
                        String letterGrade = convertToLetterGrade(overall);
                        
                        int overallIndex = FIXED_COLUMNS.length + gradeComponents.size();
                        int letterIndex = overallIndex + 1;
                        
                        if (overallIndex < rowData.length) {
                            rowData[overallIndex] = overall > 0 ? df.format(overall) : "";
                        }
                        if (letterIndex < rowData.length) {
                            rowData[letterIndex] = letterGrade;
                        }
                        
                        gradesModel.addRow(rowData);
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
    
    /**
     * Get grade components for a specific section
     */
    private List<String> getGradeComponentsForSection(Long sectionId) {
        try {
            List<Enrollment> enrollments = enrollmentService.listBySection(sectionId);
            Set<String> componentsSet = new HashSet<>();
            
            for (Enrollment enrollment : enrollments) {
                List<Grade> grades = gradeService.listComponents(enrollment.getEnrollmentId());
                grades.stream()
                    .map(Grade::getComponent)
                    .forEach(componentsSet::add);
            }
            
            List<String> components = new ArrayList<>(componentsSet);
            components.sort(String::compareTo);
            
            // If no components found, return default set
            if (components.isEmpty()) {
                components = Arrays.asList("Assignment", "Quiz", "Midterm", "Final");
            }
            
            return components;
        } catch (Exception e) {
            logger.error("Error getting grade components for section", e);
            // Return default components if unable to fetch
            return Arrays.asList("Assignment", "Quiz", "Midterm", "Final");
        }
    }
    
    /**
     * Batch-fetch student names to avoid per-row database calls
     */
    private Map<Long, String> getStudentNamesMap(List<Enrollment> enrollments) {
        Map<Long, String> studentNames = new HashMap<>();
        
        if (enrollments == null || enrollments.isEmpty()) {
            return studentNames;
        }
        
        try {
            // Extract unique student IDs
            List<Long> studentIds = enrollments.stream()
                .map(Enrollment::getStudentId)
                .distinct()
                .collect(Collectors.toList());
            
            // Batch fetch students
            List<Student> students = studentDAO.findByIds(studentIds);
            
            // Build name map
            for (Student student : students) {
                if (student != null && student.getStudentId() != null) {
                    String fullName = buildStudentDisplayName(student);
                    studentNames.put(student.getStudentId(), fullName);
                }
            }
            
            logger.debug("Fetched {} student names for {} enrollments", studentNames.size(), enrollments.size());
            
        } catch (Exception e) {
            logger.error("Error batch-fetching student names", e);
            // Return empty map - fallback to student IDs will be used
        }
        
        return studentNames;
    }
    
    /**
     * Build a display name for a student
     */
    private String buildStudentDisplayName(Student student) {
        if (student == null) {
            return null;
        }
        
        StringBuilder name = new StringBuilder();
        
        // Add first name if available
        if (student.getFirstName() != null && !student.getFirstName().trim().isEmpty()) {
            name.append(student.getFirstName().trim());
        }
        
        // Add last name if available
        if (student.getLastName() != null && !student.getLastName().trim().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(student.getLastName().trim());
        }
        
        // Add roll number in parentheses if available and name is not empty
        if (name.length() > 0 && student.getRollNo() != null && !student.getRollNo().trim().isEmpty()) {
            name.append(" (").append(student.getRollNo().trim()).append(")");
        }
        
        // If no name components were found, use roll number or student ID
        if (name.length() == 0) {
            if (student.getRollNo() != null && !student.getRollNo().trim().isEmpty()) {
                name.append(student.getRollNo().trim());
            } else if (student.getStudentId() != null) {
                name.append("Student ").append(student.getStudentId());
            }
        }
        
        return name.toString();
    }

    private double getGradeComponent(List<Grade> grades, String component) {
        return grades.stream()
            .filter(g -> component.equals(g.getComponent()) && g.getScore() != null && g.getMaxScore() != null)
            .filter(g -> g.getMaxScore() > 0) // Guard against division by zero
            .mapToDouble(g -> {
                if (g.getMaxScore() <= 0) {
                    logger.warn("Invalid maxScore {} for grade component {}, using 0.0", g.getMaxScore(), component);
                    return 0.0;
                }
                return (g.getScore() / g.getMaxScore()) * 100;
            })
            .findFirst()
            .orElse(0.0);
    }

    /**
     * Get the Grade object for a specific component from a list of grades
     */
    private Grade getGradeByComponent(List<Grade> grades, String component) {
        return grades.stream()
            .filter(g -> component.equals(g.getComponent()))
            .findFirst()
            .orElse(null);
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
                        List<Enrollment> enrollments = enrollmentService.listBySection(selectedItem.section.getSectionId());
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
                List<Enrollment> enrollments = enrollmentService.listBySection(selectedItem.section.getSectionId());
                int calculatedCount = 0;
                
                for (Enrollment enrollment : enrollments) {
                    List<Grade> grades = gradeService.listComponents(enrollment.getEnrollmentId());
                    
                    // Check if all components have grades
                    boolean allGraded = grades.stream().allMatch(g -> g.getScore() != null);
                    
                    if (allGraded && !grades.isEmpty()) {
                        // Validate maxScore values before calculation
                        boolean hasValidMaxScores = grades.stream().allMatch(g -> g.getMaxScore() != null && g.getMaxScore() > 0);
                        
                        if (!hasValidMaxScores) {
                            logger.warn("Skipping final grade calculation for enrollment {} due to invalid maxScore values", 
                                enrollment.getEnrollmentId());
                            continue;
                        }
                        
                        // Calculate weighted final grade
                        double totalWeight = grades.stream().mapToDouble(Grade::getWeight).sum();
                        
                        if (Math.abs(totalWeight - 100.0) < 0.01) { // Weights must sum to 100%
                            double finalGrade = grades.stream()
                                .mapToDouble(g -> {
                                    if (g.getMaxScore() <= 0) {
                                        logger.warn("Invalid maxScore {} for grade component {}, using 0.0", 
                                            g.getMaxScore(), g.getComponent());
                                        return 0.0;
                                    }
                                    return (g.getScore() / g.getMaxScore()) * g.getWeight();
                                })
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

    /**
     * Saves grade edits to the database when table cells are modified.
     * @param row The row index in the table model
     * @param column The column index (2=Assignment, 3=Quiz, 4=Midterm, 5=Final)
     * @param newValue The new grade value entered by the user
     */
    private void saveGradeEdit(int row, int column, Object newValue) {
        try {
            // Get identifiers from the table model
            Long studentId = (Long) gradesModel.getValueAt(row, 0); // Student ID column
            String studentName = (String) gradesModel.getValueAt(row, 1); // Student Name for logging
            
            // Map column to grade component type using dynamic structure
            if (!isGradeComponentColumn(column)) {
                logger.warn("Column {} is not a grade component column", column);
                return;
            }
            
            int componentIndex = column - FIXED_COLUMNS.length;
            if (componentIndex < 0 || componentIndex >= gradeComponents.size()) {
                logger.warn("Invalid component index {} for column {}", componentIndex, column);
                return;
            }
            
            String componentType = gradeComponents.get(componentIndex);
            
            // Validate and convert the new value
            final Double gradeValue;
            if (newValue != null && !newValue.toString().trim().isEmpty()) {
                try {
                    double tempValue = Double.parseDouble(newValue.toString().trim());
                    
                    // Validate grade range (0-100)
                    if (tempValue < 0 || tempValue > 100) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, 
                                "Grade must be between 0 and 100.", 
                                "Invalid Grade", JOptionPane.ERROR_MESSAGE);
                            // Reset to previous value or empty
                            gradesModel.setValueAt("", row, column);
                        });
                        return;
                    }
                    gradeValue = tempValue;
                } catch (NumberFormatException e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, 
                            "Please enter a valid number for the grade.", 
                            "Invalid Grade Format", JOptionPane.ERROR_MESSAGE);
                        // Reset to previous value or empty
                        gradesModel.setValueAt("", row, column);
                    });
                    return;
                }
            } else {
                gradeValue = null;
            }
            
            // Get current section
            SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
            if (selectedItem == null || selectedItem.section == null) {
                logger.warn("No section selected for grade edit");
                return;
            }
            
            // Perform database update in background thread
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        // Find the enrollment for this student and section
                        List<Enrollment> enrollments = enrollmentService.listBySection(selectedItem.section.getSectionId());
                        Enrollment targetEnrollment = null;
                        for (Enrollment enrollment : enrollments) {
                            if (enrollment.getStudentId().equals(studentId)) {
                                targetEnrollment = enrollment;
                                break;
                            }
                        }
                        
                        if (targetEnrollment == null) {
                            logger.error("No enrollment found for student {} in section {}", studentId, selectedItem.section.getSectionId());
                            return false;
                        }
                        
                        // Update or create the grade component
                        if (gradeValue != null) {
                            // Check if grade component already exists
                            List<Grade> existingGrades = gradeService.listComponents(targetEnrollment.getEnrollmentId());
                            Grade existingGrade = null;
                            for (Grade grade : existingGrades) {
                                if (componentType.equals(grade.getComponent())) {
                                    existingGrade = grade;
                                    break;
                                }
                            }
                            
                            if (existingGrade != null) {
                                // Update existing grade
                                String result = gradeService.updateScore(existingGrade.getGradeId(), gradeValue);
                                return "UPDATED".equals(result);
                            } else {
                                // Create new grade component with default weight
                                double defaultWeight = getDefaultWeight(componentType);
                                String result = gradeService.addComponent(targetEnrollment.getEnrollmentId(), 
                                    componentType, gradeValue, 100.0, defaultWeight);
                                return "ADDED".equals(result);
                            }
                        } else {
                            // Set grade to null (empty) if value is empty
                            List<Grade> existingGrades = gradeService.listComponents(targetEnrollment.getEnrollmentId());
                            for (Grade grade : existingGrades) {
                                if (componentType.equals(grade.getComponent())) {
                                    String result = gradeService.updateScore(grade.getGradeId(), null);
                                    return "UPDATED".equals(result);
                                }
                            }
                            return true; // Nothing to update
                        }
                    } catch (Exception e) {
                        logger.error("Error updating grade for student {} component {}", studentId, componentType, e);
                        return false;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            logger.info("Successfully updated {} grade for student {} ({})", 
                                componentType, studentId, studentName);
                            
                            // Recalculate overall and letter grades for this row
                            updateRowCalculations(row);
                            
                            // Show brief success indicator
                            statusLabel.setText("Grade saved for " + studentName);
                            
                            // Clear status after 3 seconds
                            Timer timer = new Timer(3000, e -> statusLabel.setText("Ready"));
                            timer.setRepeats(false);
                            timer.start();
                        } else {
                            JOptionPane.showMessageDialog(GradeEntryPanel.this, 
                                "Failed to save grade. Please try again.", 
                                "Save Error", JOptionPane.ERROR_MESSAGE);
                            
                            // Refresh the entire table to restore consistent state
                            loadGrades();
                        }
                    } catch (Exception e) {
                        logger.error("Error in grade save completion", e);
                        JOptionPane.showMessageDialog(GradeEntryPanel.this, 
                            "Error saving grade: " + e.getMessage(), 
                            "Save Error", JOptionPane.ERROR_MESSAGE);
                        
                        // Refresh the entire table to restore consistent state
                        loadGrades();
                    }
                }
            };
            worker.execute();
            
        } catch (Exception e) {
            logger.error("Error processing grade edit", e);
            JOptionPane.showMessageDialog(this, 
                "Error processing grade edit: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Get default weight for grade component type.
     */
    private double getDefaultWeight(String componentType) {
        switch (componentType) {
            case "Assignment": return 0.20; // 20%
            case "Quiz": return 0.20;       // 20%
            case "Midterm": return 0.30;    // 30%
            case "Final": return 0.30;      // 30%
            default: return 0.25;           // 25% default
        }
    }
    
    /**
     * Recalculate overall and letter grades for a specific row.
     */
    private void updateRowCalculations(int row) {
        try {
            // Get grade values from dynamic grade component columns
            double totalPoints = 0.0;
            double totalWeight = 0.0;
            
            for (int i = 0; i < gradeComponents.size(); i++) {
                int col = FIXED_COLUMNS.length + i;
                Object value = gradesModel.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    try {
                        double grade = Double.parseDouble(value.toString());
                        String componentType = gradeComponents.get(i);
                        double weight = getDefaultWeight(componentType);
                        
                        totalPoints += grade * weight;
                        totalWeight += weight;
                    } catch (NumberFormatException e) {
                        // Skip invalid values
                    }
                }
            }
            
            // Calculate overall grade
            if (totalWeight > 0) {
                double overallGrade = totalPoints / totalWeight;
                gradesModel.setValueAt(df.format(overallGrade), row, 6); // Overall column
                
                // Calculate letter grade
                String letterGrade = convertToLetterGrade(overallGrade);
                gradesModel.setValueAt(letterGrade, row, 7); // Letter Grade column
            } else {
                gradesModel.setValueAt("", row, 6);
                gradesModel.setValueAt("", row, 7);
            }
        } catch (Exception e) {
            logger.error("Error updating row calculations", e);
        }
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