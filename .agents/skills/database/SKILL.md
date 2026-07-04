---
name: database-interaction
description: Guidance on how to manage the data layer
---

# Skill: Database Interactions


## Technical Constraints

- Use PSQL 18+. You may fully use features of PSQL up to version 18 without fear of backwards compatibility. 
- All SQL should be written using either `HQL` (for inline code) or `pgplsql` (in scripts).
- The database interaction layer is stored in `:data`.
- We use a multi-single-tenant system. The `tenant-id` is associated with a specific table or column. 
- Use `flyway` for database management, with the specific scripts in `resources`.

## Specific Guidance

### Database Design

- Everything is kept 3+ NF and generally 5NF except for materialized views, which can be 2NF.
- The database on the whole follows a CTI (Class Table Inheritance) pattern with a central `entities` table.
- Table names are generally _plural_ in their names. The DTO objects derived from them are generally _singular_ in their names.
- In general we rely on synthetic IDs based on a UUIDv7 that the database generates.

### Multitenancy

The system is a multi-single-tenant design, with the `tenant-id` associated with the column. Enforcement is via row level security. To implement this the `studio-id` is taken by the DAO, the `tenant-id` is retrieved from a `caffeine` cache, and then used to query the database.

### Pattern for Objects

Objects generally will have:

1. A DTO representing the object from the database. This includes an interface as
   well as an implementation.
2. A DAO that allows loading and writing a given object. These include both an interface
   as well as an implementation.
3. Bindings in Guice.

All DTO objects share sealed marker interfaces to represent their classification and type. All of the DAO objects similarly share a (curiously recursive template pattern) sealed interface with basic functionality represented. 

Hibernate is used as an ORM. 


