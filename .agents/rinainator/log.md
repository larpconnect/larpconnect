## 2025-03-01 - Avoid @Named in Guice DI

**Learning:** The project's Java coding standards specifically avoid using `@Named` for Guice dependency injection, preferring custom qualifier annotations. This makes tracking dependencies across the codebase easier.

**Action:** Replace instances of `@Named` with custom qualifier annotations, specifically in `WebServerVerticle` and `ServerBindingModule`.
