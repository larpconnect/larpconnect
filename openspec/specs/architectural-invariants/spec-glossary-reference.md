# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Application codebase and package namespace prefix `com.larpconnect.njall`. |
| Library module | `glossary/technical.md` | Gradle library module `:integration` hosting ArchUnit architecture tests. |
| Module | `glossary/technical.md` | Gradle library/application modules (`:data`, `:api`, `:integration`) and Guice binding modules configuring subpackages (`StudiosModule`, `DaoModule`, etc.). |
| User plane | `glossary/technical.md` | Studio-facing HTTP routes and Pekko actors under `com.larpconnect.njall.api.studios.*`. |
| Actor | `glossary/technical.md` | Apache Pekko typed actor factory and message protocol types. |
| DAO | `glossary/technical.md` | Data access interfaces and Hibernate implementations organized under `com.larpconnect.njall.data.dao.*`. |
