package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.SubjectDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.User;
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
 * Subject management: subject catalog CRUD and teacher ownership.
 */
public class AdminSubjectsServlet extends HttpServlet {

    private SubjectDAO subjectDAO;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.subjectDAO = new SubjectDAO();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SecurityUtil.requireAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }

        List<Map<String, Object>> subjects = subjectDAO.findAll().stream()
                .map(sub -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", sub.getId());
                    map.put("code", sub.getCode());
                    map.put("name", sub.getName());
                    map.put("department", sub.getDepartment());
                    map.put("credits", sub.getCredits());
                    if (sub.getTeacher() != null) {
                        Map<String, Object> teacher = new HashMap<>();
                        teacher.put("id", sub.getTeacher().getId());
                        teacher.put("name", sub.getTeacher().getName());
                        map.put("teacher", teacher);
                    }
                    return map;
                })
                .toList();

        List<Map<String, Object>> teachers = userDAO.findAll().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .sorted(Comparator.comparing(User::getName))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getName());
                    return m;
                })
                .toList();

        Map<String, Object> realData = new HashMap<>();
        realData.put("subjects", subjects);
        realData.put("teachers", teachers);
        realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

        request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
        request.getRequestDispatcher("/WEB-INF/pages/adminSubjects.jsp").forward(request, response);
    }
}