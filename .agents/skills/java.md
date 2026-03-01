# Skill: Modern Java (JDK 25)

## Domain Context

This skill handles working in the modern Java 25 environment.

## Technical Constraints

1. The system is in Java 25 LTS. It uses gradle with the kotlin DSL. Use Java 25
   features and avoid legacy constructions such as anonymous inner classes. Use
   the stream API, lambdas, and a functional style.
2. This project has the experimental features enabled. This should be used for
   the structured concurrency features in Java 25.
3. Primary code must be written in Java 21+. Build files must be written in
   kotlin. Integration tests may be written in gherkin.

## Specific Guidance

### Core Architectural Principles

- Type and Null Safety: Developers must prioritize rigorous type safety and the
  mitigation of null-related vulnerabilities. The use of Optional<T> is
  mandatory for return types where a value may be absent. Direct invocation of
  .get() on an Optional instance is prohibited; developers should employ
  `orElseThrow(Supplier)` or equivalent safe-access patterns. Assume values that
  are not annotated as `Nullable` or explicitly contracted as such in some other
  way are non-null by default.
- Modern Java Implementation: All source code shall target JDK 21 or later.
  Implementation should leverage contemporary features, specifically Records for
  immutable data carriers, Sealed Classes and Interfaces for restricted type
  hierarchies, and Pattern Matching within switch expressions to enhance code
  clarity and exhaustive analysis.
- Modular Maintainability: Method definitions should remain concise and
  specialized. Adherence to SOLID design principles is required to facilitate
  long-term maintainability and codebase scalability.
- Concurrent Performance: For high-concurrency and I/O-intensive operations, the
  use of Virtual Threads (Project Loom) is the preferred standard. This approach
  ensures optimal resource utilization and high throughput without the overhead
  associated with traditional platform threads.

### Guava

Guava utilities are explicitly allowed in the system and preferred to Apache
Commons utilities where available. Use context7 to get the latest documentation.

- Use Guava when there is not a similar piece of functionality in the JDK (e.g.,
  use the JDK `Optional`, not the Guava equivalent). Exceptions include: Joiner,
  `io`, and immutable collections, which should always be used in preference to
  similar tools elsewhere.
- Do not use `@Deprecated` components unless absolutely necessary and document
  why they are necessary if they must be used.
- Do not use `@Beta` components in the library modules. They _may_ be used
  anywhere in testing as well as in the application components (repl and batch).

exercised.

### Implementation Directives

- Immutability: Immutability is the default preference. Developers should
  utilize Records and declare variables as final whenever feasible.
  - Use `ImmutableList`, `ImmutableSet`, etc whenever possible. Prefer creating
    new objects to updating old ones.
- Classes should be either abstract or final where possible. Package protected
  should be the default for classes, with any exposure of their functionality
  done through an interface with Guice.
- Avoid static methods.
- Strongly prefer static inner classes where inner classes are used.
- Include a `package-info.java` file in every package with a public object in
  it.
- Functional programming: Prefer `streaming` interfaces, closures, and
  functional patterns.
- Asynchronous Processing: Virtual threads should be leveraged for I/O
  operations to maintain system responsiveness and throughput. Avoid
  synchronized blocks or heavy ThreadLocals that can lead to 'pinning' of
  virtual threads; prefer ReentrantLocks where necessary. Avoid the use of
  `Thread` directly and use listening thread pools from guava if non-virtual
  threads are needed.
- Exceptional Circumstances:
  - Unchecked exceptions are appropriate for critical application-level
    failures.
  - Custom checked exceptions should be utilized for domain-specific errors that
    allow for recovery.
  - Fault Tolerance: Background processes must implement robust error handling
    to prevent silent thread termination.
- Domain Modeling: Utilize Sealed Interfaces and Records to model domain states
  precisely. This approach minimizes "primitive obsession" by encapsulating data
  within contextually meaningful types. Avoid "stringly typed" systems and use
  enums or records as appropriat.
- Logging
  - Always log error messages. Do not ever `printStackTrace`.
  - Do not ignore exceptions. If they are truly trivial they can be logged at a
    debug level, or a comment can be inserted indicating that the exception
    cannot happen.

happen.

### Java Style

- When there only exists a single implementation for a java interface or
  abstract class, that class may be named `Default<InterfaceName>`. It should
  never be named `InterfaceNameImpl`.
- Interfaces should just be the interface name and not include any marking in
  the name indicating that they are an interface (so `Example` not `IExample` or
  `ExampleInterface`).
- Classes should be package protected. If they need to be used outside of the
  class there should be a guice binding to an interface and/or a factory. If
  they are public they should _always_ be final.
- Classes should be either abstract or final.
- Abstract classes should be named `Abstract<ClassName>`. They should typically
  be package protected.
- Thread safety should always be documented using JSR305 annotations.
- For non-record classes: Prefer using factories or factory methods to using
  `new <ClassName>(<args>)`. The best case is to inject them with guice.
