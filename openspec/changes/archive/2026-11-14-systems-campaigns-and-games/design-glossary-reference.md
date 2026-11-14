# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| User plane | `glossary/technical.md` | Horizontal slice serving studio-facing HTTP routes and Pekko actors. |
| Studios | `glossary/business.md` | Tenanted organizations hosting systems, campaigns, events, and games. |
| Events | `glossary/business.md` | Scheduled gathering entities persisted in `njall_users.events`. |
| Locations | `glossary/business.md` | Physical venue entities referenced by events and games. |
| Individual | `glossary/business.md` | Person entities hosted under a studio. |
| Collections | `glossary/business.md` | Ordered entity containers hydrating active item display names. |
| Hashtag | `glossary/business.md` | Case-insensitively indexed tag entities under a studio. |
| Link | `glossary/business.md` | External reference entities under a studio. |
| Actor | `glossary/business.md` | Federated studio actor entities and Pekko Typed actor components. |
| System | `glossary/business.md` | Ruleset entities managed at `/api/studios/{studio-id}/v1/systems`. |
| GameSystem | `glossary/technical.md` | Java implementation prefix (`GameSystem`, `GameSystemDAO`) avoiding `java.lang.System` collisions. |
| Campaigns | `glossary/business.md` | Multi-event chronicles managed at `/api/studios/{studio-id}/v1/campaigns`. |
| Games | `glossary/business.md` | 2nd-level CTI event subtype managed at `/api/studios/{studio-id}/v1/games`. |
| FeatherPub | `glossary/technical.md` | Federated protocol motivating single-canonical-URI resource references. |
| DAO | `glossary/technical.md` | Data access interfaces and Hibernate implementations in `:data`. |
