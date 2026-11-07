## 1. Database Schema Migration, Seed Scripts, and Persistence Models (:data)

- [x] 1.1 Create Flyway migration `data/src/main/resources/db/migration/V12__entities_owner.sql` adding `owner UUID NOT NULL`, `fk_entities_owner` composite self-referencing foreign key `(tenant_id, owner) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE`, and `idx_entities_owner` on `njall_users.entities`, and dropping `idx_actors_owner`, `fk_actors_owner`, and `owner_id` from `njall_users.actors`.
- [x] 1.2 Update local PostgreSQL seed scripts (`docker/postgres/seed/02-test-studios.sql`, `03-studio-links.sql`, and `04-studio-locations.sql`) to provision a self-owned studio **Actor** for each seeded **Studio** and populate `njall_users.entities.owner` across all seeded entities.
- [x] 1.3 Update `EntityBaseEntity` in `:data` to map immutable `owner` (`@Column(name = "owner", nullable = false, updatable = false)`) without a setter, remove `ownerId` from `AgentEntity`, and update all **CTI** entity constructors (`AgentEntity`, `CollectionEntity`, `EventEntity`, `HashtagEntity`, `IndividualEntity`, `LinkEntity`, `LocationEntity`).
- [x] 1.4 Update immutable **CTI** domain records (`Agent`, `Collection`, `Event`, `Hashtag`, `Individual`, `Link`, `Location`, and `ExpandedEntity`) in `:data` to expose `UUID owner()`.
- [x] 1.5 Update tenanted **DAO** interfaces and implementations (`DefaultAgentDAO`, `DefaultCollectionDAO`, `DefaultEventDAO`, `DefaultHashtagDAO`, `DefaultIndividualDAO`, `DefaultLinkDAO`, `DefaultLocationDAO`) to accept `owner`, validate within the `@NjallUsers` transaction that `owner` references an active `'Actor'` **Entity** in the same **Tenant**, support self-owned **Actor** creation (`Optional<UUID> owner` defaulting to `id`), and set `entities.owner = actor.id` on provisioned `inbox` and `outbox` **Collections**.
- [x] 1.6 Update `:data` unit and Testcontainers tests to cover `owner` provenance, self-owned **Actor** creation, and non-**Actor** / soft-deleted owner rejection, and verify `:data` tests and static checks pass (`./gradlew :data:check`).

## 2. OpenAPI Contract and Shared Generator URI Utilities (:api)

- [x] 2.1 Update `openapi.yaml` to replace `ownerId` with `generator` on `CreateActorRequest` (optional) and `ActorResponse`, add optional `generator` to `CreateCollectionRequest` and required `generator` to `OrderedCollectionResponse`, require `generator` on `CreateEventRequest`, `CreateIndividualRequest`, `CreateLinkRequest`, `CreateLocationRequest`, and `CreateTagRequest`, and include `generator` URI on `EventResponse`, `IndividualResponse`, `LinkResponse`, `LocationResponse`, `TagResponse`, and expanded collection item schemas.
- [x] 2.2 Implement a shared helper (`GeneratorUriSupport`) in `com.larpconnect.njall.api.studios` to parse `generator` inputs accepting either a bare **Actor** UUID or a canonical studio **Actor** URI (`/api/studios/{studio-id}/v1/actors/{actor-id}` matching the target studio) and format `owner` UUIDs as canonical studio **Actor** URIs.

## 3. Studio Routes, DTOs, and Pekko Actor Protocols (:api)

- [x] 3.1 Update **Actor** request/response **DTO** records (`CreateActorRequest`, `ActorResponse`), `AgentCommand`, `AgentActor`, `AgentValidation`, and `AgentRoute` to accept optional `generator` (UUID or URI, defaulting to self-ownership) and emit `generator` URI.
- [x] 3.2 Update **Collection** request/response **DTO** records (`CreateCollectionRequest`, `OrderedCollectionResponse`), `CollectionCommand`, `CollectionActor`, `CollectionValidation`, and `CollectionRoute` to retain `ownerId`, accept optional `generator` (defaulting to `ownerId` when omitted), and emit `generator` URI on `OrderedCollectionResponse` and hydrated `ExpandedEntity` items (including `/inbox` and `/outbox` views).
- [x] 3.3 Update event, individual, link, location, and tag request/response **DTO** records, Pekko actor commands/actors (`EventActor`, `IndividualActor`, `LinkActor`, `LocationActor`, `TagActor`), validators, and routes (`EventRoute`, `IndividualRoute`, `LinkRoute`, `LocationRoute`, `TagRoute`) to require and validate `generator` on creation and emit `generator` URI on all responses.
- [x] 3.4 Update `:api` unit tests (`BehaviorTestKit` actor tests, validation tests, and Pekko HTTP `RouteTest` suites) to verify UUID and URI `generator` inputs, defaulting rules, `400 Bad Request` error handling, and `generator` URI outputs, and verify `:api` tests and static checks pass (`./gradlew :api:check`).

## 4. End-to-End Integration and Behavioral Acceptance Tests (:integration)

- [x] 4.1 Update Cucumber step definitions and feature files in `:integration` (`actors.feature`, `collections.feature`, `events.feature`, `individuals.feature`, `links.feature`, `locations.feature`, `tags.feature`) to provision studio **Actor** generators, supply `generator` (both UUID and canonical URI forms) in creation requests, and assert `generator` URIs in responses and expanded collection items.
- [x] 4.2 Add Cucumber scenarios verifying self-owned **Actor** creation when `generator` is omitted, **Collection** `generator` inference from an **Actor** `ownerId`, and `400 Bad Request` rejection when a non-**Actor** or soft-deleted **Entity** is supplied as `generator`.
- [x] 4.3 Verify `:integration` tests and ArchUnit checks pass (`./gradlew :integration:check`).

## 5. Verification Gate, OpenSpec Validation, and Clean Build

- [x] 5.1 Validate the OpenSpec change strictly (`openspec validate entity-owner-and-generator --type change --strict`).
- [x] 5.2 Execute Spotless formatting across all modules (`./gradlew spotlessApply`).
- [x] 5.3 Execute full build and static verification gates (`./gradlew check build`) ensuring JaCoCo coverage (85% line, 90% branch), ErrorProne, SpotBugs, and Checkstyle pass without violations.

