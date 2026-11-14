SET search_path = njall_users, public;

-- Table for durable 8-day deduplication of received ActivityPub/FeatherPub activities
CREATE TABLE IF NOT EXISTS njall_users.inbox_activities (
    tenant_id UUID NOT NULL,
    target_actor_id UUID NOT NULL,
    activity_id VARCHAR NOT NULL,
    received_on TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    expires_on TIMESTAMPTZ NOT NULL DEFAULT (clock_timestamp() + INTERVAL '8 days'),

    PRIMARY KEY (tenant_id, target_actor_id, activity_id),
    CONSTRAINT fk_inbox_activities_actors FOREIGN KEY (tenant_id, target_actor_id)
        REFERENCES njall_users.actors (tenant_id, id) ON DELETE CASCADE
);

-- Index for opportunistic pruning of expired deduplication records
CREATE INDEX IF NOT EXISTS idx_inbox_activities_expires
    ON njall_users.inbox_activities (tenant_id, expires_on);

-- Row-Level Security
ALTER TABLE njall_users.inbox_activities ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_inbox_activities ON njall_users.inbox_activities;
CREATE POLICY rls_inbox_activities ON njall_users.inbox_activities
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_inbox_activities_admin ON njall_users.inbox_activities;
CREATE POLICY rls_inbox_activities_admin ON njall_users.inbox_activities
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.inbox_activities TO njall_users;
GRANT ALL ON njall_users.inbox_activities TO njall_admin;
