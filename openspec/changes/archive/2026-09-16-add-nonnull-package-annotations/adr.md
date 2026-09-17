# ADR Review Manifest

- Status: completed
- Review date: 2026-09-16

## Review Summary

ADR review completed for this change.

## In-Force ADRs Reviewed

- `adr/0001-pekko-http-runtime-architecture.md`
- `adr/0002-dropwizard-healthcheck-actor-pattern.md`
- `adr/0003-data-module-and-flyway-migration-architecture.md`
- `adr/0004-picocli-subcommand-architecture.md`
- `adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md`

## New Durable ADRs Created

- `adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md`: Establishes package-level `@NullMarked` defaults across production modules, standardizes on `org.jspecify.annotations.Nullable`, unifies under JSpecify 1.0.1, and eliminates redundant injection-time `requireNonNull` checks.
