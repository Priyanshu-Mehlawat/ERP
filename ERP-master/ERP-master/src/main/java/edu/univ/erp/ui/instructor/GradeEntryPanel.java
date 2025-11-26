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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                        
                        // Null-safe name composition with fallbacks
                        String firstName = currentInstructor.getFirstName() != null ? 
                            currentInstructor.getFirstName().trim() : "";
                        String lastName = currentInstructor.getLastName() != null ? 
                            currentInstructor.getLastName().trim() : "";
                        
                        String displayName;
                        if (firstName.isEmpty() && lastName.isEmpty()) {
                            displayName = "Unknown Instructor";
                        } else if (firstName.isEmpty()) {
                            displayName = lastName;
                        } else if (lastName.isEmpty()) {
                            displayName = firstName;
                        } else {
                            displayName = firstName + " " + lastName;
                        }
                        
                        statusLabel.setText("Welcome, " + displayName);
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
                        
                        // Add row first with empty calculated columns
                        int overallIndex = FIXED_COLUMNS.length + gradeComponents.size();
                        int letterIndex = overallIndex + 1;
                        
                        if (overallIndex < rowData.length) {
                            rowData[overallIndex] = "";
                        }
                        if (letterIndex < rowData.length) {
                            rowData[letterIndex] = "";
                        }
                        
                        gradesModel.addRow(rowData);
                        
                        // Calculate and set normalized overall grade using dynamic column lookup
                        if (totalWeight > 0) {
                            double overall = totalScore / totalWeight;
                            String letterGrade = convertToLetterGrade(overall);
                            
                            // Use dynamic column lookup and only set when column exists (same as updateRowCalculations)
                            int overallCol = gradesModel.findColumn("Overall");
                            if (overallCol != -1) {
                                int currentRow = gradesModel.getRowCount() - 1;
                                gradesModel.setValueAt(df.format(overall), currentRow, overallCol);
                            }
                            
                            int letterCol = gradesModel.findColumn("Letter Grade");
                            if (letterCol != -1) {
                                int currentRow = gradesModel.getRowCount() - 1;
                                gradesModel.setValueAt(letterGrade, currentRow, letterCol);
                            }
                        }
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
     * Optimized to use a single DB query instead of N+1 calls
     */
    private List<String> getGradeComponentsForSection(Long sectionId) {
        try {
            // Use the new optimized service method that performs a single DB query
            return gradeService.getComponentsForSection(sectionId);
        } catch (SQLException e) {
            logger.error("Database error getting grade components for section {}: {}", sectionId, e.getMessage(), e);
            // Return default components if unable to fetch due to database error
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

        // Create dialog for adding grade component - fix parent window casting
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Add Grade Component", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx, insets 20", "[right]rel[grow,fill]", "[]10[]10[]10[]10[]"));
        
        dialog.add(new JLabel("Component Name:"), "");
        JTextField componentField = new JTextField(20);
        dialog.add(componentField, "wrap");
        
        dialog.add(new JLabel("Max Score:"), "");
        JTextField maxScoreField = new JTextField("100", 10);
        dialog.add(maxScoreField, "wrap");
        
        dialog.add(new JLabel("Weight (%):"), "");
        JTextField weightField = new JTextField("25", 10);
        JPanel weightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        weightPanel.add(weightField);
        weightPanel.add(new JLabel("  (e.g., 25 for 25%)"));
        dialog.add(weightPanel, "wrap");
        
        // Show current total weight
        dialog.add(new JLabel(""), "");
        JLabel currentWeightLabel = new JLabel("<html><i>Current components: " + gradeComponents.size() + "</i></html>");
        currentWeightLabel.setForeground(Color.GRAY);
        dialog.add(currentWeightLabel, "wrap");
        
        // Common component suggestions
        dialog.add(new JLabel("Quick Add:"), "");
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        String[] suggestions = {"Homework", "Project", "Participation", "Lab", "Presentation", "Essay"};
        for (String suggestion : suggestions) {
            JButton btn = new JButton(suggestion);
            btn.setMargin(new Insets(2, 5, 2, 5));
            btn.addActionListener(e -> componentField.setText(suggestion));
            quickPanel.add(btn);
        }
        dialog.add(quickPanel, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addBtn = new JButton("Add Component");
        JButton cancelBtn = new JButton("Cancel");
        
        addBtn.addActionListener(e -> {
            try {
                String component = componentField.getText().trim();
                String maxScoreStr = maxScoreField.getText().trim();
                String weightStr = weightField.getText().trim();
                
                if (component.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Component name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    componentField.requestFocus();
                    return;
                }
                
                // Check for duplicate component name
                if (gradeComponents.contains(component)) {
                    JOptionPane.showMessageDialog(dialog, "A component with this name already exists.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    componentField.requestFocus();
                    return;
                }
                
                double maxScore;
                double weight;
                
                try {
                    maxScore = Double.parseDouble(maxScoreStr);
                    if (maxScore <= 0) {
                        JOptionPane.showMessageDialog(dialog, "Max score must be greater than 0.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                        maxScoreField.requestFocus();
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a valid number for max score.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    maxScoreField.requestFocus();
                    return;
                }
                
                try {
                    weight = Double.parseDouble(weightStr);
                    if (weight <= 0 || weight > 100) {
                        JOptionPane.showMessageDialog(dialog, "Weight must be between 0 and 100.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                        weightField.requestFocus();
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a valid number for weight.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    weightField.requestFocus();
                    return;
                }
                
                // Disable buttons during operation
                addBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                addBtn.setText("Adding...");
                
                final double finalMaxScore = maxScore;
                final double finalWeight = weight;
                
                // Add component to all enrollments in this section
                SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        List<Enrollment> enrollments = enrollmentService.listBySection(selectedItem.section.getSectionId());
                        int successCount = 0;
                        for (Enrollment enrollment : enrollments) {
                            String result = gradeService.addComponent(enrollment.getEnrollmentId(), component, null, finalMaxScore, finalWeight);
                            if ("ADDED".equals(result)) {
                                successCount++;
                            } else {
                                logger.warn("Failed to add component for enrollment {}: {}", enrollment.getEnrollmentId(), result);
                            }
                        }
                        return successCount;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            int count = get();
                            JOptionPane.showMessageDialog(dialog, 
                                "Grade component '" + component + "' added successfully!\n" +
                                "Applied to " + count + " student(s).",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            loadGrades(); // Refresh the grades view
                        } catch (Exception ex) {
                            logger.error("Error adding grade component", ex);
                            JOptionPane.showMessageDialog(dialog, "Error adding component: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            addBtn.setEnabled(true);
                            cancelBtn.setEnabled(true);
                            addBtn.setText("Add Component");
                        }
                    }
                };
                worker.execute();
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span 2, center");
        
        dialog.pack();
        dialog.setMinimumSize(new Dimension(450, 280));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void calculateFinalGrades() {
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.section == null) {
            JOptionPane.showMessageDialog(this, "Please select a section first.", "No Section", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (gradesModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No students to calculate grades for.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show calculation preview dialog
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Calculate Final Grades", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new MigLayout("fillx, insets 15", "[grow]", "[][grow][]"));
        
        mainPanel.add(new JLabel("<html><h3>Final Grade Calculation</h3>" +
            "<p>Course: " + selectedItem.section.getCourseCode() + " - " + selectedItem.section.getSectionNumber() + "</p></html>"), "wrap");
        
        // Preview table
        String[] previewColumns = {"Student", "Current Grade", "Letter Grade", "Status"};
        DefaultTableModel previewModel = new DefaultTableModel(previewColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        int readyCount = 0;
        int incompleteCount = 0;
        
        for (int i = 0; i < gradesModel.getRowCount(); i++) {
            String studentName = (String) gradesModel.getValueAt(i, 1);
            
            // Check if all grade components have values
            boolean complete = true;
            for (int j = 0; j < gradeComponents.size(); j++) {
                int col = FIXED_COLUMNS.length + j;
                Object value = gradesModel.getValueAt(i, col);
                if (value == null || value.toString().trim().isEmpty()) {
                    complete = false;
                    break;
                }
            }
            
            int overallCol = gradesModel.findColumn("Overall");
            int letterCol = gradesModel.findColumn("Letter Grade");
            
            String overall = overallCol != -1 ? String.valueOf(gradesModel.getValueAt(i, overallCol)) : "";
            String letter = letterCol != -1 ? String.valueOf(gradesModel.getValueAt(i, letterCol)) : "";
            
            String status;
            if (complete && !overall.isEmpty() && !overall.equals("null")) {
                status = "✓ Ready";
                readyCount++;
            } else {
                status = "⚠ Incomplete";
                incompleteCount++;
            }
            
            previewModel.addRow(new Object[]{studentName, overall, letter, status});
        }
        
        JTable previewTable = new JTable(previewModel);
        previewTable.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        mainPanel.add(scrollPane, "grow, wrap");
        
        // Summary
        JLabel summaryLabel = new JLabel("<html><b>Summary:</b> " + readyCount + " ready, " + incompleteCount + " incomplete</html>");
        if (incompleteCount > 0) {
            summaryLabel.setForeground(Color.ORANGE.darker());
        } else {
            summaryLabel.setForeground(new Color(0, 128, 0));
        }
        mainPanel.add(summaryLabel, "wrap");
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton calculateBtn = new JButton("Calculate & Save Finals");
        JButton cancelBtn = new JButton("Cancel");
        
        final int finalReadyCount = readyCount;
        
        calculateBtn.addActionListener(e -> {
            if (finalReadyCount == 0) {
                JOptionPane.showMessageDialog(dialog, "No students have complete grades to calculate.", "No Data", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            calculateBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            calculateBtn.setText("Calculating...");
            
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
                            // Validate maxScore values
                            boolean hasValidMaxScores = grades.stream()
                                .allMatch(g -> g.getMaxScore() != null && g.getMaxScore() > 0);
                            
                            if (!hasValidMaxScores) {
                                continue;
                            }
                            
                            // Calculate weighted final grade
                            double totalWeight = grades.stream().mapToDouble(Grade::getWeight).sum();
                            
                            // Allow some tolerance for weights not exactly 100%
                            if (totalWeight > 0) {
                                double finalGrade = grades.stream()
                                    .mapToDouble(g -> {
                                        double normalized = (g.getScore() / g.getMaxScore()) * 100;
                                        return normalized * (g.getWeight() / totalWeight);
                                    })
                                    .sum();
                                
                                String letterGrade = convertToLetterGrade(finalGrade);
                                boolean updated = enrollmentService.updateFinalGrade(enrollment.getEnrollmentId(), letterGrade);
                                if (updated) {
                                    calculatedCount++;
                                }
                            }
                        }
                    }
                    
                    return calculatedCount;
                }
                
                @Override
                protected void done() {
                    try {
                        Integer count = get();
                        dialog.dispose();
                        JOptionPane.showMessageDialog(GradeEntryPanel.this,
                            "Final grades calculated and saved for " + count + " student(s).",
                            "Calculation Complete", JOptionPane.INFORMATION_MESSAGE);
                        loadGrades(); // Refresh the view
                    } catch (Exception ex) {
                        logger.error("Error calculating final grades", ex);
                        JOptionPane.showMessageDialog(dialog,
                            "Error calculating final grades: " + ex.getMessage(),
                            "Calculation Error", JOptionPane.ERROR_MESSAGE);
                        calculateBtn.setEnabled(true);
                        cancelBtn.setEnabled(true);
                        calculateBtn.setText("Calculate & Save Finals");
                    }
                }
            };
            worker.execute();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(calculateBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setMinimumSize(new Dimension(550, 400));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportGrades() {
        if (gradesModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No grades to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SectionItem selectedItem = (SectionItem) sectionCombo.getSelectedItem();
        String defaultFileName = "grades";
        String sectionInfo = "Grades";
        
        if (selectedItem != null && selectedItem.section != null) {
            Section section = selectedItem.section;
            defaultFileName = section.getCourseCode() + "_" + section.getSectionNumber() + "_grades";
            defaultFileName = defaultFileName.replace(" ", "_");
            sectionInfo = section.getCourseCode() + " - " + section.getCourseTitle() + " (Section " + section.getSectionNumber() + ")";
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Grades");
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
        
        final String finalSectionInfo = sectionInfo;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header info
            writer.println("# Grade Export Report");
            writer.println("# " + finalSectionInfo);
            if (selectedItem != null && selectedItem.section != null) {
                writer.println("# Semester: " + selectedItem.section.getSemester() + " " + selectedItem.section.getYear());
            }
            writer.println("# Export Date: " + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("# Total Students: " + gradesModel.getRowCount());
            writer.println();
            
            // Write column headers
            StringBuilder header = new StringBuilder();
            for (int i = 0; i < gradesModel.getColumnCount(); i++) {
                if (i > 0) header.append(",");
                header.append(escapeCSV(gradesModel.getColumnName(i)));
            }
            writer.println(header);
            
            // Write data rows
            int gradeCountA = 0, gradeCountB = 0, gradeCountC = 0, gradeCountD = 0, gradeCountF = 0;
            
            for (int row = 0; row < gradesModel.getRowCount(); row++) {
                StringBuilder line = new StringBuilder();
                for (int col = 0; col < gradesModel.getColumnCount(); col++) {
                    if (col > 0) line.append(",");
                    Object value = gradesModel.getValueAt(row, col);
                    line.append(escapeCSV(value != null ? value.toString() : ""));
                }
                writer.println(line);
                
                // Count letter grades for summary
                int letterCol = gradesModel.findColumn("Letter Grade");
                if (letterCol != -1) {
                    Object letterValue = gradesModel.getValueAt(row, letterCol);
                    if (letterValue != null) {
                        switch (letterValue.toString()) {
                            case "A": gradeCountA++; break;
                            case "B": gradeCountB++; break;
                            case "C": gradeCountC++; break;
                            case "D": gradeCountD++; break;
                            case "F": gradeCountF++; break;
                        }
                    }
                }
            }
            
            // Write summary section
            writer.println();
            writer.println("# Grade Distribution Summary");
            writer.println("# A: " + gradeCountA + " students");
            writer.println("# B: " + gradeCountB + " students");
            writer.println("# C: " + gradeCountC + " students");
            writer.println("# D: " + gradeCountD + " students");
            writer.println("# F: " + gradeCountF + " students");
            
            JOptionPane.showMessageDialog(this,
                "Grades exported successfully!\n\n" +
                "File: " + file.getAbsolutePath() + "\n\n" +
                "Summary:\n" +
                "  Total Students: " + gradesModel.getRowCount() + "\n" +
                "  A: " + gradeCountA + ", B: " + gradeCountB + ", C: " + gradeCountC + 
                ", D: " + gradeCountD + ", F: " + gradeCountF,
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            logger.error("Error exporting grades", e);
            JOptionPane.showMessageDialog(this,
                "Error exporting file: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Escape a string for CSV format
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
                                    componentType, gradeValue, 100.0, defaultWeight * 100.0);
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
                        double weight = getDefaultWeight(componentType) * 100.0; // Convert to percentage
                        
                        // Use percentage weight for consistency with stored weights
                        double weightDecimal = weight / 100.0; // Convert back to decimal for calculation
                        totalPoints += grade * weightDecimal;
                        totalWeight += weightDecimal;
                    } catch (NumberFormatException e) {
                        // Skip invalid values
                    }
                }
            }
            
            // Calculate overall grade
            if (totalWeight > 0) {
                double overallGrade = totalPoints / totalWeight;
                
                // Use dynamic column lookup instead of hard-coded indices
                int overallCol = gradesModel.findColumn("Overall");
                if (overallCol != -1) {
                    gradesModel.setValueAt(df.format(overallGrade), row, overallCol);
                }
                
                // Calculate letter grade
                String letterGrade = convertToLetterGrade(overallGrade);
                int letterCol = gradesModel.findColumn("Letter Grade");
                if (letterCol != -1) {
                    gradesModel.setValueAt(letterGrade, row, letterCol);
                }
            } else {
                // Clear values when no weight
                int overallCol = gradesModel.findColumn("Overall");
                if (overallCol != -1) {
                    gradesModel.setValueAt("", row, overallCol);
                }
                
                int letterCol = gradesModel.findColumn("Letter Grade");
                if (letterCol != -1) {
                    gradesModel.setValueAt("", row, letterCol);
                }
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