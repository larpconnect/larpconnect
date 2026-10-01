# system-runtime-orchestration Specification

## Purpose

Multi-container orchestration via Docker Compose and Gradle lifecycle tasks, managing PostgreSQL initialization, healthcheck barriers, sequential schema migration execution, and HTTP server runtime startup.

## Requirements

### Requirement: Multi-Container Service Orchestration via Docker Compose
The system SHALL provide a Docker Compose configuration defining five coordinated services: `postgres`, `migrate`, `seed`, `server`, and `haproxy`. The `postgres` service SHALL start the database engine and report readiness via healthcheck. The `migrate` service SHALL execute database migrations via the `migrate` CLI subcommand only after `postgres` reports healthy, running as an ephemeral task that exits upon completion. The `seed` service SHALL execute the test data kit using `psql` as `njall_admin` only after `postgres` reports healthy and the `migrate` service has completed successfully (exit code 0), running as an ephemeral task that exits upon completion. The `server` service SHALL launch the HTTP server runtime via the `server` CLI subcommand only after `postgres` is healthy, `migrate` has completed successfully (exit code 0), and `seed` has completed successfully (exit code 0). The `haproxy` service SHALL provide the public ingress gateway exposing HTTP on port 8080 and TLS on port 8443, routing traffic to `server:8080`, performing active health checks against `/api/admin/v1/health`, and stripping the W3C `traceparent` header from responses returned to clients. Docker Compose network isolation SHALL segment containers into an `edge` network connecting `haproxy` and `server`, and an `internal` network connecting `server`, `seed`, `migrate`, and `postgres`. The `server` container SHALL NOT publish any ports to the host network.

#### Scenario: Sequential container startup ordering
- **GIVEN** a clean Docker Compose environment with no running containers
- **WHEN** the compose stack is launched with `docker compose up`
- **THEN** the `postgres` container starts first and performs healthcheck polling
- **AND** the `migrate` container runs `bin/server migrate` against the database once `postgres` is healthy
- **AND** the `seed` container runs the test data kit against the database once `migrate` terminates successfully with exit status 0
- **AND** the `server` container launches `bin/server server` only after both `migrate` and `seed` terminate successfully with exit status 0
- **AND** the `haproxy` container launches, binds ports 8080 and 8443 on the host, and proxies requests to `server:8080`
- **AND** the `server` container does not publish port 8080 directly to the host network

#### Scenario: Ingress strips traceparent header from client response
- **GIVEN** the Docker Compose stack is running with `haproxy` and `server`
- **WHEN** a client sends an HTTP GET request to `http://localhost:8080/`
- **THEN** the request is forwarded by HAProxy to the backend server
- **AND** the backend server generates an internal W3C `traceparent` header
- **AND** HAProxy strips the `traceparent` header before returning the HTTP 200 response to the client

#### Scenario: Migration failure halts server initialization
- **GIVEN** a Docker Compose stack where the `migrate` task fails with a non-zero exit status
- **WHEN** container execution progresses
- **THEN** neither the `seed` nor `server` container SHALL be started
- **AND** the compose deployment terminates with an error status

#### Scenario: Seed failure halts server initialization
- **GIVEN** a Docker Compose stack where the `seed` task fails with a non-zero exit status
- **WHEN** container execution progresses
- **THEN** the `server` container SHALL NOT be started
- **AND** the compose deployment terminates with an error status

### Requirement: Automated Idempotent Test Data Seeding
The system SHALL provide an automated, idempotent test data kit located in directory `docker/postgres/seed/` executed by the `seed` service within Docker Compose. The `seed` service SHALL run all SQL files in lexicographical order connecting to PostgreSQL as the `njall_admin` **Role** using credentials derived from the master secret seed `NJALL_DB_SECRET`. The test data kit SHALL populate initial test records under two designated test **Studios**: "Valkyrie Example Larp Studio" with **Alias** `valkyrie_example` and "Ironwood Fake Larp Chronicles" with **Alias** `ironwood_fake`. The kit SHALL populate test **Admin User** accounts with assigned administrative **Roles**, default studio roles in `njall_users.default_studio_roles`, tenanted **Link** fixtures, physical **Locations** ("Camp Example", "Blackthorn Fake Manor Grounds"), and geospatial **Address** records. All data insertion statements SHALL enforce strict idempotency by utilizing fixed deterministic **ID** values and `ON CONFLICT DO NOTHING` clauses. All test entities created by the test data kit SHALL comply with the naming convention of using "Larp" (capitalized word) and containing either "example" or "fake" within their names.

#### Scenario: Idempotent execution of test data kit on stack startup
- **GIVEN** a Docker Compose stack where `postgres` is healthy and `migrate` has finished with exit status 0
- **WHEN** the `seed` service executes the SQL scripts in `docker/postgres/seed/`
- **THEN** test **Studios** `valkyrie_example` and `ironwood_fake` exist in both `njall_admin.studios_lookup` and `njall_users.studios`
- **AND** test **Admin User** accounts, default studio roles, **Link** records, and **Locations** are present in the database
- **AND** re-executing the `seed` service against an existing database volume completes with exit status 0 without creating duplicate records

