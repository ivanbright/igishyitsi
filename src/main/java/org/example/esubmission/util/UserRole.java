package org.example.esubmission.util;

/**
 * Enumeration of user roles in the system.
 * <p>
 * Defines the different types of users and their descriptive labels.
 * </p>
 */
public enum UserRole {
    SUPER_ADMIN("Super Administrator"),
    ADMIN("Administrator"),
    STUDENT("Student"),
    TEACHER("Teacher"),
    PARENT("Parent");

    private String description;
    UserRole(String description) {
        this.description = description;
    }

    /**
     * Returns the descriptive label for the role.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
