## Context

In **Njall**, database connectivity across the **Data plane** is partitioned into distinct profiles: `migration` (transient Flyway migrations connecting as `njall`), `admin` (Hibernate session factory connecting as `njall_admin`), and `users` (Hibernate session factory connecting as `njall_users`).

Previously, [`reference.conf`](../../../../common/src/main/resources/reference.conf) provided empty fallback passwords (`password = ""`), and [`SessionConfig.java`](../../../../data/src/main/java/com/larpconnect/njall/data/config/SessionConfig.java) treated passwords as purely optional: if a password was missing or blank, the system omitted the `AvailableSettings.JAKARTA_JDBC_PASSWORD` property, silently falling back to trust authentication against PostgreSQL.

While repository ADR [0015-docker-compose-secret-derivation-and-isolation.md](../../../../adr/0015-docker-compose-secret-derivation-and-isolation.md) eliminated hardcoded passwords from Compose and initialization scripts by dynamically deriving secrets, unconfigured environments or misconfigured deployments could still attempt passwordless connections without warning. Connecting to PostgreSQL without a password should never be an implicit default; it must be an intentional, explicit opt-in.

## Goals / Non-Goals

**Goals:**
- Enforce fail-fast validation during configuration ingestion: unconfigured database profiles without passwords must throw descriptive exceptions during boot.
- Require explicit opt-in for passwordless / trust authentication via `trust-auth = true`.
- Support global configuration defaults (`larpconnect.data.database.trust-auth = false`) with profile-level overrides (`larpconnect.data.database.<profile>.trust-auth`).
- Establish unambiguous precedence: explicit non-blank passwords take precedence; `trust-auth = true` enables passwordless connections when passwords are blank or omitted.
- Maintain test suite ergonomics: provide a test classpath configuration overlay (`src/test/resources/application.conf`) in the `:common` **Library module** enabling `trust-auth = true` for tests, avoiding boilerplate overrides or fake credentials in unit tests.
- Support CLI migration workflows with an optional `--trust-auth` command-line flag on `MigrateCommand`.

**Non-Goals:**
- Implementing interactive terminal password prompts during startup.
- Changing production secret retrieval mechanisms or secret vault integrations.
- Modifying Docker Compose secret generation established in ADR 0015.

## Architecture

The following C4 Component diagram illustrates the configuration parsing, fail-fast validation barriers, and connection factory boundaries:

