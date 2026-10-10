# reactions-schema Specification

## Purpose

Flyway database migration V7__reactions.sql provisioning the njall_users.reactions subordinate table, PostgreSQL Row-Level Security (RLS) policies, indexes, and the njall_users.reaction_counts materialized view supporting concurrent refreshes and cross-Studio aggregation.

## Requirements

### Requirement: Reactions Schema Definition and Foreign Keys
The system SHALL apply Flyway database migration `V7__reactions.sql` to initialize the `njall_users.reactions` subordinate table. The table SHALL declare composite primary key `(tenant_id, id)` where `id` defaults to `uuidv7()` and enforces unique constraint `unq_reaction_id` on `id`. The table SHALL define mandatory foreign key `(tenant_id, target_id)` referencing `njall_users.entities(tenant_id, id)` with `ON DELETE CASCADE`, optional foreign key `(tenant_id, link_id)` referencing `njall_users.links(tenant_id, id)` with `ON DELETE SET NULL`, `reaction_type VARCHAR(128) NOT NULL`, and `created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.

#### Scenario: Subordinate reaction record creation and foreign key resolution
- **GIVEN** an active target **Entity** and an optional media **Link** exist for a **Tenant**
- **WHEN** a **Reaction** record is inserted into `njall_users.reactions` with the corresponding `target_id` and `link_id`
- **THEN** the record is persisted with a generated UUIDv7 identifier, valid audit timestamp, and referential integrity to both target entity and link

#### Scenario: Cascade deletion upon hard deletion of target entity
- **GIVEN** an active target **Entity** has associated **Reaction** records
- **WHEN** the target **Entity** row is deleted from `njall_users.entities`
- **THEN** all associated reaction records for that target entity are automatically cascade deleted

#### Scenario: Set null upon deletion of referenced reaction media link
- **GIVEN** a **Reaction** record references a custom emoji **Link** via `link_id`
- **WHEN** the referenced **Link** row is deleted from `njall_users.links`
- **THEN** the reaction record remains intact with its `link_id` set to `NULL`

### Requirement: Reactions Row-Level Security and Role Privileges
The system SHALL enable Row-Level Security (RLS) on `njall_users.reactions`. The system SHALL install RLS policy `rls_reactions` for role `njall_users` restricting rows to `tenant_id = current_setting('app.tenant_id', true)::uuid`. The system SHALL install RLS policy `rls_reactions_admin` for role `njall_admin` permitting all operations without tenant restriction. The system SHALL grant `SELECT, INSERT, UPDATE, DELETE` on `njall_users.reactions` to role `njall_users`, and grant `ALL` to role `njall_admin`.

#### Scenario: Tenant isolation enforcements for reactions
- **GIVEN** reactions exist across multiple **Tenant** studios in the database
- **WHEN** a session authenticated as `njall_users` sets `app.tenant_id` to a specific **Tenant** UUID and queries `njall_users.reactions`
- **THEN** only reaction records matching the configured `tenant_id` are returned

#### Scenario: Administrative bypass of tenant boundary
- **GIVEN** reactions exist across multiple **Tenant** studios in the database
- **WHEN** an administrative session authenticated as `njall_admin` queries `njall_users.reactions`
- **THEN** reaction records across all tenants are accessible

### Requirement: Materialized View for Pre-Aggregated Reaction Counts
The system SHALL create the Materialized View `njall_users.reaction_counts` pre-aggregating reaction counts for active entities (`WHERE E.deleted_on IS NULL`) joined with optional link metadata (`url`, `media_type`, `link_type`) from `njall_users.links`. The system SHALL define a `UNIQUE` index on `(tenant_id, target_id, reaction_type, link_id) NULLS NOT DISTINCT` to support non-blocking `REFRESH MATERIALIZED VIEW CONCURRENTLY`. The system SHALL define read-optimized indexes on `(tenant_id DESC, target_id DESC)` for single-target payload resolution and on `(reaction_type, count DESC)` for cross-**Studio** popular reaction queries. The system SHALL grant `SELECT` on `njall_users.reaction_counts` to `njall_users` and `ALL` to `njall_admin`.

#### Scenario: Aggregation excludes soft-deleted entities
- **GIVEN** reaction records exist for both active entities and soft-deleted entities (`deleted_on IS NOT NULL`)
- **WHEN** the materialized view is refreshed
- **THEN** reaction counts are aggregated exclusively for active entities where `deleted_on IS NULL`

#### Scenario: Non-blocking concurrent refresh execution
- **GIVEN** the `njall_users.reaction_counts` materialized view is populated with reaction data
- **WHEN** an administrative connection executes `REFRESH MATERIALIZED VIEW CONCURRENTLY njall_users.reaction_counts`
- **THEN** the command succeeds without error and without locking concurrent `SELECT` queries on the view

#### Scenario: Cross-studio visibility on aggregated reaction counts
- **GIVEN** multiple **Studio** tenants have reactions recorded in `njall_users.reactions`
- **WHEN** `njall_users.reaction_counts` is queried for popular or trending reactions
- **THEN** pre-aggregated counts reflect activity across studios without requiring live table joins
