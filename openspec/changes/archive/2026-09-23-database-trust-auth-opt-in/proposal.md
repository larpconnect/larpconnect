## Why

In the current **Njall** persistence architecture, database configuration profiles (`migration`, `admin`, and `users`) permit blank or omitted passwords without explicit verification. If an environment or deployment configuration lacks credentials, the system silently omits password properties and falls back to unauthenticated or trust-authenticated connections. This silent fallback violates secure-by-default and fail-fast invariants, risking unintended connection establishment or masked configuration drift. Trust-authenticated connections to the PostgreSQL **Data plane** must require an explicit, intentional opt-in, failing fast during configuration ingestion when credentials are missing and trust authentication is not enabled.

## What Changes

- **Fail-Fast Credential Invariant**: Mandate non-blank database passwords by default across all database configuration profiles (`migration`, `admin`, `users`). If a profile lacks a non-blank password and `trust-auth` is not explicitly enabled, configuration ingestion (`SessionConfig`, `MigrationConfig`, and `DatabaseConfig`) MUST fail fast immediately with an `IllegalStateException`.
- **Explicit Trust-Auth Configuration Hierarchy**: Introduce an explicit `trust-auth` boolean configuration key. Support a global default (`larpconnect.data.database.trust-auth = false`, overridable via `${?LARPCONNECT_DATA_DATABASE_TRUST_AUTH}`) alongside profile-level overrides (`larpconnect.data.database.migration.trust-auth`, `larpconnect.data.database.admin.trust-auth`, `larpconnect.data.database.users.trust-auth`).
- **Precedence Semantics**: When a non-blank password is provided, authentication uses the password. An explicit `trust-auth = true` setting enables passwordless connections when passwords are blank, null, or omitted.
- **Secure Reference Configuration Baseline**: Update `reference.conf` in the `:common` **Library module** so database credentials default to empty strings with `trust-auth = false`, guaranteeing that unconfigured runtime deployments fail fast.
- **Test Classpath Configuration Overlay**: Provide a test classpath configuration (`src/test/resources/application.conf`) in `:common` setting `larpconnect.data.database.trust-auth = true` so unit tests verifying module wiring, dependency injection, and actor graphs can instantiate components without requiring real secrets or live databases.
- **CLI Migration Flag**: Add an optional `--trust-auth` flag to the `migrate` CLI subcommand (`MigrateCommand` and `MigrationOptions`) in the `:server` **Application module**, layered via `CliConfigBuilder` into `larpconnect.data.database.migration.trust-auth`.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `data-persistence`: Update dual role-scoped session factory requirements to mandate passwords by default and require explicit `trust-auth = true` for passwordless connections.
- `database-migration`: Update administrative database connection lifecycle and CLI execution requirements to mandate passwords by default and support explicit `--trust-auth` overrides.
- `command-line-interface`: Update `migrate` subcommand requirements to support the `--trust-auth` option.

## Impact

- **Affected Modules**:
  - `:common`: [`reference.conf`](../../../../common/src/main/resources/reference.conf) baseline defaults, new `src/test/resources/application.conf` test overlay.
  - `:data`: [`SessionConfig.java`](../../../../data/src/main/java/com/larpconnect/njall/data/config/SessionConfig.java), [`MigrationConfig.java`](../../../../data/src/main/java/com/larpconnect/njall/data/config/MigrationConfig.java), [`DatabaseConfig.java`](../../../../data/src/main/java/com/larpconnect/njall/data/config/DatabaseConfig.java), and data module test suites.
  - `:server`: [`MigrateCommand.java`](../../../../server/src/main/java/com/larpconnect/njall/server/cli/MigrateCommand.java), [`MigrationOptions.java`](../../../../server/src/main/java/com/larpconnect/njall/server/cli/MigrationOptions.java), [`CliConfigBuilder.java`](../../../../server/src/main/java/com/larpconnect/njall/server/cli/CliConfigBuilder.java), and CLI test suites.
- **Runtime & Deployment**: Unconfigured environments running without database passwords will now fail fast during boot rather than attempting passwordless connections, unless `LARPCONNECT_DATA_DATABASE_TRUST_AUTH=true` is set.
- **Dependencies**: No external library additions. Leverages standard Typesafe Config and Picocli features already in use.
