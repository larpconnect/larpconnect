## Context

With Flyway bootstrap migrations established in `:data`, the application requires a persistent domain querying layer and administrative REST APIs. The system runs on Java 25 LTS with Apache Pekko Typed and Google Guice. Database access must adhere to role-based access control across `njall_admin` (server-level admin) and `njall_users` (tenanted runtime). Furthermore, blocking relational database calls must not starve the Pekko HTTP dispatcher.

## Goals / Non-Goals

**Goals:**
- Provide sealed `DatabaseObject` and `DAO<T extends DatabaseObject>` interfaces in `:data` with `findById(UUID id)` and `list()`.
- Configure dual Hibernate `SessionFactory` instances in `:data` bound via custom Guice qualifiers `@NjallAdmin` and `@NjallUsers` with configurable connection pools.
- Provide an immutable, read-only domain representation of `Server` and `ServerContact` backed by a `ServerDAO` implementation using the `@NjallAdmin` session factory.
- Allow `:api` to depend on `:data`, preserving the DAG `:common` -> `:data` -> `:api` -> `:server`.
- Implement `GET /api/admin/v1/servers` in `:api` returning camelCase JSON, offloading blocking queries to Java 25 virtual threads via a typed `ServerAdminActor`.
- Update `openapi.yaml` and provide dual-layer testing (unit tests and Cucumber integration tests).

**Non-Goals:**
- Modifying the existing V1 Flyway schema (both `njall.servers` and `njall.server_contacts` remain as defined).
- Implementing write, update, or delete operations for servers or contacts.
- Tenanted entity DAOs or row-level security caching (deferred to tenant-specific changes).

## Architectural Diagrams (C4 Level 2 & Level 3)

### C4 Container Diagram
```
+─────────────────────────────────────────────────────────────────────────────+
|                                CLIENT BROWSER / CLI                         |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │ HTTP GET /api/admin/v1/servers
                                       v
+─────────────────────────────────────────────────────────────────────────────+
|                         HTTP SERVER RUNTIME (:server)                       |
|                                                                             |
|   +─────────────────────────────────────────────────────────────────────+   |
|   |                       API COMPONENT (:api)                          |   |
|   |                                                                     |   |
|   |   +──────────────────+               +──────────────────────────+   |   |
|   |   | DefaultAdminRoute| ─(typed ask)─>| ServerAdminActor         |   |   |
|   |   +──────────────────+               | (virtual thread worker)  |   |   |
|   |                                      +─────────────┬────────────+   |   |
|   +────────────────────────────────────────────────────┼────────────────+   |
|                                                        │                    |
|                                                        v                    |
|   +─────────────────────────────────────────────────────────────────────+   |
|   |                   DATA PERSISTENCE COMPONENT (:data)                |   |
|   |                                                                     |   |
|   |   +──────────────────+               +──────────────────────────+   |   |
|   |   | ServerDAO        |<──────────────| DefaultServerDAO         |   |   |
|   |   +──────────────────+               +─────────────┬────────────+   |   |
|   |                                                    │                    |
|   |                                                    v                    |
|   |                                      +──────────────────────────+   |   |
|   |                                      | @NjallAdmin              |   |   |
|   |                                      | SessionFactory           |   |   |
|   |                                      +─────────────┬────────────+   |   |
|   +────────────────────────────────────────────────────┼────────────────+   |
+────────────────────────────────────────────────────────┼────────────────────+
                                                         │ JDBC (njall_admin)
                                                         v
+─────────────────────────────────────────────────────────────────────────────+
|                        POSTGRESQL 18+ DATABASE                              |
|   - njall.servers                                                           |
|   - njall.server_contacts                                                   |
+─────────────────────────────────────────────────────────────────────────────+
```

### C4 Component Sequence Diagram
```
Client             DefaultAdminRoute        ServerAdminActor       DefaultServerDAO       Hibernate
  │                        │                        │                     │                   │
  │──GET /api/admin/v1/───>│                        │                     │                   │
  │   servers              │──GetServers(replyTo)──>│                     │                   │
  │                        │                        │──supplyAsync()─────>│                   │
  │                        │                        │  (Virtual Thread)   │──openSession()───>│
  │                        │                        │                     │──HQL queries─────>│
  │                        │                        │                     │<──entities────────│
  │                        │                        │                     │──closeSession()──>│
  │                        │                        │<──ImmutableList─────│                   │
  │                        │<──ServersListed────────│                     │                   │
  │<──200 OK (JSON)────────│                        │                     │                   │
```

