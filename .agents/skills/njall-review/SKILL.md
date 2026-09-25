---
name: njall-review
description: Guidance for performing read-only, human-like peer code reviews on Java changes between the current branch/HEAD and origin/main against AGENTS.md, Njall skills, and Java/Pekko best practices.
---

# Skill: Peer Code Review (njall-review)

## Domain Context

This skill guides agents in conducting high-value, read-only peer code reviews on Java changes in **Project Njall**.
Its purpose is to review code the way an experienced human staff engineer would: focusing on architectural intent,
behavioral isolation, Java 25 modernization, Pekko Typed concurrency safety, and meaningful tests—rather than
mechanically re-checking formatting or counting lines that static linters already handle.

---

## Technical Constraints & Invariants

1. **Strictly Read-Only (Non-Modifying)**:
   - Review agents **MUST NOT** edit source files, format code, or stage changes.
   - Running `./gradlew spotlessApply` is **STRICTLY PROHIBITED** during review.
   - The review is an analytical evaluation that outputs feedback to the user and archives an execution log.
2. **WSL Execution**:
   - All Git and Gradle commands must be executed in WSL using direct binary invocation (`wsl git ...`, `wsl ./gradlew ...`) with `BypassSandbox: true`.
3. **Repository-Relative Links**:
   - All citations to files and lines in review outputs must use workspace-relative paths with line anchors:
     `[ServerConfig.java:45-52](common/src/main/java/com/larpconnect/njall/common/config/ServerConfig.java#L45-L52)`.
   - Do **NOT** use `file:///` URLs or raw unanchored filenames.
4. **Log Archiving**:
   - Every review must be saved to `.agents/logs/reviews/review-<branch>-<timestamp>.md` to maintain operational history per `AGENTS.md` Section 1.

---

## Review Execution Protocol

When requested to perform a code review, follow this five-step sequence:

```
+──────────────────────────────────────────────────────────────────────────+
|  1. DISCOVERY        wsl git fetch origin main --quiet                   |
|                      wsl git diff --name-status origin/main...HEAD -- '*.java' |
|                      wsl git diff -U3 origin/main...HEAD -- '*.java'     |
+────────────────────────────────────┬─────────────────────────────────────+
                                     v
+──────────────────────────────────────────────────────────────────────────+
|  2. FULL CONTEXT     Read the full file content of modified classes to   |
|                      understand surrounding component architecture       |
+────────────────────────────────────┬─────────────────────────────────────+
                                     v
+──────────────────────────────────────────────────────────────────────────+
|  3. HUMAN AUDIT      Analyze against Modernization, IOSP-Lite, Pekko,    |
|                      Ecosystem, and Test Meaningfulness                  |
+────────────────────────────────────┬─────────────────────────────────────+
                                     v
+──────────────────────────────────────────────────────────────────────────+
|  4. REPORT & ARCHIVE Deliver formatted review with relative links;       |
|                      archive to .agents/logs/reviews/review-<branch>-<time>.md   |
+──────────────────────────────────────────────────────────────────────────+
```

### Step 1: Branch Resolution & Diff Discovery

Determine the current branch and diff scope:
```powershell
# Get active branch name
wsl git rev-parse --abbrev-ref HEAD

# Ensure origin/main tracking reference is up to date
wsl git fetch origin main --quiet
```

**Scope Rules**:
- **On a feature branch**: Inspect the three-dot merge-base diff against `origin/main`:
  ```powershell
  wsl git diff --name-status origin/main...HEAD -- '*.java'
  wsl git diff -U3 origin/main...HEAD -- '*.java'
  ```
- **On `main` with uncommitted/staged changes**: Fall back to inspecting working tree changes:
  ```powershell
  wsl git diff --name-status HEAD -- '*.java'
  wsl git diff -U3 HEAD -- '*.java'
  ```
- **On clean `main`**: If no diff exists against `origin/main`, check `HEAD~1...HEAD` or report that the branch is clean and in sync with `origin/main`.


### Step 2: Read Full File Context

