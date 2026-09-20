package org.example.esubmission.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents the assignment of a teacher to a class.
 * <p>
 * This entity is mapped to the {@code teacher_classes} table. It is the
 * persisted, authorization-relevant relationship that governs which classes
 * a teacher may open for assignment authoring and which students they see.
 * A teacher may be assigned to a class that has no students yet.
 * </p>
 */
@Entity
@Table(name = "teacher_classes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "class_name"}))
public class TeacherClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}