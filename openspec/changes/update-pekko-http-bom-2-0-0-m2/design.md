## Context

**Njall** centralizes third-party dependency versions in `gradle/libs.versions.toml` and enforces platform constraints in the `:parent` **Module** (`parent/build.gradle.kts`), which is imported by all **Library module** and **Application module** targets. Currently, `pekko` is at `2.0.0-M4` while `pekko-http` (`org.apache.pekko:pekko-http-bom_3`) is at `2.0.0-M1`.

Apache Pekko HTTP `2.0.0-M2` is built against Apache Pekko `2.0.0-M4`, Scala `3.3.8`, Jackson `2.22.x` (governed in **Njall** by `jackson-bom` `2.22.4`), and JUnit Jupiter `6.1.3`. Upgrading `org.apache.pekko:pekko-http-bom_3` to `2.0.0-M2` aligns the HTTP stack with the core **Actor** and stream runtime and incorporates upstream HTTP/2 and inflater/deflater resource-cleanup fixes.

In-force ADRs reviewed: `0001` through `0038` (excluding `0006` and `0019`, which are superseded by `0020`). Specifically, [ADR-0001](../../../adr/0001-pekko-http-runtime-architecture.md) establishes Apache Pekko Typed and Pekko HTTP across the multimodule pipeline.

## Goals / Non-Goals

**Goals:**
- Upgrade `pekko-http` in `gradle/libs.versions.toml` from `2.0.0-M1` to `2.0.0-M2`.
- Ensure all BOM-governed Pekko HTTP artifacts (`pekko-http_3`, `pekko-http-jackson_3`, `pekko-http-testkit_3`) resolve cleanly to `2.0.0-M2` via `:parent`.
- Verify zero compilation warnings/errors, static analysis violations, or test regressions across `:api`, `:server`, `:test`, and `:integration`.

**Non-Goals:**
- Introducing new HTTP endpoints, routes, or OpenAPI schema modifications.
- Changing database schemas, Flyway migrations, or **DAO** implementations.
- Altering `ServerConfig` or `CoordinatedShutdown` phase wiring in `:server`.

## Decisions

### Decision 1: Single-Point Version Catalog Upgrade via `pekko-http-bom_3`

Update `pekko-http = "2.0.0-M2"` in `gradle/libs.versions.toml` while retaining versionless library coordinates for `pekko-http`, `pekko-http-jackson`, and `pekko-http-testkit` governed by `api(platform(libs.pekko.http.bom))` in `:parent`.

- **Rationale**: Centralizing the version in `gradle/libs.versions.toml` and enforcing it through `:parent` guarantees that `pekko-http_3`, `pekko-http-core_3`, `pekko-parsing_3`, `pekko-http-jackson_3`, and `pekko-http-testkit_3` upgrade atomically across all modules without version drift.
- **Alternatives Considered**: Pinning individual Pekko HTTP artifact versions in `gradle/libs.versions.toml` or module build scripts. Rejected because it bypasses BOM alignment and risks transitive skew between `pekko-http_3` and `pekko-http-core_3`.

### Decision 2: Source Compatibility Verification Against Upstream `2.0.0-M2` API Removals

Audit all `org.apache.pekko.http.*` usages in `:api`, `:server`, `:test`, and `:integration` against upstream `v2.0.0-M1...v2.0.0-M2` changes before applying code edits.

- **Rationale**: Upstream Pekko HTTP `2.0.0-M2` removed several long-deprecated APIs (`HttpApp`, `Coder.encode`, `ConnectionContext.defaultPort`, `ParserSettings.apply/create(Config)`, 1-arg `UnsupportedRequestContentTypeRejection.apply`, and the deprecated `javadsl` `route` directive). Codebase inspection confirms **Njall** uses none of these removed APIs; routes use `AllDirectives` / `Directives`, marshalling uses `Jackson`, and **Server** binding uses `Http.get(system).newServerAt(...).bind(...)`.
- **Alternatives Considered**: Proactively refactoring route or testkit classes to use `JUnitJupiterRouteTest`. Rejected as unnecessary churn; existing route tests use `route().seal().function(system)` directly against an `ActorSystem` and remain fully supported in `2.0.0-M2`.

