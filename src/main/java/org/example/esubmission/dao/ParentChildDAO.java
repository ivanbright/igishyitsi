package org.example.esubmission.dao;

import org.example.esubmission.model.ParentChild;
import org.example.esubmission.model.User;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
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
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM ParentChild pc WHERE pc.schoolId = :schoolId AND pc.parent.id = :parentId ORDER BY pc.student.name", ParentChild.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("parentId", parentId)
                    .list();
        }
    }

    /**
     * Returns the parent&#8211;student links for a student.
     */
    public List<ParentChild> findByStudentId(Long studentId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM ParentChild pc WHERE pc.schoolId = :schoolId AND pc.student.id = :studentId ORDER BY pc.parent.name", ParentChild.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("studentId", studentId)
                    .list();
        }
    }

    /**
     * Returns the distinct children (students) of a parent.
     */
    public List<User> findStudentsByParent(Long parentId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT pc.student FROM ParentChild pc WHERE pc.schoolId = :schoolId AND pc.parent.id = :parentId ORDER BY pc.student.name", User.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("parentId", parentId)
                    .list();
        }
    }

    /**
     * Returns the distinct parents linked to a student.
     */
    public List<User> findParentsByStudent(Long studentId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT pc.parent FROM ParentChild pc WHERE pc.schoolId = :schoolId AND pc.student.id = :studentId ORDER BY pc.parent.name", User.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("studentId", studentId)
                    .list();
        }
    }

    public boolean exists(Long parentId, Long studentId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(pc.id) FROM ParentChild pc WHERE pc.schoolId = :schoolId AND pc.parent.id = :parentId AND pc.student.id = :studentId", Long.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("parentId", parentId)
                    .setParameter("studentId", studentId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public ParentChild save(ParentChild parentChild) {
        Long schoolId = TenantContext.requireSchoolId();
        if (parentChild.getSchoolId() == null) {
            parentChild.setSchoolId(schoolId);
        }
        if (!schoolId.equals(parentChild.getSchoolId())) {
            throw new IllegalStateException("Parent-child link belongs to another school");
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (parentChild.getId() != null) {
                ParentChild existing = session.get(ParentChild.class, parentChild.getId());
                if (existing != null && !schoolId.equals(existing.getSchoolId())) {
                    throw new IllegalStateException("Parent-child link belongs to another school");
                }
            }
            ParentChild saved = session.merge(parentChild);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save parent-child link", e);
        }
    }

    public void remove(Long parentId, Long studentId) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createQuery("DELETE FROM ParentChild pc WHERE pc.schoolId = :schoolId AND pc.parent.id = :parentId AND pc.student.id = :studentId")
                    .setParameter("schoolId", schoolId)
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