# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| User plane | `glossary/technical.md` | Horizontal slice managing user-facing studio endpoints and actors. |
| Data plane | `glossary/technical.md` | Horizontal slice managing tenant persistence and database queries. |
| FeatherPub | `glossary/technical.md` | ActivityPub-inspired federated protocol developed for Njall. |
| Entity | `glossary/business.md` | Root federatable object stored in `njall_users.entities`. |
| Actor | `glossary/business.md` | Federated identity capable of generating entities and owning collections. |
| Collections | `glossary/business.md` | Ordered entity container repathed under its generating actor. |
| Studio | `glossary/business.md` | Tenanted organization hosting actors and collections. |
| Tenant | `glossary/business.md` | Isolated studio boundary enforced via Row-Level Security. |
| generator | `glossary/business.md` | Structural actor responsible for creating an entity (`entities.generator`). |
| owner | `glossary/business.md` | Semantic parent entity to which a collection is attached (`collections.owner_id`). |
| DAO | `glossary/technical.md` | Data access object abstracting tenanted Hibernate persistence. |

