## 1. Module & Build Infrastructure

- [x] 1.1 Register `:data` module in `settings.gradle.kts` and add project constraint in `bom/build.gradle.kts`.
- [x] 1.2 Create `data/build.gradle.kts` applying `njall.java-library-conventions` with dependencies on `:common`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, Guice, and test dependencies (strictly excluding Pekko).
- [x] 1.3 Add `:data` implementation dependency to `server/build.gradle.kts` and verify dependency graph builds.

## 2. Migration Script & Configuration Layer

- [x] 2.1 Define database and migration configuration hierarchy in `common/src/main/resources/reference.conf` with default values and server placeholder mappings.
- [x] 2.2 Create typed configuration records `DatabaseConfig` and `MigrationConfig` in `com.larpconnect.njall.data.config`.
- [x] 2.3 Create `V1__bootstrap_schemas_and_servers.sql` in `data/src/main/resources/db/migration/` containing PostGIS extension, schemas, role search paths, privileges, enums (`trole`, `tcontact`), tables (`servers`, `server_contacts`), and placeholder seed inserts.
- [x] 2.4 Implement `DatabaseConfigModule` in `com.larpconnect.njall.data.config` and add unit tests verifying config extraction and validation.

## 3. Flyway Database Migrator Implementation

- [x] 3.1 Implement `DatabaseMigrator` interface and `FlywayDatabaseMigrator` in `com.larpconnect.njall.data.migration` managing transient `njall` connection creation, Flyway execution with placeholders, and immediate connection teardown.
- [x] 3.2 Implement `MigrationModule` in `com.larpconnect.njall.data.migration` and root `DataModule` in `com.larpconnect.njall.data` installing subpackage modules.
- [x] 3.3 Create unit tests in `data/src/test/` for `FlywayDatabaseMigrator`, `DataModule`, and `MigrationModule` verifying lifecycle and Guice bindings.

## 4. Server CLI Flag Integration

- [x] 4.1 Update `ServerModule` in `com.larpconnect.njall.server` to install `DataModule`.
- [x] 4.2 Update `ServerApp` argument handling to detect `--migrate`, execute `DatabaseMigrator`, and terminate with exit code 0 on success (or 1 on failure).
- [x] 4.3 Add unit tests in `server/src/test/` verifying CLI migration dispatch and standard runtime isolation.

## 5. Testcontainers Integration Verification & Quality Gates

- [x] 5.1 Implement a Testcontainers-backed Cucumber integration feature (`database_migration.feature`) and steps (`DatabaseMigrationSteps.java`) in `:integration` using `postgis/postgis:18-3.6-alpine` with superuser test role provisioning (`njall`, `njall_admin`, `njall_users`, `njall_system`).
- [x] 5.2 Assert bootstrap migration correctness in `:integration`: schemas created and owned by `njall`, enums created, seed data inserted with replaced variables, and role query permissions granted.
- [x] 5.3 Run `openspec validate data-flyway-migration --type change --strict` to ensure specification compliance.
- [x] 5.4 Run `./gradlew check build` across all modules ensuring Spotless, Checkstyle, SpotBugs, and JaCoCo coverage gates pass.
