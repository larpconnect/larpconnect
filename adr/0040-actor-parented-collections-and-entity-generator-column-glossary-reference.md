# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| User plane | `glossary/technical.md` | Horizontal slice managing studio and user runtime operations. |
| Collections | `glossary/business.md` | Ordered entity container nested beneath `/actors/{actor-id}/collections`. |
| Entity | `glossary/business.md` | Federatable root resource stored in `njall_users.entities`. |
| Actor | `glossary/business.md` | Federated identity entity establishing structural provenance and collection URL parentage. |
| generator | `glossary/business.md` | Structural actor provenance persisted in `njall_users.entities.generator` and exposed as a URI. |
| owner | `glossary/business.md` | Semantic parent entity persisted in `njall_users.collections.owner_id`. |
| Individual | `glossary/business.md` | Person entity eligible as a semantic collection `ownerId`. |
| Events | `glossary/business.md` | Gathering entity eligible as a semantic collection `ownerId`. |
| DAO | `glossary/technical.md` | Data access object enforcing tenant and active actor provenance rules. |
| Studio | `glossary/business.md` | Tenanted organization resolved from `{studio-id}`. |
| Tenant | `glossary/business.md` | Studio Row-Level Security data boundary. |
| FeatherPub | `glossary/technical.md` | ActivityPub-inspired federation protocol governing studio entity representations. |
| DTO | `glossary/technical.md` | Serialized request/response data transfer object in `:api`. |

