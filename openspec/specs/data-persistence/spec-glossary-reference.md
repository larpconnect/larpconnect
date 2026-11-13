# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| DAO | `glossary/technical.md` | Data access object interfaces and Hibernate implementations. |
| Data plane | `glossary/technical.md` | Tenant-facing persistence layer housing DAOs, Hibernate entities, and health probes. |
| Tenant | `glossary/business.md` | Row-Level Security partition boundary identified by `tenant_id`. |
| Studio | `glossary/business.md` | Tenanted gaming organization owning isolated data. |
| Actor | `glossary/business.md` | Federated identity subtype validated as entity provenance by `ActorGeneratorValidator`. |
| Entity | `glossary/business.md` | Polymorphic root record in `njall_users.entities`. |
| Collections | `glossary/business.md` | Ordered entity container scoped to its generating actor. |
| Individual | `glossary/business.md` | Non-actor person entity rejected as a structural generator. |
| generator | `glossary/business.md` | Immutable structural actor provenance stored in `njall_users.entities.generator`. |

