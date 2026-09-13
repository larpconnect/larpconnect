## Context

The codebase contains an inconsistency in logger declarations:
1. `DefaultAdminRoute.java` defines `private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdminRoute.class);`. This violates project standards by using `static` and uppercase `LOGGER`.
2. `ServerApp.java` and `DefaultHttpServerService.java` use `LoggerFactory.getLogger(getClass())` instead of the explicit class literal `Foo.class`.
3. The build static analysis suite (`config/checkstyle/checkstyle.xml`) does not currently enforce naming conventions for `Logger` variables, allowing regressions.

Both `.agents/skills/njall-java/SKILL.md` and `.agents/skills/njall-review/SKILL.md` mandate `private final Logger logger = LoggerFactory.getLogger(Foo.class)`.

## Goals / Non-Goals

**Goals:**
- Add an AST-based Checkstyle rule to forbid naming any `Logger` field or variable `LOGGER` or `LOG`.
- Ensure the Checkstyle rule does NOT fail if a logger is declared `static`, while allowing future rules or reviews to guide instance-based loggers.
- Clean up all existing instances of `static Logger` and `LOGGER` in `DefaultAdminRoute.java`.
- Standardize all logger instantiations across the repository on explicit class literals (`Foo.class`).

**Non-Goals:**
- Forbidding `static` modifiers across all types via Checkstyle (static loggers are cleaned up manually in this change).
- Adding broad identifier naming restrictions that could interfere with legitimate constants.
- Introducing external annotation processors (e.g. Lombok `@Slf4j`)—the project strictly uses standard SLF4J factories.

## Component Architecture (C4-Inspired)

```
+-------------------------------------------------------------------------+
|                           GRADLE BUILD PIPELINE                         |
+-------------------------------------------------------------------------+
|                                                                         |
|   +-----------------------------------------------------------------+   |
|   |                  Static Analysis Quality Gate                   |   |
|   |                                                                 |   |
|   |   config/checkstyle/checkstyle.xml                              |   |
|   |     +-------------------------------------------------------+   |   |
|   |     | TreeWalker                                            |   |   |
|   |     |   * MatchXPath: Forbid LOGGER and LOG variable names  |   |   |
|   |     |   * MethodLength, CyclomaticComplexity, NPath         |   |   |
|   |     +---------------------------+---------------------------+   |   |
|   +---------------------------------┼-------------------------------+   |
|                                     │ Inspects AST                      |
|                                     v                                   |
|   +-----------------------------------------------------------------+   |
|   |                      Java Source Modules                        |   |
|   |                                                                 |   |
|   |   :api       - DefaultAdminRoute (logger, DefaultAdminRoute.class)  |   |
|   |   :server    - ServerApp, DefaultHttpServerService (Foo.class)      |   |
|   |   :common    - (no logger declarations)                         |   |
|   |   :integration - Cucumber steps (no logger declarations)        |   |
|   +-----------------------------------------------------------------+   |
|                                                                         |
+-------------------------------------------------------------------------+
```

## Decisions

### Decision 1: Use `MatchXPath` in Checkstyle instead of Regex or IllegalIdentifierName
- **Option 1: `MatchXPath` under `TreeWalker`** (Selected):
  ```xml
  <module name="MatchXPath">
      <property name="query"
                value="//VARIABLE_DEF[./TYPE//IDENT[@text='Logger'] and ./IDENT[@text='LOGGER' or @text='LOG']]"/>
      <message key="matchxpath.match"
               value="Logger variable must not be named 'LOGGER' or 'LOG'. Use 'logger' instead."/>
  </module>
  ```
  *Rationale*: Precise AST querying that inspects both the variable type (`Logger` or `org.slf4j.Logger`) and identifier names (`LOGGER` or `LOG`). It does not trigger false positives on other constants, does not suffer from multiline regex fragility, and does not reject `static final Logger logger`.
- **Option 2: `IllegalIdentifierName`** (Rejected): Regex-based identifier checking across all variables without type awareness.
- **Option 3: `ConstantName`** (Rejected): Standard Checkstyle `ConstantName` requires all-caps for static final fields, which contradicts our convention and would reject lowercase static loggers.

### Decision 2: Remove `static` and rename `LOGGER` to `logger` in `DefaultAdminRoute`
- **Rationale**: Route instances are injected by Guice, their methods are instance methods, and SLF4J loggers are thread-safe. Declaring the logger as `private final Logger logger = LoggerFactory.getLogger(DefaultAdminRoute.class);` conforms to `njall-java` and `njall-review` rules.

### Decision 3: Standardize on `Foo.class` over `getClass()`
- **Rationale**: `getClass()` incurs dynamic method dispatch and can produce unexpected sub-class logger categories if a class is extended. Explicit class literals (`ServerApp.class`, `DefaultHttpServerService.class`) are deterministic, faster, and standard across Njall.

## Risks / Trade-offs

- **[Risk] MatchXPath performance in large codebases** -> *Mitigation*: Checkstyle executes `MatchXPath` in-memory over parsed ASTs during `TreeWalker` traversal; runtime impact across Njall's small-to-medium compilation units is negligible (sub-second).
- **[Risk] Multi-logger classes needing distinct names (e.g. `auditLogger`)** -> *Mitigation*: The XPath query specifically targets `LOGGER` and `LOG`. It does not restrict custom lowercase identifiers like `auditLogger` or `securityLogger`.

## Migration Plan

1. Update `config/checkstyle/checkstyle.xml` with the `MatchXPath` rule.
2. Update `DefaultAdminRoute.java` (`static` removal, `LOGGER` -> `logger`, fix call sites).
3. Update `ServerApp.java` and `DefaultHttpServerService.java` (`getClass()` -> `Foo.class`).
4. Execute `wsl ./gradlew checkstyleMain checkstyleTest` and `wsl ./gradlew check build`.

## Open Questions

- None. All branches of the decision tree were resolved during exploration.
