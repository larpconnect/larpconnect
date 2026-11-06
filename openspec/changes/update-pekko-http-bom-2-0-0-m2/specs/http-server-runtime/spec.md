## ADDED Requirements

### Requirement: Pekko HTTP Bill of Materials Version Alignment
The system SHALL declare `org.apache.pekko:pekko-http-bom_3` at version `2.0.0-M2` or higher in `gradle/libs.versions.toml` and import it as a platform constraint in the `:parent` **Module**, governing all Pekko HTTP artifacts (`pekko-http_3`, `pekko-http-jackson_3`, and `pekko-http-testkit_3`) consumed across `:api`, `:server`, `:test`, and `:integration`.

#### Scenario: Pekko HTTP BOM resolves to 2.0.0-M2 across dependent modules
- **GIVEN** `gradle/libs.versions.toml` defines `pekko-http = "2.0.0-M2"`
- **AND** `:parent` imports `libs.pekko.http.bom` as a platform dependency
- **WHEN** the Gradle build resolves dependencies and executes tests across `:api`, `:server`, `:test`, and `:integration`
- **THEN** `org.apache.pekko:pekko-http-bom_3` and its managed artifacts resolve to version `2.0.0-M2`
- **AND** all HTTP routing, Jackson marshalling, and **Server** lifecycle checks pass without compilation or runtime regressions

