package edu.univ.erp.ui.admin;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import net.miginfocom.swing.MigLayout;

/**
 * Instructor Assignment Panel for Admin
 * Allows assigning instructors to course sections
 */
public class InstructorAssignmentPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InstructorAssignmentPanel.class);
    
    private final SectionDAO sectionDAO;
    private final CourseDAO courseDAO;
    private final InstructorDAO instructorDAO;
    
    private JTable sectionTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> semesterFilterCombo;
    private JComboBox<String> assignmentFilterCombo;
    private JTextField searchField;
    
    // Cache for courses and instructors
    private Map<Long, Course> courseCache = new HashMap<>();
    private Map<Long, Instructor> instructorCache = new HashMap<>();
    private List<Instructor> allInstructors;
    
    public InstructorAssignmentPanel() {
        this.sectionDAO = new SectionDAO();
        this.courseDAO = new CourseDAO();
        this.instructorDAO = new InstructorDAO();
        initComponents();
        loadData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Content panel that will be scrollable
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Instructor Assignment");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        // Stats panel on right of title
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statsPanel.add(new JLabel("Quick assign instructors to unassigned sections"));
        titlePanel.add(statsPanel, BorderLayout.EAST);
        
        contentPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        // Toolbar
        mainPanel.add(createToolbarPanel(), BorderLayout.NORTH);
        
        // Table
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        
        // Buttons
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        
        contentPanel.add(mainPanel, BorderLayout.CENTER);
        
        // Wrap entire content in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[grow]", ""));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Search
        filterPanel.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        searchField.addActionListener(e -> filterSections());
        filterPanel.add(searchField);
        
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> filterSections());
        filterPanel.add(searchBtn);
        
        filterPanel.add(Box.createHorizontalStrut(15));
        
        // Semester filter
        filterPanel.add(new JLabel("Semester:"));
        semesterFilterCombo = new JComboBox<>(new String[]{
            "All", "Fall 2024", "Spring 2025", "Summer 2025", "Fall 2025"
        });
        semesterFilterCombo.addActionListener(e -> filterSections());
        filterPanel.add(semesterFilterCombo);
        
        filterPanel.add(Box.createHorizontalStrut(15));
        
        // Assignment status filter
        filterPanel.add(new JLabel("Status:"));
        assignmentFilterCombo = new JComboBox<>(new String[]{
            "All Sections", "Unassigned Only", "Assigned Only"
        });
        assignmentFilterCombo.addActionListener(e -> filterSections());
        filterPanel.add(assignmentFilterCombo);
        
        filterPanel.add(Box.createHorizontalStrut(15));
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadData());
        filterPanel.add(refreshBtn);
        
        panel.add(filterPanel, "wrap");
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"Section ID", "Course Code", "Course Title", "Section #", 
                           "Semester", "Schedule", "Room", "Current Instructor", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        sectionTable = new JTable(tableModel);
        sectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionTable.setRowHeight(28);
        sectionTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        sectionTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        sectionTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        sectionTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        sectionTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        sectionTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        sectionTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        sectionTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        sectionTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        sectionTable.getColumnModel().getColumn(8).setPreferredWidth(80);
        
        JScrollPane scrollPane = new JScrollPane(sectionTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        JButton assignBtn = new JButton("Assign Instructor");
        assignBtn.setFont(new Font("Arial", Font.BOLD, 12));
        assignBtn.addActionListener(e -> showAssignDialog());
        panel.add(assignBtn);
        
        JButton unassignBtn = new JButton("Remove Assignment");
        unassignBtn.addActionListener(e -> removeAssignment());
        panel.add(unassignBtn);
        
        JButton viewDetailsBtn = new JButton("View Section Details");
        viewDetailsBtn.addActionListener(e -> viewSectionDetails());
        panel.add(viewDetailsBtn);
        
        JButton viewInstructorBtn = new JButton("View Instructor Info");
        viewInstructorBtn.addActionListener(e -> viewInstructorInfo());
        panel.add(viewInstructorBtn);
        
        return panel;
    }
    
    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Load all courses
                List<Course> courses = courseDAO.findAll();
                courseCache.clear();
                for (Course c : courses) {
                    courseCache.put(c.getCourseId(), c);
                }
                
                // Load all instructors
                allInstructors = instructorDAO.findAll();
                instructorCache.clear();
                for (Instructor i : allInstructors) {
                    instructorCache.put(i.getInstructorId(), i);
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    loadSections();
                } catch (Exception e) {
                    logger.error("Error loading data", e);
                    JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                        "Error loading data: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void loadSections() {
        SwingWorker<List<Section>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Section> doInBackground() throws Exception {
                return sectionDAO.findAll();
            }
            
            @Override
            protected void done() {
                try {
                    List<Section> sections = get();
                    displaySections(sections);
                } catch (Exception e) {
                    logger.error("Error loading sections", e);
                }
            }
        };
        worker.execute();
    }
    
    private void displaySections(List<Section> sections) {
        tableModel.setRowCount(0);
        
        for (Section section : sections) {
            Course course = courseCache.get(section.getCourseId());
            Instructor instructor = section.getInstructorId() != null ? 
                instructorCache.get(section.getInstructorId()) : null;
            
            String courseCode = course != null ? course.getCode() : "N/A";
            String courseTitle = course != null ? course.getTitle() : "N/A";
            String instructorName = instructor != null ? instructor.getFullName() : "-- Not Assigned --";
            String status = instructor != null ? "Assigned" : "Unassigned";
            String schedule = formatSchedule(section);
            String semester = section.getSemester() + " " + section.getYear();
            
            tableModel.addRow(new Object[]{
                section.getSectionId(),
                courseCode,
                courseTitle,
                section.getSectionNumber(),
                semester,
                schedule,
                section.getRoom(),
                instructorName,
                status
            });
        }
    }
    
    private String formatSchedule(Section section) {
        StringBuilder schedule = new StringBuilder();
        if (section.getDayOfWeek() != null) {
            schedule.append(section.getDayOfWeek());
        }
        if (section.getStartTime() != null && section.getEndTime() != null) {
            if (schedule.length() > 0) schedule.append(" ");
            schedule.append(section.getStartTime().toString())
                    .append("-")
                    .append(section.getEndTime().toString());
        }
        return schedule.length() > 0 ? schedule.toString() : "TBA";
    }
    
    private void filterSections() {
        String searchText = searchField.getText().trim().toLowerCase();
        String semesterFilter = (String) semesterFilterCombo.getSelectedItem();
        String assignmentFilter = (String) assignmentFilterCombo.getSelectedItem();
        
        SwingWorker<List<Section>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Section> doInBackground() throws Exception {
                List<Section> allSections = sectionDAO.findAll();
                
                return allSections.stream()
                    .filter(s -> {
                        // Search filter
                        if (!searchText.isEmpty()) {
                            Course course = courseCache.get(s.getCourseId());
                            String courseCode = course != null ? course.getCode().toLowerCase() : "";
                            String courseTitle = course != null ? course.getTitle().toLowerCase() : "";
                            if (!courseCode.contains(searchText) && !courseTitle.contains(searchText)) {
                                return false;
                            }
                        }
                        
                        // Semester filter
                        if (!"All".equals(semesterFilter)) {
                            String[] parts = semesterFilter.split(" ");
                            if (parts.length >= 1 && !parts[0].equals(s.getSemester())) {
                                return false;
                            }
                        }
                        
                        // Assignment filter
                        if ("Unassigned Only".equals(assignmentFilter)) {
                            return s.getInstructorId() == null;
                        } else if ("Assigned Only".equals(assignmentFilter)) {
                            return s.getInstructorId() != null;
                        }
                        
                        return true;
                    })
                    .toList();
            }
            
            @Override
            protected void done() {
                try {
                    displaySections(get());
                } catch (Exception e) {
                    logger.error("Error filtering sections", e);
                }
            }
        };
        worker.execute();
    }
    
    private void showAssignDialog() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a section to assign an instructor",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        String courseCode = (String) tableModel.getValueAt(selectedRow, 1);
        String courseTitle = (String) tableModel.getValueAt(selectedRow, 2);
        String sectionNum = (String) tableModel.getValueAt(selectedRow, 3);
        String currentInstructor = (String) tableModel.getValueAt(selectedRow, 7);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Assign Instructor", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx, insets 20", "[right]rel[grow,fill]", ""));
        
        // Section info
        dialog.add(new JLabel("Course:"), "");
        dialog.add(new JLabel(courseCode + " - " + courseTitle), "wrap");
        
        dialog.add(new JLabel("Section:"), "");
        dialog.add(new JLabel(sectionNum), "wrap");
        
        dialog.add(new JLabel("Current Instructor:"), "");
        dialog.add(new JLabel(currentInstructor), "wrap");
        
        dialog.add(new JSeparator(), "span, growx, gaptop 10, gapbottom 10, wrap");
        
        // Instructor selection
        dialog.add(new JLabel("Assign To:"), "");
        JComboBox<String> instructorCombo = new JComboBox<>();
        instructorCombo.addItem("-- Select Instructor --");
        
        if (allInstructors != null) {
            for (Instructor instructor : allInstructors) {
                String dept = instructor.getDepartment() != null ? " (" + instructor.getDepartment() + ")" : "";
                instructorCombo.addItem(instructor.getFullName() + dept + " [ID: " + instructor.getInstructorId() + "]");
            }
        }
        dialog.add(instructorCombo, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton assignBtn = new JButton("Assign");
        JButton cancelBtn = new JButton("Cancel");
        
        assignBtn.addActionListener(e -> {
            int instructorIndex = instructorCombo.getSelectedIndex();
            if (instructorIndex <= 0) {
                JOptionPane.showMessageDialog(dialog,
                    "Please select an instructor",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Instructor selectedInstructor = allInstructors.get(instructorIndex - 1);
            assignInstructor(sectionId, selectedInstructor.getInstructorId(), 
                           selectedInstructor.getFullName(), courseCode, sectionNum);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(assignBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, gaptop 15, wrap");
        
        dialog.pack();
        dialog.setMinimumSize(new Dimension(450, 250));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void assignInstructor(Long sectionId, Long instructorId, String instructorName, 
                                  String courseCode, String sectionNum) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                sectionDAO.assignInstructor(sectionId, instructorId);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                        "Successfully assigned " + instructorName + 
                        "\nto " + courseCode + " Section " + sectionNum,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadSections();
                } catch (Exception e) {
                    logger.error("Error assigning instructor", e);
                    JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                        "Error assigning instructor: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void removeAssignment() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a section",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String currentInstructor = (String) tableModel.getValueAt(selectedRow, 7);
        if ("-- Not Assigned --".equals(currentInstructor)) {
            JOptionPane.showMessageDialog(this,
                "This section has no instructor assigned",
                "No Assignment", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        String courseCode = (String) tableModel.getValueAt(selectedRow, 1);
        String sectionNum = (String) tableModel.getValueAt(selectedRow, 3);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove instructor assignment from " + courseCode + " Section " + sectionNum + "?\n\n" +
            "Current Instructor: " + currentInstructor,
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    sectionDAO.assignInstructor(sectionId, null);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                            "Instructor assignment removed successfully",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadSections();
                    } catch (Exception e) {
                        logger.error("Error removing assignment", e);
                        JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                            "Error removing assignment: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void viewSectionDetails() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section");
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                Section section = sectionDAO.findById(sectionId);
                Course course = courseCache.get(section.getCourseId());
                Instructor instructor = section.getInstructorId() != null ? 
                    instructorCache.get(section.getInstructorId()) : null;
                
                StringBuilder details = new StringBuilder();
                details.append("SECTION DETAILS\n");
                details.append("═══════════════════════════════════════\n\n");
                details.append("Course: ").append(course != null ? course.getCode() + " - " + course.getTitle() : "N/A").append("\n");
                details.append("Section Number: ").append(section.getSectionNumber()).append("\n");
                details.append("Semester: ").append(section.getSemester()).append(" ").append(section.getYear()).append("\n");
                details.append("Schedule: ").append(formatSchedule(section)).append("\n");
                details.append("Room: ").append(section.getRoom()).append("\n\n");
                
                details.append("INSTRUCTOR\n");
                details.append("───────────────────────────────────────\n");
                if (instructor != null) {
                    details.append("Name: ").append(instructor.getFullName()).append("\n");
                    details.append("Email: ").append(instructor.getEmail() != null ? instructor.getEmail() : "N/A").append("\n");
                    details.append("Department: ").append(instructor.getDepartment() != null ? instructor.getDepartment() : "N/A").append("\n");
                } else {
                    details.append("No instructor assigned\n");
                }
                
                details.append("\nENROLLMENT\n");
                details.append("───────────────────────────────────────\n");
                details.append("Capacity: ").append(section.getCapacity()).append("\n");
                details.append("Enrolled: ").append(section.getEnrolled()).append("\n");
                details.append("Available: ").append(section.getCapacity() - section.getEnrolled()).append("\n");
                
                return details.toString();
            }
            
            @Override
            protected void done() {
                try {
                    String details = get();
                    JTextArea textArea = new JTextArea(details, 18, 45);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                        scrollPane, "Section Details", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error loading details", e);
                }
            }
        };
        worker.execute();
    }
    
    private void viewInstructorInfo() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section");
            return;
        }
        
        String currentInstructor = (String) tableModel.getValueAt(selectedRow, 7);
        if ("-- Not Assigned --".equals(currentInstructor)) {
            JOptionPane.showMessageDialog(this,
                "This section has no instructor assigned",
                "No Instructor", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                Section section = sectionDAO.findById(sectionId);
                if (section.getInstructorId() == null) {
                    return null;
                }
                
                Instructor instructor = instructorDAO.findById(section.getInstructorId());
                if (instructor == null) {
                    return null;
                }
                
                // Count sections taught by this instructor
                List<Section> allSections = sectionDAO.findAll();
                long sectionCount = allSections.stream()
                    .filter(s -> instructor.getInstructorId().equals(s.getInstructorId()))
                    .count();
                
                StringBuilder info = new StringBuilder();
                info.append("INSTRUCTOR INFORMATION\n");
                info.append("═══════════════════════════════════════\n\n");
                info.append("Name: ").append(instructor.getFullName()).append("\n");
                info.append("Employee ID: ").append(instructor.getEmployeeId() != null ? instructor.getEmployeeId() : "N/A").append("\n");
                info.append("Email: ").append(instructor.getEmail() != null ? instructor.getEmail() : "N/A").append("\n");
                info.append("Phone: ").append(instructor.getPhoneNumber() != null ? instructor.getPhoneNumber() : "N/A").append("\n");
                info.append("Department: ").append(instructor.getDepartment() != null ? instructor.getDepartment() : "N/A").append("\n\n");
                
                info.append("TEACHING LOAD\n");
                info.append("───────────────────────────────────────\n");
                info.append("Sections Assigned: ").append(sectionCount).append("\n");
                
                return info.toString();
            }
            
            @Override
            protected void done() {
                try {
                    String info = get();
                    if (info == null) {
                        JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                            "Instructor information not available",
                            "Not Found", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    JTextArea textArea = new JTextArea(info, 15, 45);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(InstructorAssignmentPanel.this,
                        scrollPane, "Instructor Information", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error loading instructor info", e);
                }
            }
        };
        worker.execute();
    }
}
