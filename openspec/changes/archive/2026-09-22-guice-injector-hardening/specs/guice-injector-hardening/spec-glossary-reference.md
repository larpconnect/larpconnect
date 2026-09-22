# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | The internal system name whose architectural constructor invariants are enforced. |
| Application module | `glossary/technical.md` | The `:server` module configuring the root injector and composing the graph. |
| Data plane | `glossary/technical.md` | Data components whose constructors must declare package-private `@Inject`. |
| API plane | `glossary/technical.md` | Administrative routing layer whose sub-routes must be explicitly declared in modules. |
