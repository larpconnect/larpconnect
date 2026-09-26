# Design: HAProxy Ingress Reverse Proxy and Network Segmentation

## Context

In **Njall**, the **Server** container currently binds port 8080 directly to the host interface (`0.0.0.0:8080`), exposing the internal Pekko HTTP engine directly to callers. While convenient during early development, direct exposure prevents edge TLS termination, bypasses centralized proxy logging, exposes internal W3C `traceparent` headers directly to downstream clients, and allows unrestricted network access from the host.

In-force decisions ([0007: Docker Compose Multi-Container Orchestration and Staged Packaging](../../adr/0007-docker-compose-and-container-orchestration.md) and [0015: Docker Compose Secret Derivation and Isolation](../../adr/0015-docker-compose-secret-derivation-and-isolation.md)) establish a coordinated Docker Compose environment with fast host-staged packaging via `:server:installDist` and lifecycle tasks in `build.gradle.kts`. This design extends that foundation by introducing HAProxy 3.1 as an ingress gateway and establishing network segmentation between the public edge and internal subsystems.

## Goals / Non-Goals

**Goals:**
- Provide an HAProxy reverse proxy fronting the Pekko HTTP **Server**, exposing port 8080 (plain HTTP) and port 8443 (TLS).
- Enforce network isolation in Docker Compose so that the **Server** container does not publish host ports and accepts HTTP traffic solely through HAProxy.
- Strip internal W3C `traceparent` headers from responses before delivering them to downstream clients.
- Provide a self-contained POSIX shell script (`docker/haproxy/generate-certs.sh`) to automatically generate a self-signed RSA 2048-bit certificate bundle (`haproxy.pem`) if absent.
- Integrate certificate generation into root Gradle lifecycle tasks (`generateComposeTls`, `composeStart`, `composeStopClean`).
- Maintain active backend health checking via HAProxy querying `/api/admin/v1/health`.

**Non-Goals:**
- Implementing an application-level shared secret header or mutual TLS gate inside Pekko HTTP (deferred as a future defense-in-depth iteration).
- Production ACME / Let's Encrypt automated certificate issuance (local self-signed certificates fulfill local development requirements).
- Changing database migrations or application packaging workflows.

## Architectural Topology (C4 Container Diagram)

```
+-----------------------------------------------------------------------------+
| Host Network (Developer Machine / External Consumer)                        |
|                                                                             |
|   Client HTTP (http://localhost:8080)                                       |
|   Client HTTPS (https://localhost:8443)                                     |
|         |                     |                                             |
+---------|---------------------|---------------------------------------------+
          |                     |
          v (port 8080)         v (port 8443 TLS)
+-----------------------------------------------------------------------------+
| Docker "edge" Network                                                       |
|                                                                             |
|   +---------------------------------------------------------------------+   |
|   | Container: larpconnect-haproxy (HAProxy 3.1-alpine)                 |   |
|   | - Binds :8080 and :8443 (ssl crt haproxy.pem)                       |   |
|   | - Strips 'traceparent' header on HTTP responses                     |   |
|   | - Active healthcheck: GET /api/admin/v1/health                      |   |
|   +----------------------------------+----------------------------------+   |
|                                      |                                      |
|                                      | proxy HTTP to http://server:8080     |
|                                      v                                      |
|   +---------------------------------------------------------------------+   |
|   | Container: larpconnect-server (ghcr.io/rblaine95/eclipse-temurin:25)|   |
|   | - Pekko HTTP Engine bound to 0.0.0.0:8080                           |   |
|   | - NO host port published! Only reachable via Docker networks        |   |
+---|----------------------------------+--------------------------------------+
                                       |
+--------------------------------------|--------------------------------------+
| Docker "internal" Network            |                                      |
|                                      | JDBC                                 |
|                                      v                                      |
|   +---------------------------------------------------------------------+   |
|   | Container: larpconnect-postgres (PostGIS 18-3.6-alpine)             |   |
|   | - Port 5432 internal (optional dev port 5433 to host)               |   |
|   +---------------------------------------------------------------------+   |
|                                      ^                                      |
|                                      | JDBC (run once)                      |
|   +----------------------------------+----------------------------------+   |
|   | Container: larpconnect-migrate (ephemeral exit 0)                   |   |
|   +---------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------+
```

## Decisions

### 1. Dual-Network Segmentation in Docker Compose
- **Decision**: Define two user-defined bridge networks: `edge` and `internal`.
  - `haproxy` is attached only to `edge`.
  - `server` is attached to both `edge` and `internal`.
  - `postgres` and `migrate` are attached only to `internal`.
  - Remove `ports:` from `server` service in `docker-compose.yml`.
- **Alternatives Considered**:
  - *Keep single flat network*: Rejected because any container could bypass HAProxy and direct host mapping would remain possible.
  - *Internal mTLS between HAProxy and Pekko*: Rejected as unnecessary operational overhead for local container orchestration.

