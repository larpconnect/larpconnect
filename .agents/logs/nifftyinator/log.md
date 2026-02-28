## 2025-02-27 - [Variable and Error Catch Grouping]

**Learning:** It's quite common for java files in this project to define a variable before assigning it, especially with things like Java Optional, Guice and Vert.x. It's also common to have multiple error catch clauses of the same action.

**Action:** Whenever I encounter variables that are instantly assigned, I should use `var`. Also when I encounter consecutive try-catch clauses that perform the same logging/error operation, I should group them using `|`. E.g. `catch (ExecutionException | TimeoutException e)`.
