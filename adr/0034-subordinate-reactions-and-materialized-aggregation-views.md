# 0034: Subordinate Reactions and Materialized Aggregation Views

## Status

Accepted

## Date

2026-10-09

## Context

Project **Njall** manages domain resources under studio **Tenant** boundaries using Common Table Inheritance (CTI) rooted in `njall_users.entities`, as established in [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md).

As interactions grow, users require the ability to attach textless **Reactions** (such as emoji, likes, and custom stickers) to existing entities. Unlike hashtags or locations, reactions are subordinate value records that do not represent first-class federated resources with independent routes or lifecycles. Furthermore, high-frequency entity payload reads require fast, pre-computed reaction aggregations without executing live table scans on every entity fetch, while supporting cross-**Studio** visibility for federation and trending feeds.

## Considered Options

- **Option 1: Subordinate Value Table with Pre-Aggregated Materialized View** (Selected)
  - `njall_users.reactions` acts as a subordinate table referencing `entities(tenant_id, id)` (`target_id`) and optionally `links(tenant_id, id)` (`link_id`).
  - Strict Row-Level Security (RLS) on `reactions` enforces tenant isolation for write operations.
  - Materialized view `njall_users.reaction_counts` pre-aggregates reaction counts grouped by tenant, target entity, reaction type, and optional link media.
  - Unique index with `NULLS NOT DISTINCT` enables non-blocking `REFRESH MATERIALIZED VIEW CONCURRENTLY`.
  - Read indexes optimize single-target payload lookups and cross-studio discovery.
- **Option 2: Class Table Inheritance (CTI) Subtype of Entities** (Rejected: creates unnecessary overhead across `entities` for lightweight reactions that do not require independent summaries, soft deletion lifecycles, or public entity routes).
- **Option 3: Embedded JSONB Reaction Array on Entities** (Rejected: concurrent reaction writes from multiple users create severe row lock contention on the target entity).
- **Option 4: Live View with Security Invoker** (Rejected: incurs aggregation compute overhead on every parent entity read and prevents cross-studio pre-aggregated caching).

## Decision

1. **Subordinate Table Modeling**: Implement `njall_users.reactions` as a subordinate table with composite primary key `(tenant_id, id)`, foreign key cascade referencing `njall_users.entities(tenant_id, id)`, and optional foreign key referencing `njall_users.links(tenant_id, id)`.
2. **Database Engine Tenant Isolation**: Enable PostgreSQL Row-Level Security (RLS) on `njall_users.reactions` with policies isolating tenant access for `njall_users` and permitting administrative access for `njall_admin`.
3. **Pre-Aggregated Materialized View**: Create `njall_users.reaction_counts` materializing active entity reaction counts (`entities.deleted_on IS NULL`) joined with link metadata, trading real-time write latency for O(1) payload reads and enabling cross-studio visibility.
4. **Concurrent Refresh Support**: Define a unique index on `(tenant_id, target_id, reaction_type, link_id) NULLS NOT DISTINCT` to support non-blocking `REFRESH MATERIALIZED VIEW CONCURRENTLY`.
5. **Target and Discovery Indexing**: Index write-path target lookups on `reactions (tenant_id DESC, target_id DESC)` and read-path queries on `reaction_counts` for single-target lookups and popular reaction queries.

## Consequences

- **Positive**: High-performance O(1) reads for target object payloads, non-blocking background refreshes, strong relational integrity to entities and media links, and support for cross-studio activity feeds.
- **Negative**: Aggregated reaction counts in `reaction_counts` lag behind raw writes until the view is refreshed; concurrent refreshes require database maintenance execution.
