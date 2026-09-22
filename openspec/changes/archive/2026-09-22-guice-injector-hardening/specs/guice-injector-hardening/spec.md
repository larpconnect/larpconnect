## ADDED Requirements

### Requirement: Disable Circular Proxies in Root ServerModule
The system SHALL install `Modules.disableCircularProxiesModule()` inside `ServerModule.configure()`. The root Guice injector SHALL NOT synthesize dynamic bytecode proxies to resolve circular dependency cycles between components, enforcing a strict directed acyclic graph across the **application module**.

#### Scenario: Injector initialization rejects circular dependency cycles
- **GIVEN** a Guice module defining a circular dependency cycle between two components
- **WHEN** the injector attempts to initialize with the circular module combined with `ServerModule`
- **THEN** Guice fails bootstrap with a configuration error prohibiting circular proxies
- **AND** zero proxy instances are generated to resolve the cycle

### Requirement: Require Explicit Inject on Constructors in Root ServerModule
The system SHALL install `Modules.requireAtInjectOnConstructorsModule()` inside `ServerModule.configure()`. The root Guice injector SHALL refuse to instantiate any class via constructor injection unless that class declares an explicit constructor annotated with `@Inject`. All singleton and persistence components in the **data plane** bound to classes, including `ActiveSessionFactories`, `DefaultDataSourceFactory`, and `DefaultFlywayFactory`, SHALL declare package-private `@Inject` constructors complying with **Njall** non-public constructor invariants.

#### Scenario: Injector rejects unannotated zero-argument constructor instantiation
- **GIVEN** a bound class lacking an `@Inject` constructor annotation
- **WHEN** the root Guice injector is initialized with `ServerModule`
- **THEN** Guice raises a configuration exception indicating the class must have a constructor annotated with `@Inject`

#### Scenario: Data plane bound components declare package-private Inject constructors
- **GIVEN** `ActiveSessionFactories`, `DefaultDataSourceFactory`, and `DefaultFlywayFactory` in `:data`
- **WHEN** class constructors are inspected for annotations and modifiers
- **THEN** each class declares a constructor annotated with `@Inject`
- **AND** each constructor has package-private access modifier without the `public` keyword

### Requirement: Require Explicit Module Bindings in Root ServerModule
The system SHALL install `Modules.requireExplicitBindingsModule()` inside `ServerModule.configure()`. The root Guice injector SHALL disable Just-In-Time (JIT) binding synthesis, requiring every requested or injected dependency to be explicitly declared via module bindings or provider methods. The **API plane** `AdminModule` SHALL explicitly declare bindings for concrete route classes `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute`.

#### Scenario: Injector fails on missing explicit binding
- **GIVEN** a class that is not declared in any installed module or provider
- **WHEN** an instance of that class is requested from the root injector
- **THEN** Guice throws a configuration exception stating that explicit bindings are required

#### Scenario: Concrete admin routes are resolved from explicit bindings
- **GIVEN** `AdminModule` installed in the root `ServerModule`
- **WHEN** `StudioAdminRoute`, `UserAdminRoute`, or `RoleAdminRoute` are requested from the injector
- **THEN** the injector resolves the instance successfully from explicit module bindings
- **AND** zero JIT bindings are created

### Requirement: Require Exact Binding Annotations in Root ServerModule
The system SHALL install `Modules.requireExactBindingAnnotationsModule()` inside `ServerModule.configure()`. The root Guice injector SHALL require binding annotations to match exact attribute values rather than matching any annotation of that type.

#### Scenario: Injector enforces exact annotation match
- **GIVEN** a dependency requested with specific annotation attributes
- **WHEN** a binding exists with differing annotation attributes
- **THEN** the root injector rejects the resolution and requires exact matching binding annotations
