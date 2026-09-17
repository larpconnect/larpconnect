## 1. Database Provisioning & Docker Packaging

- [x] 1.1 Create `docker/postgres/init/01-init.sql` with role creation (`njall`, `njall_admin`, `njall_users`, `njall_system`), role permissions, database ownership, and PostGIS extension installation.
- [x] 1.2 Update `Dockerfile` to stage prebuilt distribution artifacts from `server/build/install/server` using `ghcr.io/rblaine95/eclipse-temurin:25`.
- [x] 1.3 Add `.dockerignore` to optimize build context and exclude local caches, `.git`, and development artifacts.

## 2. Docker Compose Infrastructure

- [x] 2.1 Create `docker-compose.yml` defining the `postgres` service using `postgis/postgis:18-3.6-alpine` with initialization volume, healthcheck, and persistent storage.
- [x] 2.2 Configure the ephemeral `migrate` service in `docker-compose.yml` with `command: ["migrate"]`, `restart: "no"`, and dependency on `postgres` reporting healthy.
- [x] 2.3 Configure the `server` service in `docker-compose.yml` with `command: ["server"]`, port 8080 mapping, and dependencies on both `postgres` healthy and `migrate` completed successfully.

## 3. Root Gradle Lifecycle Tasks

- [x] 3.1 Create root `build.gradle.kts` declaring `composeUp` and `composeStart` tasks that depend on `:server:installDist` and invoke `docker compose up`.
- [x] 3.2 Add companion lifecycle tasks `composeDown`, `composeLogs`, and `composeClean` to root `build.gradle.kts`.

## 4. Verification and Specification Validation

- [x] 4.1 Execute `./gradlew check build` to verify compilation, Spotless, SpotBugs, ErrorProne, Checkstyle, and test suites pass.
- [x] 4.2 Validate container orchestration via `./gradlew composeStart`, verify `/admin/health` responds HTTP 200, and execute `./gradlew composeDown`.
- [x] 4.3 Run `openspec validate docker-compose-system-runtime --type change --strict` to confirm OpenSpec change integrity.
