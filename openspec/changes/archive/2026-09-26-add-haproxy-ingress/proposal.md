# Proposal: HAProxy Ingress Reverse Proxy and Network Segmentation

## Why

In **Njall**, the **Server** container currently exposes port 8080 directly on the host network, leaving the backend HTTP service directly accessible without an edge proxy layer. Introducing HAProxy as the ingress reverse proxy establishes TLS termination on port 8443, enforces network isolation so that the backend **Server** receives traffic exclusively from HAProxy, and strips internal W3C `traceparent` headers from responses returned to external clients.

## What Changes

- Add HAProxy as an ingress service (`haproxy`) to Docker Compose, exposing port 8080 (plain HTTP) and port 8443 (TLS).
- Remove host port bindings from the `server` service in `docker-compose.yml` so that it is no longer directly reachable from the host machine.
- Segment Docker Compose networks into `edge` (shared exclusively by `haproxy` and `server`) and `internal` (shared by `server`, `migrate`, and `postgres`).
- Configure HAProxy with maxconn 100, modern TLS ciphersuites, timeouts, active health checking against `/api/admin/v1/health`, and response header stripping for `traceparent`.
- Add a standalone POSIX shell script `docker/haproxy/generate-certs.sh` to generate a self-signed RSA 2048-bit certificate bundle (`haproxy.pem`) for local development if absent.
- Register a `generateComposeTls` Gradle task in `build.gradle.kts` hooking into `composeStart`, and update `composeStopClean` to wipe generated certificates.
- Update `.gitignore` to exclude generated TLS certificates while tracking directory structure.

## Capabilities

### New Capabilities

<!-- None -->

### Modified Capabilities

- `system-runtime-orchestration`: Extends Docker Compose orchestration to four services (`postgres`, `migrate`, `server`, `haproxy`), enforces network segmentation preventing direct host access to the backend **Server**, strips `traceparent` response headers at the edge, and automates TLS certificate generation and cleanup in Gradle lifecycle tasks.

## Impact

- **Docker Compose**: `docker-compose.yml` updated with `haproxy` service, network declarations (`edge`, `internal`), and removal of `ports:` on `server`.
- **Infrastructure / HAProxy**: New configuration `docker/haproxy/haproxy.cfg` and cert script `docker/haproxy/generate-certs.sh`.
- **Build / Lifecycle**: Root `build.gradle.kts` updated with `generateComposeTls` task and dependencies.
- **Git**: `.gitignore` updated to exclude generated `.pem` and `.key` files in `docker/haproxy/certs/`.
- **Documentation**: `README.md` updated with HAProxy ingress details, port mappings (8080/8443), certificate generation workflows, and compose lifecycle commands.
- **Runtime Compatibility**: Backwards-compatible entrypoint on `http://localhost:8080` preserved through HAProxy forwarding, with added `https://localhost:8443` HTTPS support.
