## Why

Njall is transitioning completely from Vert.x to Apache Pekko as its core runtime backbone. To ensure all development and AI agent code generation adheres to strict architectural invariants (Java 25 LTS, sealed message hierarchies, stateless typed behaviors, Guice injection, and dual-layer testing), a dedicated `njall-pekko` skill and authoritative guidelines are required.

## What Changes

- Create `.agents/skills/njall-pekko/SKILL.md` establishing binding rules, architectural patterns, and recipes for Apache Pekko Typed in Njall.
- Mandate Java 25 sealed interfaces and records for typed message protocols with explicit `replyTo` references.
- Enforce stateless actors by default, reserving functional parameter recursion for stateful behaviors.
- Prohibit Pekko Classic APIs and enforce explicit `ActorRef<T>` message passing (`tell`) over blocking `ask`.
- Define the standard Guice Behavior Factory pattern for injecting external dependencies into actor hierarchies without violating IOSP-Lite.
- Establish the dual-testing standard: `BehaviorTestKit` for deterministic Layer 1 unit tests in `src/test`, and `ActorTestKit` with `TestProbe` for Layer 2 integration tests.

## Capabilities

### New Capabilities
- `agent-skills/njall-pekko`: Standards and developer instructions for designing, implementing, and testing Apache Pekko Typed actors in Njall.

### Modified Capabilities
None.

## Impact

- Creates `.agents/skills/njall-pekko/SKILL.md`.
- Establishes the authoritative pattern for actor development, Guice dependency injection, and testkit usage across all current and future modules.
