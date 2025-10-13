package edu.univ.erp.service;

import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.domain.Section;

import java.sql.SQLException;
import java.util.List;

public class SectionService {
    private final SectionDAO sectionDAO = new SectionDAO();

    public Section get(Long id) throws SQLException { return sectionDAO.findById(id); }

    public List<Section> listByCourse(Long courseId, String semester, Integer year) { return sectionDAO.listByCourse(courseId, semester, year); }

    public List<Section> listOpen(String semester, int year) { return sectionDAO.listOpenSections(semester, year); }

    public List<Section> listByInstructor(Long instructorId) throws SQLException { 
        return sectionDAO.listByInstructor(instructorId); 
    }

    public List<Section> listAllSections() throws SQLException {
        return sectionDAO.findAll();
    }
}
