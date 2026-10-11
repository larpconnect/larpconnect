# 0036: Tenanted Individuals Architecture

## Status

Accepted

## Date

2026-10-10

## Context

In **Njall**, gaming organizations (**Studios**) manage individuals, participants, performers, and non-player characters who may interact with the studio without possessing a user account. Following the multi-single-tenant data isolation architecture established in [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md), and [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md), the system requires first-class models and API endpoints for managing tenanted **Individuals**.

The architecture must guarantee tenant isolation across entity relationships, maintain audit history through soft deletion, and expose clean RESTful endpoints adhering to Google AIP standards while explicitly omitting collection listing endpoints.

## Considered Options

- **Option 1: Individuals as CTI Subtype with Standard Collection POST and Direct Lookups** (Selected)
  - `njall_users.individuals` inherits from `njall_users.entities(tenant_id, id)` via CTI.
  - Expose `POST /api/studios/{studio-id}/v1/individuals` generating server-side UUIDv7 identifiers.
  - Expose `GET`, `PATCH` (AIP-134), and `DELETE` (soft) under `/api/studios/{studio-id}/v1/individuals/{individual-id}`.
  - Omit collection listing (`GET /api/studios/{studio-id}/v1/individuals`), returning HTTP 404 Not Found.
- **Option 2: Parameterized POST at Member URI (`POST .../individuals/{id}`)** (Rejected: violates system-wide invariant where the server assigns time-ordered UUIDv7 identifiers).
- **Option 3: Standalone Individuals Table without CTI** (Rejected: duplicates entity audit fields `created_on`, `updated_on`, `deleted_on`, and prevents polymorphic reaction and hashtag associations).

## Decision

1. **CTI for Individuals**: `njall_users.individuals` inherits from `njall_users.entities` using primary key `(tenant_id, id)`. Deletion is soft-deleted via `entities.deleted_on`.
2. **Row-Level Security**: Row-Level Security is enabled on `njall_users.individuals` enforcing `tenant_id = current_setting('app.tenant_id', true)::uuid` with bypass for `njall_admin`.
3. **REST Route Pattern**: Expose `/api/studios/{studio-id}/v1/individuals` (POST) and `/api/studios/{studio-id}/v1/individuals/{id}` (GET, PATCH, DELETE) via `IndividualsRoute` and Pekko Typed `IndividualActor`.
4. **List Endpoint Omission**: Do not provide collection list endpoint on `/api/studios/{studio-id}/v1/individuals`.

## Consequences

- **Positive**: Strict tenant isolation enforced at SQL constraint and RLS levels; individuals inherit CTI polymorphic capabilities; soft deletion preserves audit history.
- **Negative**: Client applications cannot query or list all individuals of a studio without specific individual IDs.
