## ADDED Requirements

### Requirement: Non-Null Record Components and Boundary Normalization
All `record` objects across all packages SHALL define strictly non-null components. Where a component value may legitimately be omitted or unspecified at creation time, the record component SHALL be declared using one of:
1. `java.util.Optional<T>` for general object references.
2. Empty immutable collections (`com.google.common.collect.ImmutableList<T>`, `ImmutableSet<T>`, `ImmutableMap<K, V>`).
3. An in-memory sentinel constant on the enumeration type (such as `AdminUserStatus.UNKNOWN`).

Record components SHALL NOT be annotated with `org.jspecify.annotations.Nullable`. Record constructors MAY accept `@Nullable` parameters to normalize null inputs into non-null representations. Sentinel enum values SHALL be resolved by the application layer before reaching database boundaries, and **DAO** implementations SHALL reject sentinel enum values.

#### Scenario: Optional general components wrapped in Optional
- **GIVEN** a record with an optional single-value component such as `roleId` or `deletedAt`
- **WHEN** the record is declared
- **THEN** the record component SHALL be declared with type `Optional<T>` and SHALL NOT be annotated with `@Nullable`

#### Scenario: Collection components normalized to empty immutable collections
- **GIVEN** a record accepting a collection of elements that may be omitted or empty
- **WHEN** the record is declared
- **THEN** the record component SHALL be declared as an immutable collection (such as `ImmutableList<T>`) without `@Nullable`
- **AND** passing `null` to a constructor SHALL normalize the component to an empty immutable collection

#### Scenario: Enum components use in-memory sentinel for unspecified states
- **GIVEN** a command or request record accepting an enumeration value that may be omitted at creation
- **WHEN** the record is declared
- **THEN** the record component SHALL be declared with the non-null enum type
- **AND** omitting the enum in a constructor or payload SHALL normalize the component to a sentinel value such as `UNKNOWN`

#### Scenario: DAO layer rejects sentinel enum values
- **GIVEN** a **DAO** implementation persisting domain entities to PostgreSQL
- **WHEN** an operation receives an entity or argument with a sentinel enum value like `AdminUserStatus.UNKNOWN`
- **THEN** the **DAO** SHALL reject the operation with an `IllegalArgumentException` rather than attempting persistence
