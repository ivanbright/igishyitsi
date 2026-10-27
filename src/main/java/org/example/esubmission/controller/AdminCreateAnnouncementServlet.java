package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.AnnouncementDAO;
import org.example.esubmission.dao.NotificationDAO;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.model.Announcement;
import org.example.esubmission.service.NotificationService;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.User;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.TenantContext;
import org.example.esubmission.util.UserRole;
import org.example.esubmission.websocket.AssignmentWebSocket;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@WebServlet("/api/admin/createAnnouncement")
public class AdminCreateAnnouncementServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(AdminCreateAnnouncementServlet.class.getName());
    private AnnouncementDAO announcementDAO;
    private NotificationDAO notificationDAO;
    private SchoolMembershipDAO schoolMembershipDAO;

    @Override
    public void init() throws ServletException {
        this.announcementDAO = new AnnouncementDAO();
        this.notificationDAO = new NotificationDAO();
        this.schoolMembershipDAO = new SchoolMembershipDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            User admin = SecurityUtil.requireAdmin(request);
            if (admin == null || !SecurityUtil.csrfValid(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\":false,\"message\":\"Unauthorized\"}");
                return;
            }

            Long schoolId = TenantContext.requireSchoolId();

            String title = request.getParameter("title");
            String content = request.getParameter("content");
            String audienceStr = request.getParameter("audience"); // "ALL", "STUDENTS", "TEACHERS"

            if (title == null || title.trim().isEmpty() ||
                content == null || content.trim().isEmpty() ||
                audienceStr == null || audienceStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Missing required fields\"}");
                return;
            }

            Announcement announcement = new Announcement();
            announcement.setTitle(title.trim());
            announcement.setContent(content.trim());
            announcement.setAuthor(admin);
            announcement.setAudience(audienceStr.trim());
            announcement.setCreatedAt(LocalDateTime.now());

            announcementDAO.save(announcement);

            // Create Notifications for the target audience within the active school
            String link = audienceStr.equals("TEACHERS") ? "/teacher/dashboard"
                    : audienceStr.equals("STUDENTS") ? "/student/dashboard" : null;
            List<SchoolMembership> members = schoolMembershipDAO.findBySchoolId(schoolId);
            int notified = 0;
            for (SchoolMembership member : members) {
                UserRole membershipRole = member.getRole();
                if (audienceStr.equals("ALL") ||
                    (audienceStr.equals("STUDENTS") && membershipRole == UserRole.STUDENT) ||
                    (audienceStr.equals("TEACHERS") && membershipRole == UserRole.TEACHER)) {
                    try {
                        new NotificationService(notificationDAO).notifyAnnouncement(member.getUser(), title.trim());
                        notified++;
                    } catch (Exception e) {
                        logger.warning("Could not notify " + member.getUser().getEmail() + ": " + e.getMessage());
                    }
                }
            }

            // Notify connected websocket clients within this school
            try {
                // Simplified payload for UI
                String payload = String.format("{\"type\":\"NEW_ANNOUNCEMENT\",\"title\":\"%s\"}", title.trim().replace("\"", "\\\""));
                AssignmentWebSocket.broadcastToSchool(schoolId, payload);
            } catch (Exception e) {
                logger.warning("Could not broadcast via WebSocket: " + e.getMessage());
            }

            out.print("{\"success\":true,\"message\":\"Announcement published to " + notified + " recipient(s)\"}");
        } catch (Exception e) {
            logger.severe("Error creating announcement: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"An internal error occurred\"}");
        }
    }
}