- Dependencies may only reach _down_ or _out_ in the package hierarchy. They may
  not reach directly _up_. So `x.y` may depend on other classes in `x.y`,
  `x.y.z`, `x.p`, or `x.p.q.r`, but never on classes in `x` proper.
- Dependencies may never be circular between packages. If `x.y` depends on
  `x.p.q.r` then `x.p.q.r` cannot depend on anything in `x.y`.
- Tests should be named `<ClassName>[OptionalTestType]Test`. If it is a test of
  a "default" implementation it should be named `<InterfaceName>Test`.
  `OptionalTestType` may be used for, e.g., performance or integration tests,
  but should not be used to mark just plain unit tests. Example test types:
  - None. The default. `FooTest`
  - Integration. `FooIntegrationTest`, typically written with cucumber, though
    may be written with other tooling.
  - Performance. `FooPerformanceTest`, used to check for performance
    regressions. Should typically use Gatling or the Java Microbenchmark
    Harness.

tests.

### Logging with SLF4J

- Every class that is going to be doing logging should have a
  `private final Logger` at the top of the class declaration, taking the class
  as the argument for the `LoggerFactory` to produce the logger.
- `fatal` is for logs surrounding the unexpected death of the process.
- `error` is for logs where there was a compromise in the integrity of the
  system in some way. This includes crashes, data corruption, etc. This includes
  situations where a major feature is broken or unusable.
- `warn` is for logs where there is an exception or error of some sort that is
  not expected and that needs to be corrected, but which may represent a
  transient failure.
- `info` is for logs that represent the progress of the application. Especially
  in batch mode.
- `debug` is for logs useful for debugging but that will be disabled at runtime.
- `trace` is for extremely fine-grained logs that are needed for debugging but
  won't even be turned on for casual users. Complex log messages in tight loops,
  for example.
- `Marker`s may be used for cross-cutting concerns. Specifically useful for:
  - Logs reporting on the application configuration
  - Initialization and shutdown logs

### Libraries

The following libraries may be included without hesitation and their
functionality should be employed freely.

- ErrorProne Annotations, `mug-errorprone`
- Any of the vert.x 5 libraries. Use context7 to get the most recent
  documentation on these when you use them.
- Mug and Mug extensions, and in particular `mug-guava`, `dot-parse`, and
  `mug-concurrent24`
- Guava, especially if the functionality does not exist in the JDK or in Mug. In
  particular, the following should be used whenever possible:
  - Immutable collections, Joiner, and Splitter.
  - `com.google.common.io`
  - `com.google.common.graph`
- Caffeine for caching, if caches are needed. Use this in preference to Guava
  caches.
- Apache Commons libraries where the functionality does not exist in Mug, Guava,
  or the JDK. In particular `io` and `lang3`. Always prefer the implementation
  in Mug or Guava when one exists in either.
  - Also worth noting are `csv`, `RDF`, and `text` which may be used if the need
    comes up. These should always be used in preference to rolling our own
    implementation of any of the above functionality
- `dev.cel:cel` if needed for expressions or user-entered "code" that cannot be
  (or should not be) done declaratively

## Antipatterns

This table contans a list of patterns to avoid, as well as the alternative to
prefer instead:

|               Instead of                |               Do this                |                Justification                 |
|-----------------------------------------|--------------------------------------|----------------------------------------------|
| `Optional.get()`                        | `Optional.orElseThrow(Supplier)`     | Throw a customized exception from a supplier |
| `synchronized`                          | `ReentrantLock`                      | Avoid pinning for virtual threads            |
| `new ArrayList<T>()`                    | `ImmutableList.builder()`            | End result is immutable                      |
| `List<T>` (interface)                   | `ImmutableList<T>`                   | Immutable interfaces are strongly preferred  |
| JUnit Asserts                           | AssertJ (e.g., `assertThat`          | AssertJ is a more robust testing system      |
| `Foo x = new Foo()`                     | `var x = new Foo()`                  | Prefer modern Java Patterns                  |
| `new RuntimeException(…)`               | `new CustomException` or IAE         | Use specific instead of generic exceptions   |
| `java.util.Logger`                      | SLF4J                                | Maintains coherent logging.                  |
| `printStackTrace`, `System.out.println` | SLF4J                                | Maintains coherent logging.                  |
| `private static final Logger`           | `private final Logger`               | Static is unnecessary here                   |
| `new NonRecordClass(…)`                 | Use guice injection and/or a factory | Easier to test and coherent with the system  |
| `@Named("…")`                           | Use a custom qualifier annotation    | Easier to track across the entire codebase   |
| `PrivateModule`                         | Use a custom qualifier annotation    | Easier to debug and reason about             |
| `method(arg1, inOutParameter)`          | `var retval = method(arg1)`          | Prefer modern java idioms                    |

## Miscellaneous

- When it is not contradicted by other instructions, use the
  [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
