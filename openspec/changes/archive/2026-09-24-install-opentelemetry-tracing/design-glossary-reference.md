# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Njall | `glossary/technical.md` | Internal code name of the application implementing distributed tracing. |
| Server | `glossary/technical.md` | The runtime server hosting the Pekko HTTP stack and Logback configuration. |
| Actor | `glossary/technical.md` | Pekko Typed actors receiving commands with TraceContext and preserving MDC in logs. |
| API plane | `glossary/technical.md` | The HTTP routing layer intercepting API requests to generate root spans and headers. |
| Module | `glossary/technical.md` | The Gradle modules (:common, :api, :server) and Guice modules composing tracing. |
