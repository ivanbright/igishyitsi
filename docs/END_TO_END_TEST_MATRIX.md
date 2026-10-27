# END-TO-END TEST MATRIX — Igishyitsi

Follow the product spec's rule: **test by user journey, not by file**. Every row is a
multi-step journey that crosses filters → controller → DAO → Hibernate → PostgreSQL →
WebSocket and ends with the user seeing the product state change in UI.

Legend:
- `OK` = verified working against the live WAR + furnished DB in this session.
- `GAP` = journey is broken/blocked; fix before declaring done.
- Journey = **one test** (all steps must complete in order).

---

## JOURNEY 1 — SUPER ADMIN ONBOARDS A SCHOOL

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | `/super/dashboard` (SUPER_ADMIN) | school list (empty before seed) | OK |
| 2 | POST `/api/super/schools` action=create (name/slug/plan/domain), CSRF header | `School created` toast; school row appears in table | OK |
| 3 | POST action=update (rename/plan/domain) | toast; row reflects changed fields | OK |
| 4 | POST action=toggle | school status flips in table | OK |
| 5 | POST action=delete on an EMPTY school | `School deleted` toast; row removed | OK |
| 6 | POST action=delete on a school WITH members | `400 Cannot delete` — blocked | OK |

---

## JOURNEY 2 — SCHOOL ADMIN SETS UP THE SCHOOL (SEED PATH)

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | `/admin/seed` (once) — idempotent | admin upgraded to SUPER_ADMIN; demo school + memberships created | OK |
| 2 | log out → log in as super admin | `/super/dashboard` | OK |
| 3 | Create school (J1) | school-scoped rows exist | OK |
| 4 | `/admin/seed` again (idempotent re-run) | no duplicates (verified: 10 memberships) | OK |

---

## JOURNEY 3 — SCHOOL ADMIN MANAGES SCHOOL DATA

> **Cross-tenant precondition:** use TWO schools (A, B). Every read must be school-scoped.

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | `/admin/dashboard` (school A admin) | dashboard loads, lists school A classes/subjects/teachers/students | GAP* |
| 2 | Create class (admin) | new class appears; school B admin DOES NOT see it | GAP* |
| 3 | Create subject | appears in list | OK |
| 4 | Add teacher + student | appear in student/teacher lists | OK |
| 5 | Allocate student→class | student visible in class roster | OK |
| 6 | Assign teacher→class/subject | teacher sees class in teacher dashboard | OK |
| 7 | Create announcement (school A) | only school A members receive | OK |

\* `AdminDash`/`/api/admin/*` still calls **global** `findAll()` reads in places — the
school-scoping gap (P0). The dashboard **page** loads but the read chain leaks tenants.

---

## JOURNEY 4 — ASSIGNMENT LIFECYCLE (TEACHER→STUDENT→GRADE)

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | Teacher creates assignment (class) | assignment appears in student's assignment list (school+class scoped) | OK |
| 2 | Student opens assignment → submits (file/answer) | submission saved under correct school_id | OK |
| 3 | Teacher opens submission → grades + feedback | grade/feedback saved + student gets notification (WebSocket) | OK |
| 4 | Student sees grade/feedback on submission | grade shown on student surface | OK |

---

## JOURNEY 5 — ANNOUNCEMENT FAN-OUT + NOTIFICATIONS

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | Admin/teacher creates announcement (scoped to school) | fan-out → only school members | OK |
| 2 | Student/teacher gets in-app notification | toast via AssignmentWebSocket broadcastToSchool | OK |
| 3 | Notification read via `/api/notifications/*` | school-scoped; unread count updates | OK |
| 4 | Mark read/mark-all-read | unread count → 0 | OK |

---

## JOURNEY 6 — PARENT (what actually exists)

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | PARENT logs in | `/profile` (no parent dashboard) | OK |
| 2 | PARENT views notifications (`/api/notifications/*`) | scoped to school | OK |
| 3 | PARENT opens child's grades/submissions | **no parent dashboard/endpoints** → cannot | GAP (Phase 5) |

---

## JOURNEY 7 — CROSS-TENANT ISOLATION (the P0 check)

| Step | Action | Wait for | Status |
| ---- | ------ | -------- | ------ |
| 1 | Create school B; log in school B admin | `/admin/dashboard` (school B) | OK |
| 2 | School B admin reads `/admin/dashboard` | must show ONLY school B data | GAP* |
| 3 | School B POSTs any admin action | CSRF token required (`X-CSRF-Token`) | GAP** |
| 4 | WebSocket message in school A | school B does NOT receive (broadcastToSchool) | OK |
| 5 | Notification in school A | school B unread count unchanged | OK |

\* global findAll reads leak cross-tenant (§7 of PRODUCT_RECONSTRUCTION.md).
\** admin JS must send X-CSRF-Token header; currently missing for some admin POSTs.
