# 0033: Federated Hashtags via Two-Tier Common Table Inheritance and Case-Insensitive Functional Indexing

## Status

Accepted

## Date

2026-10-09

## Context

Project **Njall** uses Class Table Inheritance (CTI) rooted in `njall_users.entities` to manage polymorphic domain objects and PostgreSQL Row-Level Security (RLS) under studio **Tenant** boundaries, as established in [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md).

As **Studios** expand, organizing games, events, and community profiles requires a structured taxonomy system. While simple freeform text tags could be stored in unindexed arrays or disjoint tables, **Hashtags** in **Njall** serve as first-class, linkable resources intended for future federation across the system. Furthermore, user tagging demands case-preserving presentation (e.g. `"SolarPunk"`) while guaranteeing case-insensitive identity lookups, URL routing, and duplicate prevention.

## Considered Options

- **Option 1: Two-Tier CTI Extending `links` with Case-Insensitive Functional Indexing** (Selected)
  - `njall_users.hashtags` extends `njall_users.links (tenant_id, id) ON DELETE CASCADE`, which extends `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
  - Stored tag preserves original casing (`VARCHAR(32)`), with a unique index on `(tenant_id DESC, LOWER(tag))`.
  - Canonical URL defaults to `/api/studios/{studio-id}/v1/tags/{tag-id}`.
  - Creation operations are idempotent, resolving or reactivating existing tags without failing on conflict.
- **Option 2: Direct Subtype of `entities` without Extending `links`** (Rejected: fails to inherit `url`, `link_type`, and `media_type` properties required when tags are federated and serialized as link arrays on domain objects).
- **Option 3: Strict Lowercase Storage** (Rejected: discards author-chosen display capitalization such as PascalCase or camelCase).
- **Option 4: Case-Sensitive Uniqueness** (Rejected: allows confusing collisions like `"larp"` and `"Larp"` within the same studio).

## Decision

1. **Two-Tier CTI Extension**: Subtype `njall_users.hashtags` directly from `njall_users.links`, inheriting all link properties and establishing a composite primary key `(tenant_id, id)`.
2. **Case-Insensitive Functional Uniqueness**: Maintain original casing in `tag VARCHAR(32) NOT NULL` and enforce case-insensitivity using a PostgreSQL functional index on `(tenant_id DESC, LOWER(tag))`.
3. **Dual-Identifier Lookup**: Expose tenanted routes under `/api/studios/{studio-id}/v1/tags/{tag-id}` where `{tag-id}` accepts either UUID or case-insensitive string tag name.
4. **Idempotent Provisioning**: Single creation (`POST`) and bulk creation (`POST :batchCreate` via Google AIP-233) operate idempotently: existing active tags are returned, and soft-deleted tags are reactivated.
5. **Entity-Hashtag Intersect Mapping**: Introduce `njall_users.hashtags_entity` join table mapping `(tenant_id, entity_id)` to `(tenant_id, hashtag_id)` with RLS policies in preparation for entity tagging.

## Consequences

- **Positive**: Consistent entity audit lifecycle, seamless link serialization for federation, preserved display styling with collision-free search, and atomic multi-table integrity.
- **Negative**: Multi-table inserts span three tables (`entities`, `links`, `hashtags`), requiring transactional coordination in `HashtagDAO`.
