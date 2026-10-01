# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Admin User | `glossary/business.md` | Testing administrative user accounts provisioned in `njall_admin.admin_users`. |
| Role | `glossary/business.md` | Administrative and default studio permissions assigned to users. |
| Studio | `glossary/business.md` | Distinct tenant accounts provisioned in the test database. |
| Tenant | `glossary/business.md` | Isolated studio environments partitioned by database Row-Level Security. |
| Alias | `glossary/business.md` | Human-readable studio identifiers (`valkyrie_example`, `ironwood_fake`). |
| ID | `glossary/business.md` | Fixed deterministic UUIDs assigned to test fixtures. |
| Entity | `glossary/business.md` | Common Table Inheritance root records in `njall_users.entities`. |
| Link | `glossary/business.md` | External URI entities associated with studios (Discord, websites). |
| Locations | `glossary/business.md` | Physical game sites associated with studios ("Camp Example"). |
| Address | `glossary/business.md` | Geospatial and postal address records associated with locations. |
| Server | `glossary/technical.md` | Application runtime service launched after test data seeding completes. |
| Migrate | `glossary/technical.md` | Ephemeral migration service that must complete before seed execution. |
