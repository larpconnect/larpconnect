## Context

In **Njall**, tenanted resources in the **User plane** use a Common Table Inheritance hierarchy rooted at `njall_users.entities`, governed by the currently in-force architectural decision set including [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md), [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md), [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md), [0033: Federated Hashtags and Case-Insensitive Functional Indexing](../../../../adr/0033-federated-hashtags-and-case-insensitive-functional-indexing.md), [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md), [0036: Tenanted Individuals Architecture](../../../../adr/0036-tenanted-individuals-architecture.md), [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md), [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md), and [0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract](../../../../adr/0039-entity-owner-and-generator-provenance.md).

Two architectural misalignments remain after ADR-0037 and ADR-0039:
1. **Unparented Collections URL Hierarchy**: Every **Collections** **Entity** has an immutable generating **Actor** (**generator**), yet HTTP routes expose collections at `/api/studios/{studio-id}/v1/collections` rather than beneath `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` alongside `/inbox` and `/outbox`.
2. **Database & Domain Naming Collision (`owner` vs `generator`)**: ADR-0039 named the root provenance column `njall_users.entities.owner` (`UUID owner()` on domain records), colliding directly with `njall_users.collections.owner_id` (`UUID ownerId()` on `Collection`), where **owner** denotes semantic attachment (for example, attaching a collection to an **Individual** or **Events** record) while **generator** denotes the responsible **Actor**.

## Goals / Non-Goals

**Goals:**
- Repath all tenanted **Collections** HTTP routes under `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` (including `/{collection-id}`, `:addItem`, and `:removeItem`), delegating from `AgentRoute` to `CollectionsRoute`.
- Bind `{actor-id}` in the URL path to `njall_users.entities.generator`, verifying that `{actor-id}` is an active **Actor** in the resolved **Studio** **Tenant** and matches `collection.generator()` on every read and write operation (returning `404 Not Found` otherwise).
- Simplify `CreateCollectionRequest` by removing `generator` from the JSON request body and making `ownerId` optional (defaulting to `{actor-id}` when omitted, while still permitting explicit semantic **owner** attachment to any active **Entity** in the same **Tenant**).
- Rename `njall_users.entities.owner` to `generator` (`fk_entities_owner` -> `fk_entities_generator`, `idx_entities_owner` -> `idx_entities_generator`) via Flyway migration `V13__rename_entities_owner_to_generator.sql`, and rename `owner` -> `generator` across `EntityBaseEntity`, `ActorGeneratorValidator`, tenanted **DAO** interfaces/implementations, and all CTI domain records.

**Non-Goals:**
- Repathing `tags`, `locations`, `events`, `individuals`, or `links` under `/actors/{actor-id}` (these endpoints remain unparented).
- Adding a collection listing endpoint at `GET /api/studios/{studio-id}/v1/actors/{actor-id}/collections` (continues returning `404 Not Found`).
- Removing `njall_users.collections.owner_id` or restricting semantic **owner** attachment exclusively to **Actor** entities.

## Architectural Diagrams (Lightweight C4-Inspired)

### Container View (`:api` Runtime & PostgreSQL Data Store)

```text
+-------------------------------------------------------------------------+
| [Client]                                                                |
+------------------------------------+------------------------------------+
                                     | HTTPS JSON
                                     | /api/studios/{studio-id}/v1/
                                     |   actors/{actor-id}/collections...
                                     v
+-------------------------------------------------------------------------+
| Container: Njall Server (:server + :api + :data, Java 25 / Pekko HTTP)  |
|                                                                         |
|  [User Plane HTTP Routes & Pekko Actors] ---> [Data Plane Tenanted DAOs]|
+------------------------------------+------------------------------------+
                                     | JDBC / Hibernate (@NjallUsers, RLS)
                                     v
+-------------------------------------------------------------------------+
| Container: PostgreSQL 18 (Schema: njall_users)                          |
|                                                                         |
|  +---------------------------------+   +------------------------------+ |
|  | entities                        |   | collections                  | |
|  | PK (tenant_id, id)              |<--| PK (tenant_id, id)           | |
|  | generator UUID NOT NULL (Actor) |   | owner_id UUID NOT NULL       | |
|  | FK fk_entities_generator        |   | UNIQUE(tenant_id,owner_id,   | |
|  +---------------------------------+   |        name)                 | |
|                                        +------------------------------+ |
+-------------------------------------------------------------------------+
```

### Component View (`:api` Subordinate Route Delegation & `:data` Scoping)

