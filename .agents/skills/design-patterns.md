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

@DefaultImplementation(DefaultFoo.class)
interface Foo {}

DefaultFoo.java:

@BuildWith(LocalModule.class)
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

class DefaultCapabilityService extends AbstractIdleService implements Capability {}
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

### Basic API Pattern

1. Create a protobuf that represents any JSON in the request and/or response.
2. Modify the `openapi.yaml` file to add the endpoint
3. Add the verticle to handle the JSON version of the request in `:api`. It MAY delegate
   to some other location or it may solve it locally. If it delegates then it SHOULD
   convert the JSON into a protobuf and convert any replies payload into JSON.
4. Wire it in the `:server` module.
5. Write unit tests.
6. Write integration tests.

### Database modifications

1. Add a file with an incrementing number in the `migrations` folder in `:data`, e.g., `00_migration_name.sql` or `01_migration_name_2.sql`
2. In that file create an _idempotent_ SQL script making changes. It can be safely assumed that SQL changes will start with the `init` and then happen _in order_.

### Standard Data Transfer Object Pattern

1. Create the object using Hibernate annotations and register it appropriately
2. If it is not part of another object's lifecycle then create a DAO for it
3. Wire it together using guice
4. Ensure that all branches are tested, especially equals functions
5. Assume pgplsql as the dialect and target it accordingly