### 2. Unified Ingress Frontend with Dynamic TLS & Header Stripping
- **Decision**: Configure `docker/haproxy/haproxy.cfg` with global and defaults settings enforcing `maxconn 100`, modern TLS, timeouts, and a unified frontend `fe_ingress`:
  ```haproxy
  global
      log stdout format raw local0 info
      maxconn 100
      ssl-default-bind-ciphersuites TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256
      ssl-default-bind-options ssl-min-ver TLSv1.2 no-tls-tickets

  defaults
      log     global
      mode    http
      option  httplog
      option  dontlognull
      retries 3
      maxconn 100
      timeout connect        5s
      timeout client        30s
      timeout server        30s
      timeout http-request  10s
      timeout http-keep-alive 10s

  frontend fe_ingress
      bind :8080
      bind :8443 ssl crt /usr/local/etc/haproxy/certs/haproxy.pem
      maxconn 100
      option forwardfor
      http-request set-header X-Forwarded-Proto https if { ssl_fc }
      http-request set-header X-Forwarded-Proto http unless { ssl_fc }
      default_backend be_server
      http-response del-header traceparent
  ```
- **Rationale**: Setting `maxconn 100` establishes a safe connection baseline for local development. Setting `X-Forwarded-Proto` informs downstream actors whether the edge connection was encrypted. The `del-header traceparent` directive satisfies the observability boundary requirement, ensuring internal trace identifiers are not leaked to external clients.

### 3. POSIX Script + Gradle Lifecycle Integration for TLS
- **Decision**: Author `docker/haproxy/generate-certs.sh` using `openssl req -x509` to generate `docker/haproxy/certs/haproxy.pem` if absent. Register a `generateComposeTls` Gradle task in `build.gradle.kts` that invokes this script before launch.
- **Alternatives Considered**:
  - *Generate certificates inside Java/Kotlin DSL*: Rejected due to complex JDK X.509 API restrictions and missing standard PEM exporter in JDK runtime without external BouncyCastle dependencies.
  - *Pure shell script wrapper around docker compose*: Rejected because Gradle must remain the unified developer entrypoint, orchestrating `:server:installDist` incremental builds and task dependency ordering.

### 4. Active Backend Health Checking
- **Decision**: Configure HAProxy backend `be_server` with:
  ```haproxy
  backend be_server
      mode http
      option httpchk
      http-check send meth GET uri /api/admin/v1/health ver HTTP/1.1 hdr Host localhost
      http-check expect status 200
      server app server:8080 check inter 2s fall 3 rise 2
  ```
- **Rationale**: Reuses the Dropwizard/Pekko healthcheck probe at `/api/admin/v1/health` established in the `admin-healthcheck` specification, ensuring HAProxy only routes traffic when the Pekko **Actor** runtime is healthy.

## Risks / Trade-offs

- **[Risk] Missing OpenSSL on non-WSL Windows Host** -> *Mitigation*: Under project standards (`njall-tool-guide`), developer commands run via WSL where OpenSSL 3.x is pre-installed. The shell script checks for `openssl` and errors clearly if absent.
- **[Risk] Browser Self-Signed Certificate Warnings on 8443** -> *Mitigation*: The certificate includes `subjectAltName=DNS:localhost,IP:127.0.0.1`. Developers can accept the local self-signed certificate in the browser or use plain HTTP on port 8080 for non-TLS testing.
- **[Risk] Host-to-Server Direct Access Attempted by Developers** -> *Mitigation*: Removing `ports:` from `server` causes immediate connection refused on `localhost:8080` if hitting `server` directly; all traffic is channeled through HAProxy.

## Migration Plan

1. Create `docker/haproxy/haproxy.cfg` and `docker/haproxy/generate-certs.sh`.
2. Update `docker-compose.yml`: add `haproxy` service, declare `edge` and `internal` networks, attach services appropriately, and remove `ports:` on `server`.
3. Update `build.gradle.kts`: add `generateComposeTls` task, link task dependencies to `composeStart`, and update `composeStopClean` to wipe `haproxy.pem`.
4. Update `.gitignore`: add `docker/haproxy/certs/*.pem` and `docker/haproxy/certs/*.key`.
5. Update `README.md` to document the ingress architecture, port mappings, and lifecycle workflows.
6. Verify `./gradlew composeStart` boots cleanly and responds to HTTP on 8080 and HTTPS on 8443 with `traceparent` removed, and stops cleanly with `./gradlew composeStopClean`.

## Open Questions

- *Application-level token gate*: Should `larpconnect-server` inspect a shared header (`X-Proxy-Secret`) to enforce defense-in-depth against unauthorized containers on the `edge` network? Deferred as an explicit backlog item for future security hardening.
