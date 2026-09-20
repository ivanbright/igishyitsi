package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.model.User;

import java.io.IOException;
import java.util.List;

/**
 * Servlet responsible for displaying students enrolled in a specific class.
 * <p>
 * This servlet retrieves student information based on the class and teacher,
 * and forwards the data to {@code viewStudents.jsp}.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class ViewStudentsServlet extends HttpServlet {

    private StudentClassDAO studentClassDAO;

    /**
     * Initializes the servlet and its DAO.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.studentClassDAO = new StudentClassDAO();
    }

    /**
     * Handles GET requests to list students in a class.
     *
     * @param req  the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User teacher = (User) session.getAttribute("user");

        String className = req.getParameter("class");
        List<User> students = studentClassDAO.findStudentsByTeacherAndClass(teacher.getId(), className);

        req.setAttribute("className", className);
        req.setAttribute("students", students);

        req.getRequestDispatcher("/WEB-INF/pages/viewStudents.jsp").forward(req, resp);
    }
}
