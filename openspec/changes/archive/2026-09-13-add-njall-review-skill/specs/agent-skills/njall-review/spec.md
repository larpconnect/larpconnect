## ADDED Requirements

### Requirement: Read-Only Diff Discovery and Scope Resolution
The skill SHALL guide agents to inspect changed Java files between the current branch and `origin/main` using merge-base diffs (`git diff --name-status origin/main...HEAD -- '*.java'` and `git diff -U3 origin/main...HEAD -- '*.java'`). If invoked on `main` with uncommitted changes, it SHALL fall back to comparing `HEAD` against the working tree. The skill SHALL strictly prohibit any code modifications, formatting mutations (`spotlessApply`), or file edits during review execution.

#### Scenario: Discovering Java changes on a feature branch
- **GIVEN** an agent is executing the `njall-review` skill on a feature branch
- **WHEN** the agent identifies changes against `origin/main`
- **THEN** it fetches `origin/main` and inspects the three-dot diff (`origin/main...HEAD`) for `*.java` files without modifying any repository files.

#### Scenario: Running review on working tree changes
- **GIVEN** an agent is executing the `njall-review` skill on `main` with uncommitted edits
- **WHEN** the agent initiates diff discovery
- **THEN** it inspects working tree changes against `HEAD` without staging, committing, or altering the files.

### Requirement: Non-Modifying Verification Gate Execution
The skill SHALL guide agents to invoke non-modifying Gradle quality gates in WSL (`spotlessCheck`, `checkstyleMain`, `spotbugsMain`, and `compileJava`), and optionally run test verification (`test`, `jacocoTestCoverageVerification`) when tests are present. The skill SHALL forbid running `spotlessApply` or any destructive task.

#### Scenario: Executing static quality gates
- **WHEN** an agent performs the automated verification step
- **THEN** it executes `wsl ./gradlew spotlessCheck checkstyleMain spotbugsMain` and incorporates any tool failures into the review findings without modifying source code.

### Requirement: Human-Centric Code Quality and Modernization Guidance
The skill SHALL instruct agents to avoid duplicating checks performed by Checkstyle or SpotBugs (such as mechanical line counts, brace placements, or unused imports) and instead evaluate code quality as a senior human engineer would. The skill SHALL provide qualitative guidance and concrete before-and-after examples focusing on:
1. Java 25 modernization (exhaustive pattern matching switch expressions, records, immutable collections).
2. Behavioral isolation (IOSP-Lite: strictly separating high-level orchestration methods from low-level execution/leaf methods).
3. Apache Pekko Typed idioms (functional parameter recursion for state, closed ADT message protocols with typed `replyTo`, Guice behavior factory patterns, and `BehaviorTestKit` for unit tests).
4. Ecosystem hierarchy (prioritizing Mug > Guice > Caffeine > Guava > Pekko > Commons).

#### Scenario: Auditing Java 25 modernization and design patterns
- **GIVEN** a diff contains cascading `instanceof` checks, procedural loops with mutable lists, or mixed orchestration and I/O logic
- **WHEN** the agent reviews the diff chunks
- **THEN** it provides concrete refactoring recommendations to modern Java 25 idioms and IOSP-Lite principles with illustrative before/after snippets.

### Requirement: Log-Level Severity Tiers
The skill SHALL categorize all review feedback using four standardized log-level severity tiers:
- `CRITICAL`: Architectural invariant violations (DAG breaches, upward package dependencies), concurrency/virtual thread pinning hazards, broken Pekko protocols, or completely unverified critical logic.
- `WARNING`: Suboptimal design, IOSP-Lite violations, best practice violations with libraries or databases, fragile test mocks, bypassing ecosystem priority (e.g. Apache Commons over Mug/Guava), or improper exception handling.
- `INFO`: Modernization opportunities (Java 25 records, switch expressions, stream pipelines), Guice binding cleanups, or documentation clarity.
- `NIT`: Minor stylistic suggestions, concise variable names, or small test cleanups.

#### Scenario: Classifying findings by severity
- **WHEN** an agent identifies issues in the audited Java diff
- **THEN** each finding is labeled with `CRITICAL`, `WARNING`, `INFO`, or `NIT` based on the impact on system integrity and maintainability.

### Requirement: Relative Link Citations and Archive Logging
The skill SHALL mandate that all file and code references in review output use repository-relative paths with line numbers (e.g., `[Class.java:45-52](path/to/Class.java#L45-L52)`) rather than absolute or `file:///` URLs. The skill SHALL also instruct the agent to persist the full markdown review report to `.agents/logs/reviews/review-<branch>-<timestamp>.md`.

#### Scenario: Formatting links and persisting review log
- **WHEN** an agent completes the code review
- **THEN** all code references are formatted as repository-relative markdown links, the report is displayed to the user, and a copy is saved to `.agents/logs/reviews/`.
