package org.example.esubmission.dao;

import org.example.esubmission.model.Notification;
import org.example.esubmission.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link Notification} entity.
 */
public class NotificationDAO {

    public Notification save(Notification notification) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Notification saved = session.merge(notification);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save notification", e);
        }
    }

    /**
     * Retrieves notifications for a specific user, most recent first.
     */
    public List<Notification> findByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Notification WHERE user.id = :userId ORDER BY createdAt DESC",
                    Notification.class)
                    .setParameter("userId", userId)
                    .setMaxResults(50)
                    .list();
        }
    }

    /**
     * Retrieves unread notifications for a specific user.
     */
    public List<Notification> findUnreadByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Notification WHERE user.id = :userId AND read = false ORDER BY createdAt DESC",
                    Notification.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }

    /**
     * Counts unread notifications for a specific user.
     */
    public Long countUnreadByUserId(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false",
                    Long.class)
                    .setParameter("userId", userId)
                    .uniqueResult();
        }
    }

    /**
     * Marks all notifications as read for a specific user.
     */
    public void markAllReadByUserId(Long userId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Notification SET read = true WHERE user.id = :userId AND read = false")
                    .setParameter("userId", userId)
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
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Notification SET read = true WHERE id = :id AND user.id = :userId AND read = false")
                    .setParameter("id", notificationId)
                    .setParameter("userId", userId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }
}
