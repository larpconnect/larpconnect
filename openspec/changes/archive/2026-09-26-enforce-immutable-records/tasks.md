## 1. Build Logic & Static Analysis Configuration

- [x] 1.1 Add `compileOnly(libs.errorprone.annotations)` to `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts` to make `@Immutable` and `@ImmutableTypeParameter` available across all modules.
- [x] 1.2 Configure `options.errorprone.checkOptions.put("Immutable:KnownImmutable", "org.apache.pekko.actor.typed.ActorRef")` and `options.errorprone.error("Immutable")` in `njall.java-common-conventions.gradle.kts` to globally recognize Pekko `ActorRef` as immutable and enforce immutability during compilation.
- [x] 1.3 Validate conventions compilation with `./gradlew compileJava`.

## 2. Common Module Immutability Annotations

- [x] 2.1 Annotate `ServerConfig` with `@Immutable`.
- [x] 2.2 Annotate `TraceContext` with `@Immutable`.
- [x] 2.3 Annotate `ApiCall` with `@Immutable` and constrain type parameter `T` with `@ImmutableTypeParameter` (`public record ApiCall<@ImmutableTypeParameter T>(...)`).
- [x] 2.4 Run `./gradlew :common:test` to verify all `:common` tests pass.

## 3. Data Module Immutability & Collection Modernization

- [x] 3.1 Annotate domain entity records `AdminRole`, `AdminUser`, `Server`, `ServerContact`, and `StudioLookup` with `@Immutable`.
- [x] 3.2 Annotate configuration records `DatabaseConfig` and `SessionConfig` with `@Immutable`.
- [x] 3.3 Modernize `MigrationConfig`: replace `List<String> schemas` with `ImmutableList<String>` and `Map<String, String> placeholders` with `ImmutableMap<String, String>`, annotate record with `@Immutable`, and maintain overloaded constructors accepting standard `List` and `Map`.
- [x] 3.4 Run `./gradlew :data:test` to verify all `:data` tests pass.

## 4. API Module DTOs and Actor Command Protocols

- [x] 4.1 Annotate administrative request **DTO** records `CreateRoleRequest`, `CreateStudioRequest`, `CreateUserRequest`, and `RoleAssignmentRequest` with `@Immutable`.
- [x] 4.2 Annotate administrative response **DTO** record `AdminErrorResponse` with `@Immutable`.
- [x] 4.3 Annotate `HealthCheckCommand` and `HealthCheckResponse` records with `@Immutable`.
- [x] 4.4 Annotate `RoleAdminCommand` and `RoleAdminResponse` records with `@Immutable`.
- [x] 4.5 Annotate `ServerAdminCommand` and `ServerAdminResponse` records with `@Immutable`.
- [x] 4.6 Annotate `StudioAdminCommand` and `StudioAdminResponse` records with `@Immutable`.
- [x] 4.7 Annotate `UserAdminCommand` and `UserAdminResponse` records with `@Immutable`, and annotate `UserAdminActor.RoleResolution` records with `@Immutable`.
- [x] 4.8 Run `./gradlew :api:test` to verify all `:api` tests pass.

## 5. Server Module CLI Options Records

- [x] 5.1 Annotate `MigrationOptions` and `ServerOptions` in `com.larpconnect.njall.server.cli` with `@Immutable`.
- [x] 5.2 Run `./gradlew :server:test` to verify all `:server` tests pass.

## 6. ArchUnit Record Immutability Invariant

- [x] 6.1 Add `records_must_be_annotated_immutable` ArchRule in `ArchitectureTest` (`:integration` **library module**) asserting that all non-test `record` classes within `com.larpconnect.njall..` are annotated with `com.google.errorprone.annotations.Immutable`.
- [x] 6.2 Run `./gradlew :integration:test` to verify that ArchUnit architectural tests and Cucumber acceptance tests pass.

## 7. Verification & OpenSpec Validation

- [x] 7.1 Execute full build and quality verification with `./gradlew check build` to verify Spotless, Checkstyle, SpotBugs, ErrorProne, and JaCoCo coverage gates pass 100%.
- [x] 7.2 Run `openspec validate enforce-immutable-records --type change --strict` in WSL to verify the change specification.
