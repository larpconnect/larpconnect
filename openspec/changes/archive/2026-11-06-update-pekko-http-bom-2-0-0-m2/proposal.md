## Why

**Njall** mandates declaring and utilizing the most recent versions of core runtime libraries and keeping the Apache Pekko ecosystem aligned with `pekko = "2.0.0-M4"`. Upgrading `org.apache.pekko:pekko-http-bom_3` from `2.0.0-M1` to `2.0.0-M2` brings upstream HTTP/2 error recovery and header-size hardening, native inflater/deflater memory leak fixes, RFC 9110 `Content-Length` parsing and `HEAD` rendering fixes, and build alignment with Apache Pekko `2.0.0-M4` and JUnit `6.1.3`.

## What Changes

- Update the `pekko-http` version entry in `gradle/libs.versions.toml` from `2.0.0-M1` to `2.0.0-M2`, upgrading `org.apache.pekko:pekko-http-bom_3` and all BOM-managed Pekko HTTP artifacts (`pekko-http_3`, `pekko-http-jackson_3`, and `pekko-http-testkit_3`).
- Verify full source compilation, static analysis (ErrorProne, SpotBugs, Checkstyle, Spotless), unit tests, and Cucumber/ArchUnit integration tests across every **Module** consuming Pekko HTTP (`:parent`, `:api`, `:server`, `:test`, and `:integration`).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `http-server-runtime`: Require the HTTP **Server** runtime and routing **Library module** dependencies managed by `org.apache.pekko:pekko-http-bom_3` in the `:parent` platform to resolve at version `2.0.0-M2` or higher without regression across routing, marshalling, or coordinated shutdown behaviors.

## Impact

- **Dependencies**: Upgrades `org.apache.pekko:pekko-http-bom_3` (and transitively `pekko-http_3`, `pekko-http-core_3`, `pekko-parsing_3`, `pekko-http-jackson_3`, and `pekko-http-testkit_3`) from `2.0.0-M1` to `2.0.0-M2` via `gradle/libs.versions.toml`.
- **Affected Modules**: `:parent` (Java platform BOM import), `:api` (**Library module** for HTTP routing and Jackson marshalling), `:server` (**Application module** for HTTP socket binding and lifecycle), `:test` (**Library module** exporting `pekko-http-testkit_3`), and `:integration` (end-to-end and ArchUnit verification).
- **Java Source Compatibility**: Upstream `2.0.0-M2` removals (`HttpApp`, `Coder.encode`, `ConnectionContext.defaultPort`, `ParserSettings.apply/create(Config)`, 1-arg `UnsupportedRequestContentTypeRejection.apply`, and deprecated `javadsl` `route` directive) are not used in **Njall**; no production or test Java source changes are anticipated unless uncovered by `./gradlew check build`.

