## Context

Currently, database configuration is read eagerly and statically in `DatabaseConfiguration.java` using `System.getenv()`. This configuration record is bound as an eager singleton instance in `DataModule.java`. This introduces circular/tight dependencies on environment variables, forces static lookup code to run even during testing or when modules are overridden, and violates strict coding invariants (IOSP-Lite/Pure Factory Isolation).

## Goals / Non-Goals

**Goals:**
- Decouple database configuration resolution from static global state.
- Implement an `Environment` abstraction in `:common` module.
- Implement `DatabaseConfigurationProvider` in `:data` module to resolve database settings dynamically and lazily.
- Ensure strict compliance with code quality metrics: IOSP-Lite (logic vs integration segregation) and Pure Factory Isolation.
- Improve testability by allowing map-backed or mock environments to be injected.

**Non-Goals:**
- Changing database schemas or flyway migration logic.
- Adding Vert.x dependencies to the `:data` module.
- Modifying environment variable names themselves (`DB_HOST`, `DB_PORT`, etc.).

## Decisions

- **Decision 1: Add Environment abstraction in `:common`**
  - *Rationale*: Environment lookup is a general-purpose utility. Placing it in `:common` allows other modules (like Vert.x api verticle, queue consumer, etc.) to reuse the same abstraction.
  - *Alternatives considered*: Placing it inside `:data` (but then other modules couldn't reuse it, or we'd have to replicate it).
- **Decision 2: Use a custom Guice `Provider` class for `DatabaseConfiguration`**
  - *Rationale*: A dedicated `DatabaseConfigurationProvider` class encapsulates the parsing logic, keeps the `DatabaseConfiguration` record clean, and makes it easy to adhere to the Integration-Operation Segregation Principle (IOSP-Lite).
  - *Alternatives considered*: Guice `@Provides` method in `DataModule`. However, since the method needs to perform parsing logic (e.g., parsing the port with fallback to 5432) and map fields, doing so in a single `@Provides` method would either mix logic/integration or require multiple helper methods in the Module itself, cluttering it. A dedicated provider class is cleaner and more testable.

## Risks / Trade-offs

- **Risk**: Missing database credentials when defaults are used.
  - *Mitigation*: Fallbacks match existing defaults exactly (`localhost`, `5432`, `larpconnect`, `postgres`, `postgres`).
- **Risk**: Potential circular dependency if `:common` depends on other modules.
  - *Mitigation*: `:common` is the base module in the project DAG and does not depend on any other project module.
