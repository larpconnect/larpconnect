## MODIFIED Requirements

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
