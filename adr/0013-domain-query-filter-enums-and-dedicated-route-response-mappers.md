# 0013: Domain Query Filter Enums and Dedicated Route Response Mappers

- Status: accepted
- Date: 2026-09-22

## Context

Method signatures accepting raw `boolean` parameters (such as `list(boolean includeDeleted)` and `mapResponseToRoute(..., boolean isCreateOperation)`) introduce ambiguity at call sites ("mystery booleans"), obscure domain intent, inhibit future query extensibility, and prevent clean Pekko HTTP Java DSL method references (`this::methodName`). 

The repository requires unambiguous, intention-revealing APIs across the data access layer, Pekko Typed actor protocols, and HTTP route handlers.

## Decision

1. **Query Filter Enums**:
   - Soft-deletion or inclusion filtering across the data plane and Pekko actor command protocols MUST use domain-specific enums (such as `DeletionFilter { ACTIVE_ONLY, INCLUDE_DELETED }`) rather than raw `boolean` parameters.
   - DAOs MUST provide parameterless convenience overloads that default to `ACTIVE_ONLY` (e.g., `list()`, `findById(id)`, `findByAlias(alias)`).
2. **Dedicated Route Response Mappers**:
   - HTTP route handlers in the API plane MUST NOT accept `boolean` flags to determine success HTTP status codes.
   - Handlers MUST use dedicated, intention-revealing mapping methods (e.g., `mapCreateResponse` for 201 Created and `mapOkResponse` for 200 OK) that can be bound directly via method references (`this::mapCreateResponse`, `this::mapOkResponse`) in `onComplete(...)` directives.

## Consequences

### Positive
- Call sites are self-documenting (e.g., `dao.list(DeletionFilter.INCLUDE_DELETED)` instead of `dao.list(true)`).
- Filter enums can be extended in the future (e.g., `DELETED_ONLY` for administrative restore/audit tooling) without signature changes.
- Pekko HTTP route directives can bind cleanly via method references without wrapper lambdas.
- Preserves backward compatibility of generic `DAO<T>` methods (`list()` and `findById(id)`).

### Negative
- Requires maintaining small domain enums and parameterless convenience overloads.
