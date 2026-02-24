# Skill: Gradle and Build Files

## Domain Context
This skill handles working with gradle and all build associated files (e.g., settings.gradle)

## Technical Constraints

1. Use gradle 9+ with the kotlin DSL. Compile using Java 25+. The gradle wrapper (`./gradlew`) is the primary entry point. 
2. Dependency Governance: Versioning must be maintained exclusively within the Version Catalog to facilitate streamlined updates and auditing
3. Use the most recent version of dependencies unless there is a documented reason to avoid a particular dependency or set of dependencies.

## Specific Guidance

* Gradle modules should be named for what they are but published as `larpconnect-<module name>`, e.g., `:core` should become `larpconnect-core`. An exception to this is the `:server` module, which should be published as `:larpconnect`
* Store common build infrastructure in a `buildSrc`.
* Maintain alphabetical ordering for dependencies.
* Alphabetically order the type of dependency import as well, e.g., api, implementation, testRuntime, etc. Put a single whitespace line between these blocks.
* Vulnerability Scanning: Execution of ./gradlew dependencyCheckAnalyze is mandatory upon the addition of any external library to mitigate supply chain risks.
* Store common configurations in `buildSrc`

### Licensing Compliance

Authorized licenses for third-party libraries include:
 * Java Standard Library (OpenJDK)
 * BSD, MIT, ISC, Apache 2.0, or EPL.

The use of GPL, AGPL, or LGPLv3+ is prohibited. Utilization of LGPLv2-licensed libraries requires formal review and approval.

### Critical Libraries

These are libraries that are basically required from the start of the project.

* The JDK
* Spotless / Findbugs / ErrorProne / Checker Framework Qual annotations in the build system
* SLF4J everywhere
* Vert.x 5
* Logback in application/server modules 
* Guice, JSR330 via jakarta annotations
* Mug 
* Guava
* Mockito in tests
* Junit5 in tests. Do not use JUnit asserts
* AssertJ in tests, including the module for Guava
* Cucumber in integration tests
* Protobuf
* JSR305
* ErrorProne Annotations, `mug-errorprone`, Checker Qual annotations, Spotbugs annotations

### Encouraged Libraries

These may be used if the functionality is needed without worrying about the complexity of the import. 

* Mug extensions, and in particular `mug-guava` and `mug-concurrent24`
* Guava, especially if the functionality does not exist in the JDK or in Mug. 
* Caffeine for caching, if caches are needed. Use this in preference to Guava caches.
* Apache Commons libraries where the functionality does not exist in Mug, Guava, or the JDK. In particular `io` and `lang3`. Always prefer the implementation in Mug or Guava when one exists in either.
  * Also worth noting are `csv`, `RDF`, and `text` which may be used if the need comes up. These should always be used in preference to rolling our own implementation of any of the above functionality
* Guice extensions
* `dev.cel:cel` if needed for expressions or user-entered "code" that cannot be (or should not be) done declaratively
