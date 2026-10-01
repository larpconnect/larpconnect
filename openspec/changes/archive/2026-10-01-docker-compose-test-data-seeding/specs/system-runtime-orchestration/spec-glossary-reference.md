# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Admin User | `glossary/business.md` | Test administrative user records inserted into `njall_admin.admin_users`. |
| Role | `glossary/business.md` | Administrative roles and the `njall_admin` database role executing seeds. |
| Studio | `glossary/business.md` | The two test studio tenants (`valkyrie_example`, `ironwood_fake`). |
| Tenant | `glossary/business.md` | Isolated studio partitions referenced by seed fixtures. |
| Alias | `glossary/business.md` | Studio lookup aliases (`valkyrie_example`, `ironwood_fake`). |
| ID | `glossary/business.md` | Deterministic UUIDs for primary keys to ensure insert idempotency. |
| Entity | `glossary/business.md` | Common Table Inheritance root records in `njall_users.entities`. |
| Link | `glossary/business.md` | URI references for Discord, documentation, and websites. |
| Locations | `glossary/business.md` | Physical game sites ("Camp Example", "Blackthorn Fake Manor Grounds"). |
| Address | `glossary/business.md` | Geospatial and postal address records associated with locations. |
| Server | `glossary/technical.md` | HTTP runtime service launched after the seed container finishes. |
| Migrate | `glossary/technical.md` | Schema migration container that precedes the seed container. |
