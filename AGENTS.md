# LarpConnect (Njall) Agent Instructions

This document establishes the binding execution constraints, architectural invariants, and development workflows for all AI agents operating on the LarpConnect repository. All code modifications, module creations, and testing strategies must conform to these definitions.

---

## 1. Agent Workspace and State Trackers

Agents must maintain operational state, skill definitions, and execution logs within the `.agents` root directory.

* **`.agents/skills/`**: Storage for specific functional capabilities. Every capability must occupy a distinct sub-folder with a `SKILL.md` file establishing its operational context (e.g., `.agents/skills/<skill name>/SKILL.md`).
* **`.agents/logs/`**: Structured markdown execution logs where agents record step-by-step reasoning, static analysis outcomes, and architectural assessment findings during a task.

---

## 2. Lingua-Franca and API Contracts

* **JSON Definition**: The universal payload communication format across all application layers (API, Queue, and Internal State) is JSON. Other formats may be supported, but they should always include JSON as an option and be able to be specified in JSON.
* **Spec-First Contract Definition**: No feature implementation may begin without establishing or updating the OpenAPI specification and behavioral cucumber tests. The OpenAPI spec dictates the exact structural shape of payload objects.

---

## 3. Dependency Management and Library Ecosystem

* **Version Upgradability**: Always declare and utilize the most recent stable versions for all third-party libraries. Centralize versions inside the `buildSrc` configuration layer using a registry. 
* **Ecosystem Preferences**: Maximize usage of standard utility and performance libraries. Do not reinvent core utilities; leverage the following preferentially:
    * **mug**: For clean, modern Java extensions and pattern matching.
    * **Google Guice**: For dependency injection / inversion of control.
    * **Caffeine**: For high-performance, in-memory caching strategies.
    * **Google Guava**: For advanced collections, graph structures, and primitives.
    * **Vert.x**: For event-driven architecture.
    * **Jackson**: For lightning-fast, highly accurate JSON parsing and serialization configurations.
    * **SLF4J** and **Logback**: For logging
    * **AssertJ**: Use this for assertions in preference to the built-in JUnit assertion framework.
    * **JUnit** (Jupiter): For unit testing
    * **Mockito**: For unit testing with mocks and stubs
    * **Testcontainers**: For integration testing against a live database.
    * **Apache Commons**: For utilities not covered by any of the above.

For our purposes here the ecosystem preferences are _ordered_, meaning that if a problem can be solved with a higher item, use it in preference to the lower item. So if, for example, a utility exists in both `mug` and `guava` then prefer the `mug` version. If one exists in both `vert.x` and `jackson`, then prefer the `vert.x` version. 

---

## 4. Code Architecture and Quality Constraints

Agents must ensure the codebase remains a clean, highly trackable Directed Acyclic Graph (DAG) using Java 25 LTS, Vert.x 5, Guice, and Hibernate.

### Structural Topology
* **Strict DAG Enforcements**: Circular dependencies between packages or Gradle modules are absolutely forbidden.
* **Package-to-Module Parity**: Exactly one top-level package is allowed per Gradle module.
* **Guice Configuration**: Every package layer must expose exactly one public Guice module. This module is exclusively responsible for running `install` on subpackage modules exactly one layer down. 
* **Application Composition**: Individual library modules must never install subpackage modules outside of test scopes. Full dependency compilation happens inside the application modules (e.g., `:server`).

### Code Quality Metrics
* **Class File Limit**: Maximum 500 Source Lines of Code (SLOC).
* **Function Limit**: Maximum 50 Source Lines of Code (SLOC).
* **Cyclomatic Complexity**: Strict maximum of 10 per method.
* **NPath Complexity**: Strict maximum of 200 per method.
* **Line Length**: Hard wrapping at 100 characters following Google Java Formatting standards. This is enforced by Spotless.

### Behavioral Isolation (IOSP-Lite)
* **Logic vs. Integration Separation**: A function may either call other class functions OR it may contain internal execution logic and external calls (e.g., repository interactions, external service hits). It may never do both.
* **Pure Factory Isolation**: If a function instantiates objects via the `new` keyword, object creation must be the *only* operation that function performs.

---

## 5. Dual-Layer Testing Strategy

No code modification is complete without hitting the project verification gates: a minimum of **85% line coverage** and **90% branch coverage** via JaCoCo, verified by Spotless, SpotBugs, ErrorProne, and Checkstyle.

### Layer 1: Unit Tests
* Must be colocated with the implementation module inside the `src/test` directory.
* Focus heavily on testing pure functions, isolated factories, mapping layers, and domain entities.

### Layer 2: Cucumber Integration Tests
* All integration and End-to-End (E2E) tests must live exclusively inside the `:integration` module.
* Integration tests must explicitly target the boundaries established by the asynchronous architecture using distinct Cucumber features:

1.  **API to Queue Boundary**: Assert that incoming HTTP/REST requests hitting the Vert.x API layer validate against the OpenAPI specification, serialize properly to JSON, and emit the expected message structures onto the AMQP 1.0 queue system.
2.  **Queue to Data Boundary**: Assert that processing an inbound AMQP 1.0 queue message through the server agents results in the appropriate state modifications. Database operations must be verified against a synthesized or mock database response layer to keep tests deterministic and isolated from external infrastructure flakiness.

### Versions

Always favor using the most recent versions of libraries and infrastructure that are available and in long-term-support.

This means:

* Java 25+
* PSQL 17+
* RabbitMQ 4.3+
* HornetQ 2.4.11.Final+
* Vert.x 5.1+
* Junit (Jupiter) 6.1+
