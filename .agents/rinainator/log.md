## 2025-03-01 - Avoid @Named in Guice DI

**Learning:** The project's Java coding standards specifically avoid using `@Named` for Guice dependency injection, preferring custom qualifier annotations. This makes tracking dependencies across the codebase easier.

**Action:** Replace instances of `@Named` with custom qualifier annotations, specifically in `WebServerVerticle` and `ServerBindingModule`.

## 2026-03-06 - Extracted Guice bindings to a BindingModule to respect architecture rules

**Learning:** According to `.agents/skills/guice_di.md`, each package should have a standard pattern of having a public `Module` that installs a package-private `BindingModule`, rather than putting bindings directly into the public `Module`. `ApiModule` in the `com.larpconnect.njall.api` package did not follow this pattern.

**Action:** Created `ApiBindingModule` and moved `ApiObjectParser`, `JsonFormat.Printer`, and `JsonFormat.Parser` bindings there. Modified `ApiModule` to install `ApiBindingModule` using `@InstallInstead(ApiModule.class)`. This matches the Guice DI standards.
