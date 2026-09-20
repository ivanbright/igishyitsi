package org.example.esubmission.service;

import org.example.esubmission.dao.NotificationDAO;
import org.example.esubmission.model.Notification;
import org.example.esubmission.model.User;

import java.util.List;

/**
 * Service for creating and managing notifications tied to domain events.
 */
public class NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    public NotificationService(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    /**
     * Creates a notification for a user.
     */
    public Notification notify(User user, String title, String message, String type, String link) {
        Notification n = new Notification(user, title, message, type, link);
        return notificationDAO.save(n);
    }

    /**
     * Notifies a student that a new assignment has been published.
     */
    public void notifyNewAssignment(User student, String assignmentTitle, String className, Long assignmentId) {
        notify(student,
                "New Assignment: " + assignmentTitle,
                "A new assignment has been published for " + className + ".",
                "NEW_ASSIGNMENT",
                "/student/submitAssignment?id=" + assignmentId);
    }

    /**
     * Notifies a teacher that a student has submitted their work.
     */
    public void notifyNewSubmission(User teacher, String studentName, String assignmentTitle, Long assignmentId) {
        notify(teacher,
                studentName + " submitted work",
                studentName + " submitted their work for \"" + assignmentTitle + "\".",
                "NEW_SUBMISSION",
                "/teacher/assignment?id=" + assignmentId);
    }

    /**
     * Notifies a student that their submission has been graded.
     */
    public void notifyGraded(User student, String assignmentTitle, int grade, int maxPoints) {
        notify(student,
                "Grade Published: " + assignmentTitle,
                "Your submission for \"" + assignmentTitle + "\" has been graded: " + grade + "/" + maxPoints + ".",
                "GRADE_PUBLISHED",
                "/student/grades");
    }

    /**
     * Notifies a user about a new announcement.
     */
    public void notifyAnnouncement(User user, String announcementTitle) {
        notify(user,
                "New Announcement",
                announcementTitle,
                "ANNOUNCEMENT",
                null);
    }

    /**
     * Retrieves all notifications for a user.
     */
    public List<Notification> getNotifications(Long userId) {
        return notificationDAO.findByUserId(userId);
    }

    /**
     * Retrieves unread notification count for a user.
     */
    public Long getUnreadCount(Long userId) {
        return notificationDAO.countUnreadByUserId(userId);
    }

    /**
     * Marks all notifications as read for a user.
     */
    public void markAllRead(Long userId) {
        notificationDAO.markAllReadByUserId(userId);
    }

    /**
     * Marks a single notification as read, only if it belongs to the given user.
     */
    public void markRead(Long userId, Long notificationId) {
        notificationDAO.markReadForUser(userId, notificationId);
    }
}
