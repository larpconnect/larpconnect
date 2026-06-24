-- Define uuidv7 generator function if not exists in the schema
CREATE OR REPLACE FUNCTION uuidv7()
RETURNS uuid AS $$
DECLARE
    timestamp_ms bigint;
    timestamp_hex text;
    random_hex text;
    uuid_string text;
BEGIN
    timestamp_ms := floor(extract(epoch from clock_timestamp()) * 1000)::bigint;
    timestamp_hex := lpad(to_hex(timestamp_ms), 12, '0');
    random_hex := lpad(to_hex(floor(random() * 4503599627370496)::bigint), 12, '0') 
                  || lpad(to_hex(floor(random() * 4503599627370496)::bigint), 4, '0');
    uuid_string := substring(timestamp_hex from 1 for 8) || '-' ||
                   substring(timestamp_hex from 9 for 4) || '-' ||
                   '7' || substring(random_hex from 2 for 3) || '-' ||
                   to_hex((8 + floor(random() * 4)::int)) || substring(random_hex from 6 for 3) || '-' ||
                   substring(random_hex from 9 for 12);
    RETURN uuid_string::uuid;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tcontact') THEN
        CREATE TYPE tcontact AS ENUM ('email', 'phone', 'address');
    END If;
END
$$;

-- Moved from admin: localized to track federated assets interacting with THIS studio
CREATE TABLE IF NOT EXISTS external_resources (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    external_uri VARCHAR NOT NULL,
    data JSONB NULL,
    last_refresh TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hostname VARCHAR GENERATED ALWAYS AS (substring(external_uri FROM '.*://([^/]*)')) STORED,
    CONSTRAINT external_resource_unique UNIQUE (external_uri)
);

-- The base polymorphic node for all ActivityStreams and tenant domain objects
CREATE TABLE IF NOT EXISTS entities (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    entity_type VARCHAR NOT NULL,
    external_id UUID NULL REFERENCES external_resources(id),
    created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMPTZ NULL DEFAULT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_entities_active ON entities (id DESC) WHERE deleted_on IS NULL INCLUDE (entity_type);
CREATE UNIQUE INDEX IF NOT EXISTS idx_entities_type_lookup ON entities (entity_type, id DESC) WHERE deleted_on IS NULL;

CREATE TABLE IF NOT EXISTS secondary_types (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    types_list VARCHAR[] NOT NULL
);

CREATE TABLE IF NOT EXISTS contact_info (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    owner_id UUID REFERENCES entities(id) ON DELETE CASCADE,
    ordering INTEGER NOT NULL,
    contact_type tcontact NOT NULL,
    contact VARCHAR NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT contact_info_singular UNIQUE (owner_id, ordering)
);

-- Retooled from server_metadata to specify local studio details
CREATE TABLE IF NOT EXISTS studio_metadata (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
);

-- Individuals are now fully scoped to the single studio schema
CREATE TABLE IF NOT EXISTS individuals (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
);

-- Studio-scoped logins. No cross-studio contamination.
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY REFERENCES individuals(id) ON DELETE CASCADE,
    username VARCHAR NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_local_username ON users (username);

CREATE TABLE IF NOT EXISTS actors (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    summary TEXT NULL,
    preferred_username VARCHAR NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_actors_local_uname ON actors USING HASH (preferred_username);

CREATE TABLE IF NOT EXISTS actor_endpoints (
    actor_id UUID NOT NULL REFERENCES actors(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL,
    endpoint VARCHAR NOT NULL,
    CONSTRAINT actor_endpoints_pkey PRIMARY KEY (actor_id, name)
);

CREATE TABLE IF NOT EXISTS collections (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_collections_local_ctx ON collections (owner_id DESC, name);

CREATE TABLE IF NOT EXISTS collection_items (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    added_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refers_to UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    role_name VARCHAR NOT NULL
);

-- Retooled: studio_id removed since the entire table is wrapped in a single studio's schema
CREATE TABLE IF NOT EXISTS role_assignments (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    individual_id UUID NOT NULL REFERENCES individuals(id) ON DELETE CASCADE,
    CONSTRAINT role_assignments_pkey PRIMARY KEY (role_id, individual_id)
);

CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    address VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS systems (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    system_id UUID NULL REFERENCES systems(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS characters (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name_template VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS character_instances (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES individuals(id) ON DELETE CASCADE,
    game_id UUID NULL REFERENCES games(id) ON DELETE SET NULL,
    individual_name VARCHAR NOT NULL,
    CONSTRAINT character_instances_unique UNIQUE (player_id, character_id, game_id)
);

---
--- Database Logic & Mutation Triggers
---

CREATE OR REPLACE FUNCTION sync_tenant_entity_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_on = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_tenant_entities
BEFORE UPDATE ON entities
FOR EACH ROW EXECUTE FUNCTION sync_tenant_entity_updated_at();

CREATE OR REPLACE FUNCTION touch_tenant_parent_entity()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE entities SET updated_on = CURRENT_TIMESTAMP WHERE id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    child_table TEXT;
BEGIN
    FOREACH child_table IN ARRAY ARRAY['studio_metadata', 'individuals', 'users', 'actors', 'collections', 'roles', 'locations', 'systems', 'campaigns', 'games', 'characters', 'character_instances']
    LOOP
        EXECUTE format('
            CREATE OR REPLACE TRIGGER trigger_touch_entity_%I
            AFTER UPDATE ON %I
            FOR EACH ROW
            WHEN (OLD.* IS DISTINCT FROM NEW.*)
            EXECUTE FUNCTION touch_tenant_parent_entity()',
            child_table, child_table);
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION propagate_tenant_entity_deletion()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_on IS NOT NULL AND OLD.deleted_on IS NULL THEN
        
        UPDATE entities SET deleted_on = NEW.deleted_on
        WHERE id IN (
            SELECT id FROM actors WHERE owner_id = NEW.id
            UNION
            SELECT id FROM collections WHERE owner_id = NEW.id
        ) AND deleted_on IS NULL;

        UPDATE entities SET deleted_on = NEW.deleted_on
        WHERE id IN (
            SELECT id FROM games WHERE campaign_id = NEW.id
            UNION
            SELECT id FROM characters WHERE campaign_id = NEW.id
        ) AND deleted_on IS NULL;

        UPDATE entities SET deleted_on = NEW.deleted_on
        WHERE id IN (
            SELECT id FROM character_instances 
            WHERE game_id = NEW.id OR character_id = NEW.id OR player_id = NEW.id
        ) AND deleted_on IS NULL;

        UPDATE contact_info SET active = FALSE WHERE owner_id = NEW.id;

    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_propagate
AFTER UPDATE OF deleted_on ON entities
FOR EACH ROW EXECUTE FUNCTION propagate_tenant_entity_deletion();
