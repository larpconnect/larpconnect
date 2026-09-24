## Why

Method signatures that accept raw `boolean` parameters (such as `list(boolean includeDeleted)` and `mapResponseToRoute(..., boolean isCreateOperation)`) introduce ambiguity at call sites ("mystery booleans"), obscure domain intent, and inhibit future extensibility. Replacing these boolean parameters with domain-specific enums and dedicated, intention-revealing methods makes API contracts self-documenting, type-safe, and cleanly extensible across the **DAO**, **Actor**, and route layers.

## What Changes

- **Domain Enum for Deletion Filtering**: Introduce a typed `DeletionFilter` enum with constants `ACTIVE_ONLY` and `INCLUDE_DELETED` in the data plane.
- **DAO Signature Refactoring**: Refactor `StudioDAO` and `DefaultStudioDAO` query methods (`list`, `findById`, `findByAlias`) to replace the `boolean includeDeleted` parameter with `DeletionFilter`, preserving parameterless convenience overloads that default to `ACTIVE_ONLY`.
- **Actor Message Refactoring**: Update `StudioAdminCommand` record definitions (`ListStudios`, `GetStudioById`, `GetStudioByAlias`) and `StudioAdminActor` to accept `DeletionFilter` instead of `boolean includeDeleted`.
- **Route Response Mapping Refactoring**: In `RoleAdminRoute`, `StudioAdminRoute`, and `UserAdminRoute`, eliminate the `boolean isCreateOperation` parameter across `mapResponseToRoute`, `mapSuccessResponse`, and `resolveSuccessStatus` by splitting them into dedicated, intention-revealing methods (`mapCreateResponse` and `mapOkResponse`) that can be bound directly via method references (`this::mapCreateResponse`, `this::mapOkResponse`).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `data-persistence`: Refactor `StudioDAO` and `DefaultStudioDAO` query method contracts to accept `DeletionFilter` instead of `boolean includeDeleted`.

## Impact

- **Affected Modules**: `:data` (domain entity/enum and DAO interfaces/implementations), `:api` (admin routes and Pekko command records), `:integration` (acceptance tests and DAO unit tests).
- **Public API Contract**: Zero externally observable REST API breaking changes; `GET /api/admin/v1/studios?include_deleted=true` continues to work identically.
- **Internal API**: Replaces boolean flag method signatures with `DeletionFilter` and dedicated route response mappers.
