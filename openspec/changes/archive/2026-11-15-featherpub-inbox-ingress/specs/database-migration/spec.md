## ADDED Requirements

### Requirement: Users Schema Inbox Activity Deduplication Table and RLS Migration
Flyway migration `V15__inbox_activity_deduplication.sql` SHALL create `njall_users.inbox_activities`
(`tenant_id`, `target_actor_id`, `activity_id`, `received_on`, `expires_on`, primary key
`(tenant_id, target_actor_id, activity_id)`, foreign key to `njall_users.actors (tenant_id, id) ON
DELETE CASCADE`, index `idx_inbox_activities_expires ON (tenant_id, expires_on)`) with Row-Level
Security (`app.tenant_id` for `njall_users`, bypass for `njall_admin`) and role grants.

#### Scenario: Migration V15 creates inbox_activities table, actor foreign key, and expiration index
- **GIVEN** a PostgreSQL database with migrations through `V14` applied
- **WHEN** Flyway migration `V15__inbox_activity_deduplication.sql` executes
- **THEN** table `njall_users.inbox_activities` exists with primary key
  `(tenant_id, target_actor_id, activity_id)`
- **AND** foreign key `fk_inbox_activities_actor` references `njall_users.actors (tenant_id, id) ON
  DELETE CASCADE`
- **AND** index `idx_inbox_activities_expires` is active on `(tenant_id, expires_on)`

#### Scenario: Duplicate inbox activity for same tenant and target actor is rejected by primary key
- **GIVEN** a database with migration `V15` applied, an active **Studio** **Tenant** `tenant1`, and
  an **Actor** `actor1`
- **AND** a row `(tenant1, actor1, 'https://remote.example/act/1')` exists in
  `njall_users.inbox_activities`
- **WHEN** a second raw `INSERT` without `ON CONFLICT` specifies the same
  `(tenant1, actor1, 'https://remote.example/act/1')`
- **THEN** PostgreSQL rejects the duplicate insert with a primary key constraint violation

#### Scenario: Row-Level Security isolates inbox_activities rows across tenants
- **GIVEN** deduplication rows persisted in `njall_users.inbox_activities` for both `tenant1` and
  `tenant2`
- **WHEN** a session connecting as `njall_users` sets `app.tenant_id` to `tenant1` and queries
  `njall_users.inbox_activities`
- **THEN** only rows belonging to `tenant1` are visible and rows belonging to `tenant2` are filtered
  out by Row-Level Security
