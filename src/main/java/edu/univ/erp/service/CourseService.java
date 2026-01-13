package edu.univ.erp.service;

import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.domain.Course;

import java.util.List;

/**
 * Service layer for course operations (business rules can be added later).
 */
public class CourseService {
    private final CourseDAO courseDAO = new CourseDAO();

    public List<Course> listAll() {
        return courseDAO.findAll();
    }

    public List<Course> search(String query) {
        return courseDAO.search(query);
    }
}
