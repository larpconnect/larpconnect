## Why

In **Njall**, studios require the ability to define physical and virtual event venues, camp sites, meeting halls, and other physical places where gatherings occur. Currently, the system supports external references via links, but lacks first-class domain models and API endpoints for managing studio locations and their corresponding physical or mailing addresses. Introducing tenanted **Locations** and nested **Address** sub-resources expands the **User verticle** to support physical venue management while maintaining strict multi-tenant Row-Level Security (RLS) and Common Table Inheritance (CTI) boundaries.

## What Changes

- Introduce a new Flyway migration `V5__locations_and_addresses.sql` in the **Data plane**:
  - `njall_users.locations`: Subtype table inheriting from `njall_users.entities` via CTI with `(tenant_id, id)` primary key and foreign key reference, containing `name VARCHAR(255) NOT NULL`.
  - `njall_users.addresses`: Child table referencing `njall_users.locations(tenant_id, id)` supporting multiple addresses per location (1:N), containing `address_type` (`PHYSICAL`, `MAILING`, `PO_BOX`, `BILLING`, `OTHER`), structured street lines 1-3, locality, administrative area, postal code, country code (ISO-3166-1 alpha-2), and a PostGIS `geom geography(Point, 4326)` geospatial point.
  - Enable PostgreSQL Row-Level Security (RLS) on both `locations` and `addresses` filtering by `app.tenant_id`.
- Implement immutable domain records and DAOs in `:data`:
  - `Location`: Domain record implementing `DatabaseObject` with `id`, `name`, `summary`, and audit timestamps.
  - `Address`: Domain record with `id`, `locationId`, `addressType`, address lines, geographic components, and GeoJSON Point coordinates (`[longitude, latitude]`).
  - `LocationDAO` and `AddressDAO` handling tenanted CRUD operations under active `@NjallUsers` sessions.
- Implement Pekko Typed actors in `:api`:
  - `LocationActor` handling `CreateLocation`, `GetLocation`, `UpdateLocation`, and `DeleteLocation` commands.
  - `AddressActor` handling `CreateAddress`, `GetAddress`, `ListAddresses`, `UpdateAddress`, and `DeleteAddress` commands.
- Expose tenanted HTTP REST endpoints in `:api`:
  - `POST   /api/studios/{studio-id}/v1/locations` (Create Location)
  - `GET    /api/studios/{studio-id}/v1/locations/{location-id}` (Get Location)
  - `PATCH  /api/studios/{studio-id}/v1/locations/{location-id}` (Update Location via AIP-134)
  - `DELETE /api/studios/{studio-id}/v1/locations/{location-id}` (Soft-delete Location via `entities.deleted_on`)
  - `POST   /api/studios/{studio-id}/v1/locations/{location-id}/addresses` (Add Address to Location)
  - `GET    /api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}` (Get Address)
  - `GET    /api/studios/{studio-id}/v1/locations/{location-id}/addresses` (List Addresses for Location)
  - `PATCH  /api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}` (Update Address via AIP-134)
  - `DELETE /api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}` (Hard-delete Address)
- Integrate GeoJSON Point schema format (`{"type": "Point", "coordinates": [<longitude>, <latitude>]}`) for geospatial fields.
- Document endpoints in `openapi.yaml`.

## Capabilities

### New Capabilities

- `user-studio-locations-and-addresses-api`: Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/locations` and nested `/api/studios/{studio-id}/v1/locations/{location-id}/addresses` for creating, retrieving, updating, and deleting studio locations and their associated addresses with Common Table Inheritance (CTI), PostGIS Point geometries (GeoJSON), and PostgreSQL Row-Level Security (RLS) isolation.

### Modified Capabilities

None.

## Impact

- **Database**: Adds `V5__locations_and_addresses.sql` migration creating `njall_users.locations` and `njall_users.addresses` with RLS policies and spatial indexing.
- **Data Module (`:data`)**: Adds `Location` and `Address` domain records, enums, Hibernate entity mappings, and `LocationDAO`/`AddressDAO`.
- **API Module (`:api`)**: Adds `LocationsRoute`, `AddressesRoute`, `LocationActor`, `AddressActor`, request/response DTOs, and updates `openapi.yaml`.
- **Integration Module (`:integration`)**: Adds Cucumber step definitions, feature files, and ArchUnit architecture verifications for the new endpoints and models.
- **External Contracts**: Extends the public OpenAPI 3.1 contract with non-breaking additions under `/api/studios/{studio-id}/v1/locations`.
