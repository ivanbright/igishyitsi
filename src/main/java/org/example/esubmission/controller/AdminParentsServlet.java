package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.ParentChildDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.UserDAO;
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

/**
 * Parent management: link parents to students (parent_children) and view
 * each child's current class.
 */
public class AdminParentsServlet extends HttpServlet {

    private UserDAO userDAO;
    private ParentChildDAO parentChildDAO;
    private StudentClassDAO studentClassDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.parentChildDAO = new ParentChildDAO();
        this.studentClassDAO = new StudentClassDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SecurityUtil.requireAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }

        List<User> parents = userDAO.findAll().stream()
                .filter(u -> u.getRole() == UserRole.PARENT)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        List<Map<String, Object>> list = new ArrayList<>();
        for (User parent : parents) {
            Map<String, Object> map = AdminProjections.userBasic(parent);

            List<Map<String, Object>> children = parentChildDAO.findStudentsByParent(parent.getId()).stream()
                    .map(child -> {
                        Map<String, Object> cm = new HashMap<>();
                        cm.put("id", child.getId());
                        cm.put("name", child.getName());
                        cm.put("email", child.getEmail());
                        List<?> enrollments = studentClassDAO.findStudentClassesByStudent(child.getId());
                        cm.put("className", enrollments.isEmpty()
                                ? null
                                : ((org.example.esubmission.model.StudentClass) enrollments.get(0)).getClassName());
                        return cm;
                    })
                    .toList();
            map.put("children", children);
            list.add(map);
        }

        List<Map<String, Object>> students = userDAO.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getName());
                    m.put("email", u.getEmail());
                    List<?> enrollments = studentClassDAO.findStudentClassesByStudent(u.getId());
                    m.put("className", enrollments.isEmpty()
                            ? null
                            : ((org.example.esubmission.model.StudentClass) enrollments.get(0)).getClassName());
                    return m;
                })
                .toList();

        Map<String, Object> realData = new HashMap<>();
        realData.put("parents", list);
        realData.put("students", students);
        realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

        request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
        request.getRequestDispatcher("/WEB-INF/pages/adminParents.jsp").forward(request, response);
    }
}