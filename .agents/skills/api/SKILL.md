---
name: api-excellence
description: For developing APIs, modifying API endpoints, adding API endpoints, or interacting with openapi.yaml.
---

# Skill: API Creation

## Purpose

This skill provides guidance on how to write effective APIs.

## Technical Constraints

- APIs are managed using OpenAPI (version 3) but otherwise follow the guidance of the [AIPs](https://google.aip.dev). Load documentation on AIP and on OpenAPI from context7 when interacting with the `openapi.yaml` file.
- Payloads are specified using protobuf and then documented using OpenAPI.
- Security is paramount when writing good APIs. Everything needs to be sanitized and validated.
- Everything should be both unit tested and (separately) integration tested. Integration tests should be written using
  cucumber and put in `:integration`

## Specific Guidance

### Structure

1. Payloads should be fully specified in a type safe manner inside of the OpenAPI spec. They should then have a fully annotated Jackson DTO object inside the code. 
2. OpenAPI is used to specify the API and any constraints
3. The API verticles live in `:api`.
4. The verticles in `:api` may forward it to other verticles outside of `:api`
5. Final conversion to the correct output format is done in the `:server` verticle.
6. For asynchronous calls the message should be put on a AMQP queue.

### Telemetry

The system uses a standard _telemetry_ pattern where requests have three parts:

1. A _trace_ id that stays the same throughout the request.
2. A _span_ id that changes at each step.
3. A _parent span_ id that represents the previous step.

These are preserved in the logs via MDC and are used to trace requests through the system.

### Tenancy

This system is a multi-single tenant design and uses the `studio` object to differentiate payloads. The
`studio` is also logged.

### Base Paths

There are four fundamental "base paths" used by the system.

1. `/`: Used for `.well-known` and a variety of other useful endpoints.
2. `/admin/v1`: Used for administrative things that affect the entire system.
3. `/studios/{studio-id}/v1`: Represents paths associated with a multi-single-tenant design. Each `{studio-id}` is associated
   with a separate `tenant-id` in the database and should have its own set of verticles for working with the various paths.

If a `v1` is used for versioning it generally comes _after_ the base path. So therefore:

* `/v1/nodeinfo`
* `/admin/v1/accounts`
* `/servers/{server-id}/v1/nodeinfo`

Responses are by default in JSON unless otherwise documented/requested.

### Logging

That a request has occurred and the path of the request may be logged at `INFO`. Non-fatal
errors may be logged at `WARN`. Parameters of requests and headers should generally not be logged without special permission.

### Basic Sanitization

Headers and arguments should all be sanitized and validated. Both against the API spec—which should be enforced strictly—
as well as against a basic check removing all non-printable characters. Input from users should NEVER be trusted even after
basic sanitization.
