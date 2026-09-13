## 1. Skill Foundation and Directory Setup

- [x] 1.1 Create `.agents/skills/njall-review/` directory and initialize `SKILL.md` with YAML frontmatter.
- [x] 1.2 Ensure `.agents/logs/` directory exists for persisted review outputs.

## 2. Review Guidance and Inspection Standards

- [x] 2.1 Document the branch detection, remote sync, and read-only Git diff extraction procedure (`origin/main...HEAD`).
- [x] 2.2 Document non-modifying automated verification tasks (`spotlessCheck`, `checkstyleMain`, `spotbugsMain`, `test`) and strict prohibition on `spotlessApply`.
- [x] 2.3 Author the human-centric code quality guide featuring Java 25 modernization, IOSP-Lite behavioral isolation, Pekko Typed actor state recursion, and meaningful AssertJ tests with concrete before/after code snippets.
- [x] 2.4 Document the four-tier log-level severity hierarchy (`CRITICAL`, `WARNING`, `INFO`, `NIT`).
- [x] 2.5 Specify output report template with repository-relative links (`[File.java:L#](path/to/File.java#L#)`) and instructions for archiving to `.agents/logs/review-<branch>-<timestamp>.md`.

## 3. Verification and Validation

- [x] 3.1 Validate markdown structure, formatting, and link syntax across the new skill documentation.
- [x] 3.2 Execute `wsl ./gradlew check build` to verify workspace integrity.
- [x] 3.3 Validate the OpenSpec change with `wsl openspec validate add-njall-review-skill --type change --strict`.
