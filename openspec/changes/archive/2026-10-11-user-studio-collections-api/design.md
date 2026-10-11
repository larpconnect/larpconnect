## Context

In **Njall**, studios and users curate collections of entities such as cast members, scheduled games, or media links. These groupings map semantically to ActivityStreams 2.0 / ActivityPub **OrderedCollection** structures. Following the architectural foundation established in [0005-sealed-dao-and-hibernate-dual-session-architecture.md](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025-user-space-studios-api-and-tenant-row-level-security.md](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md), and [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md), **Collections** are designed as a first-class Common Table Inheritance (CTI) subtype extending `njall_users.entities`, coupled with a subordinate ordered junction table `njall_users.collection_items`, and exposed through asynchronous Pekko Typed actors adhering to Google AIP standards.

## Goals / Non-Goals

**Goals:**
- Provide tenanted persistence for **Collections** under `njall_users.collections` and `njall_users.collection_items` with PostgreSQL Row-Level Security (RLS) enforcement.
- Link collections strictly to an owner **Entity** within the same studio tenant using composite foreign keys, enforcing unique collection names per owner.
- Maintain arbitrary user-defined ordering across collection items, defaulting to prepending (newest first, ordinal 0) while allowing insertions at specific targets (`before=<id>`) or integer indices.
- Support dual representation forms via `?form={compressed|expanded}`: compressed (default) emitting only referenced entity identifiers, and expanded embedding full domain entity payloads.
- Expose tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/collections` supporting creation, detail retrieval, AIP-134 partial updates, soft deletion, and AIP-136 custom methods (`:addItem`, `:removeItem`) returning minimal HTTP 204 No Content responses.
- Adhere to the established repository standards for immutable records, sealed Pekko protocols, Guice module bindings, and JaCoCo coverage gates.

**Non-Goals:**
- Pagination pages (`OrderedCollectionPage`) in this phase; collections are retrieved as whole collections.
- Cross-tenant federated collection resolution (reserved for future FeatherPub extensions).
- Collection listing endpoints across all collections of a studio.

## Architectural Diagrams (C4 Level 2 & 3)

### C4 Container Diagram
```text
+─────────────────────────────────────────────────────────────────────────────+
|                                  Njall Host                                 |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                            :api Verticle                              |  |
|  |  +────────────────+    +────────────────+    +─────────────────────+  |  |
|  |  |CollectionsRoute| ──>| StudioLookup   |    |   CollectionActor   |  |  |
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
|  |  |             DefaultCollectionDAO (@NjallUsers Session)          |  |  |
|  |  +────────────────────────────────┬────────────────────────────────+  |  |
|  +───────────────────────────────────┼───────────────────────────────────+  |
+──────────────────────────────────────┼──────────────────────────────────────+
                                       │ SQL (CTI + Junction + RLS)
+──────────────────────────────────────▼──────────────────────────────────────+
|                           PostgreSQL Database                               |
|  +─────────────────────────+            +────────────────────────────────+  |
|  |   njall_users.entities  | <───────── |    njall_users.collections     |  |
|  +─────────────────────────+ (CTI)      +───────────────┬────────────────+  |
|       ^               ^                                 │ 1                 |
|       │ (Owner FK)    │                                 │                   |
|       │               │ (RefersTo FK)                   │ N                 |
|       │               │                 +───────────────▼────────────────+  |
|       └───────────────┴──────────────── |  njall_users.collection_items  |  |
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
|  | CollectionsRoute | ────────────────────────> |   StudioLookupCache   |   |
|  +────────┬─────────+                           +───────────────────────+   |
|           │                                                                 |
|           │ Validate & Dispatch CollectionCommand                           |
|           ▼                                                                 |
|  +──────────────────+     Execute Command       +───────────────────────+   |
|  | CollectionActor  | ────────────────────────> |     CollectionDAO     |   |
|  +────────┬─────────+                           +───────────────────────+   |
|           │                                                                 |
|           ▼                                                                 |
|  +─────────────────────────+                                                |
|  | CollectionActorResponse |                                                |
|  | (Success/NotFound/...)  |                                                |
|  +─────────────────────────+                                                |
+─────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### 1. CTI Subtype for Collections referencing `njall_users.entities`
- **Choice**: Map `njall_users.collections` with composite primary key `(tenant_id, id)` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
- **Rationale**: Extends the established pattern from [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md) and [0036-tenanted-individuals-architecture.md](../../../../adr/0036-tenanted-individuals-architecture.md). Collections inherit common entity fields (`summary`, `created_on`, `updated_on`, `deleted_on`) and participate in polymorphic entity associations.
- **Alternatives Considered**: Independent table without CTI. Rejected because it duplicates audit tracking and prevents treating collections polymorphically as entities.

