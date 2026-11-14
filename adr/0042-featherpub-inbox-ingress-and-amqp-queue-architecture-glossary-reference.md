# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| Studio | `glossary/business.md` | Tenanted organization hosting Actors and receiving FeatherPub inbox deliveries. |
| Actor | `glossary/business.md` | Federated studio identity resolved by UUID or `preferredUsername`. |
| FeatherPub | `glossary/technical.md` | Federated protocol defining Activity envelopes and inbox ingress rules. |
| Activity | `glossary/business.md` | Discriminated 10-verb FeatherPub message envelope queued over AMQP 1.0. |
| Library module | `glossary/technical.md` | Non-executable `:queue` Gradle module encapsulating AMQP 1.0 client infrastructure. |
| Server | `glossary/technical.md` | Host runtime coordinating Pekko HTTP, Hibernate sessions, and AMQP 1.0 lifecycle. |
| generator | `glossary/business.md` | Structural parent Actor column (`njall_users.entities.generator`) walked for recipient routing. |
| Entity | `glossary/business.md` | Root CTI domain object mutated asynchronously by future queue consumers. |
