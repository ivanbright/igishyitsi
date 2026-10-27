package org.example.esubmission.dao;

import org.example.esubmission.model.School;
import org.example.esubmission.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link School} (tenant) entity.
 */
public class SchoolDAO {

    public List<School> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM School ORDER BY name", School.class).list();
        }
    }

    public School findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(School.class, id);
        }
    }

    public School findBySlug(String slug) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM School s WHERE s.slug = :slug", School.class)
                    .setParameter("slug", slug)
                    .uniqueResult();
        }
    }

    /**
     * Returns the default (first created) school. Used as the tenant fallback
     * for accounts that have no membership yet, preserving single-tenant
     * behaviour during the migration to multi-tenancy.
     */
    public School findDefault() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM School s ORDER BY s.id ASC", School.class)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public School save(School school) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            School saved = session.merge(school);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save school", e);
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            School school = session.get(School.class, id);
            if (school != null) {
                session.remove(school);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete school", e);
        }
    }
}