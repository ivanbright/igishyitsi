package org.example.esubmission.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a parent&#8211;student relationship.
 * <p>
 * This entity is mapped to the {@code parent_children} table. It links a
 * {@link User} with the PARENT role to a {@link User} with the STUDENT role,
 * giving the parent visibility into that student's record.
 * </p>
 */
@Entity
@Table(name = "parent_children",
        uniqueConstraints = @UniqueConstraint(name = "uk_parent_children_school_parent_student",
                columnNames = {"school_id", "parent_id", "student_id"}))
public class ParentChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The school (tenant) this parent-child link belongs to.
     */
    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @ManyToOne
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
}