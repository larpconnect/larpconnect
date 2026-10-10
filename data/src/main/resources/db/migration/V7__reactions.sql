SET search_path = njall_users, public;

-- Reactions Subordinate Table (Value-type records attached to njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.reactions (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL DEFAULT uuidv7(),
    target_id UUID NOT NULL,
    link_id UUID NULL,
    reaction_type VARCHAR(128) NOT NULL,
    created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_reaction_id UNIQUE (id),
    CONSTRAINT fk_reactions_entities FOREIGN KEY (tenant_id, target_id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_links FOREIGN KEY (tenant_id, link_id)
        REFERENCES njall_users.links (tenant_id, id) ON DELETE SET NULL
);

-- Write-path covering index for tenant and target lookups
CREATE INDEX IF NOT EXISTS idx_reactions_target_count
    ON njall_users.reactions (tenant_id DESC, target_id DESC)
    INCLUDE (reaction_type, link_id);

-- Row-Level Security
ALTER TABLE njall_users.reactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_reactions ON njall_users.reactions;
CREATE POLICY rls_reactions ON njall_users.reactions
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_reactions_admin ON njall_users.reactions;
CREATE POLICY rls_reactions_admin ON njall_users.reactions
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.reactions TO njall_users;
GRANT ALL ON njall_users.reactions TO njall_admin;

-- Materialized View for Pre-Aggregated Reaction Counts (Cached Aggregations)
DROP MATERIALIZED VIEW IF EXISTS njall_users.reaction_counts;
CREATE MATERIALIZED VIEW njall_users.reaction_counts AS
SELECT
    R.tenant_id,
    R.target_id,
    R.reaction_type,
    R.link_id,
    L.url,
    L.media_type,
    L.link_type,
    COUNT(*) AS count
FROM njall_users.reactions R
INNER JOIN njall_users.entities E ON (R.tenant_id = E.tenant_id AND R.target_id = E.id)
LEFT JOIN njall_users.links L ON (R.tenant_id = L.tenant_id AND R.link_id = L.id)
WHERE E.deleted_on IS NULL
GROUP BY R.tenant_id, R.target_id, R.reaction_type, R.link_id, L.url, L.media_type, L.link_type;

-- Unique index to support non-blocking REFRESH MATERIALIZED VIEW CONCURRENTLY
CREATE UNIQUE INDEX IF NOT EXISTS unq_reaction_counts_bucket
    ON njall_users.reaction_counts (tenant_id, target_id, reaction_type, link_id)
    NULLS NOT DISTINCT;

-- Read indices on the materialized view
CREATE INDEX IF NOT EXISTS idx_reaction_counts_target
    ON njall_users.reaction_counts (tenant_id DESC, target_id DESC);

CREATE INDEX IF NOT EXISTS idx_reaction_counts_popular
    ON njall_users.reaction_counts (reaction_type, count DESC);

-- Permissions on Materialized View
GRANT SELECT ON njall_users.reaction_counts TO njall_users;
GRANT ALL ON njall_users.reaction_counts TO njall_admin;
