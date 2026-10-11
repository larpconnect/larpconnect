# 0037: Tenanted Collections Architecture

## Status

Accepted

## Date

2026-10-11

## Context

In **Njall**, gaming organizations (**Studios**) and users curate and organize groups of entities (such as individuals, events, or media links) into ordered sequences mapping to ActivityPub **OrderedCollection** structures. Following the multi-single-tenant data isolation architecture established in [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md), [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md), and [0036: Tenanted Individuals Architecture](0036-tenanted-individuals-architecture.md), the system requires first-class models and API endpoints for managing tenanted **Collections**.

The architecture must enforce tenant isolation across collection ownership and member references, support arbitrary user-defined item ordering (defaulting to prepending newest first), provide dual representation formats (compressed vs expanded), and expose clean RESTful endpoints adhering to Google AIP standards with minimal responses for item mutations.

## Considered Options

- **Option 1: Collections as CTI Subtype with Ordered Junction Table and Dual Form Retrieval** (Selected)
  - `njall_users.collections` inherits from `njall_users.entities(tenant_id, id)` via CTI.
  - `njall_users.collection_items` stores member references with integer ordinals for deliberate sequencing.
  - Enforce composite foreign keys `(tenant_id, owner_id)` and uniqueness `(tenant_id, owner_id, name)`.
  - Expose `POST /api/studios/{studio-id}/v1/collections` generating server-side UUIDv7 identifiers.
  - Expose `GET /api/studios/{studio-id}/v1/collections/{id}?form={compressed|expanded}` defaulting to compressed ID lists.
  - Expose `PATCH` (AIP-134) and `DELETE` (soft) under `/api/studios/{studio-id}/v1/collections/{id}`.
  - Expose `POST ...:addItem` and `POST ...:removeItem` custom methods (AIP-136) returning HTTP 204 No Content.
  - Omit collection listing (`GET /api/studios/{studio-id}/v1/collections`), returning HTTP 404 Not Found.
- **Option 2: Embedded JSON Array of Items in Collections Table** (Rejected: prevents relational integrity verification, complicates concurrent item mutations, and prevents foreign key cascades).
- **Option 3: Fractional Float Positioning for Items** (Rejected: discrete integer ordinals provide predictable, stable zero-based indexing matching ActivityPub ordered sequences without floating-point precision degradation).

## Decision

1. **CTI for Collections**: `njall_users.collections` inherits from `njall_users.entities` using primary key `(tenant_id, id)`. Deletion is soft-deleted via `entities.deleted_on`.
2. **Owner Scoping and Uniqueness**: `owner_id` references `njall_users.entities(tenant_id, id)` via composite foreign key, with a unique constraint on `(tenant_id, owner_id, name)` ensuring distinct collection names per owner while allowing multiple collections per owner.
3. **Ordered Junction Storage**: `njall_users.collection_items` maintains deliberate item sequences using `ordinal INTEGER NOT NULL` with shift-based insertion (defaulting to prepending at index 0) and support for explicit target placement (`before=<id>` or integer index).
4. **Row-Level Security**: Row-Level Security is enabled on both `collections` and `collection_items` enforcing `tenant_id = current_setting('app.tenant_id', true)::uuid` with bypass for `njall_admin`.
5. **Dual Representation Forms**: `GET .../collections/{id}` supports `?form=compressed` (default, returning item UUIDs) and `?form=expanded` (hydrating full active entity objects). Soft-deleted entities are automatically excluded from items and count.
6. **Custom Mutation Methods**: Item additions and removals use AIP-136 custom methods (`:addItem`, `:removeItem`) returning minimal HTTP 204 No Content responses.
7. **List Endpoint Omission**: Do not provide collection list endpoint on `/api/studios/{studio-id}/v1/collections`.

## Consequences

- **Positive**: Strict tenant isolation enforced at SQL constraint and RLS levels; deliberate item ordering is preserved and queryable; dual forms optimize network transfer while supporting rich client needs; soft deletion preserves audit history.
- **Negative**: Inserting items in large collections requires transactional integer ordinal shifting.
