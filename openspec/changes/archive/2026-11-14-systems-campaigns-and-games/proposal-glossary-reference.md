# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| Studio | `glossary/business.md` | Tenanted organization hosting systems, campaigns, events, and games. |
| Tenant | `glossary/business.md` | Isolated studio data boundary enforced via PostgreSQL Row-Level Security. |
| User plane | `glossary/technical.md` | Horizontal slice serving studio-facing HTTP routes and Pekko actors. |
| System | `glossary/business.md` | Roleplaying or meeting ruleset entity managed under `/v1/systems`. |
| Campaigns | `glossary/business.md` | Series of events or games organized under a studio at `/v1/campaigns`. |
| Games | `glossary/business.md` | Specialized event subtype associated with a system and optional campaign at `/v1/games`. |
| Events | `glossary/business.md` | Scheduled gathering entity classified by `event_type` at `/v1/events`. |
| Entity | `glossary/business.md` | Root Common Table Inheritance object in `njall_users.entities`. |
| FeatherPub | `glossary/technical.md` | Federated protocol motivating single-canonical-URI resource references. |
| GameSystem | `glossary/technical.md` | Java implementation prefix representing `System` entities without colliding with `java.lang.System`. |
| DAO | `glossary/technical.md` | Data access object interface and Hibernate implementation in `:data`. |
| Collections | `glossary/business.md` | Ordered entity container whose expanded view hydrates system, campaign, and game display names. |
| Actor | `glossary/business.md` | Federated studio identity and Pekko actor implementation handling commands. |
| generator | `glossary/business.md` | Responsible Actor recorded in `njall_users.entities.generator`. |
| DTO | `glossary/technical.md` | Immutable Jackson data transfer record used on HTTP endpoints. |
