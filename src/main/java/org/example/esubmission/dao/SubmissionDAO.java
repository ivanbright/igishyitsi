package org.example.esubmission.dao;

import org.example.esubmission.model.Submission;
import org.example.esubmission.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Data Access Object for {@link Submission} entity.
 * <p>
 * This class provides methods to manage assignment submissions by students.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class SubmissionDAO {
    
    /**
     * Saves a new submission or updates an existing one.
     *
     * @param submission the submission to save
     * @return the saved submission
     * @throws RuntimeException if the operation fails
     */
    public Submission save(Submission submission) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(submission);
            tx.commit();
            return submission;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save submission", e);
        }
    }

    /**
     * Retrieves all submissions for a specific assignment.
     *
     * @param assignmentId the ID of the assignment
     * @return a list of submissions, ordered by submission date descending
     */
    public List<Submission> findByAssignmentId(Long assignmentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Submission WHERE assignment.id = :assignmentId ORDER BY submittedAt DESC", Submission.class)
                    .setParameter("assignmentId", assignmentId)
                    .list();
        }
    }

    /**
     * Retrieves a submission for a specific assignment by a specific student.
     *
     * @param assignmentId the ID of the assignment
     * @param studentId    the ID of the student
     * @return the submission, or {@code null} if not found
     */
    public Submission findByAssignmentAndStudent(Long assignmentId, Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Submission> submissions = session.createQuery(
                    "FROM Submission WHERE assignment.id = :assignmentId AND student.id = :studentId", Submission.class)
                    .setParameter("assignmentId", assignmentId)
                    .setParameter("studentId", studentId)
                    .list();
            return submissions.isEmpty() ? null : submissions.get(0);
        }
    }

    /**
     * Retrieves all submissions made by a specific student.
     *
     * @param studentId the ID of the student
     * @return a list of submissions, ordered by submission date descending
     */
    public List<Submission> findByStudentId(Long studentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Submission WHERE student.id = :studentId ORDER BY submittedAt DESC", Submission.class)
                    .setParameter("studentId", studentId)
                    .list();
        }
    }

    /**
     * Retrieves a submission by its unique identifier.
     *
     * @param id the unique identifier of the submission
     * @return the submission entity, or {@code null} if not found
     */
    public Submission findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Submission.class, id);
        }
    }

    /**
     * Retrieves a submission for a specific student and assignment.
     *
     * @param studentId    the ID of the student
     * @param assignmentId the ID of the assignment
     * @return the submission, or {@code null} if not found
     */
    public Submission findByStudentAndAssignment(Long studentId, Long assignmentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Submission> submissions = session.createQuery(
                    "FROM Submission WHERE student.id = :studentId AND assignment.id = :assignmentId", Submission.class)
                    .setParameter("studentId", studentId)
                    .setParameter("assignmentId", assignmentId)
                    .list();
            return submissions.isEmpty() ? null : submissions.get(0);
        }
    }

    /**
     * Retrieves all submissions for assignments belonging to a specific teacher.
     *
     * @param teacherId the ID of the teacher
     * @return a list of submissions, ordered by submission date descending
     */
    public List<Submission> findByTeacherId(Long teacherId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Submission WHERE assignment.teacher.id = :teacherId ORDER BY submittedAt DESC", Submission.class)
                    .setParameter("teacherId", teacherId)
                    .list();
        }
    }

    /**
     * Counts the total number of submissions for all assignments of a specific teacher.
     *
     * @param teacherId the ID of the teacher
     * @return the count of submissions
     */
    public Long countByTeacherId(Long teacherId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT COUNT(s) FROM Submission s WHERE s.assignment.teacher.id = :teacherId", Long.class)
                    .setParameter("teacherId", teacherId)
                    .uniqueResult();
        }
    }

    /**
     * Retrieves all submissions in the system.
     *
     * @return a list of all submissions, ordered by submission date descending
     */
    public List<Submission> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Submission ORDER BY submittedAt DESC", Submission.class)
                    .list();
        }
    }
}
