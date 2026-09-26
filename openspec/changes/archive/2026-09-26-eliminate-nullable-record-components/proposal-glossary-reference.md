# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | The internal name of the application whose codebase is being modified. |
| API plane | `glossary/technical.md` | The layer containing HTTP routes, requests, and commands whose records are updated. |
| Data plane | `glossary/technical.md` | The layer containing domain records, configs, entities, and DAOs. |
| Application plane | `glossary/technical.md` | Internal application and actor messaging layers passing record commands. |
| Admin verticle | `glossary/technical.md` | The vertical slice managing server administrative users, roles, and studios. |
| Actor | `glossary/technical.md` | Pekko actors handling command records and resolving sentinel states. |
| DAO | `glossary/technical.md` | Data access objects persisting records and guarding against sentinel values. |
| Module | `glossary/technical.md` | Gradle modules within the project. |
| Library module | `glossary/technical.md` | Gradle modules providing functionality such as `:integration`. |
