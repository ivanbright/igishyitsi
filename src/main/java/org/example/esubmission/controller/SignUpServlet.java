package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
import org.example.esubmission.service.EmailService;
import org.example.esubmission.service.UserService;

import java.io.IOException;

/**
 * Servlet responsible for user registration.
 * <p>
 * This servlet handles account creation requests, performs input validation,
 * and uses {@link UserService} to persist new users in the system.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class SignUpServlet extends HttpServlet {

    private UserService userService;
    private EmailService emailService;
    /**
     * Initializes the servlet and its service.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.userService = new UserService(new UserDAO());
        this.emailService = new EmailService();
    }

    /**
     * Handles POST requests for user sign-up.
     * <p>
     * Validates user input, creates a new user account, and redirects
     * to the login page upon success.
     * </p>
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String phone = request.getParameter("phone");
        String captchaInput = request.getParameter("captcha");

        String name = (firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "");
        name = name.trim();

        // Captcha validation
        HttpSession session = request.getSession();
        String captchaStored = (String) session.getAttribute("captcha");

        if (captchaInput == null || !captchaInput.equalsIgnoreCase(captchaStored)) {
            request.setAttribute("error", "Invalid security code. Please try again.");
            request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
            return;
        }

        // Input validation
        if (name == null || name.trim().isEmpty()) {
            request.setAttribute("error", "Name is required");
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Email is required");
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
            return;
        }

        if (password == null || password.length() < 6) {
            request.setAttribute("error", "Password must be at least 6 characters");
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match");
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
            return;
        }

        try {
            User user = userService.createUser(name.trim(), email.trim(), phone != null ? phone.trim() : null, password);

            // Account starts unverified: generate a code, email it, then take the user
            // through the /verify flow before their first login.
            String code = userService.generateVerificationCode(user);
            emailService.sendVerificationEmail(user.getEmail(), code);

            session.setAttribute("pendingUser", user);
            response.sendRedirect(request.getContextPath() + "/verify");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Registration failed. Please try again.");
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
        }
    }

    /**
     * Handles GET requests to display the sign-up page.
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/pages/signup.jsp").forward(request, response);
    }
}
