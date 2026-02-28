# Skill: Git

## Domain Context

This skill handles the proper use of git.

## Technical Constraints

- At the start of the session, run
  `git config --global credential.helper 'cache --timeout=3600'` and ensure the
  provided token is cached.
- There is a github action that monitors compliance with the various rules.
  Always fix (non-billing) issues that come from these actions.
- Branch names should begin with the name of the agent and a slash, so `jules/`
  for jules, `ona/` for ona, etc. If the agent has been "renamed" (e.g., with
  "you are `<foo>`") then "`<foo>`" is the name of the agent.

## Specific Guidance

### Git Messages

1. The first line of every commit message must be 74 characters or less and must
   be in plain text.
2. The body of the commit message is separated by at least 1 whitespace line
   from the first line and should be in markdown.
3. Lines of the body of the commit message should not exceed 100 characters.
4. Commit messages should be descriptive of what was done in the commit.
5. Commit messages should be professional and written with an eye toward future
   developers.
