package org.example.esubmission.dao;

import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link ClassGroup} entity.
 */
public class ClassGroupDAO {

    public List<ClassGroup> findAll() {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ClassGroup c WHERE c.schoolId = :schoolId ORDER BY c.name", ClassGroup.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    public List<ClassGroup> findBySchoolId(Long schoolId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ClassGroup c WHERE c.schoolId = :schoolId ORDER BY c.name", ClassGroup.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    public List<ClassGroup> findActive() {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM ClassGroup c WHERE c.schoolId = :schoolId AND c.active = true ORDER BY c.name", ClassGroup.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    public ClassGroup findById(Long id) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            ClassGroup classGroup = session.get(ClassGroup.class, id);
            return classGroup != null && schoolId.equals(classGroup.getSchoolId()) ? classGroup : null;
        }
    }

    public ClassGroup findByName(String name) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM ClassGroup c WHERE c.schoolId = :schoolId AND LOWER(c.name) = LOWER(:name)", ClassGroup.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("name", name)
                    .uniqueResult();
        }
    }

    public ClassGroup save(ClassGroup classGroup) {
        Long schoolId = TenantContext.requireSchoolId();
        if (classGroup.getSchoolId() == null) {
            classGroup.setSchoolId(schoolId);
        }
        if (!schoolId.equals(classGroup.getSchoolId())) {
            throw new IllegalStateException("Class belongs to another school");
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (classGroup.getId() != null) {
                ClassGroup existing = session.get(ClassGroup.class, classGroup.getId());
                if (existing != null && !schoolId.equals(existing.getSchoolId())) {
                    throw new IllegalStateException("Class belongs to another school");
                }
            }
            ClassGroup saved = session.merge(classGroup);
            tx.commit();
            return saved;
          } catch (Exception e) {
              try {
                  if (tx != null && tx.isActive()) tx.rollback();
              } catch (Exception ignored) {
              }
              throw new RuntimeException("Failed to save class", e);
          }
    }

    public void delete(Long id) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ClassGroup classGroup = session.get(ClassGroup.class, id);
            if (classGroup != null && schoolId.equals(classGroup.getSchoolId())) {
                session.remove(classGroup);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete class", e);
        }
    }
}