package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.TeacherClassDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.model.User;
import org.example.esubmission.util.JsonUtil;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class management: class catalog (classes), enrollments, assigned teachers
 * and linked assignments.
 */
public class AdminClassesServlet extends HttpServlet {

    private ClassGroupDAO classGroupDAO;
    private StudentClassDAO studentClassDAO;
    private TeacherClassDAO teacherClassDAO;
    private AssignmentDAO assignmentDAO;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.classGroupDAO = new ClassGroupDAO();
        this.studentClassDAO = new StudentClassDAO();
        this.teacherClassDAO = new TeacherClassDAO();
        this.assignmentDAO = new AssignmentDAO();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SecurityUtil.requireAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ClassGroup group : classGroupDAO.findAll()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", group.getId());
            map.put("name", group.getName());
            map.put("description", group.getDescription());
            map.put("active", group.isActive());

            Long studentCount = studentClassDAO.countStudentsByClassName(group.getName());
            map.put("studentCount", studentCount == null ? 0L : studentCount);
            map.put("assignmentCount", (long) assignmentDAO.findByClassName(group.getName()).size());

            Set<User> teacherSet = new LinkedHashSet<>();
            List<User> viaAssignments = teacherClassDAO.findTeachersByClass(group.getName());
            List<User> viaEnrollments = studentClassDAO.findTeachersByClass(group.getName());
            if (viaAssignments != null) teacherSet.addAll(viaAssignments);
            if (viaEnrollments != null) teacherSet.addAll(viaEnrollments);
            List<Map<String, Object>> teachers = teacherSet.stream()
                    .map(t -> {
                        Map<String, Object> tm = new HashMap<>();
                        tm.put("id", t.getId());
                        tm.put("name", t.getName());
                        return tm;
                    })
                    .toList();
            map.put("teachers", teachers);
            list.add(map);
        }

        List<Map<String, Object>> teachers = userDAO.findAll().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getName());
                    return m;
                })
                .toList();

        Map<String, Object> realData = new HashMap<>();
        realData.put("classes", list);
        realData.put("teachers", teachers);
        realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

        request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
        request.getRequestDispatcher("/WEB-INF/pages/adminClasses.jsp").forward(request, response);
    }
}