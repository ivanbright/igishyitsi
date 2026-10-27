package org.example.esubmission.dao;

import org.example.esubmission.model.ClassGroup;
import org.example.esubmission.model.School;
import org.example.esubmission.model.SchoolMembership;
import org.example.esubmission.model.StudentClass;
import org.example.esubmission.model.Subject;
import org.example.esubmission.model.User;
import org.example.esubmission.service.UserService;
import org.example.esubmission.util.PasswordUtils;
import org.example.esubmission.util.TenantContext;
import org.example.esubmission.util.UserRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two-school tenant-isolation test (P0). Proves that DAO findAll() reads are
 * school-scoped: rows seeded under school A are invisible to school B's scoped
 * reads cherry-, and vice versa. Self-cleaning: seeded schools/rows are deleted
 * in afterAll so the dev DB (furnitdb) is left untouched.
 */
class TenantIsolationTest {

    private static final SubjectDAO subjectDAO = new SubjectDAO();
    private static final ClassGroupDAO classGroupDAO = new ClassGroupDAO();
    private static final SchoolDAO schoolDAO = new SchoolDAO();
    private static final SchoolMembershipDAO schoolMembershipDAO = new SchoolMembershipDAO();
    private static final UserDAO userDAO = new UserDAO();

    private static School schoolA;
    private static School schoolBOP;
    private static Long subjectA;
    private static Long subjectB;
    private static Long classA;
    private static Long classB;
    private static Long sharedCodeSubjectA;
    private static Long sharedCodeSubjectB;
    private static Long sharedNameClassA;
    private static Long sharedNameClassB;
    private static Long duplicateProbeClass;

    @BeforeAll
    static void seedTwoSchools() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
        schoolA = schoolDAO.save(new School("Isolation A " + suffix, "iso-a-" + suffix));
        schoolBOP = schoolDAO.save(new School("Isolation B " + suffix, "iso-b-" + suffix));

        TenantContext.setSchoolId(schoolA.getId());
        subjectA = subjectDAO.save(new Subject("ISA-101", "Math A", "Math", 2)).getId();
        classA = classGroupDAO.save(new ClassGroup("Class A-1", "Room 101")).getId();

        TenantContext.setSchoolId(schoolBOP.getId());
        subjectB = subjectDAO.save(new Subject("ISB-101", "Math B", "Math", 2)).getId();
        classB = classGroupDAO.save(new ClassGroup("Class B-1", "Room 202")).getId();

