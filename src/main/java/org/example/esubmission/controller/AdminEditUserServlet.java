package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.User;
import org.example.esubmission.service.UserService;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.TenantContext;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

@WebServlet("/api/admin/editUser")
public class AdminEditUserServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(AdminEditUserServlet.class.getName());
    private UserService userService;
    private UserDAO userDAO;
    private SchoolMembershipDAO schoolMembershipDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.userService = new UserService(this.userDAO);
        this.schoolMembershipDAO = new SchoolMembershipDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            if (SecurityUtil.requireAdmin(request) == null || !SecurityUtil.csrfValid(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\":false,\"message\":\"Unauthorized\"}");
                return;
            }

            String userIdStr = request.getParameter("userId");
            String name = request.getParameter("name");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            String roleStr = request.getParameter("role");

            if (userIdStr == null || name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty() || roleStr == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Missing required fields\"}");
                return;
            }

            Long userId;
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid user ID\"}");
                return;
            }

            User userToEdit = userDAO.findById(userId);
            if (userToEdit == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\":false,\"message\":\"User not found\"}");
                return;
            }

            // Check if email changed and is already taken
            if (!userToEdit.getEmail().equals(email)) {
                User existingWithEmail = userDAO.findByEmail(email);
                if (existingWithEmail != null && !existingWithEmail.getId().equals(userId)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Email is already in use\"}");
                    return;
                }
            }

            UserRole role;
            try {
                role = UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid role\"}");
                return;
            }
            if (role == UserRole.SUPER_ADMIN) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"School administrators cannot assign the platform administrator role\"}");
                return;
            }
            if (userToEdit.getId().equals(SecurityUtil.requireAdmin(request).getId())
                    && role != SecurityUtil.effectiveRole(SecurityUtil.requireAdmin(request))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"You cannot change your own school role\"}");
                return;
            }

            userToEdit.setName(name.trim());
            userToEdit.setEmail(email.trim());
            userToEdit.setPhone(phone != null ? phone.trim() : null);
            userDAO.save(userToEdit);

            SchoolMembership membership = schoolMembershipDAO.findByUserAndSchool(userId, TenantContext.requireSchoolId());
            if (membership == null) {
                throw new IllegalStateException("School membership not found");
            }
            membership.setRole(role);
            schoolMembershipDAO.save(membership);

            out.print("{\"success\":true,\"message\":\"User updated successfully\"}");
        } catch (Exception e) {
            logger.severe("Error updating user: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"An internal error occurred\"}");
        }
    }
}
