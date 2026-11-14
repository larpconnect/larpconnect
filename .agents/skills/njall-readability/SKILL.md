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
* Ternary operators, while allowed, are disfavored and need extra scrutiny.
* Switch statements should be "new style" switch statements.

#### Miscellaneous Naming Conventions

* Acronyms and initialisms in names should be spelled out as if they were words. So `Dao` and not `DAO` and `Larp` not `LARP`. See the section on camel case for more specific interpretations.
* Single letter variables are reserved for:
  - Iterators
  - Unnamed or unused values (which may alternatively be, or start with, `_`), e.g., `Predicate<String> alwaysTrue = _ -> true;`
* Identifiers are always named with ASCII and, unless in a special case (such as an unused value or a test method name), should always match the regex `\w+`.
* Identifiers are **NEVER** prefixed with a type indicator such as `kValue` to indicate that a value is constant or `iValue` to indicate that it is an integer.
* Avoid any pattern that duplicates a name adjacently in a class. For example: `FooBarFactoryFactory`, `FooProviderProvider`, `FooServiceService`. When this would happen naturally there are several ways to address it, including:
  - Refactoring so that the structure is no longer required
  - Wrapping objects in other patterns, e.g., creating a `ProviderFactory` instead of a `ProviderProvider`
  - Reconsidering if the structure is actually what is being created (is it _actually_ a `ProviderProvider` or is it just a `Provider` that is producing a `Supplier`?)
  - Reconsider whether the naming is actually required by the coding standards at all.
- Avoid any pattern in naming that has three "descriptor" suffixes consecutively, e.g., `FooSupplierProviderFactory` or `FooProviderSupplierProvider`.  It is extremely unlikely that these patterns are actually going to be required, and the reasoning for them deserves additional scrutiny.
- The only classes that can be prefixed with `Abstract` are `abstract`. These **must** be prefixed with `Abstract`.
- The only classes that can be prefixed with `Default` are concrete and (usually, though there can be exceptions when dealing with standards like JPA) both package private and `final`. If there is a `Default` class then there SHOULD NOT be any other non-test implementations.
- The `Module` suffix is reserved for Guice modules.
- Enums are always named in with `UPPER_BLOCK_LETTERING`

##### Camel Case Examples

| Prose Form | UpperCamelCase | lowerCamelCase | Incorrect |
| ---------- | -------------- | -------------- | --------- |
| XML HTTP request | XmlHttpRequest | xmlHttpRequest | xmlHTTPRequest |
| outer stopwatch  | OuterStopwatch | outerStopwatch | outerStopWatch |
| system DAO | SystemDao | systemDao | SystemDAO |

#### Constants

Constants are defined as being:

1. Static final values
2. That are deeply immutable
3. That have no side effects

Examples of constants and that can therefore be named with `UPPER_BLOCK_LETTERING`:

* Strings
* Numbers
* Enums
* Protobuf objects
* Immutable collections that contain immutable objects

Examples of things that are not constants are thus must be named with `lowerCamelCase` even if they are `static final`:

* Loggers (side effects)
* HTTP Clients (side effects)
* Immutable collections that **contain** mutable objects (not deeply immutable)

#### Strings

##### Non-ASCII characters

When strings contain non-ASCII characters it is **always** preferred to use the actual unicode character, e.g., `String unitAbbrev = "μs"`. If this is not possible (e.g., because it is a non-printable character) then it should use an escape and be commented, e.g., `return '\ufeff' + content; // byte order mark`.

##### Blocks

When blocks of text need to be used inline then always use multiline strings instead of string concatentation, for example:

```java
"""
Multiple lines
can be represented
like this
"""
```

String concatenation should be avoided, but not for performance reasons. Rather it should be avoided because there is almost always a better solution in modern java.

#### Methods

- Public methods should avoid `boolean` arguments. Prefer using an enum flag or two separate methods.


#### Database Fields and Naming

- Tables are plural
- When a name is a foreign key identifier backed by a UUID it is suffixed with `_id` (or it **is** `id`). nb this does not necessarily hold in _other layers_ of the system: the API may very well not munge its name to match the database schema.
- Always scope a table name in the appropriate schema.

#### Logging

In general:

- `ERROR` is for unrecoverable, system-level errors. 
- `WARN` is for recoverable or user-level errors.
- `INFO` is for informational pieces of data that is contextually relevant to what is happening. 
- `DEBUG` and below are for information that we do not want continually streaming in production.

### Report

When done print a report to the user or invoking agent. That report should break down all readability issues identified. 