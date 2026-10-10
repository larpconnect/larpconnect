# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Studio | `glossary/business.md` | Tenant organization boundary under which hashtags are defined and managed. |
| Hashtag | `glossary/business.md` | First-class searchable naming entity and Link subtype. |
| Link | `glossary/business.md` | Common Table Inheritance base subtype for external or federated references. |
| Entity | `glossary/business.md` | Root polymorphic object tracking tenant ownership, audit timestamps, and soft deletion. |
| Tenant | `glossary/business.md` | Database isolation boundary enforced via PostgreSQL Row-Level Security. |
| Actor | `glossary/technical.md` | Apache Pekko typed actor handling asynchronous command execution for hashtags. |
| Alias | `glossary/business.md` | Natural human-readable identifier for a studio used in path segments. |
| ID | `glossary/business.md` | Unique UUID identifier for an entity or hashtag. |
