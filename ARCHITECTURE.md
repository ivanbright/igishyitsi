# Igishyitsi e-Submission — Architecture Design

Modernized from a single-tenant assignment-submission prototype into a **multi-tenant
Software-as-a-Service (SaaS) platform**. Each school is a tenant with fully isolated data
(sharing one schema via row-level `school_id` scoping) and its own per-school member with a
school-scoped role.

---

## 1. System Overview

**Igishyitsi** is a Java/Jakarta EE (Servlet) web application that lets schools manage
submissions online:

- **Students** browse assignments, submit files/answers, and track grades.
- **Teachers** create/grade assignments nested under their classes and send announcements.
- **School Admins** manage classes, students, teachers, parents, announcements, and grades.
- **Super Admins (platform level)** onboard and manage schools (tenants) and their subscription plans.

The application runs on a single shared database (PostgreSQL `furnitdb`), a single servlet
container (Tomcat 10.1, Jakarta EE 10/Servlet 6.0), and uses a **shared-schema,
row-level-scoped** multi-tenancy model: every tenant table carries a `school_id` column, and
all DAO writes/reads are filtered through a `TenantContext` resolved per request.

---

## 2. Technology Stack

| Concern | Technology |
|---|---|
| Language / platform | Java 21 (bytecode target 21) |
| Web framework | Jakarta Servlet 6.0 + JSP/JSTL, Jakarta Mail, Jakarta WebSocket (2.1) |
| Persistence | Hibernate ORM 6.2 (JPA 3.1 mapping), `hbm2ddl=update` |
| Database | PostgreSQL 10 (`furnitdb`), `schools`, `school_members` etc. |
| JSON | Jackson 2.16 (`jackson-databind`, `jsr310`) |
| Auth / tokens | JSESSIONID sessions, JWT (jjwt 0.12.3) API tokens, bcrypt (jbcrypt 0.4) |
| CSRF | Session-bound tokens, validated in filters and mutating servlets |
| Build | Maven 3 (WAR packaging, final name `igishyitsi`), wrapper `mvnw.cmd` |
| Servlet container | Apache Tomcat 10.1.50 (context path `/igishyitsi`) |
| Front-end | JSP + JSTL, shared `layout.tag` master page, CSS (`dashboard.css`, `toast.css`), vanilla JS with `fetch` to JSON APIs |

---

## 3. High-Level Layer Diagram

```
Browser (JSP/JSPs + layout.tag)
   │  HTML/JSON/fetch (X-CSRF-Token header for mutating calls)
   ▼
Servlet container (Tomcat) — WAR "igishyitsi"
   │
   ├─ Filter chain (see §5)
   │   └─ TenantFilter → TenantContext (school_id for the request)
   ▼
Controllers (servlets)  ←─────────  role (session role + SchoolMembership role)
   │  doGet/doPost; CSRF check; wrap TenantContext/DAO calls
   ▼
Service layer (NotificationService, UserService, EmailService, ...)
   ▼
DAO layer (Hibernate)  models carry school_id; DAOs call TenantContext
   ▼
Hibernate SessionFactory (HibernateUtil)
   ▼
PostgreSQL furnitdb   (school-scoped rows; NOT NULL school_id constraints)
```

---

## 4. Package / Component Map

Source root: `src/main/java/org/example/esubmission/`

