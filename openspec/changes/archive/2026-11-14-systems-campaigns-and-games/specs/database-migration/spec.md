## ADDED Requirements

### Requirement: Users Schema Event Type Enum and Events Column Migration
The system SHALL apply `V14__systems_campaigns_and_games.sql` creating `njall_users.tevent_type` (`'EVENT_TYPE_UNSPECIFIED'`, `'GAME'`, `'WORKSHOP'`, `'CONFERENCE'`, `'MEETUP'`), adding `event_type njall_users.tevent_type NOT NULL DEFAULT 'EVENT_TYPE_UNSPECIFIED'` to `njall_users.events`, and granting `USAGE` on `njall_users.tevent_type` to `njall_users` and `njall_admin`.

#### Scenario: Migration V14 creates tevent_type enum and events.event_type column
- **GIVEN** a PostgreSQL database with migrations through `V13` applied
- **WHEN** Flyway migration `V14__systems_campaigns_and_games.sql` executes
- **THEN** enum `njall_users.tevent_type` exists with values `EVENT_TYPE_UNSPECIFIED`, `GAME`, `WORKSHOP`, `CONFERENCE`, and `MEETUP`
- **AND** column `event_type` exists on `njall_users.events` with default `'EVENT_TYPE_UNSPECIFIED'`

### Requirement: Users Schema Systems, Campaigns, and Games CTI Tables and RLS Migration
Migration `V14__systems_campaigns_and_games.sql` SHALL create CTI tables `njall_users.systems` (referencing `entities`, unique `(tenant_id, lower(name))`), `njall_users.campaigns` (referencing `entities`, nullable `system_id` `ON DELETE SET NULL`), and 2nd-level CTI `njall_users.games` (referencing `events`, required `system_id`, nullable `campaign_id` `ON DELETE SET NULL`), with lookup indices, Row-Level Security (`app.tenant_id` for `njall_users`, bypass for `njall_admin`), and grants.

#### Scenario: Case-insensitive unique index on systems rejects duplicate tenant system names
- **GIVEN** a database with migration `V14` applied, an active **Studio** **Tenant** `tenant1`, and an **Actor** `actor1`
- **AND** a system named "Call of Cthulhu" is inserted under `tenant1`
- **WHEN** an insert into `njall_users.systems` under `tenant1` specifies `name = 'call of cthulhu'`
- **THEN** PostgreSQL rejects the insert with a unique constraint violation on `idx_systems_tenant_name`

#### Scenario: Cross-tenant system or campaign association on games is rejected by composite foreign keys
- **GIVEN** studio tenants `tenant1` and `tenant2`, with a **System** `system2` persisted under `tenant2`
- **WHEN** an insert into `njall_users.games` under `tenant1` specifies `system_id = system2`
- **THEN** PostgreSQL rejects the insert with a foreign key violation on `fk_games_system`

#### Scenario: Row-Level Security isolates systems, campaigns, and games across tenants
- **GIVEN** records persisted in `njall_users.systems`, `njall_users.campaigns`, and `njall_users.games` for both `tenant1` and `tenant2`
- **WHEN** a session connecting as `njall_users` sets `app.tenant_id` to `tenant1` and queries all three tables
- **THEN** only rows belonging to `tenant1` are visible and rows belonging to `tenant2` are filtered out by Row-Level Security
