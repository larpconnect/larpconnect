## Context

In **Njall**, studios manage community participants, players, performers, and non-player characters. Following the architectural precedent established in [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md), [0029-tenanted-locations-and-addresses-architecture.md](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md), and [0035-tenanted-events-and-location-associations-architecture.md](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md), this design establishes **Individuals** as a first-class Class Table Inheritance (CTI) subtype extending `njall_users.entities`. Individuals are surfaced through asynchronous Pekko Typed HTTP routes adhering to Google AIP standards under single-tenant studio paths.

## Goals / Non-Goals

**Goals:**
- Provide tenanted persistence for **Individuals** under `njall_users.individuals` inheriting from `njall_users.entities` with PostgreSQL Row-Level Security (RLS) enforcement.
- Expose single-tenant HTTP REST endpoints under `/api/studios/{studio-id}/v1/individuals` supporting entity creation (POST), single entity retrieval (GET), partial update (PATCH conforming to AIP-134), and soft deletion (DELETE).
- Omit collection listing (`GET /api/studios/{studio-id}/v1/individuals`) by design.
- Adhere strictly to repository invariants: Java 25 LTS, immutable records, sealed Pekko protocols, Guice module hierarchy, IOSP-lite behavioral separation, and 85%/90% JaCoCo coverage gates.

**Non-Goals:**
- Specialized domain attributes (e.g., pronouns, emergency contact, billing records, or user account linkage) for this milestone.
- Full-text or cross-studio search capabilities.
- Cross-tenant federation protocols (deferred to future FeatherPub work).

## Architectural Diagrams (C4 Level 2 & 3)

### C4 Container Diagram
```
+-----------------------------------------------------------------------------+
|                                  Njall Host                                 |
|                                                                             |
|  +-----------------------------------------------------------------------+  |
|  |                            :api Module                                |  |
|  |  +──────────────────+     +────────────────+     +─────────────────+  |  |
|  |  | IndividualsRoute | ──> | StudioLookup   |     | IndividualActor |  |  |
|  |  |                  |     | Cache          |     |                 |  |  |
|  |  +─────────┬────────+     +────────────────+     +────────┬────────+  |  |
|  |            │                                              │           |  |
|  |            └──────────────── Ask Command ─────────────────┘           |  |
|  +───────────────────────────────────────────────────────────┼───────────+  |
|                                                              │              |
|                                                              │ Invokes DAO  |
|  +-----------------------------------------------------------▼-----------+  |
|  |                           :data Module                                |  |
|  |  +-----------------------------------------------------------------+  |  |
|  |  |            DefaultIndividualDAO (@NjallUsers Session)           |  |  |
|  |  +--------------------------------┬--------------------------------+  |  |
|  +-----------------------------------┼-----------------------------------+  |
+--------------------------------------┼--------------------------------------+
                                       │ SQL (CTI + RLS)
+--------------------------------------▼--------------------------------------+
|                           PostgreSQL Database                               |
|  +─────────────────────────+            +────────────────────────────────+  |
|  |   njall_users.entities  | <───────── |    njall_users.individuals     |  |
|  +─────────────────────────+            +────────────────────────────────+  |
+-----------------------------------------------------------------------------+
```

### C4 Component Diagram (:api Module)
```
+-----------------------------------------------------------------------------+
|                                :api Container                               |
|                                                                             |
|   HTTP Request                                                              |
|        │                                                                    |
|        ▼                                                                    |
|  +──────────────────+       Resolve Tenant       +──────────────────────+   |
|  | IndividualsRoute | ─────────────────────────> |  StudioLookupCache   |   |
|  +─────────┬────────+                            +──────────────────────+   |
|            │                                                                |
|            │ Validate & Dispatch IndividualCommand                          |
|            ▼                                                                |
|  +──────────────────+      Execute Command       +──────────────────────+   |
|  | IndividualActor  | ─────────────────────────> |    IndividualDAO     |   |
|  +─────────┬────────+                            +──────────────────────+   |
|            │                                                                |
|            ▼                                                                |
|  +─────────────────────────+                                                |
|  | IndividualActorResponse |                                                |
|  |  (Success/Deleted/...)  |                                                |
|  +─────────────────────────+                                                |
+-----------------------------------------------------------------------------+
```

