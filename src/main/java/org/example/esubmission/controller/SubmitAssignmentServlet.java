package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.model.Assignment;
import org.example.esubmission.service.NotificationService;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.model.StudentClass;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Servlet responsible for handling assignment submissions.
 * <p>
 * This servlet supports multi-part requests for file uploads and stores 
 * submitted files in the server's {@code uploads} directory.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
                 maxFileSize = 1024 * 1024 * 10,      // 10MB
                 maxRequestSize = 1024 * 1024 * 50)   // 50MB
public class SubmitAssignmentServlet extends HttpServlet {
    
    private AssignmentDAO assignmentDAO;
    private SubmissionDAO submissionDAO;
    private NotificationService notificationService;
    private StudentClassDAO studentClassDAO;
    private static final String UPLOAD_DIR = "uploads";

    /**
     * Initializes the servlet and ensures the upload directory exists.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.assignmentDAO = new AssignmentDAO();
        this.submissionDAO = new SubmissionDAO();
        this.notificationService = new NotificationService();
        this.studentClassDAO = new StudentClassDAO();
        
        // Create upload directory if it doesn't exist
        String applicationPath = getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;
        File uploadDir = new File(uploadFilePath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    /**
     * Handles GET requests to display the assignment submission form.
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User student = (User) session.getAttribute("user");

        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String assignmentIdStr = request.getParameter("id");
        if (assignmentIdStr == null || assignmentIdStr.isEmpty()) {
            response.sendError(400, "Assignment ID is required");
            return;
        }

        try {
            Long assignmentId = Long.parseLong(assignmentIdStr);
            Assignment assignment = assignmentDAO.findById(assignmentId);
            
            if (assignment == null) {
                response.sendError(404, "Assignment not found");
                return;
            }

            // Check if student is enrolled in the class
            boolean isEnrolled = false;
            List<StudentClass> studentClasses = studentClassDAO.findStudentClassesByStudent(student.getId());
            for (StudentClass sc : studentClasses) {
                if (sc.getClassName().equals(assignment.getClassName())) {
                    isEnrolled = true;
                    break;
                }
            }
            if (!isEnrolled) {
                response.sendError(403, "You are not enrolled in the class for this assignment.");
                return;
            }

            // Check if student is over-submitting an already graded submission
            Submission existingSubmission = submissionDAO.findByStudentAndAssignment(student.getId(), assignmentId);
            boolean gradedException = existingSubmission != null && existingSubmission.getGrade() != null;

            response.sendRedirect(request.getContextPath() + "/studentDash?submit=" + assignmentId
                    + (gradedException ? "&error=graded" : ""));
            
        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid assignment ID");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading assignment");
        }
    }

    /**
     * Handles POST requests to submit an assignment.
     * <p>
     * Processes file uploads, saves them to disk, and records the submission
     * in the database.
     * </p>
     *
     * @param request  the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User student = (User) session.getAttribute("user");

        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String assignmentIdStr = request.getParameter("assignmentId");
            String content = request.getParameter("content");
            Part filePart = request.getPart("file");

            if (assignmentIdStr == null || assignmentIdStr.isEmpty()) {
                response.sendError(400, "Assignment ID is required");
                return;
            }

            Long assignmentId = Long.parseLong(assignmentIdStr);
            Assignment assignment = assignmentDAO.findById(assignmentId);
            
            if (assignment == null) {
                response.sendError(404, "Assignment not found");
                return;
            }

            // Check if student is enrolled in the class
            boolean isEnrolled = false;
            List<StudentClass> studentClasses = studentClassDAO.findStudentClassesByStudent(student.getId());
            for (StudentClass sc : studentClasses) {
                if (sc.getClassName().equals(assignment.getClassName())) {
                    isEnrolled = true;
                    break;
                }
            }
            if (!isEnrolled) {
                response.sendError(403, "You are not enrolled in the class for this assignment.");
                return;
            }

            // Check if already submitted
            Submission submission = submissionDAO.findByStudentAndAssignment(student.getId(), assignmentId);
            boolean isResubmission = (submission != null);
            
            if (submission == null) {
                submission = new Submission();
            } else if (submission.getGrade() != null) {
                // Already graded, reject resubmission
                session.setAttribute("error", "Cannot resubmit: Your assignment has already been graded.");
                response.sendRedirect(request.getContextPath() + "/studentDash");
                return;
            }

            // Handle file upload
            String fileUrl = submission.getFileUrl(); // Keep old file by default
            if (filePart != null && filePart.getSize() > 0) {
                String fileName = extractFileName(filePart);
                if (fileName != null && !fileName.isEmpty()) {
                    fileUrl = saveFile(filePart, fileName);
                }
            }

            // Optional: Google Form response URL  
            String formResponseUrl = request.getParameter("formResponseUrl");

            // Update submission
            submission.setAssignment(assignment);
            submission.setStudent(student);
            submission.setContent(content);
            submission.setFileUrl(fileUrl);
            if (formResponseUrl != null && !formResponseUrl.trim().isEmpty()) {
                submission.setFormResponseUrl(formResponseUrl.trim());
            }

            // Detect late submission
            if (assignment.getDeadline() != null && java.time.LocalDateTime.now().isAfter(assignment.getDeadline())) {
                submission.setStatus("LATE");
            } else {
                submission.setStatus("ON_TIME");
            }

            // Save submission (create or update)
            submissionDAO.save(submission);

            // Notify the teacher
            try {
                if (isResubmission) {
                    notificationService.notifyNewSubmission(
                        assignment.getTeacher(), student.getName(),
                        assignment.getTitle() + " (Resubmission)", assignment.getId());
                } else {
                    notificationService.notifyNewSubmission(
                        assignment.getTeacher(), student.getName(),
                        assignment.getTitle(), assignment.getId());
                }
            } catch (Exception ne) {
                System.err.println("Failed to send submission notification: " + ne.getMessage());
            }

            // Broadcast new submission to the teacher
            String titleText = assignment.getTitle().replace("\"", "\\\"");
            if (isResubmission) titleText += " (Resubmission)";
            String jsonMessage = String.format("{\"type\":\"NEW_SUBMISSION\", \"studentName\":\"%s\", \"assignmentTitle\":\"%s\"}",
                student.getName().replace("\"", "\\\""),
                titleText
            );
            org.example.esubmission.websocket.AssignmentWebSocket.broadcastToUser(assignment.getTeacher().getId(), jsonMessage);

            // Redirect to dashboard with success message
            session.setAttribute("message", isResubmission ? "Assignment resubmitted successfully!" : "Assignment submitted successfully!");
            response.sendRedirect(request.getContextPath() + "/studentDash");

        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid assignment ID");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error submitting assignment");
        }
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return null;
    }

    private String saveFile(Part filePart, String fileName) throws IOException {
        String applicationPath = getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;
        
        // Sanitize filename: remove path separators and dangerous chars
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.isEmpty()) safeName = "upload";
        
        // Create unique filename to avoid conflicts
        String uniqueFileName = System.currentTimeMillis() + "_" + safeName;
        Path filePath = Paths.get(uploadFilePath, uniqueFileName);
        
        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return UPLOAD_DIR + "/" + uniqueFileName;
    }
}
