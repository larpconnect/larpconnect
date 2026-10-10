## Why

**Studios** and players in **Njall** require mechanisms to express textless feedback—such as likes, emoji reactions, and custom stickers—on domain **Entity** objects. Rather than modeling reactions as heavyweight first-class entities with independent API routes, reactions exist as lightweight subordinate attachments associated with target entities. Introducing the reactions schema now establishes the persistent storage layer, PostgreSQL Row-Level Security (**Tenant** boundary), and pre-aggregated **Reaction** summary views with cross-**Studio** visibility ahead of future payload integrations.

## What Changes

- Add Flyway database migration `V7__reactions.sql` creating the `njall_users.reactions` subordinate table referencing `njall_users.entities(tenant_id, id)` and optionally `njall_users.links(tenant_id, id)`.
- Enforce PostgreSQL Row-Level Security (RLS) on `njall_users.reactions` with policies for `njall_users` (tenanted isolation) and `njall_admin` (bypass).
- Grant table permissions on `njall_users.reactions` to database roles `njall_users` and `njall_admin`.
- Define the covering index `idx_reactions_target_count` on `(tenant_id DESC, target_id DESC) INCLUDE (reaction_type, link_id)`.
- Define the `njall_users.reaction_counts` Materialized View pre-aggregating active entity reaction counts, grouped by tenant, target entity, reaction type, and optional link media.
- Create a `UNIQUE` index with `NULLS NOT DISTINCT` on `njall_users.reaction_counts` to support non-blocking `REFRESH MATERIALIZED VIEW CONCURRENTLY`.
- Create lookup and discovery indices on `njall_users.reaction_counts` for single-target payload resolution and cross-**Studio** popularity queries.
- Add migration and schema verification tests in `:data` validating that `V7__reactions.sql` applies cleanly and concurrent view refresh operates without error.

## Capabilities

### New Capabilities
- `reactions-schema`: Flyway database migration `V7__reactions.sql` provisioning the `njall_users.reactions` subordinate table, Row-Level Security policies, indexes, and the `njall_users.reaction_counts` materialized view supporting concurrent refreshes and cross-**Studio** aggregation.

### Modified Capabilities
<!-- None -->

## Impact

- **Database**: Adds `njall_users.reactions` table, `njall_users.reaction_counts` materialized view, associated indexes, RLS policies, and role grants via `V7__reactions.sql`.
- **Data Module (`:data`)**: Migration integration tests verify that Flyway executes cleanly against PostgreSQL and that concurrent materialized view refresh succeeds.
- **APIs**: No changes to existing entity endpoints or payloads in this phase.
