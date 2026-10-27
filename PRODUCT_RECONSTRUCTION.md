# IGISHYITSI — PRODUCT RECONSTRUCTION & END-TO-END IMPLEMENTATION

You are working on **Igishyitsi**, an existing Java/Jakarta EE school-management application that is being transformed from an assignment-submission prototype into a **real multi-tenant SaaS platform for schools**.

The current codebase contains substantial backend, database, authentication, role, multi-tenancy, JSP, API, and WebSocket work.

However, the application currently feels incomplete and disconnected:

* UI flows are vague.
* Pages exist without clear relationships.
* Some backend capabilities are not properly exposed in the frontend.
* Some frontend actions do not lead to complete backend workflows.
* Navigation does not consistently represent the actual product domain.
* Different roles do not feel like participants in the same school system.
* Some functionality appears to have been implemented independently rather than as part of an end-to-end workflow.
* The application does not yet feel like a coherent SaaS product.
* Do NOT solve this by simply creating more pages or making existing pages prettier.

Your task is to **reconstruct the actual product from the existing codebase and then turn it into one coherent, usable, connected SaaS.**

---

# 1. CORE RULE

## DO NOT GUESS.

Before changing substantial code, inspect the existing system.

You must understand:

* existing database schema
* JPA entities
* relationships
* DAOs
* services
* servlets/controllers
* filters
* authentication
* authorization
* tenant isolation
* APIs
* JSPs
* JavaScript
* WebSockets
* navigation
* existing CRUD operations
* existing workflows
* existing migrations
* existing seed/demo data

If something already exists, **reuse and connect it** instead of creating a duplicate implementation.

If something is missing, identify it explicitly before implementing it.

If the intended behavior cannot be determined from the existing system, do not silently invent a complicated behavior. Document the ambiguity and choose the smallest behavior consistent with the existing product.

---

# 2. FIRST: RECONSTRUCT THE EXISTING PRODUCT

Do NOT immediately start coding.

Perform a complete codebase audit.

Create an internal implementation document:

`docs/PRODUCT_RECONSTRUCTION.md`

It must contain:

### A. What Igishyitsi currently is

Describe the actual product represented by the existing code.

### B. Current roles

Identify the actual roles and what each can currently do.

At minimum investigate:

* SUPER_ADMIN
* ADMIN
* TEACHER
* STUDENT
* PARENT, if actually implemented

Do not assume a role has functionality merely because an entity or enum exists.

### C. Domain entities

Map the actual entities:

* School
* User
* SchoolMembership
* ClassGroup
* TeacherClass
* StudentClass
* Subject
* Assignment
* Submission
* Announcement
* Notification
* ParentChild
* Person
* and anything else actually present

For every entity determine:

* who creates it
* who modifies it
* who can view it
* who owns it
* what it belongs to
* what depends on it
* what happens when it changes

### D. Existing workflows

Find actual workflows already implemented.

For example:

```text
Login
School onboarding
User creation
Class creation
Student allocation
Teacher allocation
Assignment creation
Assignment submission
Grading
Announcements
Notifications
Password reset
etc.
```

Do not merely list URLs.

Trace each workflow through:

```text
UI
→ HTTP request
→ filter
→ servlet
→ service
→ DAO
→ Hibernate
→ database
→ response
→ UI
```

### E. Broken/disconnected workflows

Identify:

* dead buttons
* dead links
* fake/mock data
* pages with no backend
* backend endpoints with no UI
* UI actions that hit the wrong endpoint
* incomplete CRUD
* inconsistent IDs
* stale state
* missing redirects
* missing refreshes
* missing error handling
* missing empty states
* missing loading states
* permission mismatches
* tenant-scope violations
* duplicate concepts
* inconsistent naming

---

# 3. BUILD THE PRODUCT DOMAIN MODEL

Before redesigning screens, define the actual relationship between the school's objects.

The core conceptual model should resemble:

```text
PLATFORM
│
├── Schools
│
└── School
     │
     ├── Users
     │    ├── Admins
     │    ├── Teachers
     │    ├── Students
     │    └── Parents
     │
     ├── Classes
     │    ├── Students
     │    └── Teachers
     │
     ├── Subjects
     │
     ├── Assignments
     │    └── Submissions
     │
     ├── Grades
     │
     ├── Announcements
     │
     └── Notifications
```

But do NOT blindly implement this diagram.

Derive the actual relationships from the codebase and database.

Document the final domain model in:

`docs/DOMAIN_MODEL.md`

Include a relationship diagram.

---

# 4. DEFINE THE PRODUCT AROUND REAL USER JOBS

The system must not be designed as a collection of dashboards.

Each role needs clear jobs.

## SUPER ADMIN

The platform operator should be able to:

