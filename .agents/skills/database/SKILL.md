---
name: database-interaction
description: Guidance on how to manage the data layer
---

# Skill: Database Interactions


## Technical Constraints

- Use PSQL 18+
- All SQL should be written using either `HQL` (for inline code) or `pgplsql` (in scripts).
- The database interaction layer is stored in `:data`.
- We use a multi-single-tenant system. The tenant id is associated with a specific table and column. 
- Use `flyway` for database management. 

## Specific Guidance

### Database Design

- Everything is kept 3+ NF and generally 5NF except for materialized views, which can be 2NF.
- The database on the whole follows a CTI (Class Table Inheritance) pattern with a central `entities` table.
- Table names are generally _plural_ in their names. The DTO objects derived from them are generally _singular_ in their names.
- In general we rely on synthetic IDs based on a UUIDv7 that the database generates.

### Multitenancy

The system is a multi-single-tenant design, with the `tenantId` associated with the column. Enforcement is via row level security.

### Pattern for Objects

Objects generally will have:

1. A DTO representing the object from the database. This includes an interface as
   well as an implementation.
2. A DAO that allows loading and writing a given object. These include both an interface
   as well as an implementation.
3. Bindings in Guice.
