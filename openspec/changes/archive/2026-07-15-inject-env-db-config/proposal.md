## Why

Decouples environment variable lookup from the static factory method in DatabaseConfiguration.java. This aligns with Guice injection best practices, adheres to strict IOSP-Lite/Pure Factory coding invariants, and allows for cleaner unit/integration testing without modifying global System properties.

## What Changes

- **Add** `Environment` interface and `SystemEnvironment` implementation in the `:common` module.
- **Add** `DatabaseConfigurationProvider` in the `:data` module to resolve config properties from the injected `Environment`.
- **Modify** `DataModule` to bind `DatabaseConfiguration` using the provider in `Singleton` scope.
- **Modify** `DatabaseMigratorTest` and other unit tests to inject a map-backed `Environment` or mock rather than using `System.setProperty` or overriding `DatabaseConfiguration` bindings.

## Capabilities

### New Capabilities

- `env-db-config`: The database configuration must be resolved from environment variables.

### Modified Capabilities

None.

## Impact

Affects configuration loading in the `:data` layer. Enhances testing isolation for unit and integration tests.
