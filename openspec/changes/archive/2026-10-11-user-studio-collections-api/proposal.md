## Why

In **Njall**, studios and users need the ability to organize, group, and sequence domain entities (such as individuals, events, or media links) into ordered sets. While the system currently models discrete entity types, it lacks a first-class grouping primitive capable of preserving deliberate item sequencing and exposing ActivityPub-compatible **OrderedCollection** semantics. Introducing tenanted **Collections** expands the **User plane** to support ordered entity curation, backed by Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS).

## What Changes

- Introduce a new Flyway migration `V10__collections.sql` in the **Data plane**:
  - `njall_users.collections`: Subtype table inheriting from `njall_users.entities` via CTI with composite primary key `(tenant_id, id)` and foreign key to `entities(tenant_id, id) ON DELETE CASCADE`.
  - Add composite foreign key `CONSTRAINT fk_collections_owner FOREIGN KEY (tenant_id, owner_id) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
  - Add uniqueness constraint `CONSTRAINT unq_collections_owner_name UNIQUE (tenant_id, owner_id, name)` ensuring collection names are distinct per owner.
  - Add index `idx_collections_owner` on `(tenant_id, owner_id DESC)` to support querying collections by owner.
  - `njall_users.collection_items`: Junction table with primary key `id UUID DEFAULT uuidv7()`, composite foreign key `(tenant_id, collection_id)` referencing `njall_users.collections (tenant_id, id) ON DELETE CASCADE`, composite foreign key `(refers_to_tenant_id, refers_to_id)` referencing `njall_users.entities (tenant_id, id) ON DELETE CASCADE`, `ordinal INTEGER NOT NULL` for deliberate item sequence, and `added_on TIMESTAMPTZ NOT NULL DEFAULT NOW()`.
  - Add uniqueness constraint `CONSTRAINT unq_collection_items_ref UNIQUE (tenant_id, collection_id, refers_to_id)` to ensure each entity appears at most once in a given collection.
  - Add index `idx_collection_items_order` on `(tenant_id, collection_id, ordinal ASC)`.
  - Enable PostgreSQL Row-Level Security (RLS) on both `collections` and `collection_items` with tenant isolation for `njall_users` and administrative bypass for `njall_admin`.
- Provide data layer mappings in `:data`:
  - `Collection` and `CollectionItem`: Immutable domain records implementing `DatabaseObject`.
  - `CollectionEntity` and `CollectionItemEntity`: JPA entities mapping tables with composite key classes.
  - `CollectionDAO` and `DefaultCollectionDAO`: Data access objects providing tenanted CRUD, item insertion at arbitrary positions (defaulting to prepending), and item removal under active `@NjallUsers` sessions.
- Expose tenanted HTTP REST endpoints in `:api`:
  - `POST   /api/studios/{studio-id}/v1/collections`: Create a collection with server-generated UUIDv7, validating non-blank `name` and existing `owner_id`. Returns HTTP 201 Created.
  - `GET    /api/studios/{studio-id}/v1/collections/{collection-id}`: Retrieve a collection by ID with optional `?form={compressed|expanded}` query parameter (defaults to `compressed`). Returns HTTP 200 OK or HTTP 404 Not Found.
  - `PATCH  /api/studios/{studio-id}/v1/collections/{collection-id}`: Apply partial updates (`name`, `summary`) adhering to Google AIP-134. Returns HTTP 200 OK or HTTP 404 Not Found.
  - `DELETE /api/studios/{studio-id}/v1/collections/{collection-id}`: Soft-delete collection via `entities.deleted_on`. Returns HTTP 204 No Content or HTTP 404 Not Found.
  - `POST   /api/studios/{studio-id}/v1/collections/{collection-id}:addItem`: Custom method (AIP-136) to append or insert an entity at a specific point (`before=<id>` or integer index). Returns HTTP 204 No Content.
  - `POST   /api/studios/{studio-id}/v1/collections/{collection-id}:removeItem`: Custom method (AIP-136) to remove an entity by `refersToId`. Returns HTTP 204 No Content.
  - Intentionally omit collection listing endpoint (`GET /api/studios/{studio-id}/v1/collections` returns HTTP 404 Not Found).
- Implement Pekko Typed actor `CollectionActor` and HTTP route `CollectionsRoute` in `:api` mounted into `StudiosRoute`.
- Document endpoints and payload schemas in `openapi.yaml`.

## Capabilities

### New Capabilities
- `user-studio-collections-api`: Defines tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/collections` for creating, retrieving with compressed/expanded form, updating, soft-deleting, and mutating items within studio collections using Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS).

### Modified Capabilities
None.

## Impact

- **Database**: Adds migration `V10__collections.sql` defining `njall_users.collections` and `njall_users.collection_items` with CTI foreign keys, ordering indices, uniqueness constraints, and RLS policies.
- **Data Module (`:data`)**: Adds `Collection`, `CollectionItem`, `CollectionEntity`, `CollectionItemEntity`, `CollectionDAO`, and `DefaultCollectionDAO`, registered in `StudiosDaoModule`.
- **API Module (`:api`)**: Adds `CollectionsRoute`, `CollectionActor`, command protocols, request/response DTOs, and updates `openapi.yaml`.
- **Integration Module (`:integration`)**: Adds Cucumber feature specifications asserting route validation, form expansion, ordered item mutations, and data persistence boundaries.
