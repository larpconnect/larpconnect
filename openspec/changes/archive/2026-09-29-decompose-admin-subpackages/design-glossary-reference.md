# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Application whose architectural constraints dictate package decomposition. |
| Library module | `glossary/technical.md` | The `:api` subproject hosting the administrative routing and actor definitions. |
| Admin verticle | `glossary/technical.md` | Administrative slice of the application being structured into subpackages. |
| Module | `glossary/technical.md` | Guice modules (`AdminModule`, `HealthAdminModule`, etc.) and Gradle subprojects. |
| Actor | `glossary/technical.md` | Pekko Typed actors executing domain operations across administrative subpackages. |
| API plane | `glossary/technical.md` | The HTTP routing boundary where admin endpoints are exposed and aggregated. |
