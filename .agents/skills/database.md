# Skill: Database Interactions

## Purpose

This skill provides guidance on how to manage the database layer.

## Technical Constraints

- Use PSQL 18+
- All SQL should be written using either `HQL` (for inline code) or `pgplsql` (in scripts).
- The database interaction layer is stored in `:data`.
- We use a multi-single-tenant system. The tenant id is associated with a specific database schema.

## Specific Guidance

### Database Design

- Everything is kept 3+ NF and generally 5NF except for materialized views, which can be 2NF.
- The database on the whole follows a CTI (Class Table Inheritance) pattern with a central `entities` table.
- Table names are generally _plural_ in their names. The DTO objects derived from them are generally _singular_ in their names.
- Comments to the effect of `-- @ai-context: <text>` are contextual information specifically for AI agents.
- In general we rely on synthetic IDs based on a UUIDv7 that the database generates.

### Multitenancy

The system is a multi-single-tenant design, with the `tenantId` associated with the PSQL _schema_. 

The following schemas are allowed and will ultimately have their own initializations.

* `njall_base` represents the core utilities and common functions that are shared between tenants.
* `njall_admin` represents the basic administrative functions for the server. Things like spinning up and tearing down
  tenants.
* `njall_server_{server-id}` are a set of schemas (one for each `{server-id}`) that represent indivdiual tenants. These
  should all be kept up to date with the same database schema and have the same migrations applied. Server ids here are limited to
  the following regex: `^([a-z]+[a-z0-9]+){3,32}`
* `njall_analytics` represents common data analysis across the server as a whole.
 
### Pattern for Objects

Objects generally will have:

1. A DTO representing the object from the database. This includes an interface as
   well as an implementation.
2. A DAO that allows loading and writing a given object. These include both an interface
   as well as an implementation.
3. Bindings in Guice. 