### Architecture & Dependency Topology (C4 Component-Level View)

```text
+-----------------------------------------------------------------------------------+
|                        GRADLE VERSION CATALOG & PLATFORM                          |
|                                                                                   |
|  [gradle/libs.versions.toml]                                                      |
|    pekko-http = "2.0.0-M2"                                                        |
|           |                                                                       |
|           v                                                                       |
|  [:parent (Java Platform Module)]                                                 |
|    api(platform("org.apache.pekko:pekko-http-bom_3:2.0.0-M2"))                    |
+-----------------------------------------+-----------------------------------------+
                                          | (Platform Version Constraints)
          +-------------------------------+-------------------------------+
          |                               |                               |
          v                               v                               v
+-------------------+           +-------------------+           +-------------------+
| :api              |           | :server           |           | :test             |
| (Library Module)  |           | (App Module)      |           | (Library Module)  |
| - pekko-http_3    |           | - pekko-http_3    |           | - pekko-http-     |
| - pekko-http-     |           | - HttpServer-     |           |   testkit_3       |
|   jackson_3       |           |   Service         |           +---------+---------+
+---------+---------+           +---------+---------+                     |
          |                               |                               |
          +---------------+---------------+-------------------------------+
                          |
                          v
                +-------------------+
                | :integration      |
                | (Library Module)  |
                | - Cucumber E2E    |
                | - ArchUnit        |
                +-------------------+
```

- **Boundaries**: `gradle/libs.versions.toml` and `:parent` form the version governance boundary; `:api`, `:server`, `:test`, and `:integration` consume BOM-constrained Pekko HTTP artifacts.
- **Responsibilities**: `:parent` imports `pekko-http-bom_3:2.0.0-M2`; `:api` defines HTTP routes and Jackson marshalling; `:server` binds the TCP socket and registers `CoordinatedShutdown`; `:test` provides testkit utilities.
- **Key Relationships**: All modules inherit `platform(project(":parent"))`, ensuring uniform resolution of `2.0.0-M2` across compile and test classpaths.
- **Assumptions**: Diagram uses ASCII format and lightweight C4-inspired component/module rigor to depict existing codebase module boundaries.
- **Open Questions**: None.

## Risks / Trade-offs

- **[Risk] Transitive Jackson or JUnit version conflict from `pekko-http-jackson_3` or `pekko-http-testkit_3` `2.0.0-M2`** -> **Mitigation**: `pekko-http-jackson_3:2.0.0-M2` declares `jackson-databind:2.22.3` and `junit-jupiter:6.1.3`; `:parent` already enforces `jackson-bom:2.22.4` and `:test` enforces `junit-bom:6.1.3`. Verify resolution and test execution via `./gradlew check build`.
- **[Risk] Stricter `Content-Length` or HTTP/2 header parsing in `2.0.0-M2` affecting existing route or Cucumber tests** -> **Mitigation**: Run `:api:test`, `:server:test`, and `:integration:test` sequentially before full `./gradlew check build`.

## Migration Plan

1. Update `pekko-http = "2.0.0-M2"` in `gradle/libs.versions.toml`.
2. Run module tests in dependency order (`:api:test`, `:server:test`, `:integration:test`) and resolve any compiler or linter findings if surfaced.
3. Run `./gradlew check build` to verify all unit tests, integration tests, JaCoCo coverage gates, Spotless, Checkstyle, SpotBugs, and ErrorProne checks.
4. Rollback strategy: Revert `pekko-http` in `gradle/libs.versions.toml` to `"2.0.0-M1"`.

## Open Questions

None. No in-force ADRs require supersession.