```text
+-------------------------------------------------------------------------+
| Component Boundary: com.larpconnect.njall.api.studios                   |
|                                                                         |
|  [StudioSubRoutes]                                                      |
|         |                                                               |
|         v                                                               |
|  [AgentRoute] (/api/studios/{studio-id}/v1/actors/{actor-id})           |
|    |-- GET/PATCH/DELETE "" ---------> [AgentActor] ---> [AgentDAO]      |
|    |-- POST ":addEndpoint" / ":removeEndpoint"                          |
|    |-- GET/POST "/inbox" & "/outbox"                                    |
|    +-- "/collections" --------------> [CollectionsRoute]                |
|          |-- POST "" (GET -> 404)            |                          |
|          +-- "/{collection-id}"              | Pekko Ask                |
|                |-- GET / PATCH / DELETE      v                          |
|                +-- POST ":addItem"    [CollectionActor]                 |
|                +-- POST ":removeItem"        |                          |
+----------------------------------------------+--------------------------+
                                               | Invokes (tenantId, actorId, ...)
                                               v
+-------------------------------------------------------------------------+
| Component Boundary: com.larpconnect.njall.data.dao                      |
|                                                                         |
|  [DefaultCollectionDAO] ---> [ActorGeneratorValidator]                  |
|         |                    (verifies generator is active 'Actor')     |
|         v                                                               |
|  [EntityBaseEntity (generator)] + [CollectionEntity (ownerId)]          |
+-------------------------------------------------------------------------+
```

- **Boundaries**: `AgentRoute` and `CollectionsRoute` are peer components inside `:api` (`api.studios.actors` -> `api.studios.collections`), communicating via Pekko `AskPattern` with `CollectionActor`, which invokes `CollectionDAO` inside `:data` over an `@NjallUsers` Hibernate session with PostgreSQL Row-Level Security (`app.tenant_id`).
- **Responsibilities**: `AgentRoute` resolves `{studio-id}` via `StudioLookupCache` and parses `{actor-id}` before delegating `/collections` subroutes to `CollectionsRoute`; `DefaultCollectionDAO` validates active **Actor** existence and `entities.generator = actorId` inside the database transaction.
- **Key Relationships**: Peer package delegation (`actors` -> `collections`) mirrors the existing `LocationsRoute` -> `AddressesRoute` composition and satisfies ArchUnit's downward/outward package dependency rule (`ADR-0011`) without introducing upward cycles.
- **Assumptions**: All studio routes retain `/v1` immediately after `{studio-id}` (`/api/studios/{studio-id}/v1/actors/{actor-id}/collections`), consistent with `/inbox`, `/outbox`, and `/locations/{location-id}/addresses`.
- **Open Questions**: None blocking implementation; ADR supersession/amendment scope is recorded below for the `adr` phase.

## Decisions

### 1. Subordinate Route Delegation from `AgentRoute` to `CollectionsRoute`
- **Choice**: Remove `CollectionsRoute` from `StudioSubRoutes` and `RouteProvider` multibindings (if bound), inject `CollectionsRoute` into `AgentRoute`, and delegate `pathPrefix("collections", () -> collectionsRoute.collectionsSubroutes(lookup, studioIdParam, actorId))` inside `AgentRoute.buildActorSubroutes`.
- **Rationale**: `AgentRoute` already owns the `/api/studios/{studio-id}/v1/actors/{actor-id}` prefix (`pathPrefix(PathMatchers.segment(), ...)`). Delegating from `AgentRoute` avoids route prefix shadowing in Pekko HTTP, eliminates duplicate `StudioLookupCache` and `{actor-id}` UUID parsing, and follows the exact precedent established by `LocationsRoute` delegating `/addresses` to `AddressesRoute`.
- **Alternatives Considered**:
  - Keeping `CollectionsRoute` as a standalone top-level route in `StudioSubRoutes` ordered ahead of `AgentRoute`. Rejected because any unmatched segment under `/actors/{actor-id}` would require fragile rejection backtracking across sibling routes and duplicate studio/actor path parsing.
  - Merging all collection handlers directly into `AgentRoute` and `AgentActor`. Rejected because it would breach the 500-SLOC class limit and 20-type package limit (`ADR-0032`).

### 2. Bind `{actor-id}` to `entities.generator` While Retaining Optional `ownerId`
- **Choice**: `{actor-id}` in `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` always maps to `njall_users.entities.generator`. On `POST`, `CreateCollectionRequest` drops `generator` and declares `Optional<UUID> ownerId`, defaulting `effectiveOwnerId = request.ownerId().orElse(actorId)`. On all collection operations (`POST`, `GET`, `PATCH`, `DELETE`, `:addItem`, `:removeItem`), if `{actor-id}` does not resolve to an active `'Actor'` **Entity** in `tenantId`, or if `collection.generator()` does not equal `{actor-id}`, the endpoint returns `404 Not Found`. If an explicit `ownerId` on `POST` does not resolve to an active **Entity** in `tenantId`, the endpoint returns `400 Bad Request`.
- **Rationale**: Preserves semantic attachment of collections to non-**Actor** entities (such as an **Individual** gallery or **Events** roster) while making the common self-owned **Actor** collection case ergonomic (`{"name": "..."}`) and guaranteeing URL hierarchy integrity (`404` when parent `{actor-id}` is missing, soft-deleted, or mismatched).
- **Alternatives Considered**:
  - Collapsing `collections.owner_id` into `entities.generator` so collections can only attach to **Actor** entities. Rejected because semantic attachment to non-**Actor** entities (established in ADR-0037 and ADR-0039) remains valuable.
  - Keeping `generator` in `CreateCollectionRequest`. Rejected as redundant and error-prone once `{actor-id}` is present in the resource path.

