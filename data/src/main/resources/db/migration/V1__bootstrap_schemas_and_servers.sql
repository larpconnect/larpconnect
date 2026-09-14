CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SCHEMA IF NOT EXISTS njall;
CREATE SCHEMA IF NOT EXISTS njall_admin;
CREATE SCHEMA IF NOT EXISTS njall_users;
CREATE SCHEMA IF NOT EXISTS njall_system;

ALTER SCHEMA njall OWNER TO njall;
ALTER SCHEMA njall_admin OWNER TO njall;
ALTER SCHEMA njall_users OWNER TO njall;
ALTER SCHEMA njall_system OWNER TO njall;

ALTER ROLE njall SET search_path = njall, njall_system, njall_admin, njall_users, public;
ALTER ROLE njall_admin SET search_path = njall, njall_admin, njall_users, public;
ALTER ROLE njall_users SET search_path = njall, njall_users, public;

ALTER DEFAULT PRIVILEGES FOR ROLE njall IN SCHEMA njall GRANT SELECT ON TABLES TO njall_admin, njall_users;
ALTER DEFAULT PRIVILEGES FOR ROLE njall IN SCHEMA njall GRANT SELECT ON SEQUENCES TO njall_admin, njall_users;

ALTER DEFAULT PRIVILEGES FOR ROLE njall IN SCHEMA njall_admin GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO njall_admin;
ALTER DEFAULT PRIVILEGES FOR ROLE njall IN SCHEMA njall_admin GRANT USAGE, SELECT ON SEQUENCES TO njall_admin;

ALTER DEFAULT PRIVILEGES FOR ROLE njall IN SCHEMA njall_users GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO njall_admin, njall_users;
ALTER DEFAULT PRIVILEGES FOR ROLE njall IN SCHEMA njall_users GRANT USAGE, SELECT ON SEQUENCES TO njall_admin, njall_users;

GRANT USAGE ON SCHEMA njall, njall_admin, njall_users TO njall_admin;
GRANT USAGE ON SCHEMA njall, njall_users TO njall_users;

GRANT SELECT ON ALL TABLES IN SCHEMA njall TO njall_admin;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA njall TO njall_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA njall_admin TO njall_admin;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA njall_admin TO njall_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA njall_users TO njall_admin;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA njall_users TO njall_admin;

GRANT SELECT ON ALL TABLES IN SCHEMA njall TO njall_users;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA njall TO njall_users;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA njall_users TO njall_users;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA njall_users TO njall_users;

SET search_path = njall;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid WHERE t.typname = 'trole' AND n.nspname = 'njall') THEN
        CREATE TYPE njall.trole AS ENUM ('NONE', 'ADMIN', 'MODERATOR', 'SECURITY');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid WHERE t.typname = 'tcontact' AND n.nspname = 'njall') THEN
        CREATE TYPE njall.tcontact AS ENUM ('EMAIL', 'ACTOR', 'PHONE', 'OTHER');
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS njall.servers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL UNIQUE,
    primary_domain VARCHAR(255) NOT NULL,
    created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO njall.servers (name, primary_domain) VALUES ('${server_name}', '${primary_domain}') ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS njall.server_contacts (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    role_type njall.trole NOT NULL,
    contact_type njall.tcontact NOT NULL,
    contact VARCHAR(255) NOT NULL,
    ordering INT NOT NULL,
    UNIQUE (role_type, contact)
);

INSERT INTO njall.server_contacts (role_type, contact_type, contact, ordering) 
VALUES ('ADMIN', 'EMAIL', '${admin_contact}', 0) ON CONFLICT (role_type, contact) DO NOTHING;
