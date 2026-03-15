## 2026-03-04 - Server Main Verticle Exception Cleanup

**Learning:** When cleaning up WebServerVerticle's start block, changing the `try finally` block with `close(in, true)` to a `try-with-resources` cleans up a lot of code, and we can also use `var` and remove redundant exception handling. Also, in `MonotonicTimeService`, changing `Duration elapsed = stopwatch.elapsed();` to `var elapsed` makes it cleaner. Guice inject can be simplified from `com.google.inject.Inject` to `jakarta.inject.Inject`.

**Action:** Apply cleaner try-with-resources patterns and combine catch blocks where possible to make verticle start up code much leaner and readable. Apply `var` for local variables. Change Guice `@Inject` to Jakarta.
