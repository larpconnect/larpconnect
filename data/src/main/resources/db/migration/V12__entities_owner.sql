SET search_path = njall_users, public;

-- Add owner column to entities table
ALTER TABLE njall_users.entities
    ADD COLUMN owner UUID NOT NULL;

-- Composite self-referencing foreign key ensuring owner entity belongs to the same tenant
ALTER TABLE njall_users.entities
    ADD CONSTRAINT fk_entities_owner FOREIGN KEY (tenant_id, owner)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE;

-- Index supporting tenant-scoped owner lookups
CREATE INDEX IF NOT EXISTS idx_entities_owner
    ON njall_users.entities (tenant_id, owner DESC);

-- Drop owner_id, constraint, and index from actors table
DROP INDEX IF EXISTS njall_users.idx_actors_owner;

ALTER TABLE njall_users.actors
    DROP CONSTRAINT IF EXISTS fk_actors_owner;

ALTER TABLE njall_users.actors
    DROP COLUMN IF EXISTS owner_id;

