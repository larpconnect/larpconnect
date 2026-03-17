## 2026-03-17 - Refactoring System.exit for Testability

**Learning:** `System.exit(1)` is inherently difficult to test in modern Java without complicated SecurityManagers (which are deprecated for removal). If a class already has an injected `Runtime` dependency, replacing `System.exit(1)` with `runtime.exit(1)` makes it trivially easy to mock and verify application shutdown behavior during unit tests without halting the test JVM.

**Action:** Whenever introducing or testing JVM shutdown behavior, prefer injecting `Runtime` and calling `runtime.exit()` rather than using the static `System.exit()`, allowing seamless testing with mock `Runtime` instances.
