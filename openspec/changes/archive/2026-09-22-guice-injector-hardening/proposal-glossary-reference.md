# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal name of the application whose root dependency injection is being hardened. |
| Server | `glossary/business.md` | The host backend application configuring the root injector. |
| Application module | `glossary/technical.md` | The executable `:server` module responsible for full dependency graph assembly. |
| Library module | `glossary/technical.md` | Subordinate modules (`:api`, `:data`, `:common`) providing components composed by the server. |
| API plane | `glossary/technical.md` | Subsystem layer containing route definitions requiring explicit bindings. |
| Data plane | `glossary/technical.md` | Subsystem persistence components requiring explicit constructor annotations. |