### 2. Composite Foreign Keys for Tenant Containment
- **Choice**: Enforce composite foreign keys for both the owner and referenced entities:
  - `FOREIGN KEY (tenant_id, owner_id) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE`
  - `FOREIGN KEY (tenant_id, collection_id) REFERENCES njall_users.collections (tenant_id, id) ON DELETE CASCADE`
  - `FOREIGN KEY (refers_to_tenant_id, refers_to_id) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE`
- **Rationale**: Guarantees database-level tenant isolation preventing cross-tenant collection ownership or dangling cross-tenant links.
- **Alternatives Considered**: Simple single-column foreign keys on `owner_id` and `collection_id`. Rejected because they do not guarantee tenant confinement at the relational layer.

### 3. Collection Name Uniqueness per Owner
- **Choice**: Add `CONSTRAINT unq_collections_owner_name UNIQUE (tenant_id, owner_id, name)`.
- **Rationale**: Allows an owner entity to maintain multiple collections (e.g. "Cast", "Crew", "Photos") while preventing duplicate names under the same owner.
- **Alternatives Considered**: Global unique index on `owner_id` (limiting each owner to a single collection). Rejected based on explicit requirements.

### 4. Ordered Items Junction with Integer Ordinals
- **Choice**: Store collection items in `njall_users.collection_items` with `ordinal INTEGER NOT NULL` indexed by `(tenant_id, collection_id, ordinal ASC)`.
- **Rationale**:
  - Default insertion shifts existing rows (`ordinal = ordinal + 1`) and inserts at `ordinal = 0` (newest first).
  - Targeted insertion before a specific entity (`before=<id>`) or at an integer index shifts rows at and above that index and inserts the new item precisely at that position.
  - Guarantees deterministic, stable ordering matching ActivityPub `OrderedCollection` requirements.
- **Alternatives Considered**: Fractional positioning (`DOUBLE PRECISION`). Rejected in favor of discrete integer indices that map directly to zero-based array indices.

### 5. Dual Representation Forms (`?form=compressed|expanded`)
- **Choice**: Expose `form` query parameter on `GET /api/studios/{studio-id}/v1/collections/{id}` defaulting to `compressed`.
  - `compressed`: Emits `orderedItems` as an array of UUID strings.
  - `expanded`: Hydrates each active referenced entity using polymorphic entity lookups and emits full entity JSON objects.
- **Rationale**: Minimizes bandwidth and serialization overhead for standard operations while allowing clients to retrieve full object graphs in a single request when needed.

### 6. AIP-136 Custom Methods for Item Mutations
- **Choice**: Implement `POST ...:addItem` and `POST ...:removeItem` returning HTTP 204 No Content.
- **Rationale**: Conforms to Google AIP-136 custom method conventions for actions that manipulate collection state without requiring full resource replacement. Returning 204 No Content minimizes payload serialization overhead.

## Risks / Trade-offs

- **[Risk]** Ordinal shifting overhead on large collections during insertions.
  - **Mitigation**: Studio collections represent curated groupings (typically dozens or hundreds of items), making transactional integer index shifts highly efficient within PostgreSQL.
- **[Risk]** Referenced entity soft deletion leaving ghost entries in `collection_items`.
  - **Mitigation**: Retrieval queries join against `entities` and filter `deleted_on IS NULL`, ensuring soft-deleted entities are automatically excluded from `orderedItems` and `totalItems`.

## Migration Plan

1. Apply Flyway migration `V10__collections.sql` creating tables, indices, RLS policies, and role grants.
2. Deploy updated `:data` module with `CollectionDAO` bindings.
3. Deploy updated `:api` verticle with `CollectionsRoute` mounted into `StudiosRoute`.

## Open Questions

None. All architectural decisions aligned during exploration.
