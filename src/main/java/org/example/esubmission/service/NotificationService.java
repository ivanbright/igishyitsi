package org.example.esubmission.service;

import org.example.esubmission.dao.NotificationDAO;
import org.example.esubmission.model.Notification;
import org.example.esubmission.model.School;
import org.example.esubmission.model.User;
import org.example.esubmission.util.TenantContext;

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
     * Creates a notification for a user, scoped to the active school.
     */
    public Notification notify(User user, String title, String message, String type, String link) {
        Notification n = new Notification(user, title, message, type, link);
        return notificationDAO.save(n);
    }

    /**
     * Creates a notification for a user in an explicitly named school, for
     * platform-level work performed outside any tenant context.
     * <p>
     * Provisioning a school and managing its accounts happens while the platform
     * administrator has no tenant, so those notifications cannot use the
     * tenant-scoped write. Callers must already hold platform-administrator
     * access.
     * </p>
     */
    public Notification notifyForSchool(School school, User user, String title, String message, String type, String link) {
        Notification n = new Notification(user, title, message, type, link);
        n.setSchoolId(school.getId());
        return notificationDAO.saveForPlatform(n);
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
     * Retrieves all notifications for a user within the active school.
     * <p>
     * A user with no active school, such as a platform administrator, has no
     * notifications to show rather than an error, so this returns an empty list
     * instead of failing the whole request.
     * </p>
     */
    public List<Notification> getNotifications(Long userId) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return List.of();
        }
        return notificationDAO.findByUserId(userId, schoolId);
    }

    /**
     * Retrieves unread notification count for a user within the active school,
     * or zero when the user has no active school.
     */
    public Long getUnreadCount(Long userId) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return 0L;
        }
        return notificationDAO.countUnreadByUserId(userId, schoolId);
    }

    /**
     * Marks all notifications as read for a user within the active school.
     * No-op when the user has no active school.
     */
    public void markAllRead(Long userId) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return;
        }
        notificationDAO.markAllReadByUserId(userId, schoolId);
    }

    /**
     * Marks a single notification as read, only if it belongs to the given user.
     */
    public void markRead(Long userId, Long notificationId) {
        notificationDAO.markReadForUser(userId, notificationId);
    }
}
