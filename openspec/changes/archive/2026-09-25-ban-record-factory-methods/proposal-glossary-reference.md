# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application being refactored. |
| API plane | `glossary/technical.md` | Request-facing layer containing HTTP directives, routes, and DTO records. |
| Data plane | `glossary/technical.md` | Persistence layer containing entity records, DAOs, and database configurations. |
| Application plane | `glossary/technical.md` | Internal processing layer containing actor commands and services. |
| Module | `glossary/technical.md` | Gradle and Guice modules configured across the repository. |
| Library module | `glossary/technical.md` | Gradle modules (:common, :api, :data, :integration) defining components and invariants. |
| Actor | `glossary/technical.md` | Pekko Typed actors receiving commands wrapped in ApiCall. |
