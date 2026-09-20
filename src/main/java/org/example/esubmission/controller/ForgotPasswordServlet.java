package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.service.EmailService;
import org.example.esubmission.service.UserService;

import java.io.IOException;

public class ForgotPasswordServlet extends HttpServlet {
    private UserService userService;
    private EmailService emailService;

    @Override
    public void init() throws ServletException {
        this.userService = new UserService(new UserDAO());
        this.emailService = new EmailService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/pages/forgotPassword.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");

        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Email is required");
            request.getRequestDispatcher("/WEB-INF/pages/forgotPassword.jsp").forward(request, response);
            return;
        }

        try {
            String token = userService.initiatePasswordReset(email.trim());
            emailService.sendPasswordResetEmail(email.trim(), token, baseUrl(request));
            request.getSession().setAttribute("resetEmail", email.trim());
            response.sendRedirect(request.getContextPath() + "/verifyReset");
        } catch (Exception e) {
            // We don't want to reveal if email exists or not for security, 
            // but for this project we'll show the error if it's explicitly about user not found
            request.setAttribute("success", "If an account exists with this email, a reset link will be sent.");
            request.getRequestDispatcher("/WEB-INF/pages/forgotPassword.jsp").forward(request, response);
        }
    }

    private String baseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = (scheme.equals("http") && port == 80)
            || (scheme.equals("https") && port == 443);
        String base = scheme + "://" + request.getServerName();
        if (!defaultPort) {
            base += ":" + port;
        }
        return base + request.getContextPath();
    }
}
