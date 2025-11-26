package edu.univ.erp.ui.instructor;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
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
    private final StudentDAO studentDAO;
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
        this.studentDAO = new StudentDAO();
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
        setLayout(new BorderLayout());
        
        // Content panel that will be scrollable
        JPanel contentPanel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]10[]20[grow]"));

        // Header
        contentPanel.add(new JLabel("<html><h2>📊 Reports & Analytics</h2></html>"), "wrap");
        
        // Section selection
        JPanel sectionPanel = new JPanel(new MigLayout("insets 0", "[]10[200!]", "[]"));
        sectionPanel.add(new JLabel("Select Section:"));
        sectionCombo = new JComboBox<>();
        sectionPanel.add(sectionCombo);
        contentPanel.add(sectionPanel, "wrap");
        
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
        
        contentPanel.add(reportsGrid, "grow");
        
        // Wrap entire content in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
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
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() :
            "All Sections";
            
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Class Performance Analysis", true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        reportDialog.add(new JLabel("<html><h3>🎯 Class Performance Analysis - " + sectionInfo + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(20, 60);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Loading panel
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel loadingPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        loadingPanel.add(new JLabel("Generating class performance report..."), "wrap");
        loadingPanel.add(progressBar, "growx");
        reportDialog.add(loadingPanel, "grow, wrap");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.setEnabled(false);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        buttonPanel.add(exportBtn);
        buttonPanel.add(closeBtn);
        reportDialog.add(buttonPanel, "center");
        
        reportDialog.setSize(750, 650);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
        
        // Load data in background
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return computeClassPerformanceReport(selectedSection, sectionInfo);
            }
            
            @Override
            protected void done() {
                try {
                    String reportContent = get();
                    reportDialog.remove(loadingPanel);
                    reportArea.setText(reportContent);
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    exportBtn.setEnabled(true);
                    exportBtn.addActionListener(e -> exportReportToPDF(reportContent, 
                        "Class_Performance_Report", reportDialog));
                    reportDialog.revalidate();
                    reportDialog.repaint();
                } catch (Exception e) {
                    logger.error("Error generating class performance report", e);
                    reportDialog.remove(loadingPanel);
                    reportArea.setText("Error generating report: " + e.getMessage());
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    reportDialog.revalidate();
                    reportDialog.repaint();
                }
            }
        };
        worker.execute();
    }
    
    private String computeClassPerformanceReport(Section selectedSection, String sectionInfo) throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("CLASS PERFORMANCE ANALYSIS\n");
        report.append("=========================\n\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        report.append("Section: ").append(sectionInfo).append("\n\n");
        
        Long instructorId = getCurrentInstructorId();
        List<Section> sections;
        if (selectedSection != null) {
            sections = List.of(selectedSection);
        } else {
            sections = sectionDAO.listByInstructor(instructorId);
        }
        
        if (sections.isEmpty()) {
            report.append("No sections found.\n");
            return report.toString();
        }
        
        int totalEnrolled = 0;
        int totalActive = 0;
        int totalDropped = 0;
        int totalCompleted = 0;
        List<StudentPerformance> studentPerformances = new ArrayList<>();
        
        for (Section section : sections) {
            List<Enrollment> enrollments = enrollmentDAO.listBySection(section.getSectionId());
            
            for (Enrollment enrollment : enrollments) {
                totalEnrolled++;
                if ("ENROLLED".equals(enrollment.getStatus())) {
                    totalActive++;
                } else if ("DROPPED".equals(enrollment.getStatus())) {
                    totalDropped++;
                } else if ("COMPLETED".equals(enrollment.getStatus())) {
                    totalCompleted++;
                }
                
                // Get grades for this enrollment
                List<Grade> grades = gradeDAO.listByEnrollment(enrollment.getEnrollmentId());
                if (!grades.isEmpty()) {
                    double totalScore = 0.0;
                    double totalWeight = 0.0;
                    
                    for (Grade grade : grades) {
                        if (grade.getScore() != null && grade.getMaxScore() != null && 
                            grade.getMaxScore() > 0 && grade.getWeight() != null) {
                            double percentage = (grade.getScore() / grade.getMaxScore()) * 100.0;
                            totalScore += percentage * (grade.getWeight() / 100.0);
                            totalWeight += grade.getWeight() / 100.0;
                        }
                    }
                    
                    if (totalWeight > 0) {
                        double avgScore = totalScore / totalWeight;
                        Student student = studentDAO.findById(enrollment.getStudentId());
                        String studentName = student != null ? 
                            student.getFirstName() + " " + student.getLastName() : "Unknown";
                        studentPerformances.add(new StudentPerformance(studentName, avgScore));
                    }
                }
            }
        }
        
        // Sort by performance
        studentPerformances.sort((a, b) -> Double.compare(b.average, a.average));
        
        double classAverage = studentPerformances.isEmpty() ? 0.0 :
            studentPerformances.stream().mapToDouble(s -> s.average).average().orElse(0.0);
        long passCount = studentPerformances.stream().filter(s -> s.average >= 60).count();
        
        // Overall Metrics
        report.append("OVERALL CLASS METRICS:\n");
        report.append("-".repeat(25)).append("\n");
        report.append(String.format("Total Enrollment: %d students\n", totalEnrolled));
        report.append(String.format("Active Students: %d (%.1f%%)\n", totalActive, 
            totalEnrolled > 0 ? (totalActive * 100.0 / totalEnrolled) : 0));
        report.append(String.format("Dropped: %d students (%.1f%%)\n", totalDropped,
            totalEnrolled > 0 ? (totalDropped * 100.0 / totalEnrolled) : 0));
        report.append(String.format("Completed: %d students\n", totalCompleted));
        report.append(String.format("Class Average: %.1f%%\n", classAverage));
        report.append(String.format("Pass Rate: %.1f%% (%d/%d)\n\n", 
            studentPerformances.isEmpty() ? 0 : (passCount * 100.0 / studentPerformances.size()),
            passCount, studentPerformances.size()));
        
        // Top Performers
        if (!studentPerformances.isEmpty()) {
            report.append("TOP PERFORMERS:\n");
            report.append("-".repeat(15)).append("\n");
            int count = Math.min(5, studentPerformances.size());
            for (int i = 0; i < count; i++) {
                StudentPerformance sp = studentPerformances.get(i);
                report.append(String.format("%d. %-20s - %.1f%% average\n", 
                    i + 1, sp.studentName, sp.average));
            }
            report.append("\n");
            
            // Students Needing Support
            List<StudentPerformance> needsSupport = studentPerformances.stream()
                .filter(s -> s.average < 70)
                .toList();
            
            if (!needsSupport.isEmpty()) {
                report.append("STUDENTS NEEDING SUPPORT:\n");
                report.append("-".repeat(25)).append("\n");
                for (StudentPerformance sp : needsSupport) {
                    String recommendation = sp.average < 60 ? "(Recommend tutoring)" : "(Schedule meeting)";
                    report.append(String.format("• %-20s - %.1f%% %s\n", 
                        sp.studentName, sp.average, recommendation));
                }
                report.append("\n");
            }
        }
        
        // Recommendations
        report.append("RECOMMENDATIONS:\n");
        report.append("-".repeat(15)).append("\n");
        if (classAverage >= 80) {
            report.append("• Class performing above average - excellent work!\n");
        } else if (classAverage >= 70) {
            report.append("• Class performing at acceptable level\n");
        } else {
            report.append("• Consider reviewing teaching methods and course difficulty\n");
        }
        
        if (totalDropped > 0 && (totalDropped * 100.0 / totalEnrolled) > 10) {
            report.append("• High dropout rate - consider student engagement strategies\n");
        }
        
        return report.toString();
    }
    
    private static class StudentPerformance {
        final String studentName;
        final double average;
        
        StudentPerformance(String studentName, double average) {
            this.studentName = studentName;
            this.average = average;
        }
    }
    
    private void generateAttendanceReport() {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() :
            "All Sections";
            
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Attendance Summary Report", true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        reportDialog.add(new JLabel("<html><h3>📅 Attendance Summary Report - " + sectionInfo + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(20, 60);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Loading panel
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel loadingPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        loadingPanel.add(new JLabel("Generating attendance report..."), "wrap");
        loadingPanel.add(progressBar, "growx");
        reportDialog.add(loadingPanel, "grow, wrap");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.setEnabled(false);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        buttonPanel.add(exportBtn);
        buttonPanel.add(closeBtn);
        reportDialog.add(buttonPanel, "center");
        
        reportDialog.setSize(750, 650);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
        
        // Load data in background
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return computeAttendanceReport(selectedSection, sectionInfo);
            }
            
            @Override
            protected void done() {
                try {
                    String reportContent = get();
                    reportDialog.remove(loadingPanel);
                    reportArea.setText(reportContent);
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    exportBtn.setEnabled(true);
                    exportBtn.addActionListener(e -> exportReportToPDF(reportContent, 
                        "Attendance_Report", reportDialog));
                    reportDialog.revalidate();
                    reportDialog.repaint();
                } catch (Exception e) {
                    logger.error("Error generating attendance report", e);
                    reportDialog.remove(loadingPanel);
                    reportArea.setText("Error generating report: " + e.getMessage());
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    reportDialog.revalidate();
                    reportDialog.repaint();
                }
            }
        };
        worker.execute();
    }
    
    private String computeAttendanceReport(Section selectedSection, String sectionInfo) throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("ATTENDANCE SUMMARY REPORT\n");
        report.append("========================\n\n");
        report.append("Report Period: Current Semester\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        report.append("Section: ").append(sectionInfo).append("\n\n");
        
        Long instructorId = getCurrentInstructorId();
        List<Section> sections;
        if (selectedSection != null) {
            sections = List.of(selectedSection);
        } else {
            sections = sectionDAO.listByInstructor(instructorId);
        }
        
        if (sections.isEmpty()) {
            report.append("No sections found.\n");
            return report.toString();
        }
        
        // Note: Since there's no attendance table, we'll generate a report based on enrollment data
        report.append("ENROLLMENT STATUS SUMMARY:\n");
        report.append("-".repeat(27)).append("\n\n");
        
        int totalStudents = 0;
        int activeStudents = 0;
        int droppedStudents = 0;
        
        for (Section section : sections) {
            List<Enrollment> enrollments = enrollmentDAO.listBySection(section.getSectionId());
            
            report.append(String.format("Section: %s - %s\n", section.getCourseCode(), section.getSectionNumber()));
            report.append("-".repeat(30)).append("\n");
            
            int sectionTotal = enrollments.size();
            int sectionActive = (int) enrollments.stream().filter(e -> "ENROLLED".equals(e.getStatus())).count();
            int sectionDropped = (int) enrollments.stream().filter(e -> "DROPPED".equals(e.getStatus())).count();
            
            totalStudents += sectionTotal;
            activeStudents += sectionActive;
            droppedStudents += sectionDropped;
            
            report.append(String.format("  Total Enrolled: %d\n", sectionTotal));
            report.append(String.format("  Currently Active: %d (%.1f%%)\n", sectionActive, 
                sectionTotal > 0 ? (sectionActive * 100.0 / sectionTotal) : 0));
            report.append(String.format("  Dropped: %d (%.1f%%)\n\n", sectionDropped,
                sectionTotal > 0 ? (sectionDropped * 100.0 / sectionTotal) : 0));
        }
        
        report.append("\nOVERALL SUMMARY:\n");
        report.append("-".repeat(16)).append("\n");
        report.append(String.format("Total Students Across All Sections: %d\n", totalStudents));
        report.append(String.format("Active Students: %d (%.1f%%)\n", activeStudents,
            totalStudents > 0 ? (activeStudents * 100.0 / totalStudents) : 0));
        report.append(String.format("Dropped Students: %d (%.1f%%)\n", droppedStudents,
            totalStudents > 0 ? (droppedStudents * 100.0 / totalStudents) : 0));
        
        report.append("\nRECOMMENDATIONS:\n");
        report.append("-".repeat(15)).append("\n");
        if (droppedStudents > 0 && (droppedStudents * 100.0 / totalStudents) > 10) {
            report.append("• High dropout rate detected - consider reaching out to students\n");
        }
        report.append("• Maintain regular communication with students\n");
        report.append("• Consider implementing attendance tracking system\n");
        
        return report.toString();
    }

    private void generateStudentProgressReport() {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() :
            "All Sections";
            
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Student Progress Tracking", true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        reportDialog.add(new JLabel("<html><h3>👥 Student Progress Tracking - " + sectionInfo + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(20, 60);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Loading panel
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel loadingPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        loadingPanel.add(new JLabel("Generating student progress report..."), "wrap");
        loadingPanel.add(progressBar, "growx");
        reportDialog.add(loadingPanel, "grow, wrap");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.setEnabled(false);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        buttonPanel.add(exportBtn);
        buttonPanel.add(closeBtn);
        reportDialog.add(buttonPanel, "center");
        
        reportDialog.setSize(750, 650);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
        
        // Load data in background
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return computeStudentProgressReport(selectedSection, sectionInfo);
            }
            
            @Override
            protected void done() {
                try {
                    String reportContent = get();
                    reportDialog.remove(loadingPanel);
                    reportArea.setText(reportContent);
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    exportBtn.setEnabled(true);
                    exportBtn.addActionListener(e -> exportReportToPDF(reportContent, 
                        "Student_Progress_Report", reportDialog));
                    reportDialog.revalidate();
                    reportDialog.repaint();
                } catch (Exception e) {
                    logger.error("Error generating student progress report", e);
                    reportDialog.remove(loadingPanel);
                    reportArea.setText("Error generating report: " + e.getMessage());
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    reportDialog.revalidate();
                    reportDialog.repaint();
                }
            }
        };
        worker.execute();
    }
    
    private String computeStudentProgressReport(Section selectedSection, String sectionInfo) throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("STUDENT PROGRESS TRACKING\n");
        report.append("========================\n\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        report.append("Section: ").append(sectionInfo).append("\n\n");
        
        Long instructorId = getCurrentInstructorId();
        List<Section> sections;
        if (selectedSection != null) {
            sections = List.of(selectedSection);
        } else {
            sections = sectionDAO.listByInstructor(instructorId);
        }
        
        if (sections.isEmpty()) {
            report.append("No sections found.\n");
            return report.toString();
        }
        
        List<StudentProgressData> progressList = new ArrayList<>();
        Map<String, Integer> componentCounts = new HashMap<>();
        Map<String, Double> componentTotals = new HashMap<>();
        
        for (Section section : sections) {
            List<Enrollment> enrollments = enrollmentDAO.listBySection(section.getSectionId());
            
            for (Enrollment enrollment : enrollments) {
                if (!"ENROLLED".equals(enrollment.getStatus())) continue;
                
                List<Grade> grades = gradeDAO.listByEnrollment(enrollment.getEnrollmentId());
                if (grades.isEmpty()) continue;
                
                Student student = studentDAO.findById(enrollment.getStudentId());
                String studentName = student != null ? 
                    student.getFirstName() + " " + student.getLastName() : "Unknown";
                
                double totalScore = 0.0;
                double totalWeight = 0.0;
                int gradedItems = 0;
                
                for (Grade grade : grades) {
                    if (grade.getScore() != null && grade.getMaxScore() != null && 
                        grade.getMaxScore() > 0 && grade.getWeight() != null) {
                        double percentage = (grade.getScore() / grade.getMaxScore()) * 100.0;
                        totalScore += percentage * (grade.getWeight() / 100.0);
                        totalWeight += grade.getWeight() / 100.0;
                        gradedItems++;
                        
                        // Track component averages
                        String component = grade.getComponent() != null ? grade.getComponent() : "Other";
                        componentCounts.merge(component, 1, Integer::sum);
                        componentTotals.merge(component, percentage, Double::sum);
                    }
                }
                
                if (totalWeight > 0) {
                    double average = totalScore / totalWeight;
                    progressList.add(new StudentProgressData(studentName, average, gradedItems));
                }
            }
        }
        
        if (progressList.isEmpty()) {
            report.append("No graded assignments found for students.\n");
            return report.toString();
        }
        
        // Sort by performance
        progressList.sort((a, b) -> Double.compare(b.average, a.average));
        
        // Calculate statistics
        double classAverage = progressList.stream().mapToDouble(s -> s.average).average().orElse(0.0);
        long excellentCount = progressList.stream().filter(s -> s.average >= 90).count();
        long goodCount = progressList.stream().filter(s -> s.average >= 70 && s.average < 90).count();
        long needsImprovementCount = progressList.stream().filter(s -> s.average < 70).count();
        
        report.append("PROGRESS OVERVIEW:\n");
        report.append("-".repeat(18)).append("\n");
        report.append(String.format("Total Students with Grades: %d\n", progressList.size()));
        report.append(String.format("Class Average: %.1f%%\n", classAverage));
        report.append(String.format("Excellent (90%%+): %d students (%.1f%%)\n", excellentCount,
            (excellentCount * 100.0 / progressList.size())));
        report.append(String.format("Good (70-89%%): %d students (%.1f%%)\n", goodCount,
            (goodCount * 100.0 / progressList.size())));
        report.append(String.format("Needs Improvement (<70%%): %d students (%.1f%%)\n\n", needsImprovementCount,
            (needsImprovementCount * 100.0 / progressList.size())));
        
        // Top performers
        report.append("TOP PERFORMERS:\n");
        report.append("-".repeat(15)).append("\n");
        int count = Math.min(5, progressList.size());
        for (int i = 0; i < count; i++) {
            StudentProgressData sp = progressList.get(i);
            report.append(String.format("%d. %-20s - %.1f%% (%d assignments)\n", 
                i + 1, sp.studentName, sp.average, sp.gradedItems));
        }
        report.append("\n");
        
        // Students needing attention
        List<StudentProgressData> needsAttention = progressList.stream()
            .filter(s -> s.average < 70)
            .toList();
        
        if (!needsAttention.isEmpty()) {
            report.append("STUDENTS NEEDING ATTENTION:\n");
            report.append("-".repeat(28)).append("\n");
            for (StudentProgressData sp : needsAttention) {
                report.append(String.format("• %-20s - %.1f%% (%d assignments)\n", 
                    sp.studentName, sp.average, sp.gradedItems));
            }
            report.append("\n");
        }
        
        // Assignment type performance
        if (!componentCounts.isEmpty()) {
            report.append("PERFORMANCE BY ASSIGNMENT TYPE:\n");
            report.append("-".repeat(32)).append("\n");
            for (Map.Entry<String, Integer> entry : componentCounts.entrySet()) {
                double avg = componentTotals.get(entry.getKey()) / entry.getValue();
                report.append(String.format("%-12s: %.1f%% average (%d submissions)\n", 
                    entry.getKey(), avg, entry.getValue()));
            }
            report.append("\n");
        }
        
        // Recommendations
        report.append("ACTION ITEMS:\n");
        report.append("-".repeat(13)).append("\n");
        if (!needsAttention.isEmpty()) {
            report.append("• Schedule meetings with struggling students\n");
        }
        if (classAverage >= 80) {
            report.append("• Continue current teaching strategies - excellent results!\n");
        }
        report.append("• Provide feedback on recent assignments\n");
        
        return report.toString();
    }
    
    private static class StudentProgressData {
        final String studentName;
        final double average;
        final int gradedItems;
        
        StudentProgressData(String studentName, double average, int gradedItems) {
            this.studentName = studentName;
            this.average = average;
            this.gradedItems = gradedItems;
        }
    }

    private void generateComprehensiveReport() {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + " Section " + selectedSection.getSectionNumber() :
            "All Sections";
            
        JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Comprehensive Semester Report", true);
        reportDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[grow]10[]"));
        
        reportDialog.add(new JLabel("<html><h3>📋 Comprehensive Semester Report - " + sectionInfo + "</h3></html>"), "wrap");
        
        JTextArea reportArea = new JTextArea(25, 70);
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Loading panel
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel loadingPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        loadingPanel.add(new JLabel("Generating comprehensive report..."), "wrap");
        loadingPanel.add(progressBar, "growx");
        reportDialog.add(loadingPanel, "grow, wrap");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.setEnabled(false);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> reportDialog.dispose());
        buttonPanel.add(exportBtn);
        buttonPanel.add(closeBtn);
        reportDialog.add(buttonPanel, "center");
        
        reportDialog.setSize(800, 700);
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
        
        // Load data in background
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return computeComprehensiveReport(selectedSection, sectionInfo);
            }
            
            @Override
            protected void done() {
                try {
                    String reportContent = get();
                    reportDialog.remove(loadingPanel);
                    reportArea.setText(reportContent);
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    exportBtn.setEnabled(true);
                    exportBtn.addActionListener(e -> exportReportToPDF(reportContent, 
                        "Comprehensive_Semester_Report", reportDialog));
                    reportDialog.revalidate();
                    reportDialog.repaint();
                } catch (Exception e) {
                    logger.error("Error generating comprehensive report", e);
                    reportDialog.remove(loadingPanel);
                    reportArea.setText("Error generating report: " + e.getMessage());
                    JScrollPane scrollPane = new JScrollPane(reportArea);
                    reportDialog.add(scrollPane, "grow, wrap", 1);
                    reportDialog.revalidate();
                    reportDialog.repaint();
                }
            }
        };
        worker.execute();
    }
    
    private String computeComprehensiveReport(Section selectedSection, String sectionInfo) throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("COMPREHENSIVE SEMESTER REPORT\n");
        report.append("============================\n\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        report.append("Instructor: ");
        
        Long instructorId = getCurrentInstructorId();
        Instructor instructor = instructorDAO.findById(instructorId);
        report.append(instructor != null ? instructor.getFullName() : "Unknown").append("\n");
        report.append("Section: ").append(sectionInfo).append("\n\n");
        
        List<Section> sections;
        if (selectedSection != null) {
            sections = List.of(selectedSection);
        } else {
            sections = sectionDAO.listByInstructor(instructorId);
        }
        
        if (sections.isEmpty()) {
            report.append("No sections found.\n");
            return report.toString();
        }
        
        report.append("EXECUTIVE SUMMARY:\n");
        report.append("-".repeat(18)).append("\n");
        report.append("This report provides a complete overview of class performance,\n");
        report.append("enrollment status, and student progress for the current semester.\n\n");
        
        // Aggregate data
        int totalSections = sections.size();
        int totalEnrolled = 0;
        int totalActive = 0;
        int totalDropped = 0;
        List<Double> allScores = new ArrayList<>();
        Map<String, Integer> gradeDistribution = new HashMap<>();
        gradeDistribution.put("A", 0);
        gradeDistribution.put("B", 0);
        gradeDistribution.put("C", 0);
        gradeDistribution.put("D", 0);
        gradeDistribution.put("F", 0);
        
        for (Section section : sections) {
            List<Enrollment> enrollments = enrollmentDAO.listBySection(section.getSectionId());
            
            for (Enrollment enrollment : enrollments) {
                totalEnrolled++;
                if ("ENROLLED".equals(enrollment.getStatus())) {
                    totalActive++;
                } else if ("DROPPED".equals(enrollment.getStatus())) {
                    totalDropped++;
                }
                
                List<Grade> grades = gradeDAO.listByEnrollment(enrollment.getEnrollmentId());
                double totalScore = 0.0;
                double totalWeight = 0.0;
                
                for (Grade grade : grades) {
                    if (grade.getScore() != null && grade.getMaxScore() != null && 
                        grade.getMaxScore() > 0 && grade.getWeight() != null) {
                        double percentage = (grade.getScore() / grade.getMaxScore()) * 100.0;
                        totalScore += percentage * (grade.getWeight() / 100.0);
                        totalWeight += grade.getWeight() / 100.0;
                    }
                }
                
                if (totalWeight > 0) {
                    double avg = totalScore / totalWeight;
                    allScores.add(avg);
                    
                    // Update grade distribution
                    if (avg >= 90) gradeDistribution.merge("A", 1, Integer::sum);
                    else if (avg >= 80) gradeDistribution.merge("B", 1, Integer::sum);
                    else if (avg >= 70) gradeDistribution.merge("C", 1, Integer::sum);
                    else if (avg >= 60) gradeDistribution.merge("D", 1, Integer::sum);
                    else gradeDistribution.merge("F", 1, Integer::sum);
                }
            }
        }
        
        double classAverage = allScores.isEmpty() ? 0.0 :
            allScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        long passCount = allScores.stream().filter(s -> s >= 60).count();
        double passRate = allScores.isEmpty() ? 0.0 : (passCount * 100.0 / allScores.size());
        
        // Key Metrics
        report.append("KEY METRICS:\n");
        report.append("-".repeat(12)).append("\n");
        report.append(String.format("• Total Sections: %d\n", totalSections));
        report.append(String.format("• Total Enrollment: %d students\n", totalEnrolled));
        report.append(String.format("• Active Students: %d (%.1f%%)\n", totalActive,
            totalEnrolled > 0 ? (totalActive * 100.0 / totalEnrolled) : 0));
        report.append(String.format("• Overall Class Average: %.1f%%\n", classAverage));
        report.append(String.format("• Pass Rate: %.1f%%\n", passRate));
        report.append(String.format("• Dropout Rate: %.1f%%\n\n", 
            totalEnrolled > 0 ? (totalDropped * 100.0 / totalEnrolled) : 0));
        
        // Grade Distribution
        if (!allScores.isEmpty()) {
            report.append("ACADEMIC PERFORMANCE:\n");
            report.append("-".repeat(21)).append("\n");
            report.append("Grade Distribution:\n");
            for (String grade : Arrays.asList("A", "B", "C", "D", "F")) {
                int count = gradeDistribution.get(grade);
                double pct = (count * 100.0) / allScores.size();
                report.append(String.format("  %s grades: %.1f%% (%d students)\n", grade, pct, count));
            }
            report.append("\n");
        }
        
        // Section Details
        report.append("SECTION BREAKDOWN:\n");
        report.append("-".repeat(18)).append("\n");
        for (Section section : sections) {
            report.append(String.format("• %s Section %s\n", section.getCourseCode(), section.getSectionNumber()));
            report.append(String.format("  Schedule: %s %s\n", 
                section.getDayOfWeek() != null ? section.getDayOfWeek() : "TBA",
                section.getStartTime() != null ? section.getStartTime().toString() : ""));
            report.append(String.format("  Capacity: %d/%d enrolled\n", section.getEnrolled(), section.getCapacity()));
        }
        report.append("\n");
        
        // Recommendations
        report.append("RECOMMENDATIONS FOR NEXT SEMESTER:\n");
        report.append("-".repeat(34)).append("\n");
        if (classAverage >= 80) {
            report.append("• Continue current teaching methods - excellent results!\n");
        } else if (classAverage >= 70) {
            report.append("• Consider adding more practice exercises\n");
        } else {
            report.append("• Review course content and teaching approach\n");
        }
        
        if (totalDropped > 0 && (totalDropped * 100.0 / totalEnrolled) > 5) {
            report.append("• Investigate causes of student dropouts\n");
        }
        
        report.append("• Maintain regular office hours and student support\n");
        report.append("• Gather student feedback for course improvements\n");
        
        return report.toString();
    }

    private void exportClassData() {
        Section selectedSection = (Section) sectionCombo.getSelectedItem();
        String sectionInfo = selectedSection != null ? 
            selectedSection.getCourseCode() + "_Section_" + selectedSection.getSectionNumber() :
            "All_Sections";
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Class Data");
        fileChooser.setSelectedFile(new File("Class_Data_" + sectionInfo + ".csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            final File outputFile = file;
            
            // Show progress dialog
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                "Exporting Data", true);
            progressDialog.setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[]"));
            progressDialog.add(new JLabel("Exporting class data to CSV..."), "wrap");
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressDialog.add(progressBar, "growx");
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    exportClassDataToCSV(selectedSection, outputFile);
                    return null;
                }
                
                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        get();
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Data exported successfully to:\n" + outputFile.getAbsolutePath(),
                            "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Error exporting data", e);
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Error exporting data: " + e.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            
            worker.execute();
            progressDialog.setVisible(true);
        }
    }
    
    private void exportClassDataToCSV(Section selectedSection, File outputFile) throws Exception {
        Long instructorId = getCurrentInstructorId();
        List<Section> sections;
        if (selectedSection != null) {
            sections = List.of(selectedSection);
        } else {
            sections = sectionDAO.listByInstructor(instructorId);
        }
        
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(outputFile))) {
            // Write header
            writer.println("Section,Student ID,Student Name,Email,Status,Average Score,Grade");
            
            for (Section section : sections) {
                String sectionName = section.getCourseCode() + " - " + section.getSectionNumber();
                List<Enrollment> enrollments = enrollmentDAO.listBySection(section.getSectionId());
                
                for (Enrollment enrollment : enrollments) {
                    Student student = studentDAO.findById(enrollment.getStudentId());
                    String studentName = student != null ? 
                        student.getFirstName() + " " + student.getLastName() : "Unknown";
                    String email = student != null && student.getEmail() != null ? student.getEmail() : "";
                    String rollNo = student != null && student.getRollNo() != null ? student.getRollNo() : "";
                    
                    // Calculate average
                    List<Grade> grades = gradeDAO.listByEnrollment(enrollment.getEnrollmentId());
                    double totalScore = 0.0;
                    double totalWeight = 0.0;
                    
                    for (Grade grade : grades) {
                        if (grade.getScore() != null && grade.getMaxScore() != null && 
                            grade.getMaxScore() > 0 && grade.getWeight() != null) {
                            double percentage = (grade.getScore() / grade.getMaxScore()) * 100.0;
                            totalScore += percentage * (grade.getWeight() / 100.0);
                            totalWeight += grade.getWeight() / 100.0;
                        }
                    }
                    
                    double average = totalWeight > 0 ? totalScore / totalWeight : 0.0;
                    String letterGrade = getLetterGrade(average);
                    
                    // Escape CSV fields
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.1f,\"%s\"%n",
                        escapeCSV(sectionName),
                        escapeCSV(rollNo),
                        escapeCSV(studentName),
                        escapeCSV(email),
                        enrollment.getStatus(),
                        average,
                        letterGrade);
                }
            }
        }
    }
    
    private String getLetterGrade(double score) {
        if (score >= 90) return "A";
        else if (score >= 80) return "B";
        else if (score >= 70) return "C";
        else if (score >= 60) return "D";
        else return "F";
    }
    
    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    private void exportReportToPDF(String content, String defaultFileName, JDialog parentDialog) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report as PDF");
        fileChooser.setSelectedFile(new File(defaultFileName + ".pdf"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
        
        if (fileChooser.showSaveDialog(parentDialog) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            
            try {
                Document document = new Document(PageSize.A4);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    PdfWriter.getInstance(document, fos);
                    document.open();
                    
                    com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD);
                    com.lowagie.text.Font normalFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.COURIER, 10);
                    
                    // Add title
                    document.add(new Paragraph(defaultFileName.replace("_", " "), titleFont));
                    document.add(new Paragraph(" "));
                    
                    // Add content
                    for (String line : content.split("\n")) {
                        document.add(new Paragraph(line, normalFont));
                    }
                    
                    document.close();
                }
                
                JOptionPane.showMessageDialog(parentDialog,
                    "Report exported successfully to:\n" + file.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logger.error("Error exporting PDF", e);
                JOptionPane.showMessageDialog(parentDialog,
                    "Error exporting PDF: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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