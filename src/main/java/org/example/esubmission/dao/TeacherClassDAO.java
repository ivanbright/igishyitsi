package org.example.esubmission.dao;

import org.example.esubmission.model.TeacherClass;
import org.example.esubmission.model.User;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link TeacherClass} entity.
 */
public class TeacherClassDAO {

    /**
     * Returns the distinct class names a teacher is assigned to.
     *
     * @param teacherId the id of the teacher
     * @return list of class names
     */
    public List<String> findByTeacherId(Long teacherId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT tc.className FROM TeacherClass tc WHERE tc.schoolId = :schoolId AND tc.teacher.id = :teacherId ORDER BY tc.className", String.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .list();
        }
    }

    /**
     * Returns all teacher-class assignment records for a teacher.
     */
    public List<TeacherClass> findAssignmentsByTeacher(Long teacherId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM TeacherClass tc WHERE tc.schoolId = :schoolId AND tc.teacher.id = :teacherId ORDER BY tc.className", TeacherClass.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .list();
        }
    }

    /**
     * Returns the distinct teachers assigned to a class.
     *
     * @param className the class name
     * @return list of teachers
     */
    public List<User> findTeachersByClass(String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT tc.teacher FROM TeacherClass tc WHERE tc.schoolId = :schoolId AND tc.className = :className ORDER BY tc.teacher.name", User.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("className", className)
                    .list();
        }
    }

    public boolean exists(Long teacherId, String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(tc.id) FROM TeacherClass tc WHERE tc.schoolId = :schoolId AND tc.teacher.id = :teacherId AND tc.className = :className", Long.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .setParameter("className", className)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public long countByClassName(String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT COUNT(tc.teacher.id) FROM TeacherClass tc WHERE tc.schoolId = :schoolId AND tc.className = :className", Long.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("className", className)
                    .uniqueResult();
        }
    }

    public TeacherClass save(TeacherClass teacherClass) {
        Long schoolId = TenantContext.requireSchoolId();
        if (teacherClass.getSchoolId() == null) {
            teacherClass.setSchoolId(schoolId);
        }
        if (!schoolId.equals(teacherClass.getSchoolId())) {
            throw new IllegalStateException("Teacher class assignment belongs to another school");
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (teacherClass.getId() != null) {
                TeacherClass existing = session.get(TeacherClass.class, teacherClass.getId());
                if (existing != null && !schoolId.equals(existing.getSchoolId())) {
                    throw new IllegalStateException("Teacher class assignment belongs to another school");
                }
            }
            TeacherClass saved = session.merge(teacherClass);
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save teacher class assignment", e);
        }
    }

    public void remove(Long teacherId, String className) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createQuery("DELETE FROM TeacherClass tc WHERE tc.schoolId = :schoolId AND tc.teacher.id = :teacherId AND tc.className = :className")
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .setParameter("className", className)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to remove teacher class assignment", e);
        }
    }
}