## Context

In **Njall**, studios and users interact through federated identities capable of publishing and receiving messages in accordance with **FeatherPub** protocols. Following the architectural foundation established in [0005-sealed-dao-and-hibernate-dual-session-architecture.md](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025-user-space-studios-api-and-tenant-row-level-security.md](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md), [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md), [0036-tenanted-individuals-architecture.md](../../../../adr/0036-tenanted-individuals-architecture.md), and [0037-tenanted-collections-architecture.md](../../../../adr/0037-tenanted-collections-architecture.md), **Actor** entities are designed as a first-class Common Table Inheritance (CTI) subtype extending `njall_users.entities`, accompanied by a subordinate `njall_users.actor_endpoints` table and paired `inbox` and `outbox` **Collections** entities. To avoid naming collision with Apache Pekko concurrency primitives in the Java codebase, all internal domain components, **DAO** abstractions, and verticles are designated as **Agent**.

## Goals / Non-Goals

**Goals:**
- Provide tenanted persistence for **Actor** entities under `njall_users.actors` and federated endpoints under `njall_users.actor_endpoints` with PostgreSQL Row-Level Security (RLS) enforcement.
- Link an **Actor** strictly to an owner **Entity** (such as an individual) within the same studio **Tenant** via composite foreign keys, supporting 1:N owner-to-actor relationships.
- Enforce case-insensitive permanent username reservation per studio **Tenant** via functional B-tree indexing on `(tenant_id, lower(preferred_username))`.
- Automatically provision paired `inbox` and `outbox` **Collections** entities upon **Actor** creation, maintaining them under active entity lifecycle rules.
- Support federated endpoint upsert (`:addEndpoint`) and removal (`:removeEndpoint`) via Google AIP-136 custom methods returning HTTP 204 No Content.
- Provide inbox and outbox collection retrieval returning expanded `OrderedCollection` payloads, while accepting incoming JSON object payloads on POST returning HTTP 201 Created.
- Adhere strictly to repository standards: immutable records, nonnull defaults, sealed Pekko protocols, Guice module bindings, and JaCoCo coverage gates.

**Non-Goals:**
- Pagination pages (`OrderedCollectionPage`) for inbox/outbox in this phase; collections are retrieved as whole resources.
- Deep serialization and federation processing of incoming/outgoing ActivityPub activities (reserved for subsequent FeatherPub delivery phases).
- Studio-wide actor listing endpoints (`GET /api/studios/{studio-id}/v1/actors` returns HTTP 404 Not Found).

## Architectural Diagrams (C4 Level 2 & 3)

### C4 Container Diagram
```text
+─────────────────────────────────────────────────────────────────────────────+
|                                  Njall Host                                 |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                            :api Verticle                              |  |
|  |  +────────────────+    +────────────────+    +─────────────────────+  |  |
|  |  |   AgentRoute   | ──>| StudioLookup   |    |     AgentActor      |  |  |
|  |  |                |    | Cache          |    |                     |  |  |
|  |  +────────┬───────+    +────────────────+    +──────────┬──────────+  |  |
|  |           │                                             │             |  |
|  |           └──────────────── Ask Command ────────────────┘             |  |
|  +─────────────────────────────────────────────────────────┼─────────────+  |
|                                                            │                |
|                                                            │ Invokes DAO    |
|  +─────────────────────────────────────────────────────────▼─────────────+  |
|  |                           :data Module                                |  |
|  |  +─────────────────────────────────────────────────────────────────+  |  |
|  |  |               DefaultAgentDAO (@NjallUsers Session)             |  |  |
|  |  +────────────────────────────────┬────────────────────────────────+  |  |
|  +───────────────────────────────────┼───────────────────────────────────+  |
+──────────────────────────────────────┼──────────────────────────────────────+
                                       │ SQL (CTI + Endpoints + Collections)
+──────────────────────────────────────▼──────────────────────────────────────+
|                           PostgreSQL Database                               |
|  +─────────────────────────+            +────────────────────────────────+  |
|  |   njall_users.entities  | <───────── |      njall_users.actors        |  |
|  +─────────────────────────+ (CTI)      +──────────────┬─────────────────+  |
|       ^               ^                                │ 1                  |
|       │ (Owner FK)    │                                │                    |
|       │               │ (1:2 Collections)              │ N                  |
|       │               │                 +──────────────▼─────────────────+  |
|       │               └──────────────── |   njall_users.actor_endpoints  |  |
|       │                                 +────────────────────────────────+  |
|       │                                                                     |
|       │                                 +────────────────────────────────+  |
|       └──────────────────────────────── |    njall_users.collections     |  |
|                                         +────────────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────+
```

