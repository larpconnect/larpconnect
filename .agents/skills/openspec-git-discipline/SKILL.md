---
name: openspec-git-discipline
description: Use when running OpenSpec propose, continue, apply, verify, archive, or worktree workflows where proposal artifacts, branches, merges, or archive timing affect git history.
license: MIT
compatibility: Requires git and OpenSpec workflow artifacts.
---

# OpenSpec Git Discipline

## Core Rule

Every OpenSpec state change must be committed before the next lifecycle phase depends on it.

- Propose/continue artifacts may be drafted on a branch, but must be committed before apply starts.
- Apply may run on `main`, a branch or a worktree
- Archive may run only after implementation is merged back.

Never create commits, branches, or merges unless the user explicitly asks.

## Gates

| Moment | Gate |
| --- | --- |
| During continue | Before creating the next artifact, ask the user to commit completed artifact changes or explicitly continue without that checkpoint. |
| After propose | Ask the user to commit proposal artifacts; offer to create a PR branch for review. |
| Before apply | Confirm the proposal change is committed; then apply may run from `main`, a branch, or a worktree. |
| After archive | Ask the user to commit archive/spec sync changes. |

## Required Checks

Before apply:

1. Run `git status --short`.
2. Verify `openspec/changes/<change>/` has no uncommitted proposal files.
3. Verify the proposal change is committed before applying from any branch/worktree.

Use this language if the proposal is not committed:

> I should not apply this yet because the proposal change has not been committed. A proposal can be drafted on a branch, but apply must start only after that proposal state is available on `main`. Please merge or commit the proposal to `main` first, then I can apply from `main`, a branch, or a worktree.

Before archive:

1. Run `git status --short`.
2. Stop if implementation work has not been merged back to `main`.

Use this language:

> I should not archive this yet because archive must run from `main` after implementation is merged back. Verify makes a change eligible to merge; it does not replace the merge.

## Red Flags

- Applying a proposal that has been modified without being committed.
- Creating the next continue artifact without asking about committing the previous one.
- Archiving from a feature branch or before implementation is committed.
- Auto-committing, branching, or merging without explicit user approval.

All of these mean: pause, explain the boundary, and ask the user to make the git state explicit.

## Changes from Original

This document is modified from the original version to take into account that `main` is a protected branch: you are not going to be doing development directly on `main` basically ever. Commits should happen on feature branches, and merges should happen from there to main.