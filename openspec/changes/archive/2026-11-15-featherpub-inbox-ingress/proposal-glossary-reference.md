# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application. |
| FeatherPub | `glossary/technical.md` | Federated protocol defining Activity envelopes and inbox delivery semantics. |
| Activity | `glossary/business.md` | Discriminated 10-verb FeatherPub message envelope delivered to an Actor inbox. |
| Actor | `glossary/business.md` | Federated studio identity receiving and routing inbox Activity messages. |
| Studio | `glossary/business.md` | Tenanted organization hosting Actors and receiving federated inbox traffic. |
| generator | `glossary/business.md` | Structural parent Actor recorded in `njall_users.entities.generator` used for pre-queue inbox routing. |
| Server | `glossary/technical.md` | Host runtime managing HTTP routes, Pekko actors, database sessions, and AMQP connections. |
| Library module | `glossary/technical.md` | Non-executable Gradle module (`:queue`, `:data`, `:api`) providing capabilities to `:server`. |
| Module | `glossary/technical.md` | Gradle compilation unit or Guice dependency injection configuration class. |
| User plane | `glossary/technical.md` | Horizontal slice serving tenanted studio and Actor HTTP endpoints. |
| Agent | `glossary/technical.md` | Java DAO, Pekko actor, and route classes representing domain Actor entities. |
| DTO | `glossary/technical.md` | Immutable Jackson data transfer records modeling Activity and batch payloads. |
