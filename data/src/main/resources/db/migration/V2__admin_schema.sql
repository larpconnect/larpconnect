SET search_path = njall_admin;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid WHERE t.typname = 'tstatus' AND n.nspname = 'njall_admin') THEN
        CREATE TYPE njall_admin.tstatus AS ENUM ('ACTIVE', 'DISABLED', 'DELETED');
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS njall_admin.admin_users (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    username VARCHAR NOT NULL,
    status njall_admin.tstatus NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_users_username ON njall_admin.admin_users (username);

CREATE TABLE IF NOT EXISTS njall_admin.admin_roles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    role_name VARCHAR NOT NULL,
    CONSTRAINT check_admin_roles_role_name CHECK (role_name ~ '^[a-z][a-z0-9_]*$')
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_roles_role_name ON njall_admin.admin_roles (role_name);

CREATE TABLE IF NOT EXISTS njall_admin.admin_role_assignments (
    admin_user_id UUID NOT NULL REFERENCES njall_admin.admin_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES njall_admin.admin_roles(id) ON DELETE CASCADE,
    CONSTRAINT admin_role_assignments_pkey PRIMARY KEY (admin_user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_admin_role_assign_user ON njall_admin.admin_role_assignments (admin_user_id DESC);
CREATE INDEX IF NOT EXISTS idx_admin_role_assign_role ON njall_admin.admin_role_assignments (role_id);

CREATE TABLE IF NOT EXISTS njall_admin.studios_lookup (
    tenant_id UUID PRIMARY KEY DEFAULT uuidv7(),
    studio_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    alias VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL DEFAULT NULL,
    CONSTRAINT check_studios_lookup_alias CHECK (alias ~ '^[a-z][a-z0-9_]*$')
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_studios_routing ON njall_admin.studios_lookup (tenant_id DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_studios_schema ON njall_admin.studios_lookup USING hash (studio_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_studios_alias ON njall_admin.studios_lookup (alias) WHERE deleted_at IS NULL;

ALTER TABLE njall_admin.studios_lookup ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_studios_lookup ON njall_admin.studios_lookup;
CREATE POLICY rls_studios_lookup ON njall_admin.studios_lookup
    FOR ALL TO njall_users USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_studios_lookup_admin ON njall_admin.studios_lookup;
CREATE POLICY rls_studios_lookup_admin ON njall_admin.studios_lookup
    FOR ALL TO njall_admin USING (true) WITH CHECK (true);

GRANT USAGE ON SCHEMA njall_admin TO njall_users;
GRANT SELECT ON njall_admin.studios_lookup TO njall_users;

CREATE OR REPLACE FUNCTION njall_admin.sync_admin_timestamp() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_scale_admin_users ON njall_admin.admin_users;
CREATE TRIGGER trigger_scale_admin_users
    BEFORE UPDATE ON njall_admin.admin_users
    FOR EACH ROW EXECUTE FUNCTION njall_admin.sync_admin_timestamp();

DROP TRIGGER IF EXISTS trigger_scale_studios ON njall_admin.studios_lookup;
CREATE TRIGGER trigger_scale_studios
    BEFORE UPDATE ON njall_admin.studios_lookup
    FOR EACH ROW EXECUTE FUNCTION njall_admin.sync_admin_timestamp();
