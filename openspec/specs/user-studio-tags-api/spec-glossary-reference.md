# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Studio | `glossary/business.md` | Tenant organization boundary under which hashtags are defined and managed. |
| Hashtag | `glossary/business.md` | First-class searchable naming entity and Link subtype. |
| Link | `glossary/business.md` | Common Table Inheritance base subtype for external or federated references. |
| Entity | `glossary/business.md` | Root polymorphic object tracking tenant generator provenance, audit timestamps, and soft deletion. |
| Tenant | `glossary/business.md` | Database isolation boundary enforced via PostgreSQL Row-Level Security. |
| Actor | `glossary/business.md` | Federated identity validated as the hashtag's structural generator. |
| Alias | `glossary/business.md` | Natural human-readable identifier for a studio used in path segments. |
| ID | `glossary/business.md` | Unique UUID identifier for an entity or hashtag. |
| generator | `glossary/business.md` | Structural actor provenance stored in `njall_users.entities.generator`. |
