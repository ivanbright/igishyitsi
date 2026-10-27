package org.example.esubmission.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.SchoolDAO;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.model.School;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.User;
import org.example.esubmission.util.TenantContext;

import java.io.IOException;
import java.util.List;

/**
 * Resolves and establishes the tenant (school) context for every request.
 * <p>
 * The school id of the authenticated user's primary membership is resolved
 * once, cached on the session, and exposed through
 * {@link TenantContext} for the duration of the request. An account with no
 * membership in any active school resolves to no tenant at all: there is
 * deliberately no fallback to a "default" school, because doing so would
 * silently grant a user access to another school's data.
 * <p>
 * Platform administrators ({@code SUPER_ADMIN}) are deliberately tenant-less.
 * They administer the whole platform from the platform portal, which operates
 * across all schools, so they are never placed inside a single school's
 * context. Each school is administered by its own {@code ADMIN} account
 * instead.
 * </p>
 * The context is always cleared in a {@code finally} block so pooled threads
 * never leak a tenant across requests.
 * </p>
 */
public class TenantFilter implements Filter {

    private static final String SESSION_SCHOOL_ID = "schoolId";

    private SchoolDAO schoolDAO;
    private SchoolMembershipDAO schoolMembershipDAO;

    @Override
    public void init(FilterConfig filterConfig) {
        this.schoolDAO = new SchoolDAO();
        this.schoolMembershipDAO = new SchoolMembershipDAO();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            Long schoolId = resolveSchoolId(httpRequest);
            TenantContext.setSchoolId(schoolId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Resolves the active school id for the request. Uses the value cached on
     * the session when available, otherwise resolves it from the user's
     * primary membership and caches it.
     *
     * @param request the HTTP request
     * @return the active school id, or {@code null} if unresolved
     */
    private Long resolveSchoolId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User)) {
            return null;
        }
        User user = (User) userObj;
        if (user.getRole() == org.example.esubmission.util.UserRole.SUPER_ADMIN) {
            session.removeAttribute(SESSION_SCHOOL_ID);
            return null;
        }
        Object cached = session.getAttribute(SESSION_SCHOOL_ID);
        if (cached instanceof Number) {
            Long cachedSchoolId = ((Number) cached).longValue();
            School cachedSchool = schoolDAO.findById(cachedSchoolId);
            SchoolMembership cachedMembership = schoolMembershipDAO.findByUserAndSchool(user.getId(), cachedSchoolId);
            if (cachedSchool != null && cachedSchool.isActive() && cachedMembership != null) {
                return cachedSchoolId;
            }
            session.removeAttribute(SESSION_SCHOOL_ID);
        }

        List<SchoolMembership> memberships = schoolMembershipDAO.findByUserIdGlobal(user.getId());
        for (SchoolMembership membership : memberships) {
            if (membership.getSchool() != null) {
                Long schoolId = membership.getSchool().getId();
                School school = schoolDAO.findById(schoolId);
                if (school != null && school.isActive()) {
                    session.setAttribute(SESSION_SCHOOL_ID, schoolId);
                    return schoolId;
                }
            }
        }
        if (!memberships.isEmpty()) {
            return null;
        }
        return null;
    }

    @Override
    public void destroy() {
    }
}