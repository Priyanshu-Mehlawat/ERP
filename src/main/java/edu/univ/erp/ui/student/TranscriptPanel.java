package edu.univ.erp.ui.student;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.EnrollmentService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel for viewing and exporting academic transcript.
 */
public class TranscriptPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptPanel.class);

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
            currentStudent = studentDAO.findByUserId(currentUser.getUserId());
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
        
        JButton exportBtn = new JButton("Export to HTML");
        exportBtn.addActionListener(new ExportActionListener());
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
                            int credits = 3; // Default credits per course
                            
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

    private class ExportActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Transcript");
            fileChooser.setSelectedFile(new File("transcript.html"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "HTML files", "html"));
            
            int userSelection = fileChooser.showSaveDialog(TranscriptPanel.this);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                exportToHTML(fileToSave);
            }
        }
    }

    private void exportToHTML(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            // Write HTML header
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n<head>\n");
            writer.write("<title>Official Transcript</title>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
            writer.write("th { background-color: #f2f2f2; }\n");
            writer.write(".header { text-align: center; margin-bottom: 30px; }\n");
            writer.write(".info { margin: 20px 0; }\n");
            writer.write(".summary { margin: 20px 0; background-color: #f9f9f9; padding: 15px; }\n");
            writer.write("</style>\n</head>\n<body>\n");
            
            // Header
            writer.write("<div class='header'>\n");
            writer.write("<h1>University ERP System</h1>\n");
            writer.write("<h2>Official Academic Transcript</h2>\n");
            writer.write("</div>\n");
            
            // Student information
            writer.write("<div class='info'>\n");
            writer.write("<h3>Student Information</h3>\n");
            if (currentStudent != null && currentUser != null) {
                writer.write("<p><strong>Name:</strong> " + currentStudent.getFirstName() + " " + 
                           currentStudent.getLastName() + "</p>\n");
                writer.write("<p><strong>Student ID:</strong> " + currentStudent.getStudentId() + "</p>\n");
                writer.write("<p><strong>Program:</strong> " + currentStudent.getProgram() + "</p>\n");
                writer.write("<p><strong>Year:</strong> " + currentStudent.getYear() + "</p>\n");
            }
            writer.write("<p><strong>Generated:</strong> " + 
                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm")) + "</p>\n");
            writer.write("</div>\n");
            
            // Academic summary
            writer.write("<div class='summary'>\n");
            writer.write("<h3>Academic Summary</h3>\n");
            writer.write("<p><strong>Cumulative GPA:</strong> " + gpaLabel.getText() + "</p>\n");
            writer.write("<p><strong>Total Credits:</strong> " + creditsLabel.getText() + "</p>\n");
            writer.write("</div>\n");
            
            // Course history table
            writer.write("<h3>Course History</h3>\n");
            writer.write("<table>\n");
            writer.write("<tr>");
            for (int i = 0; i < transcriptModel.getColumnCount(); i++) {
                writer.write("<th>" + transcriptModel.getColumnName(i) + "</th>");
            }
            writer.write("</tr>\n");
            
            for (int i = 0; i < transcriptModel.getRowCount(); i++) {
                writer.write("<tr>");
                for (int j = 0; j < transcriptModel.getColumnCount(); j++) {
                    Object value = transcriptModel.getValueAt(i, j);
                    writer.write("<td>" + (value != null ? value.toString() : "") + "</td>");
                }
                writer.write("</tr>\n");
            }
            writer.write("</table>\n");
            
            // Footer
            writer.write("<div style='margin-top: 50px; text-align: center; font-size: 12px; color: #666;'>\n");
            writer.write("<p>This is an official transcript generated by the University ERP System</p>\n");
            writer.write("</div>\n");
            
            writer.write("</body>\n</html>");
            
            JOptionPane.showMessageDialog(this, 
                "Transcript exported successfully to:\n" + file.getAbsolutePath(),
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