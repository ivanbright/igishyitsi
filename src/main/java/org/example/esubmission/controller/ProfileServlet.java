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
import org.example.esubmission.util.PasswordUtils;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.TenantContext;
import org.example.esubmission.util.TenantView;

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

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(ProfileServlet.class.getName());

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
        
        HttpSession session = request.getSession(false);
        Object userAttribute = session == null ? null : session.getAttribute("user");
        if (!(userAttribute instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User student = (User) userAttribute;

        try {
            List<Submission> submissions = List.of();
            if (TenantContext.getSchoolId() != null) {
                submissions = submissionDAO.findByStudentId(student.getId());
            }

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
            request.setAttribute("csrfToken", SecurityUtil.getCsrfToken(request));
            request.setAttribute("submissions", submissions);
            request.setAttribute("totalSubmissions", totalSubmissions);
            request.setAttribute("gradedSubmissions", gradedSubmissions);
            request.setAttribute("averageGrade", averageGrade);
            TenantView.apply(request);
            
            request.getRequestDispatcher("/WEB-INF/pages/profile.jsp").forward(request, response);
            
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error loading profile", e);
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
        
        HttpSession session = request.getSession(false);
        Object userAttribute = session == null ? null : session.getAttribute("user");
        if (!(userAttribute instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User student = (User) userAttribute;
        if (!SecurityUtil.csrfValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
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
            } else if ("changePassword".equals(action)) {
                String currentPassword = request.getParameter("currentPassword");
                String newPassword = request.getParameter("newPassword");
                String confirmPassword = request.getParameter("confirmPassword");

                String error = validatePasswordChange(currentPassword, newPassword, confirmPassword, student);
                if (error != null) {
                    session.setAttribute("error", error);
                } else {
                    student.setPassword(PasswordUtils.hashPassword(newPassword));
                    userDAO.save(student);
                    session.setAttribute("user", student);
                    session.setAttribute("message", "Password changed successfully!");
                }
            }
            
            response.sendRedirect(request.getContextPath() + "/profile");
            
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error updating profile", e);
            response.sendError(500, "Error updating profile");
        }
    }

    /**
     * Validates a password change request, returning an error message or
     * {@code null} when the change may proceed.
     */
    private String validatePasswordChange(String currentPassword, String newPassword, String confirmPassword,
                                          User user) {
        if (currentPassword == null || !PasswordUtils.checkPassword(currentPassword, user.getPassword())) {
            return "Current password is incorrect";
        }
        if (newPassword == null || newPassword.length() < 8) {
            return "New password must be at least 8 characters";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "New passwords do not match";
        }
        if (newPassword.equals(currentPassword)) {
            return "New password must be different from the current password";
        }
        return null;
    }
}
