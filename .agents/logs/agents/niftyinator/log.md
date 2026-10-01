## 2026-09-27 18:50

- Java overload resolution preferentially binds null arguments and `ImmutableList` references to canonical record constructors over secondary overloaded constructors accepting generic `List` or `@Nullable` types due to subtyping specificity rules. Consequently, records such as `CreateUserRequest`, `UserAdminCommand.CreateUser`, and `RoleAssignmentRequest` must retain compact constructors to perform fallback normalization, requiring `@SuppressWarnings("RedundantNullCheck")` in `@NullMarked` packages to satisfy ErrorProne.
- Guice circular dependency verification in `ServerModuleTest` requires unread private fields to force dependency graph cycles through constructors, which ErrorProne flags as `[UnusedVariable]` without an explicit `@SuppressWarnings("unused")`.

## 2026-10-01 07:58

- Hibernate JPA `@ManyToMany` associations on `AdminUserEntity` must retain mutable `HashSet` fields rather than Guava `ImmutableSet` carriers because Hibernate requires mutable collection implementations for proxy instantiation, lazy fetching, and entity dirty tracking.
- Route implementations (`StudiosRoute`, `LinksRoute`, `LocationsRoute`, and admin routes) must retain public class visibility because Guice container assertions across package boundaries (such as `ApiModuleTest` in `com.larpconnect.njall.api`) verify bindings against concrete route classes in addition to `RouteProvider` set multibindings.
