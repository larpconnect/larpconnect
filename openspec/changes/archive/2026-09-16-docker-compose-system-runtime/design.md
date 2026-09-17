## Context

Project Njall / LarpConnect is a modular Java 25 LTS application utilizing Apache Pekko HTTP, Google Guice, Hibernate, and PostgreSQL 18+ with PostGIS. Running the full stack currently requires manually provisioning and configuring a PostgreSQL instance, running Flyway schema migrations, and launching the server process.

To streamline local development, integration testing, and operational deployment, we introduce a Docker Compose multi-container stack driven directly by standard Gradle tasks (`./gradlew composeUp`).

### In-Force ADR Alignment
- **ADR-0001**: Pekko HTTP runtime and coordinated shutdown.
- **ADR-0002**: Dropwizard healthcheck actor and `/admin/health` endpoint.
- **ADR-0003**: Data module and Flyway migration architecture.
- **ADR-0004**: Picocli CLI with `migrate` and `server` subcommands.
- **ADR-0005**: Hibernate dual session factories (`njall_admin`, `njall_users`).
- **ADR-0006**: Package-level non-null default conventions.

---

## Goals / Non-Goals

**Goals:**
- Provide a single Gradle task (`composeUp`) that builds the application distribution, initializes PostgreSQL, executes schema migrations, and starts the HTTP server.
- Orchestrate containers with strict dependency barriers: `migrate` runs only when `postgres` is healthy; `server` runs only when `migrate` completes with status 0.
- Automate first-boot PostgreSQL role creation (`njall`, `njall_admin`, `njall_users`, `njall_system`) and PostGIS extension installation.
- Provide clean lifecycle management (`composeDown`, `composeStart`, `composeLogs`, `composeClean`).
- Maintain sub-5-second local rebuild and restage times via host-driven incremental packaging.

**Non-Goals:**
- Production Kubernetes manifest generation or Helm charting (deferred to future deployment changes).
- Building an isolated shadowJar; standard Gradle `installDist` distribution is preferred.
- Modifying Java application code or OpenAPI specs.

---

## Architecture (C4 Container Diagram)

```
+---------------------------------------------------------------------------------+
|                                 DEVELOPER HOST                                  |
|                                                                                 |
|   +-------------------------------------------------------------------------+   |
|   |                        Gradle Build Tool (:server)                      |   |
|   |   - Compiles Java 25 sources incrementally via Gradle Daemon            |   |
|   |   - Stages distribution into server/build/install/server                |   |
|   |   - Invokes: docker compose up --build                                  |   |
|   +------------------------------------+------------------------------------+   |
+----------------------------------------|----------------------------------------+
                                         |
                                         v
+---------------------------------------------------------------------------------+
|                    DOCKER COMPOSE RUNTIME ENVIRONMENT                           |
|                                                                                 |
|   +------------------------------------+                                        |
|   | Container: postgres                |                                        |
|   | (postgis/postgis:18-3.6-alpine)    |                                        |
|   | - 01-init.sql configures roles     |                                        |
|   | - Healthcheck: pg_isready          |                                        |
|   +-----------------+------------------+                                        |
|                     |                                                           |
|       (service_healthy)                                                         |
|                     |                                                           |
|                     v                                                           |
|   +------------------------------------+                                        |
|   | Container: migrate                 |                                        |
|   | (larpconnect:latest)               |                                        |
|   | - Exec: bin/server migrate         |                                        |
|   | - Applies Flyway V1 migrations     |                                        |
|   | - Exits 0 upon completion          |                                        |
|   +-----------------+------------------+                                        |
|                     |                                                           |
|       (service_completed_successfully)                                          |
|                     |                                                           |
|                     v                                                           |
|   +------------------------------------+                                        |
|   | Container: server                  |                                        |
|   | (larpconnect:latest)               |                                        |
|   | - Exec: bin/server server          |                                        |
|   | - Binds port 8080                  |                                        |
|   | - Serves Pekko HTTP routes         |                                        |
|   +-----------------+------------------+                                        |
|                     |                                                           |
+---------------------|-----------------------------------------------------------+
                      v
             [ http://localhost:8080 ]
```

---

## Decisions

### Decision 1: Host-Driven Distribution Packaging
- **Choice**: The Gradle `composeUp` task executes `:server:installDist` on the host, staging distribution files into `server/build/install/server/`. The `Dockerfile` packages this pre-staged directory directly into `ghcr.io/rblaine95/eclipse-temurin:25`.
- **Rationale**: Leverages host Gradle daemon caching and incremental compilation. Rebuilding the container after a code change takes ~2 seconds instead of minutes of redundant dependency resolution inside Docker.
- **Alternatives Considered**: Multi-stage Docker build running Gradle inside container. Rejected due to extreme build latency and heavy memory overhead on local developer environments.

### Decision 2: Ephemeral Migration Container in Compose Stack
- **Choice**: Separate the application lifecycle in `docker-compose.yml` into two services: `migrate` (ephemeral task with `restart: "no"`) and `server` (long-running service).
- **Rationale**: Cleanly isolates schema initialization failures. If migrations fail, Compose does not boot `server`. `docker compose ps` clearly reports migration exit status (e.g. `Exited (0)`).
- **Alternatives Considered**: Combined entrypoint script running `migrate && exec server` within one container. Rejected because it hides migration failure metrics from Compose orchestration and complicates scaling `server` independently.

### Decision 3: Automated Postgres Role & PostGIS Provisioning via Init Script
- **Choice**: Mount `docker/postgres/init/01-init.sql` into `/docker-entrypoint-initdb.d/` in the `postgres` service.
- **Rationale**: Automates all manual prerequisites identified in `data/src/main/resources/db/prework.md` (creating roles `njall`, `njall_admin`, `njall_users`, `njall_system`, granting permissions, enabling PostGIS) on fresh cluster initialization.
- **Alternatives Considered**: Provisioning roles via Flyway. Rejected because Flyway connects as `njall` and cannot create superuser roles or database-level extensions outside its managed schemas.

### Decision 4: Root Gradle Task Lifecycle Integration
- **Choice**: Introduce standard `compose*` tasks in root `build.gradle.kts`:
  - `composeUp`: `:server:installDist` + `docker compose up --build` (foreground).
  - `composeDown`: `docker compose down`.
  - `composeStart`: `:server:installDist` + `docker compose up --build -d` (background).
  - `composeLogs`: `docker compose logs -f`.
  - `composeClean`: `docker compose down -v`.
- **Rationale**: Provides ergonomic developer commands matching idiomatic Gradle patterns without requiring manual shell navigation or complex Docker CLI syntax.

---

## Risks / Trade-offs

- **[Risk] Running `docker compose up` outside Gradle when distribution is stale** -> **Mitigation**: Document that developers should use `./gradlew composeUp` or build `:server:installDist` before invoking raw `docker compose`.
- **[Risk] Stale volume data across database schema revisions** -> **Mitigation**: Provide `composeClean` task that invokes `docker compose down -v` to reset data volumes with one command.
- **[Risk] Port collision on host 5432 or 8080** -> **Mitigation**: Ports are exposed using environment variable overrides (`${PORT:-8080}`, `${POSTGRES_PORT:-5432}`) in `docker-compose.yml`.

---

## Migration Plan

1. Create `docker/postgres/init/01-init.sql`.
2. Update `Dockerfile` to package `server/build/install/server`.
3. Create `docker-compose.yml`.
4. Create root `build.gradle.kts` registering `compose*` tasks.
5. Verify `./gradlew composeUp` starts all services, applies migrations, and responds to `/admin/health`.

---

## Open Questions

None. Architecture and requirements were established during exploration.
