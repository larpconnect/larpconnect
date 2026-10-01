## 1. Database Migration & Schema

- [x] 1.1 Create Flyway migration `V5__locations_and_addresses.sql` in `data/src/main/resources/db/migration/` defining `njall_users.locations` (CTI subtype referencing `entities(tenant_id, id) ON DELETE CASCADE`) and `njall_users.addresses` (subordinate child table referencing `locations(tenant_id, id) ON DELETE CASCADE`).
- [x] 1.2 Configure indexes (`idx_addresses_search` composite B-tree index, `idx_addresses_geom` GiST index), enable Row-Level Security (RLS) on both tables with `current_setting('app.tenant_id', true)::uuid` policies, and grant table permissions to `njall_users` and `njall_admin`.
- [x] 1.3 Verify database migration execution against PostGIS test container in `:data`.

## 2. Data Plane Domain Models & DAOs

- [x] 2.1 Create `AddressType` enum in `com.larpconnect.njall.data.domain` (`PHYSICAL`, `MAILING`, `PO_BOX`, `BILLING`, `OTHER`).
- [x] 2.2 Create `GeoJsonPoint` immutable record in `com.larpconnect.njall.data.domain` implementing RFC 7946 GeoJSON Point structure (`type = "Point"`, `coordinates = [longitude, latitude]`) with geographic boundary validation.
- [x] 2.3 Create `Location` immutable domain record implementing `DatabaseObject` in `com.larpconnect.njall.data.domain`.
- [x] 2.4 Create `Address` immutable domain record implementing `DatabaseObject` in `com.larpconnect.njall.data.domain`.
- [x] 2.5 Implement `LocationDAO` interface and `DefaultLocationDAO` in `com.larpconnect.njall.data.dao` with transaction-scoped `app.tenant_id` RLS configuration and soft deletion filtering.
- [x] 2.6 Implement `AddressDAO` interface and `DefaultAddressDAO` in `com.larpconnect.njall.data.dao` with transaction-scoped RLS and PostGIS spatial point reading/writing (`ST_AsGeoJSON`, `ST_SetSRID(ST_GeomFromGeoJSON(?), 4326)`).
- [x] 2.7 Bind `LocationDAO` and `AddressDAO` in `DataModule`.
- [x] 2.8 Author unit tests in `data/src/test/` for `LocationDAO`, `AddressDAO`, and domain records, verifying `./gradlew :data:test` passes.

## 3. API Plane DTOs, Actors & Routes

- [x] 3.1 Create request and response DTO records (`CreateLocationRequest`, `UpdateLocationRequest`, `CreateAddressRequest`, `UpdateAddressRequest`, `LocationResponse`, `AddressResponse`) adhering to `@Immutable`, non-null components, and AIP-134 field masks.
- [x] 3.2 Implement `LocationActor` with Pekko Typed behavior, message protocol, and actor factory in `com.larpconnect.njall.api.studios`.
- [x] 3.3 Implement `AddressActor` with Pekko Typed behavior, message protocol, and actor factory in `com.larpconnect.njall.api.studios`.
- [x] 3.4 Implement `LocationsRoute` in `com.larpconnect.njall.api.studios` handling `/api/studios/{studio-id}/v1/locations[/{id}]` and nested `/api/studios/{studio-id}/v1/locations/{id}/addresses[/{id}]`.
- [x] 3.5 Configure Guice bindings in `StudiosModule` for actors, actor factories, and routes.
- [x] 3.6 Update `openapi.yaml` documenting all location and address endpoints, schemas, parameters, and error responses.
- [x] 3.7 Author unit tests in `api/src/test/` for DTOs, actors, and routes, verifying `./gradlew :api:test` passes.

## 4. Acceptance Testing & ArchUnit Verification

- [x] 4.1 Create Cucumber acceptance feature `studio_locations_and_addresses.feature` in `integration/src/test/resources/features/` covering location and address CRUD, AIP-134 updates, cross-tenant RLS isolation, and GeoJSON serialization.
- [x] 4.2 Implement step definitions in `integration/src/test/java/com/larpconnect/njall/integration/` exercising endpoints against the HTTP server runtime.
- [x] 4.3 Verify ArchUnit rules in `:integration` for record immutability, non-public `@Inject` constructors, and package dependency DAG invariants.
- [x] 4.4 Execute `./gradlew :integration:test` and verify all acceptance scenarios pass.

## 5. Architectural Quality Gate & Build Verification

- [x] 5.1 Execute `./gradlew check build` to verify Spotless, Checkstyle, SpotBugs, ErrorProne, and JaCoCo coverage gates across all modules.
- [x] 5.2 Execute `openspec validate add-studio-locations-and-addresses-api --type change --strict` to verify OpenSpec coherence.
