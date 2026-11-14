SET search_path = njall_users, public;

-- Event classification enum
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'tevent_type' AND n.nspname = 'njall_users'
    ) THEN
        CREATE TYPE njall_users.tevent_type AS ENUM (
            'EVENT_TYPE_UNSPECIFIED',
            'GAME',
            'WORKSHOP',
            'CONFERENCE',
            'MEETUP'
        );
    END IF;
END
$$;

GRANT USAGE ON TYPE njall_users.tevent_type TO njall_users;
GRANT USAGE ON TYPE njall_users.tevent_type TO njall_admin;

ALTER TABLE njall_users.events
    ADD COLUMN IF NOT EXISTS event_type njall_users.tevent_type
        NOT NULL DEFAULT 'EVENT_TYPE_UNSPECIFIED';

-- Systems Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.systems (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    name VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_systems_id UNIQUE (id),
    CONSTRAINT fk_systems_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_systems_tenant_name
    ON njall_users.systems (tenant_id, lower(name));

ALTER TABLE njall_users.systems ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_systems ON njall_users.systems;
CREATE POLICY rls_systems ON njall_users.systems
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_systems_admin ON njall_users.systems;
CREATE POLICY rls_systems_admin ON njall_users.systems
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.systems TO njall_users;
GRANT ALL ON njall_users.systems TO njall_admin;

-- Campaigns Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.campaigns (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    system_id UUID NULL,
    title VARCHAR NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_campaigns_id UNIQUE (id),
    CONSTRAINT fk_campaigns_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_campaigns_system FOREIGN KEY (tenant_id, system_id)
        REFERENCES njall_users.systems (tenant_id, id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_campaigns_title
    ON njall_users.campaigns (tenant_id, title);

CREATE INDEX IF NOT EXISTS idx_campaigns_system
    ON njall_users.campaigns (tenant_id, system_id);

ALTER TABLE njall_users.campaigns ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_campaigns ON njall_users.campaigns;
CREATE POLICY rls_campaigns ON njall_users.campaigns
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_campaigns_admin ON njall_users.campaigns;
CREATE POLICY rls_campaigns_admin ON njall_users.campaigns
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.campaigns TO njall_users;
GRANT ALL ON njall_users.campaigns TO njall_admin;

-- Games Subtype Table (2nd-level Common Table Inheritance referencing njall_users.events)
CREATE TABLE IF NOT EXISTS njall_users.games (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL,
    system_id UUID NOT NULL,
    campaign_id UUID NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_games_id UNIQUE (id),
    CONSTRAINT fk_games_events FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.events (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_games_system FOREIGN KEY (tenant_id, system_id)
        REFERENCES njall_users.systems (tenant_id, id),
    CONSTRAINT fk_games_campaign FOREIGN KEY (tenant_id, campaign_id)
        REFERENCES njall_users.campaigns (tenant_id, id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_games_system
    ON njall_users.games (tenant_id, system_id);

CREATE INDEX IF NOT EXISTS idx_games_campaign
    ON njall_users.games (tenant_id, campaign_id);

ALTER TABLE njall_users.games ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_games ON njall_users.games;
CREATE POLICY rls_games ON njall_users.games
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_games_admin ON njall_users.games;
CREATE POLICY rls_games_admin ON njall_users.games
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.games TO njall_users;
GRANT ALL ON njall_users.games TO njall_admin;
