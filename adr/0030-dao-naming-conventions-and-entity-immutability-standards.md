# 0030: DAO Naming Conventions and Entity Immutability Standards

## Status

Accepted

## Date

2026-09-30

## Context

In ADR 0005, Project Njall established the sealed base **DAO** hierarchy and Hibernate session management architecture. Over subsequent feature increments, several quality inconsistencies and anti-patterns entered the persistence layer:
1. An interface was named `DefaultStudioRoleDAO`, which inverted the project's naming standard where interfaces represent the unadorned contract (`<Domain>DAO`) and implementations carry the `Default` prefix (`Default<Domain>DAO`). This led to the concrete class being named `DefaultDefaultStudioRoleDAO`.
2. Primary key entity lookups frequently employed HQL query string execution (`session.createQuery`) rather than direct identifier lookups (`session.find`), bypassing first-level cache lookups.
3. JPA entities declared mutator methods (setters) on columns configured with `@Column(..., updatable = false)`, violating domain immutability invariants and creating misleading APIs.
4. Non-record constructors accumulated up to 11 parameters without static threshold validation.

## Decision

1. **DAO Naming Convention**: All **DAO** interfaces in `:data` must be named `<Domain>DAO` without `Default` prefixes. Concrete implementations must be named `Default<Domain>DAO`. The sealed `DAO<T extends DatabaseObject>` interface must permit only these clean interface types.
2. **Direct Primary Key Lookups**: All single-entity primary key lookups must use `session.find(EntityClass.class, id)` instead of HQL string queries.
3. **Entity Field Immutability**: No JPA entity class may declare setter methods for persistent fields annotated with `updatable = false`. If every field on an entity is non-updatable after insertion, the entity class must be annotated with Hibernate `@Immutable`.
4. **Parameter Threshold Enforcement**: Constructors and methods on non-record classes are limited to a maximum of 8 parameters, enforced by automated Checkstyle rules. Complex object creation must use builders, parameter objects, or identity grouping.
5. **Safe Coordinate Parsing**: GeoJSON coordinate extraction must utilize exception-safe parsing via Guava `Doubles.tryParse`, returning an empty `Optional` on invalid numeric formats rather than throwing `NumberFormatException`.

## Consequences

- **Positive**: Clean, predictable naming across all persistence interfaces and implementations; eliminated `DefaultDefault*` naming stutter; enforced encapsulation on database-managed immutable identifiers and audit fields; guaranteed compile-time or static check prevention of bloated constructors.
- **Negative**: Requires refactoring entity instantiation call sites that previously relied on 9+ parameter constructors.
- **Related ADRs**: Complements ADR 0005 (Sealed DAO Architecture), ADR 0006 (Nullability Conventions), ADR 0021 (ArchUnit Record Factory Prohibition), and ADR 0022 (ArchUnit Immutable Records).
