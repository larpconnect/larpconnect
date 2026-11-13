# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Actor | `glossary/business.md` | Entity capable of sending and receiving federated messages and parenting subordinate collections. |
| Agent | `glossary/technical.md` | Java implementation components representing Actor entities. |
| Studio | `glossary/business.md` | Tenanted gaming organization hosted on Njall. |
| Entity | `glossary/business.md` | Common Table Inheritance root object representing federatable resources. |
| Collections | `glossary/business.md` | Entity holding references to an ordered set of other entities, including inbox, outbox, and subordinate actor collections. |
| Tenant | `glossary/business.md` | Studio tenancy boundary enforced by Row-Level Security. |
| Alias | `glossary/business.md` | Plain text identifier resolving to internal studio tenant ID. |
| DAO | `glossary/technical.md` | Data access object abstracting entity persistence. |
| generator | `glossary/business.md` | Immutable structural actor provenance stored in `njall_users.entities.generator`. |
