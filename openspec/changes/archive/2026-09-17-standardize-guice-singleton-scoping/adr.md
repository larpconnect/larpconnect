# ADR Review Manifest

- Status: completed
- Review date: 2026-09-17

## Review Summary

ADR review completed for this change. The change standardizes dependency injection singleton scoping rules across all Guice modules and prohibits class-level scope annotations on implementation classes.

## In-Force ADRs Reviewed

- `0001-pekko-http-runtime-architecture.md`: Accepted
- `0002-dropwizard-healthcheck-actor-pattern.md`: Accepted
- `0003-data-module-and-flyway-migration-architecture.md`: Accepted
- `0004-picocli-subcommand-architecture.md`: Accepted
- `0005-sealed-dao-and-hibernate-dual-session-architecture.md`: Accepted
- `0006-package-level-nonnull-defaults-and-nullability-conventions.md`: Accepted
- `0007-docker-compose-and-container-orchestration.md`: Accepted

## New Durable ADRs Created

- `adr/0008-module-level-singleton-scoping-convention.md`: Establishes the architectural convention requiring all singleton lifecycles to be configured strictly in Guice modules (`.in(Singleton.class)` or `@Provides @Singleton`) and prohibiting `@Singleton` on class definitions.
