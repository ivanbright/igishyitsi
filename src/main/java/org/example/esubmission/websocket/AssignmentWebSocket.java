package org.example.esubmission.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time notifications of assignments and submissions.
 * <p>
 * This class manages active WebSocket sessions mapped by user ID and provides 
 * utility methods to broadcast messages to specific users or entire classes.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
@ServerEndpoint("/ws/assignments/{userId}")
public class AssignmentWebSocket {

    // Thread-safe map of active sessions: Key = userId, Value = Session
    private static final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    private static final StudentClassDAO studentClassDAO = new StudentClassDAO();
    private static final SchoolMembershipDAO schoolMembershipDAO = new SchoolMembershipDAO();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userIdStr) {
        try {
            Long userId = Long.parseLong(userIdStr);
            sessions.put(userId, session);
            System.out.println("WebSocket Opened: User " + userId + " connected. Session ID: " + session.getId());
        } catch (NumberFormatException e) {
            System.err.println("WebSocket Connection Error: Invalid userId '" + userIdStr + "'");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // We primarily use this websocket for server-to-client pushing, 
        // but we can log incoming messages if necessary.
        System.out.println("Received message from session " + session.getId() + ": " + message);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userIdStr) {
        try {
            Long userId = Long.parseLong(userIdStr);
            sessions.remove(userId);
            System.out.println("WebSocket Closed: User " + userId + " disconnected.");
        } catch (NumberFormatException e) {
            System.err.println("WebSocket Closure Error: Invalid userId");
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket Error on session " + session.getId() + ": " + throwable.getMessage());
    }

    // --- Static Broadcast Utilities ---

    /**
     * Broadcasts a text message to a specific user if they are connected.
     *
     * @param userId  the user ID to send the message to
     * @param message the JSON or string message
     */
    public static void broadcastToUser(Long userId, String message) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                System.err.println("Failed to send message to user " + userId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a text message to all students enrolled in a specific class
     * under a specific teacher.
     *
     * @param teacherId the ID of the teacher
     * @param className the name of the class
     * @param message   the JSON or string message
     */
    public static void broadcastToClass(Long teacherId, String className, String message) {
        List<User> students = studentClassDAO.findStudentsByTeacherAndClass(teacherId, className);
        for (User student : students) {
            broadcastToUser(student.getId(), message);
        }
    }

    /**
     * Broadcasts a text message to all connected users who belong to the
     * given school (tenant). Users outside the school never receive it.
     *
     * @param schoolId the ID of the school whose members receive the message
     * @param message  the JSON or string message
     */
    public static void broadcastToSchool(Long schoolId, String message) {
        if (schoolId == null) {
            return;
        }
        for (Long userId : sessions.keySet()) {
            if (schoolMembershipDAO.findByUserAndSchool(userId, schoolId) != null) {
                broadcastToUser(userId, message);
            }
        }
    }

    /**
     * Broadcasts a text message to all connected users.
     *
     * @param message the JSON or string message
     */
    public static void broadcastToAll(String message) {
        for (Long userId : sessions.keySet()) {
            broadcastToUser(userId, message);
        }
    }
}
