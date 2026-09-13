## 1. Skill Creation and Core Rules

- [x] 1.1 Create `.agents/skills/njall-pekko/SKILL.md` with appropriate YAML frontmatter and domain context, verifying the file exists with valid skill metadata.
- [x] 1.2 Document protocol conventions using Java 25 sealed interfaces, immutable records, and explicit `ActorRef<Response> replyTo` fields, verifying code examples demonstrate exhaustive pattern matching.
- [x] 1.3 Document actor behavior statelessness by default and functional state recursion (`active(State)`), verifying instructions prohibit mutable actor instance variables.
- [x] 1.4 Document the Guice Behavior Factory pattern for injecting services into actor hierarchies, verifying the separation between Guice DI and actor lifecycles.
- [x] 1.5 Document the dual-testing standard (`BehaviorTestKit` for synchronous unit tests in `src/test` and `ActorTestKit` with `TestProbe` for `:integration`), verifying AssertJ assertions are mandated.
- [x] 1.6 Add complete end-to-end domain recipe demonstrating protocol definition, stateless behavior, Guice factory binding, and a synchronous `BehaviorTestKit` unit test.

## 2. Validation and Review

- [x] 2.1 Verify `.agents/skills/njall-pekko/SKILL.md` formatting, wrapping, and code snippet accuracy against Java 25 LTS and Apache Pekko Typed standards.
- [x] 2.2 Run `openspec validate` to confirm all planning and delta spec artifacts satisfy schema requirements.
