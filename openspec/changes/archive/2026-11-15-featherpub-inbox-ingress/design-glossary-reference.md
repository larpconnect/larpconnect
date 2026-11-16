# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| Studio | `glossary/business.md` | Tenanted organization hosting Actors and receiving FeatherPub inbox deliveries. |
| FeatherPub | `glossary/technical.md` | Federated protocol defining Activity envelopes, Content-Digest, and inbox semantics. |
| Library module | `glossary/technical.md` | Non-executable `:queue`, `:data`, and `:api` Gradle modules composed in `:server`. |
| Actor | `glossary/business.md` | Federated studio identity and Pekko Typed concurrency actor processing inbox commands. |
| Activity | `glossary/business.md` | Union-free 10-verb FeatherPub message envelope validated, deduplicated, and queued. |
| generator | `glossary/business.md` | Structural parent Actor column (`njall_users.entities.generator`) walked for pre-queue routing. |
| Server | `glossary/technical.md` | Host runtime coordinating Pekko HTTP, Hibernate sessions, and AMQP 1.0 lifecycle. |
| Entity | `glossary/business.md` | Federated CTI domain object mutated by future downstream queue consumers. |
| Collections | `glossary/business.md` | Ordered entity containers owned by Actors (`inbox`, `outbox`, custom collections). |
| DTO | `glossary/technical.md` | Immutable Jackson records modeling Activity and batch request/response payloads. |
