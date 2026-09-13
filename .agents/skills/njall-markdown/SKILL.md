---
name: njall-markdown
description: Handles the creation of markdown documents to document the system.
---

# Skill: Writing Documentation in Markdown

## Domain Context

This skill handles the creation of markdown documents to document the system.

## Technical Constraints

1. Markdown files, by default, are stored in `docs/` but may be placed inside of
   subdirectories.
2. Markdown files are properly formatted using a structured document format (so
   favoring headers to represent document structure).
3. Write at a 9-10th grade level.
4. Assume a knowledge base of a person with a bachelors-level understanding of
   of theoretical computer science and a familiarity with LARP concepts.
5. Limit line length to 100 characters. Run `./gradlew spotlessApply` before finalizing.
6. Unless otherwise directed the ideal document length is around 1024±256 words.
   This does not apply to code, typeset mathematics, or mermaid.js diagrams that
   may be included.

## Specific Guidance

- Load documentation from the GitHub Flavored Markdown Spec on context7.
- Prefer that pseudocode be written in mathematical pseudocode. If that is not
  possible, then use Java.
- Use mermaid.js for diagrams. Load the mermaid.js documentation from context7.
- Files should always be UTF-8
- Try to break up large amounts of text with bullet points, code snippets,
  multiple sections, or diagrams.
- **DO NOT** _ever_ use unqualified, static paths to files (e.g., `file:///Users/...`). Always use relative paths. Remember: we should be able to check out the project on a brand new system and have all of the links still work.
