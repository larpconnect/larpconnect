## Context

In **Njall**, studios manage community and live-action events. While venue locations are modeled through [0029-tenanted-locations-and-addresses-architecture.md](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md), scheduled gatherings and activities currently lack first-class models. This design establishes tenanted **Events** as a Class Table Inheritance (CTI) subtype extending `njall_users.entities`, connected to physical **Locations** via composite foreign keys, and surfaced over asynchronous Pekko Typed routes adhering to Google AIP standards.

## Goals / Non-Goals

**Goals:**
- Provide tenanted persistence for **Events** under `njall_users.events` with PostgreSQL Row-Level Security (RLS) enforcement.
- Link events optionally to **Locations** within the same studio tenant using composite foreign keys.
- Enforce temporal ordering constraints (`end_time >= start_time`) at both the application and database layers.
- Expose tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/events` supporting creation, collection listing, detail retrieval, AIP-134 partial updates, and soft deletion.
- Adhere to the established repository standards for immutable records, sealed Pekko protocols, Guice module bindings, and JaCoCo coverage gates.

**Non-Goals:**
- Complex query filtering (e.g. date ranges, location filters, or full-text search) on collection listing for this phase.
- Event recurrence, ticketing, RSVP tracking, or scheduling conflict detection.
- Cross-tenant event federation protocols (reserved for future FeatherPub extensions).

## Architectural Diagrams (C4 Level 2 & 3)

### C4 Container Diagram
```
+─────────────────────────────────────────────────────────────────────────────+
|                                  Njall Host                                 |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                            :api Verticle                              |  |
|  |  +───────────────+     +────────────────+     +────────────────────+  |  |
|  |  |  EventsRoute  | ──> | StudioLookup   |     |    EventActor      |  |  |
|  |  |               |     | Cache          |     |                    |  |  |
|  |  +───────┬───────+     +────────────────+     +──────────┬─────────+  |  |
|  |          │                                               │            |  |
|  |          └───────────────── Ask Command ─────────────────┘            |  |
|  +──────────────────────────────────────────────────────────┼────────────+  |
|                                                             │               |
|                                                             │ Invokes DAO   |
|  +──────────────────────────────────────────────────────────▼────────────+  |
|  |                           :data Module                                |  |
|  |  +─────────────────────────────────────────────────────────────────+  |  |
|  |  |               DefaultEventDAO (@NjallUsers Session)             |  |  |
|  |  +────────────────────────────────┬────────────────────────────────+  |  |
|  +───────────────────────────────────┼───────────────────────────────────+  |
+──────────────────────────────────────┼──────────────────────────────────────+
                                       │ SQL (CTI + RLS)
+──────────────────────────────────────▼──────────────────────────────────────+
|                           PostgreSQL Database                               |
|  +─────────────────────────+            +────────────────────────────────+  |
|  |   njall_users.entities  | <───────── |      njall_users.events        |  |
|  +─────────────────────────+            +───────────────┬────────────────+  |
|                                                         │                   |
|  +─────────────────────────+                            │ (tenant_id,       |
|  |   njall_users.locations | <──────────────────────────┘  location_id)     |
|  +─────────────────────────+                                                |
+─────────────────────────────────────────────────────────────────────────────+
```

### C4 Component Diagram (:api Module)
```
+─────────────────────────────────────────────────────────────────────────────+
|                                :api Container                               |
|                                                                             |
|   HTTP Request                                                              |
|        │                                                                    |
|        ▼                                                                    |
|  +─────────────+       Resolve Tenant       +───────────────────────────+   |
|  | EventsRoute | ─────────────────────────> |     StudioLookupCache     |   |
|  +──────┬──────+                            +───────────────────────────+   |
|         │                                                                   |
|         │ Validate & Dispatch EventCommand                                  |
|         ▼                                                                   |
|  +──────────────+      Execute Command      +───────────────────────────+   |
|  |  EventActor  | ────────────────────────> |         EventDAO          |   |
|  +──────┬───────+                           +───────────────────────────+   |
|         │                                                                   |
|         ▼                                                                   |
|  +─────────────────────────+                                                |
|  |   EventActorResponse    |                                                |
|  |  (Success/Deleted/...)  |                                                |
|  +─────────────────────────+                                                |
+─────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### 1. CTI Subtype for Events referencing `njall_users.entities`
- **Choice**: Map `njall_users.events` with a composite primary key `(tenant_id, id)` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
- **Rationale**: Extends the established pattern from [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md) and [0029-tenanted-locations-and-addresses-architecture.md](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md). Allows events to inherit common audit fields (`created_on`, `updated_on`, `deleted_on`, `summary`) and enables automatic association with hashtags (`njall_users.hashtags_entity`) and value reactions (`njall_users.reactions`) without custom junction tables.
- **Alternatives Considered**: Independent standalone table with foreign key to entities. Rejected because it violates CTI polymorphic queryability and duplicates audit infrastructure.

