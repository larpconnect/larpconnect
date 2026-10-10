# 0035: Tenanted Events and Location Associations Architecture

## Status

Accepted

## Date

2026-10-10

## Context

In **Njall**, gaming organizations (**Studios**) manage gathering schedules, games, and sessions. Following the multi-single-tenant data isolation architecture established in [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md), [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md), and [0029: Tenanted Locations and Addresses Architecture](0029-tenanted-locations-and-addresses-architecture.md), the system requires first-class models and API endpoints for managing tenanted **Events**.

Scheduled events may optionally take place at a defined venue (**Locations**). The architecture must guarantee tenant isolation across entity relationships, enforce chronological integrity on event intervals, support soft deletion without cascading historical data loss, and expose clean RESTful endpoints adhering to Google AIP standards.

## Considered Options

- **Option 1: Events as CTI Subtype with Composite Foreign Key to Locations and Check Constraint** (Selected)
  - `njall_users.events` inherits from `njall_users.entities(tenant_id, id)` via CTI.
  - Foreign key `(tenant_id, location_id)` references `njall_users.locations(tenant_id, id) ON DELETE SET NULL`, enforcing that referenced locations belong strictly to the same studio tenant at the database engine level.
  - Check constraint `chk_events_time_order CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)` guarantees chronological validity.
  - REST endpoints mounted under `/api/studios/{studio-id}/v1/events[/{id}]` supporting create, list, get, AIP-134 patch, and soft delete.
- **Option 2: Standalone Events Table without CTI** (Rejected: duplicates entity audit fields `created_on`, `updated_on`, `deleted_on`, and precludes polymorphic association with federated hashtags and value reactions).
- **Option 3: Single-Column Foreign Key on `location_id`** (Rejected: fails to enforce tenant boundary isolation at the database constraint level, requiring application-layer verification to prevent cross-tenant associations).

## Decision

1. **CTI for Events**: `njall_users.events` inherits from `njall_users.entities` using primary key `(tenant_id, id)`. Deletion is soft-deleted via `entities.deleted_on`.
2. **Tenant-Safe Location Reference**: Events reference locations via composite foreign key `(tenant_id, location_id) REFERENCES njall_users.locations (tenant_id, id) ON DELETE SET NULL`. Deleting a location clears the reference without deleting the event.
3. **Storage-Level Temporal Validation**: Chronological ordering is enforced via database constraint `chk_events_time_order` alongside application validation in `EventValidation`.
4. **Row-Level Security**: Row-Level Security is enabled on `njall_users.events` enforcing `tenant_id = current_setting('app.tenant_id', true)::uuid` with bypass for `njall_admin`.
5. **REST Route Pattern**: Expose `/api/studios/{studio-id}/v1/events` (POST, GET) and `/api/studios/{studio-id}/v1/events/{id}` (GET, PATCH, DELETE) via `EventsRoute` and Pekko Typed `EventActor`.

## Consequences

- **Positive**: Complete tenant isolation guaranteed at both SQL constraint and RLS levels; events automatically support hashtags and reactions via CTI; soft deletion preserves audit history.
- **Negative**: Foreign key constraint requires composite primary keys across joined tables; events referencing deleted locations retain a historical reference until explicit dissociation or purge.
