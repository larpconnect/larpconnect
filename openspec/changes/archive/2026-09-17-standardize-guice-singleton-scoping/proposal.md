## Why

In Project Njall, dependency injection lifecycles and scopes are architectural concerns that must be governed centrally by Guice modules rather than intrinsically hardcoded into implementation classes. Several classes in the codebase (specifically `ActiveSessionFactories` in `:data` and `DefaultHttpServerService` in `:server`) currently apply the `@Singleton` annotation directly to the class definition. In the case of `DefaultHttpServerService`, module inspection fails to convey its singleton lifecycle; in the case of `ActiveSessionFactories`, class-level scoping redundantly duplicates the module binding.

Removing `@Singleton` from implementation classes and declaring `.in(Singleton.class)` strictly in the corresponding Guice modules establishes a clean, consistent dependency injection architecture, decouples domain and service classes from Guice scope annotations, and improves module readability and test isolation.

## What Changes

- **Remove Class-Level `@Singleton` in `:data`**: Remove `@Singleton` and its import from `ActiveSessionFactories.java`, retaining its existing module binding `bind(ActiveSessionFactories.class).in(Singleton.class)` in `SessionModule.java`.
- **Remove Class-Level `@Singleton` in `:server`**: Remove `@Singleton` and its import from `DefaultHttpServerService.java`.
- **Declare Explicit Module Scoping in `:server`**: Update `HttpServerModule.java` to bind `HttpServerService` to `DefaultHttpServerService` explicitly with `.in(Singleton.class)`.
- **Verify Module Singleton Contracts in Tests**: Update `HttpServerModuleTest.java` to verify that repeated resolutions of `HttpServerService` yield the identical singleton instance.
- **Document Guice Scoping Standards**: Update `.agents/skills/guice/SKILL.md` to explicitly forbid `@Singleton` annotations on class definitions and mandate module-level scoping (`.in(Singleton.class)` or `@Provides @Singleton`).

## Capabilities

### New Capabilities
- `guice-singleton-scoping`: Enforces explicit module-level singleton scoping for Guice-managed services and components, prohibiting class-level `@Singleton` annotations on implementation classes.

### Modified Capabilities
<!-- No modified capabilities; runtime and public API behavior remain identical. -->

## Impact

- **Affected Java Classes**:
  - `data/src/main/java/com/larpconnect/njall/data/session/ActiveSessionFactories.java`
  - `server/src/main/java/com/larpconnect/njall/server/http/DefaultHttpServerService.java`
  - `server/src/main/java/com/larpconnect/njall/server/http/HttpServerModule.java`
- **Affected Tests**:
  - `server/src/test/java/com/larpconnect/njall/server/http/HttpServerModuleTest.java`
- **Documentation**:
  - `.agents/skills/guice/SKILL.md`
- **Verification Gates**:
  - `./gradlew check` (Spotless, Checkstyle, SpotBugs, ErrorProne, JaCoCo line/branch thresholds)
