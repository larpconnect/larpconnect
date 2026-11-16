# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| User plane | `glossary/technical.md` | Horizontal slice hosting studio Actor inbox HTTP routes and Pekko actors. |
| Studio | `glossary/business.md` | Tenanted organization resolved via `StudioLookupCache` on inbox requests. |
| Alias | `glossary/business.md` | Human-readable studio slug accepted in `{studio-id}` and local recipient URIs. |
| Tenant | `glossary/business.md` | Isolated studio boundary scoping Actor resolution and deduplication rows. |
| Actor | `glossary/business.md` | Federated studio identity receiving and routing Activity payloads. |
| Client | `glossary/business.md` | Local or remote caller submitting FeatherPub Activities to an Actor inbox. |
| FeatherPub | `glossary/technical.md` | Federated protocol governing Activity schemas, Content-Digest, and inbox semantics. |
| Activity | `glossary/business.md` | Discriminated 10-verb FeatherPub action envelope validated and queued at ingress. |
| generator | `glossary/business.md` | Structural parent Actor column (`njall_users.entities.generator`) walked for recipient routing. |
| Server | `glossary/technical.md` | Application runtime across whose restarts PostgreSQL deduplication persists. |
