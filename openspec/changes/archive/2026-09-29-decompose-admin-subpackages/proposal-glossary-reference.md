# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal name of the application whose architectural principles govern package decomposition. |
| Actor | `glossary/technical.md` | Pekko Typed actor components relocated to administrative subpackages. |
| API plane | `glossary/technical.md` | The HTTP routing and API layer undergoing subpackage restructuring. |
| Admin verticle | `glossary/technical.md` | Administrative slice of the application being organized into subpackages. |
| Module | `glossary/technical.md` | Gradle modules (such as `:api`) and Guice dependency injection modules installed in `AdminModule`. |
| Library module | `glossary/technical.md` | The `:api` subproject which provides HTTP routing without a standalone application entry point. |
| Server | `glossary/business.md` | Administrative management of server host instances and runtime servers. |
| Studio | `glossary/business.md` | Tenant administration routes and actors partitioned into the `studios` subpackage. |
| User | `glossary/business.md` | Administrative user management routes and actors partitioned into the `users` subpackage. |
| Role | `glossary/business.md` | Permission and role administration partitioned into `roles` and `studioroles` subpackages. |
