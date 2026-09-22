## Context

The Project **Njall** codebase adheres to strict software craftsmanship standards (Java 25 LTS, Pekko Typed, and IOSP-Lite behavioral isolation). Previously, boolean flag parameters were introduced in two architectural locations:
1. The **data plane** and query layer (`StudioDAO.list(boolean includeDeleted)`, `findById(..., boolean includeDeleted)`, `findByAlias(..., boolean includeDeleted)`), which propagated `boolean` flags upward into Pekko **Actor** command records (`StudioAdminCommand`) and route helper methods.
2. The **API plane** response completion handlers in `RoleAdminRoute`, `StudioAdminRoute`, and `UserAdminRoute`, where a `boolean isCreateOperation` parameter was passed through private routing pipelines to toggle between `StatusCodes.CREATED` (201) and `StatusCodes.OK` (200).

Raw boolean parameters obscure caller intent at call sites (`dao.list(true)` vs `dao.list(false)`), prevent clean Pekko DSL method references, and limit domain extensibility.

```
+──────────────────────────────────────────────────────────────────────────────+
|                 C4 COMPONENT DIAGRAM: ELIMINATING BOOLEAN FLAGS              |
+──────────────────────────────────────────────────────────────────────────────+
|                                                                              |
|  [HTTP Client]                                                               |
|        |                                                                     |
|        | GET /api/admin/v1/studios?include_deleted=true                      |
|        v                                                                     |
|  +────────────────────────────────────────────────────────────────────────+  |
|  | API Plane: StudioAdminRoute                                            |  |
|  | - Parses query parameter into DeletionFilter                           |  |
|  | - Employs dedicated response mappers:                                  |  |
|  |   * onComplete(this::askCreateStudio, this::mapCreateResponse)         |  |
|  |   * onComplete(this::askListStudios, this::mapOkResponse)              |  |
|  +───────────────────────────────────┬────────────────────────────────────+  |
|                                      |                                       |
|                                      | ListStudios(DeletionFilter, replyTo)  |
|                                      v                                       |
|  +────────────────────────────────────────────────────────────────────────+  |
|  | Application Plane: StudioAdminActor                                    |  |
|  | - Consumes typed DeletionFilter in command records                     |  |
|  | - Dispatches to DAO using explicit domain filter                       |  |
|  +───────────────────────────────────┬────────────────────────────────────+  |
|                                      |                                       |
|                                      | studioDao.list(filter)                |
|                                      v                                       |
|  +────────────────────────────────────────────────────────────────────────+  |
|  | Data Plane: StudioDAO / DefaultStudioDAO                               |  |
|  | - StudioDAO.list(DeletionFilter filter)                                |  |
|  | - StudioDAO.findById(UUID id, DeletionFilter filter)                   |  |
|  | - StudioDAO.findByAlias(String alias, DeletionFilter filter)           |  |
|  | - DeletionFilter { ACTIVE_ONLY, INCLUDE_DELETED }                       |  |
|  +────────────────────────────────────────────────────────────────────────+  |
|                                                                              |
+──────────────────────────────────────────────────────────────────────────────+
```

## Goals / Non-Goals

**Goals:**
- Introduce a typed domain enum `DeletionFilter` (`ACTIVE_ONLY`, `INCLUDE_DELETED`) in `com.larpconnect.njall.data.domain`.
- Refactor `StudioDAO` and `DefaultStudioDAO` method signatures to accept `DeletionFilter`, retaining zero-argument convenience overloads that default to `DeletionFilter.ACTIVE_ONLY`.
- Update `StudioAdminCommand` records (`ListStudios`, `GetStudioById`, `GetStudioByAlias`) and `StudioAdminActor` to consume `DeletionFilter`.
- In `RoleAdminRoute`, `StudioAdminRoute`, and `UserAdminRoute`, eliminate `boolean isCreateOperation` by introducing dedicated `mapCreateResponse` and `mapOkResponse` methods that enable Pekko `this::mapCreateResponse` / `this::mapOkResponse` method references.
- Ensure 100% backward compatibility for the external HTTP REST API contract and database schemas.

**Non-Goals:**
- Altering the OpenAPI specification or external query parameter names (`?include_deleted=true`).
- Adding soft-delete functionality to entities that currently do not support it (`Server`, `AdminUser`, `AdminRole`).

## Decisions

### Decision 1: Domain-Level `DeletionFilter` Enum
- **Decision**: Define `public enum DeletionFilter { ACTIVE_ONLY, INCLUDE_DELETED }` in package `com.larpconnect.njall.data.domain`. Include helper method `public boolean includesDeleted() { return this == INCLUDE_DELETED; }`.
- **Rationale**: Replaces mystery boolean parameters (`true`/`false`) with explicit, self-documenting domain constants. Placing it in `data.domain` allows both the data layer and API layer to share the filter contract without circular dependencies.
- **Alternatives Considered**:
  - *Separate DAO query methods (`list()`, `listIncludingDeleted()`)*: Doubles method count across `StudioDAO` and doesn't resolve parameter passing in Pekko command records.
  - *String-based query parameters or flags*: Loses compile-time type safety.

### Decision 2: Zero-Argument Convenience Overloads on `StudioDAO`
- **Decision**: Provide parameterless `list()`, `findById(UUID id)`, and `findByAlias(String alias)` on `StudioDAO` as default methods forwarding to `DeletionFilter.ACTIVE_ONLY`.
- **Rationale**: Satisfies the generic `DAO<StudioLookup>` contract (`list()` and `findById(UUID id)`) while making the common case (querying active, non-deleted records) concise and ergonomic.
- **Alternatives Considered**:
  - *Requiring `DeletionFilter` on every single invocation without defaults*: Breaks symmetry with `DAO<T>` interface and adds boilerplate to call sites that only care about active **Studio** lookups.

### Decision 3: Dedicated Route Response Mappers
- **Decision**: In `RoleAdminRoute`, `StudioAdminRoute`, and `UserAdminRoute`, replace `mapResponseToRoute(..., boolean isCreateOperation)` with `mapCreateResponse(Try<T>)` and `mapOkResponse(Try<T>)` delegating to a private helper accepting `StatusCode successStatus`.
- **Rationale**: Allows direct method references (`this::mapCreateResponse`, `this::mapOkResponse`) in `onComplete(...)`, eliminating lambda syntax and the boolean flag.
- **Alternatives Considered**:
  - *Passing `StatusCode` directly to `mapResponseToRoute`*: While better than a boolean, it still requires lambda wrapping (`responseTry -> mapResponseToRoute(responseTry, StatusCodes.CREATED)`), whereas separate methods allow clean method references.

## Risks / Trade-offs

- **[Risk] Test compilation breakages in unit tests passing boolean literals**:
  - *Mitigation*: Update unit tests in `DefaultStudioDAOTest`, `StudioAdminRouteTest`, and integration step definitions to supply `DeletionFilter` or invoke parameterless overloads.
- **[Risk] Null check omission**:
  - *Mitigation*: Ensure `DefaultStudioDAO` and `StudioAdminCommand` enforce `Objects.requireNonNull(filter, "filter cannot be null")`.

## Migration Plan

No database schema migration is required. All changes are compile-time internal refactorings within `:data` and `:api`.

## Open Questions

None. The design is coherent with all in-force ADRs (0001 through 0012).
