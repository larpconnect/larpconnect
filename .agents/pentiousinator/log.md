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
