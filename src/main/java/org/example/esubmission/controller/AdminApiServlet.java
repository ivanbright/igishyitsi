package org.example.esubmission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.AnnouncementDAO;
import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.dao.ParentChildDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.SubjectDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.dao.TeacherClassDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.Assignment;
import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.model.ParentChild;
import org.example.esubmission.model.StudentClass;
import org.example.esubmission.model.Subject;
import org.example.esubmission.model.TeacherClass;
import org.example.esubmission.model.User;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative admin API for the administration modules.
 * <p>
 * Every handler enforces a live {@code ADMIN} session AND a valid CSRF token
 * ({@code X-CSRF-Token} header) and re-validates referenced entities and roles
 * server-side before mutating state. No endpoint trusts client-supplied IDs or
 * relationships.
 * </p>
 */
public class AdminApiServlet extends HttpServlet {

    private UserDAO userDAO;
    private SubjectDAO subjectDAO;
    private AssignmentDAO assignmentDAO;
    private AnnouncementDAO announcementDAO;
    private ClassGroupDAO classGroupDAO;
    private TeacherClassDAO teacherClassDAO;
    private ParentChildDAO parentChildDAO;
    private StudentClassDAO studentClassDAO;
    private SubmissionDAO submissionDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.subjectDAO = new SubjectDAO();
        this.assignmentDAO = new AssignmentDAO();
        this.announcementDAO = new AnnouncementDAO();
        this.classGroupDAO = new ClassGroupDAO();
        this.teacherClassDAO = new TeacherClassDAO();
        this.parentChildDAO = new ParentChildDAO();
        this.studentClassDAO = new StudentClassDAO();
        this.submissionDAO = new SubmissionDAO();
        this.objectMapper = JsonUtil.MAPPER;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (SecurityUtil.requireAdmin(request) == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }
        if (!SecurityUtil.csrfValid(request)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String pathInfo = request.getPathInfo();
        String action = request.getParameter("action");

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path or action");
                return;
            }

