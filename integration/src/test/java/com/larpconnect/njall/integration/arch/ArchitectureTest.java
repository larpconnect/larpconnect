package com.larpconnect.njall.integration.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.hibernate.SessionFactory;

/** ArchUnit architectural invariants for Project Njall. */
@AnalyzeClasses(
    packages = "com.larpconnect.njall",
    importOptions = {ImportOption.DoNotIncludeTests.class})
final class ArchitectureTest {

  @ArchTest
  public static final ArchRule session_factory_must_not_be_injected_bare =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .should(notInjectBareSessionFactory())
          .as("SessionFactory must not be injected bare without Provider wrapping");

  private static ArchCondition<JavaClass> notInjectBareSessionFactory() {
    return new ArchCondition<>("not inject SessionFactory directly without a Provider") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        checkConstructors(javaClass, events);
        checkMethods(javaClass, events);
        checkFields(javaClass, events);
      }
    };
  }

  private static void checkConstructors(JavaClass javaClass, ConditionEvents events) {
    for (JavaConstructor constructor : javaClass.getConstructors()) {
      if (isInjectAnnotated(constructor)) {
        checkParameters(constructor.getParameters(), constructor.getFullName(), events);
      }
    }
  }

  private static void checkMethods(JavaClass javaClass, ConditionEvents events) {
    for (JavaMethod method : javaClass.getMethods()) {
      if (isInjectAnnotated(method) || method.isAnnotatedWith("com.google.inject.Provides")) {
        checkParameters(method.getParameters(), method.getFullName(), events);
      }
    }
  }

  private static void checkParameters(
      Iterable<JavaParameter> parameters, String location, ConditionEvents events) {
    for (JavaParameter parameter : parameters) {
      if (isBareSessionFactory(parameter.getRawType())) {
        var message =
            String.format(
                "%s injects bare SessionFactory (parameter index %d)",
                location, parameter.getIndex());
        events.add(SimpleConditionEvent.violated(parameter, message));
      }
    }
  }

  private static void checkFields(JavaClass javaClass, ConditionEvents events) {
    for (JavaField field : javaClass.getFields()) {
      if (isInjectAnnotated(field) && isBareSessionFactory(field.getRawType())) {
        var message = String.format("Field %s injects bare SessionFactory", field.getFullName());
        events.add(SimpleConditionEvent.violated(field, message));
      }
    }
  }

  private static boolean isInjectAnnotated(HasAnnotations<?> element) {
    return element.isAnnotatedWith("com.google.inject.Inject")
        || element.isAnnotatedWith("jakarta.inject.Inject")
        || element.isAnnotatedWith("javax.inject.Inject");
  }

  private static boolean isBareSessionFactory(JavaClass type) {
    return type.isAssignableTo(SessionFactory.class);
  }
}
