package org.example.esubmission.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.model.User;
import org.example.esubmission.util.JwtUtil;
import org.example.esubmission.util.SecurityUtil;

import java.io.IOException;

/**
 * Filter responsible for verifying user authentication.
 * <p>
 * This filter checks for a valid session and redirects unauthenticated users
 * to the login page, except for public resources and login-related endpoints.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class AuthenticationFilter implements Filter {

    /**
     * Filters requests to ensure the user is authenticated.
     * <p>
     * Allows access to public paths (login, signup, static assets) and checks
     * for a valid "user" attribute in the session for other paths.
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
        
        // Allow access to public pages and static resources
        if (path.equals("/") || path.equals("/index.jsp") || 
            path.endsWith("login.jsp") || path.endsWith("signup.jsp") || 
            path.contains("/login") || path.contains("/signup") || path.contains("/logout") ||
            path.contains("/css/") || path.contains("/js/") || path.contains("/images/") ||
            path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check for valid session
        HttpSession session = httpRequest.getSession(false);
        
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            if (user.isDisabled()) {
                session.invalidate();
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?message=account_disabled");
                return;
            }
            // Authenticated pages embed the session CSRF token. Login rotates the
            // session, so a cached page would present a token from a destroyed
            // session. Disable caching for every authenticated response.
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            httpResponse.setHeader("Pragma", "no-cache");
            // Establish a CSRF token for admin state-changing endpoints.
            SecurityUtil.getCsrfToken(httpRequest);
            chain.doFilter(request, response);
        } else {
            // No valid session, redirect to login
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
        }
    }
    
    /**
     * Extracts a token value from the request cookies.
     *
     * @param request    the HttpServletRequest
     * @param cookieName the name of the cookie to find
     * @return the cookie value, or {@code null} if not found
     */
    private String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
