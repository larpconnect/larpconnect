---
name: njall-pekko
description: Standards for Apache Pekko Typed actors, sealed record message protocols, stateless behaviors, Guice behavior factories, and BehaviorTestKit in Njall.
---

# Skill: Apache Pekko Typed Architecture

## Domain Context

This skill defines the binding execution constraints, architectural invariants, and design
patterns for implementing concurrent actor pipelines with **Apache Pekko Typed (2.0-M4+, Scala 3)**
in **Project Njall**. Njall employs Pekko as its primary application runtime, eliminating Vert.x
and classic Akka/Pekko untyped conventions.

---

## Technical Constraints

1. **Java 25 LTS Standard**: All actor implementations must target Java 25 LTS, leveraging sealed
   hierarchies, immutable records, pattern matching switch expressions, and virtual threads.
2. **Strictly Typed Actors**: Always use `org.apache.pekko.actor.typed.Behavior<T>` and
   `org.apache.pekko.actor.typed.ActorRef<T>`. All Pekko Classic APIs (`pekko.actor.Actor`,
   `UntypedActor`, `getSender()`, and untyped `ActorRef`) are strictly forbidden.
3. **Stateless Default**: Actors must be stateless by default. Mutable instance variables inside
   actor classes are prohibited. State transitions must be modeled as functional parameter
   recursion (`active(State state)`).
4. **Guice Factory Isolation**: Actors must never be instantiated directly by Guice. External
   dependencies must be injected into Guice-managed `*BehaviorFactory` interfaces.
5. **Dual-Layer Testing**:
   - **Layer 1 (Unit Tests in `src/test`)**: Must use `BehaviorTestKit` for synchronous,
     zero-overhead, deterministic execution.
   - **Layer 2 (Integration Tests in `:integration`)**: Must use `ActorTestKit` and `TestProbe`.
   - All assertions must use **AssertJ** (`assertThat(...)`).

---

## Specific Guidance

### 1. Message Protocols with Sealed Interfaces and Records

All messages (commands, events, and replies) must form closed Algebraic Data Types (ADTs):
- **Root Protocol**: Define as a `public sealed interface <Name>Command`.
- **Command Variants**: Define as `public record <Variant>(... , ActorRef<Response> replyTo) implements <Name>Command`.
- **Explicit `replyTo`**: Pekko Typed eliminates implicit sender tracking. Any command expecting
  a reply must explicitly define a typed `ActorRef<ResponseType> replyTo` component.
- **Exhaustive Handling**: Use Java 25 pattern matching within switch expressions to guarantee
  compile-time exhaustiveness over all permitted variants.

```java
package org.larpconnect.njall.character.protocol;

import org.apache.pekko.actor.typed.ActorRef;

public sealed interface CharacterCommand {
    record RegisterCharacter(
        String characterId,
        String name,
        ActorRef<CharacterResponse> replyTo
    ) implements CharacterCommand {}

    record RetireCharacter(
        String characterId,
        ActorRef<CharacterResponse> replyTo
    ) implements CharacterCommand {}
}
```

Response protocols follow the identical sealed record pattern:

```java
package org.larpconnect.njall.character.protocol;

public sealed interface CharacterResponse {
    record CharacterRegistered(String characterId) implements CharacterResponse {}
    record CharacterOperationFailed(String characterId, String reason) 
        implements CharacterResponse {}
}
```

---

### 2. Stateless Behaviors and Functional Recursion

#### Stateless Actors (Default)
When an actor performs transformations, validations, or delegating operations without maintaining
accumulating internal state, implement it as a static factory returning a stateless behavior via
`Behaviors.receiveMessage`:

```java
public final class CharacterWorker {
    private CharacterWorker() {}

    public static Behavior<CharacterCommand> create(CharacterValidator validator) {
        return Behaviors.receiveMessage(message -> switch (message) {
            case CharacterCommand.RegisterCharacter reg -> onRegister(validator, reg);
            case CharacterCommand.RetireCharacter ret -> onRetire(validator, ret);
        });
    }

    private static Behavior<CharacterCommand> onRegister(
        CharacterValidator validator,
        CharacterCommand.RegisterCharacter cmd
    ) {
        if (validator.isValidName(cmd.name())) {
            cmd.replyTo().tell(new CharacterResponse.CharacterRegistered(cmd.characterId()));
        } else {
            cmd.replyTo().tell(new CharacterResponse.CharacterOperationFailed(
                cmd.characterId(), "Invalid character name"
            ));
        }
        return Behaviors.same();
    }

    private static Behavior<CharacterCommand> onRetire(
        CharacterValidator validator,
        CharacterCommand.RetireCharacter cmd
    ) {
        // ...
        return Behaviors.same();
    }
}
```

#### Stateful Actors (Functional Parameter Recursion)
If state accumulation is unavoidable (e.g. sequence counters, buffers, aggregators), **never** declare
mutable fields (`private int count;`). Instead, define an immutable state record and pass it through
recursive behavior methods:

```java
public final class CharacterAggregator {
    private record State(ImmutableSet<String> activeIds) {}

    private CharacterAggregator() {}

    public static Behavior<CharacterCommand> create() {
        return active(new State(ImmutableSet.of()));
    }

    private static Behavior<CharacterCommand> active(State state) {
        return Behaviors.receiveMessage(message -> switch (message) {
            case CharacterCommand.RegisterCharacter reg -> {
                State nextState = new State(
                    ImmutableSet.<String>builder()
                        .addAll(state.activeIds())
                        .add(reg.characterId())
                        .build()
                );
                reg.replyTo().tell(new CharacterResponse.CharacterRegistered(reg.characterId()));
                yield active(nextState);
            }
            case CharacterCommand.RetireCharacter ret -> {
                // Return updated recursive state
                yield active(state);
            }
        });
    }
}
```

---

### 3. Actor References & Message Passing

- **Strictly Encapsulated**: Never expose actor instances or internal methods. Outside components
  must only hold `ActorRef<Command>`.
- **Prefer Fire-and-Forget (`tell`)**: Non-blocking message emission (`actorRef.tell(cmd)`) is the
  standard communication paradigm.
- **Minimize `ask`**: Avoid `AskPattern.ask(...)` inside actor logic as it introduces futures,
  timeout overhead, and thread hops. When integrating at external HTTP boundaries, immediately
  pipe or adapt responses back into the typed actor messaging stream.

---

### 4. Guice Behavior Factory Pattern

To preserve the Directed Acyclic Graph (DAG) and comply with Njall's IOSP-Lite constraints,
external dependencies (databases, external clients, configuration) must be passed into actors via
dedicated Guice factories:

```
+─────────────────────────────────────────────────────────────────────────────+
|                             GUICE CONTAINER                                 |
|                                                                             |
|   +-----------------------+                 +---------------------------+   |
|   | External Dependencies |                 | Behavior / Actor Factory  |   |
|   | (Repos, Config, AMQP) |───────────────> | (Injects deps into actor) |   |
|   +-----------------------+                 +-------------+-------------+   |
+───────────────────────────────────────────────────────────│─────────────────+
                                                            │ spawns
                                                            v
+─────────────────────────────────────────────────────────────────────────────+
|                        PEKKO ACTOR TREE (TYPED)                             |
|                                                                             |
|   CoordinatorActor ──> context.spawn(factory.create(), "worker-name")       |
+─────────────────────────────────────────────────────────────────────────────+
```

#### Step 1: Define the Factory Interface
```java
package org.larpconnect.njall.character.actor;

import org.apache.pekko.actor.typed.Behavior;
import org.larpconnect.njall.character.protocol.CharacterCommand;

public interface CharacterWorkerFactory {
    Behavior<CharacterCommand> create();
}
```

#### Step 2: Implement Factory with Guice Injection
```java
package org.larpconnect.njall.character.actor;

import com.google.inject.Inject;
import org.apache.pekko.actor.typed.Behavior;
import org.larpconnect.njall.character.domain.CharacterValidator;
import org.larpconnect.njall.character.protocol.CharacterCommand;

final class DefaultCharacterWorkerFactory implements CharacterWorkerFactory {
    private final CharacterValidator validator;

    @Inject
    DefaultCharacterWorkerFactory(CharacterValidator validator) {
        this.validator = validator;
    }

    @Override
    public Behavior<CharacterCommand> create() {
        return CharacterWorker.create(validator);
    }
}
```