        TenantContext.clear();
    }

    @Test
    void schoolAReadsOnlySchoolA() {
        TenantContext.setSchoolId(schoolA.getId());
        try {
            List<Subject> subjects = subjectDAO.findAll();
            List<ClassGroup> classes = classGroupDAO.findAll();
            assertEquals(1, subjects.size(), "school A must see only its own subject");
            assertEquals(1, classes.size(), "school A must see only its own class");
            assertEquals(subjectA, subjects.get(0).getId());
            assertEquals(classA, classes.get(0).getId());
            assertFalse(subjects.get(0).getId().equals(subjectB), "A must not see B's subject");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void schoolBReadsOnlySchoolB() {
        TenantContext.setSchoolId(schoolBOP.getId());
        try {
            List<Subject> subjects = subjectDAO.findAll();
            List<ClassGroup> classes = classGroupDAO.findAll();
            assertEquals(1, subjects.size(), "school B must see only its own subject");
            assertEquals(1, classes.size(), "school B must see only its own class");
            assertEquals(subjectB, subjects.get(0).getId());
            assertEquals(classB, classes.get(0).getId());
            assertFalse(subjects.get(0).getId().equals(subjectA), "B must not see A's subject");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void globalReadsStillResolveAfterScoping() {
        assertTrue(subjectDAO.findAllGlobal().size() >= 2, "global variant retains platform reads");
    }

    @Test
    void schoolCannotDeleteAnotherSchoolClass() {
        TenantContext.setSchoolId(schoolA.getId());
        try {
            classGroupDAO.delete(classB);
            TenantContext.setSchoolId(schoolBOP.getId());
            assertEquals(classB, classGroupDAO.findById(classB).getId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void twoSchoolsMayReuseTheSameSubjectCodeAndClassName() {
        String sharedCode = "SHARED-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String sharedName = "Shared Class " + UUID.randomUUID().toString().substring(0, 6);

        TenantContext.setSchoolId(schoolA.getId());
        try {
            sharedCodeSubjectA = subjectDAO.save(new Subject(sharedCode, "School A Subject", "General", 1)).getId();
            sharedNameClassA = classGroupDAO.save(new ClassGroup(sharedName, null)).getId();
        } finally {
            TenantContext.clear();
        }

        TenantContext.setSchoolId(schoolBOP.getId());
        try {
            sharedCodeSubjectB = subjectDAO.save(new Subject(sharedCode, "School B Subject", "General", 1)).getId();
            sharedNameClassB = classGroupDAO.save(new ClassGroup(sharedName, null)).getId();
            assertFalse(sharedCodeSubjectA.equals(sharedCodeSubjectB), "each school gets its own subject row");
            assertFalse(sharedNameClassA.equals(sharedNameClassB), "each school gets its own class row");
        } finally {
            TenantContext.clear();
        }

        cleanUpSharedRows();
    }

    @Test
    void duplicateClassNameWithinTheSameSchoolIsRejected() {
        String name = "Dup " + UUID.randomUUID().toString().substring(0, 6);
        TenantContext.setSchoolId(schoolA.getId());
        try {
            ClassGroup first = classGroupDAO.save(new ClassGroup(name, null));
            duplicateProbeClass = first.getId();
            assertNotNull(classGroupDAO.findByName(name), "first save is visible to the same school");
            assertThrows(RuntimeException.class, () -> classGroupDAO.save(new ClassGroup(name, null)),
                    "a second class with the same name in the same school must be rejected");
        } finally {
            TenantContext.clear();
            cleanUpDuplicateProbe();
        }
    }

    private static void cleanUpSharedRows() {
        try {
            if (sharedCodeSubjectA != null) {
                TenantContext.setSchoolId(schoolA.getId());
                subjectDAO.delete(sharedCodeSubjectA);
                sharedCodeSubjectA = null;
            }
            if (sharedCodeSubjectB != null) {
                TenantContext.setSchoolId(schoolBOP.getId());
                subjectDAO.delete(sharedCodeSubjectB);
                sharedCodeSubjectB = null;
            }
            if (sharedNameClassA != null) {
                TenantContext.setSchoolId(schoolA.getId());
                classGroupDAO.delete(sharedNameClassA);
                sharedNameClassA = null;
            }
            if (sharedNameClassB != null) {
                TenantContext.setSchoolId(schoolBOP.getId());
                classGroupDAO.delete(sharedNameClassB);
                sharedNameClassB = null;
            }
        } finally {
            TenantContext.clear();
        }
    }

    private static void cleanUpDuplicateProbe() {
        try {
            if (duplicateProbeClass != null) {
                TenantContext.setSchoolId(schoolA.getId());
                classGroupDAO.delete(duplicateProbeClass);
                duplicateProbeClass = null;
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void enrollmentsAreScopedToTheActiveSchool() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
        School schoolAEnroll = schoolDAO.save(new School("Enroll A " + suffix, "enroll-a-" + suffix));
        School schoolBEnroll = schoolDAO.save(new School("Enroll B " + suffix, "enroll-b-" + suffix));
        String sharedClass = "Shared " + suffix;

        UserService userService = new UserService(userDAO);
        User teacherA = userService.createUser("Teacher A " + suffix, "ta-" + suffix + "@test.local", null, "Teach@1234", UserRole.TEACHER);
        User studentA = userService.createUser("Student A " + suffix, "sa-" + suffix + "@test.local", null, "Stud@12345", UserRole.STUDENT);
        User teacherB = userService.createUser("Teacher B " + suffix, "tb-" + suffix + "@test.local", null, "Teach@1234", UserRole.TEACHER);
        User studentB = userService.createUser("Student B " + suffix, "sb-" + suffix + "@test.local", null, "Stud@12345", UserRole.STUDENT);

        StudentClassDAO enrollmentDAO = new StudentClassDAO();
        try {
            TenantContext.setSchoolId(schoolAEnroll.getId());
            enrollmentDAO.save(newEnrollment(studentA, sharedClass, teacherA, schoolAEnroll.getId()));
            TenantContext.setSchoolId(schoolBEnroll.getId());
            enrollmentDAO.save(newEnrollment(studentB, sharedClass, teacherB, schoolBEnroll.getId()));

            TenantContext.setSchoolId(schoolAEnroll.getId());
            List<User> inA = enrollmentDAO.findStudentsInClass(sharedClass);
            assertEquals(1, inA.size());
            assertEquals(studentA.getEmail(), inA.get(0).getEmail());
            assertEquals(1, enrollmentDAO.countStudentsByClassName(sharedClass));
            assertEquals(List.of(sharedClass), enrollmentDAO.findAllClasses());
            assertEquals(1, enrollmentDAO.findTeachersByClass(sharedClass).size());
            assertEquals(teacherA.getEmail(), enrollmentDAO.findTeachersByClass(sharedClass).get(0).getEmail());
            assertEquals(1, enrollmentDAO.findStudentsByTeacherAndClass(teacherA.getId(), sharedClass).size());

            TenantContext.setSchoolId(schoolBEnroll.getId());
            List<User> inB = enrollmentDAO.findStudentsInClass(sharedClass);
            assertEquals(1, inB.size());
            assertEquals(studentB.getEmail(), inB.get(0).getEmail());
            assertTrue(enrollmentDAO.findStudentsByTeacherAndClass(teacherA.getId(), sharedClass).isEmpty());
            assertTrue(enrollmentDAO.findEnrollmentsByTeacher(teacherA.getId()).isEmpty());
            assertTrue(enrollmentDAO.findStudentClassesByStudent(studentA.getId()).isEmpty());
        } finally {
            TenantContext.setSchoolId(schoolAEnroll.getId());
            enrollmentDAO.removeByStudentAndClass(studentA.getId(), sharedClass);
            userDAO.delete(studentA.getId());
            userDAO.delete(teacherA.getId());
            TenantContext.setSchoolId(schoolBEnroll.getId());
            enrollmentDAO.removeByStudentAndClass(studentB.getId(), sharedClass);
            userDAO.delete(studentB.getId());
            userDAO.delete(teacherB.getId());
            TenantContext.clear();
            schoolDAO.delete(schoolAEnroll.getId());
            schoolDAO.delete(schoolBEnroll.getId());
        }
    }

    private static StudentClass newEnrollment(User student, String className, User teacher, Long schoolId) {
        StudentClass enrollment = new StudentClass();
        enrollment.setSchoolId(schoolId);
        enrollment.setStudent(student);
        enrollment.setClassName(className);
        enrollment.setTeacher(teacher);
        return enrollment;
    }

    @Test
    void platformCanProvisionASchoolAdminWithoutAnActiveTenant() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
        School fresh = schoolDAO.save(new School("Bootstrap " + suffix, "boot-" + suffix));
        String email = "boot-" + suffix + "@test.local";
        User admin = new UserService(new UserDAO()).createUser("Bootstrap Admin", email, null, "Boot@12345", UserRole.ADMIN);
        SchoolMembership membership = null;
        try {
            assertNotNull(admin.getId());
            assertEquals(UserRole.ADMIN, admin.getRole());
            assertNotEquals("Boot@12345", admin.getPassword());
            assertTrue(PasswordUtils.checkPassword("Boot@12345", admin.getPassword()));

            TenantContext.clear();
            membership = schoolMembershipDAO.saveForPlatform(new SchoolMembership(fresh, admin, UserRole.ADMIN));

            assertNotNull(membership.getId());
            assertEquals(fresh.getId(), membership.getSchool().getId());

            List<SchoolMembership> members = schoolMembershipDAO.findBySchoolId(fresh.getId());
            assertEquals(1, members.size());
            assertEquals(email, members.get(0).getUser().getEmail());

            TenantContext.clear();
            assertThrows(IllegalStateException.class,
                    () -> schoolMembershipDAO.save(new SchoolMembership(fresh, admin, UserRole.ADMIN)));
        } finally {
            TenantContext.setSchoolId(fresh.getId());
            try {
                if (membership != null) {
                    schoolMembershipDAO.remove(membership.getId());
                }
                userDAO.delete(admin.getId());
            } catch (Exception ignored) {
            } finally {
                TenantContext.clear();
            }
            schoolDAO.delete(fresh.getId());
        }
    }

    @AfterAll
    static void tearDownSeededRows() {
        cleanUpSharedRows();
        cleanUpDuplicateProbe();
        try {
            if (subjectA != null) {
                TenantContext.setSchoolId(schoolA.getId());
                subjectDAO.delete(subjectA);
            }
            if (subjectB != null) {
                TenantContext.setSchoolId(schoolBOP.getId());
                subjectDAO.delete(subjectB);
            }
            if (classA != null) {
                TenantContext.setSchoolId(schoolA.getId());
                classGroupDAO.delete(classA);
            }
            if (classB != null) {
                TenantContext.setSchoolId(schoolBOP.getId());
                classGroupDAO.delete(classB);
            }
            if (schoolA != null) schoolDAO.delete(schoolA.getId());
            if (schoolBOP != null) schoolDAO.delete(schoolBOP.getId());
        } finally {
            TenantContext.clear();
        }
    }
}
