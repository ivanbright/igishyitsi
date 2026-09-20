package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;
import org.example.esubmission.util.UserRole;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servlet responsible for displaying and managing submission details.
 * <p>
 * This servlet allows students, teachers, and admins to view submission information
 * and download submitted files securely.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class SubmissionDetailsServlet extends HttpServlet {
    
    private SubmissionDAO submissionDAO;

    /**
     * Initializes the servlet and its DAO.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        this.submissionDAO = new SubmissionDAO();
    }

    /**
     * Handles GET requests to view submission details.
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
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if ("download".equalsIgnoreCase(action) || "true".equalsIgnoreCase(request.getParameter("download"))) {
            doPost(request, response);
            return;
        }

        String submissionIdStr = request.getParameter("id");
        if (submissionIdStr == null || submissionIdStr.isEmpty()) {
            response.sendError(400, "Submission ID is required");
            return;
        }

        try {
            Long submissionId = Long.parseLong(submissionIdStr);
            Submission submission = submissionDAO.findById(submissionId);
            
            if (submission == null) {
                response.sendError(404, "Submission not found");
                return;
            }

            // Verify permission: Student owner, assignment Teacher, or Admin
            boolean isStudentOwner = submission.getStudent() != null && submission.getStudent().getId().equals(currentUser.getId());
            boolean isTeacherOwner = submission.getAssignment() != null && submission.getAssignment().getTeacher() != null && submission.getAssignment().getTeacher().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

            if (!isStudentOwner && !isTeacherOwner && !isAdmin) {
                response.sendError(403, "Access denied");
                return;
            }

            boolean isTeacherViewing = !isStudentOwner && (isTeacherOwner || isAdmin);

            request.setAttribute("submission", submission);
            request.setAttribute("backLink", isTeacherViewing ? "/teacher/dashboard" : "/mySubmissions");
            request.getRequestDispatcher("/WEB-INF/pages/submissionDetails.jsp").forward(request, response);
            
        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid submission ID");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading submission details");
        }
    }

    /**
     * Handles POST requests for submission management (e.g., file downloads).
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
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        String submissionIdStr = request.getParameter("submissionId");

        if ("download".equals(action)) {
            try {
                Long submissionId = Long.parseLong(submissionIdStr);
                Submission submission = submissionDAO.findById(submissionId);
                
                if (submission == null) {
                    response.sendError(404, "Submission not found");
                    return;
                }

                // Verify permission: Student owner, assignment Teacher, or Admin
                boolean isStudentOwner = submission.getStudent() != null && submission.getStudent().getId().equals(currentUser.getId());
                boolean isTeacherOwner = submission.getAssignment() != null && submission.getAssignment().getTeacher() != null && submission.getAssignment().getTeacher().getId().equals(currentUser.getId());
                boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

                if (!isStudentOwner && !isTeacherOwner && !isAdmin) {
                    response.sendError(403, "Access denied");
                    return;
                }

                String fileUrl = submission.getFileUrl();
                if (fileUrl == null || fileUrl.isEmpty()) {
                    response.sendError(404, "No file attached");
                    return;
                }

                // Get file path
                String applicationPath = getServletContext().getRealPath("");
                Path filePath = Paths.get(applicationPath, fileUrl);
                
                if (!Files.exists(filePath)) {
                    response.sendError(404, "File not found on server");
                    return;
                }

                // Set response headers for file download
                String fileName = filePath.getFileName().toString();
                String mimeType = getServletContext().getMimeType(filePath.toString());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                response.setContentLengthLong(Files.size(filePath));

                // Write file to response
                try (OutputStream out = response.getOutputStream()) {
                    Files.copy(filePath, out);
                }

            } catch (NumberFormatException e) {
                response.sendError(400, "Invalid submission ID");
            } catch (Exception e) {
                e.printStackTrace();
                response.sendError(500, "Error downloading file");
            }
        } else {
            response.sendError(400, "Invalid action");
        }
    }
}
