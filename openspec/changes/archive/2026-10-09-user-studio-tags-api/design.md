## Context

In **Njall**, **Studios** represent tenant organizational boundaries requiring rich taxonomy tools to organize games, events, and community entities. As established in [0027: Common Table Inheritance and Tenanted Links Architecture](../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md) and [0025: User Space Studios API and Tenant Row-Level Security](../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md), user-plane domain entities inherit from the central polymorphic root `njall_users.entities` and enforce strict PostgreSQL Row-Level Security (RLS) via `app.tenant_id`.

**Hashtags** are first-class, federated metadata entities that extend external and internal **Link** capabilities. Rather than creating disjoint tag registries, hashtags in **Njall** are modeled as a Class Table Inheritance (CTI) subtype extending `njall_users.links`. This design enables seamless federation and allows domain objects to reference hashtags directly as structured links.

## Goals / Non-Goals

**Goals:**
- Provide tenanted REST endpoints under `/api/studios/{studio-id}/v1/tags` for creating, bulk-provisioning (`:batchCreate`), retrieving, listing, patching, and soft-deleting studio hashtags.
- Support dual-identifier lookup on `GET /{tag-id}`, resolving either public UUID or case-insensitive string tag name.
- Preserve original tag casing for display while enforcing case-insensitive uniqueness per studio tenant using a functional PostgreSQL index on `LOWER(tag)`.
- Support idempotent creation: returning existing active or reactivated tags instead of raising conflict errors.
- Adhere to architectural invariants: package size limits (<= 20 types per package under [0032](../../../adr/0032-package-size-limits-and-subpackage-decomposition.md)), single public Guice module per package, immutable domain records ([0022](../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md)), and strict `@NullMarked` conventions ([0006](../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md)).

**Non-Goals:**
- Global cross-studio hashtag aggregation or federated search indexing.
- Free-form substring search or autocomplete queries (reserved for future searchverticles).
- Associating hashtags to actors/events in this phase (the intersect join table `hashtags_entity` is provisioned in the migration schema, but entity association endpoints are deferred).

## C4 Architectural Diagrams

### Container & Component Overview (C4 Level 2 / 3)

```
+─────────────────────────────────────────────────────────────────────────────+
|                               HTTP CLIENT                                   |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │ HTTPS / JSON
                                       ▼
+─────────────────────────────────────────────────────────────────────────────+
|                      PEKKO HTTP ROUTING CONTAINER                           |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                             TagsRoute                                 |  |
|  |  - In-memory StudioLookupCache tenant resolution                      |  |
|  |  - DTO validation (TagValidation: length, unicode, strip '#')         |  |
|  |  - Path segment inspection (UUID vs tag name)                         |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ Ask (TagCommand)                     |
|                                      ▼                                      |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                             TagActor                                  |  |
|  |  - Typed message handler (Create, BatchCreate, Get, List, Patch, Del) |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
+──────────────────────────────────────┼──────────────────────────────────────+
                                       │ Session / DAO Call
                                       ▼
+─────────────────────────────────────────────────────────────────────────────+
|                       HIBERNATE DATA ACCESS CONTAINER                        |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                           DefaultHashtagDAO                           |  |
|  |  - Transaction-scoped: set_config('app.tenant_id', :tenantId, true)   |  |
|  |  - Multi-table CTI atomic persist (entities -> links -> hashtags)     |  |
|  |  - Case-insensitive lookups via LOWER(tag)                            |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
+──────────────────────────────────────┼──────────────────────────────────────+
                                       │ SQL over JDBC (RLS Enforced)
                                       ▼
+─────────────────────────────────────────────────────────────────────────────+
|                           POSTGRESQL DATABASE                               |
|                                                                             |
|  njall_users.entities ◄── njall_users.links ◄── njall_users.hashtags        |
|  (tenant_id, id)          (tenant_id, id)       (tenant_id, id, tag)        |
+─────────────────────────────────────────────────────────────────────────────+
```

### Dynamic Request Flow: Dual-Identifier Retrieval

```
Client               TagsRoute              StudioLookupCache     TagActor            DefaultHashtagDAO       PostgreSQL
  │                      │                          │                │                        │                │
  │── GET /.../tags/{id}─►                          │                │                        │                │
  │                      │── findByIdOrAlias() ────►│                │                        │                │
  │                      │◄─ tenant_id ─────────────│                │                        │                │
  │                      │                                           │                        │                │
  │                      │── is UUID? ──────────────────────────────►│                        │                │
  │                      │   [YES] -> TagCommand.GetById(id)         │                        │                │
  │                      │   [NO]  -> TagCommand.GetByTag(name)      │                        │                │
  │                      │                                           │── findById/ByTag() ───►│                │
  │                      │                                           │                        │── set_config ─►│
  │                      │                                           │                        │── SELECT CTI ─►│
  │                      │                                           │◄─ Hashtag domain ──────│◄─ row data ────│
  │                      │◄─ TagActorResponse.Success(TagResponse) ──│                        │                │
  │◄─ HTTP 200 (JSON) ───│                                           │                        │                │
```

