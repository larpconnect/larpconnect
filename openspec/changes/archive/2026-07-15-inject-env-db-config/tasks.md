## 1. Common Module Refactoring

- [x] 1.1 Create `Environment` interface in `:common` module
- [x] 1.2 Create `SystemEnvironment` implementation in `:common` module
- [x] 1.3 Bind `Environment` to `SystemEnvironment` in `CommonModule`

## 2. Data Module Refactoring

- [x] 2.1 Create `DatabaseConfigurationProvider` in `:data` module
- [x] 2.2 Update `DataModule` to bind `DatabaseConfiguration` using `DatabaseConfigurationProvider` in `Singleton` scope

## 3. Testing and Verification

- [x] 3.1 Write unit tests for `DatabaseConfigurationProvider` in `DatabaseConfigurationProviderTest`
- [x] 3.2 Add `MapEnvironment` to test helpers
- [x] 3.3 Update `DatabaseMigratorTest` and `DataModuleTest` to use map-backed or mock environment instead of direct static overrides
