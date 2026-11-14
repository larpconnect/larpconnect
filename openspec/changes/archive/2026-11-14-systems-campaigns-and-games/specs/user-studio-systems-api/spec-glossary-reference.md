# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| System | `glossary/business.md` | Roleplaying or meeting ruleset entity managed under `/api/studios/{studio-id}/v1/systems`. |
| GameSystem | `glossary/technical.md` | Java implementation prefix (`GameSystem`, `GameSystemDAO`) avoiding `java.lang.System` collisions. |
| Entity | `glossary/business.md` | Root Common Table Inheritance record in `njall_users.entities`. |
| Studio | `glossary/business.md` | Tenanted organization resolved via `{studio-id}`. |
| Alias | `glossary/business.md` | Human-readable studio slug accepted in `{studio-id}`. |
| Actor | `glossary/business.md` | Federated studio actor identity required as entity generator. |
| Tenant | `glossary/business.md` | Isolated studio boundary enforced via PostgreSQL Row-Level Security. |
| generator | `glossary/business.md` | Responsible Actor recorded in `njall_users.entities.generator`. |
