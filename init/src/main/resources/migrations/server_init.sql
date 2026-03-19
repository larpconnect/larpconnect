CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tcontact') THEN
        CREATE TYPE tcontact AS ENUM ('email', 'phone', 'address');
    END IF;
END
$$;

-- Represents external sources of data.
CREATE TABLE IF NOT EXISTS external_resources (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    external_uri VARCHAR NOT NULL,
    data JSONB NULL,
    last_refresh TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hostname VARCHAR GENERATED ALWAYS AS (substring(external_uri FROM '.*://([^/
]*)')) STORED,
    CONSTRAINT external_resource_unique UNIQUE (external_uri)
);

-- Represents the base "entity" class. All independent objects are entities.
CREATE TABLE IF NOT EXISTS entities (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    entity_type VARCHAR NOT NULL,
    external_id UUID NULL REFERENCES external_resources(id),
    -- The following three fields are managed by database triggers, not by the application
    created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_on TIMESTAMPTZ NULL DEFAULT NULL -- Deleting entities will involve setting this to a value
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_entities_id ON entities (id DESC) WHERE deleted_on IS NULL INCLUDE (entity_type);
CREATE UNIQUE INDEX IF NOT EXISTS idx_entities_type_id ON entities (entity_type, id DESC) WHERE deleted_on IS NULL;
CREATE INDEX IF NOT EXISTS idx_entities_type_hash ON entities USING HASH (entity_type) WHERE deleted_on IS NULL;
CREATE INDEX IF NOT EXISTS idx_entities_ext_id ON entities (external_id DESC) WHERE external_id IS NULL;

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

CREATE INDEX IF NOT EXISTS idx_contact_info_active ON contact_info (owner_id DESC, contact_type) WHERE active INCLUDE (ordering, contact);

-- Will typically only contain one row. Will have multiple contact_info rows.
CREATE TABLE IF NOT EXISTS server_metadata (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
    -- Will have multiple contact entries
);

-- Represents an ActivityPub actor. Nb. that unlikely many other systems this one supports one individual having multiple actors to represent themselves.
CREATE TABLE IF NOT EXISTS actors (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES entities(id),
    summary TEXT NULL,
    preferred_username VARCHAR NOT NULL
    -- Will have multiple collections associated with it.
);

CREATE INDEX IF NOT EXISTS idx_actors_owner ON actors (owner_id DESC) INCLUDE (preferred_username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_actors_username_hash ON actors USING HASH (preferred_username);

-- actor:endpoints. There are some that are prescribed, but a bunch of other possibilities. These do not exist independently of actors.
CREATE TABLE IF NOT EXISTS actor_endpoints (
    actor_id UUID NOT NULL REFERENCES actors(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL,
    endpoint VARCHAR NOT NULL,
    CONSTRAINT actor_endpoints_pkey PRIMARY KEY (actor_id, name)
);

CREATE INDEX IF NOT EXISTS idx_actor_endpoints_actor ON actor_endpoints (actor_id DESC) INCLUDE (name, endpoint);

-- Collection objects that are associated with entities.
CREATE TABLE IF NOT EXISTS collections (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES entities(id),
    name VARCHAR NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_collections_owner_name ON collections (owner_id DESC, name);
CREATE INDEX idx_collections_owner ON collections (owner_id DESC);

-- Note that a single collection may have multiple of the same item added to it. There is no primary key here.
CREATE TABLE IF NOT EXISTS collection_items (
    -- synthetic id for schema purposes
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    added_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refers_to UUID NOT NULL REFERENCES entities(id)
);

CREATE INDEX IF NOT EXISTS idx_coll_items_coll ON collection_items (collection_id DESC) INCLUDE (added_on, refers_to);
CREATE INDEX IF NOT EXISTS idx_coll_items_coll_added ON collection_items (collection_id DESC, added_on DESC) INCLUDE (refers_to);

-- Individuals
CREATE TABLE IF NOT EXISTS individuals (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
);

-- Individuals who have individual logins on the system
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY REFERENCES individuals(id) ON DELETE CASCADE,
    username VARCHAR NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users (username);

-- A LARP studio
CREATE TABLE IF NOT EXISTS studios (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    role_name VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS studio_roles (
    studio_id UUID NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    individual_id UUID NOT NULL REFERENCES individuals(id) ON DELETE CASCADE,
    CONSTRAINT studio_roles_pkey PRIMARY KEY (studio_id, role_id, individual_id)
);

CREATE INDEX IF NOT EXISTS idx_studio_roles_studio ON studio_roles (studio_id DESC) INCLUDE (role_id, individual_id);

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
    system_id UUID NULL REFERENCES systems(id)
);

CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id)
);

CREATE INDEX IF NOT EXISTS idx_games_campaign ON games (campaign_id DESC);

CREATE TABLE IF NOT EXISTS characters (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    name_template VARCHAR NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_characters_camp ON characters (campaign_id DESC);

CREATE TABLE IF NOT EXISTS character_instances (
    id UUID PRIMARY KEY REFERENCES entities(id) ON DELETE CASCADE,
    character_id UUID NOT NULL REFERENCES characters(id),
    player_id UUID NOT NULL REFERENCES individuals(id),
    game_id UUID NULL REFERENCES games(id),
    individual_name VARCHAR NOT NULL,
    CONSTRAINT character_instances_unique UNIQUE (player_id, character_id, game_id)
);

CREATE INDEX IF NOT EXISTS idx_char_inst_player ON character_instances (player_id DESC);
CREATE INDEX IF NOT EXISTS idx_char_inst_game ON character_instances (game_id DESC) INCLUDE (player_id) WHERE game_id IS NOT NULL;

-- Base trigger to update the parent entity timestamp
CREATE OR REPLACE FUNCTION sync_entity_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_on = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_update_entities
BEFORE UPDATE ON entities
FOR EACH ROW EXECUTE FUNCTION sync_entity_updated_at();

-- Trigger function to bubble updates from subclasses up to the parent entity
CREATE OR REPLACE FUNCTION touch_parent_entity()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE entities SET updated_on = CURRENT_TIMESTAMP WHERE id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply the bubble-up trigger to all CTI subclass tables
DO $$
DECLARE
    child_table TEXT;
BEGIN
    FOREACH child_table IN ARRAY ARRAY['server_metadata', 'actors', 'collections', 'individuals', 'users', 'studios', 'roles', 'locations', 'systems', 'campaigns', 'games', 'characters', 'character_instances']
    LOOP
        EXECUTE format('
            CREATE OR REPLACE TRIGGER trigger_touch_entity_%I
            AFTER UPDATE ON %I
            FOR EACH ROW
            WHEN (OLD.* IS DISTINCT FROM NEW.*)
            EXECUTE FUNCTION touch_parent_entity()',
            child_table, child_table);
    END LOOP;
END;
$$;

-- Soft delete propagation
CREATE OR REPLACE FUNCTION propagate_entity_deletion()
RETURNS TRIGGER AS $$
BEGIN
    -- Only trigger if deleted_on is being set for the first time
    IF NEW.deleted_on IS NOT NULL AND OLD.deleted_on IS NULL THEN

        -- 1. Propagate to Actors, Collections owned by this entity
        -- These are 'owned' sub-entities.
        UPDATE entities
        SET deleted_on = NEW.deleted_on
        WHERE id IN (
            SELECT id FROM actors WHERE owner_id = NEW.id
            UNION
            SELECT id FROM collections WHERE owner_id = NEW.id
        ) AND deleted_on IS NULL;

        -- 2. Propagate LARP Hierarchy (Campaign -> Games & Characters)
        -- If a Campaign is deleted, delete all related Games and Characters
        UPDATE entities
        SET deleted_on = NEW.deleted_on
        WHERE id IN (
            SELECT id FROM games WHERE campaign_id = NEW.id
            UNION
            SELECT id FROM characters WHERE campaign_id = NEW.id
        ) AND deleted_on IS NULL;

        -- 3. Propagate Game/Character Instances
        -- If a Game or Character is deleted, the Instance must die too
        UPDATE entities
        SET deleted_on = NEW.deleted_on
        WHERE id IN (
            SELECT id FROM character_instances
            WHERE game_id = NEW.id OR character_id = NEW.id OR player_id = NEW.id
        ) AND deleted_on IS NULL;

        -- 4. Handle non-entity metadata (Physical removal or deactivation)
        UPDATE contact_info SET active = FALSE WHERE owner_id = NEW.id;

    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_soft_delete_propagate
AFTER UPDATE OF deleted_on ON entities
FOR EACH ROW EXECUTE FUNCTION propagate_entity_deletion();
