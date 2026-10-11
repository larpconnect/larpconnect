SET search_path = njall_users, public;

-- Individuals Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.individuals (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    name VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_individuals_id UNIQUE (id),
    CONSTRAINT fk_individuals_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE
);

-- Row-Level Security
ALTER TABLE njall_users.individuals ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_individuals ON njall_users.individuals;
CREATE POLICY rls_individuals ON njall_users.individuals
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_individuals_admin ON njall_users.individuals;
CREATE POLICY rls_individuals_admin ON njall_users.individuals
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.individuals TO njall_users;
GRANT ALL ON njall_users.individuals TO njall_admin;
