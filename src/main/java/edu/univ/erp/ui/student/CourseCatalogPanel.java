package edu.univ.erp.ui.student;

import edu.univ.erp.domain.Course;
import edu.univ.erp.service.CourseService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * Panel to display the course catalog with simple search.
 */
public class CourseCatalogPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(CourseCatalogPanel.class);

    private final CourseService courseService = new CourseService();
    private JTable table;
    private JTextField searchField;
    private DefaultTableModel model;

    public CourseCatalogPanel() {
        initComponents();
        loadCourses(null);
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[][grow][]"));

        // Search bar
        JPanel searchPanel = new JPanel(new MigLayout("insets 0", "[][grow][]", ""));
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField();
        searchPanel.add(searchField, "growx");
        JButton searchBtn = new JButton("Go");
        searchBtn.addActionListener(e -> loadCourses(searchField.getText()));
        searchPanel.add(searchBtn);
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> { searchField.setText(""); loadCourses(null); });
        searchPanel.add(resetBtn, "gapx 10");
        add(searchPanel, "growx, wrap");

        // Table
        model = new DefaultTableModel(new Object[]{"Code", "Title", "Credits", "Department"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), "grow, wrap");

        // Status bar
        JLabel status = new JLabel("Ready");
        add(status, "growx");
    }

    private void loadCourses(String query) {
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() {
                if (query == null || query.isBlank()) {
                    return courseService.listAll();
                }
                return courseService.search(query);
            }

            @Override
            protected void done() {
                try {
                    List<Course> courses = get();
                    model.setRowCount(0);
                    for (Course c : courses) {
                        model.addRow(new Object[]{c.getCode(), c.getTitle(), c.getCredits(), c.getDepartment()});
                    }
                } catch (Exception e) {
                    logger.error("Failed to load courses", e);
                    JOptionPane.showMessageDialog(CourseCatalogPanel.this,
                            "Failed to load courses: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
