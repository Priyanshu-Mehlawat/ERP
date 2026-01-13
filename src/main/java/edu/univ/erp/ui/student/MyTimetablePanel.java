package edu.univ.erp.ui.student;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.domain.Section;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Panel to display student's weekly class timetable.
 */
public class MyTimetablePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MyTimetablePanel.class);

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final SectionService sectionService = new SectionService();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JPanel schedulePanel;
    private JLabel statusLabel;
    private Student currentStudent;

    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final String[] TIME_SLOTS = {"08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};

    public MyTimetablePanel() {
        loadCurrentStudent();
        initComponents();
        loadTimetable();
    }

    private void loadCurrentStudent() {
        try {
            Long userId = SessionManager.getInstance().getCurrentUser().getUserId();
            if (userId == null) {
                logger.warn("User ID is null, cannot load student data");
                return;
            }
            currentStudent = studentDAO.findByUserId(userId);
            if (currentStudent == null) {
                logger.warn("No student found for user ID: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error loading current student", e);
        }
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][grow][]"));

        // Header
        add(new JLabel("<html><h2>My Timetable - Fall 2025</h2></html>"), "wrap");

        // Schedule grid
        schedulePanel = new JPanel();
        schedulePanel.setLayout(new GridLayout(TIME_SLOTS.length + 1, DAYS.length + 1, 2, 2));
        schedulePanel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        buildScheduleGrid();
        
        JScrollPane scrollPane = new JScrollPane(schedulePanel);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        add(scrollPane, "grow, wrap");

        // Status and controls
        JPanel bottomPanel = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
        statusLabel = new JLabel("Loading...");
        bottomPanel.add(statusLabel, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadTimetable());
        bottomPanel.add(refreshBtn);
        
        add(bottomPanel, "growx");
    }

    private void buildScheduleGrid() {
        schedulePanel.removeAll();
        
        // Header row
        schedulePanel.add(new JLabel("Time", SwingConstants.CENTER));
        for (String day : DAYS) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
            schedulePanel.add(dayLabel);
        }

        // Time slot rows
        for (String timeSlot : TIME_SLOTS) {
            JLabel timeLabel = new JLabel(timeSlot, SwingConstants.CENTER);
            timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
            schedulePanel.add(timeLabel);
            
            for (String day : DAYS) {
                JPanel cellPanel = new JPanel();
                cellPanel.setBorder(BorderFactory.createEtchedBorder());
                cellPanel.setBackground(Color.WHITE);
                cellPanel.setName(day + "_" + timeSlot); // For lookup
                schedulePanel.add(cellPanel);
            }
        }
    }

    private void loadTimetable() {
        if (currentStudent == null) {
            statusLabel.setText("Student data not available");
            return;
        }

        SwingWorker<List<TimetableEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TimetableEntry> doInBackground() {
                List<TimetableEntry> entries = new ArrayList<>();
                try {
                    List<Enrollment> enrollments = enrollmentService.listByStudent(currentStudent.getStudentId());
                    for (Enrollment enrollment : enrollments) {
                        if ("ENROLLED".equals(enrollment.getStatus())) {
                            Section section = sectionService.get(enrollment.getSectionId());
                            if (section != null && section.getDayOfWeek() != null && 
                                section.getStartTime() != null && section.getEndTime() != null) {
                                
                                TimetableEntry entry = new TimetableEntry();
                                entry.courseCode = enrollment.getCourseCode();
                                entry.courseTitle = enrollment.getCourseTitle();
                                // Section number available in enrollment.getSectionNumber() if needed
                                entry.instructor = enrollment.getInstructorName();
                                entry.room = section.getRoom();
                                entry.dayOfWeek = section.getDayOfWeek();
                                entry.startTime = section.getStartTime();
                                entry.endTime = section.getEndTime();
                                
                                entries.add(entry);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error loading timetable", e);
                }
                return entries;
            }

            @Override
            protected void done() {
                try {
                    List<TimetableEntry> entries = get();
                    buildScheduleGrid(); // Clear previous data
                    
                    for (TimetableEntry entry : entries) {
                        placeEntryOnGrid(entry);
                    }
                    
                    schedulePanel.revalidate();
                    schedulePanel.repaint();
                    
                    statusLabel.setText("Showing " + entries.size() + " scheduled classes");
                } catch (Exception e) {
                    logger.error("Failed to load timetable", e);
                    statusLabel.setText("Error loading timetable: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void placeEntryOnGrid(TimetableEntry entry) {
        String[] days = entry.dayOfWeek.split(",");
        for (String day : days) {
            day = day.trim();
            if (isDayValid(day)) {
                String timeSlot = findTimeSlot(entry.startTime);
                if (timeSlot != null) {
                    JPanel cell = findCellPanel(day + "_" + timeSlot);
                    if (cell != null) {
                        cell.removeAll();
                        cell.setLayout(new MigLayout("insets 2", "[grow]", "[][]"));
                        
                        JLabel courseLabel = new JLabel("<html><b>" + entry.courseCode + "</b></html>");
                        courseLabel.setFont(courseLabel.getFont().deriveFont(Font.BOLD, 10f));
                        cell.add(courseLabel, "wrap");
                        
                        String timeStr = entry.startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + 
                                        "-" + entry.endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                        JLabel timeLabel = new JLabel(timeStr);
                        timeLabel.setFont(timeLabel.getFont().deriveFont(9f));
                        cell.add(timeLabel, "wrap");
                        
                        if (entry.room != null) {
                            JLabel roomLabel = new JLabel(entry.room);
                            roomLabel.setFont(roomLabel.getFont().deriveFont(9f));
                            roomLabel.setForeground(Color.BLUE);
                            cell.add(roomLabel);
                        }
                        
                        cell.setBackground(new Color(200, 230, 255));
                        cell.setToolTipText(entry.courseCode + " - " + entry.courseTitle + 
                                          (entry.instructor != null ? " (" + entry.instructor + ")" : ""));
                    }
                }
            }
        }
    }

    private boolean isDayValid(String day) {
        for (String validDay : DAYS) {
            if (validDay.equalsIgnoreCase(day)) return true;
        }
        return false;
    }

    private String findTimeSlot(LocalTime startTime) {
        int hour = startTime.getHour();
        for (String slot : TIME_SLOTS) {
            int slotHour = Integer.parseInt(slot.split(":")[0]);
            if (slotHour == hour) return slot;
        }
        return null;
    }

    private JPanel findCellPanel(String name) {
        for (Component comp : schedulePanel.getComponents()) {
            if (comp instanceof JPanel && name.equals(comp.getName())) {
                return (JPanel) comp;
            }
        }
        return null;
    }

    // Helper class for timetable entries
    private static class TimetableEntry {
        String courseCode;
        String courseTitle;
        String instructor;
        String room;
        String dayOfWeek;
        LocalTime startTime;
        LocalTime endTime;
    }
}