# 0028: Admin Subpackage Decomposition and Module Hierarchy

## Status

Accepted

## Date

2026-09-29

## Context

The **API plane** for administrative management in **Njall** was initially grouped in a single flat package, `com.larpconnect.njall.api.admin`. As features expanded, this package accumulated 38 classes spanning distinct domains: health probes, server management, studio administration, studio role management, user administration, and role administration.

Co-locating all these concerns in one flat package creates several architectural drawbacks:
1. **Coupling and Monolithic Module Configuration**: `AdminModule` was directly configuring all sub-actors, actor factories, request marshallers, and routing paths, violating the Single Responsibility Principle and the modular package standard established in `AGENTS.md`.
2. **ArchUnit Invariant Compliance**: Under [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md), package dependencies must strictly flow down or out, never up. As soon as domain logic is partitioned into subpackages, any shared models or validation located in the parent package would cause downward classes to depend upward on their ancestor package, triggering ArchUnit failures.
3. **Route Aggregation without Mixed Responsibilities**: In `DefaultAdminRoute`, health and server listing HTTP handlers were implemented inline, while studio, user, and role routes were injected as delegate route classes, creating an inconsistent mixture of internal handling logic and subroute delegation contrary to IOSP-Lite.

## Considered Options

- **Option 1: Domain-Aligned Subpackages with Isolated Common Package and Dedicated Route Classes** (Selected)
  - Split `com.larpconnect.njall.api.admin` into `health`, `servers`, `studios`, `studioroles`, `users`, `roles`, and `common`.
  - Isolate shared error responses (`AdminErrorResponse`) and validators (`AdminValidation`) in `common`, enabling peer-level lateral dependencies without ancestor violations.
  - Extract `HealthAdminRoute` and `ServersAdminRoute` into `health` and `servers`, transforming `DefaultAdminRoute` into a pure composite route aggregator.
  - Expose a dedicated Guice **module** in each subpackage (`HealthAdminModule`, `ServersAdminModule`, `StudiosAdminModule`, `StudioRolesAdminModule`, `UsersAdminModule`, `RolesAdminModule`, `AdminCommonModule`), installed exclusively by `AdminModule`.
- **Option 2: Partial Decomposition Keeping Shared Code in Parent Package** (Rejected: violates ArchUnit no-ancestor-dependencies rule; subpackages cannot import classes from `com.larpconnect.njall.api.admin`).
- **Option 3: Retain Flat Package Structure** (Rejected: high cognitive load, difficult navigability, and violates package-to-module composition principles).

## Decision

1. **Subpackage Hierarchy**: Decompose `com.larpconnect.njall.api.admin` into subpackages: `health`, `servers`, `studios`, `studioroles`, `users`, `roles`, and `common`.
2. **NullMarked Package Declarations**: Every subpackage containing public types must include a `package-info.java` annotated with `@org.jspecify.annotations.NullMarked`.
3. **Shared Utilities in admin.common**: Relocate `AdminErrorResponse`, `AdminValidation`, and the admin `ObjectMapper` provider into `com.larpconnect.njall.api.admin.common`.
4. **Dedicated Route Implementations**: Extract `HealthAdminRoute` and `ServersAdminRoute`. `DefaultAdminRoute` in `com.larpconnect.njall.api.admin` acts purely as a top-level composite aggregator composing subroutes via `concat(...)`.
5. **Hierarchical Guice Modules**: Each subpackage declares a public Guice **module** (`HealthAdminModule`, `ServersAdminModule`, `StudiosAdminModule`, `StudioRolesAdminModule`, `UsersAdminModule`, `RolesAdminModule`, `AdminCommonModule`). `AdminModule` in `com.larpconnect.njall.api.admin` installs each subpackage **module** exactly one layer down and binds `AdminRoute` to `DefaultAdminRoute`.

## Consequences

- **Positive**: Strict adherence to the DAG package topology and ArchUnit invariants; clean separation of administrative domains; uniform structure across all subpackages; test isolation improved by colocating tests in matching subpackage directories.
- **Negative**: Class references across 38 files require package relocation and import updates across `:api` and test classes.
