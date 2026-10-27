package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.StudentClass;
import org.example.esubmission.model.User;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;

/**
 * Servlet responsible for allocating a student to a class under a teacher.
 * <p>
 * Handles POST requests that contain student ID, teacher ID, and class name.
 * Creates a {@link StudentClass} entity and saves it using {@link StudentClassDAO}.
 * Redirects to the admin dashboard with success or error messages.
 * </p>
 *
 * Example parameters expected in the request:
 * <ul>
 *     <li>studentId - ID of the student to allocate</li>
 *     <li>teacherId - ID of the teacher responsible for the class</li>
 *     <li>className - Name of the class</li>
 * </ul>
 *
 * Possible redirect query parameters:
 * <ul>
 *     <li>?success=allocated - Allocation successful</li>
 *     <li>?error=invalid_data - Invalid request parameters</li>
 *     <li>?error=allocation_failed - Allocation failed due to exception</li>
 * </ul>
 *
 * @author Mugisha Ivan Bright
 */
public class AllocateStudentServlet extends HttpServlet {

    /**
     * DAO for managing student-class assignments.
     */
    private StudentClassDAO studentClassDAO;

    /**
     * DAO for accessing user data (students and teachers).
     */
    private UserDAO userDAO;

    /**
     * Initializes the servlet and DAO instances.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.studentClassDAO = new StudentClassDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Handles POST requests to allocate a student to a class under a teacher.
     * <p>
     * Reads studentId, teacherId, and className from request parameters.
     * If valid, creates and saves a {@link StudentClass} entity.
     * Redirects to the admin dashboard with success or error messages.
     * </p>
     *
     * @param req  the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = SecurityUtil.requireAdmin(req);
        if (admin == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }
        if (!SecurityUtil.csrfValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        try {
            Long studentId = Long.parseLong(req.getParameter("studentId"));
            Long teacherId = Long.parseLong(req.getParameter("teacherId"));
            String className = req.getParameter("className");

            User student = userDAO.findById(studentId);
            User teacher = userDAO.findById(teacherId);

            if (student != null && teacher != null && className != null) {
                StudentClass studentClass = new StudentClass();
                studentClass.setStudent(student);
                studentClass.setTeacher(teacher);
                studentClass.setClassName(className);

                studentClassDAO.save(studentClass);
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard?success=allocated");
            } else {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=invalid_data");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=allocation_failed");
        }
    }
}
