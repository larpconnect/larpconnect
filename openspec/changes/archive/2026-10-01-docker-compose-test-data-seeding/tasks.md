## 1. Modular SQL Test Data Kit Creation

- [x] 1.1 Create directory `docker/postgres/seed/` to house the modular test fixture scripts.
- [x] 1.2 Create `docker/postgres/seed/01-default-roles-and-admin-users.sql` populating admin **Roles** (`superuser`, `operator`), test **Admin User** accounts (`admin_alice_example`, `test_fake_admin`), role assignments, and default studio roles with fixed UUIDs and `ON CONFLICT DO NOTHING`.
- [x] 1.3 Create `docker/postgres/seed/02-test-studios.sql` populating test **Studios** ("Valkyrie Example Larp Studio" with **Alias** `valkyrie_example` and "Ironwood Fake Larp Chronicles" with **Alias** `ironwood_fake`) into `njall_users.studios` and `njall_admin.studios_lookup` with fixed deterministic **ID** values and `ON CONFLICT DO NOTHING`.
- [x] 1.4 Create `docker/postgres/seed/03-studio-links.sql` populating CTI root **Entity** entries and tenanted **Link** records (Discord, website, rules documentation) for both test studios with `ON CONFLICT DO NOTHING`.
- [x] 1.5 Create `docker/postgres/seed/04-studio-locations.sql` populating CTI root **Entity** entries, **Locations** ("Camp Example", "Blackthorn Fake Manor Grounds"), and geospatial **Address** records with PostGIS coordinates for both test studios with `ON CONFLICT DO NOTHING`.

## 2. Docker Compose Orchestration Configuration

- [x] 2.1 Add the ephemeral `seed` service to `docker-compose.yml` using `postgis/postgis:18-3.6-alpine`, attached to `internal` network, executing all `/docker-seed/*.sql` scripts as `njall_admin` with derived credentials, depending on `postgres` healthy and `migrate` success, with `restart: "no"`.
- [x] 2.2 Update the `server` service in `docker-compose.yml` to depend on `seed: condition: service_completed_successfully`.

## 3. Verification and Specification Validation

- [x] 3.1 Verify Compose configuration and startup ordering with `./gradlew composeStart` or Docker Compose validation.
- [x] 3.2 Execute `./gradlew check build` to verify all linters (Spotless, Checkstyle, SpotBugs, ErrorProne) and unit/integration tests pass.
- [x] 3.3 Execute `openspec validate docker-compose-test-data-seeding --type change --strict` to verify OpenSpec specification integrity.
