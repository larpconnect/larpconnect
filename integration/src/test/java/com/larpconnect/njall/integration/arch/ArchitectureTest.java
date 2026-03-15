package com.larpconnect.njall.integration.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.larpconnect.njall")
public final class ArchitectureTest {
  private ArchitectureTest() {}

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
          .areNotAssignableTo(com.google.inject.Module.class)
          .and()
          .resideOutsideOfPackage("com.larpconnect.njall.proto..")
          .and()
          .resideOutsideOfPackage("com.larpconnect.njall.data.entity..")
          .and()
          .resideOutsideOfPackage("com.larpconnect.njall.data.dao..")
          .and()
          .resideOutsideOfPackage("com.larpconnect.njall.integration..")
          .should()
          .notBePublic();

  @ArchTest
  public static final ArchRule
      non_interface_and_non_record_and_non_anonymous_classes_should_be_abstract_or_final =
          classes()
              .that()
              .areNotInterfaces()
              .and()
              .areNotEnums()
              .and()
              .areNotRecords()
              .and()
              .areNotAnonymousClasses()
              .and()
              .resideOutsideOfPackage("com.larpconnect.njall.proto..")
              .and()
              .resideOutsideOfPackage("com.larpconnect.njall.data.entity..")
              .and()
              .resideOutsideOfPackage("com.larpconnect.njall.data.dao..")
              .and()
              .resideOutsideOfPackage("com.larpconnect.njall.integration..")
              .should()
              .haveModifier(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
              .orShould()
              .haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL);
}
