# agent-skills/njall-pekko Specification

## Purpose

Establishes architectural standards and operational guidelines for building, injecting, and testing Apache Pekko Typed actors in Project Njall.

## Requirements

### Requirement: Message Protocols Using Sealed Interfaces and Records
The skill SHALL mandate that all Pekko actor command and event protocols are modeled using Java sealed interfaces for the root protocol type and immutable Java records for individual message variants. Request messages requiring a response SHALL explicitly include a typed `ActorRef<Response> replyTo` field.

#### Scenario: Designing a request-response message protocol
- **WHEN** an actor protocol is created
- **THEN** the root command is defined as a sealed interface, all command variants are defined as immutable records, and requests requiring a reply explicitly declare `ActorRef<Response> replyTo`.

### Requirement: Stateless Actors by Default
The skill SHALL mandate that actor behaviors remain stateless by default using `Behaviors.receive` or `Behaviors.receiveMessage`. Mutable instance variables inside actor classes SHALL be strictly forbidden. Stateful actors SHALL manage state exclusively through functional parameter recursion across immutable state records.

#### Scenario: Implementing actor behavior
- **WHEN** an actor behavior is implemented
- **THEN** it does not maintain mutable instance variables and handles any state transitions via functional recursive behavior methods passing immutable state records.

### Requirement: Typed Actor References and Tell Pattern
The skill SHALL require all actor interactions to use Pekko Typed (`ActorRef<T>` and `Behavior<T>`) exclusively. Pekko Classic APIs (`UntypedActor`, `pekko.actor.ActorRef`, `getSender()`) SHALL be strictly prohibited. Asynchronous fire-and-forget message passing (`tell`) SHALL be preferred over blocking `ask` patterns.

#### Scenario: Sending messages between actors
- **WHEN** an actor sends a message to another actor
- **THEN** it sends the message using `.tell(...)` via a typed `ActorRef<T>` without invoking untyped APIs or blocking on futures.

### Requirement: Guice Integration via Behavior Factories
The skill SHALL define the Guice Behavior Factory pattern for injecting external dependencies into actor hierarchies. Actors SHALL NOT be instantiated directly by Guice. External services and repositories SHALL be injected into a Guice-managed `*BehaviorFactory` interface whose implementation creates the `Behavior<Command>` instance.

#### Scenario: Injecting external dependencies into actors
- **WHEN** an actor requires external dependencies such as repositories or clients
- **THEN** dependencies are injected via Guice into a dedicated `*BehaviorFactory` implementation, which supplies the configured `Behavior<Command>` to `context.spawn(...)`.

### Requirement: Dual-Layer Testing with BehaviorTestKit and ActorTestKit
The skill SHALL mandate the use of `BehaviorTestKit` for deterministic, synchronous Layer 1 unit tests located in `src/test`, and `ActorTestKit` with `TestProbe` for Layer 2 asynchronous integration tests. All assertions SHALL use AssertJ rather than JUnit assertions.

#### Scenario: Unit testing an actor behavior
- **WHEN** writing a unit test for an isolated behavior in `src/test`
- **THEN** it executes synchronously using `BehaviorTestKit` and `TestInbox`, asserting effects and message replies with AssertJ.

#### Scenario: Integration testing an actor workflow
- **WHEN** testing asynchronous multi-actor workflows or boundaries in `:integration`
- **THEN** `ActorTestKit` and `TestProbe` are used to verify message delivery and lifecycle events.
