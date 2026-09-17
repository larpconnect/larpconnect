# 0007: Docker Compose Multi-Container Orchestration and Staged Packaging

## Status

Accepted

## Date

2026-09-16

## Context

Project Njall requires a reliable, reproducible, and deterministic mechanism to run the full application topology locally and in automated CI environments. Running the system requires:
1. PostgreSQL 18+ with the PostGIS 3.6 extension.
2. Initialized database roles (`njall`, `njall_admin`, `njall_users`, `njall_system`) with appropriate privileges.
3. Executing Flyway schema migrations via the `migrate` CLI subcommand.
4. Launching the HTTP server runtime via the `server` CLI subcommand.

Prior to this decision, developers had to manually stand up and configure PostgreSQL, or rely solely on Testcontainers during test suite execution. Furthermore, building Docker containers with nested Gradle invocations introduced significant latency on developer machines.

## Considered Options

- **Option 1: Docker Compose with Host-Staged Packaging and Ephemeral Migration Service** (Selected)
- **Option 2: Combined Monolithic Container Entrypoint (`migrate && exec server`)** (Rejected: obscures migration failure states and complicates independent scaling)
- **Option 3: In-Container Multi-Stage Gradle Build** (Rejected: ignores Gradle daemon caching, leading to 30-60+ second rebuild latency)

## Decision

1. **Adopt Docker Compose for System Orchestration**: Define a standardized `docker-compose.yml` comprising:
   - `postgres`: Container running `postgis/postgis:18-3.6-alpine` with healthcheck polling via `pg_isready`.
   - `migrate`: Ephemeral container that executes `bin/server migrate` once `postgres` is healthy and exits with status 0 (`restart: "no"`).
   - `server`: Persistent container executing `bin/server server` that launches only after `migrate` completes successfully.
2. **Automated Database Initialization**: Mount initialization SQL in `/docker-entrypoint-initdb.d/` to configure roles (`njall`, `njall_admin`, `njall_users`, `njall_system`), grant role administration options to `njall`, and enable PostGIS on first cluster startup.
3. **Host-Driven Packaging (`installDist`)**: The host Gradle process builds and stages the distribution via `:server:installDist`. The runtime `Dockerfile` copies this pre-staged directory directly into `ghcr.io/rblaine95/eclipse-temurin:25`, enabling sub-3-second local rebuilds.
4. **Gradle Lifecycle Task Integration**: Standardize on root Gradle tasks (`composeUp`, `composeDown`, `composeStart`, `composeLogs`, `composeClean`) for managing container lifecycles.

## Consequences

- **Positive**: Zero-config turnkey system boot; clear separation between migration execution and server runtime; fast incremental rebuilds via host Gradle daemon.
- **Negative**: Requires Docker / Docker Compose installed and running on the host machine; developer must stage `:server:installDist` before executing raw `docker compose` commands directly.
