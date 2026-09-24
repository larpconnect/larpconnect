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
Any parameter, record component, or return value that can legitimately evaluate to `null` SHALL be explicitly annotated with `org.jspecify.annotations.Nullable`. Public command-line interface command accessors and options records SHALL return `java.util.Optional<T>` to isolate framework-level nullness at the command boundary rather than leaking `@Nullable` return values into application code.

#### Scenario: Optional CLI options and return values are explicitly nullable
- **GIVEN** a method or constructor parameter that accepts `null` (such as internal CLI fields populated by Picocli or optional parameters in `CliRunner`)
- **WHEN** the method or field signature is declared
- **THEN** internal framework-injected fields SHALL be annotated with `@Nullable` while public command option accessors and options records SHALL return `Optional<T>`.

#### Scenario: Query methods returning null on absent records are explicitly nullable
- **GIVEN** an internal data retrieval method such as `DefaultServerDAO.findServerEntity`
- **WHEN** Hibernate returns `null` for non-existent entities
- **THEN** the method return type SHALL be explicitly annotated with `@Nullable`.

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
