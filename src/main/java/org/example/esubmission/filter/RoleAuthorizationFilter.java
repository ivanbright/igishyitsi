package org.example.esubmission.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.model.User;
import org.example.esubmission.util.UserRole;

import java.io.IOException;

/**
 * Filter responsible for role-based authorization.
 * <p>
 * This filter enforces access control by checking the user's role against 
 * the requested path. It ensures that only authorized users can access 
 * admin, teacher, or student-specific areas.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class RoleAuthorizationFilter implements Filter {

    /**
     * Filters requests to enforce role-based access control.
     * <p>
     * Checks the request URI and compares it with the user's role from the session.
     * Forwards to an error page (403 Forbidden) if the user lacks the required role.
     * </p>
     *
     * @param request  the ServletRequest
     * @param response the ServletResponse
     * @param chain    the FilterChain
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getServletPath();
        HttpSession session = httpRequest.getSession(false);
        
        // Allow static resources and root/index to bypass role check
        if (path.equals("/") || path.equals("/index.jsp") ||
            path.contains("/css/") || path.contains("/js/") || path.contains("/images/") ||
            path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }

        // If no session, let AuthenticationFilter handle it
        if (session == null || session.getAttribute("user") == null) {
            chain.doFilter(request, response);
            return;
        }
        
        User user = (User) session.getAttribute("user");
        UserRole userRole = user.getRole();

        // PARENT accounts can view their own profile but no dashboards yet
        if (path.equals("/profile") && userRole == UserRole.PARENT) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check role-based access
        if (path.contains("/admin/") && userRole != UserRole.ADMIN) {
            httpRequest.setAttribute("errorMessage", "Admin access required");
            httpRequest.getRequestDispatcher("/error403.jsp").forward(request, response);
            return;
        }
        
        if (path.contains("/teacher/") && userRole != UserRole.TEACHER) {
            httpRequest.setAttribute("errorMessage", "Teacher access required");
            httpRequest.getRequestDispatcher("/error403.jsp").forward(request, response);
            return;
        }
        
        if ((path.contains("/student/") || path.contains("/studentDash") || 
             path.contains("/grades") || path.contains("/mySubmissions") || 
             path.contains("/profile") || path.contains("/submitAssignment") || 
             path.contains("/submissionDetails")) && userRole != UserRole.STUDENT) {
            httpRequest.setAttribute("errorMessage", "Student access required");
            httpRequest.getRequestDispatcher("/error403.jsp").forward(request, response);
            return;
        }
        
        // Role matches, proceed
        chain.doFilter(request, response);
    }
}
