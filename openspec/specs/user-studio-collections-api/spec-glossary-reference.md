# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Collections | `glossary/business.md` | Ordered entity container exposed under `/api/studios/{studio-id}/v1/actors/{actor-id}/collections`. |
| Entity | `glossary/business.md` | Common Table Inheritance root object representing federatable resources. |
| Studio | `glossary/business.md` | Tenanted gaming organization hosted on Njall. |
| Tenant | `glossary/business.md` | Studio tenancy boundary enforced by Row-Level Security. |
| Alias | `glossary/business.md` | Human-readable unique studio identifier. |
| Actor | `glossary/business.md` | Federated identity entity establishing collection URL parentage and provenance. |
| generator | `glossary/business.md` | Actor provenance recorded in `njall_users.entities.generator` and bound to `{actor-id}`. |
| owner | `glossary/business.md` | Semantic entity attachment target recorded in `njall_users.collections.owner_id`. |
