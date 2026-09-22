## 1. Refactor Data Persistence Layer to Inject Provider<SessionFactory>

- [x] 1.1 In `data/src/main/java/com/larpconnect/njall/data/dao/DefaultServerDAO.java`, update the constructor to inject `@NjallAdmin Provider<SessionFactory> sessionFactoryProvider`, store it in a `private final Provider<SessionFactory>`, and update `findById` and `list` to invoke `sessionFactoryProvider.get().openSession()`.
- [x] 1.2 In `data/src/test/java/com/larpconnect/njall/data/dao/DefaultServerDAOTest.java`, update test initialization to pass a provider lambda `() -> sessionFactory` into `new DefaultServerDAO(...)`.
- [x] 1.3 Verify `:data` module quality gates pass with `wsl ./gradlew :data:check`.

## 2. Implement ArchUnit Rule in Integration Module

- [x] 2.1 In `integration/build.gradle.kts`, add dependency `testImplementation(libs.archunit.junit5)`.
- [x] 2.2 In `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java`, implement the ArchUnit test class analyzing `com.larpconnect.njall..` non-test classes to enforce that no constructor, method, or field annotated with `@Inject` (or method annotated with `@Provides`) declares a parameter or field of raw type `org.hibernate.SessionFactory`.
- [x] 2.3 Verify `:integration` module quality gates and architecture tests pass with `wsl ./gradlew :integration:check`.

## 3. End-to-End Verification & OpenSpec Validation

- [x] 3.1 Run full project verification suite across all modules with `wsl ./gradlew check build`.
- [x] 3.2 Run `wsl openspec validate archunit-sessionfactory-provider --type change --strict` to verify OpenSpec artifact consistency.
