# 0029: Tenanted Locations and Addresses Architecture

## Status

Accepted

## Date

2026-09-29

## Context

Project **Njall** operates on a multi-single-tenant model where gaming organizations (**Studios**) have strict data isolation enforced at the database engine level via PostgreSQL Row-Level Security (RLS) under schema `njall_users`, as established in [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md), and [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md).

As **Njall** expands to support physical venue management, studios need to define physical gathering places (**Locations**) and one or more physical, mailing, or geospatial destinations (**Address**) associated with them. The architecture must preserve Common Table Inheritance (CTI) on `njall_users.entities`, support multiple typed addresses per location, and serialize PostGIS spatial data in standard GeoJSON format without introducing unneeded third-party spatial dependencies.

## Considered Options

- **Option 1: Locations CTI Subtype with Subordinate 1:N Addresses and RFC 7946 GeoJSON Point** (Selected)
  - `njall_users.locations` inherits from `njall_users.entities(tenant_id, id)` via CTI.
  - `njall_users.addresses` is a child table referencing `njall_users.locations(tenant_id, id)` with `ON DELETE CASCADE` and no unique constraint on `location_id`, supporting multiple addresses per location.
  - An `address_type` column discriminates between physical venues, mailing addresses, and PO boxes.
  - `geom geography(Point, 4326)` stores spatial points in PostGIS and serializes as standard GeoJSON Point (`{"type": "Point", "coordinates": [longitude, latitude]}`).
  - Nested REST endpoints: `/api/studios/{studio-id}/v1/locations` and `/api/studios/{studio-id}/v1/locations/{location-id}/addresses`.
- **Option 2: Embedded Single Address Columns in Locations** (Rejected: precludes multiple addresses such as physical venue and separate mailing PO box; tightly couples physical address data to venue entity).
- **Option 3: Untyped Coordinate Array `[lat, lon]`** (Rejected: high ambiguity risk and conflicts with RFC 7946 GeoJSON `[longitude, latitude]` standard).

## Decision

1. **CTI for Locations**: `njall_users.locations` inherits from `njall_users.entities` with composite primary key `(tenant_id, id)`. Soft deletion is governed by `entities.deleted_on`.
2. **Subordinate 1:N Addresses**: `njall_users.addresses` references `njall_users.locations(tenant_id, id) ON DELETE CASCADE`. Addresses are deleted physically upon deletion of the parent location or via direct DELETE operations.
3. **GeoJSON Point Representation**: Geospatial points use PostGIS `geography(Point, 4326)` and are mapped to standard RFC 7946 GeoJSON Point objects with `[longitude, latitude]` coordinate order.
4. **Hierarchical Sub-Resource Routes**: The HTTP surface exposes `/api/studios/{studio-id}/v1/locations[/{id}]` and nested `/api/studios/{studio-id}/v1/locations/{location-id}/addresses[/{id}]` through `LocationsRoute` conforming to AIP-134 for partial updates.
5. **RLS Isolation**: Row-Level Security is enabled on both `locations` and `addresses` tables, enforcing `tenant_id = current_setting('app.tenant_id', true)::uuid`.

## Consequences

- **Positive**: Clean separation of venue metadata and structured postal/geospatial destinations; full support for multiple addresses per venue; native PostGIS spatial querying capability; industry-standard GeoJSON integration.
- **Negative**: Address lookups require joins against parent location and entity tables; GeoJSON coordinate validation must enforce longitude [-180, 180] and latitude [-90, 90] bounds explicitly.
