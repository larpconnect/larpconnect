# Glossary Reference

| Term | Source Glossary | Context |
| --- | --- | --- |
| Module | `glossary/technical.md` | The `:parent` Gradle platform module and dependent modules verified sequentially. |
| Library module | `glossary/technical.md` | The `:test` and `:api` non-executable Gradle modules verified in dependency order. |
| Application module | `glossary/technical.md` | The executable `:server` Gradle module verified after `:api`. |
| Server | `glossary/technical.md` | The HTTP server runtime component tested in `:server`. |

