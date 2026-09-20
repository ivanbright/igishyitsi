package org.example.esubmission.dao;

import org.example.esubmission.model.ParentChild;
import org.example.esubmission.model.User;
import org.example.esubmission.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link ParentChild} entity.
 */
public class ParentChildDAO {

    /**
     * Returns the parent&#8211;student links for a parent.
     */
    public List<ParentChild> findByParentId(Long parentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ParentChild pc WHERE pc.parent.id = :parentId ORDER BY pc.student.name", ParentChild.class)
                    .setParameter("parentId", parentId)
                    .list();
        }
    }

    /**
     * Returns the parent&#8211;student links for a student.
     */
    public List<ParentChild> findByStudentId(Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ParentChild pc WHERE pc.student.id = :studentId ORDER BY pc.parent.name", ParentChild.class)
                    .setParameter("studentId", studentId)
                    .list();
        }
    }

    /**
     * Returns the distinct children (students) of a parent.
     */
    public List<User> findStudentsByParent(Long parentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT pc.student FROM ParentChild pc WHERE pc.parent.id = :parentId ORDER BY pc.student.name", User.class)
                    .setParameter("parentId", parentId)
                    .list();
        }
    }

    /**
     * Returns the distinct parents linked to a student.
     */
    public List<User> findParentsByStudent(Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT pc.parent FROM ParentChild pc WHERE pc.student.id = :studentId ORDER BY pc.parent.name", User.class)
                    .setParameter("studentId", studentId)
                    .list();
        }
    }

    public boolean exists(Long parentId, Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(pc.id) FROM ParentChild pc WHERE pc.parent.id = :parentId AND pc.student.id = :studentId", Long.class)
                    .setParameter("parentId", parentId)
                    .setParameter("studentId", studentId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public ParentChild save(ParentChild parentChild) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ParentChild saved = session.merge(parentChild);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save parent-child link", e);
        }
    }

    public void remove(Long parentId, Long studentId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createQuery("DELETE FROM ParentChild pc WHERE pc.parent.id = :parentId AND pc.student.id = :studentId")
                    .setParameter("parentId", parentId)
                    .setParameter("studentId", studentId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to remove parent-child link", e);
        }
    }
}