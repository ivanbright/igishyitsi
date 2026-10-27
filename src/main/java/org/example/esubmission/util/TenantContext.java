package org.example.esubmission.util;

/**
 * Holds the tenant (school) context for the current request thread.
 * <p>
 * The current thread's school id is populated by {@code TenantFilter}
 * from the authenticated user's active school membership. DAO write
 * methods use it to stamp {@code school_id} on every tenant-scoped row
 * so that data isolation is enforced at the schema level.
 * </p>
 */
public final class TenantContext {

    private static final ThreadLocal<Long> SCHOOL_ID = new ThreadLocal<>();

    private TenantContext() {}

    /**
     * Sets the school id for the current thread.
     *
     * @param schoolId the active school id, or {@code null} to clear
     */
    public static void setSchoolId(Long schoolId) {
        SCHOOL_ID.set(schoolId);
    }

    /**
     * Returns the school id for the current thread, or {@code null} if no
     * tenant is active.
     *
     * @return the active school id
     */
    public static Long getSchoolId() {
        return SCHOOL_ID.get();
    }

    /**
     * Returns the school id for the current thread.
     *
     * @return the active school id
     * @throws IllegalStateException if no tenant is active
     */
    public static Long requireSchoolId() {
        Long schoolId = SCHOOL_ID.get();
        if (schoolId == null) {
            throw new IllegalStateException(
                    "No tenant (school) is active for the current request. " +
                    "Tenant-scoped writes require a resolved tenant.");
        }
        return schoolId;
    }

    /**
     * Clears the tenant context for the current thread. Expected to be called
     * in a {@code finally} block so pooled threads do not leak a tenant.
     */
    public static void clear() {
        SCHOOL_ID.remove();
    }
}