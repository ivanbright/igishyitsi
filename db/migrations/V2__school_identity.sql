-- School identity and contact details.
--
-- Tenants had no identity of their own: a signed-in user could not tell which
-- school they were in, and a school had nowhere to record its own address,
-- contact details, logo or description.
--
-- Every column is nullable and additive, so this is safe to run against an
-- existing database.

BEGIN;

ALTER TABLE schools ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS phone VARCHAR(255);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS description TEXT;

COMMIT;
