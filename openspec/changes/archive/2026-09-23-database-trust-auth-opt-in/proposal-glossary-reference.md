# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal name of the application whose database authentication policy is being updated. |
| Server | `glossary/business.md` | The host application runtime orchestrating backend database connections and migrations. |
| Data plane | `glossary/technical.md` | The PostgreSQL persistence layer receiving role authentication and connection policies. |
| Application module | `glossary/technical.md` | The executable `:server` module exposing the `migrate` CLI subcommand. |
| Library module | `glossary/technical.md` | The `:common` and `:data` reusable libraries containing configuration records and factories. |
