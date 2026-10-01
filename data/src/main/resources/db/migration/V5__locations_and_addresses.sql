SET search_path = njall_users, public;

-- Locations Subtype Table (Common Table Inheritance referencing njall_users.entities)
CREATE TABLE IF NOT EXISTS njall_users.locations (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_locations_id UNIQUE (id),
    CONSTRAINT fk_locations_entities FOREIGN KEY (tenant_id, id)
        REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE
);

ALTER TABLE njall_users.locations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_locations ON njall_users.locations;
CREATE POLICY rls_locations ON njall_users.locations
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_locations_admin ON njall_users.locations;
CREATE POLICY rls_locations_admin ON njall_users.locations
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Addresses Subordinate Table (1:N child table referencing njall_users.locations)
CREATE TABLE IF NOT EXISTS njall_users.addresses (
    tenant_id UUID NOT NULL,
    id UUID NOT NULL DEFAULT uuidv7(),
    location_id UUID NOT NULL,
    address_type VARCHAR(64) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255) NOT NULL DEFAULT '',
    address_line_3 VARCHAR(255) NOT NULL DEFAULT '',
    locality VARCHAR(255) NOT NULL,
    administrative_area VARCHAR(255) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country_code CHAR(2) NOT NULL,
    geom geography(Point, 4326) NULL,

    PRIMARY KEY (tenant_id, id),
    CONSTRAINT unq_addresses_id UNIQUE (id),
    CONSTRAINT fk_addresses_locations FOREIGN KEY (tenant_id, location_id)
        REFERENCES njall_users.locations (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_addresses_location
    ON njall_users.addresses (tenant_id, location_id);

CREATE INDEX IF NOT EXISTS idx_addresses_search
    ON njall_users.addresses (tenant_id, country_code, administrative_area, locality, postal_code)
    INCLUDE (location_id);

CREATE INDEX IF NOT EXISTS idx_addresses_geom
    ON njall_users.addresses USING GIST (geom);

ALTER TABLE njall_users.addresses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_addresses ON njall_users.addresses;
CREATE POLICY rls_addresses ON njall_users.addresses
    FOR ALL TO njall_users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

DROP POLICY IF EXISTS rls_addresses_admin ON njall_users.addresses;
CREATE POLICY rls_addresses_admin ON njall_users.addresses
    FOR ALL TO njall_admin
    USING (true) WITH CHECK (true);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.locations TO njall_users;
GRANT ALL ON njall_users.locations TO njall_admin;

GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.addresses TO njall_users;
GRANT ALL ON njall_users.addresses TO njall_admin;
