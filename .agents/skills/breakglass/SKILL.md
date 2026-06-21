---
name: breakglass
description: If stuck on how to resolve code quality issues from Spotless, Checkstyle, SpotBugs, JaCoCo, ErrorProne, PMD, ArchUnit, user requests, or any other source.
---

# Skill: Handling being Stuck

## Specific Guidance

### Cyclomatic Complexity or NPath Issues

Strategies:

- **Breaking up logic**: Breaking blocks of code, especially blocks of code with
  internal control logic, out into new methods, so instead of complex code
  blocks you end up with:

  ```
  public void method() {
    if (fooCondition) {
    fooLogic();
    } else if (barCondition) {
    barLogic();
    } else {
    bazLogic();
    }
  }
  ```

  Each of `fooLogic()`, `barLogic()`, and `bazLogic()` may have their own
  control structures (loops etc), but this reduces their complexity noticeably.
  If needed for testing purposes you can also put these behind an interface and
  then mock that interface.

- **Moving lambdas into functions**: Turning complex lambda statements into methods
  and passing those methods instead, or having a function that creates a function to
  pass in, often reduces the complexity of a method greatly.

- **Removing Defensive Branches**: It is not unusual to add "defensive branches"
  that are impossible to reach. Confirm that they are impossible to reach via
  `./gradlew check` and by mentally reasoning through the function, but if they
  are impossible to reach you may just be able to remove them as dead code.

- **Law of Demeter**: Also called the "one dot rule." If a method is calling
  functions on things that it got from functions of other things, that's a
  strong signal that you need multiple methods. Even something as simple as:

  ```
    public void method() {
    var tmp = foo.fetchValue();
    var bar = processFoo(tmp);
    // etc
    }

    private Bar processFoo(Foo tmp) {
    return tmp.bar();
    }
  ```

  Note that this doesn't usually apply when working with **record** objects or other kinds of DTO, but
  it does apply when working when objects that involve actual application logic.

- **Use Guice**: Do not use `new` if you can avoid it, instead inject the dependency, or a factory to build the dependency, via Guice. Use `AssistedInject` and guice factories for constructing more complex objects at runtime. These factories can then be mocked. 

### Cannot Instantiate Package-Private Constructor

Almost always in these cases it is because you are expected to either:

1. `install` a different `Module` than this one (if this is a module)
2. Get this object strictly from an injector (via guice).

DO NOT UNDER ANY CIRCUMSTANCES use reflection to create the object instead.

### Public constructor is disallowed

You are expected to bind this class to an interface using guice. In some circumstances
a factory pattern may be correct, in which case use `FactoryModuleBuilder`.

### Test Coverage Too Low

Ensure that all branches—especially error paths—have tests that cover them. Some additional specific techniques:

* Break out part of the class that's "difficult to test" into another (package-private) object and inject that object.
  This additional object often will make it easier to test.
* Inject interfaces for elements that have any nondeterminism built into them. For example `Clock` and `RandomGenerator`.
  This will allow you to mock them and better control the values they generate during execution.
* Inject things that you would otherwise get from static methods, e.g., `Runtime`.
