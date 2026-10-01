## Context

In **Njall**, studios need the ability to manage event venues, campgrounds, game sites, and physical gathering spaces. Previously, the system supported external links via Common Table Inheritance (CTI) on `njall_users.entities` (established in ADR-0025 and `user-studio-links-api`), but lacked first-class persistence, actors, and API endpoints for physical **Locations** and their corresponding **Address** destinations.

This design introduces tenanted **Locations** and nested **Address** sub-resources under the **Studio** namespace (`/api/studios/{studio-id}/v1/locations`). Following our architectural invariants, data persistence is partitioned in the **Data plane** under schema `njall_users` with PostgreSQL Row-Level Security (RLS) enforcement, while the **API plane** utilizes in-memory `StudioLookupCache` for zero-DB tenant resolution and Pekko Typed actors for asynchronous command handling.

## Goals / Non-Goals

**Goals:**
- Provide CTI-backed **Locations** entities under `njall_users.locations` inheriting from `njall_users.entities`.
- Provide 1:N **Address** child records under `njall_users.addresses` linked by `(tenant_id, location_id)` with `ON DELETE CASCADE`.
- Support multiple address types (`PHYSICAL`, `MAILING`, `PO_BOX`, `BILLING`, `OTHER`).
- Support optional PostGIS geospatial points (`geography(Point, 4326)`) serialized as standard GeoJSON Point objects (`{"type": "Point", "coordinates": [longitude, latitude]}`).
- Expose tenanted CRUD routes under `/api/studios/{studio-id}/v1/locations` and nested `/api/studios/{studio-id}/v1/locations/{location-id}/addresses`.
- Adhere strictly to Google AIP-134 for partial updates via PATCH and AIP-193 for structured error payloads.

**Non-Goals:**
- Proximity search, bounding-box spatial queries, or distance-based location filtering (deferred to a future spatial search capability).
- Geocoding or address validation integrations (e.g. Google Maps or OpenStreetMap external lookups).
- Global or cross-tenant location directories (all locations remain strictly scoped to their owning studio tenant).

## C4 Component Architecture

```mermaid
flowchart TD
    subgraph ClientPlane["Client Application"]
        Client["HTTP Client / Browser"]
    end

    subgraph ApiContainer["API Plane Container (:api)"]
        Cache["StudioLookupCache (In-Memory)"]
        Route["LocationsRoute (RouteProvider)"]
        LocActor["LocationActor (Pekko Typed)"]
        AddrActor["AddressActor (Pekko Typed)"]
    end

    subgraph DataContainer["Data Plane Container (:data)"]
        LocDAO["LocationDAO / DefaultLocationDAO"]
        AddrDAO["AddressDAO / DefaultAddressDAO"]
        SessionFactory["@NjallUsers SessionFactory"]
    end

    subgraph DB["PostgreSQL 18+ PostGIS Database"]
        EntitiesTbl["njall_users.entities (CTI Root)"]
        LocationsTbl["njall_users.locations (Subtype)"]
        AddressesTbl["njall_users.addresses (Child)"]
    end

    Client -->|HTTP REST| Route
    Route -->|Resolve {studio-id}| Cache
    Route -->|ask Pattern| LocActor
    Route -->|ask Pattern| AddrActor
    LocActor -->|CRUD Commands| LocDAO
    AddrActor -->|CRUD Commands| AddrDAO
    LocDAO -->|RLS Session| SessionFactory
    AddrDAO -->|RLS Session| SessionFactory
    SessionFactory -->|SQL / app.tenant_id| EntitiesTbl
    SessionFactory -->|SQL / app.tenant_id| LocationsTbl
    SessionFactory -->|SQL / app.tenant_id| AddressesTbl
```

## Decisions

