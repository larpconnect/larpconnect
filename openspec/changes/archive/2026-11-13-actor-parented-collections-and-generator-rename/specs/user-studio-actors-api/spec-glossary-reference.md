# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Studio | `glossary/business.md` | Tenanted organization owning studio actors and subordinate collections. |
| Actor | `glossary/business.md` | Federated identity entity under `/api/studios/{studio-id}/v1/actors`. |
| Entity | `glossary/business.md` | Root federatable record storing `njall_users.entities.generator`. |
| Alias | `glossary/business.md` | Human-readable studio identifier resolved via `StudioLookupCache`. |
| Tenant | `glossary/business.md` | Isolated studio boundary enforced via PostgreSQL Row-Level Security. |
| generator | `glossary/business.md` | Immutable structural actor provenance stored in `entities.generator`. |
| Collections | `glossary/business.md` | Ordered entity containers including provisioned `inbox`, `outbox`, and subordinate actor collections. |

