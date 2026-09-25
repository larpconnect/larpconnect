## ADDED Requirements

### Requirement: Record Objects Must Not Declare Static Factory Methods
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any `record` class within the `com.larpconnect.njall..` namespace MUST NOT declare static factory methods that return either the record's own type or an `Optional` containing the record's type. All record construction SHALL occur through public canonical or overloaded constructors, or through external Guice-managed factories or providers. Static utility methods returning types other than the record or `Optional` of the record SHALL remain permitted.

#### Scenario: ArchUnit architecture suite detects zero static factory methods on record classes
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero `record` classes declare static methods whose return type is the record type or an `Optional` of the record type

#### Scenario: Static utility functions on record classes are permitted
- **GIVEN** a `record` class within `com.larpconnect.njall..` that declares a static method returning a boolean, primitive, or unrelated type
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** ArchUnit treats the method as an allowed utility function and passes without violations
