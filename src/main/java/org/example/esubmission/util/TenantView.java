package org.example.esubmission.util;

import jakarta.servlet.http.HttpServletRequest;
import org.example.esubmission.dao.SchoolDAO;
import org.example.esubmission.model.School;

/**
 * Exposes the active tenant to the shared dashboard layout.
 * <p>
 * A signed-in user has to be able to tell which school they are working in,
 * otherwise the portal looks identical for every tenant. Each tenant dashboard
 * servlet calls {@link #apply(HttpServletRequest)} once it has resolved its
 * tenant, and the layout renders the school name and logo from the request
 * attributes set here.
 * </p>
 */
public final class TenantView {

    public static final String ATTR_SCHOOL = "school";
    public static final String ATTR_SCHOOL_NAME = "schoolName";
    public static final String ATTR_SCHOOL_LOGO = "schoolLogoUrl";

    private TenantView() {
    }

    /**
     * Sets the current school's view attributes from the active tenant.
     * <p>
     * A request with no active tenant, such as a platform administrator, simply
     * gets no school attributes and the layout falls back to the product name.
     * </p>
     *
     * @param request the current request
     * @return the active school, or {@code null} when there is no tenant
     */
    public static School apply(HttpServletRequest request) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return null;
        }
        School school = new SchoolDAO().findById(schoolId);
        if (school == null) {
            return null;
        }
        request.setAttribute(ATTR_SCHOOL, school);
        request.setAttribute(ATTR_SCHOOL_NAME, school.getName());
        if (school.getLogoUrl() != null && !school.getLogoUrl().trim().isEmpty()) {
            request.setAttribute(ATTR_SCHOOL_LOGO, school.getLogoUrl().trim());
        }
        return school;
    }
}