```
+---------------------------------------------------------------------------------------------------+
|                                C4 COMPONENT CONFIGURATION LIFECYCLE                               |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|   +------------------------------------+       +----------------------------------------------+   |
|   | Configuration Sources              |       | CLI Subcommand Execution                     |   |
|   |                                    |       |                                              |   |
|   |  [reference.conf]                  |       |  [MigrateCommand]                            |   |
|   |   - trust-auth = false             |       |   - Optional<String> password                |   |
|   |   - password = ""                  |       |   - Optional<Boolean> trustAuth              |   |
|   |                                    |       |                       |                      |   |
|   |  [application.conf (test overlay)] |       |                       v                      |   |
|   |   - trust-auth = true              |       |  [CliConfigBuilder]                          |   |
|   |                                    |       |   - Layers CLI flags over HOCON              |   |
|   |  [Environment Variables]           |       |   - Sets migration.trust-auth override       |   |
|   |   - LARPCONNECT_DATA_DATABASE_...  |       +-----------------------+----------------------+   |
|   +-----------------+------------------+                               |                          |
|                     |                                                  |                          |
|                     +------------------------+-------------------------+                          |
|                                              |                                                    |
|                                              v                                                    |
|                   +---------------------------------------------------------+                     |
|                   | Typesafe Config Composite Tree                          |                     |
|                   +--------------------------+------------------------------+                     |
|                                              |                                                    |
|                                              v                                                    |
|                   +---------------------------------------------------------+                     |
|                   | Configuration Parsing & Validation Invariant Barrier    |                     |
|                   | (DatabaseConfig.fromConfig / SessionConfig / Migration) |                     |
|                   +--------------------------+------------------------------+                     |
|                                              |                                                    |
|                          +-------------------+-------------------+                                |
|                          |                                       |                                |
|       [Missing Password & trust-auth=false]            [Valid Password OR trust-auth=true]        |
|                          |                                       |                                |
|                          v                                       v                                |
|             (Throws IllegalStateException)             Constructs Immutable Config Records:       |
|             "Password required for profile              - MigrationConfig                         |
|              unless trust-auth is enabled"              - SessionConfig (admin)                   |
|                                                         - SessionConfig (users)                   |
|                                                                  |                                |
|                                                                  v                                |
|                                             +--------------------+--------------------+           |
|                                             | Factory Execution Layer                 |           |
|                                             |                                         |           |
|                                             |  [DefaultDataSourceFactory]             |           |
|                                             |   - Uses config.password() if present   |           |
|                                             |   - Omits password if trustAuth=true    |           |
|                                             |                                         |           |
|                                             |  [DefaultSessionFactoryFactory]         |           |
|                                             |   - Sets JAKARTA_JDBC_PASSWORD if set   |           |
|                                             |   - Omits property if trustAuth=true    |           |
|                                             +--------------------+--------------------+           |
|                                                                  |                                |
|                                                                  v                                |
|                                             +--------------------+--------------------+           |
|                                             | PostgreSQL Database (Data plane)        |           |
|                                             |  - Accepts password or trust auth       |           |
|                                             +-----------------------------------------+           |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

### Architectural Analysis

- **Boundaries**: Clear separation between raw HOCON configuration trees, typed immutable configuration records (`MigrationConfig`, `SessionConfig`, `DatabaseConfig`), and connection factories (`DefaultDataSourceFactory`, `DefaultSessionFactoryFactory`).
- **Responsibilities**: Configuration parsing methods (`fromConfig`) and record constructors validate credential invariants, rejecting invalid states before Guice injection or connection attempts. Connection factories consume validated records and apply JDBC properties.
- **Relationships**: `CliConfigBuilder` injects CLI overrides into the configuration tree before `MigrationConfig.fromConfig` parses them.
- **Assumptions**: Test environments run with classpath resources from `:common`'s `src/test/resources/application.conf`, while production builds package only `reference.conf`.
- **Open Questions**: None; the design aligns with all resolved requirements from the discovery interview.

## Decisions

### Decision 1: Configuration Key Hierarchy (Global with Profile Overrides)

- **Choice**: Introduce `trust-auth` as a boolean setting supporting a global default with profile-level overrides:
  - Global default: `larpconnect.data.database.trust-auth = false` (overridable via `LARPCONNECT_DATA_DATABASE_TRUST_AUTH`)
  - Profile overrides:
    - `larpconnect.data.database.migration.trust-auth`
    - `larpconnect.data.database.admin.trust-auth`
    - `larpconnect.data.database.users.trust-auth`
- **Rationale**: Provides consistent repository-wide security defaults while allowing granular exceptions. For example, a dedicated migration container running on a secure Unix domain socket might use trust authentication while the long-running application session pools require password authentication.
- **Alternatives Considered**:
  - *Global-only toggle*: Rejected because it prevents role-specific security tuning.
  - *Strictly per-profile without global fallback*: Rejected because it forces operators to configure the flag redundantly across three separate blocks.

### Decision 2: Fail-Fast Enforcement Point (Configuration Ingestion Barrier)

- **Choice**: Enforce credential validation inside `SessionConfig.fromConfig`, `MigrationConfig.fromConfig`, and canonical record constructors. If `trustAuth` is `false` and `password` is `null` or blank, throw an `IllegalStateException` identifying the offending profile.
- **Rationale**: Conforms to the project's fail-fast invariant. An invalid deployment is detected immediately at boot before any Guice bindings are provisioned or database network sockets are opened.
- **Alternatives Considered**:
  - *Deferred enforcement in connection factories*: Rejected because it allows bad configuration to construct valid-looking records and survive until the first on-demand connection attempt.

### Decision 3: Precedence Semantics (Password Takes Precedence)

- **Choice**: If a non-blank password is provided, it is used for authentication. The `trust-auth = true` setting is evaluated when the password is blank, null, or omitted to determine whether passwordless connections are permitted.
- **Rationale**: Maximizes operational flexibility, especially in test and staging environments where a global `trust-auth = true` overlay might be present while specific profiles supply real credentials.
- **Alternatives Considered**:
  - *Strict mutual exclusion (fail if both password and trust-auth=true are set)*: Rejected to avoid unnecessary developer friction when combining base test configs with specific credential overrides.

### Decision 4: Test Classpath Configuration Overlay

- **Choice**: Add `src/test/resources/application.conf` in `:common` containing `larpconnect.data.database.trust-auth = true`.
- **Rationale**: In Typesafe Config, `application.conf` automatically overlays `reference.conf` when present on the classpath. Because `src/test/resources` is excluded from production jar packaging, test suites execute with `trust-auth = true` by default, eliminating the need to mock configs or provide dummy secrets in unit tests. Production deployments run without `application.conf`, strictly enforcing fail-fast behavior.
- **Alternatives Considered**:
  - *Explicit `Modules.override()` in every test*: Rejected due to high boilerplate across dozens of existing test classes.
  - *Placeholder passwords in `reference.conf`*: Rejected because it masks missing configuration and causes unhelpful authentication failures against live databases instead of clean configuration validation errors.

### Decision 5: CLI Migration Trigger Support (`--trust-auth`)

- **Choice**: Add an optional `--trust-auth` flag to `MigrateCommand` and `MigrationOptions`, handled by `CliConfigBuilder`.
- **Rationale**: Preserves parity with existing CLI overrides (`--jdbc-url`, `--username`, `--password`). Operators running migrations against local or trust-configured databases can supply `--trust-auth` directly without creating ad-hoc configuration files.

## Risks / Trade-offs

- **[Risk] Existing unit tests loading `ConfigFactory.load()` might fail if test classpath overlay is omitted** -> **Mitigation**: Place `application.conf` in `:common`'s `src/test/resources`, which is transitively available across all dependent modules during Gradle test execution.
- **[Risk] Operator forgets to set database password in production** -> **Mitigation**: The application fails fast with a clear, actionable exception during initialization rather than failing cryptically during subsequent database query execution.
- **[Risk] Confusion between blank password `""` and missing password** -> **Mitigation**: Both blank strings (`"".isBlank()`) and `null` values are treated identically as unauthenticated, requiring `trust-auth = true`.

## Migration Plan

1. Add `src/test/resources/application.conf` to `:common` with `trust-auth = true`.
2. Update `common/src/main/resources/reference.conf` to declare `trust-auth = false` with environment variable substitution.
3. Update `SessionConfig` and `MigrationConfig` records to include `trustAuth` and implement fail-fast validation in constructors and `fromConfig`.
4. Update `DatabaseConfig.fromConfig` to layer global and profile-level `trust-auth` settings.
5. Update `DefaultDataSourceFactory` and `DefaultSessionFactoryFactory` to honor `trustAuth`.
6. Add `--trust-auth` option to `MigrateCommand`, `MigrationOptions`, and `CliConfigBuilder`.
7. Update unit tests in `:common`, `:data`, and `:server`.
8. Verify full build passes: `./gradlew check build`.

## Open Questions

*(None. All decisions resolved via interview and aligned with in-force ADRs).*
