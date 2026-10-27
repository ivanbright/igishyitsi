package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
import org.example.esubmission.util.SecurityUtil;

import java.io.IOException;

/**
 * Servlet responsible for handling the user onboarding process.
 * <p>
 * This servlet lets a newly signed-up user complete their profile
 * (name / phone) before entering the dashboard.
 * </p>
 */
public class OnBoardingServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object userAttribute = session == null ? null : session.getAttribute("user");
        if (!(userAttribute instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User user = (User) userAttribute;
        if (!SecurityUtil.csrfValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        request.setAttribute("user", user);
        request.setAttribute("csrfToken", SecurityUtil.getCsrfToken(request));

        String action = request.getParameter("action");
        if (!"updateProfile".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported onboarding action");
            return;
        }

        String name = request.getParameter("name");
        String phone = request.getParameter("phone");
        if (name == null || name.trim().isEmpty()) {
            request.setAttribute("error", "Name is required");
            request.getRequestDispatcher("/WEB-INF/pages/onboarding.jsp").forward(request, response);
            return;
        }

        user.setName(name.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        userDAO.save(user);
        session.setAttribute("user", user);

        switch (user.getRole()) {
            case SUPER_ADMIN:
                response.sendRedirect(request.getContextPath() + "/super/dashboard");
                break;
            case ADMIN:
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                break;
            case TEACHER:
                response.sendRedirect(request.getContextPath() + "/teacher/dashboard");
                break;
            case PARENT:
                response.sendRedirect(request.getContextPath() + "/profile");
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/student/dashboard");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object userAttribute = session == null ? null : session.getAttribute("user");
        if (!(userAttribute instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setAttribute("user", (User) userAttribute);
        request.setAttribute("csrfToken", SecurityUtil.getCsrfToken(request));
        request.getRequestDispatcher("/WEB-INF/pages/onboarding.jsp").forward(request, response);
    }
}
