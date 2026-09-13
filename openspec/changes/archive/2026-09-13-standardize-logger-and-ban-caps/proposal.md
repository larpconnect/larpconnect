## Why

Inconsistent logger declaration patterns (such as uppercase `LOGGER` or `LOG`, unnecessary `static` modifiers, and virtual `getClass()` dispatch) violate Njall's code quality standards and review guidelines. Enforcing logger naming via Checkstyle prevents regressions, while normalizing current loggers ensures uniform, idiomatic SLF4J usage across all modules.

## What Changes

- Add a Checkstyle `MatchXPath` rule in `config/checkstyle/checkstyle.xml` to forbid naming any variable of type `Logger` as `LOGGER` or `LOG`.
- In `DefaultAdminRoute.java`, remove the `static` modifier, rename `LOGGER` to `logger`, and update call sites (`LOGGER.error` and `LOGGER.warn`).
- In `ServerApp.java` and `DefaultHttpServerService.java`, normalize `LoggerFactory.getLogger(getClass())` to explicit class literals (`ServerApp.class` and `DefaultHttpServerService.class`).
- Verify the build passes all quality gates with `./gradlew checkstyleMain checkstyleTest` and `./gradlew check build`.

## Capabilities

### New Capabilities
- `logger-conventions`: Automated Checkstyle linting rule forbidding `LOGGER` and `LOG` variable names for `Logger` instances, and standardization on `private final Logger logger = LoggerFactory.getLogger(Foo.class)`.

### Modified Capabilities
<!-- None: No functional or runtime API behavioral changes. -->

## Impact

- **Build/Linting**: `config/checkstyle/checkstyle.xml` updated with a new `MatchXPath` check.
- **Source Code**:
  - `api/src/main/java/com/larpconnect/njall/api/admin/DefaultAdminRoute.java`
  - `server/src/main/java/com/larpconnect/njall/server/ServerApp.java`
  - `server/src/main/java/com/larpconnect/njall/server/http/DefaultHttpServerService.java`
- **APIs/Dependencies**: Zero external API or dependency changes.
