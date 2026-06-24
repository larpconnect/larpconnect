CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Define uuidv7 generator function if not exists
CREATE OR REPLACE FUNCTION uuidv7()
RETURNS uuid AS $$
DECLARE
    timestamp_ms bigint;
    timestamp_hex text;
    random_hex text;
    uuid_string text;
BEGIN
    timestamp_ms := floor(extract(epoch from clock_timestamp()) * 1000)::bigint;
    timestamp_hex := lpad(to_hex(timestamp_ms), 12, '0');
    random_hex := lpad(to_hex(floor(random() * 4503599627370496)::bigint), 12, '0') 
                  || lpad(to_hex(floor(random() * 4503599627370496)::bigint), 4, '0');
    uuid_string := substring(timestamp_hex from 1 for 8) || '-' ||
                   substring(timestamp_hex from 9 for 4) || '-' ||
                   '7' || substring(random_hex from 2 for 3) || '-' ||
                   to_hex((8 + floor(random() * 4)::int)) || substring(random_hex from 6 for 3) || '-' ||
                   substring(random_hex from 9 for 12);
    RETURN uuid_string::uuid;
END;
$$ LANGUAGE plpgsql;

-- Define account lifecycle states for platform operators
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tstatus') THEN
        CREATE TYPE tstatus AS ENUM ('ACTIVE', 'DISABLED', 'DELETED');
    END IF;
END
$$;

-- Platform-wide administrators who manage the LarpConnect installation itself.
-- Identity/Credentials are decoupled for downstream RLS/Auth integration.
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    username VARCHAR NOT NULL,
    status tstatus NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_users_username ON admin_users (username);

-- Platform-level roles for system operations (e.g., 'SUPER_ADMIN', 'BILLING')
CREATE TABLE IF NOT EXISTS admin_roles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    role_name VARCHAR NOT NULL
);

-- Access assignments matching the tenant table design conventions
CREATE TABLE IF NOT EXISTS admin_role_assignments (
    admin_user_id UUID NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES admin_roles(id) ON DELETE CASCADE,
    CONSTRAINT admin_role_assignments_pkey PRIMARY KEY (admin_user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_admin_role_assign_user ON admin_role_assignments (admin_user_id DESC);

-- The master routing catalog. 
-- Maps a studio to its obfuscated base32 physical database schema location.
CREATE TABLE IF NOT EXISTS studios (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR NOT NULL,
    schema_name VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL DEFAULT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_studios_routing ON studios (id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_studios_schema ON studios (schema_name);

-- Administrative operational metadata sync triggers
CREATE OR REPLACE FUNCTION sync_admin_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_scale_admin_users 
BEFORE UPDATE ON admin_users FOR EACH ROW EXECUTE FUNCTION sync_admin_timestamp();

CREATE TRIGGER trigger_scale_studios 
BEFORE UPDATE ON studios FOR EACH ROW EXECUTE FUNCTION sync_admin_timestamp();