            switch (pathInfo) {
                case "/users":
                    if ("updateRole".equals(action)) {
                        handleUpdateRole(request, response);
                    } else if ("status".equals(action)) {
                        handleUserStatus(request, response);
                    } else {
                        sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
                    }
                    break;
                case "/teacherClass":
                    handleTeacherClass(request, response);
                    break;
                case "/subjectTeacher":
                    handleSubjectTeacher(request, response);
                    break;
                case "/classes":
                    handleClasses(request, response);
                    break;
                case "/enroll":
                    handleEnroll(request, response);
                    break;
                case "/withdraw":
                    handleWithdraw(request, response);
                    break;
                case "/parentChild":
                    handleParentChild(request, response);
                    break;
                default:
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric value");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (SecurityUtil.requireAdmin(request) == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }
        if (!SecurityUtil.csrfValid(request)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String pathInfo = request.getPathInfo();
        try {
            if (pathInfo == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
                return;
            }
            String idStr = request.getParameter("id");
            if (idStr == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "ID missing");
                return;
            }
            Long id = Long.parseLong(idStr);

            switch (pathInfo) {
                case "/users":
                    handleDeleteUser(request, response, id);
                    break;
                case "/subjects":
                    subjectDAO.delete(id);
                    sendSuccess(response, "Subject deleted");
                    break;
                case "/announcements":
                    announcementDAO.delete(id);
                    sendSuccess(response, "Announcement deleted");
                    break;
                case "/classes":
                    handleDeleteClass(response, id);
                    break;
                default:
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void handleUpdateRole(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userIdStr = request.getParameter("userId");
        String roleStr = request.getParameter("role");

        if (userIdStr == null || roleStr == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing userId or role");
            return;
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            UserRole role = UserRole.valueOf(roleStr);
            User user = userDAO.findById(userId);

            if (user == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }
            if (user.getId().equals(SecurityUtil.requireAdmin(request).getId()) && role != UserRole.ADMIN) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "You cannot demote your own account");
                return;
            }

            user.setRole(role);
            userDAO.save(user);
            sendSuccess(response, "Role updated to " + role);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid role value");
        }
    }

    private void handleUserStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userIdStr = request.getParameter("userId");
        if (userIdStr == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing userId");
            return;
        }
        Long userId = Long.parseLong(userIdStr);
        User user = userDAO.findById(userId);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        User admin = SecurityUtil.requireAdmin(request);
        if (request.getParameter("disabled") != null) {
            boolean disabled = Boolean.parseBoolean(request.getParameter("disabled"));
            if (disabled && user.getId().equals(admin.getId())) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "You cannot disable your own account");
                return;
            }
            user.setDisabled(disabled);
        }
        if (request.getParameter("emailVerified") != null) {
            user.setEmailVerified(Boolean.parseBoolean(request.getParameter("emailVerified")));
        }

        userDAO.save(user);
        sendSuccess(response, "Account status updated");
    }

    private void handleTeacherClass(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String teacherIdStr = request.getParameter("teacherId");
        String className = request.getParameter("className");
        String action = request.getParameter("assignAction");

        if (teacherIdStr == null || className == null || className.trim().isEmpty() || action == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing teacherId, className or assignAction");
            return;
        }
        Long teacherId = Long.parseLong(teacherIdStr);
        String name = className.trim();

        User teacher = userDAO.findById(teacherId);
        if (teacher == null || teacher.getRole() != UserRole.TEACHER) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User is not a teacher");
            return;
        }

        if ("remove".equals(action)) {
            teacherClassDAO.remove(teacherId, name);
            sendSuccess(response, "Removed teacher from " + name);
            return;
        }
        if (!"assign".equals(action)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid assignAction");
            return;
        }
        if (!classResolvable(name)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Class '" + name + "' is not known. Create it in the Classes module first.");
            return;
        }
        if (teacherClassDAO.exists(teacherId, name)) {
            sendSuccess(response, "Teacher is already assigned to " + name);
            return;
        }
        TeacherClass assignment = new TeacherClass();
        assignment.setTeacher(teacher);
        assignment.setClassName(name);
        teacherClassDAO.save(assignment);
        sendSuccess(response, "Assigned teacher to " + name);
    }

    private void handleSubjectTeacher(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String subjectIdStr = request.getParameter("subjectId");
        String teacherIdStr = request.getParameter("teacherId");

        if (subjectIdStr == null || teacherIdStr == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing subjectId or teacherId");
            return;
        }
        Subject subject = subjectDAO.findById(Long.parseLong(subjectIdStr));
        if (subject == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Subject not found");
            return;
        }

        if (teacherIdStr.trim().isEmpty()) {
            subject.setTeacher(null);
            subjectDAO.save(subject);
            sendSuccess(response, "Teacher unassigned from " + subject.getCode());
            return;
        }

        User teacher = userDAO.findById(Long.parseLong(teacherIdStr));
        if (teacher == null || teacher.getRole() != UserRole.TEACHER) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User is not a teacher");
            return;
        }
        subject.setTeacher(teacher);
        subjectDAO.save(subject);
        sendSuccess(response, "Teacher assigned to " + subject.getCode());
    }

    private void handleClasses(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        if (action == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing action");
            return;
        }

        switch (action) {
            case "create": {
                String name = request.getParameter("name");
                if (name == null || name.trim().isEmpty()) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Class name is required");
                    return;
                }
                String trimmed = name.trim();
                if (classGroupDAO.findByName(trimmed) != null) {
                    sendError(response, HttpServletResponse.SC_CONFLICT, "A class with this name already exists");
                    return;
                }
                ClassGroup group = new ClassGroup(trimmed, request.getParameter("description"));
                classGroupDAO.save(group);
                sendSuccess(response, "Class '" + trimmed + "' created");
                break;
            }
            case "rename": {
                String idStr = request.getParameter("id");
                ClassGroup group = idStr == null ? null : classGroupDAO.findById(Long.parseLong(idStr));
                if (group == null) {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Class not found");
                    return;
                }
                if (request.getParameter("description") != null) {
                    group.setDescription(request.getParameter("description"));
                }
                String newName = request.getParameter("name");
                if (newName != null && !newName.trim().isEmpty() && !group.getName().equalsIgnoreCase(newName.trim())) {
                    String trimmed = newName.trim();
                    if (classGroupDAO.findByName(trimmed) != null) {
                        sendError(response, HttpServletResponse.SC_CONFLICT, "A class with this name already exists");
                        return;
                    }
                    String refs = classReferences(group.getName());
                    if (refs != null) {
                        sendError(response, HttpServletResponse.SC_CONFLICT,
                                "Cannot rename: class is in use (" + refs + "). Withdraw enrollments, remove teacher assignments and delete assignments first.");
                        return;
                    }
                    group.setName(trimmed);
                }
                classGroupDAO.save(group);
                sendSuccess(response, "Class updated");
                break;
            }
            case "archive":
            case "activate": {
                String idStr = request.getParameter("id");
                ClassGroup group = idStr == null ? null : classGroupDAO.findById(Long.parseLong(idStr));
                if (group == null) {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Class not found");
                    return;
                }
                group.setActive("archive".equals(action));
                classGroupDAO.save(group);
                sendSuccess(response, "Class " + ("archive".equals(action) ? "archived" : "reactivated"));
                break;
            }
            default:
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown class action");
        }
    }

    private void handleDeleteClass(HttpServletResponse response, Long id) throws IOException {
        ClassGroup group = classGroupDAO.findById(id);
        if (group == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Class not found");
            return;
        }
        String refs = classReferences(group.getName());
        if (refs != null) {
            sendError(response, HttpServletResponse.SC_CONFLICT, "Cannot delete: class is in use (" + refs + ").");
            return;
        }
        classGroupDAO.delete(id);
        sendSuccess(response, "Class deleted");
    }

    private void handleEnroll(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String studentIdStr = request.getParameter("studentId");
        String teacherIdStr = request.getParameter("teacherId");
        String className = request.getParameter("className");

        if (studentIdStr == null || teacherIdStr == null || className == null || className.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing studentId, teacherId or className");
            return;
        }
        Long studentId = Long.parseLong(studentIdStr);
        Long teacherId = Long.parseLong(teacherIdStr);
        String name = className.trim();

        if (!classResolvable(name)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Class '" + name + "' is not known. Create it in the Classes module first.");
            return;
        }
        User student = userDAO.findById(studentId);
        if (student == null || student.getRole() != UserRole.STUDENT) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User is not a student");
            return;
        }
        User teacher = userDAO.findById(teacherId);
        if (teacher == null || teacher.getRole() != UserRole.TEACHER) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Teacher is not valid");
            return;
        }

        studentClassDAO.removeByStudentAndClass(studentId, name);
        StudentClass enrollment = new StudentClass();
        enrollment.setStudent(student);
        enrollment.setClassName(name);
        enrollment.setTeacher(teacher);
        studentClassDAO.save(enrollment);
        sendSuccess(response, "Enrolled " + student.getName() + " in " + name);
    }

    private void handleWithdraw(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String studentIdStr = request.getParameter("studentId");
        String className = request.getParameter("className");
        if (studentIdStr == null || className == null || className.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing studentId or className");
            return;
        }
        Long studentId = Long.parseLong(studentIdStr);
        User student = userDAO.findById(studentId);
        if (student == null || student.getRole() != UserRole.STUDENT) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User is not a student");
            return;
        }
        studentClassDAO.removeByStudentAndClass(studentId, className.trim());
        sendSuccess(response, "Withdrew " + student.getName() + " from " + className.trim());
    }

    private void handleParentChild(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String parentIdStr = request.getParameter("parentId");
        String studentIdStr = request.getParameter("studentId");
        String action = request.getParameter("linkAction");

        if (parentIdStr == null || studentIdStr == null || action == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing parentId, studentId or linkAction");
            return;
        }
        Long parentId = Long.parseLong(parentIdStr);
        Long studentId = Long.parseLong(studentIdStr);

        User parent = userDAO.findById(parentId);
        if (parent == null || parent.getRole() != UserRole.PARENT) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User is not a parent");
            return;
        }
        User student = userDAO.findById(studentId);
        if (student == null || student.getRole() != UserRole.STUDENT) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User is not a student");
            return;
        }

        if ("remove".equals(action)) {
            parentChildDAO.remove(parentId, studentId);
            sendSuccess(response, "Removed " + student.getName() + " from " + parent.getName());
            return;
        }
        if (!"add".equals(action)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid linkAction");
            return;
        }
        if (parentChildDAO.exists(parentId, studentId)) {
            sendSuccess(response, "Parent is already linked to " + student.getName());
            return;
        }
        ParentChild link = new ParentChild();
        link.setParent(parent);
        link.setStudent(student);
        parentChildDAO.save(link);
        sendSuccess(response, "Linked " + parent.getName() + " to " + student.getName());
    }

    private void handleDeleteUser(HttpServletRequest request, HttpServletResponse response, Long id) throws IOException {
        User user = userDAO.findById(id);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }
        User admin = SecurityUtil.requireAdmin(request);
        if (user.getId().equals(admin.getId())) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "You cannot delete your own account");
            return;
        }
        if (user.getRole() == UserRole.TEACHER && assignmentDAO.countByTeacherId(id) > 0) {
            sendError(response, HttpServletResponse.SC_CONFLICT, "Cannot delete: teacher has assignments");
            return;
        }
        userDAO.delete(id);
        sendSuccess(response, "User deleted");
    }

    /**
     * A class name is valid when it exists in the classes catalog OR still has
     * legacy enrollment rows. This keeps pre-catalog data usable while preventing
     * arbitrary free-text class names.
     */
    private boolean classResolvable(String name) {
        if (classGroupDAO.findByName(name) != null) {
            return true;
        }
        return !studentClassDAO.findEnrollmentsByClassName(name).isEmpty();
    }

    /**
     * Returns a human-readable summary of references to a class name, or
     * {@code null} when the class is unreferenced.
     */
    private String classReferences(String name) {
        StringBuilder refs = new StringBuilder();
        Long enrollments = studentClassDAO.countStudentsByClassName(name);
        if (enrollments != null && enrollments > 0) {
            refs.append(enrollments).append(" enrolled student(s), ");
        }
        long teacherCount = teacherClassDAO.countByClassName(name);
        if (teacherCount > 0) {
            refs.append(teacherCount).append(" teacher assignment(s), ");
        }
        List<Assignment> assignments = assignmentDAO.findByClassName(name);
        if (!assignments.isEmpty()) {
            refs.append(assignments.size()).append(" assignment(s), ");
        }
        if (refs.length() == 0) {
            return null;
        }
        return refs.substring(0, refs.length() - 2);
    }

    private void sendSuccess(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", message);
        objectMapper.writeValue(response.getWriter(), res);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("message", message);
        objectMapper.writeValue(response.getWriter(), res);
    }
}