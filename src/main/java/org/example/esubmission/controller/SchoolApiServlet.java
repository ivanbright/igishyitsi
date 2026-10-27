package org.example.esubmission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.SchoolDAO;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.School;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.User;
import org.example.esubmission.service.NotificationService;
import org.example.esubmission.service.UserService;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * JSON API for the platform administration surface.
 * <p>
 * Every handler enforces a live {@code SUPER_ADMIN} session AND a valid CSRF
 * token ({@code X-CSRF-Token} header) before mutating state.
 * </p>
 * <ul>
 *   <li>GET  /api/super/schools           - list schools</li>
 *   <li>POST /api/super/schools?action=create  - create a school</li>
 *   <li>POST /api/super/schools?action=update  - rename/set plan/domain</li>
 *   <li>POST /api/super/schools?action=toggle  - activate/deactivate</li>
 *   <li>POST /api/super/schools?action=delete  - delete an empty school</li>
 *   <li>POST /api/super/schools?action=addAdmin - add an administrator to a school</li>
 * </ul>
 * <p>
 * Creating a school also creates that school's own first administrator, so
 * each tenant is run by its own {@code ADMIN} account. Platform administrators
 * remain tenant-less and administer schools from this portal only.
 * </p>
 */
public class SchoolApiServlet extends HttpServlet {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Set<String> PLANS = Set.of("FREE", "PLUS", "PREMIUM");
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(SchoolApiServlet.class.getName());

    private SchoolDAO schoolDAO;
    private SchoolMembershipDAO schoolMembershipDAO;
    private UserDAO userDAO;
    private NotificationService notificationService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        this.schoolDAO = new SchoolDAO();
        this.schoolMembershipDAO = new SchoolMembershipDAO();
        this.userDAO = new UserDAO();
        this.notificationService = new NotificationService();
        this.objectMapper = JsonUtil.MAPPER;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (SecurityUtil.requireSuperAdmin(request) == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }

        List<Map<String, Object>> schools = new java.util.ArrayList<>();
        for (School s : schoolDAO.findAll()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("slug", s.getSlug());
            map.put("domain", s.getDomain());
            map.put("plan", s.getPlan());
            map.put("active", s.isActive());
            map.put("memberCount", schoolMembershipDAO.findBySchoolId(s.getId()).size());
            schools.add(map);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("schools", schools);
        objectMapper.writeValue(response.getWriter(), res);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (SecurityUtil.requireSuperAdmin(request) == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }
        if (!SecurityUtil.csrfValid(request)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("create".equals(action)) {
                handleCreate(request, response);
            } else if ("update".equals(action)) {
                handleUpdate(request, response);
            } else if ("toggle".equals(action)) {
                handleToggle(request, response);
            } else if ("delete".equals(action)) {
                handleDelete(request, response);
            } else if ("addAdmin".equals(action)) {
                handleAddAdmin(request, response);
            } else {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
            }
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric value");
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String slug = request.getParameter("slug");

        if (name == null || name.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "School name is required");
            return;
        }
        String trimmedName = name.trim();

        String normalizedSlug;
        if (slug == null || slug.trim().isEmpty()) {
            normalizedSlug = uniqueSlugFromName(trimmedName);
        } else {
            normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
            if (!SLUG_PATTERN.matcher(normalizedSlug).matches()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Slug must be lowercase letters, digits and single hyphens (e.g. 'green-hills-academy')");
                return;
            }
            if (schoolDAO.findBySlug(normalizedSlug) != null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "A school with this slug already exists");
                return;
            }
        }

        School school = new School(trimmedName, normalizedSlug);
        school.setDomain(blankToNull(request.getParameter("domain")));
        school.setAddress(blankToNull(request.getParameter("address")));
        school.setPhone(blankToNull(request.getParameter("phone")));
        school.setEmail(blankToNull(request.getParameter("email")));
        school.setLogoUrl(blankToNull(request.getParameter("logoUrl")));
        school.setDescription(blankToNull(request.getParameter("description")));
        String plan = planOrDefault(request.getParameter("plan"));
        if (plan == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid plan");
            return;
        }
        school.setPlan(plan);
        if (request.getParameter("active") != null) {
            school.setActive(Boolean.parseBoolean(request.getParameter("active")));
        }
        School saved = schoolDAO.save(school);

