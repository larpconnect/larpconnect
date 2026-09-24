# 0015: Docker Compose Secret Derivation and Local Environment Isolation

- Status: accepted, amends ADR-0007
- Date: 2026-09-23

## Context

ADR 0007 adopted Docker Compose for multi-container local runtime orchestration. In its initial implementation, default passwords for PostgreSQL superuser and application roles were hardcoded directly in `docker-compose.yml` and `docker/postgres/init/01-init.sql`.

Hardcoded credentials in tracked repository files trigger automated security scanners (e.g. Gitleaks, GitHub Secret Scanning), present credential leakage risks in process inspection and metadata, and prevent dynamic credential updates on fresh database initialization. Furthermore, lack of explicit `.gitignore` rules for environment configuration files risks accidental commits of developer secrets.

## Decision

1. **Ephemeral Master Secret Seed Generation**:
   - The root Gradle build (`composeUp`, `composeStart`) MUST check for a local `.env` file before invoking Docker Compose.
   - If missing, Gradle MUST generate a 16-byte cryptographically secure random hexadecimal seed using `java.security.SecureRandom` and persist it as `NJALL_DB_SECRET` into `.env`.
2. **Deterministic Role Credential Munging**:
   - `docker-compose.yml` MUST NOT contain hardcoded plaintext passwords.
   - Separate passwords MUST be derived from `NJALL_DB_SECRET` for each database role using deterministic prefixes:
     - Migration: `njall_migration_${NJALL_DB_SECRET}`
     - Admin Verticle: `njall_admin_${NJALL_DB_SECRET}`
     - User Verticle: `njall_users_${NJALL_DB_SECRET}`
     - System Verticle: `njall_system_${NJALL_DB_SECRET}`
     - Engine / Superuser: `postgres_${NJALL_DB_SECRET}`
3. **Dynamic Role Initialization via Entrypoint Script**:
   - Replace static `01-init.sql` with an executable `01-init.sh` shell script executed by the PostgreSQL container entrypoint.
   - The script MUST dynamically create roles using the derived credentials from `NJALL_DB_SECRET`.
4. **Environment Isolation and Git Exclusion**:
   - `.gitignore` MUST explicitly exclude `.env` and `.env.*` while allowing tracked `.env.example`.
   - A sanitized `.env.example` template MUST be committed to document environment variables.
5. **Volume and Credential Lifecycle Synchronization**:
   - `composeClean` MUST delete the local `.env` file when purging persistent PostgreSQL volumes (`docker compose down -v`), ensuring credential regeneration occurs cleanly on subsequent cluster startup.

## Consequences

### Positive
- Completely eliminates hardcoded passwords from tracked repository files, eliminating automated security scanner alerts.
- Enforces strict role credential isolation: accidental use of user credentials for admin operations fails fast with authentication errors.
- Preserves zero-friction developer experience: running `./gradlew composeStart` immediately generates secrets and launches the container stack.
- Guarantees that local environment files cannot be inadvertently committed to version control.

### Negative
- Direct invocation of `docker compose up` without prior `./gradlew composeStart` requires either running Gradle first or copying `.env.example` to `.env`.
