# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application implementing distributed tracing. |
| Actor | `glossary/technical.md` | Pekko Typed actors receiving commands with trace context and logging via MDC. |
| API plane | `glossary/technical.md` | The HTTP routing layer intercepting API requests and attaching tracing headers. |
| Server | `glossary/technical.md` | The runtime hosting the Pekko HTTP server and Logback configuration. |
| Module | `glossary/technical.md` | The Gradle modules (:common, :api, :server) and Guice modules composing tracing. |
