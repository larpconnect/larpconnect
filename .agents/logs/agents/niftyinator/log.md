## 2026-09-27 18:50

- Java overload resolution preferentially binds null arguments and `ImmutableList` references to canonical record constructors over secondary overloaded constructors accepting generic `List` or `@Nullable` types due to subtyping specificity rules. Consequently, records such as `CreateUserRequest`, `UserAdminCommand.CreateUser`, and `RoleAssignmentRequest` must retain compact constructors to perform fallback normalization, requiring `@SuppressWarnings("RedundantNullCheck")` in `@NullMarked` packages to satisfy ErrorProne.
- Guice circular dependency verification in `ServerModuleTest` requires unread private fields to force dependency graph cycles through constructors, which ErrorProne flags as `[UnusedVariable]` without an explicit `@SuppressWarnings("unused")`.
