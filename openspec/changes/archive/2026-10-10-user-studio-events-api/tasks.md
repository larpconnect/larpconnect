## 1. Database Migration and Persistence Layer (:data)

- [x] 1.1 Create Flyway migration script `V8__events.sql` defining `njall_users.events` with CTI foreign key to `entities`, composite foreign key to `locations`, time ordering check constraint, indices, Row-Level Security policies, and role grants
- [x] 1.2 Implement migration unit test `V8EventsMigrationTest` validating table structure, RLS activation, and constraints
- [x] 1.3 Create immutable domain record `Event` implementing `DatabaseObject` with non-null components and boundary normalization
- [x] 1.4 Create JPA entity `EventEntity` mapping `njall_users.events` using `@IdClass(EntityId.class)`
- [x] 1.5 Define `EventDAO` interface and implement `DefaultEventDAO` providing tenanted CRUD operations (`create`, `findById`, `listAll`, `patch`, `softDelete`) under active `@NjallUsers` sessions
- [x] 1.6 Configure Guice dependency injection bindings for `EventDAO` within the `:data` module
- [x] 1.7 Implement comprehensive unit tests for `Event`, `EventEntity`, and `DefaultEventDAO` and verify `./gradlew :data:test` passes

## 2. API Contract and Pekko HTTP Routing (:api)

- [x] 2.1 Update `openapi.yaml` documenting `/api/studios/{studio-id}/v1/events` and `/api/studios/{studio-id}/v1/events/{id}` endpoints, request bodies, and schema components
- [x] 2.2 Create Jackson-annotated DTOs `CreateEventRequest`, `UpdateEventRequest`, and `EventResponse`
- [x] 2.3 Implement `EventValidation` utility for UUID parsing, non-blank title checks, and temporal order validation
- [x] 2.4 Implement sealed message protocols `EventCommand` and `EventActorResponse`
- [x] 2.5 Implement `EventActor` and `EventActorFactory` handling command execution and error translation
- [x] 2.6 Implement `EventsRoute` handling tenanted route routing, in-memory studio resolution via `StudioLookupCache`, and AIP-134 field mask processing
- [x] 2.7 Expose Guice bindings in `EventsModule` and mount `EventsRoute` within the studio route hierarchy
- [x] 2.8 Implement unit tests for `EventActor`, `EventValidation`, DTOs, and `EventsRoute` and verify `./gradlew :api:test` passes

## 3. Integration Testing and Verification Gates (:integration)

- [x] 3.1 Create Cucumber specification `user_studio_events_api.feature` in `:integration` modeling all create, list, get, patch, and soft-delete scenarios
- [x] 3.2 Implement Cucumber step definitions in `:integration` asserting API-to-queue and queue-to-data boundary behaviors
- [x] 3.3 Validate ArchUnit architectural rules, DAG enforcement, and package boundaries
- [x] 3.4 Validate OpenSpec change integrity by running `openspec validate user-studio-events-api --type change --strict`
- [x] 3.5 Execute full verification suite via `./gradlew check build` verifying Spotless formatting, Checkstyle, SpotBugs, ErrorProne, and JaCoCo coverage gates
