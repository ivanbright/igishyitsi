package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.SchoolDAO;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.model.School;
import org.example.esubmission.util.AdminProjections;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders the platform administration dashboard where a {@code SUPER_ADMIN}
 * manages schools (tenants) and their subscription plans.
 */
public class SuperAdminServlet extends HttpServlet {

    private SchoolDAO schoolDAO;
    private SchoolMembershipDAO schoolMembershipDAO;

    @Override
    public void init() throws ServletException {
        this.schoolDAO = new SchoolDAO();
        this.schoolMembershipDAO = new SchoolMembershipDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SecurityUtil.requireSuperAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Platform admins only");
            return;
        }

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        try {
            List<School> schools = schoolDAO.findAll();

            long totalMembers = 0;
            for (School school : schools) {
                totalMembers += schoolMembershipDAO.findBySchoolId(school.getId()).size();
            }

            List<Map<String, Object>> schoolList = schools.stream()
                    .map(s -> AdminProjections.schoolBasic(s,
                            schoolMembershipDAO.findBySchoolId(s.getId()).size()))
                    .collect(Collectors.toList());

            Map<String, Object> stats = new HashMap<>();
            stats.put("schools", (long) schools.size());
            stats.put("totalMembers", totalMembers);

            Map<String, Object> realData = new HashMap<>();
            realData.put("stats", stats);
            realData.put("schools", schoolList);
            realData.put("currentUser", AdminProjections.userBasic(SecurityUtil.requireSuperAdmin(request)));
            realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

            request.setAttribute("csrfToken", SecurityUtil.getCsrfToken(request));
        request.setAttribute("realData", JsonUtil.forScriptEmbedding(JsonUtil.MAPPER.writeValueAsString(realData)));
            request.getRequestDispatcher("/WEB-INF/pages/superAdmin.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading platform dashboard");
        }
    }
}