#### Step 3: Bind in Local Guice Module
```java
package org.larpconnect.character.actor;

import com.google.inject.AbstractModule;

public final class CharacterActorModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CharacterWorkerFactory.class).to(DefaultCharacterWorkerFactory.class);
    }
}
```

---

### 5. Testing with Pekko TestKits

#### Layer 1: Synchronous Unit Tests (`BehaviorTestKit`)
All unit tests in implementation modules (`src/test`) must use `BehaviorTestKit` to maintain 100%
deterministic execution without spinning up background actor threads or triggering async timeouts:

```java
package org.larpconnect.njall.character.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.larpconnect.njall.character.domain.CharacterValidator;
import org.larpconnect.njall.character.protocol.CharacterCommand;
import org.larpconnect.njall.character.protocol.CharacterResponse;

final class CharacterWorkerTest {

    @Test
    @DisplayName("registerCharacter with valid name emits CharacterRegistered")
    void registerCharacter_validName_emitsRegisteredResponse() {
        CharacterValidator validator = mock(CharacterValidator.class);
        when(validator.isValidName("Thorin")).thenReturn(true);

        BehaviorTestKit<CharacterCommand> testKit = 
            BehaviorTestKit.create(CharacterWorker.create(validator));
        TestInbox<CharacterResponse> replyInbox = TestInbox.create();

        var command = new CharacterCommand.RegisterCharacter(
            "char-101", "Thorin", replyInbox.getRef()
        );
        testKit.run(command);

        CharacterResponse response = replyInbox.receiveMessage();
        assertThat(response)
            .isInstanceOf(CharacterResponse.CharacterRegistered.class)
            .extracting(r -> ((CharacterResponse.CharacterRegistered) r).characterId())
            .isEqualTo("char-101");
    }
}
```

#### Layer 2: Asynchronous Integration Tests (`ActorTestKit`)
All multi-actor lifecycle and boundary tests in `:integration` use `ActorTestKit` and `TestProbe`:

```java
package org.larpconnect.njall.integration.character;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.larpconnect.character.protocol.CharacterCommand;
import org.larpconnect.character.protocol.CharacterResponse;

final class CharacterIntegrationTest {
    private static ActorTestKit testKit;

    @BeforeAll
    static void setup() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    static void tearDown() {
        testKit.shutdownTestKit();
    }

    @Test
    void executeCharacterWorkflow() {
        TestProbe<CharacterResponse> probe = testKit.createTestProbe();
        ActorRef<CharacterCommand> worker = testKit.spawn(CharacterWorker.create(name -> true));

        worker.tell(new CharacterCommand.RegisterCharacter("char-202", "Gimli", probe.ref()));

        CharacterResponse response = probe.receiveMessage();
        assertThat(response).isInstanceOf(CharacterResponse.CharacterRegistered.class);
    }
}
```

---

## Antipatterns

| Instead of | Do this | Justification |
| :--- | :--- | :--- |
| `pekko.actor.ActorRef` (untyped) | `ActorRef<T>` (typed) | Compile-time message type safety |
| `AbstractActor` / `UntypedActor` | `Behavior<T>` / `Behaviors.receive` | Eliminates runtime casting errors |
| `getSender()` | Explicit `ActorRef<Response> replyTo` | Transparent protocol contracts |
| Mutable fields (`private int count;`) | Functional parameter recursion (`active(State)`) | Thread safety, zero race conditions |
| `AskPattern.ask(...)` everywhere | Fire-and-forget `.tell(...)` with `replyTo` | Non-blocking, zero future timeout overhead |
| `@Inject` directly on Actor class | Inject Guice deps into `*BehaviorFactory` | Decouples Guice DI from actor lifecycle |
| `ActorTestKit` in `src/test` unit tests | `BehaviorTestKit` in `src/test` | Deterministic, synchronous, faster build execution |
| JUnit `assertEquals(...)` | AssertJ `assertThat(...)` | Project standard assertion engine |
| Leaking actor class instance | Expose only `ActorRef<T>` | Strict encapsulation boundary |
