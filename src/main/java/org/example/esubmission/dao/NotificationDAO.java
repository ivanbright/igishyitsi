package org.example.esubmission.dao;

import org.example.esubmission.model.Notification;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link Notification} entity.
 */
public class NotificationDAO {

    public Notification save(Notification notification) {
        Long schoolId = TenantContext.requireSchoolId();
        if (notification.getSchoolId() == null) {
            notification.setSchoolId(schoolId);
        }
        requireSameSchool(notification.getSchoolId(), schoolId);
        return persist(notification, schoolId);
    }

    /**
     * Persists a notification for an explicitly named school, for platform-level
     * work performed with no active tenant.
     * <p>
     * Provisioning a school and notifying its new administrator happens while the
     * platform administrator has no tenant, so it cannot use the tenant-scoped
     * write. This method performs no authorization of its own; callers must
     * already hold platform-administrator access.
     * </p>
     */
    public Notification saveForPlatform(Notification notification) {
        Long schoolId = notification.getSchoolId();
        if (schoolId == null) {
            throw new IllegalArgumentException("Notification must name a school");
        }
        return persist(notification, schoolId);
    }

    private void requireSameSchool(Long notificationSchoolId, Long schoolId) {
        if (!schoolId.equals(notificationSchoolId)) {
            throw new IllegalStateException("Notification belongs to another school");
        }
    }

    private Notification persist(Notification notification, Long schoolId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (notification.getId() != null) {
                Notification existing = session.get(Notification.class, notification.getId());
                if (existing != null && !schoolId.equals(existing.getSchoolId())) {
                    throw new IllegalStateException("Notification belongs to another school");
                }
            }
            Notification saved = session.merge(notification);
            tx.commit();
            return saved;
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to save notification", e);
        }
    }

    /**
     * Retrieves notifications for a specific user within the active school, most recent first.
     */
    public List<Notification> findByUserId(Long userId, Long schoolId) {
        Long activeSchoolId = requireActiveSchool(schoolId);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Notification n WHERE n.user.id = :userId AND n.schoolId = :schoolId ORDER BY n.createdAt DESC",
                    Notification.class)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", activeSchoolId)
                    .setMaxResults(50)
                    .list();
        }
    }

    /**
     * Retrieves unread notifications for a specific user within a school.
     */
    public List<Notification> findUnreadByUserId(Long userId, Long schoolId) {
        Long activeSchoolId = requireActiveSchool(schoolId);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Notification n WHERE n.user.id = :userId AND n.schoolId = :schoolId AND n.read = false ORDER BY n.createdAt DESC",
                    Notification.class)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", activeSchoolId)
                    .list();
        }
    }

    /**
     * Counts unread notifications for a specific user within a school.
     */
    public Long countUnreadByUserId(Long userId, Long schoolId) {
        Long activeSchoolId = requireActiveSchool(schoolId);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.schoolId = :schoolId AND n.read = false",
                    Long.class)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", activeSchoolId)
                    .uniqueResult();
        }
    }

    /**
     * Marks all notifications as read for a specific user within a school.
     */
    public void markAllReadByUserId(Long userId, Long schoolId) {
        Long activeSchoolId = requireActiveSchool(schoolId);
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Notification SET read = true WHERE user.id = :userId AND schoolId = :schoolId AND read = false")
                    .setParameter("userId", userId)
                    .setParameter("schoolId", activeSchoolId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to mark notifications as read", e);
        }
    }

    /**
     * Marks a single notification as read only if it belongs to the given user.
     */
    public void markReadForUser(Long userId, Long notificationId) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Notification SET read = true WHERE id = :id AND user.id = :userId AND schoolId = :schoolId AND read = false")
                    .setParameter("id", notificationId)
                    .setParameter("userId", userId)
                    .setParameter("schoolId", schoolId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    private Long requireActiveSchool(Long requestedSchoolId) {
        Long activeSchoolId = TenantContext.requireSchoolId();
        if (requestedSchoolId != null && !activeSchoolId.equals(requestedSchoolId)) {
            throw new IllegalStateException("Notification school does not match the active school");
        }
        return activeSchoolId;
    }
}
