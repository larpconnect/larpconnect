# 0032: Package Size Limits and Subpackage Decomposition

## Status

Accepted

## Date

2026-10-09

## Context

As the **Njall** codebase expanded, several packages accumulated excessive numbers of classes, records, and interfaces. In particular, `com.larpconnect.njall.api.studios` swelled to 68 types across 73 class files, while `com.larpconnect.njall.data.dao` accumulated 26 types across 40 class files.

This concentration introduces several architectural issues:
1. **Navigability and Cohesion**: Overcrowded packages obscure domain boundaries and mix distinct tenant entities, routes, and persistence concerns.
2. **Structural Topology Invariants**: Under [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md) and [0028: Admin Subpackage Decomposition and Module Hierarchy](0028-admin-subpackage-decomposition-and-module-hierarchy.md), subpackages must not depend upward on ancestor packages, and each package layer must expose exactly one public Guice **module**. Without explicit package size limits, packages naturally grow until they become unmaintainable monoliths.

## Considered Options

- **Option 1: ArchUnit-Enforced Package Size Threshold (<= 20) with Complete Subpackage Decomposition** (Selected)
  - Introduce an automated ArchUnit rule in `:integration` enforcing that any package within `com.larpconnect.njall..` contains at most 20 combined records, interfaces, and classes.
  - Count semantics:
    - Enums do not count.
    - Paired `Default<InterfaceName>` and `<InterfaceName>` in the same package count together as 1.
    - Mandatory `@NullMarked` `package-info` does not count.
    - Only public Guice modules count.
    - Static inner classes count, while private inner classes are excluded.
  - Decompose `com.larpconnect.njall.api.studios` into `common`, `links`, `locations`, `addresses`, and root `studios`.
  - Decompose `com.larpconnect.njall.data.dao` into `common`, `studios`, `admin`, `servers`, and root `dao`.
- **Option 2: Checkstyle or Linter File-Count Rules** (Rejected: cannot evaluate bytecode characteristics such as `Default<Interface>` pairing, public Guice module visibility, or compiled static inner class types).
- **Option 3: Selective Refactoring Without Architectural Test Invariant** (Rejected: without automated CI verification, packages will regress over time).

## Decision

1. **Package Size Invariant**: Enforce in `ArchitectureTest.java` that no package exceeds 20 types under the established counting semantics.
2. **Mandatory Package Info**: Verify every production package declares `@NullMarked` in `package-info.java`.
3. **Studios Subpackage Decomposition**:
   - `com.larpconnect.njall.api.studios.common`: Houses shared models (`StudioErrorResponse`).
   - `com.larpconnect.njall.api.studios.links`: Houses link **Actor**, **DTO**, command, response protocols, and routes.
   - `com.larpconnect.njall.api.studios.locations`: Houses location **Actor**, command, response protocols, and routes.
   - `com.larpconnect.njall.api.studios.addresses`: Houses address **Actor**, command, response protocols, and routes.
   - `com.larpconnect.njall.api.studios`: Root package where `StudiosRoute` aggregates subroutes and `StudiosModule` installs direct submodules.
4. **DAO Subpackage Decomposition**:
   - `com.larpconnect.njall.data.dao.common`: Houses `DAO`, `EntityBaseEntity`, `EntityId`, and builders.
   - `com.larpconnect.njall.data.dao.studios`: Houses studio, lookup, location, address, and link **DAO** and JPA entity types.
   - `com.larpconnect.njall.data.dao.admin`: Houses admin user, admin role, and studio role **DAO** and JPA entity types.
   - `com.larpconnect.njall.data.dao.servers`: Houses **Server** and server contact **DAO** and JPA entity types.
   - `com.larpconnect.njall.data.dao`: Root package where `DaoModule` installs direct submodules.

## Consequences

- **Positive**: Strict, automated architectural gate preventing bloated packages; clean domain isolation; zero upward dependencies; improved test colocation.
- **Negative**: Package reorganization requires file relocation and import updates across `:api`, `:data`, and test suites.
