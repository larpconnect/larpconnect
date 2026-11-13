## ADDED Requirements

### Requirement: Users Schema Entity Generator Column Rename Migration
The system SHALL apply Flyway migration `V13__rename_entities_owner_to_generator.sql` renaming `njall_users.entities.owner` to `generator`, renaming constraint `fk_entities_owner` to `fk_entities_generator`, and replacing `idx_entities_owner` with `idx_entities_generator` on `(tenant_id, generator DESC)`, while leaving `njall_users.collections` (`owner_id`, `fk_collections_owner`, `unq_collections_owner_name`, `idx_collections_owner`) unchanged for semantic **Collections** **owner** attachment.

#### Scenario: Migration renames entities owner column, constraint, and index to generator
- **GIVEN** a database with migrations through V12 applied
- **WHEN** migration V13 executes
- **THEN** column `generator UUID NOT NULL` exists on `njall_users.entities` and column `owner` no longer exists on `njall_users.entities`
- **AND** composite foreign key constraint `fk_entities_generator` references `njall_users.entities (tenant_id, id) ON DELETE CASCADE`
- **AND** index `idx_entities_generator` is active on `njall_users.entities (tenant_id, generator DESC)`
- **AND** column `owner_id`, constraint `fk_collections_owner`, constraint `unq_collections_owner_name`, and index `idx_collections_owner` remain active on `njall_users.collections`

#### Scenario: Self-generated actor entity satisfies renamed composite foreign key constraint
- **GIVEN** a database with migration V13 applied and an active **Studio** **Tenant** `tenant1`
- **WHEN** a row is inserted into `njall_users.entities` with `id = actorId` and `generator = actorId` under `tenant1`
- **THEN** the insert succeeds and satisfies `fk_entities_generator`

#### Scenario: Cross-tenant entity generator reference is rejected by composite foreign key
- **GIVEN** **Studio** **Tenant** records `tenant1` and `tenant2` and an **Actor** `actor1` persisted under `tenant1`
- **WHEN** an insert into `njall_users.entities` under `tenant2` specifies `generator = actor1`
- **THEN** the database rejects the insert with a foreign key constraint violation on `fk_entities_generator`
