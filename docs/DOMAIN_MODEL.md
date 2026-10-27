# DOMAIN MODEL — Igishyitsi

> Verified against the live entity classes under `src/main/java/org/example/esubmission/model`
> and the applied `V1__multi_tenancy.sql`.

---

## Relationship diagram

```text
School 1 ──── * SchoolMembership * ──── 1 User          (roles: SUPER_ADMIN/ADMIN/TEACHER/STUDENT/PARENT)
School 1 ──── * ClassGroup (classes)
School 1 ──── * Subject
School 1 ──── * Announcement
School 1 ──── * Assignment
School 1 ──── * Submission
School 1 ──── * Notification

User 1 ──── * SchoolMembership * ──── 1 School         (per-school role; TenantContext)
User 1 ──── * StudentClass * ──── 1 ClassGroup          (student ↔ class)
User 1 ──── * TeacherClass * ──── 1 ClassGroup          (teacher ↔ class)
User 1 ──── * ParentChild * ──── 1 User                 (parent ↔ student)

ClassGroup 1 ──── * Assignment
ClassGroup 1 ──── * Subject (via TeacherClass)
Assignment 1 ──── * Submission
Submission 1 ──── 1 User (student) + 1 User (grader/feedback)
```

## Entities

### School
- `id`, `name`, `slug`, `domain`, `plan` (FREE/PLUS/PREMIUM), `active`
- Owns ALL tenant data through `school_id` on child tables.

### User
- `id`, `email` (global unique), `passwordHash`, `name`, `role` (global role), `phone`,
  `twoFactorEnabled`, `verified` flags.
- **Note**: line-level tenant member roles come from `SchoolMembership`, NOT `User.role`.

### SchoolMembership
- `id`, `schoolId`, `userId`, `role` (per-school), `joinedAt`
- Unique `(school_id, user_id)`. This is the tenant join. `TenantFilter` sets the active
  school from the logged-in user's membership.

### ClassGroup
- `id`, `schoolId`, `name`, `code`, `year`, (optional `teacherId` as class teacher)
- The `classes` table.

### Subject
- `id`, `schoolId`, `code`, `name`, `classGroupId` (optional), `teacherId` (optional).

### TeacherClass
- `id`, `schoolId`, `teacherId`, `classGroupId`, `subjectId`
- Assigns teacher → (class, subject).

### StudentClass
- `id`, `schoolId`, `studentId`, `classGroupId`
- Allocates student → class.

### Assignment
- `id`, `schoolId`, `classGroupId`, `subjectId` (optional), `teacherId`, `title`,
  `description`, `dueDate`, `published`.

### Submission
- `id`, `schoolId`, `assignmentId`, `studentId`, `content`, `filePath`, `grade`, `feedback`,
  `status` (SUBMITTED/GRADED/etc.), `submittedAt`.

### Announcement
- `id`, `schoolId`, `authorId`, `title`, `content`, `audience` (role-scoped), `createdAt`.

### Notification
- `id`, `schoolId`, `userId`, `type`, `title`, `message`, `read`, `createdAt`.
- Reads are school-scoped via `NotificationService` + `TenantContext`.

### ParentChild
- `id`, `schoolId`, `parentId`, `studentId`
- Joins parent ↔ student within the school.

## Tenant isolation rules

- Every tenant table has `school_id` NOT NULL (enforced by `V1__multi_tenancy.sql`).
- All DAOs scope reads/writes through `TenantContext.requireSchoolId()`.
- `school_members` unique constraint `(school_id, user_id)` prevents duplicate cross-school
  membership entries for the same user+school.
- CSRF: `X-CSRF-Token` header or `csrfToken` param, checked by `SecurityUtils`.
