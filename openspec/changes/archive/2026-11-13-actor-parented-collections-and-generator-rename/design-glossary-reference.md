# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| User plane | `glossary/technical.md` | Horizontal slice hosting studio HTTP routes and Pekko actors. |
| Collections | `glossary/business.md` | Ordered entity container repathed beneath its generating actor. |
| Entity | `glossary/business.md` | Root federatable object stored in `njall_users.entities`. |
| Actor | `glossary/business.md` | Federated identity entity establishing collection URL parentage and provenance. |
| generator | `glossary/business.md` | Structural actor provenance column (`njall_users.entities.generator`) and JSON URI field. |
| owner | `glossary/business.md` | Semantic attachment target stored in `njall_users.collections.owner_id`. |
| Individual | `glossary/business.md` | Non-actor person entity eligible as a semantic collection `ownerId`. |
| Events | `glossary/business.md` | Gathering entity eligible as a semantic collection `ownerId`. |
| Studio | `glossary/business.md` | Tenanted organization resolved via `{studio-id}` UUID or alias. |
| Tenant | `glossary/business.md` | Studio Row-Level Security partition boundary. |
| DAO | `glossary/technical.md` | Data access object abstracting tenanted Hibernate sessions. |
| Client | `glossary/business.md` | External HTTP consumer calling the studio API. |

