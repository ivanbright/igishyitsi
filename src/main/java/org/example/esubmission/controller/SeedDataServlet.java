package org.example.esubmission.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.esubmission.dao.AssignmentDAO;
import org.example.esubmission.dao.SchoolDAO;
import org.example.esubmission.dao.SchoolMembershipDAO;
import org.example.esubmission.dao.StudentClassDAO;
import org.example.esubmission.dao.SubmissionDAO;
import org.example.esubmission.dao.UserDAO;
import org.example.esubmission.model.Assignment;
import org.example.esubmission.model.School;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.StudentClass;
import org.example.esubmission.model.User;
import org.example.esubmission.service.AssignmentService;
import org.example.esubmission.util.PasswordUtils;
import org.example.esubmission.util.SecurityUtil;
import org.example.esubmission.util.TenantContext;
import org.example.esubmission.util.UserRole;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Servlet that seeds initial demo data into the database.
 * <p>
 * Access at: GET /admin/seed
 * Creates a demo school, an administrator, a teacher, two students, enrolls
 * them in "Software Engineering", creates three assignments, and records the
 * school memberships for every account.
 * </p>
 * <p>
 * This is idempotent: if the teacher email already exists, seeding is skipped.
 * </p>
 *
 * @author Mugisha Ivan Bright
 */
public class SeedDataServlet extends HttpServlet {

    private UserDAO userDAO;
    private StudentClassDAO studentClassDAO;
    private AssignmentService assignmentService;
    private SchoolDAO schoolDAO;
    private SchoolMembershipDAO schoolMembershipDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
        this.studentClassDAO = new StudentClassDAO();
        this.assignmentService = new AssignmentService(new AssignmentDAO(), new SubmissionDAO(), new StudentClassDAO());
        this.schoolDAO = new SchoolDAO();
        this.schoolMembershipDAO = new SchoolMembershipDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SecurityUtil.requireAdmin(req) == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<html><head><title>Seed Data</title><style>");
        out.println("body{font-family:sans-serif;max-width:700px;margin:40px auto;background:#0f172a;color:#e2e8f0;}");
        out.println("h1{color:#38bdf8;} .ok{color:#4ade80;} .warn{color:#fbbf24;} .err{color:#f87171;}");
        out.println("pre{background:#1e293b;padding:16px;border-radius:8px;overflow:auto;}");
        out.println("</style></head><body><h1>🌱 Igishyitsi — Seed Data</h1><pre>");

