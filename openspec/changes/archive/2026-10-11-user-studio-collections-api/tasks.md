## 1. Database Migration and Persistence Layer (:data)

- [x] 1.1 Create Flyway migration script `V10__collections.sql` defining `njall_users.collections` and `njall_users.collection_items` with CTI foreign keys, owner uniqueness constraint, item ordering index, Row-Level Security policies, and role grants
- [x] 1.2 Implement migration unit test `V10CollectionsMigrationTest` validating table structures, constraints, and RLS activation
- [x] 1.3 Create immutable domain records `Collection` and `CollectionItem` implementing `DatabaseObject` with non-null components and boundary normalization
- [x] 1.4 Create JPA entities `CollectionEntity` and `CollectionItemEntity` mapping tables with composite key classes
- [x] 1.5 Define `CollectionDAO` interface and implement `DefaultCollectionDAO` providing tenanted CRUD, shift-based `addItem`, `removeItem`, and `findById` supporting compressed/expanded forms under active `@NjallUsers` sessions
- [x] 1.6 Configure Guice dependency injection bindings for `CollectionDAO` within `StudiosDaoModule`
- [x] 1.7 Implement comprehensive unit tests for `Collection`, `CollectionEntity`, `CollectionItemEntity`, and `DefaultCollectionDAO` and verify `./gradlew :data:test` passes

## 2. API Contract and Pekko HTTP Routing (:api)

- [x] 2.1 Update `openapi.yaml` documenting `/api/studios/{studio-id}/v1/collections`, `/{collection-id}`, `:addItem`, and `:removeItem` endpoints, request bodies, query parameters (`?form`), and schema components
- [x] 2.2 Create Jackson-annotated DTOs `CreateCollectionRequest`, `UpdateCollectionRequest`, `AddItemRequest`, `RemoveItemRequest`, and `OrderedCollectionResponse`
- [x] 2.3 Implement `CollectionValidation` utility for UUID parsing, non-blank name checks, and placement specifier validation
- [x] 2.4 Implement sealed message protocols `CollectionCommand` and `CollectionActorResponse`
- [x] 2.5 Implement `CollectionActor` and `CollectionActorFactory` handling command execution and error translation
- [x] 2.6 Implement `CollectionsRoute` handling tenanted routing, in-memory studio resolution via `StudioLookupCache`, `?form` query handling, AIP-134 field mask processing, and AIP-136 custom methods (`:addItem`, `:removeItem`) returning HTTP 204 No Content
- [x] 2.7 Expose Guice bindings in `CollectionsModule` and mount `CollectionsRoute` within `StudioSubRoutes`
- [x] 2.8 Implement unit tests for `CollectionActor`, `CollectionValidation`, DTOs, and `CollectionsRoute` and verify `./gradlew :api:test` passes

## 3. Integration Testing and Verification Gates (:integration)

- [x] 3.1 Create Cucumber specification `user_studio_collections_api.feature` in `:integration` modeling create, get (compressed and expanded), patch, soft-delete, addItem (default, before, index), and removeItem scenarios
- [x] 3.2 Implement Cucumber step definitions in `:integration` asserting API-to-queue and queue-to-data boundary behaviors
- [x] 3.3 Validate ArchUnit architectural rules, DAG enforcement, and package boundaries
- [x] 3.4 Execute full verification suite via `./gradlew check build` verifying Spotless formatting, Checkstyle, SpotBugs, ErrorProne, and JaCoCo coverage gates
