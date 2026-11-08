## Why

In **Njall**, federated objects in the **User plane** adhere to **FeatherPub** and **ActivityStreams** conventions, where every published or stored **Entity** originates from a responsible **Actor**. Currently, the Common Table Inheritance (**CTI**) root table (`njall_users.entities`) records tenant identity and lifecycle timestamps (`created_on`, `updated_on`, `deleted_on`) but lacks a universal provenance column linking each **Entity** to the **Actor** that generated it. Meanwhile, `njall_users.actors` stores an `owner_id` column on the subtype table that duplicates this structural provenance concept, whereas `njall_users.collections` uses `owner_id` for semantic attachment (such as attaching an inbox, outbox, or forum collection to a parent **Entity**). Introducing an immutable `owner` column (`UUID NOT NULL`) on `njall_users.entities`—exposed across all studio REST API representations as a `generator` URI—establishes uniform **Actor** provenance across every **Entity** subtype while cleanly separating structural generation from semantic **Collection** attachment.

## What Changes

- Introduce Flyway migration `V12__entities_owner.sql` in the **Data plane**:
  - Add `owner UUID NOT NULL` to `njall_users.entities` with composite self-referencing foreign key `CONSTRAINT fk_entities_owner FOREIGN KEY (tenant_id, owner) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE` and supporting index `idx_entities_owner` on `(tenant_id, owner DESC)`.
  - Drop the redundant `owner_id` column, `fk_actors_owner` constraint, and `idx_actors_owner` index from `njall_users.actors`, consolidating **Actor** ownership onto `njall_users.entities.owner`.
  - Retain `njall_users.collections.owner_id` for semantic attachment of a **Collection** to its target **Entity** (distinct from `njall_users.entities.owner`, which records the **Actor** that generated the **Collection**).
- Update **DAO** and JPA persistence models in `:data`:
  - Map `owner` (`UUID`, `nullable = false`, `updatable = false`) on `EntityBaseEntity` and propagate it across all **CTI** domain records (`Agent`, `Collection`, `Event`, `Hashtag`, `Individual`, `Link`, `Location`, and `ExpandedEntity`).
  - Enforce in application **DAO** create paths (`DefaultAgentDAO`, `DefaultCollectionDAO`, `DefaultEventDAO`, `DefaultHashtagDAO`, `DefaultIndividualDAO`, `DefaultLinkDAO`, `DefaultLocationDAO`) that `owner` references an existing, non-deleted **Entity** in the same **Studio** **Tenant** whose `entity_type` is `'Actor'`.
  - Allow an **Actor** to be self-owned (`owner = id`) when created via `DefaultAgentDAO` if no explicit generator **Actor** is supplied, and set `entities.owner = actor.id` (alongside `collections.owner_id = actor.id`) on the automatically provisioned `inbox` and `outbox` **Collections**.
  - Default `Collection` generation (`owner`) to `ownerId` when `generator` is omitted on collection creation, verifying that the resolved `owner` is an active **Actor** in the same **Tenant**.
- Update tenanted studio HTTP REST contracts and `openapi.yaml` in `:api`:
  - Expose `"generator": "/api/studios/{studio-id}/v1/actors/{owner-id}"` across all **Entity** responses (`ActorResponse`, `OrderedCollectionResponse`, `EventResponse`, `IndividualResponse`, `LinkResponse`, `LocationResponse`, `TagResponse`) and hydrated collection items (`ExpandedEntity`).
  - **BREAKING**: Replace `ownerId` with `generator` on `CreateActorRequest` (optional; defaults to self-ownership) and `ActorResponse`.
  - Keep `ownerId` on `CreateCollectionRequest` and `OrderedCollectionResponse` for semantic attachment, and add optional `generator` to `CreateCollectionRequest` (inferring `generator = ownerId` when omitted) and required `generator` to `OrderedCollectionResponse`.
  - **BREAKING**: Require `generator` on `CreateEventRequest`, `CreateIndividualRequest`, `CreateLinkRequest`, `CreateLocationRequest`, and `CreateTagRequest` (including `BatchCreateTagsRequest` items).
  - Accept either a bare **Actor** UUID string or a canonical studio **Actor** URI (`/api/studios/{studio-id}/v1/actors/{actor-id}` matching the target `{studio-id}`) for the `generator` field in all creation requests, returning `400 Bad Request` when malformed, cross-studio, missing (where required), or referencing a non-existent, soft-deleted, or non-**Actor** **Entity**.
