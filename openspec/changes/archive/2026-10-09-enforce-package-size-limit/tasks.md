## 1. Studio Subpackage Decomposition in `:api`

- [x] 1.1 Create subpackages `common`, `links`, `locations`, and `addresses` under `api/src/main/java/com/larpconnect/njall/api/studios/` with `@NullMarked` `package-info.java` files.
- [x] 1.2 Move `StudioErrorResponse` to `com.larpconnect.njall.api.studios.common`.
- [x] 1.3 Relocate link components (`LinkActor`, `LinkActorFactory`, `DefaultLinkActorFactory`, `LinkCommand`, `LinkResponse`, `LinkActorResponse`, `LinksRoute`, `LinkValidation`, request DTOs) to `com.larpconnect.njall.api.studios.links`, create `LinksModule`, and colocate corresponding test classes.
- [x] 1.4 Relocate address components (`AddressActor`, `AddressActorFactory`, `DefaultAddressActorFactory`, `AddressCommand`, `AddressResponse`, `AddressActorResponse`, `AddressValidation`, request DTOs) to `com.larpconnect.njall.api.studios.addresses`, create `AddressesModule`, extract `AddressesRoute`, and colocate corresponding test classes.
- [x] 1.5 Relocate location components (`LocationActor`, `LocationActorFactory`, `DefaultLocationActorFactory`, `LocationCommand`, `LocationResponse`, `LocationActorResponse`, `LocationsRoute`, `LocationValidation`, request DTOs) to `com.larpconnect.njall.api.studios.locations`, create `LocationsModule`, and colocate corresponding test classes.
- [x] 1.6 Update `com.larpconnect.njall.api.studios`: configure `StudiosRoute` to aggregate `LinksRoute`, `LocationsRoute`, and `AddressesRoute`; update `StudiosModule` to install subpackage modules.
- [x] 1.7 Run `./gradlew :api:test` to verify all `:api` tests pass before progressing to another **module**.

## 2. DAO Subpackage Decomposition in `:data`

- [x] 2.1 Create subpackages `common`, `studios`, `admin`, and `servers` under `data/src/main/java/com/larpconnect/njall/data/dao/` with `@NullMarked` `package-info.java` files.
- [x] 2.2 Relocate common **DAO** interfaces and base entities (`DAO`, `EntityBaseEntity`, `EntityId`, `DefaultCreateBuilder`, `DefaultPatchBuilder`) to `com.larpconnect.njall.data.dao.common` and create `DaoCommonModule`.
- [x] 2.3 Relocate studio DAOs and entities (`StudioDAO`, `DefaultStudioDAO`, `StudioEntity`, `StudioLookupDAO`, `DefaultStudioLookupDAO`, `StudioLookupEntity`, `LocationDAO`, `DefaultLocationDAO`, `LocationEntity`, `AddressDAO`, `DefaultAddressDAO`, `AddressEntity`, `LinkDAO`, `DefaultLinkDAO`, `LinkEntity`) to `com.larpconnect.njall.data.dao.studios` and create `StudiosDaoModule`.
- [x] 2.4 Relocate admin DAOs and entities (`AdminUserDAO`, `DefaultAdminUserDAO`, `AdminUserEntity`, `AdminRoleDAO`, `DefaultAdminRoleDAO`, `AdminRoleEntity`, `StudioRoleDAO`, `DefaultStudioRoleDAO`, `DefaultStudioRoleEntity`) to `com.larpconnect.njall.data.dao.admin` and create `AdminDaoModule`.
- [x] 2.5 Relocate **Server** DAOs and entities (`ServerDAO`, `DefaultServerDAO`, `ServerEntity`, `ServerContactEntity`) to `com.larpconnect.njall.data.dao.servers` and create `ServersDaoModule`.
- [x] 2.6 Update `DaoModule` in `com.larpconnect.njall.data.dao` to install direct subpackage DAO modules; update any external imports across the codebase; colocate test classes in matching subpackages.
- [x] 2.7 Run `./gradlew :data:test` to verify all `:data` tests pass before progressing to another **module**.

## 3. ArchUnit Package Size Invariant Enforcement in `:integration`

- [x] 3.1 Implement the `packages_must_not_exceed_twenty_types` rule in `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java` enforcing the 6 counting rules and mandatory `package-info`.
- [x] 3.2 Run `./gradlew :integration:test` to verify ArchUnit architecture checks pass without violations.

## 4. Verification and Validation

- [x] 4.1 Run `openspec validate enforce-package-size-limit --type change --strict` to verify OpenSpec schema adherence.
- [x] 4.2 Run `./gradlew check build` to ensure the entire build, tests, Spotless formatting, and lint checks pass cleanly.
