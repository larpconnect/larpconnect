## Why

As the **Njall** codebase has grown, packages such as `com.larpconnect.njall.api.studios` (68 types) and `com.larpconnect.njall.data.dao` (26 types) have accumulated excessive numbers of classes, records, and interfaces, degrading code navigability and violating modularity boundaries. Enforcing a strict package size limit via ArchUnit ensures all packages remain cohesive, focused, and maintainable while preventing future architectural degradation.

## What Changes

- Enforce an ArchUnit package size rule in the `:integration` **library module** restricting any package within `com.larpconnect.njall..` to at most 20 combined records, interfaces, and classes.
- Standardize package counting semantics in ArchUnit:
  - Enums do not count toward the limit.
  - A `Default<InterfaceName>` implementation paired with `<InterfaceName>` in the same package counts as 1.
  - `@NullMarked` `package-info` is mandatory for all production packages and does not count toward the limit.
  - Only public Guice modules count (package-private Guice modules are exempt from the count).
  - Static inner classes count, excluding true `private` inner classes.
- Decompose `com.larpconnect.njall.api.studios` into domain-aligned subpackages:
  - `com.larpconnect.njall.api.studios.common`: Houses shared studio HTTP models (`StudioErrorResponse`).
  - `com.larpconnect.njall.api.studios.links`: Houses link **Actor**, **DTO**, command, response protocols, and routes.
  - `com.larpconnect.njall.api.studios.locations`: Houses location **Actor**, command, response protocols, and routes.
  - `com.larpconnect.njall.api.studios.addresses`: Houses address **Actor**, command, response protocols, and routes.
  - `com.larpconnect.njall.api.studios`: Root package retaining studio core actor protocols, `StudiosRoute` aggregating subroutes, and `StudiosModule`.
- Decompose `com.larpconnect.njall.data.dao` into domain-aligned subpackages:
  - `com.larpconnect.njall.data.dao.common`: Houses base persistence infrastructure (`DAO`, `EntityBaseEntity`, `EntityId`, builders).
  - `com.larpconnect.njall.data.dao.studios`: Houses studio, lookup, location, address, and link **DAO** and JPA entity types.
  - `com.larpconnect.njall.data.dao.admin`: Houses admin user, admin role, and studio role **DAO** and JPA entity types.
  - `com.larpconnect.njall.data.dao.servers`: Houses **Server** and server contact **DAO** and JPA entity types.
  - `com.larpconnect.njall.data.dao`: Root package retaining `DaoModule` composing direct subpackage modules.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `architectural-invariants`: Enforces package size limit of at most 20 types with paired default exclusions, enum exclusions, public Guice module filters, and mandatory `package-info`.
- `user-studios-api`: Decomposes studio user-plane HTTP components into `links`, `locations`, `addresses`, and `common` subpackages while preserving aggregating routing in `StudiosRoute`.
- `data-persistence`: Decomposes DAO and JPA entities into `common`, `studios`, `admin`, and `servers` subpackages while preserving entity registration and DAO singleton bindings.

## Impact

- **Production Code**: Refactors `api/src/main/java/com/larpconnect/njall/api/studios` and `data/src/main/java/com/larpconnect/njall/data/dao` into clean subpackage hierarchies.
- **Unit & Integration Tests**: Relocates existing unit tests in `:api` and `:data` to match target subpackages; adds ArchUnit test in `:integration`.
- **Public API / Contracts**: Zero breaking changes to external HTTP contracts or database schemas. All route paths and JSON contracts remain unchanged.
