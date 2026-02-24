# Skill: Guice Dependency Injection

## Domain Context
This skill handles working with Guice for dependency injection.

## Technical Constraints

Guice is the preferred dependency injection framework.

* Always use Guice 7+
* Dependencies form a directed acyclic graph
* Favor explicit bindings and declarations

## Specific guidance

The following best practices are followed:

* Every package may have exactly one public `Module`. It may have any number of private `Module`s.
* Do not ever use `com.google.inject.PrivateModule`. Prefer using purpose-built annotations, especially for types that we do not own as part of this project. 
* Modules may _either_ include other modules _or_ include bindings, never both. When both are needed create a package-protected class that extends `AbstractModule` with the bindings to `install` in the public `Module`.
* A package's public module should include all of the public modules of immediate subpackages. This should turn the modules into a directed-acyclic-graph of packages.
* Always use constructor injection and explicit bindings. Do not use `@ImplementedBy`.
* Prefer creating:
  * A public interface and a package-protected implementation class, then binding it in a module
  * A public sealed interface and a package-protected implementation class, then binding it in a module.
  * A record.
* Applications (e.g., servers) top level module should always include:
  * `Modules.requireAtInjectOnConstructorsModule()`
  * `Modules.disableCircularProxiesModule()`
  * `Modules.requireExplicitBindingsModule()`
* Every library should have a single top level `Module` that can be included.
* Prefer JSR330 annotations or jakarta annotations to guice-specific annotations.
* Do not use `@Named` annotations and instead use custom annotations.

### Standard Pattern

The standard pattern for a given package is to have **two** modules:

Class 1 is the _binding_ module: 

```
package foo;

@InstallInstead(<Foo>Module.class)
(package private) final class <Foo>BindingModule extends AbstractModule {
  <Foo>BindingModule() {}

  // Bindings, providers, etc go here
}
```

Class 2 is the _installing_ module:

```
package foo;

public final class <Foo>Module extends AbstractModule {
   public <Foo>Module() {}

   protected void configure() {
      install(new <Foo>BindingModule());
   }
}
```

It would also install the `<Baz>Module()` from `foo.baz`.

Now, any time you want something bound by `<Foo>BindingModule` the `InstallInstead` annotation prompts you to install `<Foo>Module` instead. This is not automated and must (for now) be manually wired—you need to add the `install()` directive yourself. But once this is done you always default to where `InstallInstead` directs you: to `FooModule()`.
