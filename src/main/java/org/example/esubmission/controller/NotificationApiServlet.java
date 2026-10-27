package org.example.esubmission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.model.Notification;
import org.example.esubmission.model.User;
import org.example.esubmission.service.NotificationService;
import org.example.esubmission.util.SecurityUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API endpoint for managing user notifications.
 * <p>
 * GET  /api/notifications         - List notifications for current user
 * POST /api/notifications/read    - Mark all as read
 * POST /api/notifications/read/ID - Mark specific notification as read
 * </p>
 */
public class NotificationApiServlet extends HttpServlet {

    private NotificationService notificationService;
    private ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        this.notificationService = new NotificationService();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            List<Notification> notifications = notificationService.getNotifications(user.getId());
            Long unreadCount = notificationService.getUnreadCount(user.getId());

            // Safe projection (exclude user password hash etc.)
            List<Map<String, Object>> safeNotifications = notifications.stream().map(n -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", n.getId());
                m.put("title", n.getTitle());
                m.put("message", n.getMessage());
                m.put("type", n.getType());
                m.put("read", n.isRead());
                m.put("link", n.getLink());
                m.put("createdAt", n.getFormattedCreatedAt());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("notifications", safeNotifications);
            result.put("unreadCount", unreadCount);

            response.setContentType("application/json");
            response.getWriter().write(mapper.writeValueAsString(result));

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading notifications");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.equals("/read") && !pathInfo.startsWith("/read/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!SecurityUtil.csrfValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        try {
            if (pathInfo.startsWith("/read")) {
                String idPart = pathInfo.substring("/read".length());
                if (idPart.isEmpty() || idPart.equals("/")) {
                    // Mark all read
                    notificationService.markAllRead(user.getId());
                } else {
                    // Mark specific notification read (owner-scoped)
                    Long notifId = Long.parseLong(idPart.replace("/", ""));
                    notificationService.markRead(user.getId(), notifId);
                }
            }

            response.setContentType("application/json");
            response.getWriter().write("{\"success\": true}");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error updating notifications");
        }
    }
}