| Package | Contents | Responsibility |
|---|---|---|
| `controller` | `LoginServlet`, `OnBoardingServlet`, `VerificationServlet`, `ForgotPasswordServlet`, `ResetPasswordServlet`, `VerifyResetOtpServlet`, `LogoutServlet`, `SignUpServlet`, `CaptchaServlet`, `StudentDash`, `TeachersDash`, `AdminDash`, `SuperAdminServlet`, `ManageClassesServlet`, `AllocateStudentServlet`, `ViewStudentsServlet`, `AssignmentDetailsServlet`, `SubmitAssignmentServlet`, `SubmissionDetailsServlet`, `MySubmissionsServlet`, `GradeSubmissionServlet`, `GradesServlet`, `ProfileServlet`, `AdminAddUserServlet`, `AdminEditUserServlet`, `AdminCreateAnnouncementServlet`, `NotificationApiServlet`, `AssignmentsApiServlet`, `AdminApiServlet`, `SchoolApiServlet`, `SeedDataServlet` | HTTP entry points. Fetch request data, build models, enforce auth/CSRF, delegate to services/DAOs, forward to JSPs or write JSON. |
| `filter` | `CharacterEncodingFilter`, `InputSanitizationFilter`, `TenantFilter`, `AuthenticationFilter`, `RoleAuthorizationFilter` | Cross-cutting request pipeline (§5). |
| `dao` | `UserDAO`, `SchoolDAO`, `SchoolMembershipDAO`, `ClassGroupDAO`, `TeacherClassDAO`, `StudentClassDAO`, `SubjectDAO`, `AssignmentDAO`, `SubmissionDAO`, `AnnouncementDAO`, `NotificationDAO`, `ParentChildDAO` | Data access; every `save()` stamps `school_id` from `TenantContext`. |
| `model` | `User`, `School`, `SchoolMembership`, `ClassGroup`, `TeacherClass`, `StudentClass`, `Subject`, `Assignment`, `Submission`, `Announcement`, `Notification`, `ParentChild`, `Person` | JPA entities. |
| `service` | `UserService`, `AssignmentService`, `NotificationService`, `EmailService`, `PhoneService` | Business logic / cross-servlet coordination. |
| `util` | `HibernateUtil` (SessionFactory), `SecurityUtil` (auth/roles/CSRF), `TenantContext`, `JwtUtil`, `JsonUtil`, `PasswordUtils`, `AdminProjections` (DTO maps), `ConfigUtil`, `UserRole`, `Gender` | Shared helpers / constants. |
| `websocket` | `AssignmentWebSocket` | Real-time notification fan-out, **school-scoped** via `broadcastToSchool`. |

Webapp (`src/main/webapp`):

- `WEB-INF/web.xml` — filter order, servlet mappings, welcome file.
- `WEB-INF/tags/layout.tag` — master layout with role-aware sidebar (STUDENT / TEACHER / ADMIN / **SUPER_ADMIN**).
- `WEB-INF/pages/*.jsp` — views (`login.jsp`, `studentDash.jsp`, `teacherDash.jsp`, `schoolAdmin.jsp`, `superAdmin.jsp`, `assignmentDetails.jsp`, `submissionDetails.jsp`, `profile.jsp`, onboarding/verify/reset, error pages).
- `css/`, `js/`, `images/` — static assets; `js/toast.js` for toast notifications.

---

## 5. Request Lifecycle & Filter Chain

Order defined in `web.xml` (all mapped to `/*` unless noted):

