## Why

Hibernate's `SessionFactory` is a heavyweight, network-connecting resource that establishes database connection pools during creation. Injecting `SessionFactory` directly ("bare") into service or DAO components forces eager resolution during dependency injection graph instantiation, coupling component instantiation to database availability and preventing lazy initialization. Enforcing provider wrapping via ArchUnit guarantees that session factories are only injected wrapped in a `Provider<SessionFactory>`, deferring resource acquisition until operations actually execute and preventing regressions codebase-wide.

## What Changes

- **Provider-Wrapped SessionFactory Injection**: Update `DefaultServerDAO` in `:data` to inject `@NjallAdmin Provider<SessionFactory>` instead of bare `@NjallAdmin SessionFactory`, resolving sessions on-demand via `sessionFactoryProvider.get().openSession()`.
- **Test Harness Updates**: Update `DefaultServerDAOTest` to supply `() -> sessionFactory` when constructing `DefaultServerDAO`.
- **ArchUnit Rule Enforcement**: Add `testImplementation(libs.archunit.junit5)` to `:integration` and implement `ArchitectureTest` in package `com.larpconnect.njall.integration.arch` enforcing that non-test classes across `com.larpconnect.njall..` do not inject bare `SessionFactory` instances into `@Inject` constructors, methods, fields, or `@Provides` methods.
- **Specification Updates**: Update the `data-persistence` capability specification to require that Hibernate session factory consumers inject `Provider<SessionFactory>`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `data-persistence`: Update session factory consumption requirements so that components requiring a Hibernate session factory must inject it wrapped in a `Provider<SessionFactory>`, enforced structurally by ArchUnit in `:integration`.

## Impact

- **Affected Code**:
  - `data/src/main/java/com/larpconnect/njall/data/dao/DefaultServerDAO.java`
  - `data/src/test/java/com/larpconnect/njall/data/dao/DefaultServerDAOTest.java`
  - `integration/build.gradle.kts`
  - `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java` (new test)
  - `openspec/specs/data-persistence/spec.md`
- **APIs & Binary Compatibility**: Internal implementation detail of `DefaultServerDAO` (package-private). Public interface `ServerDAO` remains untouched.
- **Dependencies**: Leverages existing `archunit-junit5` (1.5.0) version constraint already defined in `parent/build.gradle.kts`.