Do not review isolated diff snippets in a vacuum. Use `view_file` to read the entire Java file being modified. Understand:
- What is this class's role in the package and module?
- How are its dependencies injected?
- Does the change preserve encapsulation and single responsibility?
- What libraries are being used?
- If the diff contains a spec, read the spec file as well to fully understand the intent of the change.

### Step 3: Perform the Qualitative Review

Audit the changes using the **Human-Centric Guidance** and classify every observation into one of the **Four Severity Tiers**. **DO NOT EDIT THE CODE**

Look specifically for:

- Does the code adhere to Google's Java Style Guide, Effective Java and the project's design principles (Njall Architecture Guide, IOSP-Lite)?
- Are the libraries used idiomatically and, if not, why not?
- Is the spec sufficiently and correctly implemented by the change?


### Step 4: Format Report and Archive

1. Present the review directly to the user in chat.
2. Write the exact markdown report to:
   `.agents/logs/reviews/review-<branch>-<timestamp>.md` (e.g. `.agents/logs/reviews/review-feature-healthcheck-20260913-143000.md`).

---

## Log-Level Severity Hierarchy

Categorize every observation using these four standardized levels:

| Level | Definition | Typical Examples |
| :--- | :--- | :--- |
| **`CRITICAL`** | Structural integrity or safety issues that compromise the system. | Circular package/module dependencies, upward package reach (`x.y` depending on `x`), concurrency thread-pinning (`synchronized` on virtual threads), leaking mutable state across actor messages, untyped Pekko Classic APIs, or zero test coverage on complex business branches. |
| **`WARNING`** | Architectural erosion, design flaws, or ecosystem violations. | IOSP-Lite violations (mixing orchestration and execution), library best-practice deviations (e.g. raw queries bypassing DAOs, using Apache Commons when Mug or Guava has an equivalent), brittle mocks testing interactions instead of behavior, or swallowing exceptions. |
| **`INFO`** | Modernization opportunities and idiomatic upgrades. | Replacing imperative `if-instanceof` chains with Java 25 pattern matching switch expressions, converting verbose DTOs to records, using streams with `ImmutableList`, or standardizing single-impl naming (`DefaultFoo` instead of `FooImpl`). |
| **`NIT`** | Minor polish and stylistic improvements. | Variable naming precision, removing redundant parentheses, minor comment clarification, or simplifying a one-line lambda. |

---

## Qualitative Review Guidance

Linters check if braces match and lines are <= 100 characters. **You check whether the code is well-engineered.**

### 1. Java 25 Modernization

Njall targets Java 25 LTS. Flag code written in older Java idioms (Java 8/11 style) and recommend modern equivalents.

#### Pattern Matching Switch Expressions over Cascading `instanceof`
```java
// ⚠️ Pre-Java 21 Imperative Cascading instanceof
public String formatPayload(Event event) {
    if (event instanceof UserCreated) {
        UserCreated uc = (UserCreated) event;
        return "Created user: " + uc.username();
    } else if (event instanceof UserDeleted) {
        UserDeleted ud = (UserDeleted) event;
        return "Deleted user: " + ud.userId();
    }
    throw new IllegalArgumentException("Unknown event type");
}

//  Modern Java 25 Exhaustive Pattern Matching Switch
public String formatPayload(Event event) {
    return switch (event) {
        case UserCreated uc -> "Created user: " + uc.username();
        case UserDeleted ud -> "Deleted user: " + ud.userId();
    };
}
```

#### Immutability and Records over Mutable Accumulation
```java
// ⚠️ Mutable collection accumulation in procedural loop
List<String> activeUsernames = new ArrayList<>();
for (User user : users) {
    if (user.isActive()) {
        activeUsernames.add(user.username());
    }
}

//  Declarative Stream with Guava ImmutableList
ImmutableList<String> activeUsernames = users.stream()
    .filter(User::isActive)
    .map(User::username)
    .collect(ImmutableList.toImmutableList());
```

#### Useless Comments

Look for "anchoring" comments that AIs like to insert and flag them as `NIT`s. For example:

```java
// Wait, I'm not sure about this change. Let me double check.

```

