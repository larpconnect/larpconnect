SET search_path = njall_users;

-- Entities Common Table Inheritance Root Table
CREATE TABLE IF NOT EXISTS njall_users.entities (
    id UUID NOT NULL DEFAULT uuidv7(),
    tenant_id UUID NOT NULL REFERENCES njall_users.studios(id),
    entity_type VARCHAR NOT NULL,
    summary VARCHAR NULL,
    created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMPTZ NULL DEFAULT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_entities_global_id UNIQUE (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_entities_active
    ON njall_users.entities (tenant_id DESC, id DESC)
    INCLUDE (entity_type)
    WHERE deleted_on IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_entities_type_lookup
    ON njall_users.entities (tenant_id DESC, entity_type ASC, id DESC)
    WHERE deleted_on IS NULL;

ALTER TABLE njall_users.entities ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_entities ON njall_users.entities;
CREATE POLICY rls_entities ON njall_users.entities
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_entities_admin ON njall_users.entities;
CREATE POLICY rls_entities_admin ON njall_users.entities
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Links Subtype Table
CREATE TABLE IF NOT EXISTS njall_users.links (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL DEFAULT uuidv7(),
    link_type VARCHAR(128) NOT NULL,
    url VARCHAR NOT NULL,
    media_type VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_link_id UNIQUE (id),
    CONSTRAINT fk_links_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE
);

ALTER TABLE njall_users.links ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_links ON njall_users.links;
CREATE POLICY rls_links ON njall_users.links
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_links_admin ON njall_users.links;
CREATE POLICY rls_links_admin ON njall_users.links
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.entities TO njall_users;
GRANT ALL ON njall_users.entities TO njall_admin;

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.links TO njall_users;
GRANT ALL ON njall_users.links TO njall_admin;
