# 0003: Data Module and Flyway Migration Architecture

## Status

Accepted

## Date

2026-09-13

## Context

Project Njall requires persistent data storage to manage servers, contacts, tenanted user structures, and domain entities. While the core application runtime is powered by Apache Pekko Typed HTTP services, persistence needs a clear architectural boundary. Mixing asynchronous actor pipelines directly with relational database drivers or database migrations can introduce thread pool contention, unwanted coupling, and credential exposure.

Additionally, schema evolution requires automated, deterministic migration management with strict role-based access control across four designated roles:
- `njall`: Administrative role used solely for migrations and DDL.
- `njall_admin`: Application management role at server scope.
- `njall_users`: Tenanted runtime access role.
- `njall_system`: Downstream analytics role.

## Considered Options

- **Option 1: Dedicated `:data` library module with Flyway and transient administrative connections** (Selected)
- **Option 2: Embed persistence and migrations inside `:server`** (Rejected: violates module boundaries and hinders independent testing of the data layer)
- **Option 3: Use Pekko Persistence / event sourcing** (Rejected: relational model with PostgreSQL 18+ and PostGIS is explicitly required for tenant and server topology)

## Decision

1. **Module Isolation**: Establish a dedicated `:data` library module (`com.larpconnect.njall.data`). This module will not depend on Apache Pekko or use actors.
2. **Flyway Migration Runner**: Use Flyway 13+ with PostgreSQL 18+ and PostGIS support.
3. **Transient Administrative Connection**: The `njall` role credentials are used exclusively during migration runs. Connections and data sources are created, executed, closed, and dereferenced immediately. No persistent connection pool is maintained for `njall`.
4. **Command-Line Migration Trigger**: Provide a `--migrate` CLI flag on `ServerApp` to execute migrations and exit immediately. Standard server launch without `--migrate` never initiates connections as `njall`.
5. **UUID Strategy**: Use `gen_random_uuid()` (UUIDv4) for public-facing server entities to avoid leaking timestamp metadata, while utilizing native `uuidv7()` for internal, chronologically clustered entities (`server_contacts`).

## Consequences

- **Positive**: Complete decoupling between persistence and Pekko actor runtime; reduced security exposure by keeping administrative database credentials transient; deterministic and verifiable schema evolution.
- **Negative**: Requires separate CLI invocation or orchestration step for database migrations during deployment.
- **Follow-up**: Implement Hibernate CTI entity models and tenant-aware DAOs within `:data` in subsequent changes.
