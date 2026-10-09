package com.larpconnect.njall.integration.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;

/** ArchUnit architectural invariants for Project Njall. */
@AnalyzeClasses(
    packages = "com.larpconnect.njall",
    importOptions = {ImportOption.DoNotIncludeTests.class})
final class ArchitectureTest {

  private static final String NJALL_NAMESPACE = "com.larpconnect.njall";

  @ArchTest
  public static final ArchRule session_factory_must_not_be_injected_bare =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .should(notInjectBareSessionFactory())
          .as("SessionFactory must not be injected bare without Provider wrapping");

  @ArchTest
  public static final ArchRule inject_constructors_must_not_be_public =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .should(notHavePublicInjectConstructors())
          .as("Constructors annotated with @Inject or @AssistedInject must not be public");

  @ArchTest
  public static final ArchRule package_dependencies_must_not_go_up =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .should(notDependOnAncestorPackages())
          .as("Package dependencies within com.larpconnect.njall must go down or out, never up");

  @ArchTest
  public static final ArchRule packages_must_not_exceed_twenty_types =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .should(notExceedTwentyTypesPerPackage())
          .as("Packages within com.larpconnect.njall must not exceed 20 types");

  private static final DescribedPredicate<JavaClass> ARE_RECORDS =
      DescribedPredicate.describe("are records", JavaClass::isRecord);

  @ArchTest
  public static final ArchRule records_must_not_declare_factory_methods =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .and(ARE_RECORDS)
          .should(notDeclareRecordFactoryMethods())
          .as("Record objects must not declare static factory methods returning the record type");

  @ArchTest
  public static final ArchRule records_must_be_annotated_immutable =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .and(ARE_RECORDS)
          .should()
          .beAnnotatedWith("com.google.errorprone.annotations.Immutable")
          .as("Record objects must be annotated with @com.google.errorprone.annotations.Immutable");

  @ArchTest
  public static final ArchRule records_must_not_have_nullable_components =
      classes()
          .that()
          .resideInAPackage("com.larpconnect.njall..")
          .and(ARE_RECORDS)
          .should(notHaveNullableRecordComponents())
          .as("Record objects must not declare @Nullable record components");

