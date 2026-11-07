## ADDED Requirements

### Requirement: Users Schema Entity Owner Provenance Migration
The system SHALL apply Flyway migration `V12__entities_owner.sql` adding `owner UUID NOT NULL` to `njall_users.entities` with composite self-referencing foreign key `fk_entities_owner` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE` and index `idx_entities_owner` on `(tenant_id, owner DESC)`. The migration SHALL drop `idx_actors_owner`, `fk_actors_owner`, and `owner_id` from `njall_users.actors` while retaining `njall_users.collections.owner_id`.

#### Scenario: Migration adds owner column and self-referencing constraint on entities
- **GIVEN** a database with migrations through V11 applied
- **WHEN** migration V12 executes
- **THEN** column `owner UUID NOT NULL` exists on `njall_users.entities`
- **AND** composite foreign key constraint `fk_entities_owner` references `njall_users.entities (tenant_id, id) ON DELETE CASCADE`
- **AND** index `idx_entities_owner` is active on `njall_users.entities (tenant_id, owner DESC)`
- **AND** column `owner_id`, constraint `fk_actors_owner`, and index `idx_actors_owner` no longer exist on `njall_users.actors`
- **AND** column `owner_id` remains active on `njall_users.collections`

#### Scenario: Self-owned actor entity satisfies composite foreign key constraint
- **GIVEN** a database with migration V12 applied and an active studio tenant `tenant1`
- **WHEN** a row is inserted into `njall_users.entities` with `id = actorId` and `owner = actorId` under `tenant1`
- **THEN** the insert succeeds and satisfies `fk_entities_owner`

#### Scenario: Cross-tenant entity owner reference is rejected by composite foreign key
- **GIVEN** studio tenants `tenant1` and `tenant2` and an **Actor** `actor1` persisted under `tenant1`
- **WHEN** an insert into `njall_users.entities` under `tenant2` specifies `owner = actor1`
- **THEN** the database rejects the insert with a foreign key constraint violation on `fk_entities_owner`
