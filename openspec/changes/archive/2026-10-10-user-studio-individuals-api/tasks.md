## 1. Database Migration & Persistence Schema

- [x] 1.1 Create Flyway migration `V9__individuals.sql` defining `njall_users.individuals` table with composite primary key `(tenant_id, id)` referencing `njall_users.entities(tenant_id, id) ON DELETE CASCADE`, unique constraint `unq_individuals_id` on `id`, and non-null `name`.
- [x] 1.2 Enable Row-Level Security on `njall_users.individuals` with tenant-isolation policy `rls_individuals` for role `njall_users` and administrative bypass policy `rls_individuals_admin` for role `njall_admin`.
- [x] 1.3 Configure table permissions granting `SELECT, INSERT, UPDATE, DELETE` to `njall_users` and `ALL` to `njall_admin`.

## 2. Data Access Layer (:data)

- [x] 2.1 Create immutable domain record `Individual` in `com.larpconnect.njall.data.domain` adhering to pure data carrier and non-null invariants.
- [x] 2.2 Create JPA mapping class `IndividualEntity` in `com.larpconnect.njall.data.dao.studios` mapped to `njall_users.individuals` with `EntityId` composite key.
- [x] 2.3 Define interface `IndividualDAO` and implement `DefaultIndividualDAO` supporting `create`, `findById`, `patch`, and `softDelete` using `@NjallUsers Provider<SessionFactory>`.
- [x] 2.4 Bind `IndividualDAO` to `DefaultIndividualDAO` as a `@Singleton` in `StudiosDaoModule`.
- [x] 2.5 Author unit tests in `:data` for `DefaultIndividualDAO`, `IndividualEntity`, and `Individual` achieving >=85% line and >=90% branch coverage. Verify `:data` tests pass before progressing.

## 3. OpenAPI Contract & Protocol DTOs (:api)

- [x] 3.1 Update `api/src/main/resources/openapi.yaml` with endpoints for `POST /api/studios/{studio-id}/v1/individuals` and `GET`, `PATCH`, `DELETE` under `/api/studios/{studio-id}/v1/individuals/{id}`, including schema components for `Individual`, `CreateIndividualRequest`, and `UpdateIndividualRequest`.
- [x] 3.2 Create immutable request and response records `CreateIndividualRequest`, `UpdateIndividualRequest`, `IndividualResponse`, and helper `IndividualValidation` in `com.larpconnect.njall.api.studios.individuals`.

## 4. Pekko Typed Actor & HTTP Route Layer (:api)

- [x] 4.1 Define sealed message protocols `IndividualCommand` and `IndividualActorResponse` in `com.larpconnect.njall.api.studios.individuals`.
- [x] 4.2 Implement `IndividualActor` behavior handling create, get, patch, and delete commands with IOSP-lite separation.
- [x] 4.3 Create factory interface `IndividualActorFactory` and implementation `DefaultIndividualActorFactory`.
- [x] 4.4 Implement `IndividualsRoute` extending `AllDirectives` and implementing `RouteProvider`, resolving studio via `StudioLookupCache` and dispatching actor commands.
- [x] 4.5 Bind route, actor factory, and actor in new Guice module `IndividualsModule` and install it in `StudiosModule`. Mount `individualsRoute` into `StudiosRoute.route()`.
- [x] 4.6 Author unit tests in `:api` for `IndividualsRoute`, `IndividualActor`, and validation helpers achieving >=85% line and >=90% branch coverage. Verify `:api` tests pass before progressing.

## 5. Integration & Acceptance Tests (:integration)

- [x] 5.1 Author Cucumber feature `user_studio_individuals.feature` in `:integration` covering POST creation, GET active individual, GET soft-deleted individual, PATCH with update_mask, DELETE soft deletion, 404 on collection GET, and cross-tenant isolation.
- [x] 5.2 Implement Cucumber step definitions in `:integration` targeting the HTTP routes and verifying database assertions.
- [x] 5.3 Validate architecture rules and package constraints via ArchUnit tests in `:integration`.

## 6. End-to-End Verification & Quality Gates

- [x] 6.1 Execute `./gradlew spotlessCheck` and run spotless formatting if needed.
- [x] 6.2 Execute full build and quality suite `./gradlew check build` ensuring all linters (SpotBugs, ErrorProne, Checkstyle) and JaCoCo coverage thresholds pass across all modules.
- [x] 6.3 Validate OpenSpec change compliance via `openspec validate user-studio-individuals-api --type change --strict`.
