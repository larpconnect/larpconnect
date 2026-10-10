## Why

In **Njall**, studios require first-class support for scheduling and publishing gatherings, sessions, and live-action games. Currently, studios can define venues and addresses via **Locations**, but lack domain models and API endpoints for representing scheduled gatherings. Introducing tenanted **Events** expands the **User plane** to support event management while preserving strict multi-tenant Row-Level Security (RLS) and Common Table Inheritance (CTI) boundaries.

## What Changes

- Introduce a new Flyway migration `V8__events.sql` in the **Data plane**:
  - `njall_users.events`: Subtype table inheriting from `njall_users.entities` via CTI with `(tenant_id, id)` primary key and foreign key reference, containing `location_id UUID NULL`, `title VARCHAR NOT NULL`, `start_time TIMESTAMPTZ NULL`, and `end_time TIMESTAMPTZ NULL`.
  - Add foreign key `CONSTRAINT fk_events_locations FOREIGN KEY (tenant_id, location_id) REFERENCES njall_users.locations (tenant_id, id) ON DELETE SET NULL`.
  - Add check constraint `CONSTRAINT chk_events_time_order CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)`.
  - Add index `idx_events_location` on `(tenant_id, location_id)` and `idx_events_time` on `(tenant_id DESC, start_time ASC, end_time ASC)`.
  - Enable PostgreSQL Row-Level Security (RLS) on `njall_users.events` with tenant isolation for `njall_users` and administrative bypass for `njall_admin`.
- Implement immutable domain records and DAOs in `:data`:
  - `Event`: Domain record implementing `DatabaseObject` containing `id`, `locationId`, `title`, `summary`, `startTime`, `endTime`, `createdOn`, `updatedOn`, and `deletedOn`.
  - `EventEntity`: JPA entity mapping `njall_users.events` with `@IdClass(EntityId.class)`.
  - `EventDAO` / `DefaultEventDAO`: Interface and implementation providing tenanted CRUD operations (`findById`, `listAll`, `create`, `patch`, `softDelete`) under active `@NjallUsers` sessions.
- Implement Pekko Typed actors and HTTP routes in `:api`:
  - `EventCommand`: Sealed interface defining actor commands (`CreateEvent`, `GetEvent`, `ListEvents`, `PatchEvent`, `DeleteEvent`).
  - `EventActor`: Actor executing event operations, validating required title, checking temporal ordering, and translating database referential integrity violations into bad request responses.
  - `EventsRoute`: HTTP route mounted under `/api/studios/{studio-id}/v1/events` resolving studio tenants via `StudioLookupCache`.
- Expose tenanted HTTP REST endpoints in `:api`:
  - `POST   /api/studios/{studio-id}/v1/events` (Create Event)
  - `GET    /api/studios/{studio-id}/v1/events` (List all active events for the studio)
  - `GET    /api/studios/{studio-id}/v1/events/{id}` (Retrieve single active event details)
  - `PATCH  /api/studios/{studio-id}/v1/events/{id}` (Partial update via Google AIP-134 update_mask)
  - `DELETE /api/studios/{studio-id}/v1/events/{id}` (Soft-delete event via `entities.deleted_on`)
- Document endpoints and payload schemas in `openapi.yaml`.

## Capabilities

### New Capabilities
- `user-studio-events-api`: Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/events` for creating, listing, retrieving, updating, and soft-deleting studio events using Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS).

### Modified Capabilities
None.

## Impact

- **Database**: Adds `V8__events.sql` migration creating `njall_users.events` with foreign key references to `entities` and `locations`, indices, and RLS policies.
- **Data Module (`:data`)**: Adds `Event` domain record, `EventEntity`, `EventDAO`, and `DefaultEventDAO`.
- **API Module (`:api`)**: Adds `EventsRoute`, `EventActor`, request/response DTOs, and updates `openapi.yaml`.
- **Integration Module (`:integration`)**: Adds Cucumber feature specifications and step definitions verifying API-to-queue and queue-to-data boundaries.
- **External Contracts**: Extends the public OpenAPI contract with non-breaking endpoints under `/api/studios/{studio-id}/v1/events`.