  private static ArchCondition<JavaClass> notDeclareRecordFactoryMethods() {
    return new ArchCondition<>("not declare static factory methods returning the record type") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        for (JavaMethod method : javaClass.getMethods()) {
          if (method.getModifiers().contains(JavaModifier.STATIC)
              && isFactoryMethodFor(method, javaClass)) {
            var message =
                String.format(
                    "Record %s declares static factory method '%s' returning %s",
                    javaClass.getFullName(),
                    method.getName(),
                    method.getRawReturnType().getFullName());
            events.add(SimpleConditionEvent.violated(method, message));
          }
        }
      }
    };
  }

  private static boolean isFactoryMethodFor(JavaMethod method, JavaClass recordClass) {
    var rawReturn = method.getRawReturnType();
    if (rawReturn.equals(recordClass)) {
      return true;
    }
    if (rawReturn.isAssignableTo(Optional.class)) {
      var returnType = method.getReturnType();
      if (returnType instanceof JavaParameterizedType parameterized) {
        for (JavaType typeArg : parameterized.getActualTypeArguments()) {
          if (typeArg.getName().equals(recordClass.getName())) {
            return true;
          }
        }
      }
    }
    return false;
  }

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

  private static ArchCondition<JavaClass> notHavePublicInjectConstructors() {
    return new ArchCondition<>("not declare public @Inject or @AssistedInject constructors") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        for (JavaConstructor constructor : javaClass.getConstructors()) {
          if (isInjectOrAssistedInject(constructor) && isPublic(constructor)) {
            var message =
                String.format(
                    "Constructor %s is annotated with @Inject/@AssistedInject but is declared"
                        + " public",
                    constructor.getFullName());
            events.add(SimpleConditionEvent.violated(constructor, message));
          }
        }
      }
    };
  }

  private static ArchCondition<JavaClass> notDependOnAncestorPackages() {
    return new ArchCondition<>("not depend on ancestor packages within com.larpconnect.njall") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        var originPackage = javaClass.getPackageName();
        if (!isWithinNjall(originPackage)) {
          return;
        }
        for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
          var targetPackage = dependency.getTargetClass().getPackageName();
          if (isWithinNjall(targetPackage) && isAncestorPackage(targetPackage, originPackage)) {
            var message =
                String.format(
                    "%s in package '%s' depends on %s in ancestor package '%s'",
                    javaClass.getFullName(),
                    originPackage,
                    dependency.getTargetClass().getFullName(),
                    targetPackage);
            events.add(SimpleConditionEvent.violated(dependency, message));
          }
        }
      }
    };
  }

  private static boolean isWithinNjall(String packageName) {
    return packageName.equals(NJALL_NAMESPACE) || packageName.startsWith(NJALL_NAMESPACE + ".");
  }

  private static boolean isAncestorPackage(String candidateAncestor, String descendant) {
    return !candidateAncestor.equals(descendant) && descendant.startsWith(candidateAncestor + ".");
  }

  private static boolean isInjectAnnotated(HasAnnotations<?> element) {
    return element.isAnnotatedWith("com.google.inject.Inject")
        || element.isAnnotatedWith("jakarta.inject.Inject")
        || element.isAnnotatedWith("javax.inject.Inject");
  }

  private static boolean isInjectOrAssistedInject(HasAnnotations<?> element) {
    return isInjectAnnotated(element)
        || element.isAnnotatedWith("com.google.inject.assistedinject.AssistedInject");
  }

  private static boolean isPublic(JavaConstructor constructor) {
    return constructor.getModifiers().contains(JavaModifier.PUBLIC);
  }

  private static boolean isBareSessionFactory(JavaClass type) {
    return type.isAssignableTo(SessionFactory.class);
  }

  private static ArchCondition<JavaClass> notHaveNullableRecordComponents() {
    return new ArchCondition<>("not declare @Nullable record components") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        var flagged = new HashSet<String>();
        try {
          var recordClass = javaClass.reflect();
          if (recordClass.isRecord()) {
            for (var rc : recordClass.getRecordComponents()) {
              for (var anno : rc.getAnnotations()) {
                if (anno.annotationType().getName().contains("Nullable")) {
                  flagged.add(rc.getName());
                  var message =
                      String.format(
                          "Record %s declares @Nullable record component '%s'",
                          javaClass.getFullName(), rc.getName());
                  events.add(SimpleConditionEvent.violated(javaClass, message));
                }
              }
            }
          }
        } catch (Exception ignored) {
          // If class cannot be loaded reflectively, fallback to field analysis below.
        }
        for (JavaField field : javaClass.getFields()) {
          if (!field.getModifiers().contains(JavaModifier.STATIC)
              && !flagged.contains(field.getName())
              && isNullable(field)) {
            var message =
                String.format(
                    "Record %s declares @Nullable field '%s'",
                    javaClass.getFullName(), field.getName());
            events.add(SimpleConditionEvent.violated(field, message));
          }
        }
      }
    };
  }

  private static boolean isNullable(HasAnnotations<?> element) {
    return element.isAnnotatedWith("org.jspecify.annotations.Nullable")
        || element.isAnnotatedWith("org.checkerframework.checker.nullness.qual.Nullable")
        || element.isAnnotatedWith("javax.annotation.Nullable");
  }

  private static ArchCondition<JavaClass> notExceedTwentyTypesPerPackage() {
    return new ArchCondition<>(
        "not exceed 20 types per package and contain @NullMarked package-info") {
      private final Map<String, List<JavaClass>> classesByPackage = new HashMap<>();

      @Override
      public void init(Collection<JavaClass> allObjectsToTest) {
        classesByPackage.clear();
      }

      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        var pkg = javaClass.getPackageName();
        if (isWithinNjall(pkg)) {
          classesByPackage.computeIfAbsent(pkg, k -> new ArrayList<>()).add(javaClass);
        }
      }

      @Override
      public void finish(ConditionEvents events) {
        for (var entry : classesByPackage.entrySet()) {
          evaluatePackage(entry.getKey(), entry.getValue(), events);
        }
      }
    };
  }

  private static void evaluatePackage(String pkg, List<JavaClass> classes, ConditionEvents events) {
    checkPackageInfo(pkg, classes, events);
    var counted = computeCountedTypes(classes);
    if (counted.size() > 20) {
      var names = counted.stream().map(JavaClass::getSimpleName).sorted().toList();
      var message =
          String.format(
              "Package '%s' has %d types (maximum allowed is 20): %s", pkg, counted.size(), names);
      events.add(SimpleConditionEvent.violated(classes.getFirst(), message));
    }
  }

  private static void checkPackageInfo(
      String pkg, List<JavaClass> classes, ConditionEvents events) {
    var hasValidPackageInfo =
        classes.stream()
            .anyMatch(
                c ->
                    c.getSimpleName().equals("package-info")
                        && c.isAnnotatedWith("org.jspecify.annotations.NullMarked"));
    if (!hasValidPackageInfo) {
      events.add(
          SimpleConditionEvent.violated(
              classes.getFirst(),
              String.format("Package '%s' is missing @NullMarked package-info.java", pkg)));
    }
  }

  private static List<JavaClass> computeCountedTypes(List<JavaClass> classes) {
    var candidates = new ArrayList<JavaClass>();
    for (var javaClass : classes) {
      if (!shouldSkipFromTypeCount(javaClass)) {
        candidates.add(javaClass);
      }
    }

    var interfaces =
        candidates.stream()
            .filter(JavaClass::isInterface)
            .map(JavaClass::getSimpleName)
            .collect(Collectors.toSet());

    var counted = new ArrayList<JavaClass>();
    var pairedSeen = new HashSet<String>();
    for (var candidate : candidates) {
      var name = candidate.getSimpleName();
      if (name.startsWith("Default")) {
        var ifaceName = name.substring("Default".length());
        if (interfaces.contains(ifaceName) && pairedSeen.add(ifaceName)) {
          continue;
        }
      }
      counted.add(candidate);
    }
    return counted;
  }

  private static boolean shouldSkipFromTypeCount(JavaClass javaClass) {
    if (javaClass.getSimpleName().equals("package-info")) {
      return true;
    }
    if (javaClass.isEnum()) {
      return true;
    }
    if (javaClass.isNestedClass() && isPrivateNested(javaClass)) {
      return true;
    }
    if (isGuiceModule(javaClass) && !javaClass.getModifiers().contains(JavaModifier.PUBLIC)) {
      return true;
    }
    return false;
  }

  /**
   * Traverses enclosing classes to determine if the class is nested within a private type. Under
   * the Java Language Specification, nested records and interfaces declared inside private
   * interfaces are implicitly {@code public static} in bytecode, so inspecting only the immediate
   * type's modifiers is insufficient.
   */
  private static boolean isPrivateNested(JavaClass javaClass) {
    if (javaClass.getModifiers().contains(JavaModifier.PRIVATE)) {
      return true;
    }
    var enclosing = javaClass.getEnclosingClass();
    while (enclosing.isPresent()) {
      if (enclosing.get().getModifiers().contains(JavaModifier.PRIVATE)) {
        return true;
      }
      enclosing = enclosing.get().getEnclosingClass();
    }
    return false;
  }

  private static boolean isGuiceModule(JavaClass javaClass) {
    return javaClass.isAssignableTo("com.google.inject.Module");
  }
}
