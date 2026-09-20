package org.example.esubmission.util;

import jakarta.servlet.http.HttpSession;
import org.example.esubmission.dao.ClassGroupDAO;
import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.model.Subject;
import org.example.esubmission.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Password-free, verification-safe Map projections of core entities for the
 * admin module JSPs.
 */
public final class AdminProjections {

    private AdminProjections() {}

    public static Map<String, Object> userBasic(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("phone", u.getPhone());
        map.put("role", String.valueOf(u.getRole()));
        map.put("emailVerified", u.isEmailVerified());
        map.put("disabled", u.isDisabled());
        return map;
    }

    public static Map<String, Object> classBasic(ClassGroup c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("description", c.getDescription());
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        return map;
    }

    public static Map<String, Object> subjectBasic(Subject s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("code", s.getCode());
        map.put("name", s.getName());
        map.put("department", s.getDepartment());
        map.put("credits", s.getCredits());
        if (s.getTeacher() != null) {
            map.put("teacherId", s.getTeacher().getId());
            map.put("teacherName", s.getTeacher().getName());
        }
        return map;
    }

    public static long loadCatalogSize(HttpSession session) {
        if (session == null) {
            return new ClassGroupDAO().findAll().size();
        }
        Object cached = session.getAttribute("catalogSize");
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        return new ClassGroupDAO().findAll().size();
    }
}
