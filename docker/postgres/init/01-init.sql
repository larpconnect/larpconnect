-- Provision application database roles
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall') THEN
    CREATE ROLE njall WITH LOGIN SUPERUSER PASSWORD 'njall';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_admin') THEN
    CREATE ROLE njall_admin WITH LOGIN PASSWORD 'njall_admin';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_users') THEN
    CREATE ROLE njall_users WITH LOGIN PASSWORD 'njall_users';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_system') THEN
    CREATE ROLE njall_system WITH LOGIN PASSWORD 'njall_system';
  END IF;
END $$;

-- Assign database ownership and administrative privileges
ALTER DATABASE larpconnect OWNER TO njall;

GRANT njall_users TO njall WITH ADMIN OPTION;
GRANT njall_admin TO njall WITH ADMIN OPTION;
GRANT njall_system TO njall WITH ADMIN OPTION;

-- Enable PostGIS spatial extension
CREATE EXTENSION IF NOT EXISTS postgis;
