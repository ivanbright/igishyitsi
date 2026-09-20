package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.*;
import org.example.esubmission.model.Assignment;
import org.example.esubmission.model.Submission;
import org.example.esubmission.model.User;
import org.example.esubmission.util.AdminProjections;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet responsible for rendering the admin overview dashboard.
 * <p>
 * Collects platform statistics and recent activity and forwards the data to
 * {@code adminDashboard.jsp}. POST is redirected to GET (PRG pattern).
 * </p>
 */
public class AdminDash extends HttpServlet {

    private UserDAO userDAO;
    private StudentClassDAO studentClassDAO;
    private AssignmentDAO assignmentDAO;
    private SubjectDAO subjectDAO;
    private SubmissionDAO submissionDAO;
    private AnnouncementDAO announcementDAO;
    private ClassGroupDAO classGroupDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.studentClassDAO = new StudentClassDAO();
        this.assignmentDAO = new AssignmentDAO();
        this.subjectDAO = new SubjectDAO();
        this.submissionDAO = new SubmissionDAO();
        this.announcementDAO = new AnnouncementDAO();
        this.classGroupDAO = new ClassGroupDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SecurityUtil.requireAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }

        try {
            List<User> allUsers = userDAO.findAll();
            List<Assignment> assignments = assignmentDAO.findAll();
            List<Submission> allSubmissions = submissionDAO.findAll();

            long students = allUsers.stream().filter(u -> u.getRole() == UserRole.STUDENT).count();
            long teachers = allUsers.stream().filter(u -> u.getRole() == UserRole.TEACHER).count();
            long parents = allUsers.stream().filter(u -> u.getRole() == UserRole.PARENT).count();
            long unverified = allUsers.stream().filter(u -> !u.isEmailVerified()).count();
            long disabled = allUsers.stream().filter(User::isDisabled).count();

            Map<String, Long> stats = new HashMap<>();
            stats.put("students", students);
            stats.put("teachers", teachers);
            stats.put("parents", parents);
            stats.put("admins", allUsers.size() - students - teachers - parents);
            stats.put("classes", (long) classGroupDAO.findAll().size());
            stats.put("subjects", (long) subjectDAO.findAll().size());
            stats.put("assignments", (long) assignments.size());
            stats.put("submissions", (long) allSubmissions.size());
            stats.put("unverified", unverified);
            stats.put("disabled", disabled);

            List<Map<String, Object>> safeAnnouncements = announcementDAO.findAll().stream().limit(10).map(announcement -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", announcement.getId());
                map.put("title", announcement.getTitle());
                map.put("content", announcement.getContent());
                map.put("createdAt", announcement.getCreatedAt());
                map.put("audience", announcement.getAudience());
                map.put("priority", announcement.getPriority());
                if (announcement.getAuthor() != null) {
                    Map<String, Object> a = new HashMap<>();
                    a.put("id", announcement.getAuthor().getId());
                    a.put("name", announcement.getAuthor().getName());
                    map.put("author", a);
                }
                return map;
            }).collect(Collectors.toList());

            List<Map<String, Object>> activityLogs = new ArrayList<>();
            allSubmissions.stream().limit(8).forEach(sub -> {
                if (sub != null && sub.getStudent() != null && sub.getAssignment() != null) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("action", "Assignment Submission");
                    log.put("user", sub.getStudent().getName());
                    log.put("time", sub.getSubmittedAt() != null ? sub.getSubmittedAt().toString() : "");
                    log.put("details", "Submitted " + sub.getAssignment().getTitle());
                    activityLogs.add(log);
                }
            });
            assignments.stream().limit(4).forEach(asn -> {
                if (asn != null && asn.getTeacher() != null) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("action", "New Assignment Created");
                    log.put("user", asn.getTeacher().getName());
                    log.put("time", asn.getCreatedAt() != null ? asn.getCreatedAt().toString() : "");
                    log.put("details", "Created " + asn.getTitle());
                    activityLogs.add(log);
                }
            });

            List<Map<String, Object>> recentUsers = allUsers.stream()
                    .sorted(Comparator.comparing(User::getId).reversed())
                    .limit(8)
                    .map(AdminProjections::userBasic)
                    .collect(Collectors.toList());

            List<Map<String, Object>> allUsersForAdmin = allUsers.stream()
                    .map(AdminProjections::userBasic)
                    .collect(Collectors.toList());

            Map<String, Object> realData = new HashMap<>();
            realData.put("stats", stats);
            realData.put("users", allUsersForAdmin);
            realData.put("students", allUsersForAdmin.stream()
                    .filter(u -> UserRole.STUDENT == UserRole.valueOf(String.valueOf(u.get("role")))).collect(Collectors.toList()));
            realData.put("teachers", allUsersForAdmin.stream()
                    .filter(u -> UserRole.TEACHER == UserRole.valueOf(String.valueOf(u.get("role")))).collect(Collectors.toList()));
            realData.put("parents", allUsersForAdmin.stream()
                    .filter(u -> UserRole.PARENT == UserRole.valueOf(String.valueOf(u.get("role")))).collect(Collectors.toList()));
            realData.put("classes", classGroupDAO.findAll().stream().map(AdminProjections::classBasic).collect(Collectors.toList()));
            realData.put("subjects", subjectDAO.findAll().stream().map(AdminProjections::subjectBasic).collect(Collectors.toList()));
            realData.put("announcements", safeAnnouncements);
            realData.put("logs", activityLogs);
            realData.put("recentUsers", recentUsers);
            realData.put("currentUser", AdminProjections.userBasic(SecurityUtil.requireAdmin(request)));
            realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

            request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
            request.getRequestDispatcher("/WEB-INF/pages/schoolAdmin.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading admin dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
    }
}