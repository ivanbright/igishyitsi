package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;
import org.example.esubmission.service.NotificationService;
import org.example.esubmission.util.SecurityUtil;

import java.io.IOException;

/**
 * Servlet for handling assignment grading by teachers.
 * <p>
 * Validates that the teacher owns the assignment, accepts grade and feedback,
 * updates the submission status to GRADED, and notifies the student.
 * </p>
 */
@WebServlet("/teacher/grade")
public class GradeSubmissionServlet extends HttpServlet {

    private SubmissionDAO submissionDAO;
    private NotificationService notificationService;

    @Override
    public void init() throws ServletException {
        this.submissionDAO = new SubmissionDAO();
        this.notificationService = new NotificationService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        User user = SecurityUtil.requireTeacher(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only teachers can grade assignments.");
            return;
        }
        if (!SecurityUtil.csrfValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String submissionIdStr = request.getParameter("submissionId");
        String gradeStr = request.getParameter("grade");
        String feedback = request.getParameter("feedback");

        if (submissionIdStr == null || gradeStr == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters.");
            return;
        }

        try {
            Long submissionId = Long.parseLong(submissionIdStr);
            int grade = Integer.parseInt(gradeStr);

            Submission submission = submissionDAO.findById(submissionId);
            if (submission == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Submission not found.");
                return;
            }

            // Verify teacher owns this assignment (IDOR protection)
            if (!submission.getAssignment().getTeacher().getId().equals(user.getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "You can only grade your own assignments.");
                return;
            }

            // Validate grade is within range
            int maxPoints = submission.getAssignment().getPoints() != null ? submission.getAssignment().getPoints() : 100;
            if (grade < 0 || grade > maxPoints) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Grade must be between 0 and " + maxPoints + "\"}");
                return;
            }

            submission.setGrade(grade);
            submission.setStatus("GRADED");
            if (feedback != null && !feedback.trim().isEmpty()) {
                submission.setFeedback(feedback.trim());
            }
            submissionDAO.save(submission);

            // Notify the student
            try {
                notificationService.notifyGraded(
                    submission.getStudent(),
                    submission.getAssignment().getTitle(),
                    grade, maxPoints);
            } catch (Exception ne) {
                System.err.println("Failed to send grade notification: " + ne.getMessage());
            }

            // Real-time WebSocket push to student
            try {
                String safeTitle = submission.getAssignment().getTitle().replace("\"", "\\\"");
                String safeFeedback = feedback != null ? feedback.replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") : "";
                String wsMessage = String.format("{\"type\":\"GRADE_POSTED\", \"assignmentId\":%d, \"title\":\"%s\", \"grade\":%d, \"maxPoints\":%d, \"feedback\":\"%s\"}",
                    submission.getAssignment().getId(),
                    safeTitle,
                    grade,
                    maxPoints,
                    safeFeedback
                );
                org.example.esubmission.websocket.AssignmentWebSocket.broadcastToUser(submission.getStudent().getId(), wsMessage);
            } catch (Exception we) {
                System.err.println("Failed to send grade websocket: " + we.getMessage());
            }

            response.setContentType("application/json");
            response.getWriter().write("{\"success\": true, \"message\": \"Grade published successfully\"}");

        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid format.");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving grade.");
        }
    }
}
