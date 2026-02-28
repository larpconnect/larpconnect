# Skill: Testing

## Domain Context

This skill handles writing tests.

## Technical Constraints

- Tests are written with JUnit5, AssertJ, and Mockito. They optionally (in
  `:integration`) use cucumber
- 100% branch coverage is required in testing
- `:test` contains centralized test logic and dependencies, `:integration`
  contains integration tests.
- Tests should be named `<ClassName>[OptionalTestType]Test`. If it is a test of
  a "default" implementation it should be named `<InterfaceName>Test`.
  `OptionalTestType` may be used for, e.g., performance or integration tests,
  but should not be used to mark just plain unit tests.

## Specific guidance

### Testing Protocols

- Test-Driven Development (TDD): The adoption of TDD is strongly encouraged.
  Developers should implement tests prior to writing functional code to ensure
  requirement alignment and incremental progress.
- Unit Testing: JUnit 5 and AssertJ are the standard frameworks for unit
  verification. Test classes must be located in `<module>/src/test/java` and
  follow the package structure of the target source.
- Integration Testing: Cucumber shall be employed for behavior-driven
  development (BDD) scenarios. For tests requiring external infrastructure, such
  as databases, Testcontainers is the required solution. Integration tests
  written with cucumber may be included in the `:integration:` module.
- Regression and Snapshot Testing: Selfie is used for verifying CLI output and
  parser state. Updates to snapshots must be explicitly reviewed before
  acceptance. Snapshot changes must be committed in the same PR as the logic
  change, and the commit message must explicitly acknowledge the output delta.
- Coverage: Write the tests alongside the code. Every branch (if/else) must be
  exercised.

### Unit Tests

- Name unit tests with osgrove's test name pattern:
  `<method>_<what is being tested>_<expected outcome>`. If the name is going to
  be longer than 90 characters then use abbreviations and omit assumed words
  (e.g., `return` in the `<expected outcome>`) to reduce the length and add a
  `@DisplayName`.
- Unit tests should aim to test one thing per `@Test`.
- Use AssertJ. Matchers should be implemented to minimize the number of
  assertions per test, with one assert per test being the goal.
- Avoid reflection in tests. Never, ever use reflection for construction of
  objects.
- Focus on behavior verification.
- Testing by injecting the public guice modules is preferable when such is an
  option.
- Aim for tests to test a single thing per test. Where possible, use only a
  single assert per test, and use AssertJ's features to be able to test complex
  objects.
- Test names should be terse and precise as to what is being tested. Avoid
  saying "is correct" in favor of saying what is correct/expected.
- This project enforces (via jacoco) extremely high testing requirements across
  both the instructions that are tested as well as ensuring that the branches
  are covered. Always design with testability in mind and make sure things are
  thoroughly tested.

### Integration Tests

- Integration tests are written with cucumber and stored in `:integration`

### Troubleshooting Inadequate Coverage

This project has a very high branch and line coverage requirement that is
sometimes difficult to reach.

Strategies you can use:

- Remove unreachable code unless the compiler requires you to have it
- Break methods down into smaller `private` units.
- Take smaller `private` methods out and create a new `class` with them. You can
  make this class package private or you can bind it with an interface. Either
  way, put it in the local binding module and then inject it.
- If you need to mock something but can't because it is `final` or because its
  constructor is package private, then create an interface for it, bind it in
  Guice, and then use Guice to inject it.
