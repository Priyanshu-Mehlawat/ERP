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
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private LocalDate currentWeekStart;
    private List<Section> loadedSections;
    
    // Time slots for the schedule
    private final String[] timeSlots = {
        "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
    };
    
    private final String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    /**
     * Main constructor with dependency injection for better testability.
     */
    public InstructorSchedulePanel(SectionService sectionService, InstructorDAO instructorDAO) {
        this.sectionService = sectionService;
        this.instructorDAO = instructorDAO;
        this.currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        this.loadedSections = new ArrayList<>();
        
        initComponents();
        loadSchedule();
    }
    
    /**
     * Convenience no-arg constructor for production use and backward compatibility.
     */
    public InstructorSchedulePanel() {
        this(new SectionService(), new InstructorDAO());
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[]10[]10[grow]10[]"));

        // Header
        add(new JLabel("<html><h2>📅 My Teaching Schedule</h2></html>"), "wrap");
        
        // Current week label
        currentWeekLabel = new JLabel("Week of " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        currentWeekLabel.setFont(currentWeekLabel.getFont().deriveFont(Font.BOLD, 16f));
        currentWeekLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(currentWeekLabel, "growx, wrap");
        
        // Legend
        JPanel legendPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]", "[]"));
        legendPanel.add(createLegendItem("🎓 Lecture", new Color(135, 206, 250)));
        legendPanel.add(createLegendItem("🏢 Office Hours", new Color(144, 238, 144)));
        legendPanel.add(createLegendItem("📚 Lab/Tutorial", new Color(255, 182, 193)));
        add(legendPanel, "wrap");
        
        // Schedule grid
        scheduleGrid = new JPanel();
        createScheduleGrid();
        
        JScrollPane scrollPane = new JScrollPane(scheduleGrid);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Weekly Schedule"));
        scrollPane.setPreferredSize(new Dimension(800, 400));
        add(scrollPane, "grow, wrap");
        
        // Action buttons - only Print Schedule
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]", "[]"));
        
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
            return cell;
        }
        
        // Collect all sections that match this day and time
        List<Section> matchingSections = new ArrayList<>();
        for (Section section : sections) {
            if (section != null && matchesDayAndTime(section, day, time)) {
                matchingSections.add(section);
            }
        }
        
        if (matchingSections.isEmpty()) {
            return cell;
        }
        
        // Set up layout
        cell.setLayout(new MigLayout("fill, insets 2", "[grow]", "[grow]"));
        
        // Use first matching section
        Section section = matchingSections.get(0);
        
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
        cell.setBackground(sectionColor);
        
        contentLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        cell.add(contentLabel, "grow");
        
        return cell;
    }
    
    /**
     * Determine section type and return appropriate color
     */
    private Color getSectionTypeColor(Section section) {
        String sectionNumber = section.getSectionNumber();
        String courseTitle = section.getCourseTitle();
        
        // Office Hours detection
        if ((courseTitle != null && courseTitle.toLowerCase().contains("office")) ||
            (sectionNumber != null && sectionNumber.toUpperCase().contains("OH"))) {
            return new Color(144, 238, 144); // Light green
        }
        
        // Lab/Tutorial detection  
        if ((sectionNumber != null && (sectionNumber.toUpperCase().startsWith("L") || 
                                      sectionNumber.toLowerCase().contains("lab"))) ||
            (courseTitle != null && (courseTitle.toLowerCase().contains("lab") || 
                                   courseTitle.toLowerCase().contains("tutorial")))) {
            return new Color(255, 182, 193); // Light pink
        }
        
        // Regular course (default)
        return new Color(135, 206, 250); // Light blue
    }
    
    /**
     * Check if a section matches the given day and time
     */
    private boolean matchesDayAndTime(Section section, String day, String time) {
        if (section == null || section.getDayOfWeek() == null) {
            return false;
        }
        
        boolean dayMatches = section.getDayOfWeek().contains(day);
        
        if (!dayMatches || section.getStartTime() == null) {
            return false;
        }
        
        try {
            LocalTime slotTime = parseTimeSlot(time);
            LocalTime sectionStart = section.getStartTime();
            LocalTime sectionEnd = section.getEndTime();
            
            if (sectionEnd != null) {
                return !slotTime.isBefore(sectionStart) && slotTime.isBefore(sectionEnd);
            } else {
                return !slotTime.isBefore(sectionStart) && slotTime.isBefore(sectionStart.plusHours(1));
            }
        } catch (Exception e) {
            logger.warn("Error parsing time slot '{}': {}", time, e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse time slot string to LocalTime
     */
    private LocalTime parseTimeSlot(String timeSlot) {
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
        
        if (isPM && hour != 12) {
            hour += 12;
        } else if (isAM && hour == 12) {
            hour = 0;
        }
        
        return LocalTime.of(hour, minute);
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
                return sectionService.listByInstructor(getCurrentInstructorId());
            }
            
            @Override
            protected void done() {
                try {
                    loadedSections = get();
                    logger.info("Loaded {} sections for instructor schedule", loadedSections.size());
                    
                    updateWeekLabel();
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
    
    private void updateWeekLabel() {
        LocalDate weekEnd = currentWeekStart.plusDays(4);
        String weekText = String.format("Week of %s - %s", 
            currentWeekStart.format(DateTimeFormatter.ofPattern("MMM dd")),
            weekEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        currentWeekLabel.setText(weekText);
    }
    
    private void printSchedule(ActionEvent e) {
        StringBuilder content = new StringBuilder();
        content.append("TEACHING SCHEDULE\n");
        content.append("=================\n\n");
        content.append("Week of: ").append(currentWeekStart.format(
            DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append("\n");
        content.append("Generated: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n\n");
        
        // Build schedule table
        content.append(String.format("%-12s", "Time"));
        for (String day : dayNames) {
            content.append(String.format("%-18s", day));
        }
        content.append("\n");
        content.append("-".repeat(102)).append("\n");
        
        for (String time : timeSlots) {
            content.append(String.format("%-12s", time));
            
            for (String day : dayNames) {
                String cellContent = "";
                
                if (loadedSections != null) {
                    for (Section section : loadedSections) {
                        if (matchesDayAndTime(section, day, time)) {
                            cellContent = section.getCourseCode() != null ? 
                                section.getCourseCode() : "Class";
                            break;
                        }
                    }
                }
                
                content.append(String.format("%-18s", cellContent));
            }
            content.append("\n");
        }
        
        content.append("\n\nLEGEND:\n");
        content.append("• Course codes represent lectures\n");
        content.append("• Office Hours are open consultation times\n");
        
        // Save directly to file
        String userHome = System.getProperty("user.home");
        String fileName = "Teaching_Schedule_" + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        File file = new File(userHome, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(content.toString());
            JOptionPane.showMessageDialog(this,
                "Schedule saved to:\n" + file.getAbsolutePath(),
                "Schedule Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            logger.error("Error saving schedule", ex);
            JOptionPane.showMessageDialog(this,
                "Error saving schedule: " + ex.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Get the current authenticated instructor's ID from the session.
     */
    private Long getCurrentInstructorId() {
        try {
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                logger.error("No current user session found.");
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: No user session found.");
            }
            
            Long userId = currentUser.getUserId();
            if (userId == null) {
                logger.warn("User ID is null in session");
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: User ID is null.");
            }
            
            Instructor instructor = instructorDAO.findByUserId(userId);
            
            if (instructor == null) {
                logger.warn("No instructor record found for user ID: {}", userId);
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: No instructor record found.");
            }
            
            Long instructorId = instructor.getInstructorId();
            if (instructorId == null) {
                logger.warn("Instructor ID is null for user ID: {}", userId);
                throw new UnsupportedOperationException(
                    "Instructor ID retrieval not available: Instructor ID is null.");
            }
            
            logger.debug("Retrieved instructor ID: {} for user ID: {}", instructorId, userId);
            return instructorId;
            
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error retrieving current instructor ID", e);
            throw new UnsupportedOperationException(
                "Instructor ID retrieval not available: Unexpected error.", e);
        }
    }
}