* onboard schools
* view schools
* manage school status
* manage school-level platform information
* manage plans/subscriptions if the existing architecture supports them
* enter a school's context where appropriate and securely

Do not expose school data globally unless the role and workflow explicitly require it.

---

## SCHOOL ADMIN

The school administrator should be able to operate the school.

Typical responsibilities include:

```text
School setup
Users
Teachers
Students
Parents
Classes
Subjects
Teacher/class assignment
Student/class allocation
Announcements
Academic configuration
School-level oversight
```

The admin should not merely see tables.

Every major entity should support a useful lifecycle:

```text
List
→ View
→ Create
→ Edit
→ Deactivate/Delete where appropriate
→ Related records
→ Actions
```

---

## TEACHER

The teacher should be able to:

```text
View teaching classes
→ Open a class
→ View students
→ Manage assignments
→ Publish assignments
→ View submissions
→ Grade submissions
→ Give feedback
→ Communicate with relevant students
```

The teacher's experience should revolve around actual teaching work.

---

## STUDENT

The student should be able to:

```text
See enrolled classes
→ See subjects
→ See assignments
→ Open assignment
→ Submit work
→ Track submission
→ Receive grade/feedback
→ View academic information
→ Receive relevant announcements/notifications
```

---

## PARENT

Only implement parent workflows that are actually supported by the existing product model.

If parent functionality is incomplete, identify the missing pieces instead of pretending it is complete.

---

# 5. DEFINE END-TO-END GOLDEN WORKFLOWS

Create:

`docs/GOLDEN_WORKFLOWS.md`

At minimum define these workflows.

---

## WORKFLOW 1 — SCHOOL ONBOARDING

```text
SUPER_ADMIN
→ Create school
→ Configure school
→ Create/designate school administrator
→ Activate school
→ Admin logs in
→ Admin lands inside correct school context
```

Verify every step against the existing backend.

---

## WORKFLOW 2 — SCHOOL SETUP

```text
ADMIN
→ Create class
→ Create subject
→ Create/register teacher
→ Create/register student
→ Assign teacher to class/subject
→ Assign student to class
```

After every operation, verify that the resulting relationships are visible from the appropriate screens.

---

## WORKFLOW 3 — ASSIGNMENT LIFECYCLE

This is a critical workflow.

```text
TEACHER
→ Open class
→ Create assignment
→ Save draft
→ Edit draft
→ Publish
→ Students in that class can see it
→ Student opens assignment
→ Student submits
→ Teacher sees submission
→ Teacher opens submission
→ Teacher grades
→ Teacher provides feedback
→ Student sees grade/feedback
→ Relevant notification is generated
```

Do not consider this feature complete until this entire chain works.

---

## WORKFLOW 4 — ANNOUNCEMENT

```text
ADMIN/TEACHER
→ Create announcement
→ Select appropriate audience
→ Publish
→ Recipients see announcement
→ Notification is generated if appropriate
→ Recipient opens announcement
```

Audience must be scoped correctly.

---

## WORKFLOW 5 — STUDENT MANAGEMENT

```text
ADMIN
→ View students
→ Search/filter
→ Open student
→ View complete student details
→ See class relationships
→ See relevant academic relationships
→ Perform permitted actions
```

A "View" button must open a meaningful detail surface.

Do not create a table where clicking a user leads nowhere.

---

## WORKFLOW 6 — TEACHER MANAGEMENT

```text
ADMIN
→ View teachers
→ Open teacher
→ View details
→ View assigned classes/subjects
→ Manage permitted assignments
```

---

## WORKFLOW 7 — CLASS MANAGEMENT

```text
ADMIN
→ Open class
→ View class details
→ View students
→ View teachers
→ View subjects
→ View assignments
→ Perform relevant actions
```

The class should become a meaningful domain hub rather than just a database record.

---

# 6. DESIGN NAVIGATION FROM THE DOMAIN

Do not create navigation based on whatever JSP files currently exist.

Navigation must reflect what the role actually does.

For example, a teacher might have:

```text
Dashboard

Teaching
├── My Classes
├── Assignments
└── Submissions

Communication
├── Announcements
└── Notifications

Account
└── Profile
```

An admin might have:

```text
Dashboard

School
├── Classes
├── Subjects
└── Academic Structure

People
├── Students
├── Teachers
└── Parents

Communication
└── Announcements

Account
└── Profile
```

These are examples, NOT instructions to blindly copy.

Derive the final navigation from the actual domain and workflows.

Document it in:

`docs/NAVIGATION_MAP.md`

Every navigation item must point to a real working destination.

---

# 7. EVERY SCREEN MUST HAVE A PURPOSE

For every screen, define:

