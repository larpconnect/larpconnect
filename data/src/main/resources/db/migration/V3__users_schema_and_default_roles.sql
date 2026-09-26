SET search_path = njall_users;

-- Utility function: reverse hostname labels (e.g. 'api.example.com' -> 'com.example.api')
CREATE OR REPLACE FUNCTION njall_users.reverse_hostname_labels(hostname TEXT)
RETURNS TEXT AS $$
DECLARE
    labels TEXT[];
    reversed TEXT[];
    i INT;
BEGIN
    IF hostname IS NULL THEN
        RETURN NULL;
    END IF;
    labels := string_to_array(lower(trim(hostname)), '.');
    reversed := '{}';
    FOR i IN REVERSE array_length(labels, 1)..1 LOOP
        reversed := array_append(reversed, labels[i]);
    END LOOP;
    RETURN array_to_string(reversed, '.');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Utility function: extract hostname from URI and reverse its labels
CREATE OR REPLACE FUNCTION njall_users.extract_reverse_hostname(uri_value TEXT)
RETURNS TEXT AS $$
DECLARE
    extracted_host TEXT;
BEGIN
    IF uri_value IS NULL THEN
        RETURN NULL;
    END IF;
    extracted_host := substring(uri_value from '^(?:[a-zA-Z][a-zA-Z0-9+.-]*://)?(?:[^@/]+@)?([^/:?#]+)');
    IF extracted_host IS NULL OR extracted_host = '' THEN
        RETURN NULL;
    END IF;
    RETURN njall_users.reverse_hostname_labels(extracted_host);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Global default studio roles
CREATE TABLE IF NOT EXISTS njall_users.default_studio_roles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR NOT NULL UNIQUE
);

ALTER TABLE njall_users.default_studio_roles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_default_studio_roles_select ON njall_users.default_studio_roles;
CREATE POLICY rls_default_studio_roles_select ON njall_users.default_studio_roles
    FOR SELECT TO njall_users USING (true);

DROP POLICY IF EXISTS rls_default_studio_roles_admin ON njall_users.default_studio_roles;
CREATE POLICY rls_default_studio_roles_admin ON njall_users.default_studio_roles
    FOR ALL TO njall_admin USING (true) WITH CHECK (true);

-- User-space tenanted studios
CREATE TABLE IF NOT EXISTS njall_users.studios (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR NOT NULL
);

ALTER TABLE njall_users.studios ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_studios ON njall_users.studios;
CREATE POLICY rls_studios ON njall_users.studios
    FOR SELECT TO njall_users
    USING (id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_studios_admin ON njall_users.studios;
CREATE POLICY rls_studios_admin ON njall_users.studios
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Backfill existing studio lookup entries into njall_users.studios if any exist
INSERT INTO njall_users.studios (id, name)
SELECT tenant_id, alias FROM njall_admin.studios_lookup
ON CONFLICT (id) DO NOTHING;

-- Foreign key linking admin lookup to user studio entity
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_studios_studios_lookup'
    ) THEN
        ALTER TABLE njall_admin.studios_lookup
            ADD CONSTRAINT fk_studios_studios_lookup
            FOREIGN KEY (tenant_id) REFERENCES njall_users.studios (id);
    END IF;
END
$$;

-- Permissions
GRANT USAGE ON SCHEMA njall_users TO njall_admin, njall_users;
GRANT SELECT ON njall_users.default_studio_roles TO njall_users;
GRANT ALL ON njall_users.default_studio_roles TO njall_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.studios TO njall_users;
GRANT ALL ON njall_users.studios TO njall_admin;
GRANT EXECUTE ON FUNCTION njall_users.reverse_hostname_labels(TEXT) TO njall_admin, njall_users;
GRANT EXECUTE ON FUNCTION njall_users.extract_reverse_hostname(TEXT) TO njall_admin, njall_users;
