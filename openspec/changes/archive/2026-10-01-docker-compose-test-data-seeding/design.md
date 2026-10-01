## Context

The local multi-container development environment in Njall orchestrates PostgreSQL, Flyway schema migrations, the backend HTTP application **Server**, and HAProxy via Docker Compose. While Flyway migrations establish schemas, tables, and constraints up to version V5, the resulting database contains zero tenant data. Developers and testers interacting with the local system must manually execute administrative REST requests to configure test **Studios**, **Admin User** accounts, **Role** assignments, **Link** records, and **Locations**.

To make the system immediately useful upon startup, we introduce an automated, idempotent test data seeding capability into the Docker Compose orchestration layer without modifying production application code or polluting production Flyway migrations.

### C4 Container Diagram

```
+───────────────────────────────────────────────────────────────────────────────────────────────────+
|                                  DOCKER COMPOSE TOPOLOGY                                          |
+───────────────────────────────────────────────────────────────────────────────────────────────────+
                                                                                                    
   [Client / Browser]                                                                               
           │                                                                                        
           ▼ (HTTP :8080 / TLS :8443)                                                               
+───────────────────────+                                                                           
|        haproxy        |  (Ingress reverse proxy, SSL termination, traceparent stripping)           
+──────────┬────────────+                                                                           
           │ (edge network)                                                                         
           ▼                                                                                        
+───────────────────────+                                                                           
|        server         |  (Pekko HTTP REST API runtime :8080)                                      
+──────────▲────────────+                                                                           
           │ (internal network) [starts ONLY after migrate AND seed exit 0]                         
           │                                                                                        
+──────────┴────────────+          +────────────────────────+                                       
|        migrate        |          |          seed          |  (Ephemeral test data kit runner)     
|  (Flyway DDL V1..V5)  | ───────> | (psql as njall_admin)  |  (Mounts docker/postgres/seed/*.sql)  
+──────────▲────────────+          +───────────┬────────────+                                       
           │ (internal network)                │                                                    
           │ [starts when healthy]             │ [executes after migrate exits 0]                   
           │                                   │                                                    
+──────────┴───────────────────────────────────▼────────────+                                       
|                         postgres                          |  (PostGIS 18-3.6-alpine engine)       
|  - 01-init.sh: provisions roles (njall, njall_admin, etc) |  (:5432 internal / :5433 host)        
|  - Schemas: njall, njall_admin, njall_users, njall_system  |                                       
+───────────────────────────────────────────────────────────+                                       
```

## Goals / Non-Goals

**Goals:**
- Provide an automated test data kit in `docker/postgres/seed/` that automatically populates two representative test **Studios** ("Valkyrie Example Larp Studio" and "Ironwood Fake Larp Chronicles") on Compose startup.
- Populate associated test administrative **Roles**, **Admin User** accounts, default studio roles, **Link** entries, physical **Locations**, and geospatial **Address** records.
- Ensure all test data operations are strictly idempotent (`ON CONFLICT DO NOTHING`) so repeated runs against an existing volume do not fail or duplicate data.
- Enforce project naming conventions across all test fixtures: use "Larp" rather than "LARP", and include "example" or "fake" in all human-readable names.
- Keep the test data kit completely decoupled from application Java code in `:server` and `:data`.

**Non-Goals:**
- Modifying production Flyway migration scripts or adding test seed data to `data/src/main/resources/db/migration/`.
- Replacing Testcontainers in `:integration` Cucumber test suites (integration tests continue to manage their own isolated ephemeral database instances).
- Implementing dynamic test data generation or randomized seed values.

## Decisions

### Decision 1: Ephemeral `seed` Service in Docker Compose using Existing PostGIS Image

- **Choice**: Introduce a lightweight `seed` service in `docker-compose.yml` that runs `psql` scripts and exits (`restart: "no"`). It uses `postgis/postgis:18-3.6-alpine` (the exact image already downloaded for the `postgres` service).
- **Alternatives Considered**:
  - *Add a `seed` CLI subcommand in `ServerApp`*: Rejected because test data does not belong in the production binary distribution.
  - *Run seeding inside `/docker-entrypoint-initdb.d/`*: Rejected because entrypoint scripts run before Flyway migrations create the required database tables.
  - *Separate Python/Node script container*: Rejected to avoid introducing unnecessary dependencies and image download overhead.
- **Rationale**: Reusing the database engine image ensures zero extra bandwidth or build steps. The container simply executes `psql` within the Docker `internal` bridge network.

### Decision 2: Modular, Ordered SQL Files in `docker/postgres/seed/`

