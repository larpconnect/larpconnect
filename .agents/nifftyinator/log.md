## 2026-03-15 - Populate :data module

**Learning:** When using Checkstyle, Hibernate Entities often fail to pass if they lack a no-args constructor (e.g., `[MissingCtor]`). However, resolving this by creating empty constructors can clash with SpotBugs complaining about exposed representations (EI/EI2), or Java compilation errors if dependencies are missing or constructors overlap with Guice provider fields. Modifying external config like spotbugs-exclude or gradle files safely resolves these checks. In Hibernate Reactive with Vert.x, avoid Java's built-in `Character` or `System` class names which cause `[JavaLangClash]` errors, requiring prefixes like `LarpSystem` and `LarpCharacter`.

**Action:** Anticipate Checkstyle `MissingCtor` rules for POJOs, and proactively prefix class names that conflict with `java.lang` (e.g., `System`, `Character`) before writing out the initial entity code.
