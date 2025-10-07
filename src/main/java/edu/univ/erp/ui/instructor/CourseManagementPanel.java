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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel for instructors to manage their assigned course sections.
 */
public class CourseManagementPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(CourseManagementPanel.class);

    private final SectionService sectionService = new SectionService();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    
    private JTable sectionsTable;
    private DefaultTableModel sectionsModel;
    private JLabel statusLabel;
    private Instructor currentInstructor;

    public CourseManagementPanel() {
        loadCurrentInstructor();
        initComponents();
        loadSections();
    }

    private void loadCurrentInstructor() {
        try {
            Long userId = SessionManager.getInstance().getCurrentUser().getUserId();
            if (userId == null) {
                logger.warn("User ID is null, cannot load instructor data");
                return;
            }
            currentInstructor = instructorDAO.findByUserId(userId);
            if (currentInstructor == null) {
                logger.warn("No instructor found for user ID: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error loading current instructor", e);
        }
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][grow][]"));

        // Header
        add(new JLabel("<html><h2>My Course Sections</h2></html>"), "wrap");

        // Sections table
        String[] columns = {"Course Code", "Course Title", "Section", "Schedule", "Room", "Enrolled", "Capacity", "Status"};
        sectionsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sectionsTable = new JTable(sectionsModel);
        sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionsTable.setAutoCreateRowSorter(true);
        
        // Set column widths
        sectionsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        sectionsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        sectionsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        sectionsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        sectionsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        sectionsTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        sectionsTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        sectionsTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(sectionsTable);
        add(scrollPane, "grow, wrap");

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0", "[grow][][][][]", ""));
        
        statusLabel = new JLabel("Loading...");
        controlPanel.add(statusLabel, "growx");
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadSections());
        controlPanel.add(refreshBtn);
        
        JButton editBtn = new JButton("Edit Section");
        editBtn.addActionListener(e -> editSelectedSection());
        controlPanel.add(editBtn);
        
        JButton settingsBtn = new JButton("Section Settings");
        settingsBtn.addActionListener(e -> openSectionSettings());
        controlPanel.add(settingsBtn);
        
        JButton exportBtn = new JButton("Export List");
        exportBtn.addActionListener(e -> exportSectionsList());
        controlPanel.add(exportBtn);
        
        add(controlPanel, "growx");
    }

    private void loadSections() {
        if (currentInstructor == null) {
            statusLabel.setText("Instructor data not available");
            return;
        }

        SwingWorker<List<Section>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Section> doInBackground() {
                try {
                    return sectionService.listByInstructor(currentInstructor.getInstructorId());
                } catch (Exception e) {
                    logger.error("Error loading sections", e);
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Section> sections = get();
                    sectionsModel.setRowCount(0);
                    
                    for (Section section : sections) {
                        String schedule = "";
                        if (section.getDayOfWeek() != null && section.getStartTime() != null && section.getEndTime() != null) {
                            schedule = section.getDayOfWeek() + " " + section.getStartTime() + "-" + section.getEndTime();
                        }
                        
                        String room = section.getRoom() != null ? section.getRoom() : "TBA";
                        String status = section.getEnrolled() >= section.getCapacity() ? "Full" : "Open";
                        
                        sectionsModel.addRow(new Object[]{
                            section.getCourseCode(),
                            section.getCourseTitle(),
                            section.getSectionNumber(),
                            schedule,
                            room,
                            section.getEnrolled(),
                            section.getCapacity(),
                            status
                        });
                    }
                    
                    statusLabel.setText("Showing " + sections.size() + " section(s)");
                } catch (Exception e) {
                    logger.error("Failed to load sections", e);
                    statusLabel.setText("Error loading sections: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void editSelectedSection() {
        int selectedRow = sectionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String courseCode = (String) sectionsModel.getValueAt(selectedRow, 0);
        String sectionNumber = (String) sectionsModel.getValueAt(selectedRow, 2);
        
        JOptionPane.showMessageDialog(this, 
            "Section editing for " + courseCode + " - " + sectionNumber + " will be implemented in future version.",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openSectionSettings() {
        int selectedRow = sectionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a section first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(this, 
            "Section settings (capacity limits, enrollment policies, etc.) coming soon!",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportSectionsList() {
        if (sectionsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(this, 
            "Export functionality will be implemented in future version.",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }
}