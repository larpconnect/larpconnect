# Execution & Verification Log: `actor-parented-collections-and-generator-rename`

- **Date**: 2026-11-13
- **OpenSpec Change**: [`openspec/changes/actor-parented-collections-and-generator-rename/proposal.md`](../../openspec/changes/actor-parented-collections-and-generator-rename/proposal.md)
- **Design**: [`openspec/changes/actor-parented-collections-and-generator-rename/design.md`](../../openspec/changes/actor-parented-collections-and-generator-rename/design.md)
- **Tasks**: [`openspec/changes/actor-parented-collections-and-generator-rename/tasks.md`](../../openspec/changes/actor-parented-collections-and-generator-rename/tasks.md)

## Summary of Implemented Changes

### 1. Database Migration, Seed Scripts, and Data Persistence (`:data`, `docker/postgres/seed/`)
- Added Flyway migration [`data/src/main/resources/db/migration/V13__rename_entities_owner_to_generator.sql`](../../data/src/main/resources/db/migration/V13__rename_entities_owner_to_generator.sql) renaming `njall_users.entities.owner` to `generator`, constraint `fk_entities_owner` to `fk_entities_generator`, and replacing `idx_entities_owner` with `idx_entities_generator` on `(tenant_id, generator DESC)` while retaining `njall_users.collections.owner_id`, `fk_collections_owner`, `unq_collections_owner_name`, and `idx_collections_owner`.
- Updated seed scripts [`docker/postgres/seed/02-test-studios.sql`](../../docker/postgres/seed/02-test-studios.sql), [`docker/postgres/seed/03-studio-links.sql`](../../docker/postgres/seed/03-studio-links.sql), and [`docker/postgres/seed/04-studio-locations.sql`](../../docker/postgres/seed/04-studio-locations.sql) to insert into `njall_users.entities.generator`.
- Renamed `EntityBaseEntity.owner` to `generator` (`getGenerator()`), replaced `ActorOwnerValidator` with [`ActorGeneratorValidator`](../../data/src/main/java/com/larpconnect/njall/data/dao/common/ActorGeneratorValidator.java) (`isActiveActor`, `validateActorGenerator`, `validateActiveEntity`), and renamed `owner` -> `generator` across all 8 `@Immutable` CTI domain records (`Agent`, `Collection`, `Event`, `Hashtag`, `Individual`, `Link`, `Location`, `ExpandedEntity`).
- Updated [`CollectionDAO`](../../data/src/main/java/com/larpconnect/njall/data/dao/studios/collections/CollectionDAO.java), [`DefaultCollectionDAO`](../../data/src/main/java/com/larpconnect/njall/data/dao/studios/collections/DefaultCollectionDAO.java), [`CollectionItemHelper`](../../data/src/main/java/com/larpconnect/njall/data/dao/studios/collections/CollectionItemHelper.java), [`AgentCollectionsHelper`](../../data/src/main/java/com/larpconnect/njall/data/dao/studios/actors/AgentCollectionsHelper.java), and sibling tenanted DAOs (`AgentDAO`, `EventDAO`, `HashtagDAO`, `IndividualDAO`, `LinkDAO`, `LocationDAO`) to use `generator` provenance and scope `CollectionDAO` reads and mutations by active generating `actorId` while validating active `ownerId` on `create`.
- Converted `DefaultCollectionDAO.ScopedCollection` from a `record` to a `private static final class` to satisfy ArchUnit's `records_must_be_annotated_immutable` rule.

