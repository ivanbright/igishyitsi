package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
import org.example.esubmission.service.EmailService;
import org.example.esubmission.service.UserService;
import org.example.esubmission.util.JwtUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Servlet responsible for user authentication and session management.
 * <p>
 * This servlet handles login requests, validates credentials using {@link UserService},
 * generates JWT tokens (access and refresh), and redirects users to their
 * respective dashboards based on their role.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class LoginServlet extends HttpServlet {

    private UserService userService;
    private EmailService emailService;

    @Override
    public void init() throws ServletException {
        this.userService = new UserService(new UserDAO());
        this.emailService = new EmailService();
    }

    /**
     * Handles POST requests for user login.
     * <p>
     * Validates input, authenticates the user, sets JWT cookies, and redirects
     * to the appropriate dashboard.
     * </p>
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String captchaInput = request.getParameter("captcha");

        // Captcha validation
        HttpSession session = request.getSession();
        String captchaStored = (String) session.getAttribute("captcha");

        if (captchaInput == null || !captchaInput.equalsIgnoreCase(captchaStored)) {
            request.setAttribute("error", "Invalid security code. Please try again.");
            request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
            return;
        }

        // Input validation
        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Email is required");
            request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Password is required");
            request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
            return;
        }

        try {
            // Authenticate user
            User user = userService.authenticateUser(email.trim(), password);

            // Reject disabled accounts
            if (user.isDisabled()) {
                request.setAttribute("error", "Your account has been disabled. Please contact the administrator.");
                request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
                return;
            }

            // Check if 2FA or Email Verification is required
            if (user.isTwoFactorEnabled() || !user.isEmailVerified()) {
                String code = userService.generateVerificationCode(user);
                emailService.sendVerificationEmail(user.getEmail(), code);
                
                session.setAttribute("pendingUser", user);
                response.sendRedirect(request.getContextPath() + "/verify");
                return;
            }

            // Standard login flow (2FA not required)
            completeLogin(user, request, response);

        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Login failed. Please try again.");
            request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
        }
    }

    /**
     * Completes the login process by generating tokens and redirecting to dashboard.
     */
    public static void completeLogin(User user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Rotate session to prevent session fixation
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);

        String accessToken = JwtUtil.generateAccessToken(user);
        String refreshToken = JwtUtil.generateRefreshToken(user);

        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60); // 1 hour
        if (request.isSecure()) accessTokenCookie.setSecure(true);
        accessTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7); // 7 days
        if (request.isSecure()) refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshTokenCookie);

        session.setAttribute("user", user);
        session.setAttribute("accessToken", accessToken);

        // Redirect based on role
        String contextPath = request.getContextPath();
        UserRole effectiveRole = SecurityUtil.primaryRole(user);
        if (effectiveRole == null) {
            response.sendRedirect(contextPath + "/onboarding");
            return;
        }
        switch (effectiveRole) {
            case SUPER_ADMIN:
                response.sendRedirect(contextPath + "/super/dashboard");
                break;
            case ADMIN:
                response.sendRedirect(contextPath + "/admin/dashboard");
                break;
            case TEACHER:
                response.sendRedirect(contextPath + "/teacher/dashboard");
                break;
            case STUDENT:
                response.sendRedirect(contextPath + "/student/dashboard");
                break;
            case PARENT:
                // PARENT accounts have no dashboard yet; land them on their profile page
                response.sendRedirect(contextPath + "/profile");
                break;
            default:
                response.sendRedirect(contextPath + "/");
        }
    }

    /**
     * Handles GET requests to display the login page.
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String success = (String) session.getAttribute("success");
        if (success != null) {
            request.setAttribute("success", success);
            session.removeAttribute("success");
        }

        if ("account_disabled".equals(request.getParameter("message"))) {
            request.setAttribute("error", "Your account has been disabled. Please contact the administrator.");
        }

        request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
    }
}
