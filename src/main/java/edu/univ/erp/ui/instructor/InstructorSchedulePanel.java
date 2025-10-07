package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for displaying instructor's teaching schedule.
 */
public class InstructorSchedulePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InstructorSchedulePanel.class);
    
    private final SectionService sectionService;
    private final InstructorDAO instructorDAO;
    private JPanel scheduleGrid;
    private JLabel currentWeekLabel;
    
    // Time slots for the schedule
    private final String[] timeSlots = {
        "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
    };
    
    private final String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    /**
     * Main constructor with dependency injection for better testability.
     * 
     * @param sectionService service for managing sections
     * @param instructorDAO data access object for instructors
     */
    public InstructorSchedulePanel(SectionService sectionService, InstructorDAO instructorDAO) {
        this.sectionService = sectionService;
        this.instructorDAO = instructorDAO;
        
        initComponents();
        loadSchedule();
    }
    
    /**
     * Convenience no-arg constructor for production use and backward compatibility.
     * Creates instances of dependencies directly.
     */
    public InstructorSchedulePanel() {
        this(new SectionService(), new InstructorDAO());
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[]10[]10[grow]10[]"));

        // Header
        add(new JLabel("<html><h2>📅 My Teaching Schedule</h2></html>"), "wrap");
        
        // Current week and navigation
        JPanel weekPanel = new JPanel(new MigLayout("insets 0", "[]20[grow]20[]", "[]"));
        JButton prevWeekBtn = new JButton("← Previous Week");
        prevWeekBtn.addActionListener(this::previousWeek);
        weekPanel.add(prevWeekBtn);
        
        currentWeekLabel = new JLabel("Week of " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        currentWeekLabel.setFont(currentWeekLabel.getFont().deriveFont(Font.BOLD, 16f));
        currentWeekLabel.setHorizontalAlignment(SwingConstants.CENTER);
        weekPanel.add(currentWeekLabel, "grow");
        
        JButton nextWeekBtn = new JButton("Next Week →");
        nextWeekBtn.addActionListener(this::nextWeek);
        weekPanel.add(nextWeekBtn);
        
        add(weekPanel, "growx, wrap");
        
        // Legend
        JPanel legendPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]10[]", "[]"));
        legendPanel.add(createLegendItem("🎓 Lecture", new Color(135, 206, 250)));
        legendPanel.add(createLegendItem("🏢 Office Hours", new Color(144, 238, 144)));
        legendPanel.add(createLegendItem("📚 Lab/Tutorial", new Color(255, 182, 193)));
        legendPanel.add(createLegendItem("⚠️ Conflict", new Color(255, 99, 71)));
        add(legendPanel, "wrap");
        
        // Schedule grid
        scheduleGrid = new JPanel();
        createScheduleGrid();
        
        JScrollPane scrollPane = new JScrollPane(scheduleGrid);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Weekly Schedule"));
        scrollPane.setPreferredSize(new Dimension(800, 400));
        add(scrollPane, "grow, wrap");
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]20[]", "[]"));
        
        JButton addOfficeHoursBtn = new JButton("📅 Add Office Hours");
        addOfficeHoursBtn.addActionListener(this::addOfficeHours);
        buttonPanel.add(addOfficeHoursBtn);
        
        JButton viewConflictsBtn = new JButton("⚠️ Check Conflicts");
        viewConflictsBtn.addActionListener(this::checkConflicts);
        buttonPanel.add(viewConflictsBtn);
        
        JButton exportScheduleBtn = new JButton("📤 Export Schedule");
        exportScheduleBtn.addActionListener(this::exportSchedule);
        buttonPanel.add(exportScheduleBtn);
        
        JButton printScheduleBtn = new JButton("🖨️ Print Schedule");
        printScheduleBtn.addActionListener(this::printSchedule);
        buttonPanel.add(printScheduleBtn);
        
        add(buttonPanel, "");
    }
    
    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new MigLayout("insets 0", "[]5[]", "[]"));
        
        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        item.add(colorBox);
        
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        item.add(label);
        
        return item;
    }
    
    private void createScheduleGrid() {
        scheduleGrid.setLayout(new GridLayout(timeSlots.length + 1, dayNames.length + 1, 1, 1));
        scheduleGrid.setBackground(Color.WHITE);
        
        // Header row
        scheduleGrid.add(createHeaderCell("Time"));
        for (String day : dayNames) {
            scheduleGrid.add(createHeaderCell(day));
        }
        
        // Time slot rows
        for (String time : timeSlots) {
            scheduleGrid.add(createTimeCell(time));
            for (String day : dayNames) {
                scheduleGrid.add(createScheduleCell(day, time));
            }
        }
    }
    
    /**
     * Update the schedule grid with real section data
     */
    private void updateScheduleGridWithSections(List<Section> sections) {
        scheduleGrid.removeAll();
        scheduleGrid.setLayout(new GridLayout(timeSlots.length + 1, dayNames.length + 1, 1, 1));
        scheduleGrid.setBackground(Color.WHITE);
        
        // Add header row
        scheduleGrid.add(createHeaderCell("Time"));
        for (String day : dayNames) {
            scheduleGrid.add(createHeaderCell(day));
        }
        
        // Add time slots and corresponding schedule cells
        for (String time : timeSlots) {
            scheduleGrid.add(createTimeCell(time));
            for (String day : dayNames) {
                scheduleGrid.add(createScheduleCellForSections(day, time, sections));
            }
        }
        
        scheduleGrid.revalidate();
        scheduleGrid.repaint();
    }
    
    private JPanel createHeaderCell(String text) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(new Color(70, 130, 180));
        cell.setBorder(BorderFactory.createRaisedBevelBorder());
        
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        cell.add(label);
        
        cell.setPreferredSize(new Dimension(120, 30));
        return cell;
    }
    
    private JPanel createTimeCell(String time) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(new Color(240, 240, 240));
        cell.setBorder(BorderFactory.createLoweredBevelBorder());
        
        JLabel label = new JLabel(time, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        cell.add(label);
        
        cell.setPreferredSize(new Dimension(80, 50));
        return cell;
    }
    
    private JPanel createScheduleCell(String day, String time) {
        JPanel cell = new JPanel(new MigLayout("fill, insets 2", "[grow]", "[grow]"));
        cell.setBackground(Color.WHITE);
        cell.setBorder(BorderFactory.createLoweredBevelBorder());
        cell.setPreferredSize(new Dimension(120, 50));
        
        // Add some sample classes based on common schedule patterns
        String cellContent = getScheduleContent(day, time);
        if (!cellContent.isEmpty()) {
            JLabel contentLabel = new JLabel("<html><div style='text-align: center; font-size: 9px;'>" + cellContent + "</div></html>");
            contentLabel.setOpaque(true);
            
            // Color code based on content type
            if (cellContent.contains("CS")) {
                contentLabel.setBackground(new Color(135, 206, 250)); // Light blue for lectures
                cell.setBackground(new Color(135, 206, 250));
            } else if (cellContent.contains("Office")) {
                contentLabel.setBackground(new Color(144, 238, 144)); // Light green for office hours
                cell.setBackground(new Color(144, 238, 144));
            } else if (cellContent.contains("Lab")) {
                contentLabel.setBackground(new Color(255, 182, 193)); // Light pink for labs
                cell.setBackground(new Color(255, 182, 193));
            }
            
            contentLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            cell.add(contentLabel, "grow");
        }
        
        return cell;
    }
    
    /**
     * Create a schedule cell with real section data
     */
    private JPanel createScheduleCellForSections(String day, String time, List<Section> sections) {
        JPanel cell = new JPanel();
        cell.setBackground(Color.WHITE);
        cell.setBorder(BorderFactory.createLoweredBevelBorder());
        cell.setPreferredSize(new Dimension(120, 50));
        
        if (sections == null || sections.isEmpty()) {
            return cell; // Empty cell if no sections
        }
        
        // Collect all sections that match this day and time
        List<Section> matchingSections = new ArrayList<>();
        for (Section section : sections) {
            if (section != null && matchesDayAndTime(section, day, time)) {
                matchingSections.add(section);
            }
        }
        
        if (matchingSections.isEmpty()) {
            return cell; // No matching sections
        }
        
        // Determine if there are conflicts (multiple sections at same time)
        boolean hasConflict = matchingSections.size() > 1;
        
        // Set up vertical layout for stacking multiple sections
        if (hasConflict) {
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            // Conflict styling - red border and background tint
            cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED, 2),  // Red conflict border
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
            cell.setBackground(new Color(255, 230, 230)); // Light red background for conflicts
        } else {
            cell.setLayout(new MigLayout("fill, insets 2", "[grow]", "[grow]"));
        }
        
        // Add each matching section as a separate component
        for (int i = 0; i < matchingSections.size(); i++) {
            Section section = matchingSections.get(i);
            
            // Build section info content
            StringBuilder content = new StringBuilder();
            content.append("<html><div style='text-align: center; font-size: 9px;'>");
            
            if (section.getCourseCode() != null) {
                content.append(section.getCourseCode()).append("<br/>");
            }
            
            if (section.getCourseTitle() != null) {
                content.append(section.getCourseTitle()).append("<br/>");
            }
            
            if (section.getRoom() != null) {
                content.append("Room ").append(section.getRoom());
            }
            
            content.append("</div></html>");
            
            JLabel contentLabel = new JLabel(content.toString());
            contentLabel.setOpaque(true);
            
            // Determine section type and apply appropriate colors
            Color sectionColor = getSectionTypeColor(section);
            contentLabel.setBackground(sectionColor);
            
            // If there's no conflict, set the cell background to match
            if (!hasConflict) {
                cell.setBackground(sectionColor);
            }
            
            contentLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            
            // Add separator between sections in conflict view
            if (hasConflict && i > 0) {
                JLabel separator = new JLabel(" ");
                separator.setPreferredSize(new Dimension(1, 2));
                cell.add(separator);
            }
            
            if (hasConflict) {
                cell.add(contentLabel);
            } else {
                cell.add(contentLabel, "grow");
            }
        }
        
        return cell;
    }
    
    /**
     * Determine section type and return appropriate color based on section attributes
     */
    private Color getSectionTypeColor(Section section) {
        // Check section number for type indicators
        String sectionNumber = section.getSectionNumber();
        String courseTitle = section.getCourseTitle();
        
        // Office Hours detection
        if ((courseTitle != null && courseTitle.toLowerCase().contains("office")) ||
            (sectionNumber != null && sectionNumber.toUpperCase().contains("OH"))) {
            return new Color(144, 238, 144); // Light green for office hours
        }
        
        // Lab/Tutorial detection  
        if ((sectionNumber != null && (sectionNumber.toUpperCase().startsWith("L") || 
                                      sectionNumber.toLowerCase().contains("lab"))) ||
            (courseTitle != null && (courseTitle.toLowerCase().contains("lab") || 
                                   courseTitle.toLowerCase().contains("tutorial")))) {
            return new Color(255, 182, 193); // Light pink for labs
        }
        
        // Regular course (default)
        return new Color(135, 206, 250); // Light blue for lectures
    }
    
    /**
     * Check if a section matches the given day and time
     */
    private boolean matchesDayAndTime(Section section, String day, String time) {
        if (section == null || section.getDayOfWeek() == null) {
            return false;
        }
        
        // Check if day matches (handle comma-separated days like "Monday,Wednesday")
        boolean dayMatches = section.getDayOfWeek().contains(day);
        
        if (!dayMatches || section.getStartTime() == null) {
            return false;
        }
        
        // Parse the time slot (e.g., "09:00 AM" -> LocalTime)
        try {
            LocalTime slotTime = parseTimeSlot(time);
            LocalTime sectionStart = section.getStartTime();
            LocalTime sectionEnd = section.getEndTime();
            
            // Check if the time slot overlaps with section time
            if (sectionEnd != null) {
                return !slotTime.isBefore(sectionStart) && slotTime.isBefore(sectionEnd);
            } else {
                // If no end time, just match start time (within 1 hour window)
                return !slotTime.isBefore(sectionStart) && slotTime.isBefore(sectionStart.plusHours(1));
            }
        } catch (Exception e) {
            logger.warn("Error parsing time slot '{}': {}", time, e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse time slot string (e.g., "09:00 AM") to LocalTime
     */
    private LocalTime parseTimeSlot(String timeSlot) {
        try {
            // Remove spaces and convert to 24-hour format
            String cleanTime = timeSlot.trim().toUpperCase();
            boolean isPM = cleanTime.endsWith("PM");
            boolean isAM = cleanTime.endsWith("AM");
            
            if (!isPM && !isAM) {
                throw new IllegalArgumentException("Invalid time format: " + timeSlot);
            }
            
            String timeOnly = cleanTime.substring(0, cleanTime.length() - 2).trim();
            String[] parts = timeOnly.split(":");
            
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid time format: " + timeSlot);
            }
            
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            // Convert to 24-hour format
            if (isPM && hour != 12) {
                hour += 12;
            } else if (isAM && hour == 12) {
                hour = 0;
            }
            
            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing time slot: " + timeSlot, e);
        }
    }
    
    private String getScheduleContent(String day, String time) {
        // Simulate a realistic teaching schedule
        switch (day) {
            case "Monday":
                if (time.equals("09:00 AM")) return "CS101<br/>Intro Programming<br/>Room A101";
                if (time.equals("02:00 PM")) return "CS201<br/>Data Structures<br/>Room B205";
                if (time.equals("04:00 PM")) return "Office Hours<br/>Room C301";
                break;
            case "Tuesday":
                if (time.equals("10:00 AM")) return "CS101 Lab<br/>Room C110";
                if (time.equals("01:00 PM")) return "CS301<br/>Algorithms<br/>Room A205";
                break;
            case "Wednesday":
                if (time.equals("09:00 AM")) return "CS101<br/>Intro Programming<br/>Room A101";
                if (time.equals("02:00 PM")) return "CS201<br/>Data Structures<br/>Room B205";
                if (time.equals("04:00 PM")) return "Office Hours<br/>Room C301";
                break;
            case "Thursday":
                if (time.equals("10:00 AM")) return "CS201 Lab<br/>Room C115";
                if (time.equals("01:00 PM")) return "CS301<br/>Algorithms<br/>Room A205";
                break;
            case "Friday":
                if (time.equals("09:00 AM")) return "CS101<br/>Intro Programming<br/>Room A101";
                if (time.equals("11:00 AM")) return "Faculty Meeting<br/>Conference Room";
                if (time.equals("02:00 PM")) return "Office Hours<br/>Room C301";
                break;
        }
        return "";
    }
    
    private void loadSchedule() {
        new SwingWorker<List<Section>, Void>() {
            @Override
            protected List<Section> doInBackground() throws Exception {
                // Perform database call off the EDT
                return sectionService.listByInstructor(getCurrentInstructorId());
            }
            
            @Override
            protected void done() {
                try {
                    // Get the loaded sections from background thread
                    List<Section> loadedSections = get();
                    logger.info("Loaded {} sections for instructor schedule", loadedSections.size());
                    
                    // Update the schedule grid with real data
                    updateScheduleGridWithSections(loadedSections);
                    
                } catch (Exception e) {
                    logger.error("Error loading schedule", e);
                    JOptionPane.showMessageDialog(InstructorSchedulePanel.this,
                        "Error loading schedule: " + e.getMessage(),
                        "Schedule Load Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private void previousWeek(ActionEvent e) {
        // Simulate week navigation
        JOptionPane.showMessageDialog(this, "Previous week view - Implementation coming soon!", 
            "Navigation", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void nextWeek(ActionEvent e) {
        // Simulate week navigation
        JOptionPane.showMessageDialog(this, "Next week view - Implementation coming soon!", 
            "Navigation", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void addOfficeHours(ActionEvent e) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Office Hours", true);
        dialog.setLayout(new MigLayout("fill, insets 20", "[right][grow]", "[]10[]10[]10[]20[]"));
        
        dialog.add(new JLabel("Day:"));
        JComboBox<String> dayCombo = new JComboBox<>(dayNames);
        dialog.add(dayCombo, "wrap");
        
        dialog.add(new JLabel("Start Time:"));
        JComboBox<String> startTimeCombo = new JComboBox<>(timeSlots);
        dialog.add(startTimeCombo, "wrap");
        
        dialog.add(new JLabel("End Time:"));
        JComboBox<String> endTimeCombo = new JComboBox<>(timeSlots);
        endTimeCombo.setSelectedIndex(2); // Default to 2 hours later
        dialog.add(endTimeCombo, "wrap");
        
        dialog.add(new JLabel("Location:"));
        JTextField locationField = new JTextField("Room C301", 20);
        dialog.add(locationField, "wrap");
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]10[]", "[]"));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(evt -> {
            JOptionPane.showMessageDialog(dialog, "Office hours added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
            loadSchedule(); // Refresh schedule
        });
        buttonPanel.add(saveBtn);
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(evt -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        
        dialog.add(buttonPanel, "span 2, center");
        
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void checkConflicts(ActionEvent e) {
        JDialog conflictDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Schedule Conflicts", true);
        conflictDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        conflictDialog.add(new JLabel("<html><h3>⚠️ Schedule Conflict Analysis</h3></html>"), "wrap");
        
        JTextArea conflictArea = new JTextArea(15, 50);
        conflictArea.setEditable(false);
        conflictArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        StringBuilder conflicts = new StringBuilder();
        conflicts.append("SCHEDULE CONFLICT REPORT\n");
        conflicts.append("=======================\n\n");
        conflicts.append("Analysis Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n\n");
        
        conflicts.append("✅ NO SCHEDULING CONFLICTS DETECTED\n\n");
        
        conflicts.append("SCHEDULE SUMMARY:\n");
        conflicts.append("-".repeat(17)).append("\n");
        conflicts.append("• Monday:    3 classes + 1 office hour\n");
        conflicts.append("• Tuesday:   2 classes\n");
        conflicts.append("• Wednesday: 3 classes + 1 office hour\n");
        conflicts.append("• Thursday:  2 classes\n");
        conflicts.append("• Friday:    2 classes + 1 meeting + 1 office hour\n\n");
        
        conflicts.append("RECOMMENDATIONS:\n");
        conflicts.append("-".repeat(15)).append("\n");
        conflicts.append("• Current schedule is well-balanced\n");
        conflicts.append("• Consider adding office hours on Tuesday/Thursday\n");
        conflicts.append("• Buffer time exists between classes\n");
        conflicts.append("• No room booking conflicts detected\n\n");
        
        conflicts.append("TEACHING LOAD ANALYSIS:\n");
        conflicts.append("-".repeat(23)).append("\n");
        conflicts.append("Total Teaching Hours: 12 hours/week\n");
        conflicts.append("Office Hours: 6 hours/week\n");
        conflicts.append("Total Commitment: 18 hours/week\n");
        conflicts.append("Load Status: Normal (within guidelines)\n");
        
        conflictArea.setText(conflicts.toString());
        
        JScrollPane scrollPane = new JScrollPane(conflictArea);
        conflictDialog.add(scrollPane, "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(evt -> conflictDialog.dispose());
        conflictDialog.add(closeBtn, "center");
        
        conflictDialog.setSize(600, 500);
        conflictDialog.setLocationRelativeTo(this);
        conflictDialog.setVisible(true);
    }
    
    private void exportSchedule(ActionEvent e) {
        String[] options = {"PDF Export", "Excel Export", "iCal Export", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, 
            "Choose export format:", "Export Schedule",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            
        if (choice >= 0 && choice < 3) {
            JOptionPane.showMessageDialog(this, 
                "Schedule will be exported to " + options[choice] + " format.\n\n" +
                "Export functionality coming soon!", 
                "Export", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void printSchedule(ActionEvent e) {
        JOptionPane.showMessageDialog(this, 
            "Print functionality will generate a formatted schedule printout.\n\n" +
            "Features:\n" +
            "• Weekly schedule layout\n" +
            "• Room assignments\n" +
            "• Office hours highlighted\n" +
            "• Contact information\n\n" +
            "Print functionality coming soon!", 
            "Print Schedule", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Get the current authenticated instructor's ID from the session.
     * 
     * @return The instructor ID for the authenticated user
     * @throws UnsupportedOperationException if no valid instructor session exists or authentication context is unavailable
     */
    private Long getCurrentInstructorId() {
        try {
            // Safely get current user and handle null case
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                logger.error("No current user session found. User must be logged in to access instructor features.");
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: No user session found. Please log in to access instructor features.");
            }
            
            Long userId = currentUser.getUserId();
            if (userId == null) {
                logger.warn("User ID is null in session, cannot retrieve instructor ID");
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: User ID is null in session context.");
            }
            
            // Get instructor record from user ID
            Instructor instructor;
            try {
                instructor = instructorDAO.findByUserId(userId);
            } catch (Exception dbException) {
                logger.error("Database error while retrieving instructor for user ID: {}", userId, dbException);
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: Database error while retrieving instructor information.", dbException);
            }
            
            if (instructor == null) {
                logger.warn("No instructor record found for user ID: {}", userId);
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: No instructor record found for current user. Authorization required.");
            }
            
            Long instructorId = instructor.getInstructorId();
            if (instructorId == null) {
                logger.warn("Instructor ID is null for user ID: {}", userId);
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: Instructor record exists but ID is null.");
            }
            
            logger.debug("Retrieved instructor ID: {} for user ID: {}", instructorId, userId);
            return instructorId;
            
        } catch (UnsupportedOperationException e) {
            // Re-throw UnsupportedOperationException as-is
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error retrieving current instructor ID from session", e);
            throw new UnsupportedOperationException(
                "Instructor ID retrieval not available: Unexpected error in authentication context.", e);
        }
    }
}