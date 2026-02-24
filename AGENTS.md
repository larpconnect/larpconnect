# LARPConnect Development Project

# Java Project Development Guidelines: Technical Standards and Best Practices

You are a Senior Java engineer working on the project `metonomy`. To maintain modularity, your technical capabilities are partitioned into "skills": 

* **Discovery**: Upon initialization, you must read `.agents/SKILLS.md` to identify active capabilities. You must then load the file you are directed to based on the capabilities you need for a given project. When you use a skill, add it's name to your commit message.
* **Delegation**: When a task falls within a skill's domain, defer to the specific constraints in that module.
* **Conflicts**: If a skill constraint conflicts with a core `AGENTS.md` rule, the specific skill constraint takes precedence.

## Key Points

* Always check `.agents/SKILLS.md` at the start of a task and load the relevant skill files. Load relevant skill files as you need them or if you anticipate needing them.
* This is a Java 25 project that uses Java 25 idioms and features. Including but not limited to virtual threads, the streaming api, sealed interfaces, and record classes. The structured concurrency experiment is enabled and free to use as well. 
  * Use records for data carriers and sealed interfaces for closed hierarchies
  * Always prefer `ImmutableList.of()`, `ImmutableList.builder()`, or `stream().collect(ImmutableList.toImmutableList())`
  * Address all warnings immediately. Use `@SuppressWarnings` only as a last resort and *always* with a justifying comment
* 100% branch coverage is required in testing. Testing is based on JUnit 5, AssertJ, and Cucumber.
* This is a Gradle project using the Kotlin DSL. Do not use groovy.
* This is a high complexity project that demands adherence to rigid coding standards and makes heavy use of abstractions.
  * Run `./gradlew spotlessApply` before every commit or check
  * Read the error messages carefully. They usually point to specific style violations or potential bugs
* This is a system that values correctness, completeness, and flexibility.
* Vert.x is the base build system. Injection is handled by Guice.

## Definition of Done

This system uses a robust set of automated tests to ensure compliance with quality standards. In order for a patch to be merged, it must first pass those quality checks.

1. Logic is complete
2. Tests are written, including integration tests in cucumber if appropriate
3. `./gradlew spotlessApply` has been run. If this is not done then spontaneous failurs will hapepn in other parts of the pipeline.
4. `./gradlew check` has been run and passes. If this is not done then the code cannot be merged and will not be reviewed to be merged.
5. `./gradlew build` has been run and passes. If this is not done then the code cannot be merged and will not be reviewed to be merged.

Once these criteria are met then the code may be pushed. You do not need to confirm readiness to push if these criteria are met.

## Core Documents

* `AGENTS.md`: You are here. A guide for agent development, also lays out broader coding standards.
* `.agents/SKILLS.md`: A routing file for determining which skills are needed and which files to load as a result. The skills themselves are stored in `.agents/skills/` files.
* [CONTRIBUTING.md](./CONTRIBUTING.md): Guide for getting started on and contributing to this project. Intended for humans.
* [README.md](./README.md): Guide for using this project, getting it trunning, and understanding the basic layout and features. Intended for humans though useful for agents.

## Technical Configuration

 * Multi-Module Architecture: To ensure separation of concerns, the project is organized into distinct modules:
   * `:parent` Contains the central dependencies. All modules within the system inherit `parent`.  Java library.
   * `:api` The basic API for the REST service.
   * `:proto` The wire protocol objects. Serialized with protobuf.
   * `:bom` Applies the java-platform plugin and pulls in all of the other modules and dependencies. 

## Workflow

Act as a senior Java developer. Before outputting the code, mentally verify it against the Checkstyle and Spotbugs configurations. If a line exceeds 100 characters or lacks a proper Javadoc, rewrite it before sending the final response.

* Before writing the implementation for a feature, write a comprehensive test suite in `:integration` that covers edge cases, null handling, and happy paths. Ensure the tests follow our Checkstyle rules for naming conventions. Only after the tests are written, provide the implementation code that satisfies them.
* Before writing a class that has any non-storage functionality, write a comprehensive junit test suite that covers edge cases, null handling, and happy paths. Ensure the tests follow our Checkstyle rules for naming conventions. Only after the tests are written, provide the implementation code that satisfies them.
* Version Control Standards:
  * Feature branches must be prefixed with `<agent name>/`, so `jules/` for jules.
  * A linear commit history is required; developers must rebase on the main branch rather than performing merge commits.
* Do not seek validation of if there is "any more work left to do" unless work has already been specified for you. Always favor getting the code compliant with checks.
* There is a github action that will run on every commit. It _must_ pass in order for the PR to be merged.
* If a commit looks like it will be greater than 500 lines, try to split it into multiple independent commits within the same PR. Some pieces of work that should generally almost always be split off into independent commits:
  * Adding a new module
  * Any changes to code quality checks that goes in alongside other work
  * Significant refactoring of old code
  * Cleanly separable "units of work" that take place in different modules (so doing the cli work separate from the configuration work). 
* Execution Order:  
  - Step 1: List the edge cases you will test.
  - Step 2: Write the Test class.
  - Step 3: Write the Implementation class.  

## Code Quality and Static Analysis

 * Formatting Standards: The Spotless plugin, configured with Google Java Format, shall be used to enforce a uniform coding style.
 * Static Analysis: Developers must utilize Checkstyle, SpotBugs, and ErrorProne to identify and rectify potential defects and stylistic inconsistencies during the build process.
 * Warning Mitigation: Compilation warnings must be resolved. The use of `@SuppressWarnings` is permitted only when accompanied by thorough documentation justifying the exception.
 * API Documentation: Publicly accessible types and methods require comprehensive Javadoc documentation. Brief descriptions may be omitted only if the functionality is inherently self-evident from the identifier naming.




