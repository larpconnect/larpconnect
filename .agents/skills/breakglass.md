# Skill: Handling being Stuck

## Domain Context

This skill handles specific problem cases that you may be encountering.

## Technical Constraints

There are several technical constraints in this project that we have found AI
agents consistently have trouble with. This is a guide to help you navigate
those.

To start:

If your problem is related to:

- **Instantiation**: Read `.agents/skills/guice_di.md`
- **Test Coverage**: Read `.agents/skills/testing.md`
- **Context Annotations**: Read `.agents/skills/context.md`
- **Data Files**: Read `.agents/skills/data.md`

Read these regardless of if you have met the trigger conditions specified in
`.jules/SKILLS.md`

If none of the above apply or if your problem is still not solved, move to
**Specific Guidance** here.

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

  Note that this doesn't usually apply when working with **record** objects, but
  it does apply when working when objects that involve actual application logic.
