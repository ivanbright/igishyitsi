package org.example.esubmission.service;

import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.dao.TeacherClassDAO;
import org.example.esubmission.model.Assignment;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service class for managing assignment-related business logic.
 * <p>
 * This class orchestrates operations between {@link AssignmentDAO}, 
 * {@link SubmissionDAO}, and {@link StudentClassDAO}.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class AssignmentService {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(AssignmentService.class.getName());

    private final AssignmentDAO assignmentDAO;
    private final SubmissionDAO submissionDAO;
    private final StudentClassDAO studentClassDAO;

    /**
     * Constructs a new AssignmentService with required DAOs.
     *
     * @param assignmentDAO   the DAO for assignments
     * @param submissionDAO   the DAO for submissions
     * @param studentClassDAO the DAO for student-class relationships
     */
    public AssignmentService(AssignmentDAO assignmentDAO, SubmissionDAO submissionDAO, StudentClassDAO studentClassDAO) {
        this.assignmentDAO = assignmentDAO;
        this.submissionDAO = submissionDAO;
        this.studentClassDAO = studentClassDAO;
    }

    /**
     * Creates and persists a new assignment.
     *
     * @param teacher     the teacher creating the assignment
     * @param title       the title of the assignment
     * @param description the description of the assignment
     * @param className   the name of the class the assignment is for
     * @param deadline    the submission deadline
     * @return the created assignment
     */
    public Assignment createAssignment(User teacher, String title, String description, String className, LocalDateTime deadline, String googleFormUrl, Integer points) {
        if (className == null
                || !teacherClassNames(teacher.getId()).stream()
                        .map(c -> c == null ? "" : c)
                        .anyMatch(c -> c.trim().equalsIgnoreCase(className.trim()))) {
            throw new IllegalArgumentException("You can only create assignments for classes that you teach.");
        }
        Assignment assignment = new Assignment();
        assignment.setTeacher(teacher);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setClassName(className);
        assignment.setDeadline(deadline);
        if (googleFormUrl != null && !googleFormUrl.trim().isEmpty()) {
            assignment.setGoogleFormUrl(googleFormUrl.trim());
        }
        assignment.setPoints(points != null ? points : 100);
        Assignment savedAssignment = assignmentDAO.save(assignment);
        
        // Broadcast new assignment to the class using JSON string
        String jsonMessage = String.format("{\"type\":\"NEW_ASSIGNMENT\", \"id\":%d, \"title\":\"%s\", \"description\":\"%s\", \"course\":\"%s\", \"dueDate\":\"%s\", \"points\":%d}", 
            savedAssignment.getId(),
            title.replace("\"", "\\\""), 
            description == null ? "" : description.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"),
            className.replace("\"", "\\\""), 
            deadline != null ? deadline.toString() : "",
            savedAssignment.getPoints()
        );
        org.example.esubmission.websocket.AssignmentWebSocket.broadcastToClass(teacher.getId(), className, jsonMessage);

        notifyEnrolledStudents(savedAssignment, className.trim());

        return savedAssignment;
    }

    /**
     * Notifies every student enrolled in the class that a new assignment exists.
     * <p>
     * The WebSocket broadcast above only reaches clients that happen to be
     * connected at that moment, so this persists the notification as well;
     * otherwise a student who was offline would never learn about the
     * assignment. Notification failures are logged and never fail the
     * assignment itself, which is already saved by this point.
     * </p>
     */
    private void notifyEnrolledStudents(Assignment assignment, String className) {
        List<User> students;
        try {
            students = studentClassDAO.findStudentsInClass(className);
        } catch (Exception e) {
            LOGGER.warning("Could not resolve students for class " + className + ": " + e.getMessage());
            return;
        }
        NotificationService notificationService = new NotificationService();
        for (User student : students) {
            try {
                notificationService.notifyNewAssignment(student, assignment.getTitle(), className, assignment.getId());
            } catch (Exception e) {
                LOGGER.warning("Could not notify " + student.getEmail() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Retrieves all assignments created by a specific teacher.
     *
     * @param teacherId the unique identifier of the teacher
     * @return a list of assignments
     */
    public List<Assignment> getTeacherAssignments(Long teacherId) {
        return assignmentDAO.findByTeacherId(teacherId);
    }

    /**
     * Retrieves an assignment by its ID.
     *
     * @param id the unique identifier of the assignment
     * @return the assignment
     */
    public Assignment getAssignment(Long id) {
        return assignmentDAO.findById(id);
    }

    /**
     * Retrieves all submissions for a specific assignment.
     *
     * @param assignmentId the unique identifier of the assignment
     * @return a list of submissions
     */
    public List<Submission> getAssignmentSubmissions(Long assignmentId) {
        return submissionDAO.findByAssignmentId(assignmentId);
    }

    /**
     * Returns the union of classes the teacher owns through student enrollments
     * and through explicit teacher-class assignments.
     */
    private Set<String> teacherClassNames(Long teacherId) {
        Set<String> names = new LinkedHashSet<>();
        List<String> fromEnrollments = studentClassDAO.findClassesByTeacher(teacherId);
        List<String> fromAssignments = new TeacherClassDAO().findByTeacherId(teacherId);
        if (fromEnrollments != null) names.addAll(fromEnrollments);
        if (fromAssignments != null) names.addAll(fromAssignments);
        return names;
    }

    /**
     * Identifies students who have not yet submitted a specific assignment.
     *
     * @param assignmentId the unique identifier of the assignment
     * @param className    the name of the class
     * @param teacherId    the ID of the teacher
     * @return a list of students who are missing a submission
     */
    public List<User> getMissingStudents(Long assignmentId, String className, Long teacherId) {
        List<User> allStudents = studentClassDAO.findStudentsByTeacherAndClass(teacherId, className);
        List<Submission> submissions = submissionDAO.findByAssignmentId(assignmentId);
        
        allStudents.removeIf(student -> 
            submissions.stream().anyMatch(sub -> sub.getStudent().getId().equals(student.getId()))
        );
        
        return allStudents;
    }
}
