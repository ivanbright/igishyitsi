package org.example.esubmission.dao;

import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link ClassGroup} entity.
 */
public class ClassGroupDAO {

    public List<ClassGroup> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ClassGroup ORDER BY name", ClassGroup.class).list();
        }
    }

    public List<ClassGroup> findActive() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ClassGroup c WHERE c.active = true ORDER BY name", ClassGroup.class).list();
        }
    }

    public ClassGroup findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(ClassGroup.class, id);
        }
    }

    public ClassGroup findByName(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ClassGroup c WHERE LOWER(c.name) = LOWER(:name)", ClassGroup.class)
                    .setParameter("name", name)
                    .uniqueResult();
        }
    }

    public ClassGroup save(ClassGroup classGroup) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ClassGroup saved = session.merge(classGroup);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save class", e);
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ClassGroup classGroup = session.get(ClassGroup.class, id);
            if (classGroup != null) {
                session.remove(classGroup);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete class", e);
        }
    }
}