# nullability-contracts Specification

## Purpose

Defines codebase-wide nullability contracts including package-level non-null defaults via JSpecify `@NullMarked`, explicit `@Nullable` annotations for optional values, and elimination of redundant defensive runtime null checks across Guice dependency injection boundaries.

## Requirements

### Requirement: Package-Level Non-Null Defaults
All non-test, non-integration Java packages in Project Njall SHALL declare `@NullMarked` from JSpecify (`org.jspecify.annotations.NullMarked`) in their `package-info.java` files.

#### Scenario: Non-test package enforces non-null defaults
- **GIVEN** any package located under `src/main/java` in modules `:api`, `:common`, `:data`, and `:server`
- **WHEN** the package is analyzed by the Java compiler, SpotBugs, or ErrorProne
- **THEN** all method parameters, return values, type arguments, and fields in that package SHALL default to non-null unless explicitly annotated.

### Requirement: Explicit Nullable Annotations for Nullable Types
Any parameter, record component, or return value that can legitimately evaluate to `null` SHALL be explicitly annotated with `org.jspecify.annotations.Nullable`. Public command-line interface command accessors and options records SHALL return `java.util.Optional<T>` to isolate framework-level nullness at the command boundary rather than leaking `@Nullable` return values into application code. Where an array reference itself may legitimately be null, it SHALL be annotated using JSpecify type-use syntax as `Type @Nullable []` to annotate the array container rather than the array elements.

#### Scenario: Optional CLI options and return values are explicitly nullable
- **GIVEN** a method or constructor parameter that accepts `null` (such as internal CLI fields populated by Picocli or optional parameters in `CliRunner`)
- **WHEN** the method or field signature is declared
- **THEN** internal framework-injected fields SHALL be annotated with `@Nullable` while public command option accessors and options records SHALL return `Optional<T>`.

#### Scenario: Query methods returning null on absent records are explicitly nullable
- **GIVEN** an internal data retrieval method such as `DefaultServerDAO.findServerEntity`
- **WHEN** Hibernate returns `null` for non-existent entities
- **THEN** the method return type SHALL be explicitly annotated with `@Nullable`.

#### Scenario: Array parameters permitting null use container type annotation
- **GIVEN** a command runner or entry point method accepting an array parameter that may be `null`
- **WHEN** declaring the method parameter signature
- **THEN** the parameter SHALL be declared with `String @Nullable [] args` to designate the array reference as nullable while preserving non-null element constraints.

### Requirement: Streamlined Guice Injected Constructors
Constructors annotated with `@Inject` and provider methods annotated with `@Provides` in Guice modules SHALL rely on Guice's non-null injection guarantee and package-level contracts, omitting redundant `requireNonNull` validation checks.

#### Scenario: Guice-managed constructors omit runtime null validation
- **GIVEN** a service or component constructor annotated with `@Inject`
- **WHEN** dependencies are passed by the Guice injector
- **THEN** the constructor SHALL assign the dependencies directly to instance fields without invoking `Objects.requireNonNull`.

#### Scenario: Guice provider methods omit runtime null validation
- **GIVEN** a provider method in a Guice module annotated with `@Provides`
- **WHEN** dependencies are supplied by Guice
- **THEN** the method SHALL consume the dependencies directly without defensive null assertions.

### Requirement: ErrorProne Static Compiler Enforcement
The **Njall** build system SHALL enforce ErrorProne compile-time static analysis rules `AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, and `RedundantNullCheck` as fatal compilation errors across all Java compilation tasks in all **modules**.

#### Scenario: Missing package null-marked annotation triggers build failure
- **GIVEN** any `package-info.java` file in a production or shared test **module**
- **WHEN** the **module** is compiled with ErrorProne
- **THEN** the compiler SHALL report a fatal compilation error if the package declaration lacks the `@NullMarked` annotation.

#### Scenario: Unannotated parameter null handling triggers build failure
- **GIVEN** a method or constructor parameter in a `@NullMarked` package that is not annotated with `@Nullable`
- **WHEN** the method or constructor performs null-checking or defensive branching on that parameter
- **THEN** ErrorProne SHALL report a `ParameterMissingNullable` compilation error.

#### Scenario: Redundant null check on contractually non-null type triggers build failure
- **GIVEN** an expression or variable that is statically non-null under `@NullMarked`
- **WHEN** code evaluates `== null`, `!= null`, or `Objects.requireNonNull` on that expression
- **THEN** ErrorProne SHALL report a `RedundantNullCheck` compilation error.

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

### Requirement: Guice Result Assertion Elimination
Test suites and production callers SHALL NOT assert `.isNotNull()` or check nullness on results returned from `Guice.createInjector` or `injector.getInstance`, relying on Guice's inherent guarantee that successful resolution yields non-null instances.

#### Scenario: Module tests verify binding resolution without redundant null assertions
- **GIVEN** a test verifying a Guice **module** configuration
- **WHEN** an injector or bound instance is resolved
- **THEN** assertions SHALL target specific functional properties, types, or singleton identities rather than checking for nullity.
