## 2026-03-01 - JUnit 6 and Java 25

**Learning:** This project uses JUnit 6 (version `6.0.3`), which requires Java
17+ and is fully compatible with Java 25. This was unexpected as JUnit 5 is
still the dominant version in many environments. The build system also enforces
Java 25 via toolchains and `--release` flags.

**Action:** When working on tests or build configuration, ensure compatibility
with JUnit 6 API and Java 25 language features. Do not downgrade to JUnit 5 or
lower Java versions.

## 2026-03-01 - Build Logic Organization

**Action:** Consolidate core compiler options (encoding, parameters) into the
base Java plugin (`larpconnect.java-common`) and keep `larpconnect.quality`
focused on linting and analysis tools.

## 2026-03-12 - Spotless Markdown and Flexmark overrides

**Learning:** The configurations setting `enabled = false` for Spotless tasks matching `*MarkdownCheck` or `*FlexmarkCheck` in `build.gradle.kts` and `buildSrc/src/main/kotlin/larpconnect.quality.gradle.kts` are deliberate design choices for this project.

**Action:** Do not remove or alter these `enabled = false` overrides during build cleanup or maintenance tasks.

## 2026-03-18 - Centralizing SpotBugs Exclusions

**Learning:** SpotBugs exclusions must be centralized in the global `config/spotbugs/exclude.xml` file. Applying `excludeFilter.set(...)` in individual project build scripts (e.g., `server/build.gradle.kts` or `proto/build.gradle.kts`) drops the global exclusions defined in the `larpconnect.quality` convention plugin instead of merging with them.

**Action:** Whenever a new SpotBugs exclusion is needed for a specific module, add it to the global `config/spotbugs/exclude.xml` with appropriate matchers (e.g., `<Package>` or `<Class>`) rather than creating a local exclusion file and overriding the filter.
## 2026-03-24 - Removing Leaky compileOnly Dependencies from Quality Plugin

**Learning:** In Gradle build scripts, injecting compile-time dependencies (such as `@Nullable`, `@SuppressFBWarnings` annotations) globally via quality-focused convention plugins (e.g., `larpconnect.quality`) is an anti-pattern. This implicitly leaks these dependencies to all modules applying the quality plugin, creating a hidden dependency hierarchy.

**Action:** Manage shared dependencies explicitly within centralized core modules (like `:parent`), which downstream projects depend on via `api` or `implementation`, rather than implicitly sneaking them into compilation via quality script plugins.
