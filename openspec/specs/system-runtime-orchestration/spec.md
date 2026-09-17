# system-runtime-orchestration Specification

## Purpose

Multi-container orchestration via Docker Compose and Gradle lifecycle tasks, managing PostgreSQL initialization, healthcheck barriers, sequential schema migration execution, and HTTP server runtime startup.

## Requirements

### Requirement: Multi-Container Service Orchestration via Docker Compose
The system SHALL provide a Docker Compose configuration defining three coordinated services: `postgres`, `migrate`, and `server`. The `postgres` service SHALL start the database engine and report readiness via healthcheck. The `migrate` service SHALL execute database migrations via the `migrate` CLI subcommand only after `postgres` reports healthy, running as an ephemeral task that exits upon completion. The `server` service SHALL launch the HTTP server runtime via the `server` CLI subcommand only after `postgres` is healthy and the `migrate` service has completed successfully (exit code 0).

#### Scenario: Sequential container startup ordering
- **GIVEN** a clean Docker Compose environment with no running containers
- **WHEN** the compose stack is launched with `docker compose up`
- **THEN** the `postgres` container starts first and performs healthcheck polling
- **AND** the `migrate` container runs `bin/server migrate` against the database once `postgres` is healthy
- **AND** the `server` container launches `bin/server server` only after `migrate` terminates successfully with exit status 0
- **AND** the HTTP server binds to port 8080 and accepts client requests

#### Scenario: Migration failure halts server initialization
- **GIVEN** a Docker Compose stack where the `migrate` task fails with a non-zero exit status
- **WHEN** container execution progresses
- **THEN** the `server` container SHALL NOT be started
- **AND** the compose deployment terminates with an error status

### Requirement: Automated PostgreSQL Cluster Initialization
The `postgres` service SHALL use the `postgis/postgis:18-3.6-alpine` image and mount initialization SQL scripts into `/docker-entrypoint-initdb.d/`. On initial database cluster creation, the script SHALL create database roles `njall` (superuser), `njall_admin`, `njall_users`, and `njall_system` with configured passwords, grant admin options on user roles to `njall`, set the `larpconnect` database owner to `njall`, and enable the `postgis` extension.

#### Scenario: First-boot database provisioning
- **GIVEN** an uninitialized Postgres volume
- **WHEN** the `postgres` service starts for the first time
- **THEN** roles `njall`, `njall_admin`, `njall_users`, and `njall_system` are created
- **AND** the `larpconnect` database is owned by `njall`
- **AND** the `postgis` extension is created and ready for spatial queries
- **AND** the healthcheck `pg_isready -U postgres -d larpconnect` reports healthy

### Requirement: Gradle Lifecycle Tasks for Compose Orchestration
The root Gradle build SHALL define lifecycle tasks for managing the containerized environment:
- `composeUp`: Compiles the server distribution via `:server:installDist` and runs `docker compose up --build` in foreground attached mode.
- `composeDown`: Executes `docker compose down` to stop containers and remove the application network.
- `composeStart`: Executes `docker compose up --build -d` in detached background mode after staging `:server:installDist`.
- `composeLogs`: Executes `docker compose logs -f` to follow service logs.
- `composeClean`: Executes `docker compose down -v` to stop containers and delete persistent database volumes.

#### Scenario: Launching the system via composeUp
- **GIVEN** the workspace source files have been modified
- **WHEN** `./gradlew composeUp` is executed
- **THEN** Gradle executes `:server:installDist` to incrementally recompile and stage application binaries
- **AND** Docker builds the application image by copying staged distribution artifacts
- **AND** Docker Compose launches `postgres`, `migrate`, and `server` services with log streaming in the terminal

#### Scenario: Tearing down the system via composeDown
- **GIVEN** the Docker Compose stack is running
- **WHEN** `./gradlew composeDown` is executed
- **THEN** `docker compose down` is executed
- **AND** all containers and networks are stopped and removed
