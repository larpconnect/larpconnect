## MODIFIED Requirements

### Requirement: Domain and Record Constructor Contract Enforcement
Constructors, compact constructors, factory methods, and public operations across domain objects, configuration classes, services, directives, and **DAO** implementations in `@NullMarked` packages SHALL rely on package-level non-null contracts and SHALL NOT perform runtime null validation (`Objects.requireNonNull` or `if (param == null)`) on parameters or fields unless explicitly annotated with `@Nullable`.

#### Scenario: Record compact constructor assigns components directly
- **GIVEN** a domain record such as `Server`, `ServerContact`, `StudioLookup`, `AdminRole`, or `AdminUser` in the **data plane**
- **WHEN** an instance is instantiated via its canonical or compact constructor
- **THEN** the constructor SHALL NOT invoke `Objects.requireNonNull` on any unannotated record components.

#### Scenario: Methods and factories omit defensive runtime null validation
- **GIVEN** a service, factory, configuration class, directive, or **DAO** method in a `@NullMarked` package
- **WHEN** the method or constructor receives parameters that lack `@Nullable` annotations
- **THEN** the method or constructor SHALL consume and assign parameters directly without invoking `Objects.requireNonNull` or checking for `null`.

#### Scenario: Unit tests rely on compile-time null safety rather than testing runtime NPE
- **GIVEN** a unit test suite for any class, record, factory, directive, or **DAO**
- **WHEN** evaluating method or constructor behavior
- **THEN** the test suite SHALL NOT include test cases that pass `null` to non-nullable parameters to assert `NullPointerException`.
