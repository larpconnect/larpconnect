## ADDED Requirements
## ADDED Requirements

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

## MODIFIED Requirements

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
