# ADR Review Manifest

- Status: completed
- Review date: 2026-09-25

## Review Summary

ADR review completed for this change. Record data carrier immutability invariants and static analysis compiler gates were evaluated against existing in-force architectural decisions, establishing a new durable ADR.

## In-Force ADRs Reviewed

- [0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md): Established ArchUnit architectural verification in `:integration`.
- [0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md): Mandated non-public visibility for `@Inject`/`@AssistedInject` constructors and down-or-out package dependencies.
- [0014-guice-injector-hardening-and-strict-bindings.md](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md): Enforced strict Guice module bindings and explicit provider declarations.
- [0020-codebase-wide-null-check-and-npe-test-elimination.md](../../../../adr/0020-codebase-wide-null-check-and-npe-test-elimination.md): Established codebase-wide nullity contracts and ErrorProne static compiler enforcement.
- [0021-archunit-record-factory-prohibition-and-pure-data-carriers.md](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md): Mandated that records in `com.larpconnect.njall..` must not declare static factory methods returning the record type or `Optional<Record>`, establishing pure data carriers.

## New Durable ADRs Created

- [0022-archunit-immutable-records-and-errorprone-enforcement.md](../../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md): Mandates that records in `com.larpconnect.njall..` must be annotated with ErrorProne `@Immutable`, configures compiler immutability error checking, and modernizes record collections to Guava immutable types.
