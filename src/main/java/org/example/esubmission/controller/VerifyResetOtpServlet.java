package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
import org.example.esubmission.service.UserService;

import java.io.IOException;
import java.time.LocalDateTime;

public class VerifyResetOtpServlet extends HttpServlet {
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = new UserService(new UserDAO());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String resetEmail = (String) session.getAttribute("resetEmail");

        if (resetEmail == null) {
            response.sendRedirect(request.getContextPath() + "/forgotPassword");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/pages/verifyResetOtp.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String resetEmail = (String) session.getAttribute("resetEmail");

        if (resetEmail == null) {
            response.sendRedirect(request.getContextPath() + "/forgotPassword");
            return;
        }

        String code = request.getParameter("code");

        if (code == null || code.trim().length() != 6) {
            request.setAttribute("error", "Please enter a valid 6-digit code.");
            request.getRequestDispatcher("/WEB-INF/pages/verifyResetOtp.jsp").forward(request, response);
            return;
        }

        try {
            User user = userService.getUserByEmail(resetEmail);
            if (user.getResetToken() != null && user.getResetToken().equals(code.trim())) {
                if (user.getResetTokenExpiry() != null && user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                    // Valid OTP! Store it as token in session to use in ResetPasswordServlet
                    session.setAttribute("resetToken", code.trim());
                    response.sendRedirect(request.getContextPath() + "/resetPassword");
                    return;
                } else {
                    request.setAttribute("error", "Your code has expired. Please request a new one.");
                }
            } else {
                request.setAttribute("error", "Invalid verification code.");
            }
        } catch (Exception e) {
            request.setAttribute("error", "An error occurred.");
        }
        
        request.getRequestDispatcher("/WEB-INF/pages/verifyResetOtp.jsp").forward(request, response);
    }
}
