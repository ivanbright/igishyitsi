package org.example.esubmission.dao;

import org.example.esubmission.model.StudentClass;
import org.example.esubmission.model.User;
import org.example.esubmission.util.HibernateUtil;
import org.example.esubmission.util.TenantContext;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link StudentClass}, the link between a student and
 * the class they are enrolled in under a given teacher.
 * <p>
 * Every read and write is scoped to the active school. Class names and teacher
 * ids are not unique across the platform, so an unscoped query here would return
 * another school's classes, teachers and students.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class StudentClassDAO {

    /**
     * Saves a new student-class allocation.
     *
     * @param studentClass the student-class allocation to save
     * @return the saved entity
     * @throws RuntimeException if the operation fails
     */
    public StudentClass save(StudentClass studentClass) {
        Long schoolId = TenantContext.requireSchoolId();
        if (studentClass.getSchoolId() == null) {
            studentClass.setSchoolId(schoolId);
        }
        if (!schoolId.equals(studentClass.getSchoolId())) {
            throw new IllegalStateException("Enrollment belongs to another school");
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            StudentClass saved = session.merge(studentClass);
            tx.commit();
            return saved;
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to save student class", e);
        }
    }

    /**
     * Retrieves students enrolled in a specific class under a specific teacher.
     *
     * @param teacherId the ID of the teacher
     * @param className the name of the class
     * @return a list of students, ordered by name
     */
    public List<User> findStudentsByTeacherAndClass(Long teacherId, String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT sc.student FROM StudentClass sc WHERE sc.schoolId = :schoolId "
                            + "AND sc.teacher.id = :teacherId AND LOWER(sc.className) = LOWER(:className) "
                            + "ORDER BY sc.student.name",
                    User.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .setParameter("className", className)
                    .list();
        }
    }

    /**
     * Retrieves the distinct students enrolled in a class, whichever teacher
     * teaches it.
     * <p>
     * This is the recipient set for class-wide events such as a published
     * assignment. A student may hold more than one enrollment for the same
     * class under different teachers, so results are de-duplicated.
     * </p>
     *
     * @param className the name of the class
     * @return a list of distinct students
     */
    public List<User> findStudentsInClass(String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT sc.student FROM StudentClass sc WHERE sc.schoolId = :schoolId "
                            + "AND LOWER(sc.className) = LOWER(:className) ORDER BY sc.student.name",
                    User.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("className", className)
                    .list();
        }
    }

    /**
     * Retrieves all unique class names associated with a specific teacher.
     *
     * @param teacherId the ID of the teacher
     * @return a list of class names, ordered alphabetically
     */
    public List<String> findClassesByTeacher(Long teacherId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT sc.className FROM StudentClass sc WHERE sc.schoolId = :schoolId "
                            + "AND sc.teacher.id = :teacherId ORDER BY sc.className",
                    String.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .list();
        }
    }

    /**
     * Retrieves all unique class names in the active school.
     *
     * @return a list of class names, ordered alphabetically
     */
    public List<String> findAllClasses() {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT sc.className FROM StudentClass sc WHERE sc.schoolId = :schoolId ORDER BY sc.className",
                    String.class)
                    .setParameter("schoolId", schoolId)
                    .list();
        }
    }

    /**
     * Retrieves all student-class allocations for a specific student.
     *
     * @param studentId the ID of the student
     * @return a list of StudentClass entities, ordered by class name
     */
    public List<StudentClass> findStudentClassesByStudent(Long studentId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM StudentClass sc WHERE sc.schoolId = :schoolId AND sc.student.id = :studentId ORDER BY sc.className",
                    StudentClass.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("studentId", studentId)
                    .list();
        }
    }

    /**
     * Counts the total number of unique students enrolled in all classes of a teacher.
     *
     * @param teacherId the ID of the teacher
     * @return the count of unique students
     */
    public Long countStudentsByTeacher(Long teacherId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT COUNT(DISTINCT sc.student.id) FROM StudentClass sc WHERE sc.schoolId = :schoolId "
                            + "AND sc.teacher.id = :teacherId",
                    Long.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .uniqueResult();
        }
    }

    /**
     * Retrieves all enrollment records for a teacher.
     *
     * @param teacherId the ID of the teacher
     * @return a list of StudentClass records
     */
    public List<StudentClass> findEnrollmentsByTeacher(Long teacherId) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM StudentClass sc WHERE sc.schoolId = :schoolId AND sc.teacher.id = :teacherId ORDER BY sc.student.name",
                    StudentClass.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("teacherId", teacherId)
                    .list();
        }
    }

    /**
     * Retrieves all enrollment records for a class name (any teacher).
     *
     * @param className the name of the class
     * @return a list of StudentClass records ordered by student name
     */
    public List<StudentClass> findEnrollmentsByClassName(String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM StudentClass sc WHERE sc.schoolId = :schoolId AND LOWER(sc.className) = LOWER(:className) "
                            + "ORDER BY sc.student.name",
                    StudentClass.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("className", className)
                    .list();
        }
    }

    /**
     * Retrieves the distinct teachers who have students enrolled in a class.
     *
     * @param className the name of the class
     * @return a list of teachers ordered by name
     */
    public List<User> findTeachersByClass(String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT sc.teacher FROM StudentClass sc WHERE sc.schoolId = :schoolId "
                            + "AND LOWER(sc.className) = LOWER(:className) ORDER BY sc.teacher.name",
                    User.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("className", className)
                    .list();
        }
    }

    /**
     * Retrieves a single enrollment record for a student in a class.
     *
     * @param studentId the ID of the student
     * @param className the name of the class
     * @return the enrollment record or {@code null}
     */
    public StudentClass findEnrollment(Long studentId, String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM StudentClass sc WHERE sc.schoolId = :schoolId AND sc.student.id = :studentId "
                            + "AND LOWER(sc.className) = LOWER(:className)",
                    StudentClass.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("studentId", studentId)
                    .setParameter("className", className)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    /**
     * Removes a student's enrollment in a class (all teacher rows).
     *
     * @param studentId the ID of the student
     * @param className the name of the class
     */
    public void removeByStudentAndClass(Long studentId, String className) {
        Long schoolId = TenantContext.requireSchoolId();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createQuery(
                    "DELETE FROM StudentClass sc WHERE sc.schoolId = :schoolId AND sc.student.id = :studentId "
                            + "AND LOWER(sc.className) = LOWER(:className)")
                    .setParameter("schoolId", schoolId)
                    .setParameter("studentId", studentId)
                    .setParameter("className", className)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to remove student enrollment", e);
        }
    }

    /**
     * Counts the distinct students enrolled in a class (any teacher).
     *
     * @param className the name of the class
     * @return the number of distinct students
     */
    public Long countStudentsByClassName(String className) {
        Long schoolId = TenantContext.requireSchoolId();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT COUNT(DISTINCT sc.student.id) FROM StudentClass sc WHERE sc.schoolId = :schoolId "
                            + "AND LOWER(sc.className) = LOWER(:className)",
                    Long.class)
                    .setParameter("schoolId", schoolId)
                    .setParameter("className", className)
                    .uniqueResult();
        }
    }
}
