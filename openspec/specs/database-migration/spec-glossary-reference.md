# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Studio | `glossary/business.md` | Organization whose tenant records isolate entity rows. |
| Tenant | `glossary/business.md` | Multi-tenant partition boundary enforced by composite keys and Row-Level Security. |
| Actor | `glossary/business.md` | Federated identity entity referenced by `njall_users.entities.generator`. |
| Collections | `glossary/business.md` | Subtype table retaining semantic `owner_id` attachment. |
| owner | `glossary/business.md` | Semantic parent entity referenced by `njall_users.collections.owner_id`. |
| System | `glossary/business.md` | Ruleset entity persisted in `njall_users.systems`. |
| Campaigns | `glossary/business.md` | Chronicle entity persisted in `njall_users.campaigns`. |
| Games | `glossary/business.md` | 2nd-level CTI event subtype persisted in `njall_users.games`. |