        try {
            // --- Ensure a demo school exists ---
            School school = schoolDAO.findBySlug("demo");
            if (school == null) {
                school = schoolDAO.save(new School("Igishyitsi Demo School", "demo"));
                out.println("<span class='ok'>✔ School created:</span> Igishyitsi Demo School (id=" + school.getId() + ")");
            }
            TenantContext.setSchoolId(school.getId());

            // --- Create admin (and membership) ---
            User admin = userDAO.findByEmail("admin@igishyitsi.com");
            if (admin == null) {
                admin = new User();
                admin.setName("System Administrator");
                admin.setEmail("admin@igishyitsi.com");
                admin.setPassword(PasswordUtils.hashPassword("Admin@123"));
                admin.setRole(UserRole.SUPER_ADMIN);
                admin.setEmailVerified(true);
                userDAO.save(admin);
                admin = userDAO.findByEmail("admin@igishyitsi.com");
                out.println("<span class='ok'>✔ Admin created:</span> admin@igishyitsi.com — Admin@123");
            } else if (admin.getRole() != UserRole.SUPER_ADMIN) {
                // Upgrade a legacy school admin into the platform administrator
                admin.setRole(UserRole.SUPER_ADMIN);
                userDAO.save(admin);
                out.println("<span class='warn'>⚠ Upgraded admin to Platform Administrator</span>");
            }
            ensureMembership(school, admin, UserRole.SUPER_ADMIN);

            User existingTeacher = userDAO.findByEmail("teacher@igishyitsi.com");
            if (existingTeacher != null) {
                ensureMembership(school, existingTeacher, UserRole.TEACHER);
                ensureMembership(school, userDAO.findByEmail("alice@igishyitsi.com"), UserRole.STUDENT);
                ensureMembership(school, userDAO.findByEmail("bob@igishyitsi.com"), UserRole.STUDENT);
                out.println("<span class='warn'>⚠ Seed data already exists. Skipping.</span>");
                out.println("Admin: admin@igishyitsi.com / Admin@123");
                out.println("Teacher: teacher@igishyitsi.com / Teacher@123");
                out.println("Student 1: alice@igishyitsi.com / Student@123");
                out.println("Student 2: bob@igishyitsi.com / Student@123");
                out.println("</pre></body></html>");
                return;
            }

            // --- Create Teacher ---
            User teacher = new User();
            teacher.setName("Prof. Jean Paul Habimana");
            teacher.setEmail("teacher@igishyitsi.com");
            teacher.setPassword(PasswordUtils.hashPassword("Teacher@123"));
            teacher.setRole(UserRole.TEACHER);
            teacher.setEmailVerified(true);
            userDAO.save(teacher);
            teacher = userDAO.findByEmail("teacher@igishyitsi.com");
            ensureMembership(school, teacher, UserRole.TEACHER);
            out.println("<span class='ok'>✔ Teacher created:</span> teacher@igishyitsi.com — Teacher@123  (id=" + teacher.getId() + ")");

            // --- Create Students ---
            User alice = new User();
            alice.setName("Alice Uwimana");
            alice.setEmail("alice@igishyitsi.com");
            alice.setPassword(PasswordUtils.hashPassword("Student@123"));
            alice.setRole(UserRole.STUDENT);
            alice.setEmailVerified(true);
            userDAO.save(alice);
            alice = userDAO.findByEmail("alice@igishyitsi.com");
            ensureMembership(school, alice, UserRole.STUDENT);
            out.println("<span class='ok'>✔ Student created:</span> alice@igishyitsi.com — Student@123  (id=" + alice.getId() + ")");

            User bob = new User();
            bob.setName("Bob Nkurunziza");
            bob.setEmail("bob@igishyitsi.com");
            bob.setPassword(PasswordUtils.hashPassword("Student@123"));
            bob.setRole(UserRole.STUDENT);
            bob.setEmailVerified(true);
            userDAO.save(bob);
            bob = userDAO.findByEmail("bob@igishyitsi.com");
            ensureMembership(school, bob, UserRole.STUDENT);
            out.println("<span class='ok'>✔ Student created:</span> bob@igishyitsi.com — Student@123  (id=" + bob.getId() + ")");

            // --- Enroll students in class ---
            String className = "Software Engineering";

            StudentClass sc1 = new StudentClass();
            sc1.setStudent(alice);
            sc1.setTeacher(teacher);
            sc1.setClassName(className);
            studentClassDAO.save(sc1);

            StudentClass sc2 = new StudentClass();
            sc2.setStudent(bob);
            sc2.setTeacher(teacher);
            sc2.setClassName(className);
            studentClassDAO.save(sc2);
            out.println("<span class='ok'>✔ Enrolled alice + bob in:</span> " + className);

            // --- Create Assignments ---
            Assignment a1 = assignmentService.createAssignment(
                teacher,
                "Introduction to Software Design Patterns",
                "Research and write a 3-page summary on at least 3 Gang-of-Four design patterns " +
                "(e.g., Singleton, Factory, Observer). Include a real-world example for each and a UML diagram.",
                className,
                LocalDateTime.now().plusDays(7),
                null, // no Google Form for this one
                100
            );
            out.println("<span class='ok'>✔ Assignment 1 created:</span> \"" + a1.getTitle() + "\" (id=" + a1.getId() + ")");

            Assignment a2 = assignmentService.createAssignment(
                teacher,
                "REST API Design & Implementation",
                "Design and implement a RESTful API for a simple Todo application using any backend language. " +
                "Submit: 1) API documentation (Postman/Swagger), 2) Source code link (GitHub), 3) A 1-page reflection on design choices.",
                className,
                LocalDateTime.now().plusDays(14),
                null,
                100
            );
            out.println("<span class='ok'>✔ Assignment 2 created:</span> \"" + a2.getTitle() + "\" (id=" + a2.getId() + ")");

            Assignment a3 = assignmentService.createAssignment(
                teacher,
                "Unit Testing with JUnit",
                "Write comprehensive unit tests for the following Java class provided in the course materials. " +
                "Achieve at least 80% code coverage. Submit your test file and a coverage report screenshot.",
                className,
                LocalDateTime.now().plusDays(3),
                "https://forms.gle/exampleFormLink",  // optional Google Form as extra resource
                80
            );
            out.println("<span class='ok'>✔ Assignment 3 created:</span> \"" + a3.getTitle() + "\" (id=" + a3.getId() + ")");

            out.println("");
            out.println("<span class='ok'>✅ Seeding complete!</span>");
            out.println("");
            out.println("Login credentials:");
            out.println("  Teacher  : teacher@igishyitsi.com / Teacher@123");
            out.println("  Student 1: alice@igishyitsi.com  / Student@123");
            out.println("  Student 2: bob@igishyitsi.com    / Student@123");

        } catch (Exception e) {
            out.println("<span class='err'>❌ Error during seeding: " + e.getMessage() + "</span>");
            e.printStackTrace(out);
        } finally {
            TenantContext.clear();
        }

        out.println("</pre></body></html>");
    }

    /**
     * Creates a school membership for a user in the demo school when one does
     * not already exist. Does nothing if the user is {@code null}.
     */
    private void ensureMembership(School school, User user, UserRole role) {
        if (user == null) {
            return;
        }
        if (schoolMembershipDAO.findByUserAndSchool(user.getId(), school.getId()) == null) {
            schoolMembershipDAO.save(new SchoolMembership(school, user, role));
        }
    }
}