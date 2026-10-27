package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.model.User;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.TenantContext;

import java.io.IOException;

/**
 * Servlet responsible for managing classes within the system.
 * <p>
 * Handles class creation for the signed-in school administrator. The class is
 * always stamped with the active tenant from {@link TenantContext} so a class
 * can never be created into another school.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class ManageClassesServlet extends HttpServlet {

    private ClassGroupDAO classGroupDAO;

    /**
     * Initializes the servlet and its DAO.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.classGroupDAO = new ClassGroupDAO();
    }

    /**
     * Handles POST requests for class management actions.
     * <p>
     * Requires a school administrator session and a valid CSRF token before
     * creating a class.
     * </p>
     *
     * @param req  the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User admin = SecurityUtil.requireAdmin(req);
        if (admin == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }
        if (!SecurityUtil.csrfValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String action = req.getParameter("action");

        if ("create".equals(action)) {
            String className = req.getParameter("name");

            if (className == null || className.trim().isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=invalid_class_name");
                return;
            }

            String trimmed = className.trim();
            if (classGroupDAO.findByName(trimmed) != null) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard?error=duplicate_class_name");
                return;
            }

            classGroupDAO.save(new ClassGroup(trimmed, req.getParameter("description")));
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?success=class_created");
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }
}
