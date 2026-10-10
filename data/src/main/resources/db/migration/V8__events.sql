SET search_path = njall_users, public;

-- Events Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.events (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    location_id UUID NULL,
    title VARCHAR NOT NULL,
    start_time TIMESTAMPTZ NULL,
    end_time TIMESTAMPTZ NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_event_id UNIQUE (id),
    CONSTRAINT fk_events_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_events_locations FOREIGN KEY (tenant_id, location_id)
        REFERENCES njall_users.locations (tenant_id, id) ON DELETE SET NULL,
    CONSTRAINT chk_events_time_order CHECK (
        end_time IS NULL OR start_time IS NULL OR end_time >= start_time
    )
);

-- Foreign key lookup index on location
CREATE INDEX IF NOT EXISTS idx_events_location
    ON njall_users.events (tenant_id, location_id);

-- Chronological timeline index for queries
CREATE INDEX IF NOT EXISTS idx_events_time
    ON njall_users.events (tenant_id DESC, start_time ASC, end_time ASC);

-- Row-Level Security
ALTER TABLE njall_users.events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_events ON njall_users.events;
CREATE POLICY rls_events ON njall_users.events
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_events_admin ON njall_users.events;
CREATE POLICY rls_events_admin ON njall_users.events
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.events TO njall_users;
GRANT ALL ON njall_users.events TO njall_admin;
