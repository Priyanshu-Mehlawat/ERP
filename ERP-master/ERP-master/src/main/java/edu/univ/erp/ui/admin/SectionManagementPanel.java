package edu.univ.erp.ui.admin;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;
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
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;

/**
 * Section Management Panel for Admin
 * Allows CRUD operations on course sections and instructor assignments.
 * 
 * <p><b>Resource Management:</b></p>
 * <p>This panel supports dependency injection for better lifecycle management and testability.
 * When using the parameterized constructor, callers can inject managed DAO instances.
 * Call {@link #dispose()} when the panel is no longer needed to release resources.</p>
 * 
 * <p><b>Usage Example (Recommended):</b></p>
 * <pre>{@code
 * // Create DAOs (could be managed by a DI container or factory)
 * SectionDAO sectionDAO = new SectionDAO();
 * CourseDAO courseDAO = new CourseDAO();
 * InstructorDAO instructorDAO = new InstructorDAO();
 * SectionService sectionService = new SectionService();
 * 
 * // Create panel with injected dependencies
 * SectionManagementPanel panel = new SectionManagementPanel(
 *     sectionDAO, courseDAO, instructorDAO, sectionService);
 * 
 * // Add to dialog
 * JDialog dialog = new JDialog(parent, "Section Management", true);
 * dialog.add(panel, BorderLayout.CENTER);
 * 
 * // Add cleanup on close
 * dialog.addWindowListener(new WindowAdapter() {
 *     public void windowClosed(WindowEvent e) {
 *         panel.dispose();
 *     }
 * });
 * 
 * dialog.setVisible(true);
 * }</pre>
 * 
 * @see SectionDAO
 * @see SectionService
 */