1. `CharacterEncodingFilter` — forces UTF-8 on request/response.
2. `InputSanitizationFilter` — sanitizes all request parameters (XSS defense).
3. `TenantFilter` — resolves the active **school_id** for the request:
   - reads a session-stored `schoolId` (set at login/onboarding for the user's primary school);
   - falls back to `SchoolDAO.findDefault()` when absent;
   - stores it in `TenantContext` (thread-local) for the request lifetime.
4. `AuthenticationFilter` — checks session / JWT token; guards `/*` app URLs.
5. `RoleAuthorizationFilter` — checks the user's (session / role + membership) role against the requested path and role requirements.

A request flows: **filter chain → servlet → services → DAOs → DB**, with `TenantContext`
providing `school_id` to every query/save.

---

## 6. Security Architecture

- **Authentication**: HTTP session + session-bound `User`; optional JWT bearer flow for API calls.
- **Authorization**:
  - `RoleAuthorizationFilter` maps paths to required roles.
  - `SecurityUtil.requireAdmin` / `requireSuperAdmin` enforce granular per-servlet checks.
  - Role held by the user AND validated against their `SchoolMembership` for the current school.
- **CSRF**: session-bound CSRF token; mutating servlets require header `X-CSRF-Token` (or `csrfToken`) to match. Checked in `SecurityUtil.csrfValid(...)` and in filters.
- **Input sanitization**: `InputSanitizationFilter` strips/escapes dangerous parameter content.
- **Password handling**: bcrypt hashing (`PasswordUtils`); forgot/reset password flows with OTP (`VerifyResetOtpServlet`) and email delivery (`EmailService`).
- **Bot protection**: `CaptchaServlet` serves a CAPTCHA on login/signup (`index.jsp`).

---

## 7. Multi-Tenancy Design (Phase 1)

### Shared-Schema, Row-Level Tenant Isolation
- Every tenant table (`classes`, `subjects`, `assignments`, `submissions`, `announcements`,
  `notifications`, `teacher_classes`, `student_classes`, `parent_children`, `school_members`)
  gains a `school_id` column.
- **Schools** table (`id`, `name`, `slug`, `domain`, `plan`, `active`) is the tenant registry.
- **School_members** (`school_id`, `user_id`, `role`) maps users to schools with a per-school role.
- `User` keeps a global identity (unique email) plus a global `role` (one of
  `STUDENT/TEACHER/ADMIN/SUPER_ADMIN`).

### Tenant Runtime
- `TenantContext` + `TenantFilter`: resolve and thread the active school per request.
- DAO `save()` **always** stamps `school_id = TenantContext.requireSchoolId()`, guaranteeing a
  row can never be written orphaned.
- Reads are school-scoped (e.g. `findBySchoolId`, memberships resolved via `TenantContext`).
- Cross-cutting features are tenant-aware:
  - `AssignmentWebSocket` → `broadcastToSchool(schoolId, ...)` (not global).
  - `NotificationService` → scoped by school.
  - Announcement fan-out → only to members of the target school.

### Migration (`db/migrations/V1__multi_tenancy.sql`)
Idempotently applied (via `psql -f`) — adds `school_id` columns (NOT NULL), unique
per-school constraints, seed membership backfill, and a `SUPER_ADMIN` role. Applied against
`furnitdb` (verified: 2 schools `legacy`/`demo`, 10 memberships, all tenant tables scoped).

---

## 8. Role-Based Surfaces

| Role | Primary URL | Contents |
|---|---|---|
| STUDENT | `/student/dashboard` | assignments list, my submissions, submit, grades, profile |
| TEACHER | `/teacher/dashboard` | classes, students, assignments, grade submissions, announcements |
| ADMIN | `/admin/dashboard` | school management: classes, students, teachers, parents, subjects, announcements, assignment seeding |
| SUPER_ADMIN | `/super/dashboard` | platform School CRUD + plan management (`SchoolApiServlet` `/api/super/schools/*`, `SuperAdminServlet`) |

---

## 9. Deployment & Runtime

- **Build**: `.\mvnw.cmd -DskipTests package` produces `target/igishyitsi.war`.
- **Deploy**: copy WAR to `C:\apache-tomcat-10.1.50\webapps\igishyitsi.war`
  (context `/igishyitsi`); Tomcat auto-deploys.
- **DB**: `furnitdb` PostgreSQL, connection configured via `ConfigUtil`; schema managed by
  both Hibernate (`hbm2ddl=update`) and the `db/migrations` psql scripts.
- **Seed/demo**: `SeedDataServlet` (`/admin/seed`) populates demo school, users, memberships.
- **Logs**: `C:\apache-tomcat-10.1.50\logs\catalina.*.log`, `localhost.*.log`.

---

## 10. Key Design Decisions / Tradeoffs

- **Java + Jakarta Servlet (no framework)** — lightweight, direct control, fits the app size;
  servlets/filters/JSP provide the classic MVC.
- **Shared-schema multi-tenancy over DB-per-tenant** — single deployment/DB, lower ops cost.
  Paid via disciplined `TenantContext` usage; deviations (e.g. any unscoped `findAll` for
  cross-tenant admin reads) are known Phase 5 follow-ups.
- **Row-level `school_id` + per-school unique constraints** — prevents accidental cross-school
  data mixing at the DB level.
- **Server-rendered JSP + targeted JSON APIs** — pages for humans, JSON endpoints for SPA-like
  admin/super-admin interactions with explicit CSRF header handling.
- **WebSocket for real-time notifications** with **school-scoped broadcast** to avoid leaking
  data across tenants.

---

## 11. Known Gaps / Next Steps (Phase 5)

- School-scope the remaining cross-tenant admin reads (`AdminDash` + `/api/admin/*` still use
  global `findAll()`).
- Send `X-CSRF-Token` header properly in the school-admin/admin JS so all admin POSTs pass CSRF
  under session tokens.
- Consider an automated migration-runner/check on startup so `V1__multi_tenancy.sql` is never
  skipped on a fresh deploy.