        String adminNote = bootstrapSchoolAdmin(request, saved);

        sendSuccess(response, "School created: " + saved.getName()
                + " (slug " + saved.getSlug() + ")" + adminNote);
    }

    /**
     * Creates the school's own first administrator so the tenant can be
     * managed by its own account rather than by the platform administrator.
     * <p>
     * A school is only useful once somebody administers it, and there is no
     * other route to that account: the platform portal deliberately cannot
     * enter a school. Returns a human-readable note for the response message.
     * </p>
     */
    private String bootstrapSchoolAdmin(HttpServletRequest request, School school) {
        String name = request.getParameter("adminName");
        String email = request.getParameter("adminEmail");
        String password = request.getParameter("adminPassword");

        if (email == null || email.trim().isEmpty()) {
            return ". No administrator was set: add one before the school can be used.";
        }
        if (name == null || name.trim().isEmpty()) {
            name = email.trim();
        }

        return attachSchoolAdmin(school, name.trim(), email.trim(), password);
    }

    /**
     * Gives a school an administrator account, reusing the account if it
     * already exists.
     * <p>
     * This is deliberately idempotent. A half-provisioned school is a real
     * state: the user row can be created and the membership then fail, leaving
     * an administrator account that belongs to no school and can never sign in.
     * Re-running this attaches that orphaned account instead of failing on the
     * duplicate email, which is what makes such a school repairable without
     * deleting anything.
     * </p>
     *
     * @return a human-readable note describing what happened
     */
    private String attachSchoolAdmin(School school, String name, String email, String password) {
        User existing = userDAO.findByEmail(email);
        if (existing != null) {
            if (schoolMembershipDAO.findByUserAndSchool(existing.getId(), school.getId()) != null) {
                throw new IllegalArgumentException(email + " already administers " + school.getName());
            }
            schoolMembershipDAO.saveForPlatform(new SchoolMembership(school, existing, UserRole.ADMIN));
            notifyAdminsSafely(school, existing,
                    "You now administer " + school.getName(),
                    "You were granted administrator access to " + school.getName()
                            + ". You can manage this school's users, classes and settings.",
                    "SCHOOL_ADMIN_GRANTED",
                    "/admin/dashboard");
            return ". Existing account " + email + " now administers " + school.getName();
        }

        if (password == null || password.trim().length() < 8) {
            throw new IllegalArgumentException("Administrator password must be at least 8 characters");
        }

        User admin = new UserService(userDAO).createUser(name, email, null, password, UserRole.ADMIN);
        admin.setEmailVerified(true);
        userDAO.save(admin);
        schoolMembershipDAO.saveForPlatform(new SchoolMembership(school, admin, UserRole.ADMIN));
        notifyAdminsSafely(school, admin,
                "You are the administrator of " + school.getName(),
                "An account was created for you to administer " + school.getName()
                        + ". Set a new password from your profile before sharing the portal widely.",
                "SCHOOL_ADMIN_GRANTED",
                "/admin/dashboard");
        return ". Administrator created: " + admin.getEmail();
    }

    /**
     * Notifies every administrator of a school. The platform administrator acts
     * without an active tenant, so these notifications are stamped explicitly
     * with the target school rather than with the caller's tenant.
     */
    private void notifyAdminsSafely(School school, User extraRecipient, String title, String message,
                                    String type, String link) {
        try {
            if (extraRecipient != null) {
                notificationService.notifyForSchool(school, extraRecipient, title, message, type, link);
            }
            for (SchoolMembership membership : schoolMembershipDAO.findBySchoolId(school.getId())) {
                UserRole role = membership.getRole();
                if (role != UserRole.ADMIN && role != UserRole.SUPER_ADMIN) {
                    continue;
                }
                User recipient = membership.getUser();
                if (recipient == null) {
                    continue;
                }
                if (extraRecipient != null && extraRecipient.getId() != null
                        && extraRecipient.getId().equals(recipient.getId())) {
                    continue;
                }
                notificationService.notifyForSchool(school, recipient, title, message, type, link);
            }
        } catch (Exception e) {
            LOGGER.warning("Could not notify administrators of " + school.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Derives a URL-safe slug from a school name and guarantees uniqueness by
     * appending a numeric suffix, so creating a school never dead-ends on a
     * missing or duplicate slug.
     */
    private String uniqueSlugFromName(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "school";
        }
        String candidate = base;
        int suffix = 2;
        while (schoolDAO.findBySlug(candidate) != null) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        School school = schoolFromParam(request);
        if (school == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "School not found");
            return;
        }

        if (request.getParameter("name") != null && !request.getParameter("name").trim().isEmpty()) {
            school.setName(request.getParameter("name").trim());
        }
        if (request.getParameter("domain") != null) {
            school.setDomain(blankToNull(request.getParameter("domain")));
        }
        if (request.getParameter("address") != null) {
            school.setAddress(blankToNull(request.getParameter("address")));
        }
        if (request.getParameter("phone") != null) {
            school.setPhone(blankToNull(request.getParameter("phone")));
        }
        if (request.getParameter("email") != null) {
            school.setEmail(blankToNull(request.getParameter("email")));
        }
        if (request.getParameter("logoUrl") != null) {
            school.setLogoUrl(blankToNull(request.getParameter("logoUrl")));
        }
        if (request.getParameter("description") != null) {
            school.setDescription(blankToNull(request.getParameter("description")));
        }
        if (request.getParameter("plan") != null) {
            String plan = planOrDefault(request.getParameter("plan"));
            if (plan == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid plan");
                return;
            }
            school.setPlan(plan);
        }
        if (request.getParameter("active") != null) {
            school.setActive(Boolean.parseBoolean(request.getParameter("active")));
        }
        schoolDAO.save(school);
        notifyAdminsSafely(school, null,
                "Your school details were updated",
                "The platform administrator updated the settings for " + school.getName()
                        + ". Sign in to review the changes.",
                "SCHOOL_UPDATED",
                "/admin/dashboard?section=school");
        sendSuccess(response, "School updated: " + school.getName());
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        School school = schoolFromParam(request);
        if (school == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "School not found");
            return;
        }
        school.setActive(!school.isActive());
        schoolDAO.save(school);
        boolean active = school.isActive();
        notifyAdminsSafely(school, null,
                active ? "Your school was reactivated" : "Your school was suspended",
                active
                        ? school.getName() + " is active again. You can use the school portal as normal."
                        : school.getName() + " has been suspended by the platform administrator. "
                                + "Access is unavailable until it is reactivated.",
                active ? "SCHOOL_REACTIVATED" : "SCHOOL_SUSPENDED",
                "/admin/dashboard");
        sendSuccess(response, (active ? "Activated" : "Deactivated") + " " + school.getName());
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        School school = schoolFromParam(request);
        if (school == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "School not found");
            return;
        }
        if (!schoolMembershipDAO.findBySchoolId(school.getId()).isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot delete a school that still has members");
            return;
        }
        schoolDAO.delete(school.getId());
        sendSuccess(response, "School deleted: " + school.getName());
    }

    /**
     * Resolves a school from the {@code id} request parameter, or {@code null}.
     */
    /**
     * Adds an administrator account to an existing school.
     * <p>
     * Needed to repair a school that ended up with no administrator, and to give
     * a school a second administrator without touching its existing one. The
     * school and all of its data are left untouched.
     * </p>
     */
    private void handleAddAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        School school = schoolFromParam(request);
        if (school == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "School not found");
            return;
        }

        String name = request.getParameter("adminName");
        String email = request.getParameter("adminEmail");
        String password = request.getParameter("adminPassword");

        if (email == null || email.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Administrator email is required");
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            name = email.trim();
        }

        sendSuccess(response, attachSchoolAdmin(school, name.trim(), email.trim(), password));
    }

    private School schoolFromParam(HttpServletRequest request) {
        String idStr = request.getParameter("id");
        if (idStr == null) {
            return null;
        }
        return schoolDAO.findById(Long.parseLong(idStr));
    }

    /**
     * Validates/normalizes the plan value, or returns {@code null} when invalid.
     */
    private String planOrDefault(String plan) {
        if (plan == null || plan.trim().isEmpty()) {
            return "FREE";
        }
        String normalized = plan.trim().toUpperCase(Locale.ROOT);
        return PLANS.contains(normalized) ? normalized : null;
    }

    private String blankToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private void sendSuccess(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", message);
        objectMapper.writeValue(response.getWriter(), res);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("message", message);
        objectMapper.writeValue(response.getWriter(), res);
    }
}