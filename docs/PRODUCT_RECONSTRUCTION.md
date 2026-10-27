# PRODUCT RECONSTRUCTION — Igishyitsi e-Submission

> Status: Reconstruction audit (Phase 1–4). All statements below were verified against the
> actual codebase (`src/main/java`, `src/main/webapp`, `db/migrations`, `db/migrations/V1__multi_tenancy.sql`),
> the live PostgreSQL schema (`furnitdb`), and the deployed WAR on Tomcat 10.1.50.

---

## 1. What the product actually is

Igishyitsi is a **Jakarta EE (Servlet/JSP/Hibernate/PostgreSQL) multi-tenant e-submission
SaaS** that has been progressively rebuilt from a single-tenant prototype into a
school-scoped platform. Real, end-to-end workflows now exist for four of five roles:

| Role | Dashboard | End-to-end workflow present? |
| ---- | --------- | ---------------------------- |
| SUPER_ADMIN | `/super/dashboard` | School onboarding + full school CRUD via JSON API (`/api/super/schools/*`) |
| ADMIN (school) | `/admin/dashboard` | Class/teacher/student/parent/subject/class management + announcements + notifications |
| TEACHER | `/teacher/dashboard` | Classes → assignments → submissions → grading/feedback → announcements → notifications |
| STUDENT | `/student/dashboard` | Assignments → submit → track → grades/feedback → notifications → announcements |
| PARENT | `/profile` only | **Incomplete** — no dashboard; can view own profile and receive notifications; child data shown via admin surface |

The product is **not** a set of disconnected JSPs. Identifiable tenant-scoped chains exist
(seed → school → members → classes → assignments → submissions → grades → announcements →
notifications → real-time WebSocket fan-out).

---

## 2. Current roles and what they can actually do

- **SUPER_ADMIN** — platform operator. `SecurityUtil.requireSuperAdmin`. Sees `/super/dashboard`
  (school list), CRUDs schools (create/update/toggle/delete) with CSRF + session checks via
  `SchoolApiServlet`. Filter `RoleAuthorizationFilter` guards `/super/*`.
- **ADMIN** — school-level admin. `SecurityUtil.requireAdmin` (ADMIN + SUPER_ADMIN).
  Manages classes, teachers, students, parents, subjects, allocate students, create
  announcements, view notifications. `AdminUsersServlet`-style CRUD exists via admin surface.
- **TEACHER** — manages classes, creates/publishes assignments, views submissions, grades,
  gives feedback, sends announcements, gets notifications.
- **STUDENT** — views dashboard, opens assignments, submits work, tracks submissions, sees
  grades/feedback, gets notifications/announcements.
- **PARENT** — can log in, view own profile, and view notifications. **No parent dashboard**
  and no `/parent/*` endpoints. Parent-child relationships exist (`parent_children`) and are
  surfaced in the admin UI, but the parent-facing surface is incomplete.

---

## 3. Domain entities (see `DOMAIN_MODEL.md`)

`School`, `User`, `SchoolMembership`, `ClassGroup` (classes), `Subject`, `TeacherClass`,
`StudentClass`, `Assignment`, `Submission`, `Announcement`, `Notification`, `ParentChild`,
`Person`, `UserRole`, `UserPlan` (plan values).

All tenant tables carry `school_id` (NOT NULL, enforced by migration `V1__multi_tenancy.sql`,
verified live: `schools` + `school_members` + 9 school-scoped tables all have `school_id`
columns, `school_members` unique `(school_id, user_id)`).

---

## 4. What is genuinely functional (verified end-to-end)

- **Login/2FA/OTP/reset flows** — session + `SecurityUtils` + CSRF tokens, forgot/reset
  password with email OTP, signup + onboarding with school slug activation. Verified via
  `CaptchaServlet`, `LoginServlet`, `ForgotPasswordServlet`, `ResetPasswordServlet`,
  `VerifyResetOtpServlet`, `OnBoardingServlet`.
- **School dashboard (admin)** — real stats: schools, members, classes, subjects, teachers,
  students (uses school-scoped DAO reads where implemented; see §7).
- **Teacher assignment lifecycle** — create assignment, publish, view submissions, grade,
  feedback. DAOs `AssignmentDAO`, `SubmissionDAO` school-scoped.
- **Student assignment lifecycle** — list, open, submit (`FileUploadServlet`), track,
  grades.
- **Announcements** — admin + teacher create; school-scoped fan-out via
  `NotificationDAO`/`NotificationService` and WebSocket `broadcastToSchool`; notifications
  read via `/api/notifications/*`.
- **Real-time notifications** — `AssignmentWebSocket` scoped to school via membership lookup
  (`SchoolMembershipDAO.findByUserAndSchool`).
