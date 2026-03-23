# Skill: API Creation

## Purpose

This skill provides guidance on how to write effective APIs.

## Technical Constraints

- APIs are managed using OpenAPI (version 3) but otherwise follow the guidance of the [AIPs](https://google.aip.dev). Load documentation on AIP and on OpenAPI from context7 when interacting with the `openapi.yaml` file.
- Payloads are specified using protobuf and then documented using OpenAPI.
- Security is paramount when writing good APIs. Everything needs to be sanitized and validated.
- Everything should be both unit tested and (separately) integration tested. Integration tests should be written using
  cucumber.

## Specific Guidance

### Structure

1. Protobuf is used to define the payloads
2. OpenAPI is used to specify the API and any constraints
3. Vert.x has a router in `:server` that does validation and that is used to forward the message to a verticle in
   `:api`.
4. The verticle in `:api` may forward it to other verticles, first converting the JSON payload into Protobuf (if it
   has not been converted already).
5. Final conversion to the correct output format is done in the `:server` verticle.

### Telemetry

The system uses a standard _telemetry_ pattern where requests have three parts:

1. A _trace_ id that stays the same throughout the request.
2. A _span_ id that changes at each step.
3. A _parent span_ id that represents the previous step.

These are preserved in the logs via MDC and are used to trace requests through the system.

### Tenancy

This system is a multi-single tenant design and uses the `server` object to differentiate payloads. The
`server` is also logged. 

### Base Paths

There are four fundamental "base paths" used by the system.

1. `/`: Used for `.well-known` and a variety of other useful endpoints.
2. `/admin`: Used for administrative things that affect the entire system.
3. `/servers/{server-id}`: Represents paths associated with a multi-single-tenant design. Each `{server-id}` is associated
   with a separate `schema` in the database and should have its own set of verticles for working with the various paths.
4. `/tools`: Represents generally useful functionality that is not tied to specific servers.

If a `v1` is used for versioning it generally comes _after_ the base path. So therefore:

* `/v1/nodeinfo`
* `/admin/v1/accounts`
* `/servers/{server-id}/v1/nodeinfo`
* `/tools/v1`

Responses are by default in JSON unless otherwise documented/requested.

### Logging

That a request has occurred and the path of the request may be logged at `INFO`. Non-fatal
errors may be logged at `WARN`. Parameters of requests and headers should generally not be logged without special permission.

### Basic Sanitization

Headers and arguments should all be sanitized and validated. Both against the API spec—which should be enforced strictly—
as well as against a basic check removing all non-printable characters. Input from users should NEVER be trusted even after
basic sanitization.

