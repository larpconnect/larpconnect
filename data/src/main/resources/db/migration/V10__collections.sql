SET search_path = njall_users, public;

-- Collections Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.collections (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    name VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_collections_id UNIQUE (id),
    CONSTRAINT fk_collections_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_collections_owner FOREIGN KEY (tenant_id, owner_id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT unq_collections_owner_name UNIQUE (tenant_id, owner_id, name)
);

CREATE INDEX IF NOT EXISTS idx_collections_owner
    ON njall_users.collections (tenant_id, owner_id DESC);

-- Row-Level Security for Collections
ALTER TABLE njall_users.collections ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_collections ON njall_users.collections;
CREATE POLICY rls_collections ON njall_users.collections
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_collections_admin ON njall_users.collections;
CREATE POLICY rls_collections_admin ON njall_users.collections
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Collection Items Junction Table
CREATE TABLE IF NOT EXISTS njall_users.collection_items (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    tenant_id UUID NOT NULL,
    collection_id UUID NOT NULL,
    ordinal INTEGER NOT NULL,
    added_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refers_to_tenant_id UUID NOT NULL,
    refers_to_id UUID NOT NULL,

    CONSTRAINT fk_collection_items_collection FOREIGN KEY (tenant_id, collection_id)
        REFERENCES njall_users.collections (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_items_entities FOREIGN KEY (refers_to_tenant_id, refers_to_id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT unq_collection_items_ref UNIQUE (tenant_id, collection_id, refers_to_id)
);

CREATE INDEX IF NOT EXISTS idx_collection_items_order
    ON njall_users.collection_items (tenant_id, collection_id, ordinal ASC);

-- Row-Level Security for Collection Items
ALTER TABLE njall_users.collection_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_collection_items ON njall_users.collection_items;
CREATE POLICY rls_collection_items ON njall_users.collection_items
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_collection_items_admin ON njall_users.collection_items;
CREATE POLICY rls_collection_items_admin ON njall_users.collection_items
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.collections TO njall_users;
GRANT ALL ON njall_users.collections TO njall_admin;

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.collection_items TO njall_users;
GRANT ALL ON njall_users.collection_items TO njall_admin;
