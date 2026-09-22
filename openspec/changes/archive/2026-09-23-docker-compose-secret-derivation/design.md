## Context

The **Njall** system runtime leverages Docker Compose to orchestrate multi-container environments comprising PostgreSQL 18+ with PostGIS, Flyway schema migration execution, and HTTP **Server** startup (codified in repository ADR [0007-docker-compose-and-container-orchestration.md](../../../../adr/0007-docker-compose-and-container-orchestration.md)).

Prior to this change, default passwords (`postgres`, `njall`, `njall_admin`, `njall_users`) were hardcoded directly in `docker-compose.yml` and the PostgreSQL initialization script (`docker/postgres/init/01-init.sql`). Furthermore, [`reference.conf`](../../../../common/src/main/resources/reference.conf) provided non-empty fallback strings for database passwords. This configuration violates repository hygiene, triggers automated security scanners, risks credential leakage in process inspection and container metadata, and lacks protection against inadvertent commits of local environment configurations.

## Goals / Non-Goals

**Goals:**
- Eliminate all hardcoded plaintext passwords from tracked repository files (`docker-compose.yml`, SQL scripts, configuration templates).
- Exclude `.env` and `.env.*` files from version control via `.gitignore` while committing a sanitized `.env.example`.
- Automate generation of a cryptographically secure random master secret seed (`NJALL_DB_SECRET`) during `./gradlew composeUp` and `./gradlew composeStart`.
- Derive distinct, isolated passwords for each database role (`njall_migration_<secret>`, `njall_admin_<secret>`, `njall_users_<secret>`, `njall_system_<secret>`) to ensure cross-role credential mistakes fail fast.
- Dynamically provision database roles in PostgreSQL using an executable `/docker-entrypoint-initdb.d/01-init.sh` script.
- Synchronize `.env` lifecycle with the PostgreSQL volume lifecycle, removing `.env` when `./gradlew composeClean` purges database volumes.

**Non-Goals:**
- Implementing production secret manager integrations (e.g., HashiCorp Vault, AWS Secrets Manager) for local testing.
- Modifying Java application code or introducing Docker Swarm file-based secret mounts (`/run/secrets/`).
- Changing Testcontainers integration test credential generation in `:integration`, which already generates secure random passwords dynamically.

## Architecture

The following C4 Container diagram visualizes the runtime boundaries, secret generation, and service startup sequence:

```
+-------------------------------------------------------------------------------------------------+
|                                C4 CONTAINER RUNTIME ARCHITECTURE                                |
+-------------------------------------------------------------------------------------------------+
|                                                                                                 |
|   +-----------------------------------------------------------------------------------------+   |
|   | Host Environment (Developer / CI Runner)                                                |   |
|   |                                                                                         |   |
|   |   +---------------------------------------------------------------------------------+   |   |
|   |   | Gradle Build Lifecycle (:root)                                                  |   |   |
|   |   |                                                                                 |   |   |
|   |   |   [composeStart / composeUp]                                                    |   |   |
|   |   |              |                                                                  |   |   |
|   |   |              v                                                                  |   |   |
|   |   |   (generateComposeEnv) ----[Creates if missing]----> [.env (Git-ignored)]       |   |   |
|   |   |              |                                       - NJALL_DB_SECRET=<seed>   |   |   |
|   |   |              v                                       - POSTGRES_PASSWORD=...    |   |   |
|   |   |   (:server:installDist)                                                         |   |   |
|   |   |              |                                                                  |   |   |
|   |   |              v                                                                  |   |   |
|   |   |   [docker compose up --build]                                                   |   |   |
|   |   +--------------|------------------------------------------------------------------+   |   |
|   |                  |                                                                      |   |   |
|   +------------------|----------------------------------------------------------------------+   |
|                      | Interpolates NJALL_DB_SECRET and passes munged role credentials          |
|                      v                                                                          |
|   +-----------------------------------------------------------------------------------------+   |
|   | Docker Compose Runtime Stack                                                            |   |
|   |                                                                                         |   |
|   |   +--------------------------+                                                          |   |
|   |   | postgres                 |                                                          |   |
|   |   | Container                |                                                          |   |
|   |   |                          |                                                          |   |
|   |   |  - 01-init.sh (Mount)    |                                                          |   |
|   |   |    Provisions roles with |                                                          |   |
|   |   |    derived passwords     |                                                          |   |
|   |   +--------------------------+                                                          |   |
|   |                ^                                                                        |   |
|   |                | Healthcheck barrier (pg_isready)                                       |   |
|   |                |                                                                        |   |
|   |   +------------+-------------+                                                          |   |
|   |   | migrate                  |                                                          |   |
|   |   | Ephemeral Container      |                                                          |   |
|   |   |                          |                                                          |   |
|   |   |  - bin/server migrate    |                                                          |   |
|   |   |  - Role: njall           |                                                          |   |
|   |   |  - Pass: njall_migration |                                                          |   |
|   |   +--------------------------+                                                          |   |
|   |                ^                                                                        |   |
|   |                | Completed successfully barrier (exit code 0)                           |   |
|   |                |                                                                        |   |
|   |   +------------+-------------+                                                          |   |
|   |   | server                   |                                                          |   |
|   |   | Persistent Container     |                                                          |   |
|   |   |                          |                                                          |   |
|   |   |  - bin/server server     |                                                          |   |
|   |   |  - Admin Role: admin     |                                                          |   |
|   |   |  - Users Role: users     |                                                          |   |
|   |   |  - Port: 8080            |                                                          |   |
|   |   +--------------------------+                                                          |   |
|   |                                                                                         |   |
|   +-----------------------------------------------------------------------------------------+   |
|                                                                                                 |
+-------------------------------------------------------------------------------------------------+
```

### Architectural Analysis

- **Boundaries**: Clear boundary between host-side build automation (Gradle), local configuration persistence (`.env`), and container runtime isolation (Docker Compose).
- **Responsibilities**: Gradle ensures valid secrets exist before container launch; Docker Compose injects munged role credentials into container environments; PostgreSQL initializes roles dynamically via shell execution; the **Application module** consumes credentials via standard environment variables.
- **Relationships**: Services maintain strict dependency chaining: `postgres` -> `migrate` -> `server`.
- **Assumptions**: Host running Gradle has access to `java.security.SecureRandom` (standard in Java 25 LTS runtime); PostgreSQL Alpine image has standard `/bin/sh` and `psql` available for entrypoint initialization.
- **Open Questions**: None; the exploration resolved all credential munging and lifecycle integration questions.

## Decisions

### Decision 1: Master Secret Seed and Deterministic Role Derivation

- **Choice**: Generate a single 16-byte cryptographically secure random hexadecimal seed (`NJALL_DB_SECRET`) and derive distinct credentials for each database role using deterministic string munging:
  - Superuser / Engine: `postgres_${NJALL_DB_SECRET}`
  - Migration / DDL: `njall_migration_${NJALL_DB_SECRET}`
  - Admin Verticle: `njall_admin_${NJALL_DB_SECRET}`
  - User Verticle: `njall_users_${NJALL_DB_SECRET}`
  - System Verticle: `njall_system_${NJALL_DB_SECRET}`
- **Rationale**: Combining a single seed with deterministic derivation satisfies two key requirements: it keeps `.env` simple with minimal configuration surface, while ensuring every database role has a distinct password. If application code or configuration accidentally mixes role credentials (e.g., using user credentials for admin operations), PostgreSQL rejects the connection immediately.
- **Alternatives Considered**:
  - *Completely distinct random passwords per role in `.env`*: Rejected as overly verbose and error-prone for local development.
  - *Shared identical password across all roles*: Rejected because it allows cross-role authorization bugs to pass undetected in local testing.