### 2. Composite Foreign Key on `location_id`
- **Choice**: Enforce `FOREIGN KEY (tenant_id, location_id) REFERENCES njall_users.locations (tenant_id, id) ON DELETE SET NULL`.
- **Rationale**: Uses PostgreSQL's composite primary key semantics to guarantee that an event can only reference a location belonging to the exact same studio tenant. Deleting a location sets `location_id` to null on referencing events without deleting the event.
- **Alternatives Considered**: Simple `FOREIGN KEY (location_id) REFERENCES locations(id)`. Rejected because it fails to enforce tenant isolation at the database constraint level.

### 3. Database Check Constraint for Temporal Validity
- **Choice**: Add `CONSTRAINT chk_events_time_order CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)`.
- **Rationale**: Prevents chronologically inverted event intervals at the storage engine level, backed by defensive validation in `EventValidation` within `:api`.
- **Alternatives Considered**: Application-only validation. Rejected because direct database updates or batch scripts could bypass application checks.

### 4. AIP-134 Partial Updates
- **Choice**: Implement `PATCH /api/studios/{studio-id}/v1/events/{id}` accepting an optional `update_mask` parameter targeting `title`, `summary`, `locationId`, `startTime`, and `endTime`.
- **Rationale**: Consistent with AIP-134 conventions across all user studio endpoints.
- **Alternatives Considered**: Full replacement via `PUT`. Rejected to prevent accidental nullification of unmodified fields.

### 5. Soft Deletion via `entities.deleted_on`
- **Choice**: Soft delete events by populating `entities.deleted_on = CURRENT_TIMESTAMP` and filtering them out of collection and detail queries (`WHERE deleted_on IS NULL`).
- **Rationale**: Retains audit history and referential consistency for historical reactions and links.
- **Alternatives Considered**: Hard deletion (`DELETE FROM entities`). Rejected because hard deletion cascades and destroys historical associations.

## Risks / Trade-offs

- **[Referential Violation on Location]** -> Mitigation: When a client submits an invalid or foreign `locationId`, Hibernate catches the foreign key constraint violation and translates it into a structured HTTP 400 Bad Request error.
- **[Timezone Deserialization Mismatch]** -> Mitigation: Use Java `Instant` formatted as standard UTC ISO-8601 strings (`yyyy-MM-dd'T'HH:mm:ss'Z'`) across Jackson DTOs and PostgreSQL `TIMESTAMPTZ`.
- **[Collection Query Volume]** -> Mitigation: Add index `idx_events_time` on `(tenant_id DESC, start_time ASC, end_time ASC)` to accelerate un-filtered active event queries.

## Migration Plan

1. Deploy Flyway migration `V8__events.sql` in `:data`.
2. Deploy backend release containing `EventDAO`, `EventActor`, and `EventsRoute`.
3. Safe rollback: Dropping table `njall_users.events` if required does not affect preexisting entity or location tables due to CTI decoupling.

## Open Questions

None. All architectural decisions align with accepted in-force ADRs.
