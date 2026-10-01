## 1. Checkstyle Parameter Enforcement and AddressEntity Refactoring

- [x] 1.1 Add XPath parameter limit rule (`//(METHOD_DEF | CTOR_DEF)[not(ancestor::RECORD_DEF) and count(PARAMETERS/PARAMETER_DEF) > 8]`) in `config/checkstyle/checkstyle.xml`.
- [x] 1.2 Refactor `AddressEntity` constructor to have at most 8 parameters, separating identity/routing fields from mutable address line fields or providing a package-private builder.
- [x] 1.3 Update `DefaultAddressDAO`, `AddressEntityTest`, `DefaultAddressDAOTest`, and `DefaultAddressDAOBranchTest` call sites to use the refactored `AddressEntity` construction.
- [x] 1.4 Verify `:data` passes `./gradlew :data:checkstyleMain :data:test`.

## 2. DAO Interface and Implementation Naming Alignment

- [x] 2.1 Rename `DefaultStudioRoleDAO` interface to `StudioRoleDAO` in `com.larpconnect.njall.data.dao`.
- [x] 2.2 Rename `DefaultDefaultStudioRoleDAO` implementation class to `DefaultStudioRoleDAO` in `com.larpconnect.njall.data.dao`.
- [x] 2.3 Update `DAO.java` permits clause to reference `StudioRoleDAO`.
- [x] 2.4 Update Guice binding in `DaoModule.java` and rename `DefaultStudioRoleDAOTest` to `StudioRoleDAOTest`.
- [x] 2.5 Update `:api` references in `StudioRoleAdminActor`, `DefaultStudioRoleAdminActorFactory`, `StudioRolesAdminModule`, and their test suites.
- [x] 2.6 Verify `:data` and `:api` pass `./gradlew :data:test :api:test`.

## 3. Query Optimization and Exception Safety

- [x] 3.1 Replace HQL primary key lookup in `DefaultStudioDAO.findById` with `session.find(StudioEntity.class, tenantId)`.
- [x] 3.2 Update `DefaultStudioDAOTest` to verify `session.find` invocation instead of query string creation.
- [x] 3.3 Replace `Double.parseDouble` with Guava `Doubles.tryParse` in `DefaultAddressDAO.parseGeoJson`.
- [x] 3.4 Add unit tests in `DefaultAddressDAOTest` verifying malformed or non-numeric coordinate strings return `Optional.empty()` without throwing `NumberFormatException`.

## 4. Entity Immutability and Query Centralization

- [x] 4.1 Remove setter methods for columns marked `updatable = false` in `EntityBaseEntity`, `AddressEntity`, `LocationEntity`, `LinkEntity`, `StudioLookupEntity`, `StudioEntity`, `AdminUserEntity`, `AdminRoleEntity`, and `DefaultStudioRoleEntity`.
- [x] 4.2 Update any unit tests that previously invoked removed setters to initialize entities via constructors.
- [x] 4.3 Centralize inline query strings into `private static final` constants or `@NamedQuery` / `@NamedNativeQuery` annotations across DAOs and entities.
- [x] 4.4 Verify that `@Immutable` is declared on entities where all fields are `updatable = false` (`ServerEntity`, `ServerContactEntity`).

## 5. Quality Verification and Strict Validation

- [x] 5.1 Run full project verification via `./gradlew check build` to satisfy all Spotless, Checkstyle, SpotBugs, ErrorProne, and Jacoco coverage thresholds.
- [x] 5.2 Run `openspec validate dao-cleanup --type change --strict` to verify change artifact validity.
