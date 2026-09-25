## ADDED Requirements

### Requirement: Record Objects Must Be Annotated with ErrorProne Immutable
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any `record` class within the `com.larpconnect.njall..` namespace (excluding test classes) MUST be annotated with `@com.google.errorprone.annotations.Immutable`.

#### Scenario: ArchUnit architecture suite verifies all record classes are annotated with @Immutable
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero `record` classes lack the `@com.google.errorprone.annotations.Immutable` annotation

#### Scenario: Non-record classes are not required to have @Immutable
- **GIVEN** compiled non-test classes in package `com.larpconnect.njall..` that are not records
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** ArchUnit ignores non-record classes for this rule and passes without violations

#### Scenario: Test records are exempted from @Immutable enforcement
- **GIVEN** record classes located within test packages or test source directories
- **WHEN** the ArchUnit architecture test suite executes with test exclusion import options
- **THEN** ArchUnit does not evaluate test records for `@Immutable`