```text
Purpose
Who can access it
Entry points
Data displayed
Available actions
Backend endpoints used
Database entities involved
Success behavior
Error behavior
Empty state
Loading state
Permission behavior
Exit/navigation paths
```

No screen should exist simply because "the SaaS needs a page for X."

---

# 8. EVERY ACTION MUST BE REAL

This is a hard requirement.

Do not leave:

```text
TODO
Coming soon
Placeholder
Fake statistics
Hardcoded users
Hardcoded assignments
Fake notifications
Dummy charts
No-op buttons
Dead links
```

unless explicitly marked as development-only and documented.

For every button:

```text
CLICK
→ UI event
→ HTTP/API request
→ backend authorization
→ business logic
→ database mutation/read
→ response
→ UI state update
```

Verify this chain.

---

# 9. FRONTEND AND BACKEND MUST SHARE ONE CONTRACT

For every frontend feature determine:

```text
UI
↓
endpoint
↓
request schema
↓
authorization
↓
service
↓
DAO
↓
entity/database
↓
response schema
↓
UI state
```

Do not invent frontend data models that don't exist in the backend.

Do not create backend endpoints that duplicate existing endpoints.

Before creating a new endpoint, search the entire codebase for an existing implementation.

---

# 10. TENANT ISOLATION IS NON-NEGOTIABLE

This is a multi-tenant SaaS.

Every school-owned resource must be correctly scoped.

The expected conceptual rule is:

```text
CURRENT REQUEST
      ↓
CURRENT USER
      ↓
CURRENT SCHOOL
      ↓
TenantContext
      ↓
authorized query/mutation
      ↓
school-scoped data
```

Audit all:

* reads
* writes
* updates
* deletes
* searches
* joins
* APIs
* WebSocket broadcasts
* notifications
* admin operations

Pay particular attention to any:

```text
findAll()
```

or global query that can expose another school's data.

The existing architecture already identifies cross-tenant admin reads as a known gap. Fix them rather than building further functionality on top of them.

---

# 11. DO NOT OVER-ENGINEER

Do not introduce:

* microservices
* new frameworks
* unnecessary frontend rewrites
* unnecessary database changes
* unnecessary authentication systems
* unnecessary event buses
* unnecessary abstractions

unless the existing product genuinely requires them.

The goal is:

```text
coherent product
+
correct architecture
+
working workflows
```

not architectural complexity.

Preserve the current Java/Jakarta EE/Tomcat/JSP/Hibernate/PostgreSQL architecture unless there is a concrete reason to change it.

---

# 12. FIX THE EXISTING PRODUCT BEFORE ADDING NEW FEATURES

Prioritize in this order:

### P0 — Broken foundations

Fix:

* authentication
* authorization
* tenant isolation
* database relationships
* broken APIs
* broken CRUD
* session problems
* CSRF issues
* navigation failures

### P1 — Core school workflows

Make these completely functional:

```text
School
→ Users
→ Classes
→ Subjects
→ Teacher assignment
→ Student allocation
→ Assignments
→ Submissions
→ Grading
```

### P2 — Communication

```text
Announcements
Notifications
WebSockets
```

### P3 — Secondary functionality

Only after the core system is coherent.

---

# 13. USE REAL DATA END-TO-END

The application must work using actual persisted data.

For example:

```text
Create class
```

must actually result in:

```text
PostgreSQL
   ↓
class row
   ↓
class appears in admin UI
   ↓
teacher can be assigned
   ↓
student can be allocated
   ↓
teacher sees class
   ↓
student sees class
```

If creating an object doesn't affect the rest of the system, the feature is incomplete.

---

# 14. BUILD DETAIL PAGES, NOT JUST TABLES

For important entities:

```text
School
Student
Teacher
Parent
Class
Subject
Assignment
Submission
Announcement
```

a user should be able to:

```text
LIST
 ↓
OPEN
 ↓
DETAIL
 ↓
UNDERSTAND
 ↓
ACT
 ↓
RETURN
```

The detail page should become a useful domain surface.

For example:

```text
Student
────────────────────────
Name
Email
Status
Class

Academic
├── Classes
├── Subjects
├── Assignments
└── Grades

Actions
├── Edit
├── Change class
└── Deactivate
```

Only expose actions actually supported by the user's role.

---

# 15. HANDLE REAL APPLICATION STATES

Every important screen must account for:

```text
Loading
Empty
Success
Error
Unauthorized
Forbidden
Not Found
Validation failure
Network/server failure
```

Do not build only the happy path.

---

# 16. TEST BY USER JOURNEY, NOT BY FILE

Do not say:

> "AssignmentServlet works."

Instead test:

```text
Teacher logs in
→ opens class
→ creates assignment
→ publishes it
→ student logs in
→ sees assignment
→ opens it
→ submits
→ teacher sees submission
→ grades it
→ student sees grade
```

