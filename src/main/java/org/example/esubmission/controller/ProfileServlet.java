package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;

import java.io.IOException;
import java.util.List;

/**
 * Servlet responsible for displaying and updating user profiles.
 * <p>
 * This servlet calculates profile statistics (total submissions, graded 
 * submissions, and average grade) for display on the profile page. 
 * It also handles profile information updates.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class ProfileServlet extends HttpServlet {
    
    private SubmissionDAO submissionDAO;
    private UserDAO userDAO;

    /**
     * Initializes the servlet and its DAO.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.submissionDAO = new SubmissionDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Handles GET requests to view the user profile.
     * <p>
     * Retrieves the user's submissions, computes statistics, and forwards to {@code profile.jsp}.
     * </p>
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User student = (User) session.getAttribute("user");

        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            List<Submission> submissions = submissionDAO.findByStudentId(student.getId());
            
            // Calculate profile statistics
            int totalSubmissions = submissions.size();
            int gradedSubmissions = (int) submissions.stream()
                .filter(s -> s.getGrade() != null)
                .count();
            
            double averageGrade = submissions.stream()
                .filter(s -> s.getGrade() != null)
                .mapToInt(Submission::getGrade)
                .average()
                .orElse(0.0);
            
            request.setAttribute("user", student);
            request.setAttribute("submissions", submissions);
            request.setAttribute("totalSubmissions", totalSubmissions);
            request.setAttribute("gradedSubmissions", gradedSubmissions);
            request.setAttribute("averageGrade", averageGrade);
            
            request.getRequestDispatcher("/WEB-INF/pages/profile.jsp").forward(request, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading profile");
        }
    }

    /**
     * Handles POST requests for profile updates.
     * <p>
     * Updates user information such as name and phone number and redirects
     * back to the profile page.
     * </p>
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User student = (User) session.getAttribute("user");

        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String action = request.getParameter("action");
            
            if ("updateProfile".equals(action)) {
                String name = request.getParameter("name");
                String phone = request.getParameter("phone");
                
                // Update user information
                student.setName(name);
                student.setPhone(phone);
                userDAO.save(student);

                // Update session
                session.setAttribute("user", student);
                
                session.setAttribute("message", "Profile updated successfully!");
            }
            
            response.sendRedirect(request.getContextPath() + "/profile");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error updating profile");
        }
    }
}
