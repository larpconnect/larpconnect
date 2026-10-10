# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Studio | `glossary/business.md` | Tenant organization boundary under which hashtags are defined and managed. |
| Hashtag | `glossary/business.md` | First-class searchable naming entity and Link subtype. |
| Link | `glossary/business.md` | Common Table Inheritance base subtype for external or federated references. |
| Entity | `glossary/business.md` | Root polymorphic object tracking tenant ownership, audit timestamps, and soft deletion. |
| Tenant | `glossary/business.md` | Database isolation boundary enforced via PostgreSQL Row-Level Security. |
| Actor | `glossary/technical.md` | Apache Pekko typed actor handling asynchronous command execution for hashtags. |
| DAO | `glossary/technical.md` | Data access object interface abstracting Hibernate/JPA hashtag queries and mutations. |
| DTO | `glossary/technical.md` | Data transfer objects serializing and deserializing JSON HTTP requests and responses. |
| User plane | `glossary/technical.md` | Architectural application plane hosting tenanted studio runtime routes. |
| Njall | `glossary/business.md` | Internal platform codebase implementing the hashtag endpoints. |
