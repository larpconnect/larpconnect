## 1. Database Schema & Migration

- [x] 1.1 Create `V6__hashtags.sql` migration introducing `njall_users.hashtags` and `njall_users.hashtags_entity` with composite primary keys, RLS policies, functional unique index on `(tenant_id DESC, LOWER(tag))`, and admin/user grants.
- [x] 1.2 Verify database migration runs cleanly against PostgreSQL test containers.

## 2. Data Persistence Layer

- [x] 2.1 Implement immutable domain record `Hashtag` in `com.larpconnect.njall.data.domain` implementing `DatabaseObject` with ErrorProne `@Immutable`.
- [x] 2.2 Create unit tests for `Hashtag` record validating properties, nonnull invariants, and helper methods.
- [x] 2.3 Implement JPA entity `HashtagEntity` and join entity `HashtagEntityMapping` in `com.larpconnect.njall.data.dao.studios`.
- [x] 2.4 Create `HashtagDAO` interface in `com.larpconnect.njall.data.dao.studios` defining `findById`, `findByTag`, `listAll`, `create`, `batchCreate`, `patch`, and `softDelete`.
- [x] 2.5 Implement `DefaultHashtagDAO` with multi-table CTI coordination, transaction-scoped `set_config('app.tenant_id', :tenantId, true)`, and case-insensitive lookups.
- [x] 2.6 Bind `HashtagDAO` to `DefaultHashtagDAO` in `StudioDaoModule` and verify `:data` unit/DAO tests pass.

## 3. OpenAPI Specification

- [x] 3.1 Update `api/src/main/resources/openapi.yaml` documenting `/api/studios/{studio-id}/v1/tags`, `/api/studios/{studio-id}/v1/tags/{tag-id}`, and `/api/studios/{studio-id}/v1/tags:batchCreate` with operation definitions, parameters, and schemas (`Tag`, `CreateTagRequest`, `BatchCreateTagsRequest`, `BatchCreateTagsResponse`, `UpdateTagRequest`).

## 4. API Layer Implementation

- [x] 4.1 Create `com.larpconnect.njall.api.studios.tags` subpackage with `@NullMarked` `package-info.java`.
- [x] 4.2 Implement request/response DTOs (`CreateTagRequest`, `BatchCreateTagsRequest`, `BatchCreateTagsResponse`, `UpdateTagRequest`, `TagResponse`) with JSON bindings.
- [x] 4.3 Implement `TagValidation` validating length (1-32), web-safe Unicode characters, and `#` stripping.
- [x] 4.4 Define `TagCommand` and `TagActorResponse` sealed protocol hierarchies.
- [x] 4.5 Implement `TagActor` handling idempotent creation, bulk creation, dual-identifier retrieval, listing, patching, and soft deletion.
- [x] 4.6 Implement `TagsRoute` handling path prefixes, in-memory `StudioLookupCache` resolution, UUID vs tag dual lookup, `:batchCreate` custom method, and Jackson marshalling.
- [x] 4.7 Create `TagsModule` Guice module, install it in `StudiosModule`, and aggregate `TagsRoute` in `StudiosRoute`.
- [x] 4.8 Add comprehensive unit tests in `:api` for `TagActorTest`, `TagsRouteTest`, and `TagValidationTest`.

## 5. Integration Acceptance Testing

- [x] 5.1 Implement Cucumber feature `user_studio_tags_api.feature` covering creation, duplicate idempotency, batch creation, dual lookup, list, patch, soft delete, and cross-tenant isolation.
- [x] 5.2 Implement step definitions in `:integration` exercising HTTP endpoints and verifying database state.
- [x] 5.3 Run ArchUnit and architectural tests in `:integration` ensuring package size limit (<= 20 types) and dependency invariants hold.

## 6. Verification & Quality Gates

- [x] 6.1 Run `./gradlew check build` to verify Spotless, Checkstyle, SpotBugs, ErrorProne, and JaCoCo coverage gates.
- [x] 6.2 Run `openspec validate user-studio-tags-api --type change --strict` to ensure planning artifacts are valid and coherent.
