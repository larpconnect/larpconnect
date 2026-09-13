## 1. Checkstyle Configuration

- [x] 1.1 Add `MatchXPath` rule under `TreeWalker` in `config/checkstyle/checkstyle.xml` forbidding `LOGGER` and `LOG` names for variables of type `Logger`.
- [x] 1.2 Verify `checkstyleMain` flags the existing `LOGGER` in `DefaultAdminRoute.java`.

## 2. Source Code Normalization

- [x] 2.1 In `api/src/main/java/com/larpconnect/njall/api/admin/DefaultAdminRoute.java`, remove `static`, rename `LOGGER` to `logger`, and update call sites to `logger.error` and `logger.warn`.
- [x] 2.2 In `server/src/main/java/com/larpconnect/njall/server/ServerApp.java`, change `LoggerFactory.getLogger(getClass())` to `LoggerFactory.getLogger(ServerApp.class)`.
- [x] 2.3 In `server/src/main/java/com/larpconnect/njall/server/http/DefaultHttpServerService.java`, change `LoggerFactory.getLogger(getClass())` to `LoggerFactory.getLogger(DefaultHttpServerService.class)`.

## 3. Module Verification & Quality Gates

- [x] 3.1 Run `wsl ./gradlew :api:check` to ensure `:api` unit tests, Checkstyle, SpotBugs, and JaCoCo coverage pass.
- [x] 3.2 Run `wsl ./gradlew :server:check` to ensure `:server` unit tests, Checkstyle, SpotBugs, and JaCoCo coverage pass.
- [x] 3.3 Run `wsl ./gradlew check build` across all modules to verify full project compliance.

## 4. OpenSpec Validation

- [x] 4.1 Run `openspec validate standardize-logger-and-ban-caps --type change --strict`.
