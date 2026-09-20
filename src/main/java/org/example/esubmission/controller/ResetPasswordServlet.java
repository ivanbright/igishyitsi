package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.service.UserService;

import java.io.IOException;

public class ResetPasswordServlet extends HttpServlet {
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = new UserService(new UserDAO());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getSession().getAttribute("resetToken");
        if (token == null || token.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/forgotPassword");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/pages/resetPassword.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getSession().getAttribute("resetToken");
        if (token == null || token.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/forgotPassword");
            return;
        }
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (password == null || password.length() < 6) {
            request.setAttribute("error", "Password must be at least 6 characters");
            request.getRequestDispatcher("/WEB-INF/pages/resetPassword.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match");
            request.getRequestDispatcher("/WEB-INF/pages/resetPassword.jsp").forward(request, response);
            return;
        }

        try {
            userService.resetPassword(token, password);
            request.getSession().removeAttribute("resetToken");
            request.getSession().removeAttribute("resetEmail");
            request.getSession().setAttribute("success", "Password reset successful! Please login with your new password.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/pages/resetPassword.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "An error occurred. Please try again.");
            request.getRequestDispatcher("/WEB-INF/pages/resetPassword.jsp").forward(request, response);
        }
    }
}
