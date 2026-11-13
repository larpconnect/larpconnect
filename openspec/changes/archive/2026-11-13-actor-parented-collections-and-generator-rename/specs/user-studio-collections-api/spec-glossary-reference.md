# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Collections | `glossary/business.md` | Ordered entity container exposed under `/api/studios/{studio-id}/v1/actors/{actor-id}/collections`. |
| Entity | `glossary/business.md` | Root federatable resource stored in `njall_users.entities`. |
| Studio | `glossary/business.md` | Tenanted organization resolved via `{studio-id}` UUID or alias. |
| Alias | `glossary/business.md` | Human-readable studio identifier resolved by `StudioLookupCache`. |
| Actor | `glossary/business.md` | Federated identity entity establishing collection URL parentage and provenance. |
| Tenant | `glossary/business.md` | Isolated studio boundary enforced via PostgreSQL Row-Level Security. |
| generator | `glossary/business.md` | Actor provenance recorded in `njall_users.entities.generator` and bound to `{actor-id}`. |
| owner | `glossary/business.md` | Semantic entity attachment target recorded in `njall_users.collections.owner_id`. |