## Decisions

### Decision 1: Sealed DAO and DatabaseObject Abstractions
- **Choice**: Define `DatabaseObject` as a sealed interface with `UUID id()` and `DAO<T extends DatabaseObject>` with `findById(UUID id)` and `list()`.
- **Alternatives Considered**:
  - Unbounded generics (`DAO<T, ID>`): Rejected because all synthetic entity IDs in Njall are UUIDs. Keeping `UUID id()` concrete on `DatabaseObject` simplifies polymorphic handling.
  - Spring Data or generic repository interfaces: Rejected per project invariants against heavy frameworks.
- **Rationale**: Sealed interfaces enforce an exhaustive, compile-time checked type hierarchy.

### Decision 2: Pure Record Domain Models vs Managed Hibernate Entities
- **Choice**: Expose immutable domain records (`Server`, `ServerContact`) from `ServerDAO`. Keep Hibernate entity classes (`ServerEntity`, `ServerContactEntity`) package-private inside `:data`.
- **Alternatives Considered**:
  - Exposing Hibernate `@Entity` classes directly: Rejected; mutable entities violate project immutability rules and can cause lazy-initialization exceptions across asynchronous actor threads.
- **Rationale**: Decouples domain representations from persistence framework lifecycles.

### Decision 3: Dual Session Factories with Custom Qualifiers
- **Choice**: Bind two distinct `SessionFactory` instances using `@NjallAdmin` and `@NjallUsers`.
- **Alternatives Considered**:
  - Single session factory switching connections dynamically: Rejected; role-based schemas and connection privileges are distinct at the role connection level in PostgreSQL.
  - `@Named("admin")`: Rejected per `njall-java` antipattern rules banning `@Named` strings.
- **Rationale**: Custom binding annotations provide type safety, clear traceability, and eliminate string typo hazards.

### Decision 4: Concurrency & Virtual Thread Offloading in Pekko
- **Choice**: `DefaultAdminRoute` communicates with a typed `ServerAdminActor`. The actor executes blocking `ServerDAO` queries using Java 25 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
- **Alternatives Considered**:
  - Running JDBC queries directly on the route dispatcher: Rejected; causes thread starvation in Pekko HTTP.
  - Reactive Hibernate (Hibernate Reactive / Mutiny): Rejected; adds substantial complexity and third-party dependencies when Java 25 virtual threads cleanly solve JDBC blocking without thread pinning.
- **Rationale**: Fits standard Pekko Typed architecture and Leverages Java 25 virtual threads per `njall-java` guidelines.

### Decision 5: JSON Naming Convention
- **Choice**: Lower camelCase for all JSON payload fields (`primaryDomain`, `createdOn`, `roleType`, `contactType`).
- **Rationale**: Adheres to Google AIP guidelines and standard JSON serialization conventions.

## Risks / Trade-offs

- **[Risk] Connection pool exhaustion under heavy admin query load** -> **Mitigation**: Configurable pool sizes in `reference.conf` with sensible defaults (min 2, max 10 for admin; min 5, max 20 for users) and strict 5-second connection acquisition timeouts.
- **[Risk] Unclosed Hibernate sessions** -> **Mitigation**: All session operations in `DefaultServerDAO` use try-with-resources (`try (var session = sessionFactory.openSession())`).
- **[Risk] Virtual thread pinning on synchronized blocks** -> **Mitigation**: Hibernate 7 and modern PostgreSQL JDBC drivers avoid internal monitor locks on socket I/O.

## Migration Plan

No database schema migration is needed. The changes are strictly additive across the Java codebase and configuration:
1. `:data`: Add DAO and Hibernate infrastructure.
2. `:api`: Add route and actor protocols.
3. `:server`: Mount updated Guice modules.
4. `:integration`: Add Cucumber acceptance tests.

## Open Questions

None. All core decisions (casing, base DAO signatures, session qualifiers, module DAG) have been aligned.
