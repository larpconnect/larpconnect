# Implementation Tasks

## 1. HAProxy Configuration and TLS Scripting

- [x] 1.1 Create executable script `docker/haproxy/generate-certs.sh` to generate self-signed RSA 2048-bit certificate bundle (`haproxy.pem`) with SANs `localhost` and `127.0.0.1` if absent.
- [x] 1.2 Create directory `docker/haproxy/certs/` with tracked `.gitkeep`, and update `.gitignore` to exclude `docker/haproxy/certs/*.pem` and `docker/haproxy/certs/*.key`.
- [x] 1.3 Create `docker/haproxy/haproxy.cfg` configuring global and frontend maxconn (100), global TLS ciphersuites, timeouts, unified frontend `fe_ingress` on ports 8080 and 8443, `http-response del-header traceparent`, and backend healthcheck on `/api/admin/v1/health`.

## 2. Gradle Lifecycle Task Integration

- [x] 2.1 Register `GenerateComposeTlsTask` in root `build.gradle.kts` to ensure `docker/haproxy/certs/haproxy.pem` exists via `docker/haproxy/generate-certs.sh`.
- [x] 2.2 Update `composeStart` task in `build.gradle.kts` to depend on `generateComposeTls` (removing `composeUp`).
- [x] 2.3 Rename `composeDown` to `composeStop`, and update `composeStopClean` task in `build.gradle.kts` to delete `docker/haproxy/certs/haproxy.pem` along with `.env`.

## 3. Docker Compose Orchestration and Network Segmentation

- [x] 3.1 Define user-defined bridge networks `edge` and `internal` in `docker-compose.yml`.
- [x] 3.2 Add `haproxy` service using `haproxy:3.1-alpine` to `docker-compose.yml` attached to `edge`, publishing ports `8080` and `8443`, mounting `haproxy.cfg` and `certs/`.
- [x] 3.3 Remove host port publishing `ports:` from `server` service in `docker-compose.yml` and attach `server` to both `edge` and `internal` networks.
- [x] 3.4 Attach `postgres` and `migrate` services exclusively to the `internal` network in `docker-compose.yml`.

## 4. Verification and Specification Compliance

- [x] 4.1 Verify certificate generation and clean lifecycle tasks via `./gradlew generateComposeTls` and `./gradlew composeStopClean`.
- [x] 4.2 Run `./gradlew check build` to confirm build, Spotless formatting, and test suites pass.
- [x] 4.3 Run `openspec validate add-haproxy-ingress --type change --strict` to verify OpenSpec planning validity.
- [x] 4.4 Update `README.md` with HAProxy ingress details, ports (8080/8443), certificate handling, and compose workflows.
