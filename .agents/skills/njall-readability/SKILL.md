---
name: njall-readability
description: Guidance for performing read-only, human-like peer code reviews on Java changes, looking to improve consistency and readability.
---

# Skill: Peer Readability Review


## Domain Context

This skill guides agents in conducting high-value, read-only peer code reviews on Java changes in **Njall**.

Its purpose is to review code the the way an experienced readability reviewer would—looking for consistency with the style guide, and in particular those parts of the style guide that are difficult to automate, and ensuring that the code is clear in its intent—rather than
mechanically re-checking formatting or counting lines that static linters already handle.


## Steps

### Determine Target

If not given any specific instructions then you are to review the diff of the java files between the current branch and `main`, including uncommitted files. 

If given specific instructions as to which files you are reviewing then those are the ones you review.

### Review the Code

Remember: Your goal in reviewing the code is **NOT** to replace Checkstyle or other automated tools. We use other automated tools for a reason. Your goal is also **NOT** to review the code for correctness or **logical** clarity. 

Rather, your job is to act as a **readability** reviewer, reviewing the code to make sure that its intent is clearly legible to a human reader in the codebase, and to ensure that the code is consistent with other code that has been added in the past. 

Examples of things that you look for:

* Are variables and methods clearly named?
* Is `lowerCamelCase` used for variables and `UPPER_BLOCK_LETTERING` used for constants only?

#### Miscellaneous Naming Conventions

* Acronyms and initialisms in names should be spelled out as if they were words. So `Dao` and not `DAO` and `Larp` not `LARP`.
* Single letter variables are reserved for:
  - Iterators
  - Unnamed or unused values (which may alternatively be, or start with, `_`), e.g., `Predicate<String> alwaysTrue = _ -> true;`
* Identifiers are always named with ASCII and, unless in a special case (such as an unused value or a test method name), should always match the regex `\w+`.
* Identifiers are **NEVER** prefixed with a type indicator such as `kValue` to indicate that a value is constant or `iValue` to indicate that it is an integer.
* 

#### Constants

Constants are defined as:

* Static final values
* That are deeply immutable
* That have no side effects

Examples of constants and that can be named with `UPPER_BLOCK_LETTERING`:

* Strings
* Numbers
* Enums
* Protobuf objects
* Immutable collections that contain immutable objects

Examples of things that are not constants are thus must be named with `lowerCamelCase` even if they`static final`:

* Loggers (side effects)
* HTTP Clients (side effects)
* Immutable collections that **contain** mutable objects (not deeply immutable)

