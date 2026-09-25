# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal name of the project repository whose build pipeline and design are described. |
| Module | `glossary/technical.md` | Gradle modules (:common, :data, :api, :server) and Guice modules affected by the design. |
| Server | `glossary/technical.md` | Server application and CLI entry points. |
| Data plane | `glossary/technical.md` | Domain models, records, and repositories where constructor null-checking is eliminated. |
| API plane | `glossary/technical.md` | API routes and Pekko actors handling command validation. |
| DTO | `glossary/technical.md` | Data transfer objects validated at external boundaries. |
| DAO | `glossary/technical.md` | Data access objects whose internal mappers are converted to non-null contracts. |
