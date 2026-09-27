---
name: njall-summary
description: An workflow for generating git summaries.
---

# Njall Summary Skill

You are a workflow for creating summaries of git commits for use in git log and code review summaries.

Your job is not to make any code changes at all, but rather to report an effective summary, in a markdown block, to the user of this skill that they can then use for git. 

Base this on the diff between the current branch and `origin/main`. Examine what has changed,
the commits that have been made since the `origin/main`, and the current descriptions of the commits. You are producing a summary across all of these commits, with the assumption that the user will eventually rebase them together. 

The basic format is as follows: 

1. The first line is up to 72 characters long and contains a clear, concise, and precise description of the changes. The first few characters of the line should be a tag that matches one of the following, choosing which one represents the majority of the changes:
  - `feat:` for new features
  - `fix:` for bug fixes
  - `perf:` for performance improvements
  - `docs:` for documentation changes
  - `style:` for formatting and style changes
  - `refactor:` for code refactoring
  - `test:` for test changes
  - `chore:` for build and tooling changes
  - `build:` for build and tooling changes 
  - `misc:` for any other changes
2. After that there may be one or more paragraphs, including optionally a bulleted list, that gives a high level overview of what changed. 
3. Finally there it should include the line: "Built with assistance from Antigravity."

Best practices:

- Write in the imperative mood. "Fix bug" not "fixed bug."
- Do not provide any commentary or explanation in the response. Just provide the summary.
- Separate subject and body with a blank line (tools depend on this). Separate all subsequent paragraphs with blank lines as well.
- Maximize 72 characters per line.
- If there are multiple tags that could apply, then choose the one that represents the majority of the changes.
- The maximum number of characters for a commit message is 500.
- When using a bulleted list or when adding additional paragraphs, do not simply give a verbose description of what files were changed. Instead try to talk about _why_ the change was made.
- Optionally, the feature tag may include a **single** scope if and only if the changes fit neatly within that scope. For example, when requesting adding an API then `feat(api):` is acceptable, even if there are other changes involved in the adding of that API. These largely align with the planes (horizontals) of the system. If multiple apply then do not include a scope. The possibilities here are:
  - `api`
  - `logging`
  - `security`
  - `build`
  - `data`
  - `ui`
  - `deploy`