- **Choice**: Organize seed scripts into ordered `.sql` files:
  - `01-default-roles-and-admin-users.sql`: Global admin users, roles, role assignments, and default studio roles.
  - `02-test-studios.sql`: Core test studios in `njall_admin.studios_lookup` and `njall_users.studios`.
  - `03-studio-links.sql`: CTI root entities and tenanted links.
  - `04-studio-locations.sql`: Tenanted locations and spatial addresses with PostGIS coordinates.
- **Alternatives Considered**:
  - *Single monolithic `seed.sql` script*: Rejected because modular files are easier to maintain, review, and extend as new domain entities are introduced.
- **Rationale**: Future test data can be added simply by dropping `05-*.sql` into the directory.

### Decision 3: Deterministic UUIDs and Idempotency via `ON CONFLICT DO NOTHING`

- **Choice**: Hardcode fixed, deterministic UUIDv7 and UUIDv4 constants for all primary and foreign keys, paired with `ON CONFLICT (...) DO NOTHING` clauses on unique constraints:
  - Valkyrie Example Larp Studio Tenant **ID**: `018d0000-0000-7000-8000-000000000001`
  - Valkyrie Example Larp Studio Public **ID**: `a0000000-0000-4000-8000-000000000001`
  - Ironwood Fake Larp Chronicles Tenant **ID**: `018d0000-0000-7000-8000-000000000002`
  - Ironwood Fake Larp Chronicles Public **ID**: `b0000000-0000-4000-8000-000000000002`
- **Alternatives Considered**:
  - *Generating dynamic UUIDs on insert (`gen_random_uuid()`)*: Rejected because repeated runs on an existing database volume would insert duplicate rows or fail unique constraints.
  - *Truncating tables before insert*: Rejected because restarting containers during development should preserve any state accumulated during testing.
- **Rationale**: Deterministic IDs make writing local API tests predictable and guarantee absolute idempotency across restarts.

### Decision 4: Execution as `njall_admin` Database Role

- **Choice**: The `seed` container authenticates as `njall_admin` with password derived from `NJALL_DB_SECRET` (`njall_admin_${NJALL_DB_SECRET}`).
- **Alternatives Considered**:
  - *Authenticate as `njall` (superuser)*: Rejected because `njall` credentials should remain transient and restricted solely to schema migrations.
  - *Authenticate as `njall_users`*: Rejected because `njall_users` is bound to single-tenant Row-Level Security policies and cannot cross-seed multiple studios or insert into `studios_lookup`.
- **Rationale**: Migration `V2` and `V3` explicitly grant `njall_admin` full permissions (`GRANT ALL`) and Row-Level Security bypass (`FOR ALL TO njall_admin USING (true) WITH CHECK (true)`), making it the ideal role for multi-tenant administrative seeding.

### Decision 5: Test Entity Naming Conventions

- **Choice**: All test fixtures adhere to two explicit conventions:
  - Use "Larp" (capitalized word) rather than "LARP" (all-caps acronym).
  - Explicitly include "example" or "fake" in all entity names, aliases, and sample URLs (e.g. `valkyrie_example`, `ironwood_fake`, "Camp Example", "Blackthorn Fake Manor Grounds", `https://valkyrie-example-larp.fake`).
- **Rationale**: Prevents confusion between testing data and actual production or template data, while maintaining uniform repository conventions.

## Risks / Trade-offs

- **[Risk]** Seed script syntax errors could block the entire Docker Compose startup sequence -> **Mitigation**: The `seed` service runs `psql -v ON_ERROR_STOP=1`, and `server` depends on `condition: service_completed_successfully`. Any syntax error immediately halts startup with clear error logs in `docker compose logs seed`.
- **[Risk]** Schema changes in future Flyway migrations could break existing seed scripts -> **Mitigation**: The seed scripts are part of version control under `docker/postgres/seed/`. Any change adding mandatory columns or constraints will update the corresponding seed SQL file.
- **[Risk]** Password derivation desynchronization -> **Mitigation**: `seed` uses the exact same environment variable derivation pattern (`njall_admin_${NJALL_DB_SECRET}`) established in ADR 0015 and used by `server`.

## Migration Plan

1. Create directory `docker/postgres/seed/` and populate `01` through `04` SQL scripts.
2. Update `docker-compose.yml` to define the `seed` service and adjust `server`'s `depends_on` conditions.
3. Verify startup with `./gradlew composeStart` on both clean volumes (`composeStopClean`) and persisted volumes (`composeStop`).
4. Rollback: Removing the `seed` service definition and restoring previous `depends_on` in `docker-compose.yml` returns Compose to its unseeded state.

## Open Questions

- *None*: In-force ADRs (specifically ADR 0007, ADR 0010, and ADR 0015) remain fully compatible with this design and are extended via a companion ADR documenting the test data kit architecture.
