## 1. Common Module Refactoring (:common)

- [x] 1.1 In `common`, create `ApiCall<T>(T call, Optional<TraceContext> context)` in `com.larpconnect.njall.common.telemetry`.
- [x] 1.2 In `common`, create `TraceparentParser` in `com.larpconnect.njall.common.telemetry` to parse W3C headers into `Optional<TraceContext>`.
- [x] 1.3 In `common`, update `TraceContext` to remove static factory methods (`fromSpan`, `fromSpanContext`, `parseTraceparent`), adding public constructors `TraceContext(Span)` and `TraceContext(SpanContext)`.
- [x] 1.4 In `common`, update `ServerConfig` to remove `.of(...)` and `.fromConfig(...)`, adding overloaded convenience constructor `ServerConfig(String host, int port)`.
- [x] 1.5 In `common`, update `ConfigModule` to parse Typesafe `Config` directly inside `provideServerConfig` and instantiate `ServerConfig` via constructor.
- [x] 1.6 Run `./gradlew :common:test` to verify all unit tests pass in `:common`.

## 2. Data Module Refactoring (:data)

- [x] 2.1 In `data`, update domain entity records (`Server`, `ServerContact`, `AdminUser`, `StudioLookup`, `AdminRole`) to remove all static `.of(...)` factory methods.
- [x] 2.2 In `data`, update configuration records (`DatabaseConfig`, `MigrationConfig`, `SessionConfig`) to remove all static `.of(...)` and `.fromConfig(...)` factory methods.
- [x] 2.3 In `data`, create `SessionConfigFactory` using Guice Assisted Injection for path-scoped session configuration creation.
- [x] 2.4 In `data`, update `DatabaseConfigModule` to parse Typesafe `Config` directly in `@Provides` methods and bind `SessionConfigFactory`.
- [x] 2.5 In `data`, update all DAO, migration, and configuration unit tests to instantiate records via direct constructors.
- [x] 2.6 Run `./gradlew :data:test` to verify all unit tests pass in `:data`.

## 3. API Module Refactoring (:api)

- [x] 3.1 In `api`, update request and response DTO records (`AdminErrorResponse`, `CreateRoleRequest`, `CreateStudioRequest`, `CreateUserRequest`, `RoleAssignmentRequest`) to remove static `.of(...)` factory methods and add overloaded constructors.
- [x] 3.2 In `api`, update `HealthCheckCommand.CheckHealth` to remove `Optional<TraceContext>`, retaining only `ActorRef<HealthCheckResponse> replyTo`.
- [x] 3.3 In `api`, update `HealthCheckActor` to handle `ApiCall<HealthCheckCommand>`, extracting MDC trace attributes from `apiCall.context()`.
- [x] 3.4 In `api`, update `DefaultTracingDirective` and `DefaultAdminRoute` to use `TraceparentParser`, `new TraceContext(span)`, and wrap actor commands in `ApiCall`.
- [x] 3.5 In `api`, update all route and actor unit tests in `:api` to instantiate records and commands using constructors and `ApiCall`.
- [x] 3.6 Run `./gradlew :api:test` to verify all unit tests pass in `:api`.

## 4. Server Module Refactoring (:server)

- [x] 4.1 In `server`, update CLI runners, commands, and service bindings to use record constructors and updated config providers.
- [x] 4.2 Run `./gradlew :server:test` to verify all unit tests pass in `:server`.

## 5. Integration Module and ArchUnit Rule Enforcement (:integration)

- [x] 5.1 In `integration`, implement the ArchUnit rule `records_must_not_declare_factory_methods` in `ArchitectureTest.java` asserting that record classes in `com.larpconnect.njall..` declare zero static factory methods returning the record type or `Optional<Record>`.
- [x] 5.2 In `integration`, update all Cucumber test steps (`AdminManagementApiSteps`, `DatabaseMigrationSteps`, `TelemetryTracingSteps`, `ServersEndpointSteps`, etc.) to use record constructors, `ApiCall`, and updated providers.
- [x] 5.3 Run `./gradlew :integration:test` to verify all Cucumber acceptance features and ArchUnit rules pass.

## 6. End-to-End Build and Spec Validation

- [x] 6.1 Run `openspec validate ban-record-factory-methods --type change --strict` to verify delta specifications.
- [x] 6.2 Execute full repository verification via `./gradlew check build` ensuring JaCoCo (85% line, 90% branch), Spotless, Checkstyle, SpotBugs, ErrorProne, and ArchUnit gates pass cleanly.
