package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.model.Assignment;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;
import org.example.esubmission.service.AssignmentService;

import java.io.IOException;
import java.util.List;

/**
 * Servlet responsible for displaying the details of a specific assignment.
 * <p>
 * This servlet retrieves assignment information, student submissions, and a list
 * of students who have not yet submitted the assignment. It is primarily used
 * by teachers to monitor assignment progress.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class AssignmentDetailsServlet extends HttpServlet {

    private AssignmentService assignmentService;

    /**
     * Initializes the servlet and its dependencies.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.assignmentService = new AssignmentService(new AssignmentDAO(), new SubmissionDAO(), new StudentClassDAO());
    }

    /**
     * Handles GET requests to view assignment details.
     * <p>
     * Retrieves the assignment by ID, verifies the teacher's ownership, 
     * collects submissions and missing students, and forwards to {@code assignmentDetails.jsp}.
     * </p>
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

        Long assignmentId = Long.parseLong(req.getParameter("id"));
        Assignment assignment = assignmentService.getAssignment(assignmentId);

        if (assignment == null || !assignment.getTeacher().getId().equals(teacher.getId())) {
            resp.sendRedirect(req.getContextPath() + "/teacher/dashboard");
            return;
        }

        List<Submission> submissions = assignmentService.getAssignmentSubmissions(assignmentId);
        List<User> missingStudents = assignmentService.getMissingStudents(assignmentId, assignment.getClassName(), teacher.getId());

        req.setAttribute("assignment", assignment);
        req.setAttribute("submissions", submissions);
        req.setAttribute("missingStudents", missingStudents);

        req.getRequestDispatcher("/WEB-INF/pages/assignmentDetails.jsp").forward(req, resp);
    }
}