public class SectionManagementPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SectionManagementPanel.class);
    
    private final SectionDAO sectionDAO;
    private final CourseDAO courseDAO;
    private final InstructorDAO instructorDAO;
    private final SectionService sectionService;
    
    private JTable sectionTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> semesterFilterCombo;
    
    /**
     * Constructor with dependency injection for better resource management and testability.
     * 
     * @param sectionDAO Section data access object
     * @param courseDAO Course data access object
     * @param instructorDAO Instructor data access object
     * @param sectionService Section business logic service
     */
    public SectionManagementPanel(SectionDAO sectionDAO, CourseDAO courseDAO, 
                                   InstructorDAO instructorDAO, SectionService sectionService) {
        this.sectionDAO = sectionDAO;
        this.courseDAO = courseDAO;
        this.instructorDAO = instructorDAO;
        this.sectionService = sectionService;
        initComponents();
        loadSections();
    }
    
    /**
     * Default constructor for backward compatibility.
     * Creates new DAO instances - use parameterized constructor for better resource management.
     * 
     * @deprecated Use {@link #SectionManagementPanel(SectionDAO, CourseDAO, InstructorDAO, SectionService)} instead
     */
    @Deprecated
    public SectionManagementPanel() {
        this(new SectionDAO(), new CourseDAO(), new InstructorDAO(), new SectionService());
    }
    
    /**
     * Cleanup method to release any resources.
     * Call this method when the panel is no longer needed.
     * Note: Current DAOs use try-with-resources for connections, so no persistent resources to clean up.
     * This method is provided for future extensibility and lifecycle management.
     */
    public void dispose() {
        // Future: If DAOs hold persistent resources, clean them up here
        // For now, DAOs use try-with-resources for each operation, so no cleanup needed
        logger.debug("SectionManagementPanel disposed");
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Section Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        // Top toolbar with search and filters
        JPanel toolbarPanel = createToolbarPanel();
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        
        // Section table
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
        searchField = new JTextField(20);
        searchField.addActionListener(e -> filterSections());
        searchPanel.add(searchField);
        
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> filterSections());
        searchPanel.add(searchBtn);
        
        // Semester filter
        searchPanel.add(Box.createHorizontalStrut(20));
        searchPanel.add(new JLabel("Semester:"));
        semesterFilterCombo = new JComboBox<>(new String[]{"All", "Fall 2024", "Spring 2025", "Summer 2025", "Fall 2025"});
        semesterFilterCombo.addActionListener(e -> filterSections());
        searchPanel.add(semesterFilterCombo);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadSections());
        searchPanel.add(refreshBtn);
        
        panel.add(searchPanel, "wrap");
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columns = {"Section ID", "Course Code", "Course Title", "Section #", "Semester", 
                           "Instructor", "Schedule", "Room", "Capacity", "Enrolled"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        sectionTable = new JTable(tableModel);
        sectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionTable.setRowHeight(25);
        sectionTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        sectionTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        sectionTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        sectionTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        sectionTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        sectionTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        sectionTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        sectionTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        sectionTable.getColumnModel().getColumn(7).setPreferredWidth(80);
        sectionTable.getColumnModel().getColumn(8).setPreferredWidth(70);
        sectionTable.getColumnModel().getColumn(9).setPreferredWidth(70);
        
        JScrollPane scrollPane = new JScrollPane(sectionTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton addBtn = new JButton("Add Section");
        addBtn.addActionListener(e -> showAddSectionDialog());
        panel.add(addBtn);
        
        JButton editBtn = new JButton("Edit Section");
        editBtn.addActionListener(e -> showEditSectionDialog());
        panel.add(editBtn);
        
        JButton deleteBtn = new JButton("Delete Section");
        deleteBtn.addActionListener(e -> deleteSection());
        panel.add(deleteBtn);
        
        JButton assignInstructorBtn = new JButton("Assign Instructor");
        assignInstructorBtn.addActionListener(e -> showAssignInstructorDialog());
        panel.add(assignInstructorBtn);
        
        JButton viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.addActionListener(e -> showSectionDetails());
        panel.add(viewDetailsBtn);
        
        return panel;
    }
    
    private void loadSections() {
        SwingWorker<List<SectionViewData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SectionViewData> doInBackground() throws Exception {
                // Fetch sections
                List<Section> sections = sectionService.listAllSections();
                
                // Preload all courses and instructors to avoid N+1 queries
                Map<Long, Course> courseMap = new java.util.HashMap<>();
                Map<Long, Instructor> instructorMap = new java.util.HashMap<>();
                
                try {
                    List<Course> courses = courseDAO.findAll();
                    for (Course course : courses) {
                        courseMap.put(course.getCourseId(), course);
                    }
                } catch (Exception e) {
                    logger.error("Error preloading courses", e);
                }
                
                try {
                    List<Instructor> instructors = instructorDAO.findAll();
                    for (Instructor instructor : instructors) {
                        instructorMap.put(instructor.getInstructorId(), instructor);
                    }
                } catch (SQLException e) {
                    logger.error("Error preloading instructors", e);
                }
                
                // Build view data objects with all resolved references
                List<SectionViewData> viewDataList = new java.util.ArrayList<>();
                for (Section section : sections) {
                    Course course = courseMap.get(section.getCourseId());
                    Instructor instructor = section.getInstructorId() != null ? 
                        instructorMap.get(section.getInstructorId()) : null;
                    
                    String courseCode = course != null ? course.getCode() : "N/A";
                    String courseTitle = course != null ? course.getTitle() : "N/A";
                    String instructorName = instructor != null ? instructor.getFullName() : "Not Assigned";
                    String schedule = formatSchedule(section);
                    
                    viewDataList.add(new SectionViewData(
                        section.getSectionId(),
                        courseCode,
                        courseTitle,
                        section.getSectionNumber(),
                        section.getSemester() + " " + section.getYear(),
                        instructorName,
                        schedule,
                        section.getRoom(),
                        section.getCapacity(),
                        section.getEnrolled()
                    ));
                }
                
                return viewDataList;
            }
            
            @Override
            protected void done() {
                try {
                    List<SectionViewData> viewData = get();
                    displaySections(viewData);
                } catch (Exception e) {
                    logger.error("Error loading sections", e);
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Error loading sections: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void displaySections(List<SectionViewData> viewDataList) {
        tableModel.setRowCount(0);
        for (SectionViewData data : viewDataList) {
            tableModel.addRow(new Object[]{
                data.sectionId,
                data.courseCode,
                data.courseTitle,
                data.sectionNumber,
                data.semester,
                data.instructorName,
                data.schedule,
                data.room,
                data.capacity,
                data.enrolled
            });
        }
    }
    
    /**
     * Internal DTO to hold pre-resolved section view data.
     * All database lookups are performed in background thread before creating this object.
     */
    private static class SectionViewData {
        final Long sectionId;
        final String courseCode;
        final String courseTitle;
        final String sectionNumber;
        final String semester;
        final String instructorName;
        final String schedule;
        final String room;
        final int capacity;
        final int enrolled;
        
        SectionViewData(Long sectionId, String courseCode, String courseTitle, String sectionNumber,
                       String semester, String instructorName, String schedule, String room,
                       int capacity, int enrolled) {
            this.sectionId = sectionId;
            this.courseCode = courseCode;
            this.courseTitle = courseTitle;
            this.sectionNumber = sectionNumber;
            this.semester = semester;
            this.instructorName = instructorName;
            this.schedule = schedule;
            this.room = room;
            this.capacity = capacity;
            this.enrolled = enrolled;
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
        
        SwingWorker<List<SectionViewData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SectionViewData> doInBackground() throws Exception {
                // Preload all courses and instructors into maps
                Map<Long, Course> courseMap = new java.util.HashMap<>();
                List<Course> courses = courseDAO.findAll();
                for (Course course : courses) {
                    courseMap.put(course.getCourseId(), course);
                }
                
                Map<Long, Instructor> instructorMap = new java.util.HashMap<>();
                try {
                    List<Instructor> instructors = instructorDAO.findAll();
                    for (Instructor instructor : instructors) {
                        instructorMap.put(instructor.getInstructorId(), instructor);
                    }
                } catch (SQLException e) {
                    logger.error("Error preloading instructors for filtering", e);
                }
                
                List<Section> allSections = sectionService.listAllSections();
                
                // Extract semester token from filter (e.g., "Fall 2024" -> "Fall")
                String semesterToken = null;
                if (semesterFilter != null && !"All".equals(semesterFilter)) {
                    String[] parts = semesterFilter.split("\\s+");
                    if (parts.length > 0) {
                        semesterToken = parts[0];
                    }
                }
                final String finalSemesterToken = semesterToken;
                
                // Filter sections and build view data
                List<SectionViewData> viewDataList = new java.util.ArrayList<>();
                for (Section s : allSections) {
                    // Search filter
                    boolean matchesSearch = searchText.isEmpty();
                    if (!matchesSearch) {
                        Course course = courseMap.get(s.getCourseId());
                        String courseCode = course != null && course.getCode() != null ? 
                            course.getCode().toLowerCase() : "";
                        String courseTitle = course != null && course.getTitle() != null ? 
                            course.getTitle().toLowerCase() : "";
                        String room = s.getRoom() != null ? s.getRoom().toLowerCase() : "";
                        
                        matchesSearch = courseCode.contains(searchText) ||
                                      courseTitle.contains(searchText) ||
                                      room.contains(searchText);
                    }
                    
                    // Semester filter
                    boolean matchesSemester = "All".equals(semesterFilter) || 
                        (finalSemesterToken != null && s.getSemester() != null && 
                         s.getSemester().equals(finalSemesterToken));
                    
                    if (matchesSearch && matchesSemester) {
                        // Build view data for matched sections
                        Course course = courseMap.get(s.getCourseId());
                        Instructor instructor = s.getInstructorId() != null ? 
                            instructorMap.get(s.getInstructorId()) : null;
                        
                        String courseCode = course != null ? course.getCode() : "N/A";
                        String courseTitle = course != null ? course.getTitle() : "N/A";
                        String instructorName = instructor != null ? instructor.getFullName() : "Not Assigned";
                        String schedule = formatSchedule(s);
                        
                        viewDataList.add(new SectionViewData(
                            s.getSectionId(),
                            courseCode,
                            courseTitle,
                            s.getSectionNumber(),
                            s.getSemester() + " " + s.getYear(),
                            instructorName,
                            schedule,
                            s.getRoom(),
                            s.getCapacity(),
                            s.getEnrolled()
                        ));
                    }
                }
                
                return viewDataList;
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
    
    private void showAddSectionDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Add New Section", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        // Load courses for dropdown
        List<Course> courses = courseDAO.findAll();
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses available. Please add courses first.");
            return;
        }
        
        // Form fields
        JComboBox<String> courseCombo = new JComboBox<>();
        for (Course course : courses) {
            courseCombo.addItem(course.getCode() + " - " + course.getTitle());
        }
        
        JTextField sectionNumberField = new JTextField("001", 10);
        JComboBox<String> semesterCombo = new JComboBox<>(new String[]{
            "Fall", "Spring", "Summer"
        });
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(2025, 2024, 2030, 1));
        JTextField dayOfWeekField = new JTextField("Monday,Wednesday", 20);
        JSpinner startHourSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 23, 1));
        JSpinner startMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 15));
        JSpinner endHourSpinner = new JSpinner(new SpinnerNumberModel(11, 0, 23, 1));
        JSpinner endMinuteSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 59, 15));
        JTextField roomField = new JTextField("Building A - Room 101", 20);
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(30, 1, 200, 1));
        
        dialog.add(new JLabel("Course:"), "");
        dialog.add(courseCombo, "wrap");
        
        dialog.add(new JLabel("Section Number:"), "");
        dialog.add(sectionNumberField, "wrap");
        
        dialog.add(new JLabel("Semester:"), "");
        dialog.add(semesterCombo, "wrap");
        
        dialog.add(new JLabel("Year:"), "");
        dialog.add(yearSpinner, "wrap");
        
        dialog.add(new JLabel("Day of Week:"), "");
        dialog.add(dayOfWeekField, "wrap");
        
        dialog.add(new JLabel("Start Time:"), "");
        JPanel startTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        startTimePanel.add(startHourSpinner);
        startTimePanel.add(new JLabel(":"));
        startTimePanel.add(startMinuteSpinner);
        dialog.add(startTimePanel, "wrap");
        
        dialog.add(new JLabel("End Time:"), "");
        JPanel endTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        endTimePanel.add(endHourSpinner);
        endTimePanel.add(new JLabel(":"));
        endTimePanel.add(endMinuteSpinner);
        dialog.add(endTimePanel, "wrap");
        
        dialog.add(new JLabel("Room:"), "");
        dialog.add(roomField, "wrap");
        
        dialog.add(new JLabel("Capacity:"), "");
        dialog.add(capacitySpinner, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Create Section");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            int courseIndex = courseCombo.getSelectedIndex();
            if (courseIndex == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a course");
                return;
            }
            
            Course selectedCourse = courses.get(courseIndex);
            String sectionNumber = sectionNumberField.getText().trim();
            String semester = (String) semesterCombo.getSelectedItem();
            int year = (Integer) yearSpinner.getValue();
            String dayOfWeek = dayOfWeekField.getText().trim();
            int startHour = (Integer) startHourSpinner.getValue();
            int startMinute = (Integer) startMinuteSpinner.getValue();
            int endHour = (Integer) endHourSpinner.getValue();
            int endMinute = (Integer) endMinuteSpinner.getValue();
            String room = roomField.getText().trim();
            int capacity = (Integer) capacitySpinner.getValue();
            
            if (sectionNumber.isEmpty() || dayOfWeek.isEmpty() || room.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required");
                return;
            }
            
            createSection(selectedCourse.getCourseId(), sectionNumber, semester, year, 
                         dayOfWeek, startHour, startMinute, endHour, endMinute, room, capacity);
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
    
    private void createSection(Long courseId, String sectionNumber, String semester, int year,
                               String dayOfWeek, int startHour, int startMinute, int endHour, int endMinute,
                               String room, int capacity) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Section section = new Section();
                section.setCourseId(courseId);
                section.setSectionNumber(sectionNumber);
                section.setSemester(semester);
                section.setYear(year);
                section.setDayOfWeek(dayOfWeek);
                section.setStartTime(java.time.LocalTime.of(startHour, startMinute));
                section.setEndTime(java.time.LocalTime.of(endHour, endMinute));
                section.setRoom(room);
                section.setCapacity(capacity);
                section.setEnrolled(0);
                
                sectionDAO.save(section);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Section created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadSections();
                } catch (Exception e) {
                    logger.error("Error creating section", e);
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Error creating section: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void showEditSectionDialog() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section to edit");
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        
        // Disable table during loading
        sectionTable.setEnabled(false);
        
        // Load section details in background thread
        SwingWorker<Section, Void> worker = new SwingWorker<>() {
            @Override
            protected Section doInBackground() throws Exception {
                return sectionDAO.findById(sectionId);
            }
            
            @Override
            protected void done() {
                // Re-enable table
                sectionTable.setEnabled(true);
                
                try {
                    Section section = get();
                    if (section == null) {
                        JOptionPane.showMessageDialog(SectionManagementPanel.this, 
                            "Section not found", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Show dialog on EDT with loaded section
                    showEditDialogWithSection(section);
                    
                } catch (Exception e) {
                    logger.error("Error loading section", e);
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Error loading section: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void showEditDialogWithSection(Section section) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Edit Section", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        JTextField sectionNumberField = new JTextField(section.getSectionNumber(), 10);
        JComboBox<String> semesterCombo = new JComboBox<>(new String[]{
            "Fall", "Spring", "Summer"
        });
        semesterCombo.setSelectedItem(section.getSemester());
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(section.getYear(), 2024, 2030, 1));
        JTextField dayOfWeekField = new JTextField(section.getDayOfWeek() != null ? section.getDayOfWeek() : "", 20);
        
        int startHour = section.getStartTime() != null ? section.getStartTime().getHour() : 10;
        int startMinute = section.getStartTime() != null ? section.getStartTime().getMinute() : 0;
        int endHour = section.getEndTime() != null ? section.getEndTime().getHour() : 11;
        int endMinute = section.getEndTime() != null ? section.getEndTime().getMinute() : 30;
        
        JSpinner startHourSpinner = new JSpinner(new SpinnerNumberModel(startHour, 0, 23, 1));
        JSpinner startMinuteSpinner = new JSpinner(new SpinnerNumberModel(startMinute, 0, 59, 15));
        JSpinner endHourSpinner = new JSpinner(new SpinnerNumberModel(endHour, 0, 23, 1));
        JSpinner endMinuteSpinner = new JSpinner(new SpinnerNumberModel(endMinute, 0, 59, 15));
        JTextField roomField = new JTextField(section.getRoom(), 20);
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(section.getCapacity(), 1, 200, 1));
        
        dialog.add(new JLabel("Section Number:"), "");
        dialog.add(sectionNumberField, "wrap");
        
        dialog.add(new JLabel("Semester:"), "");
        dialog.add(semesterCombo, "wrap");
        
        dialog.add(new JLabel("Year:"), "");
        dialog.add(yearSpinner, "wrap");
        
        dialog.add(new JLabel("Day of Week:"), "");
        dialog.add(dayOfWeekField, "wrap");
        
        dialog.add(new JLabel("Start Time:"), "");
        JPanel startTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        startTimePanel.add(startHourSpinner);
        startTimePanel.add(new JLabel(":"));
        startTimePanel.add(startMinuteSpinner);
        dialog.add(startTimePanel, "wrap");
        
        dialog.add(new JLabel("End Time:"), "");
        JPanel endTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        endTimePanel.add(endHourSpinner);
        endTimePanel.add(new JLabel(":"));
        endTimePanel.add(endMinuteSpinner);
        dialog.add(endTimePanel, "wrap");
        
        dialog.add(new JLabel("Room:"), "");
        dialog.add(roomField, "wrap");
        
        dialog.add(new JLabel("Capacity:"), "");
        dialog.add(capacitySpinner, "wrap");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            // Validate capacity before saving
            int newCapacity = (Integer) capacitySpinner.getValue();
            int currentEnrolled = section.getEnrolled();
            
            if (newCapacity < currentEnrolled) {
                JOptionPane.showMessageDialog(dialog,
                    String.format("Cannot reduce capacity to %d.\n" +
                                  "Section currently has %d enrolled student%s.\n" +
                                  "Capacity must be at least %d.",
                                  newCapacity, 
                                  currentEnrolled,
                                  currentEnrolled == 1 ? "" : "s",
                                  currentEnrolled),
                    "Invalid Capacity",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            section.setSectionNumber(sectionNumberField.getText().trim());
            section.setSemester((String) semesterCombo.getSelectedItem());
            section.setYear((Integer) yearSpinner.getValue());
            section.setDayOfWeek(dayOfWeekField.getText().trim());
            section.setStartTime(java.time.LocalTime.of(
                (Integer) startHourSpinner.getValue(), 
                (Integer) startMinuteSpinner.getValue()));
            section.setEndTime(java.time.LocalTime.of(
                (Integer) endHourSpinner.getValue(), 
                (Integer) endMinuteSpinner.getValue()));
            section.setRoom(roomField.getText().trim());
            section.setCapacity(newCapacity);
            
            updateSection(section);
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
    
    private void updateSection(Section section) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                sectionDAO.update(section);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Section updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadSections();
                } catch (Exception e) {
                    logger.error("Error updating section", e);
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Error updating section: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void deleteSection() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section to delete");
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        String courseCode = (String) tableModel.getValueAt(selectedRow, 1);
        String sectionNumber = (String) tableModel.getValueAt(selectedRow, 3);
        int enrolled = (Integer) tableModel.getValueAt(selectedRow, 9);
        
        if (enrolled > 0) {
            JOptionPane.showMessageDialog(this,
                "Cannot delete section with enrolled students.\n" +
                "Please move students to other sections first.",
                "Cannot Delete",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete section " + courseCode + "-" + sectionNumber + "?\n" +
            "This action cannot be undone.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    sectionDAO.delete(sectionId);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(SectionManagementPanel.this,
                            "Section deleted successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        loadSections();
                    } catch (Exception e) {
                        logger.error("Error deleting section", e);
                        JOptionPane.showMessageDialog(SectionManagementPanel.this,
                            "Error deleting section: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void showAssignInstructorDialog() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section");
            return;
        }
        
        Long sectionId = (Long) tableModel.getValueAt(selectedRow, 0);
        String courseCode = (String) tableModel.getValueAt(selectedRow, 1);
        String sectionNumber = (String) tableModel.getValueAt(selectedRow, 3);
        
        // Load instructors
        List<Instructor> instructors;
        try {
            instructors = instructorDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error loading instructors", e);
            JOptionPane.showMessageDialog(this, "Error loading instructors: " + e.getMessage());
            return;
        }
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, 
            "Assign Instructor to " + courseCode + "-" + sectionNumber, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new MigLayout("fillx", "[right]rel[grow,fill]", ""));
        
        JComboBox<String> instructorCombo = new JComboBox<>();
        instructorCombo.addItem("-- No Instructor --");
        for (Instructor instructor : instructors) {
            instructorCombo.addItem(instructor.getFullName() + " (ID: " + instructor.getInstructorId() + ")");
        }
        
        dialog.add(new JLabel("Select Instructor:"), "");
        dialog.add(instructorCombo, "wrap");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton assignBtn = new JButton("Assign");
        JButton cancelBtn = new JButton("Cancel");
        
        assignBtn.addActionListener(e -> {
            int instructorIndex = instructorCombo.getSelectedIndex();
            Long instructorId = instructorIndex > 0 ? instructors.get(instructorIndex - 1).getInstructorId() : null;
            
            assignInstructor(sectionId, instructorId);
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(assignBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, "span, center, wrap");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void assignInstructor(Long sectionId, Long instructorId) {
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
                    String message = instructorId != null ? 
                        "Instructor assigned successfully!" : 
                        "Instructor unassigned successfully!";
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        message,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadSections();
                } catch (Exception e) {
                    logger.error("Error assigning instructor", e);
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Error assigning instructor: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void showSectionDetails() {
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
                Course course = courseDAO.findById(section.getCourseId());
                Instructor instructor = section.getInstructorId() != null ? 
                    instructorDAO.findById(section.getInstructorId()) : null;
                
                StringBuilder details = new StringBuilder();
                details.append("SECTION DETAILS\n");
                details.append("═══════════════════════════════════════\n\n");
                details.append("Section ID: ").append(section.getSectionId()).append("\n");
                details.append("Course: ").append(course.getCode()).append(" - ").append(course.getTitle()).append("\n");
                details.append("Section Number: ").append(section.getSectionNumber()).append("\n");
                details.append("Semester: ").append(section.getSemester()).append(" ").append(section.getYear()).append("\n");
                details.append("Schedule: ").append(formatSchedule(section)).append("\n");
                details.append("Room: ").append(section.getRoom()).append("\n");
                details.append("Instructor: ").append(instructor != null ? instructor.getFullName() : "Not Assigned").append("\n");
                details.append("\nENROLLMENT\n");
                details.append("───────────────────────────────────────\n");
                details.append("Capacity: ").append(section.getCapacity()).append("\n");
                details.append("Enrolled: ").append(section.getEnrolled()).append("\n");
                details.append("Available Seats: ").append(section.getCapacity() - section.getEnrolled()).append("\n");
                
                // Guard against division by zero
                if (section.getCapacity() <= 0) {
                    logger.warn("Invalid section capacity: {} for section ID: {}", 
                               section.getCapacity(), section.getSectionId());
                    details.append("Fill Rate: N/A (invalid capacity)").append("\n");
                } else {
                    double fillRate = (section.getEnrolled() * 100.0) / section.getCapacity();
                    details.append("Fill Rate: ").append(String.format("%.1f%%", fillRate)).append("\n");
                }
                
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
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        scrollPane,
                        "Section Details",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("Error loading section details", e);
                    JOptionPane.showMessageDialog(SectionManagementPanel.this,
                        "Error loading section details: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
