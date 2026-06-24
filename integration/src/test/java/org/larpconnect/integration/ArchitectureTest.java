package org.larpconnect.integration;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Validates the codebase's structural invariants using ArchUnit. */
public final class ArchitectureTest {
  private static JavaClasses productionClasses;

  @BeforeAll
  public static void setUp() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.larpconnect");
  }

  @Test
  public void verifyNoCircularDependenciesBetweenModules() {
    slices().matching("org.larpconnect.(*)..").should().beFreeOfCycles().check(productionClasses);
  }

  @Test
  public void verifyNoCircularDependenciesBetweenPackages() {
    slices()
        .assignedFrom(new PackageSliceAssignment())
        .should()
        .beFreeOfCycles()
        .check(productionClasses);
  }

  @Test
  public void verifyAtMostOnePublicGuiceModulePerPackageLayer() {
    Map<String, List<JavaClass>> classesByPackage =
        productionClasses.stream().collect(Collectors.groupingBy(JavaClass::getPackageName));

    for (Map.Entry<String, List<JavaClass>> entry : classesByPackage.entrySet()) {
      String packageName = entry.getKey();
      List<JavaClass> publicGuiceModules =
          entry.getValue().stream()
              .filter(c -> c.isAssignableTo(com.google.inject.Module.class))
              .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
              .collect(Collectors.toList());

      if (publicGuiceModules.size() > 1) {
        throw new AssertionError(
            String.format(
                "Package '%s' has more than one public Guice module: %s",
                packageName,
                publicGuiceModules.stream().map(JavaClass::getName).collect(Collectors.toList())));
      }
    }
  }

  @Test
  public void verifyLibraryModulesDoNotInstallExternalModules() {
    for (JavaClass clazz : productionClasses) {
      if (clazz.isAssignableTo(com.google.inject.Module.class)) {
        checkLibraryModuleDependencies(clazz);
      }
    }
  }

  private void checkLibraryModuleDependencies(JavaClass clazz) {
    String packageName = clazz.getPackageName();
    String libPrefix = getLibraryPrefix(packageName);
    if (libPrefix == null) {
      return;
    }
    for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
      JavaClass target = dependency.getTargetClass();
      if (target.isAssignableTo(com.google.inject.Module.class)
          && target.getPackageName().startsWith("org.larpconnect.")) {
        String targetPackageName = target.getPackageName();
        if (!targetPackageName.startsWith(libPrefix)) {
          throw new AssertionError(
              String.format(
                  "Library module '%s' in package '%s' illegally references external Guice"
                      + " module '%s' in package '%s'",
                  clazz.getName(), packageName, target.getName(), targetPackageName));
        }
      }
    }
  }

  @Test
  public void verifyGuiceModuleChildPackageInstallation() {
    Map<String, JavaClass> publicModulesByPackage =
        productionClasses.stream()
            .filter(c -> c.isAssignableTo(com.google.inject.Module.class))
            .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
            .collect(Collectors.toMap(JavaClass::getPackageName, c -> c));

    for (Map.Entry<String, JavaClass> entry : publicModulesByPackage.entrySet()) {
      String pkgName = entry.getKey();
      JavaClass module = entry.getValue();

      if (pkgName.startsWith("org.larpconnect.server")
          || pkgName.startsWith("org.larpconnect.integration")) {
        continue;
      }
      checkChildModuleInstallation(pkgName, module, publicModulesByPackage);
    }
  }

  private void checkChildModuleInstallation(
      String pkgName, JavaClass module, Map<String, JavaClass> publicModulesByPackage) {
    List<JavaClass> immediateSubModules =
        publicModulesByPackage.entrySet().stream()
            .filter(e -> isImmediateSubpackage(pkgName, e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

    Set<JavaClass> dependencies =
        module.getDirectDependenciesFromSelf().stream()
            .map(Dependency::getTargetClass)
            .filter(c -> c.isAssignableTo(com.google.inject.Module.class))
            .filter(c -> c.getPackageName().startsWith("org.larpconnect."))
            .collect(Collectors.toSet());

    checkDependenciesAreImmediateSubpackages(module, pkgName, dependencies);
    checkAllImmediateSubpackagesAreInstalled(module, pkgName, immediateSubModules, dependencies);
  }

  private void checkDependenciesAreImmediateSubpackages(
      JavaClass module, String pkgName, Set<JavaClass> dependencies) {
    for (JavaClass dep : dependencies) {
      if (dep.getPackageName().equals(pkgName)) {
        continue;
      }
      if (!isImmediateSubpackage(pkgName, dep.getPackageName())) {
        throw new AssertionError(
            String.format(
                "Module '%s' in package '%s' is only allowed to depend on modules in its"
                    + " immediate subpackages exactly one layer down, but references '%s' in"
                    + " package '%s'",
                module.getName(), pkgName, dep.getName(), dep.getPackageName()));
      }
    }
  }

  private void checkAllImmediateSubpackagesAreInstalled(
      JavaClass module,
      String pkgName,
      List<JavaClass> immediateSubModules,
      Set<JavaClass> dependencies) {
    for (JavaClass subModule : immediateSubModules) {
      if (!dependencies.contains(subModule)) {
        throw new AssertionError(
            String.format(
                "Module '%s' in package '%s' must install its immediate subpackage module '%s'",
                module.getName(), pkgName, subModule.getName()));
      }
    }
  }

  private static String getLibraryPrefix(String packageName) {
    if (!packageName.startsWith("org.larpconnect.")) {
      return null;
    }
    String sub = packageName.substring("org.larpconnect.".length());
    int dot = sub.indexOf('.');
    String moduleName = dot == -1 ? sub : sub.substring(0, dot);
    if (moduleName.equals("server") || moduleName.equals("integration")) {
      return null;
    }
    return "org.larpconnect." + moduleName;
  }

  private static boolean isImmediateSubpackage(String parent, String child) {
    if (!child.startsWith(parent + ".")) {
      return false;
    }
    String sub = child.substring(parent.length() + 1);
    return !sub.contains(".");
  }

  private static final class PackageSliceAssignment implements SliceAssignment {
    @Override
    public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
      return SliceIdentifier.of(javaClass.getPackageName());
    }

    @Override
    public String getDescription() {
      return "Package slice assignment";
    }
  }
}
