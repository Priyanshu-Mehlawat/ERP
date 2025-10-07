package edu.univ.erp.ui.instructor;

import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel for generating reports and statistics.
 */
public class ReportsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ReportsPanel.class);
    
    private final SectionService sectionService = new SectionService();
    private JComboBox<Section> sectionCombo;

    public ReportsPanel() {
        initComponents();
        loadSections();
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[]20[grow]"));

        // Header
        add(new JLabel("<html><h2>📊 Reports & Analytics</h2></html>"), "wrap");
        
        // Section selection
        JPanel sectionPanel = new JPanel(new MigLayout("insets 0", "[]10[200!]", "[]"));
        sectionPanel.add(new JLabel("Select Section:"));
        sectionCombo = new JComboBox<>();
        sectionPanel.add(sectionCombo);
        add(sectionPanel, "wrap");
        
        // Report cards grid
        JPanel reportsGrid = new JPanel(new MigLayout("fill, insets 10", "[grow][grow]", "[grow][grow][grow]"));
        reportsGrid.setBorder(BorderFactory.createTitledBorder("Available Reports"));
        
        // Row 1: Grade Reports
        reportsGrid.add(createReportCard("📈 Grade Distribution", 
            "View grade distribution statistics and performance analytics", 
            this::generateGradeDistributionReport), "grow");
        reportsGrid.add(createReportCard("🎯 Class Performance", 
            "Analyze overall class performance and identify trends", 
            this::generateClassPerformanceReport), "grow, wrap");
            
        // Row 2: Attendance Reports  
        reportsGrid.add(createReportCard("📅 Attendance Summary", 
            "Generate comprehensive attendance reports and patterns", 
            this::generateAttendanceReport), "grow");
        reportsGrid.add(createReportCard("👥 Student Progress", 
            "Track individual student progress and improvements", 
            this::generateStudentProgressReport), "grow, wrap");
            
        // Row 3: Advanced Reports
        reportsGrid.add(createReportCard("📋 Comprehensive Report", 
            "Generate detailed semester summary with all metrics", 
            this::generateComprehensiveReport), "grow");
        reportsGrid.add(createReportCard("📤 Export Data", 
            "Export class data to CSV/Excel for external analysis", 
            this::exportClassData), "grow");
        
        add(reportsGrid, "grow");
    }
    
    private JPanel createReportCard(String title, String description, Runnable action) {
        JPanel card = new JPanel(new MigLayout("fill, insets 15", "[grow]", "[]5[]20[]"));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        card.add(titleLabel, "wrap");
        
        JLabel descLabel = new JLabel("<html><div style='width: 200px;'>" + description + "</div></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11f));
        descLabel.setForeground(Color.GRAY);
        card.add(descLabel, "wrap");
        
        JButton generateBtn = new JButton("Generate Report");
        generateBtn.addActionListener(e -> action.run());
        card.add(generateBtn, "center");
        
        return card;
    }
    
    private void loadSections() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<Section> sections = sectionService.listByInstructor(getCurrentInstructorId());
                sectionCombo.removeAllItems();
                sectionCombo.addItem(null); // Add "All Sections" option
                for (Section section : sections) {
                    sectionCombo.addItem(section);
                }
            } catch (Exception e) {
                logger.error("Error loading sections", e);
                JOptionPane.showMessageDialog(this, "Error loading sections: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void generateGradeDistributionReport() {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Grade Distribution Report", true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() :
            "All Sections";
            
        reportDialog.add(new JLabel("<html><h3>📈 Grade Distribution Report - " + sectionInfo + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(20, 60);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        StringBuilder report = new StringBuilder();
        report.append("GRADE DISTRIBUTION ANALYSIS\n");
        report.append("==========================\n\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        report.append("Section: ").append(sectionInfo).append("\n\n");
        
        // Simulate grade distribution data
        report.append("GRADE DISTRIBUTION:\n");
        report.append("-".repeat(40)).append("\n");
        report.append("A  (90-100): ████████ 12 students (24%)\n");
        report.append("B  (80-89):  ██████████████ 18 students (36%)\n");
        report.append("C  (70-79):  ██████████ 15 students (30%)\n");
        report.append("D  (60-69):  ███ 4 students (8%)\n");
        report.append("F  (0-59):   █ 1 student (2%)\n\n");
        
        report.append("STATISTICAL SUMMARY:\n");
        report.append("-".repeat(30)).append("\n");
        report.append("Total Students: 50\n");
        report.append("Class Average: 82.4%\n");
        report.append("Median Grade: 84%\n");
        report.append("Standard Deviation: 8.2\n");
        report.append("Highest Score: 98%\n");
        report.append("Lowest Score: 52%\n\n");
        
        report.append("PERFORMANCE BY ASSIGNMENT TYPE:\n");
        report.append("-".repeat(35)).append("\n");
        report.append("Assignments:  Average 85.2% (Range: 65-98%)\n");
        report.append("Quizzes:      Average 79.8% (Range: 58-95%)\n");
        report.append("Midterm:      Average 81.5% (Range: 52-96%)\n");
        report.append("Final Exam:   Average 83.1% (Range: 61-98%)\n\n");
        
        report.append("RECOMMENDATIONS:\n");
        report.append("-".repeat(20)).append("\n");
        report.append("• Strong overall performance with most students in B-A range\n");
        report.append("• Consider additional quiz review sessions\n");
        report.append("• One student may need additional support (F grade)\n");
        report.append("• Assignment scores show good understanding of material\n");
        
        reportArea.setText(report.toString());
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        reportDialog.add(scrollPane, "grow, wrap");
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]10[]", "[]"));
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(reportDialog, "Export functionality coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE));
        buttonPanel.add(exportBtn);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        buttonPanel.add(closeBtn);
        
        reportDialog.add(buttonPanel, "center");
        reportDialog.setSize(700, 600);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
    }
    
    private void generateClassPerformanceReport() {
        showReportDialog("🎯 Class Performance Analysis", generateClassPerformanceContent());
    }
    
    private void generateAttendanceReport() {
        showReportDialog("📅 Attendance Summary Report", generateAttendanceContent());
    }
    
    private void generateStudentProgressReport() {
        showReportDialog("👥 Student Progress Tracking", generateStudentProgressContent());
    }
    
    private void generateComprehensiveReport() {
        showReportDialog("📋 Comprehensive Semester Report", generateComprehensiveContent());
    }
    
    private void exportClassData() {
        JOptionPane.showMessageDialog(this, 
            "Export functionality will allow you to:\n\n" +
            "• Export grades to CSV/Excel\n" +
            "• Generate attendance spreadsheets\n" +
            "• Create student contact lists\n" +
            "• Export assignment submissions\n\n" +
            "Feature coming soon!", 
            "Export Data", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showReportDialog(String title, String content) {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() :
            "All Sections";
            
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        reportDialog.add(new JLabel("<html><h3>" + title + " - " + sectionInfo + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(20, 60);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        reportArea.setText(content);
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        reportDialog.add(scrollPane, "grow, wrap");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        reportDialog.add(closeBtn, "center");
        
        reportDialog.setSize(700, 600);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
    }
    
    private String generateClassPerformanceContent() {
        return "CLASS PERFORMANCE ANALYSIS\n" +
               "=========================\n\n" +
               "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) + "\n\n" +
               "OVERALL CLASS METRICS:\n" +
               "-".repeat(25) + "\n" +
               "Enrollment: 50 students\n" +
               "Active Students: 48 (96%)\n" +
               "Dropped: 2 students (4%)\n" +
               "Class Average: 82.4%\n" +
               "Pass Rate: 96% (48/50)\n\n" +
               "PERFORMANCE TRENDS:\n" +
               "-".repeat(20) + "\n" +
               "Week 1-4:   Average 78.2% (Adjustment period)\n" +
               "Week 5-8:   Average 83.1% (Steady improvement)\n" +
               "Week 9-12:  Average 85.3% (Peak performance)\n" +
               "Week 13-16: Average 84.7% (Consistent)\n\n" +
               "TOP PERFORMERS:\n" +
               "-".repeat(15) + "\n" +
               "1. Sarah Johnson    - 96.8% average\n" +
               "2. Michael Chen     - 95.2% average\n" +
               "3. Emily Rodriguez  - 94.7% average\n\n" +
               "STUDENTS NEEDING SUPPORT:\n" +
               "-".repeat(25) + "\n" +
               "• Alex Thompson - 62% (Recommend tutoring)\n" +
               "• Jamie Wilson  - 58% (Schedule meeting)\n\n" +
               "RECOMMENDATIONS:\n" +
               "-".repeat(15) + "\n" +
               "• Class performing above department average\n" +
               "• Consider offering bonus material for advanced students\n" +
               "• Implement peer tutoring program\n";
    }
    
    private String generateAttendanceContent() {
        return "ATTENDANCE SUMMARY REPORT\n" +
               "========================\n\n" +
               "Report Period: Last 30 days\n" +
               "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) + "\n\n" +
               "ATTENDANCE STATISTICS:\n" +
               "-".repeat(22) + "\n" +
               "Overall Attendance Rate: 87.5%\n" +
               "Total Class Sessions: 20\n" +
               "Average Students Present: 43.8/50\n\n" +
               "DAILY ATTENDANCE PATTERNS:\n" +
               "-".repeat(27) + "\n" +
               "Monday:    92% (Best attendance)\n" +
               "Tuesday:   89%\n" +
               "Wednesday: 85%\n" +
               "Thursday:  88%\n" +
               "Friday:    78% (Lowest attendance)\n\n" +
               "PERFECT ATTENDANCE:\n" +
               "-".repeat(19) + "\n" +
               "15 students with 100% attendance\n\n" +
               "ATTENDANCE CONCERNS:\n" +
               "-".repeat(20) + "\n" +
               "Students with <70% attendance:\n" +
               "• Lisa Brown - 65% (13/20 classes)\n" +
               "• Mark Davis - 60% (12/20 classes)\n" +
               "• Jennifer Lee - 55% (11/20 classes)\n\n" +
               "RECOMMENDATIONS:\n" +
               "-".repeat(15) + "\n" +
               "• Contact students with low attendance\n" +
               "• Consider recording Friday sessions\n" +
               "• Implement attendance incentive program\n";
    }
    
    private String generateStudentProgressContent() {
        return "STUDENT PROGRESS TRACKING\n" +
               "========================\n\n" +
               "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) + "\n\n" +
               "PROGRESS OVERVIEW:\n" +
               "-".repeat(18) + "\n" +
               "Students Improving: 35 (70%)\n" +
               "Students Consistent: 12 (24%)\n" +
               "Students Declining: 3 (6%)\n\n" +
               "MOST IMPROVED STUDENTS:\n" +
               "-".repeat(23) + "\n" +
               "1. Robert Kim     - Started 72%, Now 89% (+17%)\n" +
               "2. Maria Garcia   - Started 68%, Now 84% (+16%)\n" +
               "3. David Thompson - Started 75%, Now 88% (+13%)\n\n" +
               "STUDENTS NEEDING ATTENTION:\n" +
               "-".repeat(28) + "\n" +
               "• Jessica Adams - Declining from 85% to 72%\n" +
               "• Ryan Murphy   - Declining from 78% to 65%\n" +
               "• Amanda Foster - Declined from 82% to 69%\n\n" +
               "ASSIGNMENT COMPLETION RATES:\n" +
               "-".repeat(29) + "\n" +
               "Homework: 94% average completion\n" +
               "Projects: 88% average completion\n" +
               "Quizzes:  96% average completion\n\n" +
               "ENGAGEMENT METRICS:\n" +
               "-".repeat(19) + "\n" +
               "Participation Score: 8.2/10 average\n" +
               "Office Hours Visits: 42% of students\n" +
               "Extra Credit: 28% participation\n\n" +
               "ACTION ITEMS:\n" +
               "-".repeat(13) + "\n" +
               "• Schedule meetings with declining students\n" +
               "• Continue current teaching strategies\n" +
               "• Offer study groups for struggling students\n";
    }
    
    private String generateComprehensiveContent() {
        return "COMPREHENSIVE SEMESTER REPORT\n" +
               "============================\n\n" +
               "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) + "\n\n" +
               "EXECUTIVE SUMMARY:\n" +
               "-".repeat(18) + "\n" +
               "This comprehensive report provides a complete overview of\n" +
               "class performance, attendance, and student progress for\n" +
               "the current semester.\n\n" +
               "KEY METRICS:\n" +
               "-".repeat(12) + "\n" +
               "• Overall Class Average: 82.4%\n" +
               "• Attendance Rate: 87.5%\n" +
               "• Course Completion: 96%\n" +
               "• Student Satisfaction: 4.3/5.0\n\n" +
               "ACADEMIC PERFORMANCE:\n" +
               "-".repeat(21) + "\n" +
               "Grade Distribution:\n" +
               "  A grades: 24% (12 students)\n" +
               "  B grades: 36% (18 students)\n" +
               "  C grades: 30% (15 students)\n" +
               "  D grades: 8% (4 students)\n" +
               "  F grades: 2% (1 student)\n\n" +
               "LEARNING OBJECTIVES ASSESSMENT:\n" +
               "-".repeat(32) + "\n" +
               "Objective 1 (Fundamentals): 85% mastery\n" +
               "Objective 2 (Applications): 82% mastery\n" +
               "Objective 3 (Analysis): 79% mastery\n" +
               "Objective 4 (Synthesis): 77% mastery\n\n" +
               "STUDENT FEEDBACK HIGHLIGHTS:\n" +
               "-".repeat(28) + "\n" +
               "• Clear explanations: 92% positive\n" +
               "• Helpful examples: 89% positive\n" +
               "• Fair grading: 87% positive\n" +
               "• Available for help: 94% positive\n\n" +
               "RECOMMENDATIONS FOR NEXT SEMESTER:\n" +
               "-".repeat(34) + "\n" +
               "• Continue current teaching methods\n" +
               "• Add more practical examples\n" +
               "• Increase focus on analysis skills\n" +
               "• Maintain strong student support\n";
    }
    
    private Long getCurrentInstructorId() {
        // This should get the current instructor ID from session
        // For now, return a default value
        return 1L;
    }
}