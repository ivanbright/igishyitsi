package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.SubjectDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.dao.TeacherClassDAO;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Teacher management: class assignments (teacher_classes), subject ownership,
 * status and workload counts.
 */
public class AdminTeachersServlet extends HttpServlet {

    private UserDAO userDAO;
    private TeacherClassDAO teacherClassDAO;
    private StudentClassDAO studentClassDAO;
    private SubjectDAO subjectDAO;
    private SubmissionDAO submissionDAO;
    private ClassGroupDAO classGroupDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.teacherClassDAO = new TeacherClassDAO();
        this.studentClassDAO = new StudentClassDAO();
        this.subjectDAO = new SubjectDAO();
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

        List<User> teachers = userDAO.findAll().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        List<Map<String, Object>> list = new ArrayList<>();
        for (User teacher : teachers) {
            Map<String, Object> map = AdminProjections.userBasic(teacher);
            Set<String> classes = new LinkedHashSet<>();
            classes.addAll(teacherClassDAO.findByTeacherId(teacher.getId()));
            classes.addAll(studentClassDAO.findClassesByTeacher(teacher.getId()));
            map.put("classes", new ArrayList<>(classes));

            List<Map<String, Object>> subjects = subjectDAO.findAll().stream()
                    .filter(s -> s.getTeacher() != null && s.getTeacher().getId().equals(teacher.getId()))
                    .map(s -> {
                        Map<String, Object> sm = new HashMap<>();
                        sm.put("id", s.getId());
                        sm.put("code", s.getCode());
                        sm.put("name", s.getName());
                        return sm;
                    })
                    .toList();
            map.put("subjects", subjects);
            map.put("assignmentCount", new AssignmentDAO().countByTeacherId(teacher.getId()));
            map.put("submissionCount", submissionDAO.countByTeacherId(teacher.getId()));
            list.add(map);
        }

        List<Map<String, Object>> classOptions = classGroupDAO.findActive().stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", c.getName());
                    return m;
                })
                .toList();

        Map<String, Object> realData = new HashMap<>();
        realData.put("teachers", list);
        realData.put("classOptions", classOptions);
        realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

        request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
        request.getRequestDispatcher("/WEB-INF/pages/adminTeachers.jsp").forward(request, response);
    }
}