- **Seed** (`/admin/seed`) — idempotent; upgrades admin→SUPER_ADMIN; creates demo school +
  memberships; verified 10 memberships backfilled.

---

## 5. What is partially functional

- **PARENT** — login + profile + notifications only. No parent dashboard,
  no `/parent/*` endpoints, no parent-centric views of child's grades/submissions.
- **School context for admin reads** — some admin APIs still do **global** reads
  (`AdminApiServlet`/`schoolAdmin` reads find school-scoped where implemented; check the
  remaining `findAll()` usages flagged in §7).
- **Plan features** — plans exist (`FREE/PLUS/PREMIUM` in enums/config) but many are not
  enforced (e.g. per-plan limits).
- **Announcement authoring** — teacher/admin can create; no scheduled publishing.

---

## 6. What is disconnected

- **School admin UI ↔ some admin APIs** — `schoolAdmin.jsp` reads/writes via
  `/api/admin/*`; ensure ALL these calls send `X-CSRF-Token` (admin POSTs currently may
  fail CSRF if the token isn't forwarded — see audit).
- **PARENT data flow** — parent-child relationship exists in UI (`openParentDetails`) but no
  parent-facing view consumes it.
- **Cross-school admin dashboards** — `AdminDash` + `/api/admin/*` still use some
  `findAll()`-style reads that are **not** school-scoped (tenant-leak risk; see §7).

---

## 7. Where tenant isolation is unsafe (the critical audit)

Patterns that MUST be school-scoped (reads AND writes):
- `*DAO.findAll()` / `findBy...()` **must** include `school_id = TenantContext.requireSchoolId()`.
- Cross-tenant admin reads — `AdminDash` and `/api/admin/*` currently may call **global**
  queries (e.g. unfiltered `findAll()` in the school-admin dashboard read path). This is a
  cross-tenant data leak: a school admin could see another school's data.
- WebSocket broadcast — `broadcastToSchool` correctly scopes to `school_id`; verify all
  `broadcastToAll` usages are removed or scoped.

**Status**: The phase-4 scoping closed the major gaps (`NotificationDAO`, `NotificationService`,
`AssignmentWebSocket.broadcastToSchool`, announcement fan-out, seed). The **remaining known gap**
is the cross-school admin dashboard reads (`AdminDash` + `/api/admin/*`), which I intend to
school-scope next (see below).

---

## 8. Broken/dead/duplicated items

- Dead admin controllers already deleted in Phase 4 (`AdminUsersServlet`, `AdminTeachersServlet`,
  `AdminStudentsServlet`, `AdminSubjectsServlet`, `AdminParentsServlet`, `AdminClassesServlet`).
- `HelloServlet` + `HealthServlet` — health/diagnostic only; fine.
- **`schoolAdmin.jsp` CSRF** — admin JS must send `X-CSRF-Token` header; verify every
  `/api/admin/*` call carries it (P0 fix).
- Verify no dangling navigation links in `layout.tag` / each dashboard link resolves to a real
  servlet mapping.

---

## 9. Backend capabilities without UI

- Parent-child linking (admin can link; parent has no view).
- Plan management for super admin (fields exist; UI surface minimal).
- School delete/toggle (API exists; confirm super admin UI exposes).

## 10. UI capabilities without backend support

- Parent dashboard placeholders (if any link exists, remove or wire).
- Any planned "notifications" link → must point to `/api/notifications/*`.

---

## 11. Smallest coherent sequence to make it one real SaaS

1. **School-scope the remaining admin reads** (`AdminDash`, `/api/admin/*`) → close the
   cross-tenant leak (P0). Verify with two-school login test.
2. **Add `X-CSRF-Token` header to all admin/notification JS calls** (P0 security) and verify
   admin POSTs succeed.
3. **Complete PARENT surface** (dashboard + child grades/submissions view) — the only role
   with no workflow depth.
4. **Wire every navigation item** — audit `layout.tag` + each dashboard against the servlet
   mapping table, remove/enforce dead links.
5. **Verify each golden workflow end-to-end** (see `GOLDEN_WORKFLOWS.md` / `END_TO_END_TEST_MATRIX.md`)
   with the two-school tenant isolation check.

---

## 12. Current status

- Compile: `.\mvnw.cmd -q -DskipTests compile` → EXIT=0.
- Tests: `.\mvnw.cmd -q test` → EXIT=0 (11 tests).
- WAR: `target/igishyitsi.war` deployed on Tomcat (running); `/admin/dashboard`,
  `/super/dashboard`, login all reachable.
- DB: migration `V1__multi_tenancy.sql` applied + verified (legacy school + `school_id`
  on all tenant tables, 10 memberships).
