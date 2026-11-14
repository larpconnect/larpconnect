# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Studio | `glossary/business.md` | Tenanted organization scoping Actor resolution and inbox deduplication. |
| Tenant | `glossary/business.md` | Row-Level Security boundary configured via `app.tenant_id`. |
| Actor | `glossary/business.md` | Federated studio identity stored in `njall_users.actors` and `njall_users.entities`. |
| Agent | `glossary/technical.md` | Java domain record and DAO (`AgentDAO`) representing Actor entities. |
| generator | `glossary/business.md` | Parent Actor column (`njall_users.entities.generator`) walked recursively for routing. |
| GameSystem | `glossary/technical.md` | Java domain record representing studio System entities in `data.domain.studios`. |
