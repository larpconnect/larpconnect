# ADR Review Manifest

- Status: completed
- Review date: 2026-10-09

## Review Summary

ADR review completed for this change. The change addresses architectural package bloat and DAG enforcement, establishing an automated ArchUnit constraint and complete subpackage decomposition for `api.studios` and `data.dao`.

## In-Force ADRs Reviewed

- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md): Enforces zero upward dependencies in `com.larpconnect.njall..`.
- [0028: Admin Subpackage Decomposition and Module Hierarchy](../../../../adr/0028-admin-subpackage-decomposition-and-module-hierarchy.md): Established the domain-aligned subpackage decomposition and module hierarchy pattern.
- [0030: DAO Naming Conventions and Entity Immutability Standards](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md): Standards governing DAO interface and entity naming.

## New Durable ADRs Created

- [0032: Package Size Limits and Subpackage Decomposition](../../../../adr/0032-package-size-limits-and-subpackage-decomposition.md): Defines the 20-type package size limit, ArchUnit counting semantics, and the complete decomposition of `com.larpconnect.njall.api.studios` and `com.larpconnect.njall.data.dao`.
