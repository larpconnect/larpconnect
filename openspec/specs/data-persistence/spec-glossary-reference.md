# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| DAO | `glossary/technical.md` | Data access object interfaces and Hibernate implementations. |
| Data plane | `glossary/technical.md` | Tenant-facing persistence layer housing DAOs, Hibernate entities, and health probes. |
| GameSystem | `glossary/technical.md` | Java domain record (`GameSystem`) and persistence components (`GameSystemDAO`, `GameSystemEntity`) for `System`. |
| Tenant | `glossary/business.md` | Row-Level Security partition boundary identified by `tenant_id`. |
| Studio | `glossary/business.md` | Tenanted gaming organization owning isolated data. |
| Actor | `glossary/business.md` | Federated identity subtype validated as entity provenance by `ActorGeneratorValidator`. |
| Entity | `glossary/business.md` | Polymorphic root record in `njall_users.entities`. |
| Collections | `glossary/business.md` | Ordered entity container scoped to its generating actor and hydrating display names via `EXPANDED_ITEMS_QUERY`. |
| Individual | `glossary/business.md` | Non-actor person entity rejected as a structural generator. |
| generator | `glossary/business.md` | Immutable structural actor provenance stored in `njall_users.entities.generator`. |
| System | `glossary/business.md` | Ruleset entity with `entity_type = 'System'`. |
| Campaigns | `glossary/business.md` | Chronicle entity with `entity_type = 'Campaign'`. |
| Games | `glossary/business.md` | 2nd-level CTI event subtype with `entity_type = 'Game'` and `event_type = 'GAME'`. |
