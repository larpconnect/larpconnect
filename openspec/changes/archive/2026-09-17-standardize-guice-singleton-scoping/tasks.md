## 1. Clean Up Class Scopes in Data Module

- [x] 1.1 Remove `@Singleton` and `import com.google.inject.Singleton;` from `ActiveSessionFactories.java` in `:data`
- [x] 1.2 Run `./gradlew :data:check` to verify `:data` module quality gates pass cleanly

## 2. Standardize Module Scoping in Server Module

- [x] 2.1 Remove `@Singleton` and `import com.google.inject.Singleton;` from `DefaultHttpServerService.java` in `:server`
- [x] 2.2 Update `HttpServerModule.java` to import `com.google.inject.Singleton` and bind `HttpServerService` to `DefaultHttpServerService` with `.in(Singleton.class)`
- [x] 2.3 Add assertion in `HttpServerModuleTest.java` verifying that repeated injections of `HttpServerService` return the identical instance
- [x] 2.4 Run `./gradlew :server:check` to verify `:server` module quality gates pass cleanly

## 3. Update Architecture Documentation

- [x] 3.1 Update `.agents/skills/guice/SKILL.md` to document the prohibition of class-level `@Singleton` annotations and the mandate for module-level `.in(Singleton.class)` scoping

## 4. System Verification and Specification Validation

- [x] 4.1 Run `./gradlew check build` across all modules to verify spotless, checkstyle, spotbugs, and JaCoCo coverage thresholds
- [x] 4.2 Run `openspec validate standardize-guice-singleton-scoping --type change --strict` to verify OpenSpec schema adherence
