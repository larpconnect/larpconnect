## Why

When spinning up the local development and testing environment via Docker Compose, the database is initialized with empty schemas and zero tenant data, forcing developers and testers to manually craft API payloads to create studios, administrative users, and domain entities before testing endpoints. Introducing an automated, idempotent test data seeding step within Docker Compose immediately provisions a standardized testing kit containing representative test studios, users, links, and locations without polluting application source code or production migration scripts.

## What Changes

- Add an ephemeral `seed` service to `docker-compose.yml` positioned between `migrate` and `server`, executing idempotent SQL scripts as the `njall_admin` role.
- Update `docker-compose.yml` service dependencies so that `server` launches only after both `migrate` and `seed` complete successfully.
- Establish a modular SQL testing kit directory at `docker/postgres/seed/` containing ordered, idempotent scripts (`01-default-roles-and-admin-users.sql`, `02-test-studios.sql`, `03-studio-links.sql`, `04-studio-locations.sql`).
- Populate two core testing **Studios** ("Valkyrie Example Larp Studio" with alias `valkyrie_example` and "Ironwood Fake Larp Chronicles" with alias `ironwood_fake`) using fixed, deterministic **ID** values, explicit Row-Level Security bypass via `njall_admin`, and `ON CONFLICT DO NOTHING` idempotency.
- Populate associated testing **Admin User** accounts, **Role** assignments, default studio roles, **Link** records, **Locations** ("Camp Example", "Blackthorn Fake Manor Grounds"), and geospatial **Address** entries.
- Maintain convention rules requiring "Larp" (capitalized word rather than acronym) and explicit inclusion of "example" or "fake" across all test entities.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `system-runtime-orchestration`: Extends Docker Compose orchestration to execute an ephemeral `seed` container after schema migrations complete and before the HTTP **Server** starts, populating an idempotent test data kit.

## Impact

- **Docker Compose**: `docker-compose.yml` introduces the `seed` service reusing the `postgis/postgis:18-3.6-alpine` image and updates the `server` service `depends_on` conditions.
- **Repository Structure**: Adds `docker/postgres/seed/` directory with version-controlled test fixture SQL files.
- **Database State**: Local environments created via `./gradlew composeStart` immediately contain two initialized test studios and accompanying data.
- **Production Isolation**: Zero modifications to `:server` or `:data` runtime modules, ensuring test fixtures are strictly confined to local development orchestration.
