## 1. Environment Isolation and Git Exclusion

- [x] 1.1 Update `.gitignore` to exclude `.env` and `.env.*` patterns while permitting tracked `!.env.example`.
- [x] 1.2 Create committed `.env.example` documenting configuration keys and non-sensitive placeholder comments.


## 2. Dynamic PostgreSQL Initialization

- [x] 2.1 Replace `docker/postgres/init/01-init.sql` with an executable `docker/postgres/init/01-init.sh` shell script that provisions roles dynamically using `NJALL_DB_SECRET`.
- [x] 2.2 Ensure `docker/postgres/init/01-init.sh` maintains Unix LF line endings to prevent container execution failures on Windows hosts.


## 3. Docker Compose and Configuration Sanitization

- [x] 3.1 Update `docker-compose.yml` to eliminate hardcoded plaintext passwords and configure derived role credentials from `NJALL_DB_SECRET`.
- [x] 3.2 Update `common/src/main/resources/reference.conf` to sanitize fallback database passwords to empty strings.


## 4. Gradle Lifecycle Orchestration

- [x] 4.1 Implement `generateComposeEnv` task in root `build.gradle.kts` using `java.security.SecureRandom` to generate a 16-byte hex secret seed if `.env` does not exist.
- [x] 4.2 Wire `composeUp` and `composeStart` tasks to depend on `generateComposeEnv`.
- [x] 4.3 Update `composeClean` task to delete the local `.env` file upon volume teardown (`docker compose down -v`).


## 5. Verification and End-to-End Testing

- [x] 5.1 Execute `./gradlew check build` to ensure all compilation, linting (Spotless, Checkstyle, SpotBugs, ErrorProne), and unit/integration tests pass.
- [x] 5.2 Execute `./gradlew composeClean composeStart` to verify automated `.env` generation, PostgreSQL role provisioning with derived credentials, successful Flyway migration execution, and HTTP server startup.


