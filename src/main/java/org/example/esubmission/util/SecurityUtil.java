package org.example.esubmission.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Shared authorization helpers for the admin surface.
 * <p>
 * These helpers are defense-in-depth: url-pattern filters already restrict
 * {@code /admin/*} and {@code /api/admin/*}, and every admin endpoint
 * additionally verifies the session role and (for state changes) the CSRF token.
 * </p>
 */
public final class SecurityUtil {

    private static final SchoolMembershipDAO SCHOOL_MEMBERSHIP_DAO = new SchoolMembershipDAO();

    private SecurityUtil() {}

    public static UserRole effectiveRole(User user) {
        return effectiveRole(user, TenantContext.getSchoolId());
    }

    public static UserRole effectiveRole(User user, Long schoolId) {
        if (user == null || user.isDisabled() || user.getId() == null) {
            return null;
        }
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return UserRole.SUPER_ADMIN;
        }
        if (schoolId != null) {
            SchoolMembership membership = SCHOOL_MEMBERSHIP_DAO.findByUserAndSchool(user.getId(), schoolId);
            return membership != null ? membership.getRole() : null;
        }
        List<SchoolMembership> memberships = SCHOOL_MEMBERSHIP_DAO.findByUserIdGlobal(user.getId());
        return memberships.isEmpty() ? user.getRole() : null;
    }

    public static UserRole primaryRole(User user) {
        if (user == null || user.isDisabled() || user.getId() == null) {
            return null;
        }
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return UserRole.SUPER_ADMIN;
        }
        List<SchoolMembership> memberships = SCHOOL_MEMBERSHIP_DAO.findByUserIdGlobal(user.getId());
        return memberships.isEmpty() ? user.getRole() : memberships.get(0).getRole();
    }

    public static boolean hasRole(User user, UserRole role) {
        return role != null && role == effectiveRole(user);
    }

    public static User requireRole(HttpServletRequest req, UserRole role) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute("user");
        if (!(attribute instanceof User)) {
            return null;
        }
        User user = (User) attribute;
        return hasRole(user, role) ? user : null;
    }

    public static User requireTeacher(HttpServletRequest req) {
        return requireRole(req, UserRole.TEACHER);
    }

    public static User requireStudent(HttpServletRequest req) {
        return requireRole(req, UserRole.STUDENT);
    }

    /**
     * Returns {@code true} when the given user is an enabled platform or school
     * administrator ({@code SUPER_ADMIN} or {@code ADMIN}).
     */
    public static boolean isAdminLike(User user) {
        UserRole role = effectiveRole(user);
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    /**
     * Returns the admin-like user ({@code ADMIN} or {@code SUPER_ADMIN}) from
     * the session, or {@code null} when the caller is not an enabled admin session.
     */
    public static User requireAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute("user");
        if (!(attribute instanceof User)) {
            return null;
        }
        User user = (User) attribute;
        return isAdminLike(user) ? user : null;
    }

    /**
     * Returns the {@code SUPER_ADMIN} user from the session, or {@code null}
     * when the caller is not an enabled platform administrator session.
     */
    public static User requireSuperAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute("user");
        if (!(attribute instanceof User)) {
            return null;
        }
        User user = (User) attribute;
        if (user.getRole() != UserRole.SUPER_ADMIN || user.isDisabled()) {
            return null;
        }
        return user;
    }

    /**
     * Returns {@code true} when the request carries a valid CSRF token.
     * <p>
     * The token lives on the session and must be echoed back on every
     * state-changing admin request, either as the {@code X-CSRF-Token} header
     * (AJAX/fetch) or as a {@code csrfToken} request parameter (plain HTML form
     * posts, which cannot send custom headers).
     * </p>
     */
    public static boolean csrfValid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }
        String token = (String) session.getAttribute("csrfToken");
        if (token == null || token.isBlank()) {
            return false;
        }
        String provided = req.getHeader("X-CSRF-Token");
        if (provided == null || provided.isBlank()) {
            provided = req.getParameter("csrfToken");
        }
        return provided != null && token.equals(provided);
    }

    /**
     * Returns the session CSRF token, generating one on first use.
     */
    public static String getCsrfToken(HttpServletRequest req) {
        HttpSession session = req.getSession();
        String token = (String) session.getAttribute("csrfToken");
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", token);
        }
        return token;
    }

    /**
     * Neutralizes common XSS vectors in a single-line value. Sensitive fields
     * (passwords, etc.) must bypass this and are excluded by the calling filter.
     */
    public static String sanitizeInput(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Generates a random 6-digit numeric verification code.
     */
    public static String generateVerificationCode() {
        return String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000));
    }
}