# Modular Skill Registry

This document defines the specialized capabilities available to AI agents such as Jules. Agents must reference the corresponding file in `.agents/skills/` when a task falls within the defined "Activation Trigger."

## 1. Core Engineering Skills (Always Active)

| Skill Name | Activation Trigger | Source File |
| :--- | :--- | :--- |
| **Java 25 Mastery** | Any `.java` file modification | `.agents/skills/java.md` |
| **Common Gradle Skills** | All projects that require compiling the code   | `.agents/skills/common.md`  |
| **Testing Standards** | Any `src/test` modification or work in `:integration` | `.agents/skills/testing.md` |
| **Git Usage**         | Downloading or pushing a commit.            | `.agents/skills/git.md`     |
| **Context Documentation** | Writing a javadoc, encountering an annotation | `.agents/skills/context.md` |

## 2. Domain-Specific Skills (Contextual)

| Skill Name | Activation Trigger | Source File |
| :--- | :--- | :--- |
| **Guice Dependency** | Modifications to `*Module.java` | `.agents/skills/guice_di.md` |
| **Gradle Mastery**   | Modification of any kotlin (`*.kts`, `*.kt`), build, or setting file | `.agents/skills/build.md` |
| **Data Handling**      | Any modifications to JSON or CSV files or work inside of `:proto` | `.jules/skills/data.md` |
| **Markdown Documentation**  | Building documentation in markdown (`.md`)   | `.jules/skills/markdown.md` |
| **Breakglass Documentation** | If you are "stuck" on a problem             | `.jules/skills/breakglass.md` |
| **Vert.x Framework**         | Modification of any file using 'io.vertx' or implementing a Verticle | `.agents/skills/vertx.md` |

## 3. Delegation Protocol

If a task involves multiple domains, load all relevant skills in the following order:
1. Core Engineering
2. Domain-Specific
3. Conflict Resolution (per `AGENTS.md`)
