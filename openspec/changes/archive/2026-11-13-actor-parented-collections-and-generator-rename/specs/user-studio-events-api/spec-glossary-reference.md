# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Events | `glossary/business.md` | Gathering entity created under `/api/studios/{studio-id}/v1/events`. |
| Entity | `glossary/business.md` | Polymorphic root record persisting `njall_users.entities.generator`. |
| Studio | `glossary/business.md` | Tenanted organization resolved via `{studio-id}` UUID or alias. |
| Alias | `glossary/business.md` | Human-readable studio identifier resolved via `StudioLookupCache`. |
| Actor | `glossary/business.md` | Federated identity validated as the event's structural generator. |
| Tenant | `glossary/business.md` | Studio Row-Level Security partition boundary. |
| Locations | `glossary/business.md` | Physical venue entity optionally associated with an event. |
| generator | `glossary/business.md` | Structural actor provenance stored in `njall_users.entities.generator`. |

