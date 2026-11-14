# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| DAO | `glossary/technical.md` | Base and tenanted data access interfaces in `com.larpconnect.njall.data.dao.*`. |
| GameSystem | `glossary/technical.md` | Java domain record (`GameSystem`) and persistence components (`GameSystemDAO`, `GameSystemEntity`) for `System`. |
| System | `glossary/business.md` | Ruleset entity with `entity_type = 'System'`. |
| Campaigns | `glossary/business.md` | Chronicle entity with `entity_type = 'Campaign'`. |
| Games | `glossary/business.md` | 2nd-level CTI event subtype with `entity_type = 'Game'` and `event_type = 'GAME'`. |
| Collections | `glossary/business.md` | Ordered entity container hydrating display names via `EXPANDED_ITEMS_QUERY`. |
| Entity | `glossary/business.md` | Root CTI record in `njall_users.entities`. |
| Actor | `glossary/business.md` | Entity generator validated by `ActorGeneratorValidator`. |
| generator | `glossary/business.md` | Responsible Actor UUID stored on `EntityBaseEntity`. |
| Studio | `glossary/business.md` | Tenanted organization owning studio entities. |
| Tenant | `glossary/business.md` | Isolated studio boundary enforced via `app.tenant_id`. |
