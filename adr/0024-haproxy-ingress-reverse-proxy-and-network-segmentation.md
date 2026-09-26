# 0024: HAProxy Ingress Reverse Proxy and Network Segmentation

## Status

Accepted

## Date

2026-09-26

## Context

Project **Njall** provides a multi-container Docker Compose deployment ([0007: Docker Compose Multi-Container Orchestration and Staged Packaging](0007-docker-compose-and-container-orchestration.md)) where the application **Server** container previously exposed port 8080 directly on the host interface. Direct exposure presents architectural limitations:
1. No edge TLS termination for encrypted local development.
2. Inability to enforce network isolation preventing unauthorized clients on the host from bypassing ingress policies.
3. Internal W3C `traceparent` headers injected by the backend Pekko HTTP tracing directive ([0017: OpenTelemetry SDK and Logback MDC Tracing Architecture](0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md)) leak internal trace state to external consumers.

## Considered Options

- **Option 1: HAProxy Ingress Gateway with Docker Compose Network Segmentation** (Selected)
- **Option 2: Direct Application-Level TLS and Header Filtering in Pekko HTTP** (Rejected: complicates JVM server configuration, duplicates edge concerns in application code, and prevents independent proxy scaling)
- **Option 3: Nginx Ingress Gateway** (Rejected: HAProxy is the designated proxy technology in the project tech stack specification)

## Decision

1. **Deploy HAProxy Ingress Service**: Add an `haproxy` service using `haproxy:3.1-alpine` binding `:8080` (plain HTTP) and `:8443` (TLS with local development certificate) to route traffic to `server:8080`.
2. **Network Segmentation**: Define two Docker bridge networks:
   - `edge`: Connects only `haproxy` and `server`.
   - `internal`: Connects `server`, `migrate`, and `postgres`.
   Remove host port publishing from the `server` container so it is unreachable from the host directly.
3. **Response Header Stripping**: Configure HAProxy to delete the `traceparent` header (`http-response del-header traceparent`) before returning responses to downstream clients.
4. **Active Backend Health Monitoring**: Configure HAProxy to perform HTTP health checks against `/api/admin/v1/health` to route only to healthy application instances.
5. **Self-Signed TLS Lifecycle**: Author `docker/haproxy/generate-certs.sh` to generate `docker/haproxy/certs/haproxy.pem` if absent. Wire a `generateComposeTls` Gradle task into `composeStart`, and wipe generated certificates during `composeStopClean`. Exclude `.pem` and `.key` files from Git tracking.

## Consequences

- **Positive**: Clean separation of edge concerns from the Java application runtime; full TLS support on port 8443; internal trace identifiers isolated from external clients; network-level isolation protects backend services.
- **Negative**: Adds an additional container to the Docker Compose topology; requires OpenSSL (available in WSL/Linux) to generate self-signed certificates on initial boot.
