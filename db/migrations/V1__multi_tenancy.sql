-- =============================================================================
-- V1__multi_tenancy.sql
-- Adds multi-tenant (school) support to an existing Igishyitsi database.
--
-- APPLY THIS MIGRATION BEFORE STARTING THE UPDATED APP on any database that
-- already has data. On a fresh database the app can create the schema itself
-- via hibernate.hbm2ddl.auto=update and this script is not required.
--
-- What it does:
--   1. Creates the `schools` and `school_members` tables.
--   2. Creates a "Legacy School" tenant if none exists.
--   3. Adds + backfills + NOT NULLs the `school_id` column on every
--      tenant-scoped table, assigning existing rows to the Legacy School.
--   4. Backfills a school membership for every existing user (using their
--      current global role).
--   5. Replaces global unique constraints on classes.name / subjects.code with
--      per-school unique constraints, and scopes teacher_classes /
--      parent_children uniqueness by school.
--
-- Idempotent: safe to run more than once.
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Schools table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schools (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    domain      VARCHAR(255),
    plan        VARCHAR(50)  NOT NULL DEFAULT 'FREE',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- 2. School memberships (user <-> school, per-school role)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS school_members (
    id          BIGSERIAL PRIMARY KEY,
    school_id   BIGINT NOT NULL REFERENCES schools(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    role        VARCHAR(255) NOT NULL,
    joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_school_members_school_user UNIQUE (school_id, user_id)
);

-- ---------------------------------------------------------------------------
-- 3. Legacy tenant that absorbs all pre-existing rows
-- ---------------------------------------------------------------------------
INSERT INTO schools (name, slug, plan, active, created_at)
SELECT 'Legacy School', 'legacy', 'FREE', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE slug = 'legacy');

-- ---------------------------------------------------------------------------
-- 4. Add, backfill and constrain school_id on every tenant table
-- ---------------------------------------------------------------------------

-- assignments
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE assignments SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE assignments ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_assignments_school ON assignments (school_id);

-- submissions
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE submissions SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE submissions ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_submissions_school ON submissions (school_id);

-- classes
ALTER TABLE classes ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE classes SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE classes ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_classes_school ON classes (school_id);

-- subjects
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE subjects SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE subjects ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_subjects_school ON subjects (school_id);

-- student_classes
ALTER TABLE student_classes ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE student_classes SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE student_classes ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_student_classes_school ON student_classes (school_id);

-- teacher_classes
ALTER TABLE teacher_classes ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE teacher_classes SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE teacher_classes ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_teacher_classes_school ON teacher_classes (school_id);

-- parent_children
ALTER TABLE parent_children ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE parent_children SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE parent_children ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_parent_children_school ON parent_children (school_id);

-- announcements
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE announcements SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE announcements ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_announcements_school ON announcements (school_id);

-- notifications
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS school_id BIGINT;
UPDATE notifications SET school_id = (SELECT id FROM schools WHERE slug = 'legacy') WHERE school_id IS NULL;
ALTER TABLE notifications ALTER COLUMN school_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_notifications_school ON notifications (school_id);

-- ---------------------------------------------------------------------------
-- 5. Backfill school memberships for existing users (provisional: each user
--    inherits their current global role within the legacy school)
-- ---------------------------------------------------------------------------
-- Ensure the unique constraint backing ON CONFLICT exists even when the
-- school_members table was pre-created by Hibernate.
ALTER TABLE school_members DROP CONSTRAINT IF EXISTS uk_school_members_school_user;
ALTER TABLE school_members ADD CONSTRAINT uk_school_members_school_user UNIQUE (school_id, user_id);

INSERT INTO school_members (school_id, user_id, role, joined_at)
SELECT s.id, u.id, u.role, NOW()
FROM users u
JOIN schools s ON s.slug = 'legacy'
ON CONFLICT (school_id, user_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Replace global unique constraints with per-school ones
-- ---------------------------------------------------------------------------
ALTER TABLE classes DROP CONSTRAINT IF EXISTS classes_name_key;
ALTER TABLE classes DROP CONSTRAINT IF EXISTS uk_classes_name;
ALTER TABLE classes DROP CONSTRAINT IF EXISTS uk_classes_school_name;
ALTER TABLE classes ADD CONSTRAINT uk_classes_school_name UNIQUE (school_id, name);

ALTER TABLE subjects DROP CONSTRAINT IF EXISTS subjects_code_key;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS uk_subjects_code;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS uk_subjects_school_code;
ALTER TABLE subjects ADD CONSTRAINT uk_subjects_school_code UNIQUE (school_id, code);

ALTER TABLE teacher_classes DROP CONSTRAINT IF EXISTS teacher_classes_teacher_id_class_name_key;
ALTER TABLE teacher_classes DROP CONSTRAINT IF EXISTS uk_teacher_classes_teacher_class;
ALTER TABLE teacher_classes DROP CONSTRAINT IF EXISTS uk_teacher_classes_school_teacher_class;
ALTER TABLE teacher_classes ADD CONSTRAINT uk_teacher_classes_school_teacher_class UNIQUE (school_id, teacher_id, class_name);

ALTER TABLE parent_children DROP CONSTRAINT IF EXISTS parent_children_parent_id_student_id_key;
ALTER TABLE parent_children DROP CONSTRAINT IF EXISTS uk_parent_children_parent_student;
ALTER TABLE parent_children DROP CONSTRAINT IF EXISTS uk_parent_children_school_parent_student;
ALTER TABLE parent_children ADD CONSTRAINT uk_parent_children_school_parent_student UNIQUE (school_id, parent_id, student_id);

ALTER TABLE school_members DROP CONSTRAINT IF EXISTS school_members_role_check;
ALTER TABLE school_members ADD CONSTRAINT school_members_role_check CHECK (role IN ('SUPER_ADMIN','ADMIN','STUDENT','TEACHER','PARENT'));

-- users.role must also accept the platform role (global role column)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('SUPER_ADMIN','ADMIN','STUDENT','TEACHER','PARENT'));

-- Tenant-scoped uniqueness: subject code and class name must be unique PER SCHOOL,
-- not platform-wide, otherwise one school blocks another school from reusing them.
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS subjects_code_key;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS uk_rg7x1lyii7kdyycw98d45vep5;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS uk_subjects_school_code;
ALTER TABLE subjects ADD CONSTRAINT uk_subjects_school_code UNIQUE (school_id, code);

ALTER TABLE classes DROP CONSTRAINT IF EXISTS classes_name_key;
ALTER TABLE classes DROP CONSTRAINT IF EXISTS uk_pgs3gcxax70h9jugbt24ugwcg;
ALTER TABLE classes DROP CONSTRAINT IF EXISTS uk_classes_school_name;
ALTER TABLE classes ADD CONSTRAINT uk_classes_school_name UNIQUE (school_id, name);

COMMIT;