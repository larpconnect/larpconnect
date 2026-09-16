# 0005: Sealed DAO and Hibernate Dual Session Architecture

## Status

Accepted

## Date

2026-09-15

## Context

Following the establishment of Flyway database migrations (ADR 0003), Project Njall requires application-layer persistence to query server metadata and support upcoming domain models. Database access is governed by strict PostgreSQL role-based access controls across `njall_admin` (administrative server operations) and `njall_users` (tenanted runtime operations). 

Additionally, Apache Pekko Typed is the reactive runtime engine (ADR 0001), where blocking JDBC calls risk thread starvation if run on the actor or HTTP route dispatchers. A clear architectural pattern is needed for base DAO abstractions, role-specific Hibernate session factory management, and non-blocking asynchronous execution.

## Decision

1. **Sealed Base Persistence Abstraction**: In `:data`, introduce sealed interfaces `DatabaseObject` (requiring `UUID id()`) and `DAO<T extends DatabaseObject>` (requiring `Optional<T> findById(UUID id)` and `ImmutableList<T> list()`). Specific DAOs (e.g. `ServerDAO`) extend `DAO<T>` to maintain compile-time checked type hierarchies.
2. **Dual Hibernate Session Factories**: Configure two isolated Hibernate `SessionFactory` singletons in `:data` bound via custom Guice qualifiers `@NjallAdmin` and `@NjallUsers`. Each session factory connects with its respective database role and search path, enforcing least privilege at the connection level.
3. **Pure Record Domain Models**: Expose immutable domain records (`Server`, `ServerContact`) from DAOs. Internal Hibernate `@Entity` classes remain package-private within `:data` and are marked `@Immutable` for read-only entities, eliminating lazy-initialization and thread-safety hazards.
4. **Pekko Virtual Thread Offloading**: Query execution from Pekko HTTP routes is dispatched to typed Pekko actors (`ServerAdminActor`), which offload blocking DAO queries onto Java 25 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) before replying to the route ask pattern.

## Consequences

- **Positive**: Clean type safety and compile-time exhaustiveness with sealed interfaces; strict role isolation between admin and tenanted queries; zero risk of thread starvation on Pekko dispatchers; domain entities remain completely immutable records.
- **Negative**: Requires maintaining two separate connection pools and SessionFactory lifecycles.
- **Follow-up**: Implement tenanted DAOs using `@NjallUsers` and caffeine-cached tenant ID resolution in subsequent changes.
