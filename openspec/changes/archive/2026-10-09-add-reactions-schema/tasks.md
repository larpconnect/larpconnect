## 1. Database Migration Authoring

- [x] 1.1 Author Flyway database migration `data/src/main/resources/db/migration/V7__reactions.sql` creating `njall_users.reactions` subordinate table with composite primary key `(tenant_id, id)`, foreign key cascade to `njall_users.entities(tenant_id, id)`, optional foreign key to `njall_users.links(tenant_id, id)` with `ON DELETE SET NULL`, and `created_on` timestamp.
- [x] 1.2 Define write-path covering index `idx_reactions_target_count` on `(tenant_id DESC, target_id DESC) INCLUDE (reaction_type, link_id)` in `V7__reactions.sql`.
- [x] 1.3 Enable PostgreSQL Row-Level Security (RLS) on `njall_users.reactions` with policy `rls_reactions` for **Tenant** isolation under `njall_users` and bypass policy `rls_reactions_admin` for `njall_admin`.
- [x] 1.4 Grant table permissions on `njall_users.reactions` to database roles `njall_users` and `njall_admin`.
- [x] 1.5 Define Materialized View `njall_users.reaction_counts` aggregating active **Entity** **Reaction** counts grouped by tenant, target entity, reaction type, and link metadata.
- [x] 1.6 Create unique index `unq_reaction_counts_bucket` on `(tenant_id, target_id, reaction_type, link_id) NULLS NOT DISTINCT` on `njall_users.reaction_counts` to enable non-blocking `REFRESH MATERIALIZED VIEW CONCURRENTLY`.
- [x] 1.7 Add read-path indexes on `njall_users.reaction_counts` for single-target payload lookups and cross-**Studio** popular queries, and grant view permissions to `njall_users` and `njall_admin`.

## 2. Verification and Quality Gates

- [x] 2.1 Execute migration suite in `:data` and verify that `V7__reactions.sql` applies cleanly against PostgreSQL.
- [x] 2.2 Verify that `REFRESH MATERIALIZED VIEW CONCURRENTLY njall_users.reaction_counts` executes without errors or lock conflicts.
- [x] 2.3 Run `./gradlew check build` to ensure all static analysis, linting, and testing gates pass.
- [x] 2.4 Run `openspec validate add-reactions-schema --type change --strict` to verify OpenSpec schema compliance.
