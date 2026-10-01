# Proposal: DAO Architecture and Quality Hardening

## Why

Over time, several inconsistencies and edge-case vulnerabilities have accumulated in the persistence layer:
1. An interface was named `DefaultStudioRoleDAO`, creating an inverted naming pattern where its concrete implementation had to be named `DefaultDefaultStudioRoleDAO`.
2. Simple primary key lookups frequently execute HQL queries (`session.createQuery`) rather than direct identifier lookups (`session.find`), bypassing first-level cache lookups and identifier caching.
3. GeoJSON coordinate extraction relies on unchecked `Double.parseDouble` calls without handling `NumberFormatException`.
4. SQL and HQL queries are duplicated as inline string literals inside DAO methods rather than leveraging static constants or compile/startup-validated `@NamedQuery` definitions.
5. Entity classes define setters on columns marked `updatable = false`, violating domain encapsulation and misleading developers.
6. Non-record constructors have grown up to 11 parameters without static analysis enforcement.

Standardizing these patterns now ensures the persistence layer remains robust, clean, and strictly conforms to repository design invariants.

## What Changes

- **DAO Interface and Implementation Realignment**:
  - Rename interface `DefaultStudioRoleDAO` to `StudioRoleDAO`.
  - Rename concrete implementation `DefaultDefaultStudioRoleDAO` to `DefaultStudioRoleDAO`.
  - Update `DAO` sealed interface hierarchy to permit `StudioRoleDAO`.
  - Update Guice **Module** bindings and API actor dependencies in `:api`.
- **Direct Entity Identifier Lookups**:
  - Replace HQL primary key queries in `DefaultStudioDAO` and related DAOs with `session.find`.
- **Safe Coordinate Parsing**:
  - Replace unchecked `Double.parseDouble` in `DefaultAddressDAO` with exception-safe parsing via `Doubles.tryParse`.
- **Query Organization**:
  - Extract inline query strings into `private static final` constants or `@NamedQuery` / `@NamedNativeQuery` definitions on entity classes.
- **Enforce Entity Immutability of Key and Audit Fields**:
  - Remove setters for all entity columns marked with `@Column(..., updatable = false)` across `EntityBaseEntity`, `AddressEntity`, `LocationEntity`, `LinkEntity`, `StudioLookupEntity`, `StudioEntity`, `AdminUserEntity`, `AdminRoleEntity`, and `DefaultStudioRoleEntity`.
- **Checkstyle Parameter Count Limit**:
  - Add a Checkstyle rule enforcing a maximum of 8 parameters for constructors and methods in non-record classes (`//(METHOD_DEF | CTOR_DEF)[not(ancestor::RECORD_DEF) and count(PARAMETERS/PARAMETER_DEF) > 8]`).
  - Refactor `AddressEntity` constructor to conform to the 8-parameter limit.
- **Codify `@Immutable` Invariant**:
  - Document and verify the requirement that any entity where all persistent columns are `updatable = false` must be annotated with Hibernate `@Immutable`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `data-persistence`: Replaces `DefaultStudioRoleDAO` specification with `StudioRoleDAO`, mandates `session.find` for primary key lookups, specifies safe coordinate parsing, and establishes strict immutability rules for `updatable = false` columns.

## Impact

- **Affected Code**:
  - `:data`: `com.larpconnect.njall.data.dao.*` (interfaces, implementations, entities, unit tests).
  - `:api`: `com.larpconnect.njall.api.admin.studioroles.*` (actor, actor factory, unit tests).
  - `config/checkstyle/checkstyle.xml`: New XPath check for parameter count.
- **Dependencies**: No external library additions; uses standard Java 25, Hibernate 7, Guice, and Guava libraries already present in the workspace.
- **APIs**: No external HTTP REST API contract changes; all changes are internal to the persistence and actor layers.
