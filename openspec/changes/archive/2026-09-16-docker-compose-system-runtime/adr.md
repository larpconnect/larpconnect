# ADR Review Manifest

- Status: completed
- Review date: 2026-09-16

## Review Summary

ADR review completed for this change. The adoption of Docker Compose for system runtime orchestration, ephemeral migration tasks, automated PostgreSQL initialization, and host-driven distribution packaging was formally codified in repository-level ADR-0007.

## In-Force ADRs Reviewed

- `adr/0001-pekko-http-runtime-architecture.md`: In force (HTTP server runtime and coordinated shutdown).
- `adr/0002-dropwizard-healthcheck-actor-pattern.md`: In force (Healthcheck actor and `/admin/health` endpoint).
- `adr/0003-data-module-and-flyway-migration-architecture.md`: In force (Flyway migration runner, with Decision 4 superseded by ADR-0004).
- `adr/0004-picocli-subcommand-architecture.md`: In force (Picocli `server` and `migrate` subcommands).
- `adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md`: In force (Dual Hibernate session factories).
- `adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md`: In force (Package-level nullability defaults).

## New Durable ADRs Created

- `adr/0007-docker-compose-and-container-orchestration.md`: Codifies Docker Compose orchestration, PostgreSQL role initialization, ephemeral migration service, host-driven distribution packaging, and Gradle lifecycle tasks.
