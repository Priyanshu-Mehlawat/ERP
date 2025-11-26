package edu.univ.erp.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Course;
import net.miginfocom.swing.MigLayout;

/**
 * Course Management Panel for Admin
 * Allows CRUD operations on the course catalog
 */
public class CourseManagementPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(CourseManagementPanel.class);
    
    private final CourseDAO courseDAO;
    private final SectionDAO sectionDAO;
    
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> departmentFilterCombo;
    
    public CourseManagementPanel() {
        this.courseDAO = new CourseDAO();
        this.sectionDAO = new SectionDAO();
        initComponents();
        loadCourses();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Course Catalog Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        // Top toolbar with search and filters
        JPanel toolbarPanel = createToolbarPanel();
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        
        // Course table
        JPanel tablePanel = createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        // Action buttons panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[grow][]", ""));
        
        // Search field
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(25);
        searchField.setToolTipText("Search by course code, title, or description");
        searchField.addActionListener(e -> filterCourses());
        searchPanel.add(searchField);
        
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> filterCourses());
        searchPanel.add(searchBtn);
        
        // Department filter
        searchPanel.add(Box.createHorizontalStrut(20));
        searchPanel.add(new JLabel("Department:"));
        departmentFilterCombo = new JComboBox<>(new String[]{
            "All", "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology", 
            "Engineering", "Business", "English", "History"
        });
        departmentFilterCombo.addActionListener(e -> filterCourses());
        searchPanel.add(departmentFilterCombo);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadCourses());
        searchPanel.add(refreshBtn);
        
        panel.add(searchPanel, "wrap");
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columns = {"Course ID", "Code", "Title", "Credits", "Department", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setRowHeight(25);
        courseTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        courseTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        courseTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        courseTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        courseTable.getColumnModel().getColumn(5).setPreferredWidth(300);
        
        JScrollPane scrollPane = new JScrollPane(courseTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton addBtn = new JButton("Add Course");
        addBtn.addActionListener(e -> showAddCourseDialog());
        panel.add(addBtn);
        
        JButton editBtn = new JButton("Edit Course");
        editBtn.addActionListener(e -> showEditCourseDialog());
        panel.add(editBtn);
        
        JButton deleteBtn = new JButton("Delete Course");
        deleteBtn.addActionListener(e -> deleteCourse());
        panel.add(deleteBtn);
        
        JButton viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.addActionListener(e -> showCourseDetails());
        panel.add(viewDetailsBtn);
        
        JButton sectionsBtn = new JButton("View Sections");
        sectionsBtn.addActionListener(e -> viewCourseSections());
        panel.add(sectionsBtn);
        
        return panel;
    }
    
    private void loadCourses() {
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                return courseDAO.findAll();
            }
            
            @Override
            protected void done() {
                try {
                    List<Course> courses = get();
                    displayCourses(courses);
                } catch (Exception e) {
                    logger.error("Error loading courses", e);
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        "Error loading courses: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void displayCourses(List<Course> courses) {
        tableModel.setRowCount(0);
        for (Course course : courses) {
            tableModel.addRow(new Object[]{
                course.getCourseId(),
                course.getCode(),
                course.getTitle(),
                course.getCredits(),
                course.getDepartment() != null ? course.getDepartment() : "N/A",
                course.getDescription() != null ? course.getDescription() : ""
            });
        }
    }
    
    private void filterCourses() {
        String searchText = searchField.getText().trim();
        String departmentFilter = (String) departmentFilterCombo.getSelectedItem();
        
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                List<Course> allCourses = searchText.isEmpty() ? 
                    courseDAO.findAll() : courseDAO.search(searchText);
                
                // Apply department filter
                if (!"All".equals(departmentFilter)) {
                    return allCourses.stream()
                        .filter(c -> departmentFilter.equals(c.getDepartment()))
                        .toList();
                }
                return allCourses;
            }
            
            @Override
            protected void done() {
                try {
                    displayCourses(get());
                } catch (Exception e) {
                    logger.error("Error filtering courses", e);
                }
            }
        };
        worker.execute();
    }
    
    private void showAddCourseDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Add New Course", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        // Form fields
        JTextField codeField = new JTextField(10);
        JTextField titleField = new JTextField(30);
        JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 6, 1));
        JComboBox<String> departmentCombo = new JComboBox<>(new String[]{
            "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology",
            "Engineering", "Business", "English", "History"
        });
        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        dialog.add(new JLabel("Course Code:"), "");
        dialog.add(codeField, "wrap");
        
        dialog.add(new JLabel(""), "");
        JLabel codeInfo = new JLabel("<html><i>Example: CSE201, MATH101</i></html>");
        codeInfo.setForeground(Color.GRAY);
        codeInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        dialog.add(codeInfo, "wrap, gapbottom 10");
        
        dialog.add(new JLabel("Title:"), "");
        dialog.add(titleField, "wrap");
        
        dialog.add(new JLabel("Credits:"), "");
        dialog.add(creditsSpinner, "wrap");
        
        dialog.add(new JLabel("Department:"), "");
        dialog.add(departmentCombo, "wrap");
        
        dialog.add(new JLabel("Description:"), "");
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        dialog.add(descScroll, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Create Course");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String code = codeField.getText().trim().toUpperCase();
            String title = titleField.getText().trim();
            int credits = (Integer) creditsSpinner.getValue();
            String department = (String) departmentCombo.getSelectedItem();
            String description = descriptionArea.getText().trim();
            
            if (code.isEmpty() || title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Course code and title are required");
                return;
            }
            
            // Validate course code format
            if (!code.matches("^[A-Z]{3,4}\\d{3}$")) {
                JOptionPane.showMessageDialog(dialog, 
                    "Invalid course code format.\nExpected format: 3-4 letters followed by 3 digits\nExample: CSE201, MATH101",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            createCourse(code, title, credits, department, description);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, wrap");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void createCourse(String code, String title, int credits, String department, String description) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Course course = new Course();
                course.setCode(code);
                course.setTitle(title);
                course.setCredits(credits);
                course.setDepartment(department);
                course.setDescription(description.isEmpty() ? null : description);
                
                courseDAO.save(course);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        "Course created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadCourses();
                } catch (Exception e) {
                    logger.error("Error creating course", e);
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
                        JOptionPane.showMessageDialog(CourseManagementPanel.this,
                            "A course with code '" + code + "' already exists.",
                            "Duplicate Course",
                            JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(CourseManagementPanel.this,
                            "Error creating course: " + errorMsg,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
    }
    
    private void showEditCourseDialog() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to edit");
            return;
        }
        
        Long courseId = (Long) tableModel.getValueAt(selectedRow, 0);
        
        // Load course details
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            JOptionPane.showMessageDialog(this, "Course not found");
            return;
        }
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Edit Course", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        // Form fields (code is read-only)
        JTextField codeField = new JTextField(course.getCode(), 10);
        codeField.setEditable(false);
        codeField.setBackground(Color.LIGHT_GRAY);
        JTextField titleField = new JTextField(course.getTitle(), 30);
        JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(course.getCredits(), 1, 6, 1));
        JComboBox<String> departmentCombo = new JComboBox<>(new String[]{
            "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology",
            "Engineering", "Business", "English", "History"
        });
        if (course.getDepartment() != null) {
            departmentCombo.setSelectedItem(course.getDepartment());
        }
        JTextArea descriptionArea = new JTextArea(course.getDescription() != null ? course.getDescription() : "", 5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        dialog.add(new JLabel("Course Code:"), "");
        dialog.add(codeField, "wrap");
        
        dialog.add(new JLabel(""), "");
        JLabel codeInfo = new JLabel("<html><i>Course code cannot be changed</i></html>");
        codeInfo.setForeground(Color.GRAY);
        codeInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        dialog.add(codeInfo, "wrap, gapbottom 10");
        
        dialog.add(new JLabel("Title:"), "");
        dialog.add(titleField, "wrap");
        
        dialog.add(new JLabel("Credits:"), "");
        dialog.add(creditsSpinner, "wrap");
        
        dialog.add(new JLabel("Department:"), "");
        dialog.add(departmentCombo, "wrap");
        
        dialog.add(new JLabel("Description:"), "");
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        dialog.add(descScroll, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            course.setTitle(titleField.getText().trim());
            course.setCredits((Integer) creditsSpinner.getValue());
            course.setDepartment((String) departmentCombo.getSelectedItem());
            String description = descriptionArea.getText().trim();
            course.setDescription(description.isEmpty() ? null : description);
            
            if (course.getTitle().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Course title is required");
                return;
            }
            
            updateCourse(course);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, wrap");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void updateCourse(Course course) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                courseDAO.update(course);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        "Course updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadCourses();
                } catch (Exception e) {
                    logger.error("Error updating course", e);
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        "Error updating course: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void deleteCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to delete");
            return;
        }
        
        Long courseId = (Long) tableModel.getValueAt(selectedRow, 0);
        String courseCode = (String) tableModel.getValueAt(selectedRow, 1);
        String courseTitle = (String) tableModel.getValueAt(selectedRow, 2);
        
        // Check if course has sections
        int sectionCount = sectionDAO.listByCourse(courseId, null, null).size();
        if (sectionCount > 0) {
            JOptionPane.showMessageDialog(this,
                "Cannot delete course with existing sections.\n" +
                "This course has " + sectionCount + " section(s).\n" +
                "Please delete all sections first.",
                "Cannot Delete",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete course:\n" +
            courseCode + " - " + courseTitle + "?\n\n" +
            "This action cannot be undone.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    courseDAO.delete(courseId);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(CourseManagementPanel.this,
                            "Course deleted successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        loadCourses();
                    } catch (Exception e) {
                        logger.error("Error deleting course", e);
                        JOptionPane.showMessageDialog(CourseManagementPanel.this,
                            "Error deleting course: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void showCourseDetails() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course");
            return;
        }
        
        Long courseId = (Long) tableModel.getValueAt(selectedRow, 0);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                Course course = courseDAO.findById(courseId);
                int sectionCount = sectionDAO.listByCourse(courseId, null, null).size();
                
                StringBuilder details = new StringBuilder();
                details.append("COURSE DETAILS\n");
                details.append("═══════════════════════════════════════\n\n");
                details.append("Course ID: ").append(course.getCourseId()).append("\n");
                details.append("Code: ").append(course.getCode()).append("\n");
                details.append("Title: ").append(course.getTitle()).append("\n");
                details.append("Credits: ").append(course.getCredits()).append("\n");
                details.append("Department: ").append(course.getDepartment() != null ? course.getDepartment() : "N/A").append("\n\n");
                
                details.append("DESCRIPTION\n");
                details.append("───────────────────────────────────────\n");
                details.append(course.getDescription() != null ? course.getDescription() : "No description available").append("\n\n");
                
                details.append("SECTIONS\n");
                details.append("───────────────────────────────────────\n");
                details.append("Total Sections: ").append(sectionCount).append("\n");
                
                return details.toString();
            }
            
            @Override
            protected void done() {
                try {
                    String details = get();
                    JTextArea textArea = new JTextArea(details, 20, 50);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        scrollPane,
                        "Course Details",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error loading course details", e);
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        "Error loading course details: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void viewCourseSections() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course");
            return;
        }
        
        Long courseId = (Long) tableModel.getValueAt(selectedRow, 0);
        String courseCode = (String) tableModel.getValueAt(selectedRow, 1);
        String courseTitle = (String) tableModel.getValueAt(selectedRow, 2);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                var sections = sectionDAO.listByCourse(courseId, null, null);
                
                if (sections.isEmpty()) {
                    return "No sections found for " + courseCode + " - " + courseTitle;
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append("SECTIONS FOR ").append(courseCode).append(" - ").append(courseTitle).append("\n");
                sb.append("═══════════════════════════════════════════════════════════\n\n");
                
                for (var section : sections) {
                    sb.append("Section ").append(section.getSectionNumber()).append("\n");
                    sb.append("  Semester: ").append(section.getSemester()).append(" ").append(section.getYear()).append("\n");
                    sb.append("  Room: ").append(section.getRoom()).append("\n");
                    sb.append("  Capacity: ").append(section.getEnrolled()).append("/").append(section.getCapacity()).append("\n");
                    if (section.getInstructorName() != null) {
                        sb.append("  Instructor: ").append(section.getInstructorName()).append("\n");
                    }
                    sb.append("\n");
                }
                
                return sb.toString();
            }
            
            @Override
            protected void done() {
                try {
                    String info = get();
                    JTextArea textArea = new JTextArea(info, 20, 60);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        scrollPane,
                        "Course Sections",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error loading sections", e);
                    JOptionPane.showMessageDialog(CourseManagementPanel.this,
                        "Error loading sections: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