That entire sequence is one test.

Create an end-to-end checklist in:

`docs/END_TO_END_TEST_MATRIX.md`

---

# 17. CREATE A TRACEABILITY MATRIX

Create:

`docs/FEATURE_TRACEABILITY.md`

Use this structure:

| Feature           | Role    | UI | Endpoint | Service | DAO | Entity | DB | Tested |
| ----------------- | ------- | -- | -------- | ------- | --- | ------ | -- | ------ |
| Create Class      | ADMIN   | ✓  | ✓        | ✓       | ✓   | ✓      | ✓  | ✓      |
| Create Assignment | TEACHER | ✓  | ✓        | ✓       | ✓   | ✓      | ✓  | ✓      |
| Submit Assignment | STUDENT | ✓  | ✓        | ✓       | ✓   | ✓      | ✓  | ✓      |
| Grade Submission  | TEACHER | ✓  | ✓        | ✓       | ✓   | ✓      | ✓  | ✓      |

Do not mark something complete merely because a file exists.

Mark it complete only when the entire chain works.

---

# 18. WORK IN SMALL VERIFIED SLICES

Do NOT spend hours modifying dozens of unrelated files.

Work like this:

```text
1. Inspect
2. Identify one broken workflow
3. Trace it end-to-end
4. Fix backend
5. Fix frontend
6. Test workflow
7. Verify tenant isolation
8. Record completion
9. Move to next workflow
```

After each workflow, provide a short report:

```text
WORKFLOW:
Assignment lifecycle

FOUND:
...

CHANGED:
...

VERIFIED:
...

REMAINING:
...

FILES:
...
```

---

# 19. DO NOT CLAIM SUCCESS WITHOUT VERIFICATION

Never say:

```text
"Implemented"
```

because code was written.

Use:

```text
IMPLEMENTED
```

only when:

* backend exists
* frontend exists
* permissions work
* persistence works
* UI reflects persisted state
* errors are handled
* workflow was actually tested

If you cannot verify something, explicitly mark:

```text
UNVERIFIED
```

---

# 20. YOUR DEFINITION OF DONE

Igishyitsi is not considered complete because all JSPs exist.

It is complete when a real user can enter the system and perform meaningful work.

At minimum:

### Super Admin

```text
Create school
→ manage school
→ manage school status
```

### School Admin

```text
Configure school
→ create classes
→ create subjects
→ manage teachers
→ manage students
→ allocate relationships
→ publish announcements
```

### Teacher

```text
See classes
→ open class
→ see students
→ create assignment
→ publish
→ receive submissions
→ grade
→ provide feedback
```

### Student

```text
See classes
→ see assignments
→ submit
→ track submission
→ receive grade
→ read feedback
```

### Cross-role

```text
Admin creates data
       ↓
Teacher can use it
       ↓
Student can interact with it
       ↓
Teacher sees student's interaction
       ↓
Admin can oversee the resulting state
```

That last chain is what makes the application feel like **one system**.

---

# 21. FINAL IMPLEMENTATION RULE

Before writing new code, always ask:

> **"What user workflow does this code complete?"**

If there is no clear answer, do not build it yet.

Before creating a new screen:

> **"Where does the user enter this screen, what can they do there, and where do they go afterward?"**

Before creating an endpoint:

> **"Which UI action needs this endpoint, and what persisted state does it change?"**

Before creating an entity:

> **"What real-world concept does this represent, who owns it, and which workflows depend on it?"**

Before declaring a feature complete:

> **"Can a real user perform the entire workflow from beginning to end?"**

---

# START NOW

Do not immediately redesign the UI.

First inspect the existing codebase and produce:

```text
docs/PRODUCT_RECONSTRUCTION.md
docs/DOMAIN_MODEL.md
docs/GOLDEN_WORKFLOWS.md
docs/NAVIGATION_MAP.md
docs/FEATURE_TRACEABILITY.md
docs/END_TO_END_TEST_MATRIX.md
```

Then report:

1. What the current product actually is.
2. What is already genuinely functional.
3. What is partially functional.
4. What is disconnected.
5. What is duplicated.
6. What is broken.
7. What backend capabilities exist without UI.
8. What UI capabilities exist without backend support.
9. Where tenant isolation is unsafe.
10. The smallest sequence of implementation steps required to turn the existing application into one coherent SaaS.

**Do not start a large implementation until this reconstruction is complete.**

After reconstruction, implement the product **workflow-by-workflow**, beginning with the highest-value core school workflow, and verify each workflow end-to-end before moving to the next.

The objective is not to produce more code.

The objective is to make **Igishyitsi behave like one real, connected, multi-tenant school SaaS.**
