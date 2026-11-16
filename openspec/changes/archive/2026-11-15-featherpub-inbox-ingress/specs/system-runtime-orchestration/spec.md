## MODIFIED Requirements

### Requirement: Multi-Container Service Orchestration via Docker Compose
The system SHALL provide a Docker Compose configuration defining six coordinated services:
`postgres`, `rabbitmq`, `migrate`, `seed`, `server`, and `haproxy`. The `postgres` service SHALL
start the database engine and report readiness via healthcheck. The `rabbitmq` service SHALL run
`rabbitmq:4.3-alpine` with native AMQP 1.0 enabled on port `5672` on the `internal` network,
authenticate using credentials derived from `NJALL_DB_SECRET` (`njall_amqp_${NJALL_DB_SECRET}`),
persist broker state in a named `rabbitmq_data` volume, and report readiness via
`rabbitmq-diagnostics -q check_port_connectivity`. The `migrate` service SHALL execute database
migrations via the `migrate` CLI subcommand only after `postgres` reports healthy, running as an
ephemeral task that exits upon completion. The `seed` service SHALL execute the test data kit using
`psql` as `njall_admin` only after `postgres` reports healthy and the `migrate` service has
completed successfully (exit code 0), running as an ephemeral task that exits upon completion. The
`server` service SHALL launch the HTTP server runtime via the `server` CLI subcommand only after
`postgres` is healthy, `rabbitmq` is healthy, `migrate` has completed successfully (exit code 0),
and `seed` has completed successfully (exit code 0). The `haproxy` service SHALL provide the public
ingress gateway exposing HTTP on port 8080 and TLS on port 8443, routing traffic to `server:8080`,
performing active health checks against `/api/admin/v1/health`, and stripping the W3C `traceparent`
header from responses returned to clients. Docker Compose network isolation SHALL segment containers
into an `edge` network connecting `haproxy` and `server`, and an `internal` network connecting
`server`, `seed`, `migrate`, `postgres`, and `rabbitmq`. Neither `server` nor `rabbitmq` SHALL
publish any ports to the host network.

#### Scenario: Sequential container startup ordering
- **GIVEN** a clean Docker Compose environment with no running containers
- **WHEN** the compose stack is launched with `docker compose up`
- **THEN** the `postgres` and `rabbitmq` containers start first and perform healthcheck polling
- **AND** the `migrate` container runs `bin/server migrate` against the database once `postgres` is
  healthy
- **AND** the `seed` container runs the test data kit against the database once `migrate` terminates
  successfully with exit status 0
- **AND** the `server` container launches `bin/server server` only after `rabbitmq` is healthy and
  both `migrate` and `seed` terminate successfully with exit status 0
- **AND** the `haproxy` container launches, binds ports 8080 and 8443 on the host, and proxies
  requests to `server:8080`
- **AND** neither the `server` container nor the `rabbitmq` container publishes ports directly to
  the host network

#### Scenario: Ingress strips traceparent header from client response
- **GIVEN** the Docker Compose stack is running with `haproxy` and `server`
- **WHEN** a client sends an HTTP GET request to `http://localhost:8080/`
- **THEN** the request is forwarded by HAProxy to the backend server
- **AND** the backend server generates an internal W3C `traceparent` header
- **AND** HAProxy strips the `traceparent` header before returning the HTTP 200 response to the
  client

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

### Requirement: Gradle Lifecycle Tasks for Compose Orchestration
The root Gradle build SHALL define lifecycle tasks for managing the containerized environment:
- `generateComposeEnv`: Ensures local `.env` exists with generated `NJALL_DB_SECRET`.
- `generateComposeTls`: Ensures local development TLS certificate
  (`docker/haproxy/certs/haproxy.pem`) exists, executing `docker/haproxy/generate-certs.sh` if
  absent.
- `composeStart`: Ensures local `.env` and TLS certificate exist, compiles the server distribution
  via `:server:installDist`, and runs `docker compose up --build -d` in detached background mode.
- `composeStop`: Executes `docker compose down` to stop containers and remove application networks.
- `composeStopClean`: Executes `docker compose down -v` to stop containers, delete persistent
  database and RabbitMQ volumes, and delete both the generated `.env` file and
  `docker/haproxy/certs/haproxy.pem`.
- `composeLogs`: Executes `docker compose logs -f` to follow service logs.

#### Scenario: Launching the system via composeUp
- **GIVEN** the workspace source files have been modified
- **WHEN** `./gradlew composeStart` is executed
- **THEN** Gradle ensures `.env` is present, generating a random 16-byte hex secret if absent
- **AND** Gradle ensures `docker/haproxy/certs/haproxy.pem` is present, generating a self-signed TLS
  cert bundle via `docker/haproxy/generate-certs.sh` if absent
- **AND** Gradle executes `:server:installDist` to incrementally recompile and stage application
  binaries
- **AND** Docker builds the application image by copying staged distribution artifacts
- **AND** Docker Compose launches `postgres`, `rabbitmq`, `migrate`, `seed`, `server`, and `haproxy`
  services in detached background mode

#### Scenario: Tearing down the system via composeDown
- **GIVEN** the Docker Compose stack is running
- **WHEN** `./gradlew composeStop` is executed
- **THEN** `docker compose down` is executed
- **AND** all containers and networks (`edge`, `internal`) are stopped and removed
- **AND** `.env` and TLS certificates are preserved for subsequent runs

#### Scenario: Resetting the system via composeClean
- **GIVEN** a running or stopped Docker Compose stack with persistent data and broker volumes,
  `.env`, and TLS certificates
- **WHEN** `./gradlew composeStopClean` is executed
- **THEN** `docker compose down -v` is executed to remove containers and volumes
- **AND** the local `.env` file is deleted
- **AND** the local `docker/haproxy/certs/haproxy.pem` file is deleted