### 2. OpenAPI Contract and Actor-Parented Studio Routes (`:api`)
- Updated [`api/src/main/resources/openapi.yaml`](../../api/src/main/resources/openapi.yaml) to repath all collection endpoints under `/api/studios/{studio-id}/v1/actors/{actor-id}/collections...` (`POST ""`, `GET`/`PATCH`/`DELETE "/{collection-id}"`, `POST "/{collection-id}:addItem"`, `POST "/{collection-id}:removeItem"`), remove `generator` from `CreateCollectionRequest`, and make `ownerId` optional (`required: [name]`).
- Removed top-level `CollectionsRoute` wiring from [`StudioSubRoutes`](../../api/src/main/java/com/larpconnect/njall/api/studios/StudioSubRoutes.java) and delegated `collections` subroutes from [`AgentRoute`](../../api/src/main/java/com/larpconnect/njall/api/studios/actors/AgentRoute.java) to [`CollectionsRoute`](../../api/src/main/java/com/larpconnect/njall/api/studios/collections/CollectionsRoute.java).
- Updated [`CreateCollectionRequest`](../../api/src/main/java/com/larpconnect/njall/api/studios/collections/CreateCollectionRequest.java), [`CollectionCommand`](../../api/src/main/java/com/larpconnect/njall/api/studios/collections/CollectionCommand.java), [`CollectionActor`](../../api/src/main/java/com/larpconnect/njall/api/studios/collections/CollectionActor.java), and [`CollectionResponseHelper`](../../api/src/main/java/com/larpconnect/njall/api/studios/common/CollectionResponseHelper.java) to propagate `actorId`, default `ownerId` to `actorId`, return `404` on missing/soft-deleted/non-Actor `{actor-id}`, generator mismatch, `GET .../collections`, and legacy `/v1/collections...` routes, and return `400` on invalid explicit `ownerId`.
- Updated `.owner()` -> `.generator()` across studio actors (`AgentActor`, `EventActor`, `IndividualActor`, `LinkActor`, `LocationActor`, `TagActor`) and unit/route tests.

### 3. End-to-End Cucumber Integration Tests (`:integration`)
- Updated [`integration/src/test/resources/features/user_studio_collections_api.feature`](../../integration/src/test/resources/features/user_studio_collections_api.feature) to target actor-parented collection endpoints and cover omitted `ownerId` defaulting, explicit non-Actor entity `ownerId`, nonexistent/soft-deleted `ownerId` (`400`), missing/soft-deleted/non-Actor `{actor-id}` (`404`), mismatched `{actorB}` (`404` without mutating state), `GET .../collections` (`404`), and legacy `/v1/collections` routes (`404`).
- Added V13 schema verification scenario and step definitions in [`integration/src/test/resources/features/database_migration.feature`](../../integration/src/test/resources/features/database_migration.feature) and [`EntityGeneratorMigrationSteps`](../../integration/src/test/java/com/larpconnect/njall/integration/EntityGeneratorMigrationSteps.java).

## Verification Results

- **OpenSpec Strict Validation (`4.1`)**:
  - Command: `wsl openspec validate "actor-parented-collections-and-generator-rename" --type change --strict`
  - Result: Exit code `0` (`Change 'actor-parented-collections-and-generator-rename' is valid`)
- **OpenSpec Task Progress**:
  - Command: `wsl openspec instructions apply --change "actor-parented-collections-and-generator-rename" --json`
  - Result: `17/17` tasks complete (`"state": "all_done"`, `"remaining": 0`)
- **Spotless Formatting (`4.2`)**:
  - Command: `wsl ./gradlew spotlessApply`
  - Result: Exit code `0` (`BUILD SUCCESSFUL in 3s`, `21 actionable tasks: 21 up-to-date`)
- **Full Build & Verification Suite (`4.3`)**:
  - Command: `wsl ./gradlew check build`
  - Result: Exit code `0` (`BUILD SUCCESSFUL in 1m 30s`, `88 actionable tasks: 18 executed, 2 from cache, 68 up-to-date`)
  - Gates passed across all modules: `spotlessCheck`, `checkstyleMain`, `checkstyleTest`, `spotbugsMain`, `spotbugsTest`, `test`, `jacocoTestReport`, `jacocoTestCoverageVerification`, `check`, `build` (including ArchUnit rules in `:integration:test`).
  - Test results by module (`*/build/test-results/test/TEST-*.xml`):
    - `:common`: `25` tests, `0` failures, `0` errors, `0` skipped
    - `:test`: `1` test, `0` failures, `0` errors, `0` skipped
    - `:data`: `380` tests, `0` failures, `0` errors, `0` skipped
    - `:api`: `447` tests, `0` failures, `0` errors, `0` skipped
    - `:server`: `64` tests, `0` failures, `0` errors, `0` skipped
    - `:integration`: `117` tests (`7` ArchUnit + `110` Cucumber scenarios), `0` failures, `0` errors, `0` skipped
    - **Total**: `1034` tests, `0` failures, `0` errors, `0` skipped
