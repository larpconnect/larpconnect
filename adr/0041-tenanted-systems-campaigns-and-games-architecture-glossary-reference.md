# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| User plane | `glossary/technical.md` | Horizontal slice serving studio-facing HTTP routes and Pekko actors. |
| Studio | `glossary/business.md` | Tenanted organization hosting systems, campaigns, events, and games. |
| System | `glossary/business.md` | Ruleset entity managed at `/api/studios/{studio-id}/v1/systems`. |
| GameSystem | `glossary/technical.md` | Java implementation prefix representing `System` entities without colliding with `java.lang.System`. |
| Campaigns | `glossary/business.md` | Series of events or games managed at `/api/studios/{studio-id}/v1/campaigns`. |
| Games | `glossary/business.md` | 2nd-level CTI event subtype managed at `/api/studios/{studio-id}/v1/games`. |
| Events | `glossary/business.md` | Scheduled gathering entity classified by `event_type` at `/api/studios/{studio-id}/v1/events`. |
| Entity | `glossary/business.md` | Root CTI object in `njall_users.entities`. |
| FeatherPub | `glossary/technical.md` | Federated protocol motivating single-canonical-URI resource references. |
| Collections | `glossary/business.md` | Ordered entity container hydrating active item display names. |
| DAO | `glossary/technical.md` | Data access interfaces and Hibernate implementations in `:data`. |
