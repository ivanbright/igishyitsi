package org.example.esubmission.service;

import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.exception.UserNotFoundException;
import org.example.esubmission.model.User;
import org.example.esubmission.util.PasswordUtils;
import org.example.esubmission.util.SecurityUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for managing user-related business logic.
 * <p>
 * This class handles user retrieval, registration, and authentication
 * using {@link UserDAO}.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class UserService {
    private final UserDAO userDAO;

    /**
     * Constructs a new UserService with the required DAO.
     *
     * @param userDAO the DAO for users
     */
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the unique identifier of the user
     * @return the user
     * @throws IllegalArgumentException if the user does not exist
     */
    public User getUser(Long id) {
        User user = userDAO.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("User with id " + id + " does not exist");
        }
        return user;
    }

    /**
     * Retrieves all users in the system.
     *
     * @return a list of all users
     * @throws UserNotFoundException if no users are found
     */
    public List<User> getUsers() {
            List<User> users = userDAO.findAll();

            if(users.isEmpty()) {
                throw new UserNotFoundException("No users found");
            }
            return users;
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address of the user
     * @return the user
     * @throws UserNotFoundException if the user does not exist
     */
    public User getUserByEmail(String email) {
            User  user = userDAO.findByEmail(email);
            if(user == null) {
                throw new UserNotFoundException("User with email " + email + " does not exist");
            }
            return user;
    }


    /**
     * Creates and persists a new user account.
     * <p>
     * Hashes the password before saving for security.
     * </p>
     *
     * @param name     the full name of the user
     * @param email    the email address
     * @param phone    the phone number
     * @param password the plain-text password to hash and store
     * @return the created user
     * @throws IllegalArgumentException if a user with the same email already exists
     */
    public User createUser(String name, String email, String phone, String password) {
        return createUser(name, email, phone, password, org.example.esubmission.util.UserRole.STUDENT);
    }

    /**
     * Creates and persists a new user account with a specific role.
     *
     * @param name     the full name of the user
     * @param email    the email address
     * @param phone    the phone number
     * @param password the plain-text password to hash and store
     * @param role     the user role
     * @return the created user
     * @throws IllegalArgumentException if a user with the same email already exists
     */
    public User createUser(String name, String email, String phone, String password, org.example.esubmission.util.UserRole role) {
        User userExists = userDAO.findByEmail(email);

        if (userExists != null) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        
        User newUser = new User();
        newUser.setName(name);
        newUser.setPassword(PasswordUtils.hashPassword(password));
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setRole(role);

        return userDAO.save(newUser);
    }

    /**
     * Authenticates a user by email and password.
     *
     * @param email    the user's email
     * @param password the user's plain-text password
     * @return the authenticated user
     * @throws IllegalArgumentException if authentication fails
     */
    public User authenticateUser(String email, String password) {
        User user = userDAO.findByEmail(email);
        
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        return user;
    }

    /**
     * Generates a new verification code for the user and sets its expiry.
     *
     * @param user the user to generate the code for
     * @return the generated code
     */
    public String generateVerificationCode(User user) {
        String code = SecurityUtil.generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationExpiry(LocalDateTime.now().plusMinutes(10));
        userDAO.save(user); // Using save for update as well
        return code;
    }

    /**
     * Verifies the provided code for the user.
     *
     * @param email the user's email
     * @param code  the code to verify
     * @return true if verification is successful
     */
    public boolean verifyCode(String email, String code) {
        User user = userDAO.findByEmail(email);
        if (user == null || user.getVerificationCode() == null) {
            return false;
        }

        if (user.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (user.getVerificationCode().equals(code)) {
            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            userDAO.save(user);
            return true;
        }

        return false;
    }

    /**
     * Initiates the password reset process by generating a token.
     *
     * @param email the user's email
     * @return the generated reset token
     * @throws UserNotFoundException if the user does not exist
     */
    public String initiatePasswordReset(String email) {
        User user = getUserByEmail(email);
        String token = SecurityUtil.generateVerificationCode();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userDAO.save(user);
        return token;
    }

    /**
     * Resets the user's password using a valid token.
     *
     * @param token       the reset token
     * @param newPassword the new password
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    public void resetPassword(String token, String newPassword) {
        User user = userDAO.findByResetToken(token);
        if (user == null || user.getResetTokenExpiry() == null ||
            user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        user.setPassword(PasswordUtils.hashPassword(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userDAO.save(user);
    }
}
