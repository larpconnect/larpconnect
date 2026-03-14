CREATE TABLE entities (
        id UUID PRIMARY KEY DEFAULT uuidv7(),
        entity_type VARCHAR NOT NULL,
        external_reference VARCHAR(2048) NULL,
        created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        deleted_on TIMESTAMP WITH TIME ZONE NULL DEFAULT NULL
);

CREATE UNIQUE INDEX ON entities (entity_type, id DESC);
CREATE INDEX ON entities USING HASH (entity_type);
CREATE INDEX ON entities (external_reference) WHERE external_reference IS NOT NULL;

CREATE TYPE tcontact AS ENUM ('email', 'phone', 'address');

CREATE TABLE contact_info (
        owner_id UUID PRIMARY KEY REFERENCES entities(id),
        ordering INTEGER NOT NULL,
        contact_type tcontact NOT NULL,
        contact VARCHAR NOT NULL,
        active BOOLEAN NOT NULL
);

CREATE INDEX ON contact_info (owner_id, contact_type) WHERE NOT active INCLUDE (ordering, contact);

-- Will typically only contain one row
CREATE TABLE server_metadata (
        name VARCHAR NOT NULL,
        admin VARCHAR NOT NULL,
        security VARCHAR NOT NULL,
        support VARCHAR NOT NULL
) INHERITS (entities);

-- Represents an ActivityPub actor. Nb. that unlikely many other systems this one supports one individual having multiple actors to represent themselves
CREATE TABLE actors (
        owner_id UUID NOT NULL REFERENCES entities(id),
        summary TEXT NULL,
        preferred_username VARCHAR NOT NULL
) INHERITS (entities);

CREATE INDEX ON actors(owner_id) INCLUDE (preferred_username);
CREATE UNIQUE INDEX ON actors USING HASH (preferred_username);

-- actor_endpoints. There are some that are prescribed, but a bunch of other possibilities.
CREATE TABLE actor_endpoints (
        actor_id UUID NOT NULL,
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

CREATE TABLE collection_items (
        collection_id UUID REFERENCES collections(id),
        added_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
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
        game_id UUID NULL REFERENCES games(id),
        individual_name VARCHAR NOT NULL,
        CONSTRAINT character_instances_pkey UNIQUE (player_id, character_id)
) INHERITS (entities);

CREATE INDEX ON character_instances (player_id DESC);
CREATE INDEX ON character_instances (game_id DESC) INCLUDE (player_id) WHERE game_id IS NOT NULL;
