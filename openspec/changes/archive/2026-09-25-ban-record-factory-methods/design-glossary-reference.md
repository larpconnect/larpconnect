# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application being refactored. |
| API plane | `glossary/technical.md` | Layer containing HTTP routes, DTO records, and API directives. |
| Data plane | `glossary/technical.md` | Layer containing persistence entities, DAOs, and database configurations. |
| Application plane | `glossary/technical.md` | Layer managing Pekko Typed actor commands and core domain execution. |
| Library module | `glossary/technical.md` | Gradle modules (:common, :api, :data, :integration) housing components and tests. |
| Actor | `glossary/technical.md` | Pekko Typed actors receiving messages wrapped in ApiCall envelopes. |
