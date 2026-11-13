# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Data plane | `glossary/technical.md` | Tenant-facing persistence layer housing DAOs and Hibernate entities. |
| generator | `glossary/business.md` | Actor provenance UUID stored in `njall_users.entities.generator`. |
| Actor | `glossary/business.md` | Federated identity subtype validated by `ActorGeneratorValidator`. |
| Entity | `glossary/business.md` | Polymorphic root record in `njall_users.entities`. |
| Studio | `glossary/business.md` | Tenanted gaming organization owning isolated data. |
| Tenant | `glossary/business.md` | Row-Level Security partition boundary identified by `tenant_id`. |
| Collections | `glossary/business.md` | Ordered entity container scoped to its generating actor. |
| DAO | `glossary/technical.md` | Data access object interface and Hibernate implementation. |
| Individual | `glossary/business.md` | Non-actor person entity rejected as a structural generator. |

