# Technical Design: DAO Architecture and Quality Hardening

## Context

The persistence layer in `:data` manages database interactions via Hibernate 7 and PostgreSQL 18. DAOs inherit from a sealed `DAO<T extends DatabaseObject>` interface and communicate with callers exclusively through immutable domain records. As the persistence layer has expanded across admin and tenanted domains, several quality inconsistencies and anti-patterns have emerged:
- Inverted DAO interface naming: `DefaultStudioRoleDAO` (interface) implemented by `DefaultDefaultStudioRoleDAO` (class).
- Inefficient single-entity primary key querying via HQL string compilation rather than `session.find`.
- Unhandled `NumberFormatException` risks in GeoJSON coordinate parsing.
- Inline HQL and native SQL strings scattered through DAO method bodies.
- Setters present on `@Column(..., updatable = false)` fields across multiple JPA entities.
- Non-record constructors with up to 11 parameters without Checkstyle threshold checks.

## Goals / Non-Goals

**Goals:**
- Realign the `StudioRoleDAO` interface and `DefaultStudioRoleDAO` implementation into standard naming patterns.
- Optimize primary key lookups using `session.find` for direct cache utilization.
- Make GeoJSON parsing resilient against malformed input using `Doubles.tryParse`.
- Centralize queries as `@NamedQuery` / `@NamedNativeQuery` annotations or static constants.
- Enforce encapsulation by deleting setters for non-updatable entity fields.
- Add an automated Checkstyle check limiting non-record methods and constructors to at most 8 parameters, refactoring `AddressEntity` accordingly.
- Document and verify the `@Immutable` annotation standard across entities.

**Non-Goals:**
- Modifying HTTP API payloads, routes, or OpenAPI schemas.
- Changing database schemas, Flyway migrations, or table column definitions.
- Converting JPA entities into Java records (Hibernate entities require mutable state for dirty checking and proxying).

## Decisions

### 1. DAO Naming Realignment
- **Decision**: Rename interface `DefaultStudioRoleDAO` to `StudioRoleDAO`, and its concrete class `DefaultDefaultStudioRoleDAO` to `DefaultStudioRoleDAO`.
- **Rationale**: Restores naming consistency where interfaces declare the clean contract (`<Domain>DAO`) and default implementations use the `Default<Interface>` prefix. Eliminates the awkward `DefaultDefault*` stutter.
- **Alternatives Considered**: Keeping `DefaultStudioRoleDAO` as the interface name and naming the implementation `PostgresDefaultStudioRoleDAO` or `HibernateDefaultStudioRoleDAO`. Rejected because no other DAO uses infrastructure prefixes; all implementations follow `Default<Interface>`.

### 2. Primary Key Lookups via `session.find`
- **Decision**: In `DefaultStudioDAO` and similar lookups, replace `session.createQuery("from StudioEntity where id = :tenantId", StudioEntity.class)` with `session.find(StudioEntity.class, tenantId)`.
- **Rationale**: `session.find` checks the Hibernate first-level session cache and entity cache before generating SQL, avoids query parsing overhead, and expresses intent directly.
- **Alternatives Considered**: Keeping HQL queries. Rejected because HQL adds unnecessary compilation overhead for simple identifier lookups.

### 3. Safe Parsing with Guava `Doubles.tryParse`
- **Decision**: In `DefaultAddressDAO.parseGeoJson`, replace `Double.parseDouble` with `Doubles.tryParse(...)`.
- **Rationale**: Returns `null` when a string is unparseable or out-of-bounds, avoiding exception-handling overhead and preventing unexpected 500 server errors on corrupt coordinates.
- **Alternatives Considered**: `try / catch (NumberFormatException)`. Functional approach with `Doubles.tryParse` is cleaner and idiomatic in this codebase.

### 4. Query Standardization via Static Constants and Named Queries
- **Decision**: Migrate inline SQL and HQL strings to `private static final` constants in DAOs or `@NamedQuery` / `@NamedNativeQuery` annotations on entities.
- **Rationale**: Centralizes query definitions, eliminates string recreation inside methods, and enables Hibernate startup validation for named queries.
- **Alternatives Considered**: Externalizing queries to XML or properties files. Rejected as unnecessary overhead; annotations and static constants are compile-time verifiable within the module.

