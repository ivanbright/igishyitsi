package org.example.esubmission.dao;

import org.example.esubmission.model.User;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link User} entity.
 * <p>
 * This class provides methods to manage users and their authentication information.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class UserDAO {
    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return the user entity, or {@code null} if not found
     */
    public User findById(long id) {
        Long schoolId = TenantContext.requireSchoolId();
        Session session = HibernateUtil.getSessionFactory().openSession();
        User user = session.get(User.class, id);
        session.close();
        if (user == null) {
            return null;
        }
        try (Session membershipSession = HibernateUtil.getSessionFactory().openSession()) {
            Long membershipCount = membershipSession.createQuery(
                    "SELECT COUNT(sm) FROM SchoolMembership sm WHERE sm.user.id = :userId AND sm.school.id = :schoolId", Long.class)
                    .setParameter("userId", id)
                    .setParameter("schoolId", schoolId)
                    .uniqueResult();
            return membershipCount != null && membershipCount > 0 ? user : null;
        }
    }

    public User findByIdGlobal(long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        User user = session.get(User.class, id);
        session.close();
        return user;
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address of the user
     * @return the user entity, or {@code null} if not found
     */
    public User findByEmail(String email) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<User> users = session.createQuery(
                "FROM User where email=: email",
                User.class
        ).setParameter("email", email).getResultList();

        session.close();

        return users.isEmpty() ? null : users.getFirst();
    }

    /**
     * Returns the platform users who are members of the given school (via
     * {@code school_members}); never falls back to a global "all users" snapshot.
     *
     * @param schoolId the school whose members are requested
     * @return the school-scoped users (never null)
     */
    public List<User> findBySchoolId(Long schoolId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT m.user FROM SchoolMembership m " +
                    "WHERE m.school.id = :schoolId ORDER BY m.user.name",
                    User.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    /**
     * Retrieves a user by their password reset token.
     *
     * @param token the reset token
     * @return the user entity, or {@code null} if not found
     */
    public User findByResetToken(String token) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<User> users = session.createQuery(
                "FROM User where resetToken=: token",
                User.class
        ).setParameter("token", token).getResultList();

        session.close();

        return users.isEmpty() ? null : users.getFirst();
    }

    /**
     * Retrieves all users in the system.
     *
     * @return a list of all users
     */
    public List<User> findAll() {
        Long schoolId = TenantContext.requireSchoolId();
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<User> users = session.createQuery(
                "SELECT DISTINCT m.user FROM SchoolMembership m WHERE m.school.id = :schoolId ORDER BY m.user.name", User.class)
                .setParameter("schoolId", schoolId)
                .list();
        session.close();
        return users;
    }

    public List<User> findAllGlobal() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<User> users = session.createQuery("FROM User ORDER BY name", User.class).list();
        session.close();
        return users;
    }

    /**
     * Saves a new user or updates an existing one.
     *
     * @param user the user entity to save
     * @return the saved user
     * @throws RuntimeException if the operation fails
     */
    public User save(User user) {

        Transaction tx = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            User saved = session.merge(user);
            tx.commit();
            return saved;
        } catch (Exception ex) {
            if (tx != null && tx.getStatus().canRollback()) {
                tx.rollback();
            }
            ex.printStackTrace();
            throw new RuntimeException("Failed to save user", ex);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

     /**
      * Deletes a user account, but only once the account has no membership in
      * any school.
      * <p>
      * A user row is global while its memberships are per-school, so a user can
      * belong to more than one school. The account is only removed when its
      * last membership anywhere is gone; otherwise removing one school would
      * destroy the account and orphan the other schools' memberships.
      * </p>
      *
      * @param id the user id
      */
     public void delete(long id) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            User user = session.get(User.class, id);
            if (user != null) {
                Long remainingMemberships = session.createQuery(
                        "SELECT COUNT(sm) FROM SchoolMembership sm WHERE sm.user.id = :userId", Long.class)
                        .setParameter("userId", id)
                        .uniqueResult();
                if (remainingMemberships != null && remainingMemberships == 0) {
                    session.remove(user);
                }
            }
            tx.commit();
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to delete user", e);
        }
    }
}
