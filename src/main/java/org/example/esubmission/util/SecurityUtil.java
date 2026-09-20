package org.example.esubmission.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.model.User;

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

    private SecurityUtil() {}

    /**
     * Returns the {@code ADMIN} user from the session, or {@code null} when the
     * caller is not an enabled admin session.
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
        if (user.getRole() != UserRole.ADMIN || user.isDisabled()) {
            return null;
        }
        return user;
    }

    /**
     * Returns {@code true} when the request carries a valid CSRF token.
     * <p>
     * The token lives on the session and must be echoed back on every
     * state-changing admin request via the {@code X-CSRF-Token} header.
     * If no token has been established on the session yet the check is
     * bypassed so existing flows (e.g. legacy form posts) keep working.
     * </p>
     */
    public static boolean csrfValid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }
        String token = (String) session.getAttribute("csrfToken");
        if (token == null) {
            return true;
        }
        return token.equals(req.getHeader("X-CSRF-Token"));
    }

    /**
     * Returns the session CSRF token, generating one on first use.
     */
    public static String getCsrfToken(HttpServletRequest req) {
        HttpSession session = req.getSession();
        String token = (String) session.getAttribute("csrfToken");
        if (token == null) {
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