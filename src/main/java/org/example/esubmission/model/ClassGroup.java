package org.example.esubmission.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a class (e.g. "S5 Software Engineering") as a first-class entity.
 * <p>
 * This entity is mapped to the {@code classes} table. Enrollment (students)
 * and teacher assignment are stored in {@link StudentClass} and
 * {@link TeacherClass} respectively, keyed by {@link #getName()}.
 * </p>
 */
@Entity
@Table(name = "classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_classes_school_name",
                columnNames = {"school_id", "name"}))
public class ClassGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The school (tenant) this class belongs to.
     */
    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ClassGroup() {}

    public ClassGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}