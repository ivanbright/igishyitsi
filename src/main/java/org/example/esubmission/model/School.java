package org.example.esubmission.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a school (tenant) in the multi-tenant e-submission system.
 * <p>
 * Every tenant-scoped entity ({@link Assignment}, {@link Submission},
 * {@link ClassGroup}, {@link Subject}, {@link StudentClass},
 * {@link TeacherClass}, {@link ParentChild}, {@link Announcement},
 * {@link Notification}) is scoped by its {@code schoolId}.
 * </p>
 */
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String domain;

    /**
     * Postal or physical address of the school, shown on its profile.
     */
    @Column(columnDefinition = "TEXT")
    private String address;

    /**
     * Public contact phone number of the school.
     */
    @Column(name = "phone")
    private String phone;

    /**
     * Public contact email address of the school.
     */
    @Column(name = "email")
    private String email;

    /**
     * URL of the school's logo, shown in the portal header.
     */
    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * Short description of the school, shown on its profile.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String plan = "FREE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public School() {}

    public School(String name, String slug) {
        this.name = name;
        this.slug = slug;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}