# LarpConnect Architecture Guide

Welcome to the LarpConnect architecture guide. This document explains the foundational reasons
behind how the system is organized, the tools chosen, and the specific conventions established to
support a multi-module federated application heavily developed with the assistance of Artificial
Intelligence.

## Why Multi-Module?

LarpConnect uses a multi-module Gradle build. This separation enforces strict architectural
boundaries between different parts of the system.

- **Isolation of Concerns:** By splitting the project into modules like `:api`, `:common`,
  `:server`, and `:init`, we ensure that low-level domain logic (like protocol definitions)
  remains entirely unaware of high-level runtime mechanics (like HTTP routing).
- **Compile-Time Enforcement:** Instead of relying on discipline to avoid cyclic dependencies or
  leaking abstractions, the build system enforces these rules. If a class in `:common` tries to
  import a class from `:server`, it simply will not compile.

## Why Vert.x and Guice?

LarpConnect is built on two primary frameworks: Vert.x and Guice. They serve very different but
complementary purposes.

### The Vert.x Event Loop

Vert.x provides a reactive, non-blocking event-driven architecture. The core concept here is the
`Verticle`, which acts much like an actor in the Actor Model.

- **Concurrency without Locks:** Vert.x uses an event loop. Code within a standard verticle is
  guaranteed to run on the same thread, eliminating the need for complex locking mechanisms around
  local state.
- **Message Passing:** Verticles communicate asynchronously by passing messages over an EventBus.
  This allows different components of the application to be entirely decoupled. A verticle does not
  need to know *who* handles a request, only *where* to send it.

### Guice Dependency Injection

While Vert.x is excellent for runtime concurrency, it does not provide strong tools for wiring the
application together. This is where Guice comes in.

- **Deterministic Initialization:** Guice is used strictly during the startup phase of the
  application to wire together all the dependencies and configurations before any Vert.x components
  begin accepting traffic.
- **Type Safety:** We avoid relying on string-based lookups or global state. Guice
  allows us to safely inject configuration values exactly where they are needed.

The integration of these two frameworks requires a rigid initialization process, typically managed
in the `:init` module, to prevent race conditions where a verticle might attempt to start before its
dependencies are fully resolved.

## Context Engineering

Because LarpConnect is designed to be co-developed with AI agents, the codebase includes specific
metadata intended to guide these agents.

### The `@AiContract` Annotation

In a highly asynchronous, event-driven system, the flow of execution can be difficult for an AI (or
a human) to trace. To solve this, we use the `@AiContract` annotation.

This annotation serves as an _ersatz_ form of "Design by Contract." It explicitly defines the
preconditions,
postconditions, and invariants of a method or component using mathematical or logical notation.
Rather than explaining *how* the code works, it guarantees *what must be true* before and after
it executes.

For example, a contract might state:
`ensure = "$res \\ge 0"`

This tells the AI that the method is guaranteed to return a positive value or zero. We also make
use of other standard annotations like `@Nullable`, `@Immutable`, and `@ThreadSafe` to further
clarify expectations and invariants for the agents.

### Guice Guidance Annotations

Guice bindings can be notoriously difficult to statically analyze. To assist agents in navigating
the dependency graph, we use several custom annotations:

- `@DefaultImplementation`: Indicates the expected concrete class for an interface.
- `@BuildWith`: Directs an agent to the Guice module required to instantiate a class with a
  private or package-private constructor.
- `@InstallInstead`: Resolves conflicts when multiple modules might provide overlapping bindings.

These annotations do not affect the runtime behavior of the application; they exist solely to build
a richer context for automated analysis and development.

## Conclusion

The architecture of LarpConnect is deliberately strict. The combination of a multi-module
structure, reactive message passing, rigorous dependency injection, and formal contract
annotations is designed to produce a system that is robust, testable, and highly legible to both
human and machine developers.
