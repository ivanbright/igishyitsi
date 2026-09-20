package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.dao.ParentChildDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.dao.TeacherClassDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
import org.example.esubmission.util.AdminProjections;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists every account with role-specific summary panels (drawer) and the
 * account controls (enabled/disabled, verified, role, delete).
 */
public class AdminUsersServlet extends HttpServlet {

    private UserDAO userDAO;
    private TeacherClassDAO teacherClassDAO;
    private ParentChildDAO parentChildDAO;
    private SubmissionDAO submissionDAO;
    private ClassGroupDAO classGroupDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.teacherClassDAO = new TeacherClassDAO();
        this.parentChildDAO = new ParentChildDAO();
        this.submissionDAO = new SubmissionDAO();
        this.classGroupDAO = new ClassGroupDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SecurityUtil.requireAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }

        List<User> all = new ArrayList<>(userDAO.findAll());
        all.sort(Comparator.comparing(User::getId));

        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : all) {
            Map<String, Object> map = AdminProjections.userBasic(user);
            if (user.getRole() == UserRole.TEACHER) {
                Set<String> classes = new LinkedHashSet<>();
                classes.addAll(teacherClassDAO.findByTeacherId(user.getId()));
                map.put("classes", new ArrayList<>(classes));
            } else if (user.getRole() == UserRole.STUDENT) {
                map.put("parentCount", parentChildDAO.findParentsByStudent(user.getId()).size());
                map.put("submittedCount", submissionDAO.findByStudentId(user.getId()).size());
            } else if (user.getRole() == UserRole.PARENT) {
                map.put("childCount", parentChildDAO.findStudentsByParent(user.getId()).size());
            }
            users.add(map);
        }

Map<String, Object> realData = new HashMap<>();
        realData.put("users", users);
        realData.put("classCount", AdminProjections.loadCatalogSize(request.getSession()));
        realData.put("currentUser", AdminProjections.userBasic(SecurityUtil.requireAdmin(request)));
        realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

        request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
        request.getRequestDispatcher("/WEB-INF/pages/adminUsers.jsp").forward(request, response);
    }
}