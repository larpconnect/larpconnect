# ADR Review Manifest

- Status: completed
- Review date: 2026-09-26

## Review Summary

ADR review completed for this change.

## In-Force ADRs Reviewed

- `adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md`: Established package-level non-null defaults via JSpecify `@NullMarked`.
- `adr/0012-cli-command-option-null-encapsulation-and-optional-accessors.md`: Established CLI command option encapsulation returning `Optional<T>` to prevent leaking nulls.
- `adr/0019-static-nullity-enforcement-and-constructor-streamlining.md`: Streamlined constructors and eliminated defensive null checks across Guice bindings.
- `adr/0020-codebase-wide-null-check-and-npe-test-elimination.md`: Prohibited defensive null checks and NPE unit tests on non-null parameters.
- `adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md`: Established records as strict data carriers and prohibited static factory methods.
- `adr/0022-archunit-immutable-records-and-errorprone-enforcement.md`: Mandated `@Immutable` on all production records enforced via ArchUnit and ErrorProne.

## New Durable ADRs Created

- `adr/0023-record-component-nonnull-invariant-and-boundary-normalization.md`: Establishes the codebase-wide record component non-null invariant, the three substitution strategies (`Optional`, empty immutable collection, enum sentinel), constructor boundary normalization, and ArchUnit enforcement.
