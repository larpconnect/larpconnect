## ADDED Requirements

### Requirement: Users Schema Entities and Links CTI Migration
The system SHALL apply Flyway migration `V4__entities_and_links_cti.sql` to initialize the Common Table Inheritance (CTI) root `njall_users.entities` table and the `njall_users.links` subtype table. The migration SHALL define `njall_users.entities` with composite primary key `(tenant_id, id)` where `id` defaults to `uuidv7()`, `tenant_id` references `njall_users.studios(id)`, and audit columns `entity_type VARCHAR NOT NULL`, `summary VARCHAR NULL`, `created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, and `deleted_on TIMESTAMPTZ NULL DEFAULT NULL`. The migration SHALL define `njall_users.links` with composite primary key `(tenant_id, id)` where `(tenant_id, id)` references `njall_users.entities(tenant_id, id)` with `ON DELETE CASCADE`, and subtype columns `link_type VARCHAR(128) NOT NULL`, `url VARCHAR NOT NULL`, and `media_type VARCHAR NOT NULL`. The migration SHALL define indices `idx_entities_active` on `(tenant_id DESC, id DESC) INCLUDE (entity_type) WHERE deleted_on IS NULL` and `idx_entities_type_lookup` on `(tenant_id DESC, entity_type ASC, id DESC) WHERE deleted_on IS NULL`. The migration SHALL enable Row-Level Security on both `entities` and `links`, define RLS policy `rls_entities` and `rls_links` filtering by `tenant_id = current_setting('app.tenant_id', true)::uuid` for `njall_users`, define bypass policies for `njall_admin`, and grant permissions to both roles.

#### Scenario: Migration provisions entities and links tables with CTI constraints
- **GIVEN** a database with migrations through V3 applied
- **WHEN** migration V4 executes
- **THEN** tables `njall_users.entities` and `njall_users.links` exist
- **AND** composite primary keys `(tenant_id, id)` are established on both tables
- **AND** foreign key constraint references `njall_users.entities(tenant_id, id)` from `njall_users.links`
- **AND** indices `idx_entities_active` and `idx_entities_type_lookup` are active

#### Scenario: Row-Level Security restricts entity and link access by tenant
- **GIVEN** records in `njall_users.entities` and `njall_users.links` for tenants `tenant1` and `tenant2`
- **WHEN** a session connecting as `njall_users` sets `app.tenant_id` to `tenant1`
- **THEN** only rows matching `tenant1` are returned from `entities` and `links`
- **AND** rows for `tenant2` are filtered out
