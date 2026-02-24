package com.larpconnect.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.larpconnect.common.annotations.DefaultImplementation;
import com.larpconnect.common.annotations.InstallInstead;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.slf4j.Logger;

@AnalyzeClasses(
    packages = "com.larpconnect",
    importOptions = {ImportOption.DoNotIncludeTests.class})
final class ArchitectureTest {

  // 1. No more than 1 public Module per package.
  @ArchTest
  public static final ArchRule no_more_than_one_public_module_per_package =
      classes()
          .that()
          .implement(Module.class)
          .and()
          .arePublic()
          .should(
              new ArchCondition<JavaClass>("reside in a package with at most one public Module") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                  long publicModulesInPackage =
                      item.getPackage().getClasses().stream()
                          .filter(
                              clazz ->
                                  clazz.isAssignableTo(Module.class)
                                      && clazz.getModifiers().contains(JavaModifier.PUBLIC))
                          .count();
                  if (publicModulesInPackage > 1) {
                    String message =
                        String.format(
                            "Package %s has %d public Modules",
                            item.getPackageName(), publicModulesInPackage);
                    events.add(SimpleConditionEvent.violated(item, message));
                  }
                }
              })
          .allowEmptyShould(true);

  // 2. Classes must be either abstract or final (does not affect records, enums, etc).
  @ArchTest
  public static final ArchRule classes_must_be_abstract_or_final =
      classes()
          .that()
          .areNotInterfaces()
          .and()
          .areNotEnums()
          .and()
          .areNotRecords()
          .and()
          .areNotAnonymousClasses()
          .should()
          .haveModifier(JavaModifier.ABSTRACT)
          .orShould()
          .haveModifier(JavaModifier.FINAL);

  // 3. Non-Module classes should not be public.
  @ArchTest
  public static final ArchRule non_module_classes_should_not_be_public =
      classes()
          .that()
          .areNotInterfaces()
          .and()
          .areNotEnums()
          .and()
          .areNotRecords()
          .and()
          .areNotAssignableTo(Module.class)
          .should()
          .notBePublic();

  // 4. Logger declarations must NOT be static.
  @ArchTest
  public static final ArchRule loggers_should_not_be_static =
      fields().that().haveRawType(Logger.class).should().notBeStatic();

  // 5. `Default<InterfaceName>` should mean that `<InterfaceName>` is annotated with
  // `@DefaultImplementation`
  @ArchTest
  public static final ArchRule default_impl_naming_convention =
      classes()
          .that()
          .haveSimpleNameStartingWith("Default")
          .and()
          .areNotInterfaces()
          .should(
              new ArchCondition<JavaClass>(
                  "implement an interface named after the class (minus 'Default') which is annotated"
                      + " with @DefaultImplementation") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                  String interfaceName = item.getSimpleName().substring("Default".length());
                  // getInterfaces() returns JavaClassList in recent versions? No, JavaClass implements HasInterfaces which returns JavaClassList/Set<JavaClass>
                  // But wait, the error said "variable i of type JavaType" which implies getInterfaces() might be returning JavaType?
                  // ArchUnit 1.4.1: JavaClass.getInterfaces() returns JavaClassList.
                  // Let's coerce to JavaClass if possible or check docs.
                  // Actually, let's use getRawType() if it's a JavaType.
                  // But let's check what getInterfaces() returns.

                  // In ArchUnit, JavaClass.getInterfaces() returns JavaClassList, which is a List<JavaClass>.
                  // JavaClass has getSimpleName() and isAnnotatedWith().
                  // The error "variable i of type JavaType" is strange if it's JavaClass.
                  // Ah, maybe I imported something wrong? No imports look standard.

                  // Wait, looking at docs for 1.0.0+:
                  // JavaClass.getInterfaces() returns JavaClassList.
                  // However, let's look at the error again:
                  // symbol: method getSimpleName() location: variable i of type JavaType

                  // Maybe getInterfaces() returns JavaType in some context?
                  // Ah, `getInterfaces()` vs `getDirectInterfaces()`?
                  // Let's try `getAllInterfaces()` or cast.

                  // Actually, let's just use `toErasure()` if it's a generic type issue, but JavaClass implements JavaType.
                  // JavaType does NOT have getSimpleName(). JavaClass does.

                  // Let's try `item.getAllInterfaces()` which returns `Set<JavaClass>`.

                  boolean implementsInterface =
                      item.getInterfaces().stream()
                          .map(i -> i.toErasure())
                          .anyMatch(
                              i ->
                                  i.getSimpleName().equals(interfaceName)
                                      && i.isAnnotatedWith(DefaultImplementation.class));

                  if (!implementsInterface) {
                    var interfaceOpt =
                        item.getInterfaces().stream()
                            .map(i -> i.toErasure())
                            .filter(i -> i.getSimpleName().equals(interfaceName))
                            .findFirst();

                    if (interfaceOpt.isPresent()) {
                      if (!interfaceOpt.get().isAnnotatedWith(DefaultImplementation.class)) {
                        events.add(
                            SimpleConditionEvent.violated(
                                item,
                                String.format(
                                    "Class %s implements %s but %s is not annotated with"
                                        + " @DefaultImplementation",
                                    item.getName(),
                                    interfaceOpt.get().getName(),
                                    interfaceOpt.get().getName())));
                      }
                    } else {
                      // Maybe strict: if named DefaultFoo, must implement Foo?
                      // The requirement says "Default<InterfaceName> should mean..."
                      // I will assume if the class is named DefaultFoo, it is INTENDED to be the
                      // default for Foo.
                      // So Foo should probably exist and be annotated.
                      // But purely from ArchUnit, we can only easily check loaded classes.
                      // Let's stick to: If it implements an interface that matches the name
                      // pattern, check annotation.
                      // If it doesn't implement it, we might ignore or flag. Let's flag for now as
                      // it's likely a violation of intent.
                      events.add(
                          SimpleConditionEvent.violated(
                              item,
                              String.format(
                                  "Class %s starts with 'Default' but does not implement a"
                                      + " matching interface %s annotated with"
                                      + " @DefaultImplementation",
                                  item.getName(), interfaceName)));
                    }
                  }
                }
              })
          .allowEmptyShould(true);

  // 6. `@InstallInstead` can only be used to annotate modules.
  @ArchTest
  public static final ArchRule install_instead_only_on_modules =
      classes()
          .that()
          .areAnnotatedWith(InstallInstead.class)
          .should()
          .beAssignableTo(Module.class)
          .allowEmptyShould(true);
}
