## Why

In **Njall**, studios and users interact through federated identities capable of publishing and receiving messages in accordance with **FeatherPub** protocols. While the system currently models individuals, events, locations, and collections, it lacks a first-class identity and messaging primitive representing an **Actor**. Introducing tenanted **Actor** entities with federated endpoints and automatically provisioned inbox and outbox **Collections** expands the **User plane** to establish verifiable identities backed by Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS). To avoid collision with Apache Pekko concurrency primitives in the Java codebase, all internal domain components and **DAO** abstractions are designated as **Agent**.

## What Changes

- Introduce Flyway migration `V11__actors.sql` in the **Data plane**:
  - `njall_users.actors`: Common Table Inheritance subtype table inheriting from `njall_users.entities` via composite primary key `(tenant_id, id)` and foreign key to `entities(tenant_id, id) ON DELETE CASCADE`.
  - Add composite foreign key `CONSTRAINT fk_actors_owner FOREIGN KEY (tenant_id, owner_id) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE` supporting a 1:N relationship from an owner **Entity** (such as an individual) to multiple **Actor** personas.
  - Add case-insensitive unique B-tree index `idx_actors_tenant_username` on `(tenant_id, lower(preferred_username))` enforcing permanent username reservation per **Studio** **Tenant**.
  - Add index `idx_actors_owner` on `(tenant_id, owner_id DESC)` for fast owner-based actor lookups.
  - `njall_users.actor_endpoints`: Subordinate table storing federated protocol endpoints with composite primary key `(tenant_id, actor_id, name)` and foreign key `(tenant_id, actor_id)` referencing `njall_users.actors (tenant_id, id) ON DELETE CASCADE`.
  - Add check constraint `CONSTRAINT chk_actor_endpoint_name CHECK (name ~ '^[a-zA-Z0-9]+$')` enforcing alphanumeric names of length under 32 characters.
  - Enable PostgreSQL Row-Level Security (RLS) on both `actors` and `actor_endpoints` with tenant isolation policies for `njall_users` and bypass policies for `njall_admin`.
- Provide data layer mappings in `:data`:
  - `Agent` and `AgentEndpoint`: Immutable domain records implementing `DatabaseObject`.
  - `AgentEntity` and `AgentEndpointEntity`: JPA entities mapping tables with composite key classes.
  - `AgentDAO` and `DefaultAgentDAO`: Data access objects providing tenanted CRUD, endpoint upsert (`:addEndpoint` overwriting existing names), endpoint removal (`:removeEndpoint`), and automatic provisioning of paired inbox and outbox **Collections** upon **Actor** creation under active `@NjallUsers` sessions.
- Expose tenanted HTTP REST endpoints in `:api`:
  - `POST   /api/studios/{studio-id}/v1/actors`: Create an **Actor** with server-generated UUIDv7, validating non-blank `preferredUsername`, existing non-deleted `ownerId`, and automatically provisioning inbox and outbox collections. Returns HTTP 201 Created.
  - `GET    /api/studios/{studio-id}/v1/actors/{actor-id}`: Retrieve an **Actor** by ID, returning metadata, inbox URL, outbox URL, and endpoints map. Returns HTTP 200 OK or HTTP 404 Not Found.
  - `PATCH  /api/studios/{studio-id}/v1/actors/{actor-id}`: Apply partial updates (`preferredUsername`, `summary`) adhering to Google AIP-134. Returns HTTP 200 OK, HTTP 404 Not Found, or HTTP 409 Conflict.
  - `DELETE /api/studios/{studio-id}/v1/actors/{actor-id}`: Soft-delete an **Actor** via `entities.deleted_on`, cascading soft deletion to associated inbox and outbox collections. Returns HTTP 204 No Content or HTTP 404 Not Found.
  - `GET    /api/studios/{studio-id}/v1/actors/{actor-id}/inbox`: Retrieve an expanded view of the actor's inbox collection. Returns HTTP 200 OK or HTTP 404 Not Found.
  - `GET    /api/studios/{studio-id}/v1/actors/{actor-id}/outbox`: Retrieve an expanded view of the actor's outbox collection. Returns HTTP 200 OK or HTTP 404 Not Found.
  - `POST   /api/studios/{studio-id}/v1/actors/{actor-id}/inbox`: Accept a new JSON object payload, returning HTTP 201 Created.
  - `POST   /api/studios/{studio-id}/v1/actors/{actor-id}/outbox`: Accept a new JSON object payload, returning HTTP 201 Created.
  - `POST   /api/studios/{studio-id}/v1/actors/{actor-id}:addEndpoint`: AIP-136 custom method to upsert an endpoint (overwriting if name exists). Returns HTTP 204 No Content.
  - `POST   /api/studios/{studio-id}/v1/actors/{actor-id}:removeEndpoint`: AIP-136 custom method to remove an endpoint by name. Returns HTTP 204 No Content.
- Implement Pekko Typed actor `AgentActor` and HTTP route `AgentRoute` in `:api` mounted into `StudiosRoute`.
- Document endpoints and payload schemas in `openapi.yaml`.

## Capabilities

### New Capabilities
- `user-studio-actors-api`: Defines tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/actors` for creating, retrieving, updating, soft-deleting, managing endpoints, and interacting with inbox and outbox collections for studio **Actor** identities using Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS).

### Modified Capabilities
None.

## Impact

- **Database**: Adds migration `V11__actors.sql` defining `njall_users.actors` and `njall_users.actor_endpoints` with CTI foreign keys, case-insensitive uniqueness indices, check constraints, and RLS policies.
- **Data Module (`:data`)**: Adds `Agent`, `AgentEndpoint`, `AgentEntity`, `AgentEndpointEntity`, `AgentDAO`, and `DefaultAgentDAO`, registered in `StudiosDaoModule`.
- **API Module (`:api`)**: Adds `AgentRoute`, `AgentActor`, command protocols, request/response DTOs, and updates `openapi.yaml`.
- **Integration Module (`:integration`)**: Adds Cucumber feature specifications asserting route validation, endpoint upsert/removal, inbox/outbox endpoints, and data persistence boundaries.