### Decision 1: Common Table Inheritance (CTI) for Locations
- **Choice**: Implement `njall_users.locations` as a subtype table referencing `(tenant_id, id)` in `njall_users.entities` with `ON DELETE CASCADE`.
- **Rationale**: Reuses the core entity lifecycle (audit timestamps `created_on`, `updated_on`, soft deletion `deleted_on`, and `summary`), consistent with `njall_users.links` and federated entity architecture.
- **Alternatives Considered**:
  - *Standalone table*: Would duplicate audit fields, soft deletion semantics, and break uniform entity polymorphic queries.

### Decision 2: 1:N Relationship for Addresses Under Locations
- **Choice**: Model `njall_users.addresses` as a child table referencing `njall_users.locations(tenant_id, id)` without a unique constraint on `location_id`.
- **Rationale**: A physical venue often requires distinct physical site addresses, mailing addresses, delivery gates, or PO boxes. An `address_type` column discriminates between these usages.
- **Alternatives Considered**:
  - *1:1 embedded columns in locations*: Fails to support multiple addresses or clean separation of concerns.
  - *CTI entity for addresses*: Unnecessary overhead since addresses are subordinate data attributes of a location rather than top-level federated entities.

### Decision 3: GeoJSON Point Format for Geospatial Data
- **Choice**: Represent PostGIS `geography(Point, 4326)` in external JSON payloads using RFC 7946 GeoJSON Point: `{"type": "Point", "coordinates": [longitude, latitude]}`.
- **Rationale**: Complies with the global geospatial standard and interoperates seamlessly with frontend mapping libraries (Leaflet, Mapbox) and PostGIS `ST_AsGeoJSON` / `ST_GeomFromGeoJSON`.
- **Alternatives Considered**:
  - *Named object `{"latitude": ..., "longitude": ...}`*: Clear, but non-standard for geospatial client libraries.
  - *Untyped coordinate array `[lat, lon]`*: High ambiguity risk regarding coordinate order.

### Decision 4: Nested Sub-resource REST Routing
- **Choice**: Expose addresses as sub-resources of locations:
  - `/api/studios/{studio-id}/v1/locations`
  - `/api/studios/{studio-id}/v1/locations/{location-id}`
  - `/api/studios/{studio-id}/v1/locations/{location-id}/addresses`
  - `/api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}`
- **Rationale**: Enforces hierarchical ownership: an address cannot exist without an active parent location within the studio's tenant.
- **Alternatives Considered**:
  - *Flat `/api/studios/{studio-id}/v1/addresses`*: Detaches the address from its parent venue, complicating authorization and consistency checks.

## Risks / Trade-offs

- **[Risk] PostGIS Extension Availability**: `geography(Point, 4326)` requires PostGIS enabled in the database.
  - *Mitigation*: The project already runs `postgis/postgis:18-3.6-alpine` in Docker Compose and Testcontainers with PostGIS enabled in `01-init.sh`.
- **[Risk] GeoJSON Coordinate Ordering Confusion**: RFC 7946 defines coordinates as `[longitude, latitude]` (X, Y), whereas informal usage often places latitude first.
  - *Mitigation*: Enforce strict server-side boundary validation (index 0 longitude [-180, 180], index 1 latitude [-90, 90]) with explicit error messages and OpenAPI schema documentation.
- **[Risk] Performance on Spatial Queries**: Spatial queries without spatial indexing can degrade database performance.
  - *Mitigation*: Add GiST spatial index `CREATE INDEX idx_addresses_geom ON njall_users.addresses USING GIST (geom);` in addition to the composite B-tree search index.

## Migration Plan

1. **Database Migration**: Create `V5__locations_and_addresses.sql` establishing tables, indexes, and RLS policies.
2. **Data Layer**: Implement domain records, DAOs, and Hibernate session queries in `:data`.
3. **Actor & Route Layer**: Implement Pekko actors, request/response protocols, and route providers in `:api`.
4. **Integration Verification**: Add Cucumber acceptance tests in `:integration` verifying CRUD workflows, RLS isolation, and GeoJSON serialization.

## Open Questions

None. All architectural decisions (CTI inheritance, 1:N cardinality, GeoJSON format, and REST URL structure) have been confirmed.
