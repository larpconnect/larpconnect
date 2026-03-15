CREATE TABLE entities (
	id UUID PRIMARY KEY DEFAULT uuidv7(),
	entity_type VARCHAR NOT NULL,
	external_reference VARCHAR(2048) NULL,
        -- The following three fields are managed by database triggers, not by the application
	created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	deleted_on TIMESTAMPTZ NULL DEFAULT NULL
);

CREATE UNIQUE INDEX ON entities (entity_type, id DESC);
CREATE INDEX ON entities USING HASH (entity_type);
CREATE INDEX ON entities (external_reference) WHERE external_reference IS NOT NULL;

CREATE TYPE tcontact AS ENUM ('email', 'phone', 'address');

CREATE TABLE contact_info (
        id UUID PRIMARY KEY DEFAULT uuidv7(),
	owner_id REFERENCES entities(id),
	ordering INTEGER NOT NULL,
	contact_type tcontact NOT NULL,
	contact VARCHAR NOT NULL,
	active BOOLEAN NOT NULL
);

CREATE INDEX ON contact_info (owner_id, contact_type) WHERE NOT active INCLUDE (ordering, contact);

-- Will typically only contain one row. Will have multiple contact_info rows.
CREATE TABLE server_metadata (
	name VARCHAR NOT NULL
) INHERITS (entities);

-- Represents an ActivityPub actor. Nb. that unlikely many other systems this one supports one individual having multiple actors to represent themselves.
CREATE TABLE actors (
	owner_id UUID NOT NULL REFERENCES entities(id),
	summary TEXT NULL,
	preferred_username VARCHAR NOT NULL
        -- Will have multiple collections associated with it.
) INHERITS (entities);

CREATE INDEX ON actors(owner_id) INCLUDE (preferred_username);
CREATE UNIQUE INDEX ON actors USING HASH (preferred_username);

-- actor:endpoints. There are some that are prescribed, but a bunch of other possibilities.
CREATE TABLE actor_endpoints (
	actor_id UUID NOT NULL REFERENCES actors(id),
	name VARCHAR NOT NULL,
	endpoint VARCHAR NOT NULL,
	CONSTRAINT actor_endpoints_pkey PRIMARY KEY (actor_id, name)
);

CREATE TABLE collections (
	owner_id UUID NOT NULL REFERENCES entities(id),
	name VARCHAR NOT NULL,
	CONSTRAINT collections_pkey PRIMARY KEY (owner_id DESC, name)
) INHERITS (entities);

CREATE UNIQUE INDEX ON collections (owner_id, name);

-- Note that a single collection may have multiple of the same item added to it. There is no primary key here.
CREATE TABLE collection_items (
	collection_id UUID REFERENCES collections(id),
	added_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	refers_to UUID NOT NULL REFERENCES entities(id)
);

CREATE INDEX ON collection_items (collection_id DESC) INCLUDE (added_on, refers_to);

-- Individuals
CREATE TABLE individuals (
	name VARCHAR NOT NULL
) INHERITS (entities);

-- Individuals who have individual logins on the system
CREATE TABLE users (
	owner_id UUID NOT NULL REFERENCES individuals(id),
	username VARCHAR NOT NULL
) INHERITS (individuals);

CREATE UNIQUE INDEX ON users (username);

-- A LARP studio
CREATE TABLE studios (
	name VARCHAR NOT NULL
) INHERITS (entities);

CREATE TABLE roles (
	role_name VARCHAR NOT NULL
) INHERITS (entities);

CREATE TABLE studio_roles (
	studio_id UUID NOT NULL REFERENCES studios(id),
	role_id UUID NOT NULL REFERENCES roles(id),
	individual_id UUID NOT NULL REFERENCES individuals(id),
	CONSTRAINT studio_roles_pkey PRIMARY KEY (studio_id, role_id, individual_id)
);

CREATE INDEX ON studio_roles (studio_id DESC);

CREATE TABLE locations (
	address VARCHAR NOT NULL
) INHERITS (entities);

CREATE TABLE systems (
	name VARCHAR NOT NULL
) INHERITS (entities);

CREATE TABLE campaigns (
	system_id UUID NULL REFERENCES systems(id)
) INHERITS (entities);

CREATE TABLE games (
	campaign_id UUID NOT NULL REFERENCES campaigns(id)
) INHERITS (entities);

CREATE TABLE characters (
	campaign_id UUID NOT NULL REFERENCES campaigns(id),
	name_template VARCHAR NOT NULL
) INHERITS (entities);

CREATE TABLE character_instances (
	character_id UUID NOT NULL REFERENCES characters(id),
	player_id UUID NOT NULL REFERENCES individuals(id),
	game_id NULL REFERENCES games(id),
	individual_name VARCHAR NOT NULL,
	CONSTRAINT character_instances_unique UNIQUE (player_id, character_id, game_id)
) INHERITS (entities);

CREATE INDEX ON character_instances (player_id DESC);
CREATE INDEX ON character_instances (game_id DESC) INCLUDE (player_id) WHERE game_id IS NOT NULL;

CREATE OR REPLACE FUNCTION sync_entity_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_on = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DO $$
DECLARE
    child_table_name TEXT;
BEGIN
    FOR child_table_name IN
        SELECT inhrelid::regclass::text
        FROM pg_inherits
        WHERE inhparent = 'entities'::regclass
    LOOP
        EXECUTE format('
            CREATE OR REPLACE TRIGGER trigger_update_%I
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION sync_entity_updated_at()',
            child_table_name, child_table_name);
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION propagate_entity_deletion()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted_on IS NOT NULL AND OLD.deleted_on IS NULL THEN
        -- Propagate to actors owned by this entity
        UPDATE actors SET deleted_on = NEW.deleted_on WHERE owner_id = NEW.id;

        -- Propagate to contact info
        UPDATE contact_info SET active = FALSE WHERE owner_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_soft_delete_propagate
AFTER UPDATE OF deleted_on ON entities
FOR EACH ROW EXECUTE FUNCTION propagate_entity_deletion();
