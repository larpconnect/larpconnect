## Why

Currently, running the full Project Njall / LarpConnect stack requires manually provisioning a PostgreSQL 18+ instance, configuring database users, running the `migrate` CLI subcommand, and then starting the HTTP server. To provide a turnkey developer workflow and deterministic local system integration, developers and operators need the ability to launch, configure, migrate, and run the entire multi-container environment via a single Gradle task (`./gradlew composeUp`).

## What Changes

- Add `docker-compose.yml` defining three orchestrated services:
  - `postgres`: PostgreSQL 18+ PostGIS container with automated healthcheck and role initialization.
  - `migrate`: Ephemeral container that executes Flyway migrations (`bin/server migrate`) and terminates with exit status 0.
  - `server`: Long-running HTTP server container (`bin/server server`) that begins listening on port 8080 once migrations complete successfully.
- Add `docker/postgres/init/01-init.sql` to configure roles (`njall`, `njall_admin`, `njall_users`, `njall_system`), grant admin privileges to the schema owner, and enable the PostGIS extension on first boot.
- Update `Dockerfile` to package the staged `:server:installDist` distribution using the Temurin Java 25 runtime image for fast, cache-leveraged builds.
- Configure root Gradle tasks in `build.gradle.kts`:
  - `composeUp`: Compiles distribution and starts the full Compose stack in the foreground.
  - `composeDown`: Stops and tears down Compose containers and networks.
  - `composeStart`: Launches the Compose stack in detached background mode (`-d`).
  - `composeLogs`: Streams logs from all Compose services.
  - `composeClean`: Tears down Compose containers and wipes persistent database volumes (`-v`).

## Capabilities

### New Capabilities
- `system-runtime-orchestration`: Defines operational and behavioral requirements for containerized system initialization, dependency-ordered database migration, HTTP server startup, and Gradle lifecycle control.

### Modified Capabilities
<!-- None. Existing capabilities (database-migration, command-line-interface, http-server-runtime) remain fully compliant. -->

## Impact
- **Root build**: Adds `build.gradle.kts` defining `compose*` lifecycle tasks and binding dependencies to `:server:installDist`.
- **Packaging**: Updates `Dockerfile` from legacy shadowJar references to standard `installDist` staging.
- **Orchestration**: Adds `docker-compose.yml` and `docker/postgres/init/01-init.sql`.
- **Java Code / APIs**: No changes required to Java source code, Pekko routes, or OpenAPI contracts.
