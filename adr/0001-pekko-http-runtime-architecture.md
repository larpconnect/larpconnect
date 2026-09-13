# 0001: Pekko HTTP Runtime Architecture and Multimodule Layout

## Status

Accepted

## Date

2026-09-12

## Context

Project Njall requires a scalable, reactive application runtime. The project has committed to Apache Pekko Typed (2.0.0-M4+, Scala 3) and Google Guice (7.0.0) as the runtime foundation on Java 25 LTS. The project layout enforces a strict Directed Acyclic Graph (DAG) with package-to-module parity under `com.larpconnect.njall.*`.

## Considered Options

- **Option 1: Apache Pekko Typed with Pekko HTTP & Google Guice** (Selected)
- **Option 2: Eclipse Vert.x with Guice** (Deprecated per project decision to unify on Pekko)
- **Option 3: Spring Boot or Quarkus** (Rejected; conflicts with Pekko actor-based architecture and lightweight footprint)

## Decision

We will use Apache Pekko Typed and Pekko HTTP configured via Google Guice across the active multimodule pipeline:
1. `:common`: Typesafe Config (`reference.conf`) and strongly-typed `ServerConfig` record.
2. `:api`: Spec-first OpenAPI contract (`openapi.yaml`) and Pekko HTTP `RootRoute` definitions.
3. `:server`: Network socket binding (`HttpServerService`), Pekko `CoordinatedShutdown` lifecycle management, and `ServerApp` main entry point.
4. `:test` & `:integration`: Dual-layer testing with `RouteTest` unit tests and Cucumber integration tests with dynamic port allocation.

## Consequences

- **Positive**: Clean module DAG, compile-time typed actors, asynchronous non-blocking HTTP processing, and deterministic phase-driven coordinated shutdown.
- **Negative**: Requires careful separation between Guice DI lifecycles and Pekko actor trees using behavior factories.
- **Follow-up**: Implement domain endpoints, persistence, and AMQP event queuing in subsequent changes.
