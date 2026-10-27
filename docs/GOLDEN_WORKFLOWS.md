# GOLDEN WORKFLOWS — Igishyitsi

Verified against live servlets, DAOs, filters, JSPshol and the deployed WAR. Each workflow
is the end-to-end chain that must work **as a whole** (`IMPLEMENTED` only when verified;
see `PRODUCT_RECONSTRUCTION.md` §19).

---

## GB1 — SCHOOL ONBOARDING (SUPER_ADMIN)

**Roles:** SUPER_ADMIN. **Entry:** `/super/dashboard`. **Exit:** school appears in super list; admin can log in.

**Chain (verified):**
```text
SuperAdminServlet (/super/dashboard; requires SUPER_ADMIN)
  → Schools list (SchoolDAO.findAll — platform scope)
  → /api/super/schools (SchoolApiServlet.doPost; action=create)
      → CSRF (X-CSRF-Token header) + requireSuperAdmin
      → SchoolDAO.save (slug uniqueness validated)
  → superAdmin.jsp refreshes list (SchoolApiServlet.doGet)

→ SUPER_ADMIN→ADMIN of new school: AdminAddUser/Seed (ADMIN role + SchoolMembership)
→ ADMIN logs in → TenantFilter resolves school → /admin/dashboard (school-scoped)
```
**Verify per §7:** every line above must actually round-trip in the running WAR
(see `END_TO_END_TEST_MATRIX.md` GB1).

---

## GB2 — SCHOOL SETUP (SCHOOL ADMIN inside own tenant)

**Roles:** ADMIN. **Entry:** `/admin/dashboard`. **Exit:** class+subject+teacher+student relationships exist and are visible.

**Chain:**
```text
ManageClassesServlet (/admin/classes) → create class (school-scoped)
ManageTeachersServlet (school-scoped) → create teacher (+school_members ADMIN for teacher)
ManageStudentsServlet (school-scoped) → create student
AllocateStudentServlet (/admin/allocate) → student ↔ class (school-scoped)
TeacherClassDAO → assign teacher → class/subject (school-scoped)
ManageSubjectsServlet (school-scoped) → create subject
AdminCreateAnnouncementServlet → school-scoped announcement fan-out

→ Then each of these must be reachable/visible from the corresponding admin surface
  (schoolAdmin.jsp / admin dashboard) — not just persisted.
```
**Cross-tenant check:** all DAO writes above stamp `school_id` from `TenantContext`; admin
reads must be school-scoped (see §7 — the AdminDash global-read gap must be closed).

---

## GB3 — ASSIGNMENT LIFECYCLE (CORE)

**Roles:** TEACHER → STUDENT → TEACHER. **Entry:** teacher `createAssignment`. **Exit:** student sees grade + feedback.

**Chain (the critical one):**
```text
Teacher: /teacher/dashboard → create assignment (title, class, subject, due, description)
  → AssignmentDAO.save (school-stamped, published=?)

Student: /studentDash → sees assignment (school+class-scoped)
  → /submitAssignment → file/answer upload (SubmissionDAO.save school-stamped)
  → /student/mySubmissions → sees own submission + status

Teacher: /teacher/assignment?details → sees submissions for that assignment
  → grade + feedback (AssignmentDetailsServlet / submissionDetails)

Student: /submissionDetails → sees grade + feedback
Notification: Submission graded → NotificationService.create (school-scoped)
WebSocket: AssignmentWebSocket.broadcastToSchool → student's in-app toast
```
**Verify:** go through the WHOLE chain in the browser with a real teacher + student in the
same school, and confirm the school-scoped WebSocket broadcast reaches only that school's
student (cross-school negative test in `END_TO_END_TEST_MATRIX.md`).

---

## GB4 — ANNOUNCEMENT PUBLISH & DELIVER

**Roles:** ADMIN/TEACHER → all school members.

**Chain:**
```text
AdminCreateAnnouncementServlet (school-scoped; requires school admin/teacher)
  → AnnouncementDAO.save (audience-scoped)
  → NotificationService.create for each target school member (school-scoped)
  → broadcastToSchool (WebSocket) → recipients' in-app notification + toast
```
**Verify:** create as school A admin → only school A members see it; school B must NOT.

---

## GB5 — STUDENT SUBMISSION + GRADING LOOP

Covered by GB3; separate node for test isolation. See `END_TO_END_TEST_MATRIX.md` GB5.

---

## GB6 — PARENT (current reality)

**Role:** PARENT. **Current actual surface:** login → `/profile` only (LoginServlet redirects
PARENT to `/profile`; RoleAuthorizationFilter blocks PARENT from all dashboards).
Parent-child links exist (`ParentChild` / parent_children), but **no parent dashboard
workflow exists**. Marked `BLOCKED/GAP` — the parent surface is reconstructed in Phase 5.

---

## GB7 — SUPER_ADMIN → DEMO SCHOOL → ADMIN → SCHOOL DATA

Cross-role "same school" chain (the product-as-one-system test):
```text
Super admin creates demo school (GB1)
→ seed creates ADMIN membership (SeedDataServlet)
→ ADMIN logs in → creates class/student/assignment
→ Teacher logs in → opens class → sees students → grades
→ Student logs in → sees class/assignments → submits → gets grade
→ Admin logs in → sees the resulting state (school-scoped)
```
This is the single most important end-to-end acceptance path. Test it end-to-end
(see `END_TO_END_TEST_MATRIX.md`).
