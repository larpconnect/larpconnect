SET search_path = njall_users, public;

-- Rename owner column to generator on entities table
ALTER TABLE njall_users.entities
    RENAME COLUMN owner TO generator;

-- Rename composite self-referencing foreign key constraint
ALTER TABLE njall_users.entities
    RENAME CONSTRAINT fk_entities_owner TO fk_entities_generator;

-- Replace tenant-scoped owner index with generator index
DROP INDEX IF EXISTS njall_users.idx_entities_owner;

CREATE INDEX IF NOT EXISTS idx_entities_generator
    ON njall_users.entities (tenant_id, generator DESC);
