package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.SchoolDAO;
import org.example.esubmission.model.School;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.TenantContext;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets a school administrator view and edit their own school's identity.
 * <p>
 * A tenant can edit its own profile, but never another tenant's: the school is
 * always resolved from the active tenant rather than from a request parameter,
 * so a school id supplied by the client is ignored. Plan, slug and active state
 * stay under platform control and are not writable here.
 * </p>
 * <p>
 * Endpoints:
 * <ul>
 *   <li>GET  /api/admin/school       - the current school's profile</li>
 *   <li>POST /api/admin/school?action=update - update the editable fields</li>
 * </ul>
 * </p>
 */
public class SchoolSettingsApiServlet extends HttpServlet {

    private SchoolDAO schoolDAO;

    @Override
    public void init() throws ServletException {
        this.schoolDAO = new SchoolDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (SecurityUtil.requireAdmin(request) == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }

        School school = currentSchool();
        if (school == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "No active school");
            return;
        }
        JsonUtil.MAPPER.writeValue(response.getWriter(), success(profile(school)));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (SecurityUtil.requireAdmin(request) == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }
        if (!SecurityUtil.csrfValid(request)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String action = request.getParameter("action");
        if (!"update".equals(action)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
            return;
        }

        School school = currentSchool();
        if (school == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "No active school");
            return;
        }

        try {
            String name = trimmed(request.getParameter("name"));
            if (name == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "School name is required");
                return;
            }
            String email = trimmed(request.getParameter("email"));
            if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Enter a valid email address");
                return;
            }
            String logoUrl = trimmed(request.getParameter("logoUrl"));
            if (logoUrl != null && !logoUrl.matches("^https?://\\S+$")) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Logo URL must start with http:// or https://");
                return;
            }

            school.setName(name);
            school.setDomain(trimmed(request.getParameter("domain")));
            school.setAddress(trimmed(request.getParameter("address")));
            school.setPhone(trimmed(request.getParameter("phone")));
            school.setEmail(email);
            school.setLogoUrl(logoUrl);
            school.setDescription(trimmed(request.getParameter("description")));

            School saved = schoolDAO.save(school);

            Map<String, Object> result = profile(saved);
            result.put("message", "School profile updated");
            JsonUtil.MAPPER.writeValue(response.getWriter(), success(result));
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not update the school profile");
        }
    }

    private School currentSchool() {
        Long schoolId = TenantContext.getSchoolId();
        return schoolId == null ? null : schoolDAO.findById(schoolId);
    }

    private static Map<String, Object> profile(School school) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", school.getId());
        map.put("name", school.getName());
        map.put("slug", school.getSlug());
        map.put("domain", school.getDomain());
        map.put("address", school.getAddress());
        map.put("phone", school.getPhone());
        map.put("email", school.getEmail());
        map.put("logoUrl", school.getLogoUrl());
        map.put("description", school.getDescription());
        map.put("plan", school.getPlan());
        return map;
    }

    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, Object> success(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.putAll(data);
        return result;
    }

    private static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        JsonUtil.MAPPER.writeValue(response.getWriter(), error);
    }
}
