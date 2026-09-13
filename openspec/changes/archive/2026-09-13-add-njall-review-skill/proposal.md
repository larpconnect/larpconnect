## Why

Automated CI gates (Checkstyle, SpotBugs, ErrorProne, Spotless) effectively catch syntactic, formatting, and raw metric violations, but cannot evaluate architectural cohesion, Java 25 modernization opportunities, Pekko Typed state idioms, behavioral isolation (IOSP-Lite), or test intent. Introducing the `njall-review` agent skill provides an automated, human-like peer review guide that performs read-only audits of Java diffs against `origin/main` and produces prioritized, actionable feedback without modifying code.

## What Changes

- Create `.agents/skills/njall-review/SKILL.md` defining read-only code review workflows, git diff inspection steps against `origin/main...HEAD`, non-modifying verification gates, and human-centric audit criteria.
- Establish guidance for evaluating code across four log-level severity tiers: `CRITICAL`, `WARNING`, `INFO`, and `NIT`.
- Provide concrete guidance and before/after code examples focusing on Java 25 modernization (pattern matching switch expressions, records, immutable collections), IOSP-Lite behavioral separation, and Pekko Typed actor state semantics.
- Define a structured report template using repository-relative path links (`[Path.java:L#](path/to/Path.java#L#)`) and instructions for archiving reviews to `.agents/logs/review-<branch>-<timestamp>.md`.

## Capabilities

### New Capabilities
- `agent-skills/njall-review`: Defines operational guidance, inspection criteria, severity classification, and archiving protocols for read-only peer code reviews on Java changes.

### Modified Capabilities

## Impact

- **Affected files**: New skill directory `.agents/skills/njall-review/SKILL.md` and log destination `.agents/logs/`.
- **APIs and Runtime**: Zero production runtime, dependency, or API impact; strictly an agent guidance skill.
- **Tooling and Workflow**: Extends agent capabilities to perform non-destructive code analysis across feature branches.
