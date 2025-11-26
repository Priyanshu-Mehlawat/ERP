package edu.univ.erp.ui.student;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.EnrollmentService;
import net.miginfocom.swing.MigLayout;

/**
 * Panel for viewing and exporting academic transcript.
 */
public class TranscriptPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptPanel.class);
    
    // Default credits per course
    private static final int DEFAULT_CREDITS = 3;

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final StudentDAO studentDAO = new StudentDAO();
    
    private JTable transcriptTable;
    private DefaultTableModel transcriptModel;
    private JLabel gpaLabel;
    private JLabel creditsLabel;
    private JLabel statusLabel;
    private Student currentStudent;
    private User currentUser;
    
    private final DecimalFormat df = new DecimalFormat("#.##");

    public TranscriptPanel() {
        loadCurrentStudent();
        initComponents();
        loadTranscript();
    }

    private void loadCurrentStudent() {
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                logger.warn("Current user or user ID is null, cannot load student data");
                return;
            }
            currentStudent = studentDAO.findByUserId(currentUser.getUserId());
            if (currentStudent == null) {
                logger.warn("No student found for user ID: {}", currentUser.getUserId());
            }
        } catch (SQLException e) {
            logger.error("Error loading current student", e);
        }
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][][][grow][]"));

        // Header
        add(new JLabel("<html><h2>Official Transcript</h2></html>"), "wrap");

        // Student info panel
        JPanel infoPanel = new JPanel(new MigLayout("insets 0", "[][]", "[][]"));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Student Information"));
        
        if (currentStudent != null && currentUser != null) {
            infoPanel.add(new JLabel("Name: "));
            infoPanel.add(new JLabel(currentStudent.getFirstName() + " " + currentStudent.getLastName()), "wrap");
            infoPanel.add(new JLabel("Student ID: "));
            infoPanel.add(new JLabel(String.valueOf(currentStudent.getStudentId())), "wrap");
            infoPanel.add(new JLabel("Program: "));
            infoPanel.add(new JLabel(currentStudent.getProgram()), "wrap");
            infoPanel.add(new JLabel("Year: "));
            infoPanel.add(new JLabel(String.valueOf(currentStudent.getYear())));
        }
        
        add(infoPanel, "wrap");

        // Summary panel
        JPanel summaryPanel = new JPanel(new MigLayout("insets 0", "[]20[]20[]", ""));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Academic Summary"));
        
        summaryPanel.add(new JLabel("Cumulative GPA: "));
        gpaLabel = new JLabel("Calculating...");
        gpaLabel.setFont(gpaLabel.getFont().deriveFont(Font.BOLD));
        gpaLabel.setForeground(new Color(0, 100, 0));
        summaryPanel.add(gpaLabel);
        
        summaryPanel.add(new JLabel("Total Credits: "));
        creditsLabel = new JLabel("0");
        creditsLabel.setFont(creditsLabel.getFont().deriveFont(Font.BOLD));
        summaryPanel.add(creditsLabel);
        
        add(summaryPanel, "wrap");

        // Transcript table
        JPanel tablePanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow]"));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Course History"));
        
        String[] columns = {"Term", "Course Code", "Course Title", "Credits", "Grade", "Grade Points"};
        transcriptModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transcriptTable = new JTable(transcriptModel);
        transcriptTable.setAutoCreateRowSorter(true);
        
        // Set column widths
        transcriptTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        transcriptTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        transcriptTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        transcriptTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        transcriptTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        transcriptTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        
        tablePanel.add(new JScrollPane(transcriptTable), "grow");
        add(tablePanel, "grow, wrap");

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[grow][]20[]", ""));
        
        statusLabel = new JLabel("Loading...");
        controlPanel.add(statusLabel, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadTranscript());
        controlPanel.add(refreshBtn);
        
        JButton exportBtn = new JButton("Export to CSV");
        exportBtn.addActionListener(e -> exportToCSV());
        controlPanel.add(exportBtn);
        
        add(controlPanel, "growx");
    }

    private void loadTranscript() {
        if (currentStudent == null) {
            statusLabel.setText("Student data not available");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    List<Enrollment> enrollments = enrollmentService.listByStudent(currentStudent.getStudentId());
                    
                    SwingUtilities.invokeLater(() -> {
                        transcriptModel.setRowCount(0);
                        
                        double totalGradePoints = 0.0;
                        int totalCredits = 0;
                        int completedCourses = 0;
                        
                        for (Enrollment enrollment : enrollments) {
                            String term = determineTerm(enrollment.getEnrolledDate());
                            String grade = enrollment.getFinalGrade();
                            int credits = DEFAULT_CREDITS;
                            
                            double gradePoints = 0.0;
                            if (grade != null && !grade.isEmpty()) {
                                gradePoints = convertLetterToGPA(grade) * credits;
                                totalGradePoints += gradePoints;
                                totalCredits += credits;
                                completedCourses++;
                            } else if ("COMPLETED".equals(enrollment.getStatus())) {
                                grade = "IP"; // In Progress
                            } else {
                                grade = ""; // Not completed
                            }
                            
                            transcriptModel.addRow(new Object[]{
                                term,
                                enrollment.getCourseCode(),
                                enrollment.getCourseTitle(),
                                credits,
                                grade,
                                grade != null && !grade.isEmpty() && !"IP".equals(grade) ? 
                                    df.format(gradePoints) : ""
                            });
                        }
                        
                        // Update summary
                        if (totalCredits > 0) {
                            double gpa = totalGradePoints / totalCredits;
                            gpaLabel.setText(df.format(gpa) + " / 4.0");
                        } else {
                            gpaLabel.setText("N/A");
                        }
                        
                        creditsLabel.setText(String.valueOf(totalCredits));
                        statusLabel.setText("Showing " + enrollments.size() + " enrollments (" + 
                                          completedCourses + " completed)");
                    });
                    
                } catch (Exception e) {
                    logger.error("Error loading transcript", e);
                    SwingUtilities.invokeLater(() -> 
                        statusLabel.setText("Error loading transcript: " + e.getMessage()));
                }
                return null;
            }
        };
        worker.execute();
    }

    private String determineTerm(java.time.LocalDateTime enrolledDate) {
        if (enrolledDate == null) return "Unknown";
        
        int year = enrolledDate.getYear();
        int month = enrolledDate.getMonthValue();
        
        if (month >= 1 && month <= 5) {
            return "Spring " + year;
        } else if (month >= 6 && month <= 8) {
            return "Summer " + year;
        } else {
            return "Fall " + year;
        }
    }

    private double convertLetterToGPA(String letterGrade) {
        if (letterGrade == null) return 0.0;
        switch (letterGrade.toUpperCase()) {
            case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.3;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.3;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.3;
            case "D": return 1.0;
            default: return 0.0;
        }
    }

    private void exportToCSV() {
        // Generate filename with timestamp
        String userHome = System.getProperty("user.home");
        String fileName = "Transcript_" + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        File file = new File(userHome, fileName);
        
        try (FileWriter writer = new FileWriter(file)) {
            // Header info
            writer.write("OFFICIAL ACADEMIC TRANSCRIPT\n");
            writer.write("University ERP System\n\n");
            
            // Student information
            if (currentStudent != null && currentUser != null) {
                writer.write("Student Name," + currentStudent.getFirstName() + " " + 
                           currentStudent.getLastName() + "\n");
                writer.write("Student ID," + currentStudent.getStudentId() + "\n");
                writer.write("Program," + currentStudent.getProgram() + "\n");
                writer.write("Year," + currentStudent.getYear() + "\n");
            }
            writer.write("Generated," + 
                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm")) + "\n\n");
            
            // Academic summary
            writer.write("Cumulative GPA," + gpaLabel.getText() + "\n");
            writer.write("Total Credits," + creditsLabel.getText() + "\n\n");
            
            // Course history header
            for (int i = 0; i < transcriptModel.getColumnCount(); i++) {
                if (i > 0) writer.write(",");
                writer.write(transcriptModel.getColumnName(i));
            }
            writer.write("\n");
            
            // Course history data
            for (int i = 0; i < transcriptModel.getRowCount(); i++) {
                for (int j = 0; j < transcriptModel.getColumnCount(); j++) {
                    if (j > 0) writer.write(",");
                    Object value = transcriptModel.getValueAt(i, j);
                    String cellValue = value != null ? value.toString() : "";
                    // Escape commas in values
                    if (cellValue.contains(",")) {
                        cellValue = "\"" + cellValue + "\"";
                    }
                    writer.write(cellValue);
                }
                writer.write("\n");
            }
            
            JOptionPane.showMessageDialog(this, 
                "Transcript exported to:\n" + file.getAbsolutePath(),
                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                
            statusLabel.setText("Transcript exported to " + file.getName());
            
        } catch (IOException ex) {
            logger.error("Error exporting transcript", ex);
            JOptionPane.showMessageDialog(this, 
                "Error exporting transcript: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}