package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
import org.example.esubmission.service.UserService;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

@WebServlet("/api/admin/addUser")
public class AdminAddUserServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(AdminAddUserServlet.class.getName());
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = new UserService(new UserDAO());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            User admin = (User) request.getSession().getAttribute("user");
            if (admin == null || admin.getRole() != UserRole.ADMIN || !SecurityUtil.csrfValid(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\":false,\"message\":\"Unauthorized\"}");
                return;
            }

            String name = request.getParameter("name");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            String password = request.getParameter("password");
            String roleStr = request.getParameter("role");

            if (name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.length() < 6 ||
                roleStr == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Missing or invalid required fields (password must be >= 6 chars)\"}");
                return;
            }

            UserRole role;
            try {
                role = UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid role\"}");
                return;
            }

            User newUser = userService.createUser(name.trim(), email.trim(), phone != null ? phone.trim() : null, password, role);
            // Auto-verify user created by admin to save time
            newUser.setEmailVerified(true);
            new UserDAO().save(newUser);

            out.print("{\"success\":true,\"message\":\"User created successfully\",\"userId\":" + newUser.getId() + "}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.severe("Error creating user: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"An internal error occurred\"}");
        }
    }
}