### 5. Removal of Setters on Non-Updatable Fields
- **Decision**: Remove setter methods for all entity fields marked `@Column(..., updatable = false)`.
- **Rationale**: JPA does not write changes to `updatable = false` columns during flush/merge. Exposing setters creates a false contract that the field can be changed in application code.
- **Alternatives Considered**: Making setters throw `UnsupportedOperationException`. Rejected because removing the method entirely provides compile-time protection.

### 6. Checkstyle Non-Record Parameter Threshold ($\le 8$) and `AddressEntity` Construction
- **Decision**:
  - Add an XPath Checkstyle rule inside `config/checkstyle/checkstyle.xml`:
    `//(METHOD_DEF | CTOR_DEF)[not(ancestor::RECORD_DEF) and count(PARAMETERS/PARAMETER_DEF) > 8]`
  - Refactor `AddressEntity` constructor from 11 parameters to a structured signature (e.g. `AddressEntity(UUID tenantId, UUID id, UUID locationId, String addressType)` plus setters for remaining address lines and areas, or a package-private builder).
- **Rationale**: Records are designed to carry arbitrary data fields, but constructors and methods on regular classes with 9+ parameters indicate an antipattern that creates error-prone argument order confusion.
- **Alternatives Considered**: Standard `ParameterNumber` checkstyle check. Standard `ParameterNumber` cannot distinguish records from classes; XPath `MatchXpath` targets `not(ancestor::RECORD_DEF)` precisely.

## Architecture & Boundary Diagrams

```
+-----------------------------------------------------------------------------------+
|                                     :api                                          |
|                                                                                   |
|    +----------------------------+             +-------------------------------+   |
|    |    StudioRoleAdminActor    | ----------> |         StudioRoleDAO         |   |
|    +----------------------------+             +---------------+---------------+   |
+---------------------------------------------------------------|-------------------+
                                                                | (Interface)
+---------------------------------------------------------------|-------------------+
|                                    :data                      |                   |
|                                                               v                   |
|    +--------------------------------------------------------------------------+   |
|    |                           DefaultStudioRoleDAO                           |   |
|    +--------------------------------------------------------------------------+   |
|                                       |                                           |
|                                       v                                           |
|    +--------------------------------------------------------------------------+   |
|    |                      DefaultStudioRoleEntity (JPA)                       |   |
|    |  - id: UUID (updatable=false, NO setter)                                 |   |
|    |  - name: String (updatable=true, with setName)                           |   |
|    +--------------------------------------------------------------------------+   |
|                                       |                                           |
|                                       v                                           |
|    +--------------------------------------------------------------------------+   |
|    |                        Hibernate Session (find / HQL)                    |   |
|    +--------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------+
```

## Risks / Trade-offs

- **[Risk] Test compilation failures due to removed setters or renamed DAOs** $\rightarrow$ **Mitigation**: Perform systematic refactoring with full search of callers in `:data` and `:api`, ensuring unit tests use constructor or builder instantiation.
- **[Risk] Checkstyle build failure on legitimate parameter lists** $\rightarrow$ **Mitigation**: The rule strictly targets non-records (`not(ancestor::RECORD_DEF)`); domain and CLI records (e.g. `Address`, `MigrationOptions`) remain unaffected.

## Migration Plan

1. Update `config/checkstyle/checkstyle.xml` with the XPath parameter limit.
2. Refactor `AddressEntity` and dependent DAO / test code to adhere to the 8-parameter limit.
3. Rename `DefaultStudioRoleDAO` $\rightarrow$ `StudioRoleDAO` and `DefaultDefaultStudioRoleDAO` $\rightarrow$ `DefaultStudioRoleDAO` across `:data` and `:api`.
4. Replace HQL queries with `session.find` where applicable.
5. Apply `Doubles.tryParse` in `DefaultAddressDAO`.
6. Remove redundant setters on `updatable = false` columns in all entities.
7. Centralize queries into static constants or `@NamedQuery`.
8. Execute `./gradlew check build` to verify formatting, Checkstyle, SpotBugs, ErrorProne, and test coverage gates.

## Open Questions

None. All decisions align with accepted ADRs (specifically ADR 0005, 0006, 0008, 0009).
