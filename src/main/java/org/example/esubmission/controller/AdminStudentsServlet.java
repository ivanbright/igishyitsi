package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.dao.ParentChildDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.SubmissionDAO;
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
 * Student management: enrollment (class + responsible teacher), parent links,
 * academic summary and status.
 */
public class AdminStudentsServlet extends HttpServlet {

    private UserDAO userDAO;
    private StudentClassDAO studentClassDAO;
    private ParentChildDAO parentChildDAO;
    private SubmissionDAO submissionDAO;
    private ClassGroupDAO classGroupDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.studentClassDAO = new StudentClassDAO();
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

        List<User> students = userDAO.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        List<Map<String, Object>> list = new ArrayList<>();
        for (User student : students) {
            Map<String, Object> map = AdminProjections.userBasic(student);

            List<Map<String, Object>> enrollments = studentClassDAO.findStudentClassesByStudent(student.getId()).stream()
                    .map(sc -> {
                        Map<String, Object> em = new HashMap<>();
                        em.put("className", sc.getClassName());
                        em.put("teacherId", sc.getTeacher().getId());
                        em.put("teacherName", sc.getTeacher().getName());
                        return em;
                    })
                    .toList();
            map.put("enrollments", enrollments);

            List<Map<String, Object>> parents = parentChildDAO.findParentsByStudent(student.getId()).stream()
                    .map(p -> {
                        Map<String, Object> pm = new HashMap<>();
                        pm.put("id", p.getId());
                        pm.put("name", p.getName());
                        pm.put("email", p.getEmail());
                        return pm;
                    })
                    .toList();
            map.put("parents", parents);

            List<?> submissions = submissionDAO.findByStudentId(student.getId());
            map.put("submittedCount", submissions.size());
            double avg = submissions.stream()
                    .filter(s -> ((org.example.esubmission.model.Submission) s).getGrade() != null)
                    .mapToInt(s -> ((org.example.esubmission.model.Submission) s).getGrade())
                    .average().orElse(0.0);
            map.put("averageGrade", avg == 0.0 ? null : Math.round(avg * 10) / 10.0);
            list.add(map);
        }

        Set<String> classNames = new LinkedHashSet<>();
        classGroupDAO.findAll().forEach(c -> classNames.add(c.getName()));
        List<String> legacy = studentClassDAO.findAllClasses();
        if (legacy != null) classNames.addAll(legacy);

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
        realData.put("students", list);
        realData.put("classes", new ArrayList<>(classNames));
        realData.put("teachers", teachers);
        realData.put("csrfToken", SecurityUtil.getCsrfToken(request));

        request.setAttribute("realData", JsonUtil.MAPPER.writeValueAsString(realData));
        request.getRequestDispatcher("/WEB-INF/pages/adminStudents.jsp").forward(request, response);
    }
}