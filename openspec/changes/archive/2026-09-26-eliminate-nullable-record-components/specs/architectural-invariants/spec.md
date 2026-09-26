## ADDED Requirements

### Requirement: Record Components Must Not Be Annotated With Nullable
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any `record` class within the `com.larpconnect.njall..` namespace (excluding test classes) MUST NOT declare record components annotated with `org.jspecify.annotations.Nullable`. All record state SHALL be held in non-null types using `java.util.Optional<T>`, empty immutable collections, or sentinel enumeration constants. Record constructors MAY accept `@Nullable` parameters to perform boundary normalization.

#### Scenario: ArchUnit architecture suite detects zero record components annotated with @Nullable
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero `record` classes declare record components annotated with `@Nullable`

#### Scenario: Record constructors with @Nullable parameters are permitted for boundary normalization
- **GIVEN** a `record` class within `com.larpconnect.njall..` declaring an overloaded constructor or compact constructor accepting a parameter annotated with `@Nullable`
- **WHEN** the ArchUnit architecture test suite in `:integration` evaluates the record class
- **THEN** ArchUnit passes without violation provided the record component itself lacks `@Nullable`