```java
// Add 1 to variable 'i' every iteration
i++;
```

#### Verify Suppressions

If there is any change made to:

- Checkstyle rules
- Compiler settings
- SpotBugs configuration

Or if any variation on `@SuppressWarnings` or `@SuppressFBWarnings` is used, you must verify

1. That the suppression is documented and given a clear reason.
2. That the suppression is justified and reasonable and that the code is well-engineered.
3. That the suppression is actually necessary.


#### Concurrency & Virtual Threads

- Avoid `synchronized` blocks or methods, as they pin virtual threads to carrier threads. Recommend `ReentrantLock` when synchronization is necessary.
- Encourage the use of actors using the `pekko` framework. 

---

### 2. Behavioral Isolation (IOSP-Lite)

Per `AGENTS.md` Section 4, a method may either call other class functions (orchestrating workflow) OR contain internal execution logic and external calls, but **not both**.

```java
// ⚠️ Violation: Method mixes orchestration, validation logic, formatting, and external calls
public void handleRegistration(RegistrationRequest req) {
    // Logic mixed directly with orchestration
    if (req.username() == null || req.username().length() < 3) {
        throw new ValidationException("Invalid username");
    }
    var user = new User(req.username(), req.email()); // Pure factory violated
    userRepository.save(user); // External call
    logger.info("User registered: {}", req.username());
}

//  IOSP-Lite: Clear separation between Orchestration and Leaf Execution
public void handleRegistration(RegistrationRequest req) {
    validateRequest(req);      // Call to validation method
    var user = createUser(req); // Call to isolated factory
    persistUser(user);         // Call to integration leaf
}
```

### 3. Pure Factory Isolation

If a method creates an object using the `new` keyword, object creation must be the **only** operation that method performs.

```java
// ⚠️ Violation: Instantiating object inside logic pipeline
public OrderSummary processOrder(Cart cart) {
    var total = calculateTotal(cart);
    return new OrderSummary(cart.id(), total, Instant.now()); // Mixed logic and new
}

//  Isolated Factory
public OrderSummary processOrder(Cart cart) {
    var total = calculateTotal(cart);
    return buildSummary(cart.id(), total);
}

private OrderSummary buildSummary(String cartId, BigDecimal total) {
    return new OrderSummary(cartId, total, Instant.now());
}
```

---

### 4. Apache Pekko Typed Idioms

1. **No Classic Pekko / Akka**:
   - Check that `Behavior<T>` and `ActorRef<T>` are used. Flag any `UntypedActor`, `ActorRef` without generics, or `getSender()`.
2. **Stateless Behaviors by Default**:
   - Flag mutable instance variables (`private int counter;`) inside actor classes. State must be carried via functional parameter recursion (`active(State state)`).
3. **Closed ADT Message Protocols**:
   - Commands must be defined as `public sealed interface FooCommand` with immutable `record` variants.
   - Any command expecting a response must include a typed `ActorRef<Response> replyTo` component.
4. **Guice Behavior Factories**:
   - External actor dependencies enter through a Guice-managed `*BehaviorFactory` interface, returning an ref object.
5. **Testing**:
   - Synchronous unit tests in `src/test` must use `BehaviorTestKit` and `TestInbox`.
   - `ActorTestKit` is restricted to asynchronous integration tests in `:integration`.

---

### 5. Ecosystem Priority & Library Best Practices

Check that developers honor the library hierarchy defined in `AGENTS.md` Section 3:
$$\text{Mug} > \text{Guice} > \text{Caffeine} > \text{Guava} > \text{Pekko} > \text{Jackson} > \text{SLF4J} > \text{AssertJ} > \text{JUnit} > \text{Mockito} > \text{Commons}$$

