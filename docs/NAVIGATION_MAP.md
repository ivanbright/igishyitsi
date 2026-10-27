# NAVIGATION MAP — Igishyitsi

Same schema as the product spec: navigation must reflect what each role actually does.
Every link below must resolve to a real, working, tenant-scoped destination
(no dead links; see `END_TO_END_TEST_MATRIX.md`).

---

## SUPER_ADMIN
```
Dashboard            /super/dashboard
Schools (CRUD)       /super/dashboard  (+ /api/super/schools JSON)
  → Create school    SchoolApiServlet action=create
  → Edit school      action=update
  → Toggle active    action=toggle
  → Delete school    action=delete
Profile              /profile
Logout               /logout
```
*(Redirect on login: SUPER_ADMIN → /super/dashboard.)*

---

## ADMIN (school)
```
Dashboard            /admin/dashboard
Manage Classes       /admin/classes
Manage Teachers      managed via schoolAdmin/admin dashboard (school-scoped)
Manage Students      schoolAdmin.jsp / admin surface
Manage Parents       schoolAdmin.jsp (parent links exist)
Manage Subjects      admin surface
Allocate Students    /admin/allocate
Announcements        AdminCreateAnnouncementServlet (school-scoped)
Notifications        /api/notifications/* (school-scoped DAO)
Profile              /profile
Logout               /logout
```

---

## TEACHER
```
Dashboard            /teacher/dashboard
My Classes           /teacher/dashboard (class list)
  Open class         ManageClassesServlet / classes
Create Assignment    /teacher/… (AssignmentCreate)
View Submissions     /teacher/… (AssignmentDetailsServlet)
Grade/Feedback       submissionDetails
Announcements        (teacher-created; school-scoped)
Profile              /profile
Logout               /logout
```

---

## STUDENT
```
Dashboard            /studentDash
Assignments          /assignments (school+class-scoped)
  Open assignment    /assignmentDetails
  Submit             /submitAssignment
My Submissions       /student/mySubmissions
Grades               /grades
Profile              /profile
Notifications        /api/notifications/* (school-scoped)
Logout               /logout
```

---

## PARENT
```
Profile              /profile            (only surface currently)
Notifications        /api/notifications/* (school-scoped; if role permitted)
Logout               /logout
```
*No parent dashboard exists — Phase 5 gap; do NOT fabricate links to one.*

---

## Cross-cutting
- Every restricted path is covered by `AuthenticationFilter` + `RoleAuthorizationFilter`
  (see web.xml); `/super/*` guarded to SUPER_ADMIN; `/admin/*` allowed for ADMIN + SUPER_ADMIN.
- `layout.tag` sidebar reflects the role from session; SUPER_ADMIN sees the platform section.
- All JSON APIs require `X-CSRF-Token` header (or `csrfToken` param) + role check.
