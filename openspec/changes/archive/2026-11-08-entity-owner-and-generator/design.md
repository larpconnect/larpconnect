## Context

In **Njall**, tenanted resources in the **User plane** use a Common Table Inheritance (**CTI**) hierarchy rooted at `njall_users.entities`, as established across [0005-sealed-dao-and-hibernate-dual-session-architecture.md](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025-user-space-studios-api-and-tenant-row-level-security.md](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md), [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md), [0029-locations-and-addresses-spatial-cti-architecture.md](../../../../adr/0029-locations-and-addresses-spatial-cti-architecture.md), [0033-hashtags-three-level-cti-and-dual-id-resolution.md](../../../../adr/0033-hashtags-three-level-cti-and-dual-id-resolution.md), [0035-tenanted-events-architecture.md](../../../../adr/0035-tenanted-events-architecture.md), [0036-tenanted-individuals-architecture.md](../../../../adr/0036-tenanted-individuals-architecture.md), [0037-tenanted-collections-architecture.md](../../../../adr/0037-tenanted-collections-architecture.md), and [0038-tenanted-actors-and-federated-endpoints-architecture.md](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md).

Under **FeatherPub** and **ActivityStreams** conventions, every **Entity** in a **Studio** **Tenant** must be attributable to an **Actor** (`generator`) that created it. Previously:
1. `njall_users.entities` had no `owner` column, leaving links, hashtags, locations, events, and individuals without **Actor** provenance.
2. `njall_users.actors` defined a subtype-specific `owner_id` column referencing `njall_users.entities(tenant_id, id)` that duplicated structural ownership on the **Actor** subtype table.
3. `njall_users.collections` defined an `owner_id` column representing semantic attachment (the **Entity** a **Collection** belongs to, such as an actor's `inbox`/`outbox` or a forum parent), which is distinct from the **Actor** that generated the **Collection**.

This design promotes structural **Actor** provenance to `njall_users.entities.owner` (`UUID NOT NULL`), removes the redundant `njall_users.actors.owner_id` column while retaining `njall_users.collections.owner_id`, enforces in application **DAO** logic that `entities.owner` always references an active **Actor** (`entity_type = 'Actor'`), and exposes the provenance on all studio REST API representations as `"generator": "/api/studios/{studio-id}/v1/actors/{owner-id}"`.

## Goals / Non-Goals

**Goals:**
- Add an immutable `owner UUID NOT NULL` column to `njall_users.entities` with a composite self-referencing foreign key `(tenant_id, owner) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE` and supporting index `idx_entities_owner` on `(tenant_id, owner DESC)`.
- Consolidate **Actor** structural ownership onto `njall_users.entities.owner` by dropping `owner_id`, `fk_actors_owner`, and `idx_actors_owner` from `njall_users.actors`.
- Preserve `njall_users.collections.owner_id` for semantic **Collection** attachment while recording the generating **Actor** in `njall_users.entities.owner`.
- Enforce in application **DAO** create paths (`DefaultAgentDAO`, `DefaultCollectionDAO`, `DefaultEventDAO`, `DefaultHashtagDAO`, `DefaultIndividualDAO`, `DefaultLinkDAO`, `DefaultLocationDAO`) that `owner` resolves to an existing, active (`deleted_on IS NULL`) **Entity** in the same **Tenant** with `entity_type = 'Actor'` (or self `owner = id` when creating a self-owned **Actor**).
- Accept `"generator"` in creation request payloads as either a bare **Actor** UUID string or a canonical studio **Actor** URI (`/api/studios/{studio-id}/v1/actors/{actor-id}` matching the target `{studio-id}`).
- Default `generator` to self-ownership (`owner = id`) when omitted on `POST /api/studios/{studio-id}/v1/actors`, and infer `generator = ownerId` when omitted on `POST /api/studios/{studio-id}/v1/collections` (requiring that the resolved `generator` is an active **Actor**).
- Expose `"generator": "/api/studios/{studio-id}/v1/actors/{owner-id}"` on all **Entity** responses (`ActorResponse`, `OrderedCollectionResponse`, `EventResponse`, `IndividualResponse`, `LinkResponse`, `LocationResponse`, `TagResponse`) and hydrated collection items (`ExpandedEntity`).

**Non-Goals:**
- Database-level `CHECK` constraints or triggers enforcing `entity_type = 'Actor'` on `njall_users.entities.owner` (PostgreSQL does not permit subqueries in `CHECK` constraints, and validation is intentionally handled in the application **DAO** layer).
- Embedding full hydrated **Actor** JSON objects inside the `generator` field of responses or expanded collection items (`generator` is always the canonical studio **Actor** URI string).
- Adding `owner` / `generator` to `njall_users.addresses` (`PostalAddress` is a subordinate value table attached to `njall_users.locations`, not a **CTI** **Entity**).
- Allowing `PATCH` endpoints to mutate `entities.owner` (`owner` is strictly immutable after creation).

## Architectural Diagrams (C4 Level 2 & 3)

### C4 Container Diagram
```text
+─────────────────────────────────────────────────────────────────────────────────+
|                                   Njall Host                                    |
|                                                                                 |
|  +───────────────────────────────────────────────────────────────────────────+  |
|  |                               :api Verticle                               |  |
|  |  +──────────────────────────────+        +─────────────────────────────+  |  |
|  |  | Studio Entity Routes         |        | Studio Entity Actors        |  |  |
|  |  | (Agent, Collection, Event,   | ──Ask─>| (AgentActor, CollectionActor|  |  |
|  |  |  Individual, Link, Location, |        |  EventActor, IndividualActor|  |  |
|  |  |  Tag + GeneratorUriSupport)  |        |  LinkActor, LocationActor,  |  |  |
|  |  +──────────────────────────────+        |  TagActor)                  |  |  |
|  |                                          +──────────────┬──────────────+  |  |
|  +─────────────────────────────────────────────────────────┼─────────────────+  |
|                                                            │ Invokes DAO        |
|  +─────────────────────────────────────────────────────────▼─────────────────+  |
|  |                               :data Module                                |  |
|  |  +─────────────────────────────────────────────────────────────────────+  |  |
|  |  | Tenanted Studio DAOs (@NjallUsers Session, Actor Owner Validator)   |  |  |
|  |  +──────────────────────────────────┬──────────────────────────────────+  |  |
|  +─────────────────────────────────────┼─────────────────────────────────────+  |
+────────────────────────────────────────┼────────────────────────────────────────+
                                         │ SQL (CTI + RLS)
+────────────────────────────────────────▼────────────────────────────────────────+
|                              PostgreSQL Database                                |
|  +───────────────────────────────────────────────────────────────────────────+  |
|  | njall_users.entities                                                      |  |
|  | PK: (tenant_id, id)                                                       |  |
|  | FK: (tenant_id, owner) -> entities(tenant_id, id) ON DELETE CASCADE       |  |
|  +──────┬──────────────┬──────────────┬──────────────┬──────────────┬────────+  |
|         │              │              │              │              │           |
|         ▼              ▼              ▼              ▼              ▼           |
|  +────────────+ +─────────────+ +───────────+ +────────────+ +───────────────+  |
|  |   actors   | | collections | |   links   | | locations  | | events /      |  |
|  | (no owner  | | (retains    | | (+hashtags| | (+addresses| | individuals   |  |
|  |  column)   | |  owner_id)  | |  subtype) | |  child)    | |               |  |
|  +────────────+ +─────────────+ +───────────+ +────────────+ +───────────────+  |
+─────────────────────────────────────────────────────────────────────────────────+
```

### C4 Component Diagram (`:api` & `:data` Generator Provenance Flow)
```text
+─────────────────────────────────────────────────────────────────────────────────+
|                        Studio Entity Creation & Retrieval                       |
|                                                                                 |
|  HTTP POST / GET                                                                |
|        │                                                                        |
|        ▼                                                                        |
|  +───────────────────────────+   Parse UUID or    +──────────────────────────+  |
|  | Studio Route              | ─────────────────> | GeneratorUriSupport      |  |
|  | (*Route)                  | <───────────────── | - parseGenerator(...)    |  |
|  +─────────────┬─────────────+   Format URI       | - formatGeneratorUri(..) |  |
|                │                                  +──────────────────────────+  |
|                │ Pekko Ask (owner UUID)                                         |
|                ▼                                                                |
|  +───────────────────────────+                                                  |
|  | Studio Pekko Actor        |                                                  |
|  | (*Actor)                  |                                                  |
|  +─────────────┬─────────────+                                                  |
|                │ DAO.create(tenantId, owner, ...)                               |
|                ▼                                                                |
|  +───────────────────────────+   Verify Active    +──────────────────────────+  |
|  | Tenanted DAO              | ─────────────────> | EntityBaseEntity         |  |
|  | (Default*DAO)             |                    | (owner UUID NOT NULL,    |  |
|  +───────────────────────────+                    |  updatable = false,      |  |
|                                                   |  entity_type = 'Actor')  |  |
|                                                   +──────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### 1. Universal `owner` Column on `njall_users.entities` with Self-Referencing Composite FK
- **Choice**: Add `owner UUID NOT NULL` to `njall_users.entities` with `CONSTRAINT fk_entities_owner FOREIGN KEY (tenant_id, owner) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE` and index `idx_entities_owner` on `(tenant_id, owner DESC)`. Map `owner` on `EntityBaseEntity` with `@Column(name = "owner", nullable = false, updatable = false)`.
- **Rationale**: Every **CTI** **Entity** shares `njall_users.entities` as its root table. Storing `owner` on `entities` guarantees at the relational level that every **Entity** has a non-null owner within the same **Tenant**, supports self-owned **Actor** records (`owner = id`) within the same `INSERT` statement in PostgreSQL, and avoids duplicating provenance columns across subtype tables.
- **Alternatives Considered**:
  - Adding `owner_id` to each subtype table individually. Rejected because it duplicates schema definitions and complicates polymorphic queries (such as `CollectionDAO` expanded item hydration).
  - Naming the database column `generator`. Rejected because `owner` is concise and consistent with internal domain ownership terminology while `generator` is the external **ActivityStreams** JSON property name.

### 2. Consolidate `njall_users.actors.owner_id` into `njall_users.entities.owner`, Retain `njall_users.collections.owner_id`
- **Choice**: Drop `owner_id`, `fk_actors_owner`, and `idx_actors_owner` from `njall_users.actors` in `V12__entities_owner.sql`. Keep `owner_id`, `fk_collections_owner`, and `unq_collections_owner_name` on `njall_users.collections`.
- **Rationale**: For an **Actor**, structural ownership and generation are identical (`entities.owner`), and an **Actor** may be self-owned (`owner = id`) or owned by another **Actor**. By contrast, a **Collection** has both a semantic owner (`collections.owner_id`, the **Entity** to which the **Collection** is attached, such as an individual, event, or actor) and a structural generator (`entities.owner`, the **Actor** that created the **Collection**).
- **Alternatives Considered**: Dropping `collections.owner_id` as well. Rejected because a **Collection** attached to a non-**Actor** **Entity** (e.g., an event roster or forum thread collection) needs `collections.owner_id` to reference that non-**Actor** **Entity** while `entities.owner` references the generating **Actor**.

### 3. Application-Layer Validation that `entities.owner` is an Active `Actor`
- **Choice**: Validate inside each tenanted **DAO**'s transactional write path (via a shared helper on `EntityBaseEntity` / session lookup) that `owner` resolves to an existing `EntityBaseEntity` in the same `tenant_id` with `deletedOn == null` and `"Actor".equals(entityType)` (except when `DefaultAgentDAO` inserts a self-owned **Actor** where `owner.equals(id)`). Throw `IllegalArgumentException` when validation fails, which the `:api` layer maps to `400 Bad Request`.
- **Rationale**: PostgreSQL does not support subqueries in `CHECK` constraints, and triggers add procedural overhead to every insert. Validating via `Session.find(EntityBaseEntity.class, new EntityId(tenantId, owner))` leverages the Hibernate first-level session cache and ` Direct Primary Key Session Lookups` invariant (`ADR-0023`).
- **Alternatives Considered**: Database `BEFORE INSERT` trigger checking `entity_type = 'Actor'`. Rejected per user requirements specifying that validating the **Actor** subtype is application logic rather than a database constraint.

### 4. Flexible `generator` Input (UUID or Studio Actor URI) and Canonical URI Output
- **Choice**: Provide a shared utility in `:api` (`GeneratorUriSupport`) that:
  1. Formats any `owner` UUID into the canonical studio **Actor** URI `"/api/studios/" + studioId + "/v1/actors/" + ownerId`, preserving the `{studio-id}` identifier segment (alias or public studio UUID) used in the request path.
  2. Parses incoming `generator` strings on `POST` requests by accepting either a bare UUID string (`<actor-uuid>`) or the canonical URI path `"/api/studios/" + studioId + "/v1/actors/<actor-uuid>"` (also accepting matching studio UUID/alias for the resolved `StudioLookup`), returning `Optional.empty()` on blank, malformed, or cross-studio URIs so the route immediately responds with `400 Bad Request`.
- **Rationale**: Aligns with **ActivityStreams** / **FeatherPub** object modeling where `generator` is an IRI linking to the generating **Actor**, while remaining ergonomic for API clients that hold either the **Actor**'s UUID or its full `generator` URI from a previous response.
- **Alternatives Considered**: Accepting only bare UUIDs under `generatorId` or accepting only full URIs. Rejected during exploration in favor of naming the JSON property `generator` and accepting both UUID and canonical URI formats on input while always emitting the canonical URI on output.

### 5. Defaulting Rules for `Actor` and `Collection` Creation
- **Choice**:
  - `POST /api/studios/{studio-id}/v1/actors`: `generator` is optional in `CreateActorRequest`. When omitted or null, `DefaultAgentDAO` sets `owner = id` (self-owned **Actor**). The automatically provisioned `inbox` and `outbox` **Collections** set both `entities.owner = actor.id` and `collections.owner_id = actor.id`.
  - `POST /api/studios/{studio-id}/v1/collections`: `ownerId` is required and `generator` is optional in `CreateCollectionRequest`. When `generator` is omitted or null, the route infers `generator = ownerId`. The **DAO** then validates that the resolved `owner` (`generator`) is an active **Actor** in the same **Tenant**; if `ownerId` is not an **Actor** and `generator` was omitted, validation fails with `400 Bad Request`.
  - `POST` for `events`, `individuals`, `links`, `locations`, and `tags` (including `tags:batchCreate`): `generator` is required on every creation request item.
- **Rationale**: Creating a bootstrap **Actor** requires self-ownership so there is no chicken-and-egg dependency on a pre-existing **Actor**. Most **Collections** owned by an **Actor** are also generated by that **Actor**, making inference ergonomic while still requiring explicit `generator` when attaching a **Collection** to a non-**Actor** **Entity**.
- **Alternatives Considered**: Requiring `generator` on all endpoints without defaults. Rejected because a studio's first **Actor** could never be created without self-ownership.

### 6. Expanded Collection Items Include `generator` URI String
- **Choice**: Add `UUID owner` to `ExpandedEntity` in `:data` (populated from `EntityBaseEntity.getOwner()` during `DefaultCollectionDAO` item hydration) and serialize `"generator": "/api/studios/{studio-id}/v1/actors/{item-owner-id}"` on each hydrated object in `orderedItems` when `form=expanded` or when reading `/inbox` and `/outbox`.
- **Rationale**: Keeps expanded collection item representations consistent with direct `GET` responses for those entities without incurring recursive **Actor** object hydration overhead.
- **Alternatives Considered**: Hydrating and embedding the full **Actor** JSON object inside `generator`. Rejected during exploration because clients rarely need the full generator object inline and **Actor** is its own resource category.

## Risks / Trade-offs

- **[Risk]** Seed scripts (`docker/postgres/seed/03-studio-links.sql`, `04-studio-locations.sql`) and integration tests that insert into `njall_users.entities` without an `owner` column will fail once `V12__entities_owner.sql` makes `owner UUID NOT NULL`.
  - **Mitigation**: Update `02-test-studios.sql` (or a dedicated seed section before links/locations) to seed a default self-owned studio **Actor** for each seeded studio (`valkyrie_example` and `ironwood_fake`), and update `03-studio-links.sql`, `04-studio-locations.sql`, and all `:data` / `:integration` test fixtures to supply a valid **Actor** `owner`.
- **[Risk]** Deleting an `owner` **Actor** physical row in `njall_users.entities` cascades via `ON DELETE CASCADE` to entities it generated.
  - **Mitigation**: **Njall** exclusively uses soft deletion (`entities.deleted_on = CURRENT_TIMESTAMP`) in application operations; physical deletion only occurs in administrative tenant teardown where cascading cleanup is desired.

## Migration Plan

1. Add Flyway migration `data/src/main/resources/db/migration/V12__entities_owner.sql`:
   - Add `owner UUID NOT NULL` to `njall_users.entities`.
   - Add composite foreign key `fk_entities_owner` on `(tenant_id, owner)` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
   - Create index `idx_entities_owner` on `njall_users.entities (tenant_id, owner DESC)`.
   - Drop index `idx_actors_owner`, constraint `fk_actors_owner`, and column `owner_id` from `njall_users.actors`.
2. Update `docker/postgres/seed/02-test-studios.sql`, `03-studio-links.sql`, and `04-studio-locations.sql` so each seeded studio provisions a self-owned studio **Actor** and all seeded entities reference that studio **Actor** in `njall_users.entities.owner`.
3. Update `:data` JPA entities (`EntityBaseEntity`, `AgentEntity`, and subtype constructors), domain records, and **DAO** implementations (`DefaultAgentDAO`, `DefaultCollectionDAO`, `DefaultEventDAO`, `DefaultHashtagDAO`, `DefaultIndividualDAO`, `DefaultLinkDAO`, `DefaultLocationDAO`).
4. Update `openapi.yaml` and `:api` DTOs, Pekko actor commands, and routes (`AgentRoute`, `CollectionRoute`, `EventRoute`, `IndividualRoute`, `LinkRoute`, `LocationRoute`, `TagRoute`).
5. Update `:data`, `:api`, and `:integration` unit and Cucumber acceptance tests.

## Open Questions

- **In-Force ADRs to Amend in the `adr` Step**:
  - [0038-tenanted-actors-and-federated-endpoints-architecture.md](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md) specified `njall_users.actors.owner_id` and `ownerId` in the **Actor** REST contract; the `adr` step will record `adr/0039-entity-owner-and-generator-provenance.md` amending ADR-0038 (as well as extending the **CTI** entity contracts in ADR-0027, ADR-0029, ADR-0033, ADR-0035, ADR-0036, and ADR-0037) without modifying the existing ADR files.