- **Apache Commons vs Mug/Guava**: If code introduces `org.apache.commons.lang3.StringUtils` for substring/joining, flag as `WARNING` and suggest `com.google.mu.util.Substring` or Guava `Joiner`.
- **Caffeine vs Guava**: For caches, ensure `Caffeine` is used rather than Guava `CacheBuilder`.
- **Database & Persistence**: Verify that database changes include versioned, idempotent Flyway migrations (`V{N}__*.sql`). Audit DAOs to ensure multitenant `studio-id` / `tenant-id` scoping is respected and CTI (Class Table Inheritance) entity patterns are preserved without inline raw SQL concatenations. Flag missing DAO abstractions as `WARNING`.
- **Single-Implementation Naming**: When an interface has a single implementation, it must be named `DefaultFoo`, **not** `FooImpl`.
- **Logging**: Ensure `private final Logger logger = LoggerFactory.getLogger(Foo.class)` is used, not `static final`, and never `System.out` or `printStackTrace`.
- **Conventional Patterns**: Ensure that conventional, straightforward, clear patterns are used that fit with the existing codebase. Flag non-obvious, unusual, or unidiomatic patterns as `WARNING`.
- **Immutability** : Prioritize immutability where possible. Use `final` for fields, `Immutable` collections where it is possible to do so, and records where it is appropriate. If a collection needs to be mutated in the beginning but then will transfer to read only, consider using a builder and then storing it as an immutable object.

---

### 6. Meaningful Test Verification

- **AssertJ**: Verify that assertions use AssertJ (`assertThat(...)`). Bare JUnit assertions (`assertEquals`, `assertTrue`) are violations.
- **Behavior over Mocks**: Check if tests actually verify behavioral outcomes rather than just verifying that mocks were called in a brittle sequence.
- **osgrove Test Naming**: Check that test method names follow `<method>_<condition>_<expectedOutcome>` or use `@DisplayName`.
- **Useful module testing**:
  - Avoid "double entry accounting" where the mere existence of a binding is checked. Configuration in general does not need to be tested on modules.
  - Provider methods can be checked directly on the instantiated module without even loading Guice.
  - Guice only returns nonnull values by default. So do not check `getInstance` is not null. By the same logic, if a constructor is annotated `@Inject`, it should not validate the passed in parameters to ensure that they are not null.

### 7. Extraneous Changes

Look for "spurious changes" that should not be there. For example, changes that are not related to the spec or the current task. 

For example, if the code is mostly focused around adding a single new HTTP route, and the implementation changes a different HTTP route in a way that is outside of the spec, that should be flagged at the `INFO` level.

---

## Review Report Output Template

When outputting review feedback, adhere to this structure:

```markdown
# Code Review: <branch-name>

**Base Target:** `origin/main`  
**Commit Range:** `origin/main...HEAD` (<commit-hash>)  
**Review Date:** YYYY-MM-DD HH:MM:SS  
**Verdict:** [APPROVED | CHANGES REQUESTED | COMMENT]

---

## Executive Summary

- **Files Analyzed:** N Java files (+X / -Y lines)
- **High-Level Assessment:** <Brief 2-3 sentence overview of the change, architectural fit, and major takeaways>

---

## Detailed Findings

### 🚨 Critical
<!-- Issues that must be addressed before merging -->
- **[Class.java:45-52](path/to/Class.java#L45-L52)**: <Title of issue>
  - **Rule Cited:** `AGENTS.md Section 4: Behavioral Isolation (IOSP-Lite)`
  - **Problem:** <Explanation of the violation>


### ⚠️ Warning
<!-- Suboptimal design, library misuses, fragile tests -->
- **[Service.java:88](path/to/Service.java#L88)**: <Title of issue>
  - **Problem:** Using Apache Commons `StringUtils` when Mug `Substring` is preferred.

### 💡 Info
<!-- Modernization, Java 25 idioms, Guice cleanliness -->
- **[Router.java:120-135](path/to/Router.java#L120-L135)**: Upgrade to pattern matching switch expression.
- **Problem:** This is a Java 25 idiom, and the system is written in modern Java.


### 📝 Nit
<!-- Minor readability or variable naming polish -->
- **[HandlerTest.java:30](path/to/HandlerTest.java#L30)**: Consider renaming `res` to `registeredResponse` for readability.

---

## Archival Note
This review report has been archived to `.agents/logs/reviews/review-<branch>-<timestamp>.md`.
```
