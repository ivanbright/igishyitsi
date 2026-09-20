package org.example.esubmission.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a notification sent to a user about a domain event.
 * <p>
 * Notifications are tied to real events such as new assignments,
 * submissions, grades, and announcements.
 * </p>
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who receives this notification. */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Short notification title. */
    @Column(nullable = false)
    private String title;

    /** Notification message body. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Type of notification event (e.g., NEW_ASSIGNMENT, SUBMISSION_GRADED). */
    @Column(nullable = false, length = 50)
    private String type;

    /** Whether the user has read this notification. */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /** Optional link to navigate to (e.g., /student/submitAssignment?id=5). */
    @Column(length = 512)
    private String link;

    /** When the notification was created. */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Notification(User user, String title, String message, String type, String link) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type;
        this.link = link;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
    }
}
