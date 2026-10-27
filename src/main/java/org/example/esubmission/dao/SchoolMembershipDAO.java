package org.example.esubmission.dao;

import org.example.esubmission.model.School;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link SchoolMembership} entity.
 * <p>
 * Memberships link a {@link org.example.esubmission.model.User} to a
 * {@link org.example.esubmission.model.School} with a per-school role.
 * </p>
 */
public class SchoolMembershipDAO {

    /**
     * Returns all school memberships for a user, primary/most recent first.
     *
     * @param userId the ID of the user
     * @return list of memberships ordered by join date descending
     */
    public List<SchoolMembership> findByUserId(Long userId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM SchoolMembership sm WHERE sm.user.id = :userId AND sm.school.id = :schoolId ORDER BY sm.joinedAt DESC",
                    SchoolMembership.class)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    public List<SchoolMembership> findByUserIdGlobal(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM SchoolMembership sm WHERE sm.user.id = :userId ORDER BY sm.joinedAt DESC",
                    SchoolMembership.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }

    /**
     * Returns the primary (most recently joined) membership for a user, or
     * {@code null} if the user belongs to no school.
     *
     * @param userId the ID of the user
     * @return the primary membership or {@code null}
     */
    public SchoolMembership findPrimaryByUserId(Long userId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<SchoolMembership> memberships = session.createQuery(
                    "FROM SchoolMembership sm WHERE sm.user.id = :userId AND sm.school.id = :schoolId ORDER BY sm.joinedAt DESC",
                    SchoolMembership.class)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", schoolId)
                    .setMaxResults(1)
                    .list();
            return memberships.isEmpty() ? null : memberships.get(0);
        }
    }

    public SchoolMembership findPrimaryByUserIdGlobal(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<SchoolMembership> memberships = session.createQuery(
                    "FROM SchoolMembership sm WHERE sm.user.id = :userId ORDER BY sm.joinedAt DESC",
                    SchoolMembership.class)
                    .setParameter("userId", userId)
                    .setMaxResults(1)
                    .list();
            return memberships.isEmpty() ? null : memberships.get(0);
        }
    }

    /**
     * Returns the membership of a user in a specific school, or {@code null}.
     *
     * @param userId   the ID of the user
     * @param schoolId the ID of the school
     * @return the membership or {@code null}
     */
    public SchoolMembership findByUserAndSchool(Long userId, Long schoolId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM SchoolMembership sm WHERE sm.user.id = :userId AND sm.school.id = :schoolId",
                    SchoolMembership.class)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", schoolId)
                    .uniqueResult();
        }
    }

    /**
     * Returns the memberships of a specific school.
     *
     * @param schoolId the ID of the school
     * @return list of memberships
     */
    public List<SchoolMembership> findBySchoolId(Long schoolId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM SchoolMembership sm WHERE sm.school.id = :schoolId ORDER BY sm.joinedAt DESC",
                    SchoolMembership.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    public SchoolMembership save(SchoolMembership membership) {
        Long schoolId = TenantContext.requireSchoolId();
        requireSameSchool(membership.getSchool(), schoolId);
        return persist(membership, schoolId);
    }

    /**
     * Persists a membership with no active tenant, for platform-level work only.
     * <p>
     * The normal {@link #save(SchoolMembership)} path deliberately insists on a
     * resolved tenant so tenant-scoped code can never write into another
     * school. Provisioning a brand-new school is the one legitimate case where
     * no tenant exists yet: the school row is created moments earlier in the
     * same operation, so the membership is scoped to that school directly.
     * </p>
     * <p>
     * This method performs no authorization of its own. Callers must already
     * have established platform-administrator access.
     * </p>
     *
     * @param membership the membership; its school must already be persisted
     * @return the persisted membership
     */
    public SchoolMembership saveForPlatform(SchoolMembership membership) {
        School school = membership.getSchool();
        if (school == null || school.getId() == null) {
            throw new IllegalArgumentException("Membership must reference a persisted school");
        }
        return persist(membership, school.getId());
    }

    private void requireSameSchool(School school, Long schoolId) {
        if (school == null || !schoolId.equals(school.getId())) {
            throw new IllegalStateException("School membership belongs to another school");
        }
    }

    private SchoolMembership persist(SchoolMembership membership, Long schoolId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (membership.getId() != null) {
                SchoolMembership existing = session.get(SchoolMembership.class, membership.getId());
                requireSameSchool(existing == null ? null : existing.getSchool(), schoolId);
            }
            SchoolMembership saved = session.merge(membership);
            tx.commit();
            return saved;
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to save school membership", e);
        }
    }

    public void remove(Long membershipId) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            SchoolMembership membership = session.get(SchoolMembership.class, membershipId);
            if (membership != null && membership.getSchool() != null && schoolId.equals(membership.getSchool().getId())) {
                session.remove(membership);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to remove school membership", e);
        }
    }
}