## Decisions

### Decision 1: Two-Tier Class Table Inheritance Chain
- **Choice**: `njall_users.hashtags` references `njall_users.links (tenant_id, id) ON DELETE CASCADE`, which in turn references `njall_users.entities (tenant_id, id) ON DELETE CASCADE`.
- **Rationale**: A hashtag is semantically a link (having a canonical URI, linkType = 'hashtag', mediaType = 'application/json', and audit lifecycle). Extends the existing CTI architecture defined in [0027](../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md).
- **Alternatives Considered**:
  - *Direct Entity Subtype*: Having `hashtags` reference `entities` directly without extending `links`. Rejected because hashtags are fundamentally linkable resources that need to be embedded as links in future federated object representations.

### Decision 2: Case-Insensitive Functional Index with Case-Preserving Storage
- **Choice**: Store user-entered casing in `njall_users.hashtags.tag VARCHAR(32) NOT NULL` and index `(tenant_id DESC, LOWER(tag))` uniquely.
- **Rationale**: Users expect display tags like `"SolarPunk"` or `"NordicLarp"` to retain their aesthetic casing, while identity lookups, URL matching, and duplicate prevention must treat `"solarpunk"` and `"SolarPunk"` as identical.
- **Alternatives Considered**:
  - *Lowercase Normalization*: Storing all tags strictly lowercased. Rejected because it degrades user presentation.
  - *Case-Sensitive Uniqueness*: Allowing both `"larp"` and `"Larp"`. Rejected because it creates community fragmentation and URL confusion.

### Decision 3: Idempotent Creation & Reactivation
- **Choice**: `POST /api/studios/{studio-id}/v1/tags` and `POST .../tags:batchCreate` return `200 OK` (with existing tag) when a matching tag already exists, and clear `deleted_on` if the tag was previously soft-deleted.
- **Rationale**: Clients frequently submit tags when authoring posts or entity profiles without prior existence queries. Idempotent creation prevents race conditions and redundant preflight GET calls.
- **Alternatives Considered**:
  - *Strict 409 Conflict*: Returning 409 when tag exists. Rejected because it imposes client-side retry complexity and chatter.

### Decision 4: Dedicated API Subpackage Decomposition
- **Choice**: Place all hashtag API components in `com.larpconnect.njall.api.studios.tags`, with a dedicated `TagsModule` installed by `StudiosModule`.
- **Rationale**: Conforms strictly to [0032: Package Size Limits and Subpackage Decomposition](../../../adr/0032-package-size-limits-and-subpackage-decomposition.md), keeping class counts well below the 20-type threshold and preventing circular package dependencies.

## Risks / Trade-offs

- **[Multi-Table Insertion Overhead]** -> Writing a hashtag requires inserting into `entities`, `links`, and `hashtags`.
  *Mitigation*: Multi-table inserts occur within a single short-lived JDBC transaction using `@NjallUsers` session pooling, with negligible latency impact for write frequencies.
- **[AIP-233 Batch Transaction Size]** -> Extremely large batch sizes could hold open database connections.
  *Mitigation*: Impose a validation constraint limiting `:batchCreate` requests to a maximum of 50 tags per request.
- **[Unicode Normalization Collisions]** -> Visually identical Unicode characters with different byte encodings could produce divergent tags.
  *Mitigation*: Validate tag strings against web-safe Unicode characters and normalize using standard Java `Normalizer.normalize(tag, Normalizer.Form.NFC)`.

## Migration Plan

1. **Flyway Migration (`V6__hashtags.sql`)**: Apply schema changes creating `njall_users.hashtags` and `njall_users.hashtags_entity`, indexes, RLS policies, and grants.
2. **Persistence Layer**: Implement `HashtagEntity`, `HashtagDAO`, and `DefaultHashtagDAO` in `:data`.
3. **API Layer**: Implement `TagsRoute`, `TagActor`, DTOs, and bindings in `:api`.
4. **Acceptance Testing**: Verify all endpoints using Cucumber integration features in `:integration`.
5. **Rollback**: Schema is backward compatible; rollback drops `hashtags_entity` and `hashtags` tables without affecting existing `links` or `entities` data.

## Open Questions

- None. All requirements, casing conventions, idempotent batch mechanics, and path invariants are resolved.
