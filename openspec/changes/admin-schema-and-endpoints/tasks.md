## 1. Database Migration

- [ ] 1.1 Create Flyway migration `data/src/main/resources/db/migration/V2__admin_schema.sql` defining `njall_admin.tstatus`, `admin_users`, `admin_roles`, `admin_role_assignments`, and `studios_lookup` tables with UUIDv7 primary keys and CHECK constraints on `alias` and `role_name` (`^[a-z][a-z0-9_]*$`).
- [ ] 1.2 Implement the `njall_admin.sync_admin_timestamp()` trigger function and attach `BEFORE UPDATE` triggers to `admin_users` and `studios_lookup`.
- [ ] 1.3 Add Row Level Security policy on `njall_admin.studios_lookup` for `njall_users` using `current_setting('app.tenant_id', true)::uuid`.
- [ ] 1.4 Add least-privilege grants allowing `njall_users` `USAGE` on schema `njall_admin` and `SELECT` on table `studios_lookup`.
- [ ] 1.5 Verify migration scripts compile and pass spotless in the `:data` module.

## 2. Data Persistence Layer (:data)

- [ ] 2.1 Implement `AdminUserStatus` enum and update `DatabaseObject` sealed hierarchy to permit `AdminUser`, `AdminRole`, and `StudioLookup`.
- [ ] 2.2 Implement immutable domain records `AdminUser`, `AdminRole`, and `StudioLookup` in `com.larpconnect.njall.data.domain`.
- [ ] 2.3 Implement JPA entities `AdminUserEntity`, `AdminRoleEntity`, and `StudioLookupEntity` mapped to `njall_admin` tables.
- [ ] 2.4 Register the new entity classes in `DaoModule` under the `@NjallAdmin Set<Class<?>>` multibinder.
- [ ] 2.5 Define DAO interfaces `AdminUserDAO`, `AdminRoleDAO`, and `StudioDAO` extending `DAO<T>` with query and mutation methods.
- [ ] 2.6 Implement `DefaultAdminUserDAO`, `DefaultAdminRoleDAO`, and `DefaultStudioDAO` injecting `@NjallAdmin Provider<SessionFactory>`.
- [ ] 2.7 Bind DAO interfaces to their default implementations in `DaoModule`.
- [ ] 2.8 Implement unit tests in `:data` for all domain records, entities, and DAOs, ensuring line and branch coverage gates pass.

## 3. Administrative Actor & API Route Layer (:api)

- [ ] 3.1 Define sealed message protocols and response ADTs: `StudioAdminCommand`, `UserAdminCommand`, `RoleAdminCommand`, and corresponding responses.
- [ ] 3.2 Implement typed Pekko actors `StudioAdminActor`, `UserAdminActor`, and `RoleAdminActor` operating on `larpconnect.blocking-dispatcher`.
- [ ] 3.3 Implement actor factory interfaces and Guice-managed factory implementations for each admin actor.
- [ ] 3.4 Implement request/response Jackson records and AIP-193 error response records, including alias and roleName format validation (`^[a-z][a-z0-9_]*$`).
- [ ] 3.5 Implement modular route classes `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute` with input validation.
- [ ] 3.6 Compose the modular sub-routes into `DefaultAdminRoute`.
- [ ] 3.7 Bind actors, factories, and sub-routes in `AdminModule`.
- [ ] 3.8 Update `api/src/main/resources/openapi.yaml` with schema definitions and endpoints for studios, users, roles, and custom methods (`:addRole`, `:removeRole`).
- [ ] 3.9 Write unit tests in `:api` using `BehaviorTestKit` for actors and `RouteTestKit` for routes, ensuring coverage gates pass.

## 4. Integration Testing & Full System Verification (:integration)

- [ ] 4.1 Add Cucumber scenarios in `:integration` verifying `V2__admin_schema.sql` table existence, triggers, RLS isolation, and permission restrictions.
- [ ] 4.2 Add Cucumber scenarios in `:integration` for end-to-end HTTP flows:
  - Studio creation, listing, soft-delete filtering, and alias/UUID retrieval.
  - User creation, listing, and username/UUID retrieval.
  - Custom methods `:addRole` and `:removeRole` with idempotent semantics and invalid role rejection.
  - Role creation, listing, and roleName/UUID retrieval.
  - AIP-193 structured error responses for 400, 404, and 409 status codes.
- [ ] 4.3 Implement step definitions in `:integration` supporting the new Cucumber scenarios.
- [ ] 4.4 Run ArchUnit architecture tests to verify zero bare `SessionFactory` injections exist across all newly created classes.
- [ ] 4.5 Execute `./gradlew check build` across all modules to verify spotless formatting, checkstyle, spotbugs, and test coverage gates pass.
- [ ] 4.6 Execute `openspec validate admin-schema-and-endpoints --type change --strict` to verify OpenSpec artifact integrity.
