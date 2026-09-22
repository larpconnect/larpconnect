#!/bin/sh
set -e

MIGRATION_PASSWORD="njall_migration_${NJALL_DB_SECRET}"
ADMIN_PASSWORD="njall_admin_${NJALL_DB_SECRET}"
USERS_PASSWORD="njall_users_${NJALL_DB_SECRET}"
SYSTEM_PASSWORD="njall_system_${NJALL_DB_SECRET}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  DO \$\$
  BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall') THEN
      CREATE ROLE njall WITH LOGIN SUPERUSER PASSWORD '$MIGRATION_PASSWORD';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_admin') THEN
      CREATE ROLE njall_admin WITH LOGIN PASSWORD '$ADMIN_PASSWORD';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_users') THEN
      CREATE ROLE njall_users WITH LOGIN PASSWORD '$USERS_PASSWORD';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_system') THEN
      CREATE ROLE njall_system WITH LOGIN PASSWORD '$SYSTEM_PASSWORD';
    END IF;
  END \$\$;

  ALTER DATABASE larpconnect OWNER TO njall;
  GRANT njall_users TO njall WITH ADMIN OPTION;
  GRANT njall_admin TO njall WITH ADMIN OPTION;
  GRANT njall_system TO njall WITH ADMIN OPTION;
  CREATE EXTENSION IF NOT EXISTS postgis;
EOSQL