- Update local PostgreSQL seed scripts (`docker/postgres/seed/`) and Cucumber acceptance tests in `:integration` to provision studio **Actor** owners and verify `generator` URI contracts.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `database-migration`: Add Flyway migration `V12__entities_owner.sql` adding `owner` and `fk_entities_owner` to `njall_users.entities`, removing `owner_id` from `njall_users.actors`, and updating seed data to include studio **Actor** owners.
- `data-persistence`: Add immutable `owner` field to `EntityBaseEntity` and all **CTI** domain models, and enforce application-level validation that `owner` resolves to an active `'Actor'` **Entity** within the same **Tenant**.
- `user-studio-actors-api`: Replace `ownerId` with optional `generator` (accepting UUID or studio **Actor** URI and defaulting to self-ownership `owner = id`) on **Actor** creation and expose `generator` URI on `ActorResponse`.
- `user-studio-collections-api`: Retain `ownerId` for semantic attachment, add optional `generator` (defaulting to `ownerId` when `ownerId` is an **Actor**) on **Collection** creation, and expose `generator` URI on `OrderedCollectionResponse` and hydrated `ExpandedEntity` items.
- `user-studio-events-api`: Require `generator` (UUID or studio **Actor** URI referencing an active **Actor**) on event creation and expose `generator` URI on `EventResponse`.
- `user-studio-individuals-api`: Require `generator` (UUID or studio **Actor** URI referencing an active **Actor**) on individual creation and expose `generator` URI on `IndividualResponse`.
- `user-studio-links-api`: Require `generator` (UUID or studio **Actor** URI referencing an active **Actor**) on link creation and expose `generator` URI on `LinkResponse`.
- `user-studio-locations-and-addresses-api`: Require `generator` (UUID or studio **Actor** URI referencing an active **Actor**) on location creation (`PostalAddress` remains a non-**CTI** component) and expose `generator` URI on `LocationResponse`.
- `user-studio-tags-api`: Require `generator` (UUID or studio **Actor** URI referencing an active **Actor**) on single and batch tag creation and expose `generator` URI on `TagResponse`.

## Impact

- **Database & Migrations**: Adds `data/src/main/resources/db/migration/V12__entities_owner.sql` and updates `docker/postgres/seed/02-test-studios.sql`, `03-studio-links.sql`, and `04-studio-locations.sql`.
- **Data Module (`:data`)**: Updates `EntityBaseEntity`, `Agent`, `AgentEntity`, `DefaultAgentDAO`, `Collection`, `CollectionEntity`, `DefaultCollectionDAO`, `ExpandedEntity`, `Event`, `EventEntity`, `DefaultEventDAO`, `Hashtag`, `HashtagEntity`, `DefaultHashtagDAO`, `Individual`, `IndividualEntity`, `DefaultIndividualDAO`, `Link`, `LinkEntity`, `DefaultLinkDAO`, `Location`, `LocationEntity`, `DefaultLocationDAO`, and unit/integration tests.
- **API Module (`:api`)**: Updates `openapi.yaml`, studio request/response DTOs, Pekko actor command protocols, and routes (`AgentRoute`, `CollectionRoute`, `EventRoute`, `IndividualRoute`, `LinkRoute`, `LocationRoute`, `TagRoute`) with shared `generator` UUID/URI parsing and `generator` URI formatting.
- **Integration Module (`:integration`)**: Updates Cucumber step definitions and feature files across studio **Actor**, **Collection**, event, individual, link, location, and tag suites to include `generator` provenance and verify validation rules.

