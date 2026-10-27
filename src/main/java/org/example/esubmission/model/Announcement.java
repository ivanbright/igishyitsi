package org.example.esubmission.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a school announcement.
 */
@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The school (tenant) this announcement belongs to.
     */
    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private String audience;

    @Column
    private String priority;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    public Announcement() {}

    public Announcement(String title, String content, User author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
}