## Decisions

### 1. CTI Subtype for Individuals referencing `njall_users.entities`
- **Choice**: Define `njall_users.individuals` with composite primary key `(tenant_id, id)` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
- **Rationale**: Extends the architectural pattern from [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md). Common audit metadata (`created_on`, `updated_on`, `deleted_on`, `summary`) resides in `entities`, while `individuals` isolates subtype properties (`name`).
- **Alternatives Considered**: A standalone non-CTI table. Rejected because it duplicates entity audit structures and prevents polymorphic reactions or future hashtag associations.

### 2. Standard Collection POST with Server-Assigned UUIDv7
- **Choice**: Expose `POST /api/studios/{studio-id}/v1/individuals` where the server assigns a UUIDv7 and returns `201 Created`.
- **Rationale**: Maintains uniform REST patterns across Njall. Client-assigned IDs on creation are rejected to prevent ID collision and preserve centralized time-sortable UUIDv7 generation.
- **Alternatives Considered**: Parameterized POST/PUT `/individuals/{id}`. Rejected as inconsistent with the rest of the application.

### 3. Deliberate Omission of Collection List Endpoint
- **Choice**: Omit `GET /api/studios/{studio-id}/v1/individuals`. Attempted queries return HTTP 404 Not Found.
- **Rationale**: Individuals represent people who may not be publicly browsable en masse without specific access controls or contextual queries. Individual detail is accessible strictly via direct ID lookups.
- **Alternatives Considered**: Providing an unpaged list endpoint. Rejected to protect privacy and respect scale constraints.

### 4. Google AIP-134 Partial Updates
- **Choice**: Support `PATCH /api/studios/{studio-id}/v1/individuals/{id}` accepting an optional `update_mask` parameter.
- **Rationale**: Standardizes update semantics with existing routes (`links`, `locations`, `events`), modifying only specified fields (`name`, `summary`) and bumping `entities.updated_on`.
- **Alternatives Considered**: Full replacement via PUT. Rejected due to payload overhead and risk of unintended overwrite of optional fields.

### 5. Multi-Tenant Row-Level Security
- **Choice**: Enable PostgreSQL RLS on `njall_users.individuals` with `rls_individuals` for role `njall_users` checking `tenant_id = current_setting('app.tenant_id', true)::uuid` and `rls_individuals_admin` for role `njall_admin` granting bypass.
- **Rationale**: Ensures defense-in-depth data isolation at the database engine level, preventing cross-tenant leakage even in case of application query defects.
- **Alternatives Considered**: Application-level tenant filtering only. Rejected because RLS is an invariant requirement in Njall.

## Risks / Trade-offs

- **[No List Endpoint Limits Discoverability]** -> Clients must acquire the `individual-id` via event attendee rosters, creation responses, or direct references. *Mitigation*: Meets present requirements; structured lookup or search can be introduced in a future capability without breaking existing routes.
- **[Soft Delete Ghosting]** -> Soft-deleted individuals remain in `njall_users.entities` and `njall_users.individuals`. *Mitigation*: Filtered out of queries by `deleted_on IS NULL`; preserves audit trail and referential integrity for historical event records.

## Migration Plan

1. Apply Flyway migration `V9__individuals.sql` on startup to provision `njall_users.individuals`, RLS policies, and role grants.
2. No existing data migration or backfill required; table is greenfield.
3. Rollback: Drop table `njall_users.individuals` if migration reverts.

## Open Questions

- None. The design is coherent with all in-force ADRs and follows established patterns.