### Decision 2: Git Exclusion and `.env.example` Template

- **Choice**: Add `.env` and `.env.*` to `.gitignore` while creating a tracked `.env.example` file.
- **Rationale**: Prevents accidental commits of local secrets, developer overrides, or machine-specific environment configurations. A committed `.env.example` provides self-documenting guidance on available configuration flags without exposing sensitive values.
- **Alternatives Considered**:
  - *Checking in dummy `.env` with committed default passwords*: Rejected because security scanners flag committed `.env` files regardless of contents.

### Decision 3: Dynamic Postgres Role Initialization via Shell Script

- **Choice**: Replace static `docker/postgres/init/01-init.sql` with an executable `docker/postgres/init/01-init.sh` script.
- **Rationale**: The official PostgreSQL container entrypoint automatically executes `.sh` scripts found in `/docker-entrypoint-initdb.d/`. Using a shell script allows reading `NJALL_DB_SECRET` and evaluating derived passwords dynamically when provisioning roles with `psql` on first cluster startup.
- **Alternatives Considered**:
  - *Pre-processing SQL files on host via `sed` / `envsubst`*: Rejected because it adds host tooling dependencies (sed/awk/bash on Windows/WSL) and risks leaving transformed SQL files containing secrets in the workspace.

### Decision 4: Gradle Lifecycle Alignment and Volume Clean Hook

- **Choice**: Introduce a `generateComposeEnv` task in root `build.gradle.kts` executed before `composeUp` and `composeStart`. Update `composeClean` to delete `.env` when tearing down persistent database volumes (`docker compose down -v`).
- **Rationale**: Preserves zero-friction developer ergonomics (`./gradlew composeStart` works immediately on fresh clones). Aligning `.env` deletion with volume purging ensures that credentials and data volumes remain synchronized: restarts reuse existing credentials, while full resets generate fresh credentials for the newly created database cluster.
- **Alternatives Considered**:
  - *Regenerating `.env` on every startup*: Rejected because regenerating `.env` without wiping volumes causes authentication failure against existing PostgreSQL clusters.

### Decision 5: Configuration Sanitization in Reference Config

- **Choice**: Update [`reference.conf`](../../../../common/src/main/resources/reference.conf) to use empty string fallback values (`password = ""`) for database credentials, relying on environment overrides (`${?LARPCONNECT_DATA_DATABASE_...}`).
- **Rationale**: Eliminates lingering plaintext passwords in configuration templates that could trigger security scanner heuristics.

## Risks / Trade-offs

- **[Risk] Running raw `docker compose up` without running Gradle first** -> **Mitigation**: If `.env` is missing when invoking raw `docker compose`, Compose outputs warnings. Document in README and `docker-compose.yml` comments that developers should run `./gradlew composeStart` or copy `.env.example` to `.env`.
- **[Risk] Line ending issues on Windows for mounted `01-init.sh`** -> **Mitigation**: Ensure `01-init.sh` is saved with Unix (`LF`) line endings and configured in `.gitattributes` to prevent CRLF corruption inside the Linux container.
- **[Risk] Inadvertent `.env` staging via git add** -> **Mitigation**: Explicit `.gitignore` entries for `.env` and `.env.*` with positive negation only for `!.env.example`.

## Migration Plan

1. Update `.gitignore` and add `.env.example`.
2. Replace `docker/postgres/init/01-init.sql` with executable `01-init.sh`.
3. Update `docker-compose.yml` to use variable interpolation and derived role credentials.
4. Update `build.gradle.kts` to add `generateComposeEnv` task and hook it into `composeUp`, `composeStart`, and `composeClean`.
5. Sanitize database password defaults in `common/src/main/resources/reference.conf`.
6. Verify via `./gradlew composeClean composeStart` that roles are created, migrations pass, and the server accepts requests.

## Open Questions

- None. ADR 0015 will codify this decision and amend ADR 0007 regarding static role initialization.
