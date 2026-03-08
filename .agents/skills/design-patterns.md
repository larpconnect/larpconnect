# Skill: Common Design Patterns

## Purpose

This skill provides guidance on common design patterns used by LarpConnect and how to navigate them.

## Technical Constraints

- The system uses JDK 25+
- It makes heavy use of guice, guava, and vert.x

## Specific Patterns

### Single Implementation Interfaces

A relatively common situation is only needing to create one type of something. For this a single implementation
interface is often the right strategy.

```java
Foo.java:

interface Foo {}

DefaultFoo.java:

final class DefaultFoo implements Foo {
   @Inject
   DefaultFoo() {}
}

LocalModule.java:

public final class LocalModule extends AbstractModule {
   void configure() {
      bind(Foo.class).to(DefaultFoo.class);
   }
}
```

This format is always preferable to having a "naked" class without an interface.

### Guice Module Layout

The basic pattern for modules is as follows:

```java
BarModule.java:

package foo.bar;

import foo.bar.baz.BazModule;
// … other imports

public class BarModule extends AbstractModule {
   void configure() {
     install(new BazModule());

     install(new BarBindingModule());
   }
}

BarBindingModule.java:

package foo.bar;

// … other imports

@InstallInstead(BarModule.class)
public class BarBindingModule extends AbstractModule {
   void configure() {
     bind(Bar.class).to(DefaultBar.class);
   }

   @Provides
   BarBuilder barBuilder() {
      // BarBuilder construction code 
   }
}
```

Note that it doesn't have to be named as a _binding_ module, but names here should generally be descriptive
rather than simply restating the kinds of bindings being used. For example, separating out the `Factory` bindings,
separating out all bindings that relate to a single internal subcomponent, etc.

### Guava Services

The easiest pattern for utilizing things that involve initialization is to have two interfaces on top of the class:

```java
public interface Capability {}

public interface CapabilityService extends Capability, Service {}

class DefaultCapabilityService implements Capability extends AbstractIdleService {}
```

Then in the guice module bind it as follows:

```java

void configuration() {
  bind(Capability.class).to(CapabilityService.class);
  bind(CapabilityService.class).to(DefaultCapabilityService.class).in(Singleton.class);
}
```

Then in code that needs to manage the _lifecycle_ of `Capability` you inject the `CapabilityService`, while the rest of the system
only needs to deal with `Capability`. This makes `Capability` easy to mock for downstream classes as well. 

### Utility Classes

_Most_ utility classes should be injected via guice. These are just like any other class and by injecting them via guice it allows for
them to be mocked. 

The exceptional cases are things where we can never really envision needing to mock it: the functions are so straightforward
and formulaic, on the level of basic transforms, or the utility so ubiquetous that static methods are called for. In that case:

```java

@Immutable
public final Utility {
  private Utility() {}

  public static String escape(String arg) { /* code here */ }
}
```

Note marking this as `final`  and `@Immutable`: these objects must always disallow inheritance and never carry state of any form. They
must also have a `private` constructor to forbid instantiation. 

Strongly prefer, however, writing this as a Guice object that can be injected:

```java

Utility.java:

@Immutable
@DefaultImplementation(DefaultUtility.class)
public interface Utility {
   String escape(String arg);
}

DefaultUtility.java:
public final Utility {
   @Inject
   Utility() {}

   @Override
   public String escape(String arg) { /* code here */ }
}
```


