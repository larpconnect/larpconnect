SET search_path = njall_users, public;

-- Federated Hashtag Links Subtype Table (Common Table Inheritance referencing njall_users.links)
CREATE TABLE IF NOT EXISTS njall_users.hashtags (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL DEFAULT uuidv7(),
    tag VARCHAR(32) NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_hashtag_id UNIQUE (id),
    CONSTRAINT fk_hashtags_links FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.links (tenant_id, id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hashtag_tag_lookup
    ON njall_users.hashtags (tenant_id DESC, LOWER(tag));

ALTER TABLE njall_users.hashtags ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_hashtags ON njall_users.hashtags;
CREATE POLICY rls_hashtags ON njall_users.hashtags
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_hashtags_admin ON njall_users.hashtags;
CREATE POLICY rls_hashtags_admin ON njall_users.hashtags
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Entity-Hashtag Intersect Mapping
CREATE TABLE IF NOT EXISTS njall_users.hashtags_entity (
    id UUID NOT NULL DEFAULT uuidv7(),
    tenant_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    hashtag_id UUID NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_hashtags_entity_id UNIQUE (id),
    CONSTRAINT unq_hashtag_entity UNIQUE (tenant_id, entity_id, hashtag_id),
    CONSTRAINT fk_hashtags_entity_entities FOREIGN KEY (tenant_id, entity_id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_hashtags_entity_hashtags FOREIGN KEY (tenant_id, hashtag_id)
        REFERENCES njall_users.hashtags (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_hashtag_entity_lookup
    ON njall_users.hashtags_entity (tenant_id DESC, entity_id DESC)
    INCLUDE (hashtag_id);

ALTER TABLE njall_users.hashtags_entity ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_hashtags_entity ON njall_users.hashtags_entity;
CREATE POLICY rls_hashtags_entity ON njall_users.hashtags_entity
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_hashtags_entity_admin ON njall_users.hashtags_entity;
CREATE POLICY rls_hashtags_entity_admin ON njall_users.hashtags_entity
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.hashtags TO njall_users;
GRANT ALL ON njall_users.hashtags TO njall_admin;

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.hashtags_entity TO njall_users;
GRANT ALL ON njall_users.hashtags_entity TO njall_admin;
