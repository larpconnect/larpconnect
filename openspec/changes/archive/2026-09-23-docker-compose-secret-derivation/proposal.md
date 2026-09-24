## Why

The **Njall** Docker Compose environment and database provisioning scripts currently embed plaintext credentials directly in tracked repository files (`docker-compose.yml` and `01-init.sql`). This triggers automated security scanners, risks credential leakage in process inspection and container metadata, and creates tight coupling where altering Compose variables does not update the static SQL role definitions. Furthermore, there is no isolation to prevent local credential files (`.env`) from being inadvertently committed to version control.

## What Changes

- Introduce automated ephemeral secret generation within root Gradle Compose tasks (`composeUp`, `composeStart`) using `java.security.SecureRandom`.
- Persist the generated master secret seed (`NJALL_DB_SECRET`) into a local, Git-ignored `.env` file upon initial startup.
- Update `.gitignore` to explicitly ignore `.env` and `.env.*` patterns while permitting tracked `.env.example`.
- Provide a committed `.env.example` file documenting configuration options with non-sensitive placeholder values.
- Update `docker-compose.yml` to remove all hardcoded plaintext passwords, deriving distinct credentials for each role (`njall_migration_<secret>`, `njall_admin_<secret>`, `njall_users_<secret>`, `njall_system_<secret>`) from the master seed.
- Replace static SQL initialization (`01-init.sql`) with a dynamic shell script (`01-init.sh`) executed by the PostgreSQL entrypoint to provision roles using the derived secrets.
- Update `composeClean` to wipe the persistent database volume alongside the local `.env` file, preserving volume-credential consistency.
- Sanitize fallback defaults in `reference.conf` so database passwords default to empty strings rather than hardcoded credentials.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `system-runtime-orchestration`: Updates Docker Compose orchestration requirements to mandate ephemeral secret seed generation, dynamic role password derivation, local `.env` isolation, Git exclusion, and automated credential teardown during volume purge.

## Impact

- **Affected Files**: `build.gradle.kts`, `docker-compose.yml`, `docker/postgres/init/01-init.sh`, `docker/postgres/init/01-init.sql` (removed/replaced), `.gitignore`, `.env.example` (new), `common/src/main/resources/reference.conf`.
- **Affected Systems**: Local development and testing container runtime orchestrating the **Server** **application module** and PostgreSQL **Data plane**.
- **Security / Scanning**: Completely eliminates hardcoded passwords from tracked files, preventing security scanner alerts and isolating testing secrets from version control.
- **Dependencies**: No external runtime or build dependencies added; utilizes standard Java standard library (`SecureRandom`, `HexFormat`, `Files`) in Gradle and POSIX shell in Alpine PostgreSQL.
