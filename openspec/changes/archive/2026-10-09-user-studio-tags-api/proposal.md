## Why

**Studios** require the ability to organize, categorize, and discover content using first-class **Hashtag** resources. In **Njall**, hashtags represent federated metadata modeled as specialized **Link** subtypes within the Common Table Inheritance (CTI) hierarchy. Introducing tenanted **Hashtag** management under the studio resource tree enables **Studio** administrators and clients to create, list, inspect, modify, bulk-provision, and soft-delete hashtags with tenant isolation and case-insensitive, case-preserving resolution.

## What Changes

- Add database migration `V6__hashtags.sql` defining `njall_users.hashtags` and `njall_users.hashtags_entity` with Row-Level Security (RLS) policies for both `njall_users` and `njall_admin`, table permissions, and a unique functional index on `(tenant_id DESC, LOWER(tag))`.
- Introduce domain records and data access objects in `:data` (`Hashtag`, `HashtagEntity`, `HashtagDAO`, `DefaultHashtagDAO`) supporting case-insensitive lookups, idempotent creation, bulk creation, AIP-134 partial updates, and soft deletion.
- Add Apache Pekko **Actor** and HTTP route components in `:api` under `/api/studios/{studio-id}/v1/tags`:
  - `GET /api/studios/{studio-id}/v1/tags`: List all active hashtags for the studio.
  - `POST /api/studios/{studio-id}/v1/tags`: Idempotently create or resolve a single hashtag.
  - `POST /api/studios/{studio-id}/v1/tags:batchCreate`: Idempotently provision multiple hashtags in bulk conforming to Google AIP-233.
  - `GET /api/studios/{studio-id}/v1/tags/{tag-id}`: Retrieve a hashtag by either its public UUID or case-insensitive string tag name.
  - `PATCH /api/studios/{studio-id}/v1/tags/{tag-id}`: Partially update a hashtag (summary, casing) conforming to Google AIP-134.
  - `DELETE /api/studios/{studio-id}/v1/tags/{tag-id}`: Soft delete a hashtag by marking `entities.deleted_on`.
- Update OpenAPI specification `api/src/main/resources/openapi.yaml` with schema definitions and route paths for tenanted hashtag endpoints.
- Provide end-to-end Cucumber acceptance scenarios in `:integration` covering HTTP validation, tenant isolation, idempotent creation, and dual-lookup mechanics.

## Capabilities

### New Capabilities
- `user-studio-tags-api`: Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/tags` for creating, retrieving, listing, updating, bulk-provisioning (`:batchCreate`), and soft-deleting studio hashtag entities as first-class objects extending links via Common Table Inheritance (CTI) with PostgreSQL Row-Level Security (RLS) isolation.

### Modified Capabilities
<!-- None -->

## Impact

- **Database**: Adds `njall_users.hashtags` and `njall_users.hashtags_entity` tables, functional unique index on `(tenant_id DESC, LOWER(tag))`, and RLS policies in `V6__hashtags.sql`.
- **Data Module (`:data`)**: Adds `Hashtag` record, `HashtagEntity` JPA entity, `HashtagDAO`, `DefaultHashtagDAO`, and Guice provider registration.
- **API Module (`:api`)**: Adds `TagCommand`, `TagActorResponse`, `TagActor`, `TagsRoute`, `TagValidation`, `TagsModule`, and request/response DTOs (`CreateTagRequest`, `BatchCreateTagsRequest`, `BatchCreateTagsResponse`, `UpdateTagRequest`, `TagResponse`).
- **OpenAPI**: Adds schema definitions (`Tag`, `CreateTagRequest`, `BatchCreateTagsRequest`, `BatchCreateTagsResponse`, `UpdateTagRequest`) and path definitions under `/api/studios/{studio-id}/v1/tags`.
- **Integration Module (`:integration`)**: Adds `user_studio_tags_api.feature` and step definitions verifying API contracts, case-insensitive resolution, and RLS tenant boundaries.
