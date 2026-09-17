## Why

Project Njall components currently lack package-level nullness contracts, leaving compiler and static analysis tools (SpotBugs, ErrorProne, Checkstyle) unable to verify parameter and return value invariants across package boundaries. Furthermore, numerous runtime `requireNonNull` checks exist defensively in places where Guice dependency injection guarantees non-null instances, adding clutter and redundant branching.

Establishing strict package-level defaults via JSpecify (`@NullMarked`), explicitly annotating genuine `@Nullable` locations with JSpecify annotations, and removing redundant injection-time null checks will improve static safety, code clarity, and architectural consistency.

## What Changes

- **Add Package-Level Nullness Annotations**: Annotate all 17 non-test, non-integration `package-info.java` files across `:api`, `:common`, `:data`, and `:server` with JSpecify `@NullMarked`.
- **Configure Build & Annotation Dependencies**: Centralize `jspecify` (1.0.1) in `gradle/libs.versions.toml` and configure `compileOnly` dependency on `jspecify` in common build conventions, removing legacy JSR-305 and SpotBugs annotations.
- **Explicit `@Nullable` Annotations**: Annotate parameters and return values that can legitimately be null (such as CLI optional flags and options records, `DefaultServerDAO.findServerEntity`, `CliRunner.execute`, `ServerCommand.call`) with `org.jspecify.annotations.Nullable`.
- **Eliminate Redundant Null Checks in Injected Constructors (Option A)**: Remove unnecessary `requireNonNull` calls from Guice `@Inject` constructors and `@Provides` methods, relying on Guice's non-null injection invariant.
- **Update Unit Tests**: Align unit tests that previously asserted `NullPointerException` on Guice-injected constructors with the updated contracts, ensuring 100% build and coverage verification.

## Capabilities

### New Capabilities
- `nullability-contracts`: Enforces default non-null parameters and return values across non-test packages, requires explicit `@Nullable` annotations for optional values, and eliminates redundant Guice injection null validations.

### Modified Capabilities
<!-- None: No external system behavior or public API contracts are changed. -->

## Impact

- **Build Logic & Dependencies**: Updates `gradle/libs.versions.toml`, `parent/build.gradle.kts`, and `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts`.
- **Java Sources**: Modifies 17 `package-info.java` files, domain/CLI/DAO classes where nullability is applicable, and Guice constructors/modules.
- **Unit Tests**: Modifies unit tests in `:api`, `:common`, and `:server` that tested null parameters for Guice-injected constructors.
- **Verification Gates**: Spotless, Checkstyle, SpotBugs, ErrorProne, and JaCoCo coverage (85% line, 90% branch) must continue to pass cleanly.
