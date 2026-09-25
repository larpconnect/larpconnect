## 1. Data Module Streamlining

- [x] 1.1 Remove defensive `requireNonNull` checks and compact constructors from `MigrationConfig`, `DatabaseConfig`, and `SessionConfig`.
- [x] 1.2 Remove obsolete constructor and `fromConfig` NPE test methods in `MigrationConfigTest`, `DatabaseConfigTest`, and `SessionConfigTest`, modernizing JSON strings to text blocks.
- [x] 1.3 Remove defensive parameter `requireNonNull` checks from `DefaultSessionFactoryFactory`, `DefaultDataSourceFactory`, and `DefaultFlywayFactory`.
- [x] 1.4 Remove factory parameter NPE test cases in `DefaultSessionFactoryFactoryTest`, `DefaultDataSourceFactoryTest`, and `DefaultFlywayFactoryTest`.
- [x] 1.5 Remove defensive parameter `requireNonNull` checks from `DefaultServerDAO` and `DefaultStudioDAO`.
- [x] 1.6 Remove **DAO** parameter NPE test assertions in `DefaultServerDAOTest` and `DefaultStudioDAOTest`.
- [x] 1.7 Verify `:data:test` passes cleanly.

## 2. Common & Server Module Streamlining

- [x] 2.1 Remove defensive `requireNonNull(config)` check in `ServerConfig.fromConfig` and delete `fromConfig_nullConfig` NPE test in `ServerConfigTest`.
- [x] 2.2 Modernize multiline string concatenations to text blocks in `CliConfigBuilderTest` to eliminate ErrorProne warnings.
- [x] 2.3 Verify `:common:test` and `:server:test` pass cleanly.

## 3. API Module Streamlining

- [x] 3.1 Remove defensive parameter `requireNonNull` checks from `DefaultTracingDirective` and delete `trace_nullSupplier` test in `TracingDirectiveTest`.
- [x] 3.2 Remove defensive compact constructors and `requireNonNull` checks from `CreateUserRequest`, `CreateStudioRequest`, `CreateRoleRequest`, and `AdminErrorResponse`.
- [x] 3.3 Remove obsolete NPE test assertion in `CreateUserRequestTest`.
- [x] 3.4 Verify `:api:test` passes cleanly.

## 4. Verification & Validation

- [x] 4.1 Validate OpenSpec change structure with `openspec validate eliminate-unannotated-null-checks --type change --strict`.
- [x] 4.2 Execute full build and quality verification with `./gradlew check` to verify spotless, spotbugs, errorprone, checkstyle, and jacoco coverage gates pass 100%.
