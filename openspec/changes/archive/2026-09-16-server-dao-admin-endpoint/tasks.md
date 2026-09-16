## 1. Dependencies and Configuration Updates

- [x] 1.1 In `gradle/libs.versions.toml`, declare `pekko-http-jackson` (or Jackson libraries) if needed, and in `data/build.gradle.kts`, add `api(libs.hibernate.core)`.
- [x] 1.2 In `common/src/main/resources/reference.conf`, add configuration blocks for `larpconnect.data.database.admin` and `larpconnect.data.database.users` with connection pool settings.
- [x] 1.3 In `data/src/main/java/com/larpconnect/njall/data/config/`, expand `DatabaseConfig` to load admin and users session configurations.
- [x] 1.4 In `api/build.gradle.kts`, add dependency `api(project(":data"))`.

## 2. Persistence Layer Core Abstractions in `:data`

- [x] 2.1 In `data/src/main/java/com/larpconnect/njall/data/annotation/` (or `data`), create Guice qualifiers `@NjallAdmin` and `@NjallUsers`.
- [x] 2.2 In `data/src/main/java/com/larpconnect/njall/data/domain/DatabaseObject.java`, create sealed interface `DatabaseObject` requiring `UUID id()`.
- [x] 2.3 In `data/src/main/java/com/larpconnect/njall/data/dao/DAO.java`, create sealed interface `DAO<T extends DatabaseObject>` with `findById(UUID id)` and `list()`.
- [x] 2.4 In `data/src/main/java/com/larpconnect/njall/data/session/`, implement `SessionFactoryFactory` and Guice providers producing distinct `SessionFactory` instances for `@NjallAdmin` and `@NjallUsers`.
- [x] 2.5 Unit test session factory creation and configuration in `data/src/test/java/`.

## 3. Server Domain Models and ServerDAO in `:data`

- [x] 3.1 In `data/src/main/java/com/larpconnect/njall/data/domain/`, implement `RoleType` and `ContactType` enums matching PostgreSQL types `njall.trole` and `njall.tcontact`.
- [x] 3.2 In `data/src/main/java/com/larpconnect/njall/data/domain/`, implement immutable records `ServerContact` and `Server` (implementing `DatabaseObject`).
- [x] 3.3 In `data/src/main/java/com/larpconnect/njall/data/dao/`, create sealed interface `ServerDAO extends DAO<Server>`.
- [x] 3.4 In `data/src/main/java/com/larpconnect/njall/data/dao/DefaultServerDAO.java`, implement `ServerDAO` using package-private Hibernate entities and `@NjallAdmin SessionFactory`.
- [x] 3.5 In `data/src/main/java/com/larpconnect/njall/data/DataModule.java`, bind `ServerDAO` and install persistence session modules.
- [x] 3.6 Implement unit tests for `DefaultServerDAO` and domain records in `data/src/test/java/`. Ensure all `:data` checks and tests pass before proceeding to `:api`.

## 4. API Layer and Admin Server Endpoint in `:api`

- [x] 4.1 In `api/src/main/resources/openapi.yaml`, specify the `GET /api/admin/v1/servers` endpoint with camelCase JSON schema for `Server` and `ServerContact`.
- [x] 4.2 In `api/src/main/java/com/larpconnect/njall/api/admin/`, define Pekko Typed protocols `ServerAdminCommand` and `ServerAdminResponse`.
- [x] 4.3 In `api/src/main/java/com/larpconnect/njall/api/admin/`, implement `ServerAdminActor` and `ServerAdminActorFactory` executing `ServerDAO.list()` on a dedicated Pekko blocking dispatcher.
- [x] 4.4 In `api/src/main/java/com/larpconnect/njall/api/admin/DefaultAdminRoute.java`, mount `GET /api/admin/v1/servers` and wire response marshalling.
- [x] 4.5 In `api/src/main/java/com/larpconnect/njall/api/admin/AdminModule.java`, bind `ServerAdminActorFactory` and provide `ActorRef<ServerAdminCommand>`.
- [x] 4.6 Implement unit tests for `ServerAdminActor` using `BehaviorTestKit` and route tests using Pekko HTTP `RouteTest` in `api/src/test/java/`. Ensure all `:api` checks and tests pass before proceeding to `:server`.

## 5. Server Application Assembly in `:server`

- [x] 5.1 In `server/src/main/java/com/larpconnect/njall/server/ServerModule.java`, wire session factory coordinated shutdown hook to ensure Hibernate pools close gracefully on exit.
- [x] 5.2 Verify full application startup and unit test suite across `:server`.

## 6. Acceptance Testing and Validation in `:integration`

- [x] 6.1 Create Cucumber feature `servers_endpoint.feature` in `integration/src/test/resources/features/` with scenarios for `GET /api/admin/v1/servers`.
- [x] 6.2 Implement step definitions in `integration/src/test/java/com/larpconnect/njall/integration/` validating HTTP response status 200, JSON schema conformance, and seed data values.
- [x] 6.3 Run `openspec validate server-dao-admin-endpoint --type change --strict` to ensure specification compliance.
- [x] 6.4 Execute full repository build and verification suite (`./gradlew check build`) ensuring JaCoCo coverage (85% line / 90% branch) and static analysis checks pass.