### C4 Component Diagram (:api Module)
```text
+─────────────────────────────────────────────────────────────────────────────+
|                                :api Container                               |
|                                                                             |
|   HTTP Request                                                              |
|        │                                                                    |
|        ▼                                                                    |
|  +──────────────────+      Resolve Tenant       +───────────────────────+   |
|  |    AgentRoute    | ────────────────────────> |   StudioLookupCache   |   |
|  +────────┬─────────+                           +───────────────────────+   |
|           │                                                                 |
|           │ Validate & Dispatch AgentCommand                                |
|           ▼                                                                 |
|  +──────────────────+     Execute Command       +───────────────────────+   |
|  |    AgentActor    | ────────────────────────> |        AgentDAO       |   |
|  +────────┬─────────+                           +───────────────────────+   |
|           │                                                                 |
|           ▼                                                                 |
|  +─────────────────────────+                                                |
|  |    AgentActorResponse   |                                                |
|  | (Success/NotFound/...)  |                                                |
|  +─────────────────────────+                                                |
+─────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### 1. CTI Subtype for Actors referencing `njall_users.entities`
- **Choice**: Map `njall_users.actors` with composite primary key `(tenant_id, id)` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
- **Rationale**: Extends the established repository pattern from ADR-0027, ADR-0036, and ADR-0037. **Actor** entities inherit common audit attributes (`summary`, `created_on`, `updated_on`, `deleted_on`) and participate in polymorphic entity lookups without duplicating `summary` in subtype storage.
- **Alternatives Considered**: Independent table without CTI. Rejected because it breaks polymorphic entity resolution and duplicates lifecycle fields.

### 2. Composite Foreign Keys for Tenant Isolation
- **Choice**: Enforce composite foreign keys for owner and subordinate tables:
  - `FOREIGN KEY (tenant_id, owner_id) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE`
  - `FOREIGN KEY (tenant_id, actor_id) REFERENCES njall_users.actors (tenant_id, id) ON DELETE CASCADE`
- **Rationale**: Enforces relational tenant boundary confinement, guaranteeing an **Actor** cannot reference an owner entity belonging to a different studio **Tenant**. Supports 1:N cardinality allowing an individual to maintain multiple actor personas.
- **Alternatives Considered**: Single-column foreign keys (`owner_id REFERENCES entities(id)`). Rejected because they allow cross-tenant foreign key linkages.

### 3. Case-Insensitive Permanent Username Indexing
- **Choice**: Enforce username uniqueness via `CREATE UNIQUE INDEX idx_actors_tenant_username ON njall_users.actors (tenant_id, lower(preferred_username))`.
- **Rationale**: PostgreSQL Hash indexes cannot enforce unique constraints (`ERROR: access method "hash" does not support unique indexes`). Functional B-tree indexing on `lower(preferred_username)` enforces case-insensitive identity isolation matching ActivityPub/WebFinger norms and prevents username impersonation even across soft-deleted records.
- **Alternatives Considered**: Case-sensitive unique index. Rejected because it allows conflicting identities like `Valkyrie` and `valkyrie` in the same tenant.

### 4. Endpoints Table with Composite Key and Upsert Semantics
- **Choice**: Store federated endpoints in `njall_users.actor_endpoints` with composite primary key `(tenant_id, actor_id, name)`. Implement `:addEndpoint` via SQL `ON CONFLICT (tenant_id, actor_id, name) DO UPDATE SET endpoint = EXCLUDED.endpoint`. Validate name with check constraint `CHECK (name ~ '^[a-zA-Z0-9]+$')` and `VARCHAR(31)`.
- **Rationale**: Eliminates separate unique indices; guarantees per-actor endpoint name uniqueness; provides clean upsert semantics; enforces alphanumeric character rules and length limits (< 32 characters) at both database and API validation layers.
- **Alternatives Considered**: Return 409 Conflict when adding an existing endpoint. Rejected in favor of idempotent upsert as confirmed during exploration.

### 5. Automatic Inbox/Outbox Provisioning as OrderedCollections
- **Choice**: When creating an **Actor**, provision two associated records in `njall_users.collections`: one with `name = 'inbox'` and one with `name = 'outbox'`, both with `owner_id = actor.id`. Soft deleting the actor cascades soft deletion to both collections.
- **Rationale**: Reuses the entire collections storage and hydration infrastructure without schema duplication. Deleting an actor automatically invalidates its inbox and outbox.
- **Alternatives Considered**: Adding explicit `inbox_id` and `outbox_id` foreign key columns to `njall_users.actors`. Rejected because mutual circular foreign key constraints complicate transactional insertion and deletion ordering.

### 6. Java Component Naming as `Agent` to Avoid Pekko Name Collision
- **Choice**: Name the Java domain record `Agent`, DAO `AgentDAO`, implementation `DefaultAgentDAO`, Pekko actor `AgentActor`, and route `AgentRoute`. Retain `/api/studios/{studio-id}/v1/actors` for external REST routes and `njall_users.actors` for database tables.
- **Rationale**: Fully eliminates linguistic ambiguity between Apache Pekko concurrency actors (`org.apache.pekko.actor.typed.Actor`) and user-facing federated actor domain entities.
- **Alternatives Considered**: Naming Pekko actors `ActorEntityActor` or `StudioActorActor`. Rejected because having `Actor` refer to both concepts throughout the codebase creates ongoing cognitive friction.

### 7. Inbox and Outbox Payload Acceptance (Deferred Serialization Protocol)
- **Choice**: Accept arbitrary JSON object payloads (`JsonNode`) on `POST /actors/{actor-id}/inbox` and `POST /actors/{actor-id}/outbox`, returning HTTP 201 Created.
- **Rationale**: Establishes route contract and verifies connectivity without prematurely locking the codebase into an unvetted ActivityStreams activity schema before the full federation protocol is designed.
- **Alternatives Considered**: Full ActivityStreams activity validation and persistence into an `activities` table. Rejected because the broader FeatherPub federation protocol is scheduled for a dedicated subsequent change.

## Risks / Trade-offs

- **[Risk]** Permanent username reservation prevents reclaiming usernames of soft-deleted actors.
  - **Mitigation**: Intentional security decision preventing actor impersonation across federated networks.
- **[Risk]** Cascading soft-delete to paired collections requires multi-entity update.
  - **Mitigation**: Performed within a single `@NjallUsers` database transaction in `DefaultAgentDAO`, updating `deleted_on` on the actor and its owned inbox/outbox entities atomically.

## Migration Plan

1. Apply Flyway migration `V11__actors.sql` creating tables, indices, constraints, RLS policies, and role grants.
2. Deploy updated `:data` module with `AgentDAO` and `DefaultAgentDAO` bindings in `StudiosDaoModule`.
3. Deploy updated `:api` verticle with `AgentRoute` mounted into `StudiosRoute`.

## Open Questions

None. All architectural decisions aligned during exploration.
