## Context

The **Njall** codebase adheres to strict modular and architectural quality gates enforced by ArchUnit, Spotless, Checkstyle, and JaCoCo. However, as business features for the multi-tenant studio vertical expanded, `com.larpconnect.njall.api.studios` swelled to 68 types across 73 class files, while `com.larpconnect.njall.data.dao` accumulated 26 types across 40 class files.

This design introduces an automated ArchUnit constraint restricting package size to at most 20 combined records, interfaces, and classes, while decomposing `api.studios` and `data.dao` into cohesive subpackages without violating DAG layering invariants.

## Goals / Non-Goals

**Goals:**
- Implement an automated ArchUnit rule in `:integration` verifying that zero production packages exceed 20 combined types.
- Define unambiguous counting rules:
  - Exclude enums and `@NullMarked` `package-info`.
  - Pair `Default<InterfaceName>` and `<InterfaceName>` in the same package as 1 type.
  - Count only public Guice modules.
  - Count static inner classes while excluding private inner classes.
  - Require presence of `@NullMarked` `package-info` in all packages.
- Decompose `com.larpconnect.njall.api.studios` into `common`, `links`, `locations`, `addresses`, and root `studios`.
- Decompose `com.larpconnect.njall.data.dao` into `common`, `studios`, `admin`, `servers`, and root `dao`.
- Maintain single public Guice module exposition per package layer and zero upward dependencies.

**Non-Goals:**
- Altering external HTTP endpoints, path parameters, query parameters, or AIP-193 error response structures.
- Changing database table schemas, CTI hierarchies, or Hibernate mapping definitions.
- Restructuring administrative API packages under `com.larpconnect.njall.api.admin` (which already comply with the 20-type limit).

## Decisions

### Decision 1: ArchUnit Custom Rule Formulation

We implement `packages_must_not_exceed_twenty_types` in `ArchitectureTest.java` as an `ArchRule`.

*Rationale:* Rather than composing restrictive fluent predicates that struggle with cross-class grouping and pairing heuristics, a dedicated `ArchRule` inspects all classes in `com.larpconnect.njall..`, groups them by package, evaluates the 6 counting rules, and records descriptive condition events on violations.

```
+─────────────────────────────────────────────────────────────────────────────+
|                     ARCHUNIT EVALUATION ALGORITHM                           |
+─────────────────────────────────────────────────────────────────────────────+
| For each package P in com.larpconnect.njall:                                |
|   1. Assert P contains package-info.class (violation if missing).           |
|   2. Filter candidate classes:                                              |
|      - Skip package-info                                                    |
|      - Skip isEnum()                                                        |
|      - Skip private static inner classes (isNestedClass() && isPrivate())   |
|      - Skip non-public Guice modules (isGuiceModule() && !isPublic())       |
|   3. Identify Interface types: { I_1, I_2, ... }                            |
|   4. Deduplicate Default<I> implementations matching known interfaces in P. |
|   5. Compute Effective Count = Remaining Candidates                         |
|   6. If Effective Count > 20 -> Fail with violated ConditionEvent.          |
+─────────────────────────────────────────────────────────────────────────────+
```

*Alternatives Considered:*
- Enforcing checkstyle file count per directory: Rejected because Checkstyle cannot inspect compiled inner class types, understand `Default<Interface>` pairing semantics, or inspect Guice module visibility.

### Decision 2: Studio Subpackage Topology and Route Aggregation (C4 Component Diagram)