#### Scenario: Test entity naming conventions enforced
- **GIVEN** the SQL seed files in `docker/postgres/seed/`
- **WHEN** records are inserted into `studios`, `locations`, `entities`, and `admin_users`
- **THEN** studio names contain "Larp" rather than "LARP"
- **AND** every human-readable test fixture name contains either "example" or "fake"

### Requirement: Automated PostgreSQL Cluster Initialization
The `postgres` service SHALL use the `postgis/postgis:18-3.6-alpine` image and mount initialization scripts into `/docker-entrypoint-initdb.d/`. On initial database cluster creation, an executable initialization shell script SHALL dynamically provision database roles `njall` (superuser), `njall_admin`, `njall_users`, and `njall_system` with passwords derived from `NJALL_DB_SECRET`, grant admin options on user roles to `njall`, set the `larpconnect` database owner to `njall`, and enable the `postgis` extension.

#### Scenario: First-boot database provisioning
- **GIVEN** an uninitialized Postgres volume
- **WHEN** the `postgres` service starts for the first time
- **THEN** roles `njall`, `njall_admin`, `njall_users`, and `njall_system` are created with derived passwords
- **AND** the `larpconnect` database is owned by `njall`
- **AND** the `postgis` extension is created and ready for spatial queries
- **AND** the healthcheck `pg_isready -U postgres -d larpconnect` reports healthy

### Requirement: Gradle Lifecycle Tasks for Compose Orchestration
The root Gradle build SHALL define lifecycle tasks for managing the containerized environment:
- `generateComposeEnv`: Ensures local `.env` exists with generated `NJALL_DB_SECRET`.
- `generateComposeTls`: Ensures local development TLS certificate (`docker/haproxy/certs/haproxy.pem`) exists, executing `docker/haproxy/generate-certs.sh` if absent.
- `composeStart`: Ensures local `.env` and TLS certificate exist, compiles the server distribution via `:server:installDist`, and runs `docker compose up --build -d` in detached background mode.
- `composeStop`: Executes `docker compose down` to stop containers and remove application networks.
- `composeStopClean`: Executes `docker compose down -v` to stop containers, delete persistent database volumes, and delete both the generated `.env` file and `docker/haproxy/certs/haproxy.pem`.
- `composeLogs`: Executes `docker compose logs -f` to follow service logs.

#### Scenario: Launching the system via composeUp
- **GIVEN** the workspace source files have been modified
- **WHEN** `./gradlew composeStart` is executed
- **THEN** Gradle ensures `.env` is present, generating a random 16-byte hex secret if absent
- **AND** Gradle ensures `docker/haproxy/certs/haproxy.pem` is present, generating a self-signed TLS cert bundle via `docker/haproxy/generate-certs.sh` if absent
- **AND** Gradle executes `:server:installDist` to incrementally recompile and stage application binaries
- **AND** Docker builds the application image by copying staged distribution artifacts
- **AND** Docker Compose launches `postgres`, `migrate`, `server`, and `haproxy` services in detached background mode

#### Scenario: Tearing down the system via composeDown
- **GIVEN** the Docker Compose stack is running
- **WHEN** `./gradlew composeStop` is executed
- **THEN** `docker compose down` is executed
- **AND** all containers and networks (`edge`, `internal`) are stopped and removed
- **AND** `.env` and TLS certificates are preserved for subsequent runs

#### Scenario: Resetting the system via composeClean
- **GIVEN** a running or stopped Docker Compose stack with persistent data volume, `.env`, and TLS certificates
- **WHEN** `./gradlew composeStopClean` is executed
- **THEN** `docker compose down -v` is executed to remove containers and volumes
- **AND** the local `.env` file is deleted
- **AND** the local `docker/haproxy/certs/haproxy.pem` file is deleted

### Requirement: Local Environment Secret Isolation and Repository Exclusion
The system SHALL isolate local container runtime secrets and certificates from source control by excluding `.env` files and TLS certificate bundles from Git tracking. The root `.gitignore` file SHALL ignore `.env`, `.env.*` (permitting `.env.example`), and `docker/haproxy/certs/*.pem` and `docker/haproxy/certs/*.key` while keeping the `docker/haproxy/certs/` directory structure tracked via `.gitkeep`. The `docker-compose.yml` configuration SHALL NOT contain plaintext passwords and SHALL derive role credentials from the master secret seed `NJALL_DB_SECRET`.

#### Scenario: Git excludes generated env files
- **GIVEN** local `.env` and `docker/haproxy/certs/haproxy.pem` files generated by Gradle or scripts
- **WHEN** Git status is evaluated
- **THEN** the `.env` and `.pem` files are ignored and excluded from untracked changes
- **AND** `.env.example` and `docker/haproxy/certs/.gitkeep` remain tracked in repository version control

#### Scenario: Cross-role credential mismatch fails fast
- **GIVEN** a running PostgreSQL service initialized with derived credentials
- **WHEN** a client or service attempts to authenticate using the `njall_users` password against the `njall_admin` role
- **THEN** authentication is rejected by PostgreSQL due to distinct derived password values
