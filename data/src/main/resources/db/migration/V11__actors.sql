SET search_path = njall_users, public;

-- Actors Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.actors (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    preferred_username VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_actors_id UNIQUE (id),
    CONSTRAINT fk_actors_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_actors_owner FOREIGN KEY (tenant_id, owner_id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE
);

-- Case-insensitive unique index per studio tenant (permanent reservation)
CREATE UNIQUE INDEX IF NOT EXISTS idx_actors_tenant_username
    ON njall_users.actors (tenant_id, lower(preferred_username));

-- Owner index supporting 1:N owner-to-actors lookups
CREATE INDEX IF NOT EXISTS idx_actors_owner
    ON njall_users.actors (tenant_id, owner_id DESC);

-- Row-Level Security for Actors
ALTER TABLE njall_users.actors ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_actors ON njall_users.actors;
CREATE POLICY rls_actors ON njall_users.actors
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_actors_admin ON njall_users.actors;
CREATE POLICY rls_actors_admin ON njall_users.actors
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Federated Actor Endpoints Table
CREATE TABLE IF NOT EXISTS njall_users.actor_endpoints (
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    name VARCHAR(31) NOT NULL,
    endpoint VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, actor_id, name),
    CONSTRAINT fk_actor_endpoints_actor FOREIGN KEY (tenant_id, actor_id)
        REFERENCES njall_users.actors (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT chk_actor_endpoint_name CHECK (name ~ '^[a-zA-Z0-9]+$')
);

-- Row-Level Security for Actor Endpoints
ALTER TABLE njall_users.actor_endpoints ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_actor_endpoints ON njall_users.actor_endpoints;
CREATE POLICY rls_actor_endpoints ON njall_users.actor_endpoints
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_actor_endpoints_admin ON njall_users.actor_endpoints;
CREATE POLICY rls_actor_endpoints_admin ON njall_users.actor_endpoints
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.actors TO njall_users;
GRANT ALL ON njall_users.actors TO njall_admin;

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.actor_endpoints TO njall_users;
GRANT ALL ON njall_users.actor_endpoints TO njall_admin;
