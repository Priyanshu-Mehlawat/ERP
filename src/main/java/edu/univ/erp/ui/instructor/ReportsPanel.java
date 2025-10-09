package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SectionService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.*;
import java.awt.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.time.format.DateTimeFormatter;

/**
 * Panel for generating reports and statistics.
 */
public class ReportsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ReportsPanel.class);
    
    private final SectionService sectionService;
    private final InstructorDAO instructorDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final GradeDAO gradeDAO;
    private final SectionDAO sectionDAO;
    private JComboBox<Section> sectionCombo;

    /**
     * Constructor with full dependency injection for testability and loose coupling.
     * 
     * @param sectionService the section service to use for section operations
     * @param instructorDAO the instructor DAO for instructor data access
     * @param enrollmentDAO the enrollment DAO for enrollment data access
     * @param gradeDAO the grade DAO for grade data access
     * @param sectionDAO the section DAO for section data access
     */
    public ReportsPanel(SectionService sectionService, InstructorDAO instructorDAO, 
                       EnrollmentDAO enrollmentDAO, GradeDAO gradeDAO, SectionDAO sectionDAO) {
        this.sectionService = sectionService;
        this.instructorDAO = instructorDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.gradeDAO = gradeDAO;
        this.sectionDAO = sectionDAO;
        initComponents();
        loadSections();
    }

    /**
     * Constructor with SectionService only - creates default DAO implementations.
     * 
     * @param sectionService the section service to use for section operations
     */
    public ReportsPanel(SectionService sectionService) {
        this(sectionService, new InstructorDAO(), new EnrollmentDAO(), new GradeDAO(), new SectionDAO());
    }

    /**
     * Default constructor - uses default implementations.
     * Creates a new SectionService and DAO instances internally.
     */
    public ReportsPanel() {
        this(new SectionService());
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
        reportArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        
        // Loading indicator
        JLabel loadingLabel = new JLabel("Loading report data...");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel loadingPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        loadingPanel.add(loadingLabel, "wrap");
        loadingPanel.add(progressBar, "growx");
        
        reportDialog.add(loadingPanel, "grow, wrap");
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]10[]", "[]"));
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.setEnabled(false); // Disabled until data is loaded
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        buttonPanel.add(exportBtn);
        buttonPanel.add(closeBtn);
        
        reportDialog.add(buttonPanel, "center");
        reportDialog.setSize(700, 600);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
        
        // Load data and generate report in background
        SwingWorker<GradeReportData, Void> worker = new SwingWorker<GradeReportData, Void>() {
            @Override
            protected GradeReportData doInBackground() throws Exception {
                return computeGradeDistributionData(selectedSection);
            }
            
            @Override
            protected void done() {
                try {
                    GradeReportData reportData = get();
                    String reportContent = buildGradeDistributionReport(reportData, sectionInfo);
                    
                    // Replace loading panel with report content
                    reportDialog.remove(loadingPanel);
                    reportArea.setText(reportContent);
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1); // Insert at position 1
                    
                    // Enable export button and set up export functionality
                    exportBtn.setEnabled(true);
                    exportBtn.addActionListener(e -> exportGradeDistributionToPDF(reportData, sectionInfo, reportDialog));
                    
                    reportDialog.revalidate();
                    reportDialog.repaint();
                    
                } catch (Exception e) {
                    logger.error("Error generating grade distribution report", e);
                    reportDialog.remove(loadingPanel);
                    JLabel errorLabel = new JLabel("<html><div style='text-align: center; color: red;'>" +
                        "Error loading report data:<br>" + e.getMessage() + "</div></html>");
                    reportDialog.add(errorLabel, "grow, wrap", 1);
                    reportDialog.revalidate();
                    reportDialog.repaint();
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Data class to hold computed grade distribution statistics
     */
    private static class GradeReportData {
        final String sectionInfo;
        final int totalStudents;
        final Map<String, Integer> gradeDistribution; // A, B, C, D, F counts
        final double average;
        final double median;
        final double standardDeviation;
        final double highestScore;
        final double lowestScore;
        final Map<String, ComponentStats> componentStats; // Stats by assignment type
        
        GradeReportData(String sectionInfo, int totalStudents, Map<String, Integer> gradeDistribution,
                       double average, double median, double standardDeviation, double highestScore, 
                       double lowestScore, Map<String, ComponentStats> componentStats) {
            this.sectionInfo = sectionInfo;
            this.totalStudents = totalStudents;
            this.gradeDistribution = gradeDistribution;
            this.average = average;
            this.median = median;
            this.standardDeviation = standardDeviation;
            this.highestScore = highestScore;
            this.lowestScore = lowestScore;
            this.componentStats = componentStats;
        }
    }
    
    private static class ComponentStats {
        final String componentType;
        final double average;
        final double min;
        final double max;
        
        ComponentStats(String componentType, double average, double min, double max) {
            this.componentType = componentType;
            this.average = average;
            this.min = min;
            this.max = max;
        }
    }
    
    /**
     * Compute grade distribution data for the selected section(s)
     */
    private GradeReportData computeGradeDistributionData(Section selectedSection) throws Exception {
        List<Enrollment> enrollments;
        String sectionInfo;
        
        Long instructorId = getCurrentInstructorId();
        if (instructorId == null) {
            throw new IllegalStateException("Cannot determine current instructor");
        }
        
        if (selectedSection != null) {
            enrollments = enrollmentDAO.listBySection(selectedSection.getSectionId());
            sectionInfo = selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber();
        } else {
            // Get all sections for this instructor
            List<Section> sections = sectionDAO.listByInstructor(instructorId);
            enrollments = new ArrayList<>();
            for (Section section : sections) {
                enrollments.addAll(enrollmentDAO.listBySection(section.getSectionId()));
            }
            sectionInfo = "All Sections";
        }
        
        if (enrollments.isEmpty()) {
            return new GradeReportData(sectionInfo, 0, new HashMap<>(), 0.0, 0.0, 0.0, 0.0, 0.0, new HashMap<>());
        }
        
        // Collect all grades and compute final scores for each student
        List<Double> finalScores = new ArrayList<>();
        Map<String, List<Double>> componentScores = new HashMap<>(); // Component type -> list of scores
        
        for (Enrollment enrollment : enrollments) {
            List<Grade> grades = gradeDAO.listByEnrollment(enrollment.getEnrollmentId());
            
            if (!grades.isEmpty()) {
                double totalScore = 0.0;
                double totalWeight = 0.0;
                
                for (Grade grade : grades) {
                    // Skip grades with null or invalid values to prevent NPE and ArithmeticException
                    if (grade.getScore() == null || grade.getMaxScore() == null || grade.getMaxScore() <= 0.0) {
                        continue; // Skip grades that can't be computed safely
                    }
                    
                    // Treat null weight as 0 (skip contribution to totals)
                    Double weight = grade.getWeight();
                    if (weight == null) {
                        continue; // Skip grades with no weight assigned
                    }
                    
                    double percentage = (grade.getScore().doubleValue() / grade.getMaxScore().doubleValue()) * 100.0;
                    totalScore += percentage * (weight.doubleValue() / 100.0);
                    totalWeight += weight.doubleValue() / 100.0;
                    
                    // Collect component scores
                    componentScores.computeIfAbsent(grade.getComponent(), k -> new ArrayList<>()).add(percentage);
                }
                
                if (totalWeight > 0) {
                    finalScores.add(totalScore / totalWeight);
                }
            }
        }
        
        if (finalScores.isEmpty()) {
            return new GradeReportData(sectionInfo, enrollments.size(), new HashMap<>(), 0.0, 0.0, 0.0, 0.0, 0.0, new HashMap<>());
        }
        
        // Compute statistics
        Collections.sort(finalScores);
        double average = finalScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Correctly compute median for both odd and even-sized lists
        double median;
        if (finalScores.isEmpty()) {
            median = 0.0; // Default for empty list
        } else {
            int size = finalScores.size();
            int index = size / 2;
            if (size % 2 == 1) {
                // Odd size: use middle element
                median = finalScores.get(index);
            } else {
                // Even size: average of two middle elements
                median = (finalScores.get(index - 1) + finalScores.get(index)) / 2.0;
            }
        }
        
        double highestScore = finalScores.get(finalScores.size() - 1);
        double lowestScore = finalScores.get(0);
        
        // Standard deviation
        double variance = finalScores.stream()
            .mapToDouble(score -> Math.pow(score - average, 2))
            .average().orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        
        // Grade distribution
        Map<String, Integer> gradeDistribution = new HashMap<>();
        gradeDistribution.put("A", 0);
        gradeDistribution.put("B", 0);
        gradeDistribution.put("C", 0);
        gradeDistribution.put("D", 0);
        gradeDistribution.put("F", 0);
        
        for (double score : finalScores) {
            if (score >= 90) gradeDistribution.put("A", gradeDistribution.get("A") + 1);
            else if (score >= 80) gradeDistribution.put("B", gradeDistribution.get("B") + 1);
            else if (score >= 70) gradeDistribution.put("C", gradeDistribution.get("C") + 1);
            else if (score >= 60) gradeDistribution.put("D", gradeDistribution.get("D") + 1);
            else gradeDistribution.put("F", gradeDistribution.get("F") + 1);
        }
        
        // Component statistics
        Map<String, ComponentStats> componentStats = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : componentScores.entrySet()) {
            List<Double> scores = entry.getValue();
            if (!scores.isEmpty()) {
                double compAverage = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double compMin = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double compMax = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                componentStats.put(entry.getKey(), new ComponentStats(entry.getKey(), compAverage, compMin, compMax));
            }
        }
        
        return new GradeReportData(sectionInfo, finalScores.size(), gradeDistribution, 
                                  average, median, standardDeviation, highestScore, lowestScore, componentStats);
    }
    
    /**
     * Build the text report from computed data
     */
    private String buildGradeDistributionReport(GradeReportData data, String sectionInfo) {
        StringBuilder report = new StringBuilder();
        report.append("GRADE DISTRIBUTION ANALYSIS\n");
        report.append("==========================\n\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        report.append("Section: ").append(sectionInfo).append("\n\n");
        
        if (data.totalStudents == 0) {
            report.append("No student data available for analysis.\n");
            return report.toString();
        }
        
        // Grade distribution
        report.append("GRADE DISTRIBUTION:\n");
        report.append("-".repeat(40)).append("\n");
        
        for (String grade : Arrays.asList("A", "B", "C", "D", "F")) {
            int count = data.gradeDistribution.getOrDefault(grade, 0);
            double percentage = (count * 100.0) / data.totalStudents;
            String gradeRange = getGradeRange(grade);
            String bars = "█".repeat(Math.max(1, (int)(percentage / 2))); // Scale bars
            report.append(String.format("%-12s %s %d students (%.1f%%)\n", 
                gradeRange, bars, count, percentage));
        }
        report.append("\n");
        
        // Statistical summary
        report.append("STATISTICAL SUMMARY:\n");
        report.append("-".repeat(30)).append("\n");
        report.append(String.format("Total Students: %d\n", data.totalStudents));
        report.append(String.format("Class Average: %.1f%%\n", data.average));
        report.append(String.format("Median Grade: %.1f%%\n", data.median));
        report.append(String.format("Standard Deviation: %.1f\n", data.standardDeviation));
        report.append(String.format("Highest Score: %.1f%%\n", data.highestScore));
        report.append(String.format("Lowest Score: %.1f%%\n", data.lowestScore));
        report.append("\n");
        
        // Performance by component type
        if (!data.componentStats.isEmpty()) {
            report.append("PERFORMANCE BY ASSIGNMENT TYPE:\n");
            report.append("-".repeat(35)).append("\n");
            
            for (ComponentStats stats : data.componentStats.values()) {
                report.append(String.format("%-12s Average %.1f%% (Range: %.1f%%-%.1f%%)\n",
                    stats.componentType + ":", stats.average, stats.min, stats.max));
            }
            report.append("\n");
        }
        
        // Recommendations
        report.append("RECOMMENDATIONS:\n");
        report.append("-".repeat(20)).append("\n");
        generateRecommendations(report, data);
        
        return report.toString();
    }
    
    private String getGradeRange(String grade) {
        switch (grade) {
            case "A": return "A  (90-100):";
            case "B": return "B  (80-89): ";
            case "C": return "C  (70-79): ";
            case "D": return "D  (60-69): ";
            case "F": return "F  (0-59):  ";
            default: return grade + ":";
        }
    }
    
    private void generateRecommendations(StringBuilder report, GradeReportData data) {
        double aPercentage = (data.gradeDistribution.getOrDefault("A", 0) * 100.0) / data.totalStudents;
        double fPercentage = (data.gradeDistribution.getOrDefault("F", 0) * 100.0) / data.totalStudents;
        
        if (aPercentage > 30) {
            report.append("• Excellent overall performance with high achievement rates\n");
        } else if (data.average >= 80) {
            report.append("• Strong overall performance with good student comprehension\n");
        } else if (data.average < 70) {
            report.append("• Consider reviewing teaching methods and course difficulty\n");
        }
        
        if (fPercentage > 10) {
            report.append("• High failure rate - consider additional support mechanisms\n");
        } else if (fPercentage > 0) {
            report.append("• Some students may need additional support\n");
        }
        
        if (data.standardDeviation > 15) {
            report.append("• High score variance - consider differentiated instruction\n");
        }
        
        // Component-specific recommendations
        for (ComponentStats stats : data.componentStats.values()) {
            if (stats.average < 70) {
                report.append("• ").append(stats.componentType).append(" scores are low - review content coverage\n");
            }
        }
    }
    
    /**
     * Export the grade distribution report to PDF
     */
    private void exportGradeDistributionToPDF(GradeReportData data, String sectionInfo, JDialog parentDialog) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Grade Distribution Report");
        fileChooser.setSelectedFile(new File("Grade_Distribution_Report.pdf"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        
        if (fileChooser.showSaveDialog(parentDialog) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            
            try {
                generatePDFReport(data, sectionInfo, file);
                JOptionPane.showMessageDialog(parentDialog, 
                    "Report exported successfully to:\n" + file.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logger.error("Error exporting PDF report", e);
                JOptionPane.showMessageDialog(parentDialog,
                    "Error exporting PDF: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Generate the actual PDF document
     */
    private void generatePDFReport(GradeReportData data, String sectionInfo, File outputFile) 
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        
        // Use try-with-resources to ensure FileOutputStream is properly closed
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            PdfWriter.getInstance(document, fos);
            document.open();
            
            try {
                // Title
                com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
                document.add(new Paragraph("Grade Distribution Report", titleFont));
                document.add(new Paragraph(" ")); // Spacing
                
                // Section info
                com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
                document.add(new Paragraph("Section: " + sectionInfo, headerFont));
                document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")), headerFont));
                document.add(new Paragraph(" "));
                
                if (data.totalStudents == 0) {
                    document.add(new Paragraph("No student data available for analysis."));
                    return; // Document will be closed in finally block
                }
                
                // Statistical Summary
                document.add(new Paragraph("Statistical Summary", headerFont));
                com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10);
                document.add(new Paragraph(String.format("Total Students: %d", data.totalStudents), normalFont));
                document.add(new Paragraph(String.format("Class Average: %.1f%%", data.average), normalFont));
                document.add(new Paragraph(String.format("Median Grade: %.1f%%", data.median), normalFont));
                document.add(new Paragraph(String.format("Standard Deviation: %.1f", data.standardDeviation), normalFont));
                document.add(new Paragraph(String.format("Highest Score: %.1f%%", data.highestScore), normalFont));
                document.add(new Paragraph(String.format("Lowest Score: %.1f%%", data.lowestScore), normalFont));
                document.add(new Paragraph(" "));
                
                // Grade Distribution
                document.add(new Paragraph("Grade Distribution", headerFont));
                for (String grade : Arrays.asList("A", "B", "C", "D", "F")) {
                    int count = data.gradeDistribution.getOrDefault(grade, 0);
                    double percentage = (count * 100.0) / data.totalStudents;
                    String gradeRange = getGradeRange(grade).replace(":", "");
                    document.add(new Paragraph(String.format("%s: %d students (%.1f%%)", 
                        gradeRange.trim(), count, percentage), normalFont));
                }
                document.add(new Paragraph(" "));
                
                // Component Performance
                if (!data.componentStats.isEmpty()) {
                    document.add(new Paragraph("Performance by Assignment Type", headerFont));
                    for (ComponentStats stats : data.componentStats.values()) {
                        document.add(new Paragraph(String.format("%s: Average %.1f%% (Range: %.1f%%-%.1f%%)",
                            stats.componentType, stats.average, stats.min, stats.max), normalFont));
                    }
                }
                
            } finally {
                // Ensure Document is always closed, even if exceptions occur
                document.close();
            }
        }
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
        try {
            // Safely get current user and handle null case
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                logger.error("No current user session found. User must be logged in to access instructor features.");
                throw new IllegalStateException("User session not found. Please log in again.");
            }
            
            Long userId = currentUser.getUserId();
            if (userId == null) {
                logger.warn("User ID is null in session, cannot retrieve instructor ID");
                return null;
            }
            
            // Get instructor record from user ID
            Instructor instructor;
            try {
                instructor = instructorDAO.findByUserId(userId);
            } catch (Exception dbException) {
                logger.error("Database error while retrieving instructor for user ID: {}", userId, dbException);
                throw new RuntimeException("Database error while retrieving instructor information", dbException);
            }
            
            if (instructor == null) {
                logger.warn("No instructor record found for user ID: {}", userId);
                return null;
            }
            
            Long instructorId = instructor.getInstructorId();
            if (instructorId == null) {
                logger.warn("Instructor ID is null for user ID: {}", userId);
                return null;
            }
            
            logger.debug("Retrieved instructor ID: {} for user ID: {}", instructorId, userId);
            return instructorId;
            
        } catch (Exception e) {
            logger.error("Error retrieving current instructor ID from session", e);
            // Re-throw IllegalStateException for session issues, wrap others in RuntimeException
            if (e instanceof IllegalStateException) {
                throw e;
            } else {
                throw new RuntimeException("Failed to retrieve current instructor ID", e);
            }
        }
    }
}