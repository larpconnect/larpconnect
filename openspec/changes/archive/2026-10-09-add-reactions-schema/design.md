## Context

In **Njall**, domain objects extending Common Table Inheritance (CTI) via `njall_users.entities` (such as studios, locations, and links) serve as the foundation for multi-tenant data storage under PostgreSQL Row-Level Security (RLS). While **Entity** instances represent discrete resources, users require the ability to attach textless responses (**Reactions** such as emojis, likes, and custom stickers) to these entities.

Unlike hashtags or locations, reactions are subordinate value records rather than first-class federated resources. They attach directly to target entities and will eventually be embedded within the reported payloads of parent objects. To ensure low read latency when assembling parent object payloads while facilitating cross-**Studio** activity feeds, aggregated reaction counts are maintained in a PostgreSQL Materialized View.

## Goals / Non-Goals

**Goals:**
- Provide persistent, tenanted storage for entity reactions via `njall_users.reactions` with composite primary key `(tenant_id, id)` and foreign key integrity to `entities` and `links`.
- Guarantee **Tenant** isolation at the database engine level using PostgreSQL Row-Level Security (RLS) for role `njall_users` while granting full bypass access to `njall_admin`.
- Pre-aggregate reaction counts in `njall_users.reaction_counts` to optimize entity payload assembly and provide cross-**Studio** visibility.
- Support non-blocking background refreshes using `REFRESH MATERIALIZED VIEW CONCURRENTLY` via a unique index with `NULLS NOT DISTINCT`.
- Index write and read paths for fast single-target lookups and popular reaction queries.

**Non-Goals:**
- Exposing REST API endpoints for reactions under `/api/studios/...` in this phase.
- Integrating reactions into the serialization payloads of existing domain objects in this phase.
- Defining per-user reaction limits or actor uniqueness constraints ahead of dedicated user account modeling.

## Architectural Boundaries (C4 Container & Component View)

```
+─────────────────────────────────────────────────────────────────────────────+
|                               NJALL PLATFORM                                |
|                                                                             |
|  +─────────────────────────+          +──────────────────────────────────+  |
|  |     Migration CLI       |          |        Pekko HTTP Server         |  |
|  |     (:server / :data)   |          |        (:server / :api)          |  |
|  +────────────┬────────────+          +─────────────────┬────────────────+  |
|               │ (Flyway V7)                             │                   |
|               v                                         │                   |
+───────────────┼─────────────────────────────────────────┼───────────────────+
                │                                         │
                v                                         v
+─────────────────────────────────────────────────────────────────────────────+
|                            POSTGRESQL DATABASE                              |
|                                                                             |
|  Schema: njall_users                                                        |
|                                                                             |
|  +─────────────────────────+             +───────────────────────────────+  |
|  |  njall_users.entities   |<────────────|     njall_users.reactions     |  |
|  |  (CTI Root Table)       | (target_id) |  (Subordinate Table + RLS)    |  |
|  +─────────────────────────+             +───────────────┬───────────────+  |
|               ^                                          │                  |
|               │                                          │ (link_id)        |
|  +────────────┴────────────+                             v                  |
|  |    njall_users.links    |<────────────────────────────+                  |
|  |    (CTI Media Subtype)  |                                                |
|  +─────────────────────────+                                                |
|               ▲                                          ▲                  |
|               │                                          │                  |
|               +───────────────────┐                      │                  |
|                                   │                      │                  |
|  +────────────────────────────────┴──────────────────────┴───────────────+  |
|  |                     njall_users.reaction_counts                       |  |
|  |                 (Materialized View Pre-Aggregation)                   |  |
|  |                                                                       |  |
|  |  - Indexed by: (tenant_id, target_id)                                 |  |
|  |  - Non-blocking: REFRESH MATERIALIZED VIEW CONCURRENTLY               |  |
|  |  - Cross-Studio trending & instant payload resolution                 |  |
|  +───────────────────────────────────────────────────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────+
```

