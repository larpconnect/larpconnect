## Context

Project Njall maintains strict architectural constraints in `AGENTS.md` (directed acyclic graph, package-to-module parity, IOSP-Lite behavioral separation, and dual-layer testing) and establishes idioms in dedicated skills (`njall-java`, `njall-pekko`, `njall-java-testing`).

Mechanical linters (Spotless, Checkstyle, SpotBugs, ErrorProne, JaCoCo) automatically guard syntax, formatting, and coverage gates during `./gradlew check`. However, they cannot assess design cohesion, Java 25 modernization, Pekko Typed actor state encapsulation, or the expressive meaning of test suites. The `njall-review` skill establishes a human-like peer review guide for AI agents to audit branch diffs against `origin/main` without mutating source code.

## Goals / Non-Goals

**Goals:**
- Provide clear, repeatable instructions for an AI agent to execute non-modifying code reviews on Java changes between the current branch and `origin/main`.
- Focus inspection on human-centric code quality: Java 25 idioms, IOSP-Lite separation, Pekko Typed state recursion, and meaningful AssertJ tests.
- Classify feedback into an intuitive log-level severity hierarchy: `CRITICAL`, `WARNING`, `INFO`, and `NIT`.
- Mandate repository-relative file links (`[File.java:L#](path/to/File.java#L#)`) and archive review logs to `.agents/logs/`.
- Incorporate non-modifying automated verification checks (`spotlessCheck`, `checkstyleMain`, `spotbugsMain`).

**Non-Goals:**
- Making code modifications, refactoring files, or running auto-formatters (`spotlessApply` is strictly forbidden).
- Re-implementing or manually duplicating linter checks (no manual line-counting, brace-checking, or cyclomatic formula calculations).
- Replacing `./gradlew check` or automated CI gates.

## Architecture & Workflow (C4 Component Diagram)

```
+─────────────────────────────────────────────────────────────────────────────+
|                         AGENT WORKSPACE ENVIRONMENT                         |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                     njall-review Skill Guidance                       |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ directs execution                    |
|                                      v                                      |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                           Git Diff Extractor                          |  |
|  |  * wsl git fetch origin main --quiet                                  |  |
|  |  * wsl git diff -U3 origin/main...HEAD -- '*.java'                    |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ feeds diff chunks                    |
|                                      v                                      |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                      Automated Non-Modifying Gates                    |  |
|  |  * wsl ./gradlew spotlessCheck checkstyleMain spotbugsMain            |  |
|  |  * wsl ./gradlew test jacocoTestReport                                |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ outputs check results                |
|                                      v                                      |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                      Human-Centric Review Auditor                     |  |
|  |  * Java 25 Modernization (records, pattern switch, immutability)      |  |
|  |  * IOSP-Lite Behavioral Isolation (orchestration vs leaf logic)       |  |
|  |  * Pekko Typed Encapsulation (closed ADT protocols, functional state) |  |
|  |  * Test Expressiveness (AssertJ behavior verification)                |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ produces review findings             |
|                                      v                                      |
|  +───────────────────────────────────────────────────────────────────────+  |
|  |                       Report & Log Generator                          |  |
|  |  * Format markdown with relative links: [File:L#](rel/path#L#)        |  |
|  |  * Archive to .agents/logs/review-<branch>-<timestamp>.md             |  |
|  +───────────────────────────────────────────────────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### 1. Read-Only Diff Inspection over In-Place Refactoring
- **Decision**: The skill is strictly non-modifying. Running `spotlessApply` or modifying source code is prohibited during review.
- **Alternatives Considered**: Automatically generating fixes or applying Spotless during the review.
- **Rationale**: A review must be an analytical, non-destructive audit. Automated changes pollute the working tree, risk unintended edits, and violate reviewer-author separation.

### 2. Human-Centric Quality Guidance over Bureaucratic Rule-Checking
- **Decision**: Avoid manual SLOC counting or brace checks that linters already catch. Focus on architectural intent, IOSP-Lite separation, Java 25 modernization, and test quality.
- **Alternatives Considered**: Having the agent mechanically count SLOC (<500/50) and cyclomatic complexity formulas on every method.
- **Rationale**: Humans and LLMs excel at semantic, conceptual reviews (e.g. noticing that a loop can become an immutable stream, or that orchestration is mixed with I/O). Checkstyle and PMD already catch mechanical metric thresholds.

### 3. Four-Tier Log-Level Severity Hierarchy
- **Decision**: Categorize findings into `CRITICAL`, `WARNING`, `INFO`, and `NIT`.
- **Alternatives Considered**: Traditional GitHub `BLOCKER` / `WARNING` / `COMMENT` or numeric severity.
- **Rationale**: Log-level naming maps naturally to software engineering impact and immediately communicates the urgency of a finding.

### 4. Repository-Relative Path Citations
- **Decision**: All code references must use workspace-relative paths with line anchors (`[Class.java:45-52](path/to/Class.java#L45-L52)`).
- **Alternatives Considered**: `file:///` URLs or raw file basenames.
- **Rationale**: Relative paths are portable across different developer machines and render cleanly in IDEs without absolute path exposure.

### 5. Review Persistence in `.agents/logs/`
- **Decision**: Every review run writes its full output to `.agents/logs/review-<branch>-<timestamp>.md`.
- **Alternatives Considered**: Ephemeral chat-only responses.
- **Rationale**: Conforms to `AGENTS.md` Section 1 requirements for operational tracking and provides an auditable history for PR preparation.

## Risks / Trade-offs

- **[Risk] Large feature branch diffs**: Diff size could exceed conversational context if hundreds of files changed.
  - **Mitigation**: The skill instructs agents to prioritize modified business and actor logic first, inspect diffs file-by-file if necessary, and summarize bulk additions (e.g. generated or boilerplate DTOs).
- **[Risk] Gradle execution time**: Running `./gradlew check` can take several minutes.
  - **Mitigation**: The skill recommends running fast static gates (`spotlessCheck checkstyleMain spotbugsMain`) first, and scoping test tasks to affected modules when appropriate.
- **[Risk] Subjective feedback disputes**: Qualitative advice might lead to overly pedantic suggestions.
  - **Mitigation**: Ground guidance with concrete before-and-after examples in the skill document.

## Migration Plan

Zero migration required. Adding `.agents/skills/njall-review/SKILL.md` is strictly additive to the agent customization ecosystem.

## Open Questions

- None. All requirements and design criteria were resolved during exploration.
