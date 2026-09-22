# 0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules

## Status

Accepted

## Date

2026-09-22

## Context

Project Njall uses Guice for dependency injection and enforces strict DAG architectural topology across packages and Gradle modules (AGENTS.md). Two structural hygiene issues compromise these architectural invariants:

1. **Public Injected Constructors**: In Guice, constructor injection functions with package-private constructors. Declaring `@Inject` or `@AssistedInject` constructors as `public` leaks implementation details and allows callers outside the package to bypass dependency injection, directly invoking `new` on concrete classes and coupling callers to implementation classes rather than interfaces.
2. **Upward Package Dependencies**: Hierarchical package structures are intended to flow downward (parent packages orchestrating/installing subpackages) or outward (peer packages collaborating sideways across domain boundaries). When subpackages import classes from parent/ancestor packages, it creates inverted package coupling and circular dependency risks. In `:api`, `RouteProvider` was placed in `com.larpconnect.njall.api`, forcing its subpackages `api.http` and `api.admin` to depend upward on the root API package.

Without automated mechanical verification, these patterns are prone to regression across future modules and features.

## Decision

1. **Non-Public Injected Constructors**: Enforce via ArchUnit in `:integration` (`ArchitectureTest`) that all constructors annotated with `@Inject` (`com.google.inject.Inject`, `jakarta.inject.Inject`, `javax.inject.Inject`) or `@AssistedInject` (`com.google.inject.assistedinject.AssistedInject`) across `com.larpconnect.njall..` non-test classes are not declared `public`.
2. **Remediate API Constructors**: Update `PekkoHealthCheck`, `RoleAdminRoute`, `StudioAdminRoute`, and `UserAdminRoute` constructors in `com.larpconnect.njall.api.admin` to package-private visibility.
3. **Downward or Outward Package Dependencies**: Enforce via ArchUnit in `:integration` that for any direct class dependency where both origin and target reside within `com.larpconnect.njall`, the target package must not be an ancestor package of the origin package (`!originPackage.equals(targetPackage) && originPackage.startsWith(targetPackage + ".")` is forbidden).
4. **Relocate `RouteProvider` to `com.larpconnect.njall.api.http`**: Move `RouteProvider` from `com.larpconnect.njall.api` to `com.larpconnect.njall.api.http`, eliminating upward dependencies while allowing `api.admin` to depend outward on sibling package `api.http`.

## Consequences

- **Positive**: Encapsulation is preserved across all Guice-injected components; bypassing dependency injection via `new` outside the package is prevented; package hierarchies remain strictly hierarchical and DAG-compliant; automated test failures prevent regressions codebase-wide.
- **Negative**: Relocating `RouteProvider` requires updating import paths in 4 files across `:api`.
- **Follow-up**: Maintain these ArchUnit rules as new modules and packages are added to Project Njall.