### Diagram Analysis
- **Boundaries**: All reaction storage and materialized views live in the `njall_users` PostgreSQL schema. Write operations enforce tenant boundaries via RLS.
- **Data Flow**: Reaction rows write to `njall_users.reactions`. The materialized view `njall_users.reaction_counts` aggregates active reaction counts across entities, decoupling read latency from write throughput.
- **Visibility**: Reaction counts are readable across studios, providing aggregated activity statistics without real-time table scan overhead.

## Decisions

### Decision 1: Subordinate Value Table vs. Class Table Inheritance (CTI)
- **Choice**: Model `njall_users.reactions` as a subordinate child table referencing `njall_users.entities(tenant_id, id)` rather than inheriting from `entities`.
- **Rationale**: Reactions are not first-class federated resources with independent lifecycles; they are metadata values attached to existing entities. Subordinate table design eliminates the overhead of multi-table inserts across `entities` and keeps reaction rows compact.
- **Alternatives Considered**:
  - *CTI Subtype of entities*: Rejected because reactions do not require standalone summaries, independent entity routes, or polymorphism.
  - *JSONB array embedded in entity row*: Rejected because concurrent reactions from multiple users would cause write conflicts and row lock contention on the parent entity.

### Decision 2: Pre-Aggregated Materialized View for Read Optimization
- **Choice**: Materialize aggregated reaction counts in `njall_users.reaction_counts` grouped by tenant, target entity, reaction type, and optional link media.
- **Rationale**: High-traffic entities accumulate numerous reactions. Materializing counts allows O(1) index-backed reads during parent object payload serialization, trading real-time write consistency for sub-millisecond read latency. Furthermore, the materialized view allows cross-**Studio** discovery and trending aggregations.
- **Alternatives Considered**:
  - *Live query / security-invoker view*: Trades write simplicity for compute overhead during every parent entity fetch; does not support pre-aggregated cross-studio caching.
  - *In-memory application cache*: Rejected because Redis/Memcached cache invalidation across distributed nodes introduces cache-sync drift.

### Decision 3: Non-Blocking Concurrent Refresh Indexing
- **Choice**: Create a unique index on `(tenant_id, target_id, reaction_type, link_id) NULLS NOT DISTINCT` on `njall_users.reaction_counts`.
- **Rationale**: Standard `REFRESH MATERIALIZED VIEW` commands take an exclusive table lock, blocking concurrent reads. PostgreSQL requires an unconditional unique index to permit `REFRESH MATERIALIZED VIEW CONCURRENTLY`. Using `NULLS NOT DISTINCT` handles reactions without custom media links (`link_id IS NULL`).

## Risks / Trade-offs

- **[Eventual Consistency on Reaction Counts]** -> Reaction counts in `reaction_counts` will lag slightly behind raw writes until the view is refreshed. *Mitigation*: Acceptable for textless reactions; background workers or scheduled actors can refresh the view periodically or on batch thresholds.
- **[Cross-Studio Aggregation Visibility]** -> Because PostgreSQL does not support RLS on materialized views, counts are visible across tenants with `SELECT` permissions. *Mitigation*: Aligns with platform federation and cross-studio discovery goals; individual reaction rows in `njall_users.reactions` remain strictly isolated under RLS.
- **[Foreign Key Cascade vs. Soft Deletion]** -> Hard deleting a target entity cascades to delete reactions, but soft deletion updates `deleted_on`. *Mitigation*: The materialized view query explicitly filters `WHERE E.deleted_on IS NULL`, ensuring soft-deleted entities are immediately excluded upon view refresh.

## Migration Plan

- **Step 1**: Add Flyway migration `data/src/main/resources/db/migration/V7__reactions.sql`.
- **Step 2**: Apply migration using `./gradlew migrate` or `./gradlew test` in `:data`.
- **Rollback**: Flyway migrations are forward-only; rollback requires applying a compensating migration or restoring a pre-migration database snapshot.

## Open Questions

- None. Refresh cadence mechanisms (e.g., Pekko scheduled task) will be implemented when API reaction ingestion workers are added.
