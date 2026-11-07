## 1. Version Catalog Upgrade

- [x] 1.1 Update `pekko-http` in `gradle/libs.versions.toml` from `"2.0.0-M1"` to `"2.0.0-M2"` so `org.apache.pekko:pekko-http-bom_3` resolves to `2.0.0-M2` in the `:parent` platform **Module**.

## 2. Module-by-Module Compilation and Test Verification

- [x] 2.1 Run `./gradlew :test:check` to verify the `:test` **Library module** (`pekko-http-testkit_3`) compiles and passes quality checks before moving to downstream modules.
- [x] 2.2 Run `./gradlew :api:test :api:check` to verify all HTTP routes, Jackson marshallers (`pekko-http-jackson_3`), and route tests in the `:api` **Library module** pass and fix any issues if needed before moving to `:server`.
- [x] 2.3 Run `./gradlew :server:test :server:check` to verify the HTTP **Server** lifecycle (`DefaultHttpServerService`, `ServerBinding`, `CoordinatedShutdown`) in the `:server` **Application module** passes and fix any issues if needed before moving to `:integration`.
- [x] 2.4 Run `./gradlew :integration:test :integration:check` to verify all Cucumber end-to-end tests and ArchUnit architectural invariants pass.

## 3. Final Validation and Build Gate

- [x] 3.1 Run `openspec validate update-pekko-http-bom-2-0-0-m2 --type change --strict` to verify all OpenSpec artifacts conform to schema rules.
- [x] 3.2 Run `./gradlew check build` and confirm the entire build, unit/integration test suites, JaCoCo coverage gates, Spotless, Checkstyle, SpotBugs, and ErrorProne checks pass.