```
+─────────────────────────────────────────────────────────────────────────────+
|                    STUDIOS COMPONENT ARCHITECTURE                           |
+─────────────────────────────────────────────────────────────────────────────+
|                                                                             |
|                      +───────────────────────────────+                      |
|                      |         StudiosModule         |                      |
|                      +───────────────────────────────+                      |
|                        /             |             \                        |
|            (installs) /   (installs) |  (installs)  \                       |
|                      v               v               v                      |
|             +-------------+  +---------------+  +----------------+          |
|             | LinksModule |  |LocationsModule|  |AddressesModule |          |
|             +-------------+  +---------------+  +----------------+          |
|                    |                 |                   |                  |
|                    v                 v                   v                  |
|             +-------------+  +---------------+  +----------------+          |
|             | LinksRoute  |  |LocationsRoute |  | AddressesRoute |          |
|             +-------------+  +---------------+  +----------------+          |
|                    \                 |                  /                   |
|                     \                |                 /                    |
|                      v               v                v                     |
|                      +───────────────────────────────+                      |
|                      |         StudiosRoute          | (Aggregator)         |
|                      +───────────────────────────────+                      |
|                                                                             |
|                      Shared HTTP Models:                                    |
|                      +───────────────────────────────+                      |
|                      |  studios.common               |                      |
|                      |  - StudioErrorResponse        |                      |
|                      +───────────────────────────────+                      |
+─────────────────────────────────────────────────────────────────────────────+
```

*Rationale:*
- Splitting `LocationsRoute` and `AddressesRoute` ensures neither package exceeds the 20-type threshold (Locations = 19 types, Addresses = 19 types).
- Placing `StudioErrorResponse` in `com.larpconnect.njall.api.studios.common` prevents subpackages from depending upward on `com.larpconnect.njall.api.studios`.
- `StudiosRoute` in `com.larpconnect.njall.api.studios` acts as an aggregating route, binding into the `RouteProvider` multibinder and concatenating the subroutes.

### Decision 3: DAO Subpackage Topology and Entity Isolation

```
+─────────────────────────────────────────────────────────────────────────────+
|                      DAO COMPONENT ARCHITECTURE                             |
+─────────────────────────────────────────────────────────────────────────────+
|                                                                             |
|                       +──────────────────────────────+                      |
|                       |          DaoModule           |                      |
|                       +──────────────────────────────+                      |
|                        /          /      \         \                        |
|            (installs) / (installs/ (installs\ (installs\                    |
|                      v          v          v            v                   |
|                 +---------+ +---------+ +---------+ +----------+            |
|                 | Common  | | Studios | |  Admin  | | Servers  |            |
|                 |  Dao    | |   Dao   | |   Dao   | |   Dao    |            |
|                 | Module  | | Module  | | Module  | |  Module  |            |
|                 +---------+ +---------+ +---------+ +----------+            |
|                      ^          ^          ^            ^                   |
|                      |          |          |            |                   |
|                      | (extends)|          |            |                   |
|                      +----------+          |            |                   |
|                     EntityBaseEntity       |            |                   |
|                                                                             |
+─────────────────────────────────────────────────────────────────────────────+
```

*Rationale:*
- Moving `EntityBaseEntity`, `EntityId`, and `DAO` to `com.larpconnect.njall.data.dao.common` allows domain DAO subpackages (`studios`, `admin`, `servers`) to reference shared persistence infrastructure as sibling packages without ancestor violations.
- `DaoModule` at the root of `com.larpconnect.njall.data.dao` remains the sole public module installing submodules one level down.

## Risks / Trade-offs

- **[Risk] Test relocation churn**: Moving 13+ test classes in `:api` and `:data` could disrupt git history or package-private test fixture access.
  - *Mitigation*: Ensure moved classes have appropriate public/package-private visibility; run `./gradlew test` immediately upon relocation.
- **[Risk] Upward dependency inadvertence**: Accidentally referencing parent classes from subpackages violates `package_dependencies_must_not_go_up`.
  - *Mitigation*: Common models are isolated in `.common` subpackages; ArchUnit test suite validates the absence of ancestor dependencies.

## Migration Plan

1. Author new ArchUnit rule in `:integration`.
2. Restructure `api.studios` into `common`, `links`, `locations`, `addresses`, and root `studios`.
3. Restructure `data.dao` into `common`, `studios`, `admin`, `servers`, and root `dao`.
4. Run `./gradlew check build` to verify compilation, test passes, Spotless formatting, and ArchUnit compliance.

## Open Questions

None. All constraints and boundaries were verified and agreed upon during exploration.
