# 0016: Database Trust Authentication Opt-in and Fail-Fast Enforcement

- Status: accepted, amends ADR-0003, ADR-0005
- Date: 2026-09-23

## Context

ADR 0003 established Flyway schema migrations, and ADR 0005 established dual role-scoped Hibernate session factories for `njall_admin` and `njall_users`. In both components, database passwords were treated as optional fallback values: if a password was missing or blank, the system omitted credential properties from JDBC and Hibernate configurations, silently attempting passwordless trust authentication against PostgreSQL.

While ADR 0015 eliminated hardcoded credentials from container orchestration, silent fallback to trust authentication creates security and operational risks. Deployments with missing or misconfigured credentials do not fail fast; instead, they may connect unexpectedly if PostgreSQL trust authentication is active, or fail cryptically during delayed connection attempts. Connecting without a password must be an explicit, intentional opt-in across the system.

## Decision

1. **Mandatory Passwords by Default**:
   - All database profiles (`migration`, `admin`, `users`) MUST require a non-blank password by default.
   - Connecting without a password when `trust-auth` is disabled MUST fail fast during configuration ingestion and record invariant checks (`SessionConfig`, `MigrationConfig`, `DatabaseConfig`) with an `IllegalStateException`.
2. **Explicit Opt-in via `trust-auth`**:
   - The system MUST introduce an explicit `trust-auth` boolean configuration property.
   - The property MUST support a global default (`larpconnect.data.database.trust-auth = false`, overridable via `${?LARPCONNECT_DATA_DATABASE_TRUST_AUTH}`) with optional profile-level overrides (`larpconnect.data.database.<profile>.trust-auth`).
3. **Precedence Semantics**:
   - If a non-blank password is provided, authentication MUST use the password.
   - The `trust-auth = true` setting ONLY enables passwordless connections when passwords are blank, null, or omitted.
4. **Test Classpath Configuration Overlay**:
   - The `:common` module MUST provide a test classpath configuration (`src/test/resources/application.conf`) setting `larpconnect.data.database.trust-auth = true`.
   - Production distributions MUST NOT package this test configuration, ensuring production runtimes strictly enforce fail-fast credential requirements.
5. **Command-Line Interface Migration Flag**:
   - The `migrate` CLI subcommand MUST expose an optional `--trust-auth` flag in `MigrateCommand` and `MigrationOptions`, mapped via `CliConfigBuilder` to `larpconnect.data.database.migration.trust-auth`.

## Consequences

### Positive
- Enforces secure-by-default database connectivity: missing credentials fail fast at startup with descriptive error messages.
- Eliminates silent degradation to unauthenticated database connections.
- Provides granular security tuning: allows trust authentication for specific roles (e.g. migration) while enforcing passwords on others.
- Preserves test suite ergonomics without boilerplate credential mocking.

### Negative
- Local development without Docker Compose or derived `.env` secrets must explicitly supply credentials or set `trust-auth = true`.
