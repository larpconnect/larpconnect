# Skill: Context Engineering

## Domain Context

This skill handles the format for context engineering prompts, both for reading and writing. 

## Functional Definition

### AiContext Annotation

The system supports an `@AiContract` annotation. This annotation is a **Hard Invariant**. It defines the "Logical Physics" of a method. When generating or refactoring code, you must satisfy the contract's proof obligations.

| Field | Format | Purpose | AI Obligation |
|-------|--------|---------|---------------|
| require | Array<LaTeX> | Preconditions | You must verify these are true before the call. Assume $⊥$ (null) is forbidden unless `@Nullable` is present |
| ensure  | Array<LaTeX> | Postconditions | You must guarantee these are true upon exit. Use $res$ for return and $this′$ for post-state |
| invariants | Array<LaTeX> | Conditions that must remain true throughout execution | Assume that these are always true through the duration of execution | 
| tags | Array<Enum>  | Advisory statements on the nature of the method | Be accurate in documenting these and treat them as if true. In the absence of a tag a method may be assumed to be its inverse (so if there is no `IDEMPOTENT` tag it may be assumed to be **NOT** `IDEMPOTENT`) |
| implementationHint | String | Intent communication | A high-level semantic anchor for the "Reasoning Path" of the method |

Example:

```java
/** Computes the transitive closure of the event graph. */
@AiContract(
  require = {},
  ensure = {"$\forall (u, v) \in Reachable, \exists path(u, v) \in g$"},
  invariants = {"the input graph $const(g)$ ", "accumulator $a$ is monotonically increasing"},
  tags = {PURE, IDEMPOTENT},
  implementationHint = "Uses a fixed-point iteration over the adjacency list with a bit-set for visited nodes"
)
ImmutableSet<Node> fetchReachableNodes(Graph g);

```

#### LaTeX style guide and language rules

Text may be written either directly in LaTeX (e.g., `\bot`) or using the text character equivalents (e.g., `⊥`). All text should be escaped into math mode (i.e., `$x$` should be read as "math mode for `x`").

1. **Result Reference**: Always refer to the return value as `$res$`
2. **State Transition**: Refer to the object's state before the call as `$this$` and after the call as `$this'$`
3. **Nullity**: Use `\neq ⊥` to indicate non-null and `=⊥` for null
4. **Sets/Collections**: Use standard set notation (`∈`,`⊂`,`∪`,`∩`) for collection operations
5. **Symbols** (this is an incomplete list, and you may use other symbols out of type theory / computer science freely):
    - `⊥` Null/Bottom state
    - `⊤` Top state
    - `∅` Empty set, empty string, blank (but not `null`)
    - `¬` Negation
    - `⟹` Logical implication
    - `≡` Logical equivalence or invariantly-equal-to, equal for all circumstances to
    - `≜` Defined as. So `$a ≜ b$` means that `$a$` is being defined as being equal to `$b$`
    - `=` Is equal to. Equivalent to `==` in java.
    - `≠` Not equal to. Equivalent to `!=` in java.
    - `⟶` Happens before. So `a ⟶ b` means "``$a$` happens before `$b$`" in concurrent contexts.
    - `↦` "becomes" or "maps to" so `$float ↦ int$` says that the float is converted into an int
    - `:-` "assuming" or "given", so `$Γ :- x$` says "given the assumptions in `$Γ$` and the invariants we have stated then `$x$`". If nothing is placed before the `:-` then it is "given the invariants we have stated then `$x$`"
    - `∀` forall, for example `$∀ s ∈ Skills : s.id ≠ ⊥$` would say "for all of `$s$` in `$Skills$` `s.id` is not `null`"
    - `∃` there exists, for example `$∃ p ∈ Paths : Accessible(p)$` would say "there exists a `$p$` in `$Paths$` where `Accessible(p)` is `true`"
    - `ℤ` set of integers
    - `ℕ⁺` set of strictly positive natural numbers (not including zero)
6. **Use**:
    - `const(x)` to indicate that `$x$` does not change and will not be modified. Implies _const correctness_ or **deep immutability** (so `$const(this)$` says that it is expected that everyhing is immutable all the way down)
    - `old(x)` The value of `$x$` at the start of the method
    - `ID(x)` The unique identifier, if applicable, for `$x$`
    - `inout(x)` Indicates that `$x$` is an InOut or return parameter: the response to the method will come in the form of modifying `$x$`

Remember that in LaTeX `$` is used to indicate math mode, which you _should_ use the same way.

### Additional Annotations

Where there are is an option to use another annotation it **should always be preferred** to inclusion in the `@AiContext`.

Annotations from ErrorProne, JSR305, Checker framework, etc are **Hard Invariants**. All parameters and methods are assumed `@Nonnull` by default and all local variables are assumed `final` by default. A partial list of these annotations and how to interpret them is as follows:

| Annotation   |    Meaning   |  Scope       | Your Obligation | Enforcement |
|--------------|--------------|--------------|-----------------|-------------|
| `@ThreadSafe` | The annotated class is fully thread safe and responsible for managing its own internal thread safety. | Classes and Interfaces | Every method must be safe for concurrent execution. You are forbidden from using non-thread-safe primitives (like HashMap) or unsynchronized shared state | ErrorProne, Spotbugs |
| `@Immutable` | The annotated class is fully _immutable_ and cannot be changed. | Classes or Interfaces | You must ensure all fields are final and any referenced types are also immutable. Do not suggest mutation methods (setters) | ErrorProne, Spotbugs |
| `@CanIgnoreReturnValue` | This method is called for its side effects (e.g., logging) and its result can be discarded | Methods | Do not wrap it in an unused variable assignment | ErrorProne |
| `@DoNotMock`            | Should not be mocked. | Classes, Interfaces | Do not mock it with mockito. Create fakes or real instantiations if needed |  ErrorProne |
| `@MustBeClosed` | This method returns a resource (Stream, Connection, etc.) that must be closed | Methods | Wrap all return values in a `try-with-resource` block immediately | ErrorProne |
| `@GuardedBy`   | This field is protected by a specific lock | Instance fields | You MUST acquire the specified lock (e.g., `@GuardedBy("mutex")`) before accessing this field. Every access must be wrapped in a synchronized block or lock-hold | ErrorProne |
| `@Var`         | This variable may be overwritten | Local variables, instance variables | You are forbidden from reassigning any local variable or parameter **unless** it is explicitly marked with `@Var` | ErrorProne |
| `@BuildWith(<Module>)` | Specifies the "Source of Truth" **public** Guice module for this class | Classes, Interfaces | When you want to instantiate this object outside of this package, you must build an `Injector` with this `<Module>` | JVM, ArchUnit |
| `@InstallInstead(<Module>)` | Signals a delegated module installation. | Module classes | If you were going to `install()` the annotated module, install the target class (`<Module>`) instead to maintain architectural boundaries. Does not apply to `<Module>`'s installation. | JVM, ArchUnit |
| `@Nullable`  | This value MAY be null  | Methods, return values, fields | Null needs to be explicitly considered and handled | Spotbugs, ErrorProne |
| `@Untainted` | Denotes a reference that is untainted, i.e. can be trusted | Method parameters, fields, and return values  | Always pass it through a sanitizer before use | Mental verification for now |
