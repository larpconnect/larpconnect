# 0031: Docker Compose Test Data Seeding and Testing Kit Architecture

## Status

Accepted

## Date

2026-10-01

## Context

Project Njall uses Docker Compose to orchestrate local development infrastructure, including PostgreSQL (PostGIS), Flyway schema migrations, backend HTTP services, and HAProxy ingress. While Flyway migrations establish schemas, tables, and constraints up to version V5, the resulting database contains zero tenant data. Developers and testers interacting with the local system must manually execute administrative REST requests to configure test studios, admin accounts, role assignments, links, and locations before testing endpoints.

Additionally, test fixtures must not pollute application production code in `:server` or production migration scripts in `:data`, must execute with proper database privileges without exposing superuser `njall` credentials, and must remain strictly idempotent across repeated container restarts.

## Considered Options

- **Option 1: Auxiliary `seed` CLI subcommand in `ServerApp`** (Rejected: embeds testing fixtures into production distribution binaries).
- **Option 2: Secondary Flyway migration location in `migrate` service** (Rejected: creates potential leakage into production deployments and couples test fixtures to Flyway schema history).
- **Option 3: Ephemeral `seed` container in Docker Compose executing modular SQL scripts** (Selected).

## Decision

1. **Adopt Ephemeral `seed` Service in Docker Compose**: Introduce a lightweight `seed` service reusing the `postgis/postgis:18-3.6-alpine` image positioned sequentially between `migrate` and `server`.
2. **Execute as `njall_admin` Role**: The `seed` container authenticates as `njall_admin` using derived credentials (`njall_admin_${NJALL_DB_SECRET}`), leveraging its granted permissions (`GRANT ALL`) and Row-Level Security bypass (`FOR ALL TO njall_admin USING (true) WITH CHECK (true)`).
3. **Modular Testing Kit**: Establish `docker/postgres/seed/` with numbered SQL files executed in lexicographical order, allowing developers to extend the testing kit simply by adding new SQL scripts.
4. **Strict Idempotency**: All test fixtures use fixed deterministic UUIDv7 and UUIDv4 constants paired with `ON CONFLICT (...) DO NOTHING` clauses, ensuring subsequent container runs without volume recreation do not duplicate records or produce errors.
5. **Naming Conventions**: Enforce "Larp" (capitalized word, not all-caps acronym) and explicit inclusion of "example" or "fake" across all test entities.

## Consequences

- **Positive**: Zero-config turnkey local environment with two pre-configured studios and complete domain profiles; test fixtures completely isolated from production application binaries and migrations; easily extensible.
- **Negative**: Adds an ephemeral container execution step (~1 second) to Docker Compose startup sequence.
- **Follow-up**: Maintain testing kit SQL fixtures as new domain entities (events, characters) are introduced.
