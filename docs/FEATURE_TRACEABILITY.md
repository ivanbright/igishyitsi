# FEATURE TRACEABILITY — Igishyitsi

Status: Phase 1–4 complete; Phase 5 = close the following gaps (marked `GAP`).

| # | Feature                | Role(s)         | UI surface                      | Endpoint / servlet            | DAO                       | Entity                       | Tenant-scoped? | Status         |
|---|------------------------|-----------------|---------------------------------|-------------------------------|---------------------------|------------------------------|----------------|----------------|
| 1 | School onboarding      | SUPER_ADMIN     | superAdmin.jsp                  | `/super/dashboard`, `/api/super/schools` | SchoolDAO                 | School                       | platform       | DONE (impl)    |
| 2 | School CRUD            | SUPER_ADMIN     | superAdmin.jsp                  | SchoolApiServlet               | SchoolDAO                 | School                       | platform       | DONE (impl)    |
| 3 | Tenant isolation       | all             | filters                         | TenantFilter, TenantContext   | All DAOs (school-stamped) | all                          | school         | DONE (impl)    |
| 4 | Login/redirect by role | all             | index.jsp/login.jsp             | LoginServlet                  | UserDAO                   | User                         | —              | DONE (impl)    |
| 5 | Class management       | ADMIN           | school admin surface            | `/admin/classes`, ManageClassesServlet | ClassGroupDAO        | ClassGroup                   | school         | DONE (impl)    |
| 6 | Subject management     | ADMIN           | admin surface                   | ManageSubjects servlet        | SubjectDAO                | Subject                      | school         | DONE (impl)    |
| 7 | Teacher management     | ADMIN           | admin surface                   | ManageTeachers servlet        | UserDAO + membership      | User, SchoolMembership       | school         | DONE (impl)    |
| 8 | Student management     | ADMIN           | admin surface                   | ManageStudents servlet        | UserDAO + membership      | User, SchoolMembership       | school         | DONE (impl)    |
| 9 | Student allocation     | ADMIN           | `/admin/allocate`               | AllocateStudentServlet        | StudentClassDAO           | StudentClass                 | school         | DONE (impl)    |
|10 | Teacher→class assign   | ADMIN           | admin surface                   | TeacherClassDAO writes        | TeacherClassDAO           | TeacherClass                 | school         | DONE (impl)    |
|11 | Assignment create      | TEACHER         | teacher dash                    | AssignmentCreateServlet       | AssignmentDAO             | Assignment                   | school         | DONE (impl)    |
|12 | Assignment submit      | STUDENT         | student dash                    | SubmitAssignmentServlet       | SubmissionDAO             | Submission                   | school         | DONE (impl)    |
|13 | Submission grade/feedback | TEACHER     | teacher dash                    | AssignmentDetails/Grade servlet | SubmissionDAO          | Submission                   | school         | DONE (impl)    |
|14 | Student submissions    | STUDENT         | `/student/mySubmissions`        | MySubmissionsServlet          | SubmissionDAO             | Submission                   | school         | DONE (impl)    |
|15 | Grades view            | STUDENT         | `/grades`                       | GradesServlet                 | SubmissionDAO             | Submission                   | school         | DONE (impl)    |
|16 | Announcements          | ADMIN/TEACHER   | admin surface                   | AdminCreateAnnouncementServlet| AnnouncementDAO          | Announcement                 | school         | DONE (impl)    |
|17 | Notifications (in-app) | all             | toast + bell                    | `/api/notifications/*`, NotificationService | NotificationDAO  | Notification                 | school         | DONE (impl)    |
|18 | WebSocket broadcast    | all             | in-app toast                   | AssignmentWebSocket.broadcastToSchool | SchoolMembershipDAO | —                       | school         | DONE (impl)    |
|19 | Parent-child link      | ADMIN           | schoolAdmin.jsp                 | ParentChildDAO                | ParentChild               | school                      | DONE (impl)    |
|20 | **PARENT dashboard**   | PARENT          | — none —                        | — none —                      | —                          | —                            | —              | **GAP**         |
|21 | **AdminDash global reads** | ADMIN       | `/admin/dashboard`              | AdminDash                     | `*DAO.findAll()` global    | all                          | **GLOBAL**     | **GAP (P0)**    |
|22 | **`/api/admin/*` reads** | ADMIN        | admin JS                       | AdminApiServlet               | `*DAO.findAll()` global    | all                          | **GLOBAL**     | **GAP (P0)**    |
|23 | CSRF header in admin JS | ADMIN         | schoolAdmin.jsp / admin JS     | admin POSTs                   | —                           | —                            | —              | **GAP (P0)**    |

---

## Known cross-tenant leaks (must fix — Phase 5/P0)

- **#21 / #22** — `AdminDash` and `/api/admin/*` still call **global** `findAll()` in reads,
  so a school admin can see/read other schools' data (cross-tenant leak). Tenant isolation is
  safe on **writes** (school-stamped) but **not on reads** for these surfaces.
- **Fix #1**: school-scope these reads through `TenantContext.getSchoolId()` filters.
- **Fix #2 (CSRF)**: add `X-CSRF-Token` header to schoolAdmin/admin JS API calls (currently
  only superAdmin.js does) so admin POSTs pass the CSRF check.
