## 1. Database Schema & Flyway Migration (Module: `:data`)

- [x] 1.1 Create Flyway migration script `V4__entities_and_links_cti.sql` under `data/src/main/resources/db/migration/` creating `njall_users.entities` and `njall_users.links` with composite primary keys, foreign key cascade, partial indices, Row-Level Security policies, and permissions.
- [x] 1.2 Write migration tests in `:data` verifying table structure, constraints, indices, and RLS behavior.
- [x] 1.3 Run `./gradlew :data:test` and verify that all database migration tests pass.

## 2. Data Plane Domain, Entities, and DAO (Module: `:data`)

- [x] 2.1 Create immutable `Link` domain record in `com.larpconnect.njall.data.domain` implementing `DatabaseObject`.
- [x] 2.2 Create package-private JPA entity classes `EntityBaseEntity` and `LinkEntity` with composite key mapping in `com.larpconnect.njall.data.dao`.
- [x] 2.3 Create `LinkDAO` non-sealed interface extending `DAO<Link>` with tenanted operations: `create`, `findById`, `patch`, and `softDelete`.
- [x] 2.4 Implement `DefaultLinkDAO` executing `SELECT set_config('app.tenant_id', :tenantId, true)` prior to executing operations against `@NjallUsers SessionFactory`.
- [x] 2.5 Register `LinkEntity` and `EntityBaseEntity` in `DaoModule` under the `@NjallUsers` entity multibinder, and bind `LinkDAO` to `DefaultLinkDAO`.
- [x] 2.6 Write unit tests in `DefaultLinkDAOTest` verifying CRUD operations, tenant RLS isolation, soft deletion, and nullability contracts.
- [x] 2.7 Run `./gradlew :data:check` and verify all tests and static analysis pass before proceeding.

## 3. OpenAPI Specification & DTOs (Module: `:api`)

- [x] 3.1 Update `api/src/main/resources/openapi.yaml` to document `/api/studios/{studio-id}/v1/links` and `/api/studios/{studio-id}/v1/links/{id}` with POST, GET, PATCH (AIP-134), and DELETE operations and schema definitions (`Link`, `CreateLinkRequest`, `UpdateLinkRequest`).
- [x] 3.2 Create immutable Jackson DTO records in `com.larpconnect.njall.api.studios` (`CreateLinkRequest`, `UpdateLinkRequest`, `LinkResponse`).
- [x] 3.3 Create validation helper methods for link payloads ensuring valid URI format and non-empty type strings.

## 4. Pekko Typed Actor & HTTP Route (Module: `:api`)

- [x] 4.1 Define sealed record message protocol `LinkCommand` (`CreateLink`, `GetLink`, `PatchLink`, `DeleteLink`) and `LinkActorResponse` (`Success`, `NotFound`, `BadRequest`, `Failure`).
- [x] 4.2 Implement Apache Pekko Typed `LinkActor` and `LinkActorFactory` delegating operations to `LinkDAO`.
- [x] 4.3 Implement `LinksRoute` under `/api/studios/{studio-id}/v1/links`, integrating `StudioLookupCache` tenant resolution, payload unmarshalling, actor dispatching, and error mapping.
- [x] 4.4 Bind `LinkActorFactory`, `LinkActor`, and `LinksRoute` in `StudiosModule`.
- [x] 4.5 Write unit tests for `LinkActor` using `BehaviorTestKit` and `LinksRouteTest` using Pekko HTTP TestKit.
- [x] 4.6 Run `./gradlew :api:check` and verify all tests and static analysis pass before proceeding.

## 5. End-to-End Cucumber Integration Testing (Module: `:integration`)

- [x] 5.1 Create Cucumber feature file `integration/src/test/resources/features/user_studio_links_api.feature` specifying scenarios for link creation, retrieval, patching, soft-deletion, and cross-tenant RLS isolation.
- [x] 5.2 Implement step definitions in `:integration` supporting link API assertions.
- [x] 5.3 Run `./gradlew :integration:test` and verify that all integration and Cucumber tests pass against Testcontainers PostgreSQL.

## 6. Verification & Strict OpenSpec Validation

- [x] 6.1 Run `./gradlew check build` across the entire project and verify Spotless, SpotBugs, ErrorProne, Checkstyle, and JaCoCo coverage gates pass.
- [x] 6.2 Run `openspec validate user-studio-links-api --type change --strict` to ensure all delta specs and change artifacts validate cleanly.
