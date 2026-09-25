# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Server | `glossary/technical.md` | Refers to the HTTP hosting component serving the `/api/admin/v1/health` route. |
| Admin verticle | `glossary/technical.md` | Refers to administrative runtime operations including health checking. |
| Actor | `glossary/technical.md` | Refers to `HealthCheckActor` coordinating probe execution in `:api`. |
| Data plane | `glossary/technical.md` | Refers to the persistence subsystem containing `AdminDatabaseHealthCheck`. |
| API plane | `glossary/technical.md` | Refers to the HTTP routing and Pekko actor health check components. |
| Library module | `glossary/technical.md` | Refers to the `:data` Gradle module where the health check is implemented. |
| Module | `glossary/technical.md` | Refers to Guice modules `DataHealthModule` and `DataModule`. |
| Njall | `glossary/technical.md` | Refers to the application runtime environment. |
