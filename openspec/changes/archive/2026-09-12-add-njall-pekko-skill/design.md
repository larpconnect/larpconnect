## Context

See `proposal.md` for motivation. Njall is transitioning completely from Vert.x to Apache Pekko (version 2.0-M4+, Scala 3) in a Java 25 LTS environment. The system employs Google Guice for dependency injection, strict Directed Acyclic Graph (DAG) package topologies, and dual-layer testing (Layer 1 unit tests with high branch coverage gates, and Layer 2 integration tests).

## Goals / Non-Goals

**Goals:**
- Author a comprehensive, prescriptive `.agents/skills/njall-pekko/SKILL.md` document for agent pair programming.
- Establish compile-time exhaustive protocol patterns using Java 25 sealed interfaces and records.
- Standardize the Guice Behavior Factory pattern to bridge Guice DI and Pekko Typed actor lifecycles without violating IOSP-Lite.
- Define explicit testing boundaries between synchronous `BehaviorTestKit` (Layer 1 unit tests) and asynchronous `ActorTestKit` (Layer 2 integration tests).

**Non-Goals:**
- Implementing production actor modules or routes in this change (handled in subsequent functional changes).
- Configuring AMQP / RabbitMQ streaming actors at this time (explicitly deferred per user direction).

## Decisions

### Decision 1: Protocol Modeling via Java 25 Sealed Interfaces and Records
- **Rationale**: Sealed interfaces define closed protocol algebraic data types (ADTs), enabling the Java 25 compiler to enforce exhaustiveness in switch expressions. Records provide immutable, concise message bodies and prevent accidental mutation during message passing.
- **Alternatives Considered**: 
  - *Traditional class hierarchies with abstract base classes*: Verbose, requires manual equals/hashCode, prone to mutable field leakage.
  - *Generic Object or untyped messages*: Discards compile-time type checking, reintroducing runtime `ClassCastException` failures.

### Decision 2: Stateless Behaviors with Immutable State Recursion
- **Rationale**: Stateless actors (`Behaviors.receiveMessage`) eliminate concurrency bugs, deadlocks, and internal race conditions. When an actor must accumulate state, passing an immutable state record to a recursive static method (`active(State state)`) keeps state purely functional.
- **Alternatives Considered**:
  - *Mutable instance variables inside `AbstractBehavior` classes*: Harder to reason about, prone to synchronization mistakes, and complicates testing.

### Decision 3: Guice Behavior Factory Pattern for Dependency Injection
- **Rationale**: Actors have lifecycles managed by the Pekko ActorSystem and supervision hierarchy, whereas Guice manages singleton and request-scoped services. Direct Guice injection into actors (`@Inject` on an actor class) violates Pekko's creation model. A Guice-managed `*BehaviorFactory` interface allows external dependencies (e.g. repositories, validators) to be injected into the factory, which cleanly produces `Behavior<Command>` instances for `context.spawn(...)`.
- **Alternatives Considered**:
  - *Pekko Guice Extension (`GuiceActorRefProvider`)*: Tied to legacy/classic Pekko and introduces heavy reflection.
  - *Direct instantiation of dependencies within actors*: Violates inversion of control and breaks testability.

### Decision 4: Tiered Testing Standards (`BehaviorTestKit` vs. `ActorTestKit`)
- **Rationale**: Layer 1 unit tests in `src/test` require 100% determinism and fast execution to satisfy Jacoco coverage gates without flakiness. `BehaviorTestKit` executes synchronously on the caller thread without thread pools or timers. Multi-actor coordination and asynchronous timing belong in `:integration` using `ActorTestKit` and `TestProbe`.
- **Alternatives Considered**:
  - *Using `ActorTestKit` for all unit tests*: Spawns heavy actor systems and threads for simple unit tests, adding latency and occasional timing race conditions.

## Risks / Trade-offs

- **[Risk]** Developers or agents might attempt to use `ActorTestKit` in simple unit tests, causing slow builds.
  - **Mitigation**: The skill explicitly marks `BehaviorTestKit` as mandatory for `src/test` unit tests and restricts `ActorTestKit` to integration workflows.
- **[Risk]** Leaking actor state through public getters or non-actor references.
  - **Mitigation**: The skill mandates exposing only `ActorRef<T>` references and enforces fire-and-forget message passing.