### 3. Rename `njall_users.entities.owner` to `generator` via Flyway `V13` and Across `:data`
- **Choice**: Add `V13__rename_entities_owner_to_generator.sql` executing `ALTER TABLE njall_users.entities RENAME COLUMN owner TO generator`, renaming `fk_entities_owner` to `fk_entities_generator`, and replacing `idx_entities_owner` with `idx_entities_generator` on `(tenant_id, generator DESC)`. Rename `EntityBaseEntity.owner` -> `generator`, `ActorOwnerValidator` -> `ActorGeneratorValidator`, and `UUID owner` -> `UUID generator` across all CTI domain records (`Agent`, `Collection`, `Event`, `Hashtag`, `Individual`, `Link`, `Location`, `ExpandedEntity`) and tenanted **DAO** interfaces.
- **Rationale**: Eliminates the confusing `Collection(UUID id, UUID owner, UUID ownerId, ...)` field collision and aligns SQL, JPA, domain records, REST JSON (`"generator"`), and the business glossary (`generator` vs `owner`).
- **Alternatives Considered**:
  - Editing `V12__entities_owner.sql` in place. Rejected because Flyway migrations are append-only once committed; mutating `V12` would break checksum validation on existing environments.
  - Keeping the SQL column named `owner` and only renaming the Java accessor. Rejected because native SQL queries (`EXPANDED_ITEMS_QUERY`, `AgentCollectionsHelper`, seed scripts) still touch the column directly, perpetuating the terminology split.

## Risks / Trade-offs

- **[Risk]** Existing clients or integration fixtures calling `/api/studios/{studio-id}/v1/collections` or passing `"generator"` in `CreateCollectionRequest` break due to the path and DTO change -> **Mitigation**: Update `openapi.yaml` and all `:api` and `:integration` Cucumber scenarios in lockstep; Jackson `FAIL_ON_UNKNOWN_PROPERTIES` behavior is tested explicitly in `:api` DTO/route tests.
- **[Risk]** Local Docker seed scripts (`docker/postgres/seed/02-test-studios.sql`, `03-studio-links.sql`, `04-studio-locations.sql`) fail if they still reference column `owner` on `njall_users.entities` after `V13` runs -> **Mitigation**: Update all seed SQL files and migration/DAO Testcontainers suites to use `generator`.
- **[Risk]** `AgentRoute` constructor parameter count could approach Checkstyle's 8-parameter limit (`ADR-0030`) when injecting `CollectionsRoute` -> **Mitigation**: `AgentRoute` currently has 5 parameters (`studioLookupCache`, `agentActor`, `system`, `objectMapper`, `askTimeout`); adding `CollectionsRoute` brings it to 6, well within the 8-parameter cap.

## Migration Plan

1. Add Flyway migration `data/src/main/resources/db/migration/V13__rename_entities_owner_to_generator.sql` renaming `njall_users.entities.owner`, constraint `fk_entities_owner`, and index `idx_entities_owner` to `generator` equivalents.
2. Update `docker/postgres/seed/02-test-studios.sql`, `03-studio-links.sql`, and `04-studio-locations.sql` to insert into `njall_users.entities (..., generator, ...)`.
3. Refactor `:data` (`EntityBaseEntity`, `ActorGeneratorValidator`, domain records, `AgentCollectionsHelper`, `CollectionDAO` / `DefaultCollectionDAO` actor-generator scoping, and sibling tenanted DAOs) and verify `./gradlew :data:check`.
4. Update `openapi.yaml` and refactor `:api` (`StudioSubRoutes`, `AgentRoute`, `CollectionsRoute`, `CollectionCommand`, `CollectionActor`, `CreateCollectionRequest`, `CollectionResponseHelper`) and verify `./gradlew :api:check`.
5. Update `:integration` Cucumber feature files and step definitions for actor-parented collection endpoints and `entities.generator` assertions, then run `./gradlew check build`.
6. **Rollback**: Prior to production release, revert the branch and drop/recreate local containers; in persistent environments, apply a compensating migration renaming `generator` back to `owner`.

## Open Questions

- **In-Force ADRs to Supersede / Amend in the `adr` Step**:
  - [0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract](../../../../adr/0039-entity-owner-and-generator-provenance.md) specified `njall_users.entities.owner` and unparented `POST /api/studios/{studio-id}/v1/collections` with required `ownerId` and optional `generator`.
  - [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md) specified unparented `/api/studios/{studio-id}/v1/collections` endpoints.
  - [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md) defined `/api/studios/{studio-id}/v1/actors/{id}` subroutes (`inbox`, `outbox`, `:addEndpoint`, `:removeEndpoint`).
  - The `adr` step will record a new repository-level `adr/0040-*.md` superseding ADR-0039 and amending ADR-0037 and ADR-0038 without modifying any prior ADR files.

