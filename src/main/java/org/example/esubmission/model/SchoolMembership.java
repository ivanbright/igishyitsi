package org.example.esubmission.model;

import jakarta.persistence.*;
import org.example.esubmission.util.UserRole;
import java.time.LocalDateTime;

/**
 * Represents a user's membership in a school with a per-school role.
 * <p>
 * A single {@link User} may belong to many {@link School}s and hold a
 * different {@link UserRole} in each one (e.g. Teacher at school A,
 * Admin at school B). Permissions are resolved against the membership
 * role of the currently active school.
 * </p>
 */
@Entity
@Table(name = "school_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_school_members_school_user",
                columnNames = {"school_id", "user_id"}))
public class SchoolMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public SchoolMembership() {}

    public SchoolMembership(School school, User user, UserRole role) {
        this.school = school;
        this.user = user;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}