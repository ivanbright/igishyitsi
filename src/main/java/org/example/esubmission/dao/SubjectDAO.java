package org.example.esubmission.dao;

import org.example.esubmission.model.Subject;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for Subject entity.
 */
public class SubjectDAO {

    public List<Subject> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long schoolId = TenantContext.requireSchoolId();
            return session.createQuery("FROM Subject s WHERE s.schoolId = :schoolId ORDER BY s.code", Subject.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    public List<Subject> findAllGlobal() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Subject ORDER BY code", Subject.class).list();
        }
    }

    public Subject findById(Long id) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Subject subject = session.get(Subject.class, id);
            return subject != null && schoolId.equals(subject.getSchoolId()) ? subject : null;
        }
    }

    public Subject save(Subject subject) {
        Long schoolId = TenantContext.requireSchoolId();
        if (subject.getSchoolId() == null) {
            subject.setSchoolId(schoolId);
        }
        if (!schoolId.equals(subject.getSchoolId())) {
            throw new IllegalStateException("Subject belongs to another school");
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (subject.getId() != null) {
                Subject existing = session.get(Subject.class, subject.getId());
                if (existing != null && !schoolId.equals(existing.getSchoolId())) {
                    throw new IllegalStateException("Subject belongs to another school");
                }
            }
            Subject saved = session.merge(subject);
            tx.commit();
            return saved;
        } catch (Exception e) {
            try {
                if (tx != null) tx.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to save subject", e);
        }
    }

    public void delete(Long id) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Subject subject = session.get(Subject.class, id);
            if (subject != null && schoolId.equals(subject.getSchoolId())) {
                session.remove(subject);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete subject", e);
        }
    }
}
