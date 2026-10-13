## 1. Database Schema Migration and Persistence Models (:data)

- [x] 1.1 Create Flyway migration `V11__actors.sql` defining `njall_users.actors`, `njall_users.actor_endpoints`, functional indices, composite foreign keys, Row-Level Security policies, and role grants.
- [x] 1.2 Define immutable domain records `Agent` and `AgentEndpoint` in `com.larpconnect.njall.data.domain`.
- [x] 1.3 Implement JPA entity classes `AgentEntity`, `AgentEndpointEntity`, and composite key `AgentEndpointId` in `com.larpconnect.njall.data.dao.studios.actors`.
- [x] 1.4 Define interface `AgentDAO` and implement `DefaultAgentDAO` in `com.larpconnect.njall.data.dao.studios.actors` providing CRUD, endpoint upsert/removal, paired inbox/outbox **Collections** provisioning, and cascading soft-delete under `@NjallUsers` sessions.
- [x] 1.5 Register `AgentDAO` and `DefaultAgentDAO` bindings in `StudiosDaoModule` and configure Hibernate XML/package mapping.
- [x] 1.6 Author unit tests for `DefaultAgentDAO` with Testcontainers verifying Common Table Inheritance mappings, Row-Level Security isolation, username uniqueness, and collection cascading.

## 2. Pekko Concurrency and Command Protocols (:api)

- [x] 2.1 Define sealed message protocols `AgentCommand` and `AgentActorResponse` in `com.larpconnect.njall.api.studios.actors`.
- [x] 2.2 Implement stateless Pekko Typed actor `AgentActor` processing creation, retrieval, patch, deletion, endpoint mutations, and inbox/outbox collection interactions via `AgentDAO` and `CollectionDAO`.
- [x] 2.3 Define `AgentActorFactory` interface and `DefaultAgentActorFactory` implementation with Guice assisted injection.
- [x] 2.4 Author `BehaviorTestKit` and actor unit tests verifying command dispatching, error mapping, and response handling.

## 3. HTTP REST Routes and DTO Contracts (:api)

- [x] 3.1 Define immutable request and response **DTO** records (`CreateActorRequest`, `UpdateActorRequest`, `AddEndpointRequest`, `RemoveEndpointRequest`, `ActorResponse`) in `com.larpconnect.njall.api.studios.actors`.
- [x] 3.2 Implement `AgentValidation` providing validation routines for non-blank usernames, owner UUIDs, endpoint name formats (`^[a-zA-Z0-9]{1,31}$`), and AIP-134 update masks.
- [x] 3.3 Implement `AgentRoute` in `com.larpconnect.njall.api.studios.actors` mounting endpoints under `/api/studios/{studio-id}/v1/actors`, handling custom methods (`:addEndpoint`, `:removeEndpoint`), and routing inbox/outbox GET/POST requests.
- [x] 3.4 Bind `AgentRoute`, `AgentActorFactory`, and **Verticle** dependencies in `ActorsModule` and mount into `StudiosRoute`.
- [x] 3.5 Update `openapi.yaml` documenting all **Actor** endpoints, custom methods, request bodies, query parameters, and response schemas.
- [x] 3.6 Author Pekko HTTP RouteTest unit tests in `:api` verifying status codes, unmarshalling, validation rejections, and error payloads.

## 4. End-to-End Integration and Behavioral Acceptance Tests (:integration)

- [x] 4.1 Author Cucumber feature `actors.feature` asserting **Actor** creation, detail retrieval, partial updates, and soft deletion against Pekko routes.
- [x] 4.2 Author Cucumber scenarios asserting endpoint upsert (`:addEndpoint`) and removal (`:removeEndpoint`) with validation error assertions.
- [x] 4.3 Author Cucumber scenarios asserting inbox and outbox collection retrieval (`GET /inbox`, `GET /outbox`) and payload submission (`POST /inbox`, `POST /outbox`).
- [x] 4.4 Implement glue step definitions in `:integration` asserting HTTP and queue/database boundaries.
- [x] 4.5 Verify ArchUnit architecture rules pass for constructor visibility, package conventions, and DAG constraints.

## 5. Verification Gate and Clean Build

- [x] 5.1 Execute Spotless formatting across all modules (`./gradlew spotlessApply`).
- [x] 5.2 Execute full build and static verification gates (`./gradlew check build`) ensuring JaCoCo coverage (85% line, 90% branch), ErrorProne, SpotBugs, and Checkstyle pass without violations.
